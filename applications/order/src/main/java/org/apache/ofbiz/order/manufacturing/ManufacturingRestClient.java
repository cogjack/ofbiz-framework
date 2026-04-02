/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package org.apache.ofbiz.order.manufacturing;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.ofbiz.base.util.Debug;

/**
 * REST client for calling Manufacturing API endpoints from the Order module.
 *
 * <p>Includes basic retry logic (1 retry with 1s delay) and a circuit breaker
 * pattern (fail-fast after 5 consecutive failures, reset after 30s).</p>
 *
 * <p>This client is used when the {@code manufacturing.api.use-rest} feature
 * flag is enabled, replacing direct {@code dispatcher.runSync} calls.</p>
 */
public final class ManufacturingRestClient {

    private static final String MODULE = ManufacturingRestClient.class.getName();

    private static final String PATH_FROM_CONFIG = "/production-runs/from-configuration";
    private static final String PATH_MKTG_PKG = "/production-runs/marketing-package";
    private static final int CONNECT_TIMEOUT_MS = 10000;
    private static final int READ_TIMEOUT_MS = 30000;
    private static final int MAX_RETRIES = 1;
    private static final long RETRY_DELAY_MS = 1000;
    private static final int CIRCUIT_BREAKER_THRESHOLD = 5;
    private static final long CIRCUIT_BREAKER_RESET_MS = 30000;

    private static final AtomicInteger CONSECUTIVE_FAILURES = new AtomicInteger(0);
    private static final AtomicLong CIRCUIT_OPEN_SINCE = new AtomicLong(0);

    private ManufacturingRestClient() { }

    /**
     * Calls the {@code POST /production-runs/from-configuration} REST endpoint.
     *
     * @param baseUrl the Manufacturing REST API base URL
     * @param facilityId the facility ID (required)
     * @param configId the product configuration ID (optional)
     * @param orderId the order ID (optional)
     * @param orderItemSeqId the order item sequence ID (optional)
     * @param quantity the quantity (optional)
     * @return a map with {@code productionRunId} on success, or error keys on failure
     */
    public static Map<String, Object> createProductionRunFromConfiguration(
            String baseUrl, String facilityId, String configId,
            String orderId, String orderItemSeqId, String quantity) {
        StringBuilder json = new StringBuilder("{");
        json.append("\"facilityId\":\"").append(escapeJson(facilityId)).append("\"");
        if (configId != null) {
            json.append(",\"configId\":\"").append(escapeJson(configId)).append("\"");
        }
        if (orderId != null) {
            json.append(",\"orderId\":\"").append(escapeJson(orderId)).append("\"");
        }
        if (orderItemSeqId != null) {
            json.append(",\"orderItemSeqId\":\"")
                    .append(escapeJson(orderItemSeqId)).append("\"");
        }
        if (quantity != null) {
            json.append(",\"quantity\":").append(quantity);
        }
        json.append("}");

        String url = baseUrl + PATH_FROM_CONFIG;
        return executeWithRetry(url, json.toString());
    }

    /**
     * Calls the {@code POST /production-runs/marketing-package} REST endpoint.
     *
     * @param baseUrl the Manufacturing REST API base URL
     * @param orderId the order ID (required)
     * @param orderItemSeqId the order item sequence ID (required)
     * @param facilityId the facility ID (required)
     * @return a map with {@code productionRunId} on success, or error keys on failure
     */
    public static Map<String, Object> createProductionRunForMktgPkg(
            String baseUrl, String orderId, String orderItemSeqId,
            String facilityId) {
        String json = "{\"orderId\":\"" + escapeJson(orderId)
                + "\",\"orderItemSeqId\":\"" + escapeJson(orderItemSeqId)
                + "\",\"facilityId\":\"" + escapeJson(facilityId) + "\"}";

        String url = baseUrl + PATH_MKTG_PKG;
        return executeWithRetry(url, json);
    }

    /**
     * Returns {@code true} if the circuit breaker is currently open
     * (too many consecutive failures).
     */
    public static boolean isCircuitOpen() {
        if (CONSECUTIVE_FAILURES.get() < CIRCUIT_BREAKER_THRESHOLD) {
            return false;
        }
        long openSince = CIRCUIT_OPEN_SINCE.get();
        if (openSince > 0
                && (System.currentTimeMillis() - openSince) > CIRCUIT_BREAKER_RESET_MS) {
            CONSECUTIVE_FAILURES.set(0);
            CIRCUIT_OPEN_SINCE.set(0);
            Debug.logInfo("Manufacturing REST circuit breaker reset after timeout", MODULE);
            return false;
        }
        return true;
    }

