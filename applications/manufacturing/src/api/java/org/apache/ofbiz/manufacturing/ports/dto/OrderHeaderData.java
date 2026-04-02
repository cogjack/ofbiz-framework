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

import java.sql.Timestamp;

/**
 * Minimal projection DTO for OrderHeader entity data needed by Manufacturing.
 * Used for order lookups during production run creation and scheduling.
 */
public final class OrderHeaderData {

    private final String orderId;
    private final String orderTypeId;
    private final String orderName;
    private final String statusId;
    private final String productStoreId;
    private final Timestamp orderDate;
    private final String currencyUom;

    public OrderHeaderData(String orderId, String orderTypeId, String orderName,
            String statusId, String productStoreId, Timestamp orderDate, String currencyUom) {
        this.orderId = orderId;
        this.orderTypeId = orderTypeId;
        this.orderName = orderName;
        this.statusId = statusId;
        this.productStoreId = productStoreId;
        this.orderDate = orderDate;
        this.currencyUom = currencyUom;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getOrderTypeId() {
        return orderTypeId;
    }

    public String getOrderName() {
        return orderName;
    }

    public String getStatusId() {
        return statusId;
    }

    public String getProductStoreId() {
        return productStoreId;
    }

    public Timestamp getOrderDate() {
        return orderDate;
    }

    public String getCurrencyUom() {
        return currencyUom;
    }
}
