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
package org.apache.ofbiz.manufacturing.ports.impl;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.manufacturing.ports.WorkEffortPort;
import org.apache.ofbiz.manufacturing.ports.dto.WorkEffortCreatedResult;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

/**
 * Dispatcher-backed implementation of {@link WorkEffortPort}.
 * Wraps {@code LocalDispatcher.runSync()} calls for all WorkEffort service invocations
 * originating from the Manufacturing module.
 */
public final class DispatcherWorkEffortPort implements WorkEffortPort {

    private final LocalDispatcher dispatcher;
    private final GenericValue userLogin;

    public DispatcherWorkEffortPort(LocalDispatcher dispatcher, GenericValue userLogin) {
        this.dispatcher = dispatcher;
        this.userLogin = userLogin;
    }

    @Override
    public WorkEffortCreatedResult createWorkEffort(String workEffortTypeId, String workEffortPurposeTypeId,
            String currentStatusId, String workEffortName, String description, String facilityId,
            Timestamp estimatedStartDate, Timestamp estimatedCompletionDate, BigDecimal quantityToProduce,
            String workEffortParentId, String fixedAssetId, BigDecimal reservPersons, Long priority,
            Double estimatedSetupMillis, Double estimatedMilliSeconds) {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("workEffortTypeId", workEffortTypeId);
        ctx.put("workEffortPurposeTypeId", workEffortPurposeTypeId);
        ctx.put("currentStatusId", currentStatusId);
        ctx.put("workEffortName", workEffortName);
        ctx.put("description", description);
        ctx.put("facilityId", facilityId);
        ctx.put("estimatedStartDate", estimatedStartDate);
        ctx.put("estimatedCompletionDate", estimatedCompletionDate);
        ctx.put("quantityToProduce", quantityToProduce);
        ctx.put("workEffortParentId", workEffortParentId);
        ctx.put("fixedAssetId", fixedAssetId);
        ctx.put("reservPersons", reservPersons);
        ctx.put("priority", priority);
        ctx.put("estimatedSetupMillis", estimatedSetupMillis);
        ctx.put("estimatedMilliSeconds", estimatedMilliSeconds);
        ctx.put("userLogin", userLogin);
        try {
            Map<String, Object> result = dispatcher.runSync("createWorkEffort", ctx);
            if (ServiceUtil.isError(result)) {
                throw new RuntimeException("createWorkEffort failed: " + ServiceUtil.getErrorMessage(result));
            }
            return new WorkEffortCreatedResult((String) result.get("workEffortId"));
        } catch (GenericServiceException e) {
            throw new RuntimeException("Error calling createWorkEffort", e);
        }
    }

    @Override
    public void updateWorkEffort(String workEffortId, String currentStatusId, Double actualMilliSeconds,
            Double actualSetupMillis, BigDecimal quantityProduced, BigDecimal quantityRejected,
            Timestamp estimatedStartDate, Timestamp estimatedCompletionDate,
            Timestamp actualStartDate, Timestamp actualCompletionDate) {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("workEffortId", workEffortId);
        ctx.put("userLogin", userLogin);
        if (currentStatusId != null) {
            ctx.put("currentStatusId", currentStatusId);
        }
        if (actualMilliSeconds != null) {
            ctx.put("actualMilliSeconds", actualMilliSeconds);
        }
        if (actualSetupMillis != null) {
            ctx.put("actualSetupMillis", actualSetupMillis);
        }
        if (quantityProduced != null) {
            ctx.put("quantityProduced", quantityProduced);
        }
        if (quantityRejected != null) {
            ctx.put("quantityRejected", quantityRejected);
        }
        if (estimatedStartDate != null) {
            ctx.put("estimatedStartDate", estimatedStartDate);
        }
        if (estimatedCompletionDate != null) {
            ctx.put("estimatedCompletionDate", estimatedCompletionDate);
        }
        if (actualStartDate != null) {
            ctx.put("actualStartDate", actualStartDate);
        }
        if (actualCompletionDate != null) {
            ctx.put("actualCompletionDate", actualCompletionDate);
        }
        try {
            Map<String, Object> result = dispatcher.runSync("updateWorkEffort", ctx);
            if (ServiceUtil.isError(result)) {
                throw new RuntimeException("updateWorkEffort failed: " + ServiceUtil.getErrorMessage(result));
            }
        } catch (GenericServiceException e) {
            throw new RuntimeException("Error calling updateWorkEffort", e);
        }
    }

    @Override
    public void createWorkEffortAssoc(String workEffortIdFrom, String workEffortIdTo,
            String workEffortAssocTypeId) {
        Map<String, Object> ctx = UtilMisc.toMap(
                "workEffortIdFrom", workEffortIdFrom,
                "workEffortIdTo", workEffortIdTo,
                "workEffortAssocTypeId", workEffortAssocTypeId,
                "userLogin", userLogin);
        try {
            Map<String, Object> result = dispatcher.runSync("createWorkEffortAssoc", ctx);
            if (ServiceUtil.isError(result)) {
                throw new RuntimeException("createWorkEffortAssoc failed: " + ServiceUtil.getErrorMessage(result));
            }
        } catch (GenericServiceException e) {
            throw new RuntimeException("Error calling createWorkEffortAssoc", e);
        }
    }