    /** Resets the circuit breaker state (useful for testing). */
    public static void resetCircuitBreaker() {
        CONSECUTIVE_FAILURES.set(0);
        CIRCUIT_OPEN_SINCE.set(0);
    }

    private static Map<String, Object> executeWithRetry(String url, String jsonBody) {
        if (isCircuitOpen()) {
            Debug.logWarning("Manufacturing REST circuit breaker is OPEN; "
                    + "failing fast for: " + url, MODULE);
            Map<String, Object> result = new HashMap<>();
            result.put("responseMessage", "error");
            result.put("errorMessage",
                    "Manufacturing REST circuit breaker is open; falling back to dispatcher");
            result.put("circuitOpen", Boolean.TRUE);
            return result;
        }

        Map<String, Object> result = null;
        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            if (attempt > 0) {
                Debug.logWarning("Retrying Manufacturing REST call (attempt "
                        + (attempt + 1) + "): " + url, MODULE);
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            result = doPost(url, jsonBody);
            if (!"error".equals(result.get("responseMessage"))) {
                recordSuccess();
                return result;
            }
        }
        recordFailure();
        return result;
    }

    private static Map<String, Object> doPost(String url, String jsonBody) {
        Map<String, Object> result = new HashMap<>();
        HttpURLConnection conn = null;
        try {
            Debug.logVerbose("Manufacturing REST call: POST " + url, MODULE);
            Debug.logVerbose("Manufacturing REST request body: " + jsonBody, MODULE);

            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
            }

            int status = conn.getResponseCode();
            String responseBody = readResponse(conn, status);
            Debug.logVerbose("Manufacturing REST response [" + status + "]: "
                    + responseBody, MODULE);

            if (status >= 200 && status < 300) {
                result.put("responseMessage", "success");
                parseJsonResponse(responseBody, result);
            } else {
                result.put("responseMessage", "error");
                result.put("errorMessage",
                        "Manufacturing REST API returned HTTP " + status
                                + ": " + responseBody);
            }
        } catch (IOException e) {
            Debug.logWarning(e, "Manufacturing REST call failed: " + url, MODULE);
            result.put("responseMessage", "error");
            result.put("errorMessage",
                    "Manufacturing REST call failed: " + e.getMessage());
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return result;
    }

    private static String readResponse(HttpURLConnection conn, int status)
            throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                status >= 400 ? conn.getErrorStream() : conn.getInputStream(),
                StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }

    /**
     * Simple JSON parser that extracts top-level string fields from a response.
     */
    private static void parseJsonResponse(String json, Map<String, Object> target) {
        if (json == null || json.trim().isEmpty()) {
            return;
        }
        String trimmed = json.trim();
        if (trimmed.startsWith("{")) {
            trimmed = trimmed.substring(1);
        }
        if (trimmed.endsWith("}")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        for (String pair : trimmed.split(",")) {
            int colonIdx = pair.indexOf(':');
            if (colonIdx < 0) {
                continue;
            }
            String key = unquote(pair.substring(0, colonIdx).trim());
            String value = pair.substring(colonIdx + 1).trim();
            if ("null".equals(value)) {
                target.put(key, null);
            } else {
                target.put(key, unquote(value));
            }
        }
    }

    private static String unquote(String value) {
        if (value == null) {
            return null;
        }
        String v = value.trim();
        if (v.length() >= 2 && v.startsWith("\"") && v.endsWith("\"")) {
            return v.substring(1, v.length() - 1);
        }
        return v;
    }

    private static String escapeJson(String raw) {
        if (raw == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            switch (c) {
            case '"':
                sb.append("\\\"");
                break;
            case '\\':
                sb.append("\\\\");
                break;
            case '\n':
                sb.append("\\n");
                break;
            case '\r':
                sb.append("\\r");
                break;
            case '\t':
                sb.append("\\t");
                break;
            default:
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static void recordSuccess() {
        CONSECUTIVE_FAILURES.set(0);
        CIRCUIT_OPEN_SINCE.set(0);
    }

    private static void recordFailure() {
        int failures = CONSECUTIVE_FAILURES.incrementAndGet();
        if (failures >= CIRCUIT_BREAKER_THRESHOLD) {
            CIRCUIT_OPEN_SINCE.compareAndSet(0, System.currentTimeMillis());
            Debug.logWarning("Manufacturing REST circuit breaker OPENED after "
                    + failures + " consecutive failures", MODULE);
        }
    }
}
