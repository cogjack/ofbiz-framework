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

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.manufacturing.rest.dto.ApiErrorResponse;
import org.apache.ofbiz.manufacturing.rest.dto.CreateMktgPkgProductionRunResponse;
import org.apache.ofbiz.manufacturing.rest.dto.CreateProductionRunResponse;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

/**
 * REST controller handling production run creation endpoints.
 *
 * <p>Provides three endpoints that delegate to existing OFBiz service implementations:
 * <ul>
 *   <li>{@code POST /production-runs} — creates a standard production run</li>
 *   <li>{@code POST /production-runs/from-configuration} — creates a production run from a product configuration</li>
 *   <li>{@code POST /production-runs/marketing-package} — creates a production run for a marketing package</li>
 * </ul>
 *
 * <p>Authentication is enforced by requiring a valid {@code userLogin} in the HTTP session,
 * matching the same permission model as the underlying dispatcher services.</p>
 */
public final class ProductionRunController {

    private static final String MODULE = ProductionRunController.class.getName();

    private ProductionRunController() { }

    /**
     * Handles {@code POST /api/v1/manufacturing/production-runs}.
     * Delegates to the {@code createProductionRun} service.
     */
    public static void handleCreateProductionRun(HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        GenericValue userLogin = getAuthenticatedUser(request, response);
        if (userLogin == null) {
            return;
        }

        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        if (dispatcher == null) {
            sendServiceUnavailable(response);
            return;
        }

        String body = RestUtil.readRequestBody(request);
        Map<String, String> fields = RestUtil.parseJsonToMap(body);

        Map<String, String> requiredChecks = new LinkedHashMap<>();
        requiredChecks.put("productId", fields.get("productId"));
        requiredChecks.put("quantity", fields.get("quantity"));
        requiredChecks.put("startDate", fields.get("startDate"));
        requiredChecks.put("facilityId", fields.get("facilityId"));
        List<String> errors = RestUtil.validateRequiredFields(requiredChecks);
        if (!errors.isEmpty()) {
            RestUtil.sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                    new ApiErrorResponse("VALIDATION_ERROR",
                            "Missing required fields", String.join("; ", errors)));
            return;
        }

