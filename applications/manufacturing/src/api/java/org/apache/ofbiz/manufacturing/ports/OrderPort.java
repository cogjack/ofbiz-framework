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
package org.apache.ofbiz.manufacturing.ports;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

import org.apache.ofbiz.manufacturing.ports.dto.OrderHeaderData;
import org.apache.ofbiz.manufacturing.ports.dto.OrderItemData;
import org.apache.ofbiz.manufacturing.ports.dto.RequirementCreatedResult;

/**
 * Port interface abstracting all outbound dispatcher calls from Manufacturing to the Order module.
 * Covers 2 dispatcher calls across 2 method signatures originating from
 * {@code ProposedOrder.java} and {@code ProductionRunServices.java}.
 *
 * <p>In Phase 1B, this interface will be backed by a dispatcher-based implementation.
 * In later phases, it may be replaced by domain events (RequirementNeeded / RequirementUpdated).</p>
 */
public interface OrderPort {

    /**
     * Creates a new requirement (e.g. purchase requirement or internal manufacturing requirement).
     * <p>Abstracts the {@code createRequirement} service.
     * Called from {@code ProposedOrder.create} during MRP processing.</p>
     *
     * @param productId            the product needed
     * @param requirementTypeId    type (e.g. INTERNAL_REQUIREMENT, PRODUCT_REQUIREMENT)
     * @param facilityId           the facility for the requirement
     * @param statusId             initial status (e.g. REQ_PROPOSED)
     * @param requiredByDate       when the requirement must be fulfilled
     * @param requirementStartDate when fulfillment should start
     * @param quantity             quantity required
     * @param description          descriptive text (e.g. MRP-generated description)
     * @return result containing the generated requirementId
     */
    RequirementCreatedResult createRequirement(String productId, String requirementTypeId, String facilityId,
            String statusId, Timestamp requiredByDate, Timestamp requirementStartDate,
            BigDecimal quantity, String description);

    /**
     * Updates an existing requirement's status or other mutable fields.
     * <p>Abstracts the {@code updateRequirement} service.
     * Called from {@code ProductionRunServices.approveRequirement}.</p>
     *
     * @param requirementId     the requirement to update
     * @param statusId          new status (e.g. REQ_APPROVED)
     * @param requirementTypeId the requirement type (required by the service contract)
     */
    void updateRequirement(String requirementId, String statusId, String requirementTypeId);

    // ========================================================================
    // Phase 1G: Cross-domain entity read methods for schema separation
    // ========================================================================

    /**
     * Retrieves an order header by its primary key.
     * <p>Replaces direct {@code EntityQuery.use(delegator).from("OrderHeader")} calls
     * in manufacturing code (25 total order-domain accesses).</p>
     *
     * @param orderId the order ID to look up
     * @return order header data projection, or null if not found
     */
    OrderHeaderData getOrderHeader(String orderId);

    /**
     * Retrieves all order items for a given order.
     * <p>Replaces direct {@code EntityQuery.use(delegator).from("OrderItem")} calls
     * in manufacturing code for production run scheduling and fulfillment.</p>
     *
     * @param orderId the order ID
     * @return list of order items for the given order
     */
    List<OrderItemData> getOrderItems(String orderId);
}
