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
package org.apache.ofbiz.service.event.types;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import org.apache.ofbiz.service.event.DomainEvent;

/**
 * Domain event published when a new requirement is created.
 */
public class RequirementCreatedEvent extends DomainEvent {

    public static final String EVENT_TYPE = "RequirementCreated";

    public RequirementCreatedEvent(String requirementId, String requirementTypeId,
            String facilityId, String productId, BigDecimal quantity, Timestamp requiredByDate) {
        super(EVENT_TYPE, "order", buildPayload(requirementId, requirementTypeId,
                facilityId, productId, quantity, requiredByDate));
    }

    private static Map<String, Object> buildPayload(String requirementId, String requirementTypeId,
            String facilityId, String productId, BigDecimal quantity, Timestamp requiredByDate) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("requirementId", requirementId);
        payload.put("requirementTypeId", requirementTypeId);
        payload.put("facilityId", facilityId);
        payload.put("productId", productId);
        payload.put("quantity", quantity);
        payload.put("requiredByDate", requiredByDate);
        return payload;
    }
}