        BigDecimal quantity;
        try {
            quantity = RestUtil.parseBigDecimal(fields.get("quantity"));
        } catch (NumberFormatException e) {
            RestUtil.sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                    new ApiErrorResponse("VALIDATION_ERROR",
                            "Invalid numeric value for 'quantity'", e.getMessage()));
            return;
        }

        Timestamp startDate;
        try {
            startDate = Timestamp.valueOf(fields.get("startDate"));
        } catch (IllegalArgumentException e) {
            RestUtil.sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                    new ApiErrorResponse("VALIDATION_ERROR",
                            "Invalid date format for 'startDate'; expected yyyy-MM-dd HH:mm:ss",
                            e.getMessage()));
            return;
        }

        Map<String, Object> serviceCtx = UtilMisc.toMap(
                "productId", fields.get("productId"),
                "pRQuantity", quantity,
                "startDate", startDate,
                "facilityId", fields.get("facilityId"),
                "userLogin", userLogin);
        if (fields.get("routingId") != null) {
            serviceCtx.put("routingId", fields.get("routingId"));
        }
        if (fields.get("workEffortName") != null) {
            serviceCtx.put("workEffortName", fields.get("workEffortName"));
        }
        if (fields.get("description") != null) {
            serviceCtx.put("description", fields.get("description"));
        }

        try {
            Map<String, Object> result = dispatcher.runSync("createProductionRun", serviceCtx);
            if (ServiceUtil.isError(result)) {
                String errorMsg = ServiceUtil.getErrorMessage(result);
                Debug.logError("createProductionRun service error: " + errorMsg, MODULE);
                RestUtil.sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        new ApiErrorResponse("SERVICE_ERROR", errorMsg, null));
                return;
            }
            String productionRunId = (String) result.get("productionRunId");
            Timestamp estCompletion = (Timestamp) result.get("estimatedCompletionDate");
            RestUtil.sendProductionRunResponse(response,
                    new CreateProductionRunResponse(
                            productionRunId,
                            "PRUN_CREATED",
                            estCompletion != null ? estCompletion.toString() : null));
        } catch (GenericServiceException e) {
            Debug.logError(e, "Error calling createProductionRun", MODULE);
            RestUtil.sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    new ApiErrorResponse("SERVICE_ERROR",
                            "Failed to create production run", e.getMessage()));
        }
    }

    /**
     * Handles {@code POST /api/v1/manufacturing/production-runs/from-configuration}.
     * Delegates to the {@code createProductionRunFromConfiguration} service.
     */
    public static void handleCreateProductionRunFromConfig(HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        GenericValue userLogin = getAuthenticatedUser(request, response);
        if (userLogin == null) {
            return;
        }

        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        if (dispatcher == null) {
            sendServiceUnavailable(response);
            return;
        }

        String body = RestUtil.readRequestBody(request);
        Map<String, String> fields = RestUtil.parseJsonToMap(body);

        Map<String, String> requiredChecks = new LinkedHashMap<>();
        requiredChecks.put("facilityId", fields.get("facilityId"));
        List<String> errors = RestUtil.validateRequiredFields(requiredChecks);
        if (!errors.isEmpty()) {
            RestUtil.sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                    new ApiErrorResponse("VALIDATION_ERROR",
                            "Missing required fields", String.join("; ", errors)));
            return;
        }

        if (fields.get("configId") == null && fields.get("orderId") == null) {
            RestUtil.sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                    new ApiErrorResponse("VALIDATION_ERROR",
                            "Either 'configId' or 'orderId' must be provided", null));
            return;
        }

        Map<String, Object> serviceCtx = UtilMisc.toMap(
                "facilityId", fields.get("facilityId"),
                "userLogin", userLogin);
        if (fields.get("configId") != null) {
            serviceCtx.put("configId", fields.get("configId"));
        }
        if (fields.get("quantity") != null) {
            try {
                serviceCtx.put("quantity", RestUtil.parseBigDecimal(fields.get("quantity")));
            } catch (NumberFormatException e) {
                RestUtil.sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                        new ApiErrorResponse("VALIDATION_ERROR",
                                "Invalid numeric value for 'quantity'", e.getMessage()));
                return;
            }
        }
        if (fields.get("orderId") != null) {
            serviceCtx.put("orderId", fields.get("orderId"));
        }
        if (fields.get("orderItemSeqId") != null) {
            serviceCtx.put("orderItemSeqId", fields.get("orderItemSeqId"));
        }

        try {
            Map<String, Object> result = dispatcher.runSync(
                    "createProductionRunFromConfiguration", serviceCtx);
            if (ServiceUtil.isError(result)) {
                String errorMsg = ServiceUtil.getErrorMessage(result);
                Debug.logError("createProductionRunFromConfiguration error: " + errorMsg, MODULE);
                RestUtil.sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        new ApiErrorResponse("SERVICE_ERROR", errorMsg, null));
                return;
            }
            String productionRunId = (String) result.get("productionRunId");
            RestUtil.sendProductionRunResponse(response,
                    new CreateProductionRunResponse(productionRunId, "PRUN_CREATED", null));
        } catch (GenericServiceException e) {
            Debug.logError(e, "Error calling createProductionRunFromConfiguration", MODULE);
            RestUtil.sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    new ApiErrorResponse("SERVICE_ERROR",
                            "Failed to create production run from configuration",
                            e.getMessage()));
        }
    }

    /**
     * Handles {@code POST /api/v1/manufacturing/production-runs/marketing-package}.
     * Delegates to the {@code createProductionRunForMktgPkg} service.
     */
    public static void handleCreateMktgPkgProductionRun(HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        GenericValue userLogin = getAuthenticatedUser(request, response);
        if (userLogin == null) {
            return;
        }

        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        if (dispatcher == null) {
            sendServiceUnavailable(response);
            return;
        }

        String body = RestUtil.readRequestBody(request);
        Map<String, String> fields = RestUtil.parseJsonToMap(body);

        Map<String, String> requiredChecks = new LinkedHashMap<>();
        requiredChecks.put("orderId", fields.get("orderId"));
        requiredChecks.put("orderItemSeqId", fields.get("orderItemSeqId"));
        requiredChecks.put("facilityId", fields.get("facilityId"));
        List<String> errors = RestUtil.validateRequiredFields(requiredChecks);
        if (!errors.isEmpty()) {
            RestUtil.sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                    new ApiErrorResponse("VALIDATION_ERROR",
                            "Missing required fields", String.join("; ", errors)));
            return;
        }

        Map<String, Object> serviceCtx = UtilMisc.toMap(
                "orderId", fields.get("orderId"),
                "orderItemSeqId", fields.get("orderItemSeqId"),
                "facilityId", fields.get("facilityId"),
                "userLogin", userLogin);

        try {
            Map<String, Object> result = dispatcher.runSync(
                    "createProductionRunForMktgPkg", serviceCtx);
            if (ServiceUtil.isError(result)) {
                String errorMsg = ServiceUtil.getErrorMessage(result);
                Debug.logError("createProductionRunForMktgPkg error: " + errorMsg, MODULE);
                RestUtil.sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        new ApiErrorResponse("SERVICE_ERROR", errorMsg, null));
                return;
            }
            String productionRunId = (String) result.get("productionRunId");
            RestUtil.sendMktgPkgResponse(response,
                    new CreateMktgPkgProductionRunResponse(productionRunId, null));
        } catch (GenericServiceException e) {
            Debug.logError(e, "Error calling createProductionRunForMktgPkg", MODULE);
            RestUtil.sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    new ApiErrorResponse("SERVICE_ERROR",
                            "Failed to create marketing package production run",
                            e.getMessage()));
        }
    }

    /**
     * Retrieves the authenticated user from the session. Sends a 401 error
     * response if no valid session/user is found.
     */
    private static GenericValue getAuthenticatedUser(HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        GenericValue userLogin = session != null
                ? (GenericValue) session.getAttribute("userLogin") : null;
        if (userLogin == null) {
            RestUtil.sendError(response, HttpServletResponse.SC_UNAUTHORIZED,
                    new ApiErrorResponse("UNAUTHORIZED",
                            "Authentication required; no valid session found", null));
            return null;
        }
        return userLogin;
    }

    private static void sendServiceUnavailable(HttpServletResponse response) throws IOException {
        RestUtil.sendError(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                new ApiErrorResponse("SERVICE_UNAVAILABLE",
                        "Service dispatcher is not available", null));
    }
}
