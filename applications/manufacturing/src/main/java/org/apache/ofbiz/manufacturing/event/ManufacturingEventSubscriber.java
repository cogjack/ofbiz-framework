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
package org.apache.ofbiz.manufacturing.event;

import java.util.HashMap;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.event.DomainEvent;
import org.apache.ofbiz.service.event.EventBus;
import org.apache.ofbiz.service.event.EventBusFactory;
import org.apache.ofbiz.service.event.types.ProductAssocDeletedEvent;
import org.apache.ofbiz.service.event.types.RequirementCreatedEvent;
import org.apache.ofbiz.service.event.types.RequirementUpdatedEvent;
import org.apache.ofbiz.service.event.types.ShipmentReceiptCreatedEvent;

/**
 * Subscribes to cross-domain events and invokes the appropriate manufacturing services.
 * This replaces the former cross-domain SECA triggers that directly coupled
 * order/product modules to manufacturing services.
 */
public final class ManufacturingEventSubscriber {

    private static final String MODULE = ManufacturingEventSubscriber.class.getName();

    private ManufacturingEventSubscriber() { }

    /**
     * Registers all manufacturing event subscriptions on the default event bus.
     * Should be called once during manufacturing module initialization.
     * @param dispatcher the local dispatcher for invoking manufacturing services
     */
    public static void registerSubscriptions(LocalDispatcher dispatcher) {
        EventBus eventBus = EventBusFactory.getEventBus();

        eventBus.subscribe(RequirementCreatedEvent.EVENT_TYPE,
                event -> handleRequirementCreated(dispatcher, event));
        eventBus.subscribe(RequirementUpdatedEvent.EVENT_TYPE,
                event -> handleRequirementUpdated(dispatcher, event));
        eventBus.subscribe(ProductAssocDeletedEvent.EVENT_TYPE,
                event -> handleProductAssocDeleted(dispatcher, event));
        eventBus.subscribe(ShipmentReceiptCreatedEvent.EVENT_TYPE,
                event -> handleShipmentReceiptCreated(dispatcher, event));

        Debug.logInfo("Manufacturing event subscriptions registered", MODULE);
    }

    private static void handleRequirementCreated(LocalDispatcher dispatcher, DomainEvent event) {
        Map<String, Object> payload = event.getPayload();
        Map<String, Object> context = new HashMap<>(payload);
        try {
            dispatcher.runSync("createProductionRunFromRequirement", context);
        } catch (GenericServiceException e) {
            Debug.logError(e, "Error invoking createProductionRunFromRequirement"
                    + " for requirementId=" + payload.get("requirementId"), MODULE);
        }
    }

    private static void handleRequirementUpdated(LocalDispatcher dispatcher, DomainEvent event) {
        Map<String, Object> payload = event.getPayload();
        Map<String, Object> context = new HashMap<>(payload);
        try {
            dispatcher.runSync("createProductionRunFromRequirement", context);
        } catch (GenericServiceException e) {
            Debug.logError(e, "Error invoking createProductionRunFromRequirement"
                    + " for requirementId=" + payload.get("requirementId"), MODULE);
        }
    }

    private static void handleProductAssocDeleted(LocalDispatcher dispatcher, DomainEvent event) {
        Map<String, Object> payload = event.getPayload();
        Map<String, Object> context = new HashMap<>(payload);
        try {
            dispatcher.runSync("updateLowLevelCode", context);
        } catch (GenericServiceException e) {
            Debug.logError(e, "Error invoking updateLowLevelCode"
                    + " for productId=" + payload.get("productId"), MODULE);
        }
    }

    private static void handleShipmentReceiptCreated(LocalDispatcher dispatcher, DomainEvent event) {
        Map<String, Object> payload = event.getPayload();
        Map<String, Object> context = new HashMap<>(payload);
        try {
            dispatcher.runSync("checkDecomposeInventoryItem", context);
        } catch (GenericServiceException e) {
            Debug.logError(e, "Error invoking checkDecomposeInventoryItem"
                    + " for shipmentId=" + payload.get("shipmentId"), MODULE);
        }
    }
}
