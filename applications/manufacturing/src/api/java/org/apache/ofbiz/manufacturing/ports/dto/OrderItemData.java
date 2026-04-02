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
package org.apache.ofbiz.manufacturing.ports.dto;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Minimal projection DTO for OrderItem entity data needed by Manufacturing.
 * Used for order item lookups during production run scheduling and fulfillment.
 */
public final class OrderItemData {

    private final String orderId;
    private final String orderItemSeqId;
    private final String orderItemTypeId;
    private final String productId;
    private final String statusId;
    private final BigDecimal quantity;
    private final BigDecimal unitPrice;
    private final String itemDescription;
    private final Timestamp estimatedDeliveryDate;
    private final Timestamp shipBeforeDate;
    private final Timestamp shipAfterDate;

    public OrderItemData(String orderId, String orderItemSeqId, String orderItemTypeId,
            String productId, String statusId, BigDecimal quantity, BigDecimal unitPrice,
            String itemDescription, Timestamp estimatedDeliveryDate,
            Timestamp shipBeforeDate, Timestamp shipAfterDate) {
        this.orderId = orderId;
        this.orderItemSeqId = orderItemSeqId;
        this.orderItemTypeId = orderItemTypeId;
        this.productId = productId;
        this.statusId = statusId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.itemDescription = itemDescription;
        this.estimatedDeliveryDate = estimatedDeliveryDate;
        this.shipBeforeDate = shipBeforeDate;
        this.shipAfterDate = shipAfterDate;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getOrderItemSeqId() {
        return orderItemSeqId;
    }

    public String getOrderItemTypeId() {
        return orderItemTypeId;
    }

    public String getProductId() {
        return productId;
    }

    public String getStatusId() {
        return statusId;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public String getItemDescription() {
        return itemDescription;
    }

    public Timestamp getEstimatedDeliveryDate() {
        return estimatedDeliveryDate;
    }

    public Timestamp getShipBeforeDate() {
        return shipBeforeDate;
    }

    public Timestamp getShipAfterDate() {
        return shipAfterDate;
    }
}
