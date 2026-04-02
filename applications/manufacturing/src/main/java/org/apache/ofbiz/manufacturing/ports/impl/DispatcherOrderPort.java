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
import org.apache.ofbiz.manufacturing.ports.OrderPort;
import org.apache.ofbiz.manufacturing.ports.dto.RequirementCreatedResult;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

/**
 * Dispatcher-backed implementation of {@link OrderPort}.
 * Wraps {@code LocalDispatcher.runSync()} calls for all Order/Requirement service invocations
 * originating from the Manufacturing module.
 */
public final class DispatcherOrderPort implements OrderPort {

    private final LocalDispatcher dispatcher;
    private final GenericValue userLogin;

    public DispatcherOrderPort(LocalDispatcher dispatcher, GenericValue userLogin) {
        this.dispatcher = dispatcher;
        this.userLogin = userLogin;
    }

    @Override
    public RequirementCreatedResult createRequirement(String productId, String requirementTypeId,
            String facilityId, String statusId, Timestamp requiredByDate, Timestamp requirementStartDate,
            BigDecimal quantity, String description) {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("productId", productId);
        ctx.put("requirementTypeId", requirementTypeId);
        ctx.put("facilityId", facilityId);
        ctx.put("statusId", statusId);
        ctx.put("requiredByDate", requiredByDate);
        ctx.put("requirementStartDate", requirementStartDate);
        ctx.put("quantity", quantity);
        ctx.put("description", description);
        ctx.put("userLogin", userLogin);
        try {
            Map<String, Object> result = dispatcher.runSync("createRequirement", ctx);
            if (ServiceUtil.isError(result)) {
                throw new RuntimeException("createRequirement failed: " + ServiceUtil.getErrorMessage(result));
            }
            return new RequirementCreatedResult((String) result.get("requirementId"));
        } catch (GenericServiceException e) {
            throw new RuntimeException("Error calling createRequirement", e);
        }
    }

    @Override
    public void updateRequirement(String requirementId, String statusId, String requirementTypeId) {
        Map<String, Object> ctx = UtilMisc.<String, Object>toMap(
                "requirementId", requirementId,
                "statusId", statusId,
                "requirementTypeId", requirementTypeId,
                "userLogin", userLogin);
        try {
            Map<String, Object> result = dispatcher.runSync("updateRequirement", ctx);
            if (ServiceUtil.isError(result)) {
                throw new RuntimeException("updateRequirement failed: " + ServiceUtil.getErrorMessage(result));
            }
        } catch (GenericServiceException e) {
            throw new RuntimeException("Error calling updateRequirement", e);
        }
    }
}
