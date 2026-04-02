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
package org.apache.ofbiz.manufacturing.rest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.ofbiz.manufacturing.rest.dto.ApiErrorResponse;
import org.apache.ofbiz.manufacturing.rest.dto.CreateMktgPkgProductionRunResponse;
import org.apache.ofbiz.manufacturing.rest.dto.CreateProductionRunResponse;

/**
 * Utility methods for JSON serialization/deserialization and HTTP response
 * writing in the Manufacturing REST API.
 *
 * <p>Uses simple manual JSON handling to avoid adding external dependencies
 * beyond what OFBiz already provides.</p>
 */
public final class RestUtil {

    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final String ENCODING_UTF8 = "UTF-8";

    private RestUtil() { }

    /**
     * Reads the full request body as a string.
     */
    public static String readRequestBody(HttpServletRequest request) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }

    /**
     * Parses a JSON string into a simple {@code Map<String, String>}.
     * Handles string and numeric values; nested objects are not supported.
     */
    public static Map<String, String> parseJsonToMap(String json) {
        Map<String, String> result = new LinkedHashMap<>();
        if (json == null || json.trim().isEmpty()) {
            return result;
        }
        String trimmed = json.trim();
        if (trimmed.startsWith("{")) {
            trimmed = trimmed.substring(1);
        }
        if (trimmed.endsWith("}")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }

        List<String> pairs = splitJsonPairs(trimmed);
        for (String pair : pairs) {
            int colonIdx = pair.indexOf(':');
            if (colonIdx < 0) {
                continue;
            }
            String key = unquote(pair.substring(0, colonIdx).trim());
            String value = pair.substring(colonIdx + 1).trim();
            if ("null".equals(value)) {
                result.put(key, null);
            } else {
                result.put(key, unquote(value));
            }
        }
        return result;
    }

    /**
     * Splits top-level comma-separated key:value pairs in a JSON object body,
     * respecting quoted strings.
     */
    private static List<String> splitJsonPairs(String body) {
        List<String> pairs = new ArrayList<>();
        int depth = 0;
        boolean inQuote = false;
        boolean escaped = false;
        int start = 0;

        for (int i = 0; i < body.length(); i++) {
            char c = body.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (c == '\\') {
                escaped = true;
                continue;
            }
            if (c == '"') {
                inQuote = !inQuote;
                continue;
            }
            if (inQuote) {
                continue;
            }
            if (c == '{' || c == '[') {
                depth++;
            } else if (c == '}' || c == ']') {
                depth--;
            } else if (c == ',' && depth == 0) {
                pairs.add(body.substring(start, i));
                start = i + 1;
            }
        }
        if (start < body.length()) {
            pairs.add(body.substring(start));
        }
        return pairs;
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

    /**
     * Safely parses a BigDecimal from a string value, returning null if blank.
     */
    public static BigDecimal parseBigDecimal(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return new BigDecimal(value.trim());
    }

    /**
     * Sends a JSON success response for a production run creation.
     */
    public static void sendProductionRunResponse(HttpServletResponse response,
            CreateProductionRunResponse dto) throws IOException {
        response.setStatus(HttpServletResponse.SC_CREATED);
        response.setContentType(CONTENT_TYPE_JSON);
        response.setCharacterEncoding(ENCODING_UTF8);

        String json = "{\"productionRunId\":" + toJsonString(dto.getProductionRunId())
                + ",\"currentStatusId\":" + toJsonString(dto.getCurrentStatusId())
                + ",\"estimatedStartDate\":" + toJsonString(dto.getEstimatedStartDate())
                + "}";
        try (PrintWriter writer = response.getWriter()) {
            writer.write(json);
        }
    }

    /**
     * Sends a JSON success response for a marketing-package production run creation.
     */
    public static void sendMktgPkgResponse(HttpServletResponse response,
            CreateMktgPkgProductionRunResponse dto) throws IOException {
        response.setStatus(HttpServletResponse.SC_CREATED);
        response.setContentType(CONTENT_TYPE_JSON);
        response.setCharacterEncoding(ENCODING_UTF8);

        String json = "{\"productionRunId\":" + toJsonString(dto.getProductionRunId())
                + ",\"finishedProductId\":" + toJsonString(dto.getFinishedProductId())
                + "}";
        try (PrintWriter writer = response.getWriter()) {
            writer.write(json);
        }
    }

    /**
     * Sends a JSON error response with the given HTTP status code.
     */
    public static void sendError(HttpServletResponse response, int statusCode,
            ApiErrorResponse error) throws IOException {
        response.setStatus(statusCode);
        response.setContentType(CONTENT_TYPE_JSON);
        response.setCharacterEncoding(ENCODING_UTF8);

        String json = "{\"errorCode\":" + toJsonString(error.getErrorCode())
                + ",\"message\":" + toJsonString(error.getMessage())
                + ",\"details\":" + toJsonString(error.getDetails())
                + "}";
        try (PrintWriter writer = response.getWriter()) {
            writer.write(json);
        }
    }

    /**
     * Converts a Java string to a JSON string literal (with quotes), or {@code null}.
     */
    private static String toJsonString(String value) {
        if (value == null) {
            return "null";
        }
        return "\"" + escapeJson(value) + "\"";
    }

    /**
     * Escapes special characters for inclusion in a JSON string value.
     */
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

    /**
     * Validates that a required field is present; returns an error message or null if valid.
     */
    public static String validateRequired(String fieldName, String value) {
        if (value == null || value.trim().isEmpty()) {
            return "Field '" + fieldName + "' is required";
        }
        return null;
    }

    /**
     * Collects validation errors for required fields.
     */
    public static List<String> validateRequiredFields(Map<String, String> fields) {
        List<String> errors = new ArrayList<>();
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            String error = validateRequired(entry.getKey(), entry.getValue());
            if (error != null) {
                errors.add(error);
            }
        }
        return errors;
    }
}
