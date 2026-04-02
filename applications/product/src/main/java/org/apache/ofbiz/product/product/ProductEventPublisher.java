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
package org.apache.ofbiz.product.product;

import java.math.BigDecimal;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.service.event.EventBusFactory;
import org.apache.ofbiz.service.event.types.ProductAssocDeletedEvent;
import org.apache.ofbiz.service.event.types.ShipmentReceiptCreatedEvent;

/**
 * Service methods that publish domain events for product-domain operations.
 * These are wired as SECAs on {@code deleteProductAssoc} and {@code createShipmentReceipt}
 * to replace the former cross-domain SECA triggers into the manufacturing module.
 */
public class ProductEventPublisher {

    private static final String MODULE = ProductEventPublisher.class.getName();

    /**
     * Publishes a {@link ProductAssocDeletedEvent} after a product association is deleted.
     */
    public static Map<String, Object> publishProductAssocDeleted(DispatchContext dctx, Map<String, Object> context) {
        String productId = (String) context.get("productId");
        String productIdTo = (String) context.get("productIdTo");
        String productAssocTypeId = (String) context.get("productAssocTypeId");

        ProductAssocDeletedEvent event = new ProductAssocDeletedEvent(productId, productIdTo, productAssocTypeId);
        EventBusFactory.getEventBus().publish(event);

        Debug.logInfo("Published ProductAssocDeletedEvent for productId=" + productId, MODULE);
        return ServiceUtil.returnSuccess();
    }

    /**
     * Publishes a {@link ShipmentReceiptCreatedEvent} after a shipment receipt is created.
     */
    public static Map<String, Object> publishShipmentReceiptCreated(DispatchContext dctx, Map<String, Object> context) {
        String shipmentId = (String) context.get("shipmentId");
        String receiptId = (String) context.get("receiptId");
        String productId = (String) context.get("productId");
        BigDecimal quantityAccepted = (BigDecimal) context.get("quantityAccepted");
        String inventoryItemId = (String) context.get("inventoryItemId");

        ShipmentReceiptCreatedEvent event = new ShipmentReceiptCreatedEvent(
                shipmentId, receiptId, productId, quantityAccepted, inventoryItemId);
        EventBusFactory.getEventBus().publish(event);

        Debug.logInfo("Published ShipmentReceiptCreatedEvent for shipmentId=" + shipmentId, MODULE);
        return ServiceUtil.returnSuccess();
    }
}
