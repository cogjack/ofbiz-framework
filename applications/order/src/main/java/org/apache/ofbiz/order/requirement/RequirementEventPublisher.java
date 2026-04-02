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
package org.apache.ofbiz.order.requirement;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.service.event.EventBusFactory;
import org.apache.ofbiz.service.event.types.RequirementCreatedEvent;
import org.apache.ofbiz.service.event.types.RequirementUpdatedEvent;

/**
 * Service methods that publish domain events when requirements are created or updated.
 * These are wired as SECAs on {@code createRequirement} and {@code updateRequirement}
 * to replace the former cross-domain SECA triggers into the manufacturing module.
 */
public class RequirementEventPublisher {

    private static final String MODULE = RequirementEventPublisher.class.getName();

    /**
     * Publishes a {@link RequirementCreatedEvent} after a requirement is successfully created.
     */
    public static Map<String, Object> publishRequirementCreated(DispatchContext dctx, Map<String, Object> context) {
        String requirementId = (String) context.get("requirementId");
        String requirementTypeId = (String) context.get("requirementTypeId");
        String facilityId = (String) context.get("facilityId");
        String productId = (String) context.get("productId");
        BigDecimal quantity = (BigDecimal) context.get("quantity");
        Timestamp requiredByDate = (Timestamp) context.get("requiredByDate");

        RequirementCreatedEvent event = new RequirementCreatedEvent(
                requirementId, requirementTypeId, facilityId, productId, quantity, requiredByDate);
        event.setDispatchContext(dctx);
        EventBusFactory.getEventBus().publish(event);

        Debug.logInfo("Published RequirementCreatedEvent for requirementId=" + requirementId, MODULE);
        return ServiceUtil.returnSuccess();
    }

    /**
     * Publishes a {@link RequirementUpdatedEvent} after a requirement is successfully updated.
     */
    public static Map<String, Object> publishRequirementUpdated(DispatchContext dctx, Map<String, Object> context) {
        String requirementId = (String) context.get("requirementId");
        String statusId = (String) context.get("statusId");
        BigDecimal quantity = (BigDecimal) context.get("quantity");

        RequirementUpdatedEvent event = new RequirementUpdatedEvent(requirementId, statusId, quantity);
        event.setDispatchContext(dctx);
        EventBusFactory.getEventBus().publish(event);

        Debug.logInfo("Published RequirementUpdatedEvent for requirementId=" + requirementId, MODULE);
        return ServiceUtil.returnSuccess();
    }
}