    @Override
    public void createWorkEffortGoodStandard(String workEffortId, String productId,
            String workEffortGoodStdTypeId, String statusId, BigDecimal estimatedQuantity, Timestamp fromDate) {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("workEffortId", workEffortId);
        ctx.put("productId", productId);
        ctx.put("workEffortGoodStdTypeId", workEffortGoodStdTypeId);
        ctx.put("statusId", statusId);
        ctx.put("estimatedQuantity", estimatedQuantity);
        ctx.put("fromDate", fromDate);
        ctx.put("userLogin", userLogin);
        try {
            Map<String, Object> result = dispatcher.runSync("createWorkEffortGoodStandard", ctx);
            if (ServiceUtil.isError(result)) {
                throw new RuntimeException("createWorkEffortGoodStandard failed: " + ServiceUtil.getErrorMessage(result));
            }
        } catch (GenericServiceException e) {
            throw new RuntimeException("Error calling createWorkEffortGoodStandard", e);
        }
    }

    @Override
    public void updateWorkEffortGoodStandard(String workEffortId, String productId,
            String workEffortGoodStdTypeId, Timestamp fromDate, BigDecimal estimatedQuantity) {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("workEffortId", workEffortId);
        ctx.put("productId", productId);
        ctx.put("workEffortGoodStdTypeId", workEffortGoodStdTypeId);
        ctx.put("fromDate", fromDate);
        ctx.put("userLogin", userLogin);
        if (estimatedQuantity != null) {
            ctx.put("estimatedQuantity", estimatedQuantity);
        }
        try {
            Map<String, Object> result = dispatcher.runSync("updateWorkEffortGoodStandard", ctx);
            if (ServiceUtil.isError(result)) {
                throw new RuntimeException("updateWorkEffortGoodStandard failed: " + ServiceUtil.getErrorMessage(result));
            }
        } catch (GenericServiceException e) {
            throw new RuntimeException("Error calling updateWorkEffortGoodStandard", e);
        }
    }

    @Override
    public void assignPartyToWorkEffort(String workEffortId, String partyId, String roleTypeId,
            Timestamp fromDate, String statusId) {
        Map<String, Object> ctx = UtilMisc.<String, Object>toMap(
                "workEffortId", workEffortId,
                "partyId", partyId,
                "roleTypeId", roleTypeId,
                "fromDate", fromDate,
                "userLogin", userLogin);
        if (statusId != null) {
            ctx.put("statusId", statusId);
        }
        try {
            Map<String, Object> result = dispatcher.runSync("assignPartyToWorkEffort", ctx);
            if (ServiceUtil.isError(result)) {
                throw new RuntimeException("assignPartyToWorkEffort failed: " + ServiceUtil.getErrorMessage(result));
            }
        } catch (GenericServiceException e) {
            throw new RuntimeException("Error calling assignPartyToWorkEffort", e);
        }
    }

    @Override
    public void createWorkEffortInventoryProduced(String workEffortId, String inventoryItemId) {
        Map<String, Object> ctx = UtilMisc.toMap(
                "workEffortId", workEffortId,
                "inventoryItemId", inventoryItemId,
                "userLogin", userLogin);
        try {
            Map<String, Object> result = dispatcher.runSync("createWorkEffortInventoryProduced", ctx);
            if (ServiceUtil.isError(result)) {
                throw new RuntimeException("createWorkEffortInventoryProduced failed: " + ServiceUtil.getErrorMessage(result));
            }
        } catch (GenericServiceException e) {
            throw new RuntimeException("Error calling createWorkEffortInventoryProduced", e);
        }
    }

    @Override
    public void createTimeEntry(String workEffortId, String partyId, String rateTypeId,
            double hours, String comments) {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("workEffortId", workEffortId);
        ctx.put("partyId", partyId);
        ctx.put("rateTypeId", rateTypeId);
        ctx.put("hours", hours);
        ctx.put("comments", comments);
        ctx.put("userLogin", userLogin);
        try {
            Map<String, Object> result = dispatcher.runSync("createTimeEntry", ctx);
            if (ServiceUtil.isError(result)) {
                throw new RuntimeException("createTimeEntry failed: " + ServiceUtil.getErrorMessage(result));
            }
        } catch (GenericServiceException e) {
            throw new RuntimeException("Error calling createTimeEntry", e);
        }
    }

    @Override
    public void createWorkEffortNote(String workEffortId, String noteInfo, String noteName,
            String noteParty, String internalNote) {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("workEffortId", workEffortId);
        ctx.put("noteInfo", noteInfo);
        ctx.put("noteName", noteName);
        ctx.put("noteParty", noteParty);
        ctx.put("internalNote", internalNote);
        ctx.put("userLogin", userLogin);
        try {
            Map<String, Object> result = dispatcher.runSync("createWorkEffortNote", ctx);
            if (ServiceUtil.isError(result)) {
                throw new RuntimeException("createWorkEffortNote failed: " + ServiceUtil.getErrorMessage(result));
            }
        } catch (GenericServiceException e) {
            throw new RuntimeException("Error calling createWorkEffortNote", e);
        }
    }
}
