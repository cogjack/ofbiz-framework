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
import java.util.HashMap;
import java.util.Map;

import org.apache.ofbiz.service.event.DomainEvent;

/**
 * Domain event published when a shipment receipt is created.
 */
public class ShipmentReceiptCreatedEvent extends DomainEvent {

    public static final String EVENT_TYPE = "ShipmentReceiptCreated";

    public ShipmentReceiptCreatedEvent(String shipmentId, String receiptId,
            String productId, BigDecimal quantityAccepted, String inventoryItemId) {
        super(EVENT_TYPE, "product", buildPayload(shipmentId, receiptId,
                productId, quantityAccepted, inventoryItemId));
    }

    private static Map<String, Object> buildPayload(String shipmentId, String receiptId,
            String productId, BigDecimal quantityAccepted, String inventoryItemId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("shipmentId", shipmentId);
        payload.put("receiptId", receiptId);
        payload.put("productId", productId);
        payload.put("quantityAccepted", quantityAccepted);
        payload.put("inventoryItemId", inventoryItemId);
        return payload;
    }
}
