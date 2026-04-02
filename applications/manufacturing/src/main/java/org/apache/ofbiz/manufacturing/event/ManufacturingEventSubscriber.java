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
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.service.event.DomainEvent;
import org.apache.ofbiz.service.event.EventBus;
import org.apache.ofbiz.service.event.EventSubscriberModule;
import org.apache.ofbiz.service.event.types.ProductAssocDeletedEvent;
import org.apache.ofbiz.service.event.types.RequirementCreatedEvent;
import org.apache.ofbiz.service.event.types.RequirementUpdatedEvent;
import org.apache.ofbiz.service.event.types.ShipmentReceiptCreatedEvent;

/**
 * Subscribes to cross-domain events and invokes the appropriate manufacturing services.
 * This replaces the former cross-domain SECA triggers that directly coupled
 * order/product modules to manufacturing services.
 *
 * <p>Discovered automatically via {@link java.util.ServiceLoader} when the
 * {@link org.apache.ofbiz.service.event.EventBusFactory} is first used.
 * Event handlers obtain the {@link LocalDispatcher} from the event's
 * {@link DispatchContext} (set by the publishing service during in-process delivery).</p>
 */
public class ManufacturingEventSubscriber implements EventSubscriberModule {

    private static final String MODULE = ManufacturingEventSubscriber.class.getName();
    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);

    @Override
    public void registerSubscriptions(EventBus eventBus) {
        if (!REGISTERED.compareAndSet(false, true)) {
            return;
        }
        eventBus.subscribe(RequirementCreatedEvent.EVENT_TYPE,
                ManufacturingEventSubscriber::handleRequirementCreated);
        eventBus.subscribe(RequirementUpdatedEvent.EVENT_TYPE,
                ManufacturingEventSubscriber::handleRequirementUpdated);
        eventBus.subscribe(ProductAssocDeletedEvent.EVENT_TYPE,
                ManufacturingEventSubscriber::handleProductAssocDeleted);
        eventBus.subscribe(ShipmentReceiptCreatedEvent.EVENT_TYPE,
                ManufacturingEventSubscriber::handleShipmentReceiptCreated);

        Debug.logInfo("Manufacturing event subscriptions registered", MODULE);
    }

    /**
     * Service method that can be invoked to initialize event subscriptions.
     * Provides an explicit hook for triggering subscription registration
     * if ServiceLoader discovery is not sufficient.
     */
    public static Map<String, Object> initEventSubscriptions(DispatchContext dctx,
            Map<String, Object> context) {
        new ManufacturingEventSubscriber().registerSubscriptions(
                org.apache.ofbiz.service.event.EventBusFactory.getEventBus());
        return ServiceUtil.returnSuccess();
    }

    private static void handleRequirementCreated(DomainEvent event) {
        LocalDispatcher dispatcher = getDispatcher(event);
        if (dispatcher == null) {
            return;
        }
        Map<String, Object> payload = event.getPayload();
        Map<String, Object> context = new HashMap<>(payload);
        context.put("userLogin", ServiceUtil.getUserLogin(event.getDispatchContext(),
                context, "system"));
        try {
            dispatcher.runSync("createProductionRunFromRequirement", context);
        } catch (GenericServiceException e) {
            Debug.logError(e, "Error invoking createProductionRunFromRequirement"
                    + " for requirementId=" + payload.get("requirementId"), MODULE);
        }
    }

    private static void handleRequirementUpdated(DomainEvent event) {
        LocalDispatcher dispatcher = getDispatcher(event);
        if (dispatcher == null) {
            return;
        }
        Map<String, Object> payload = event.getPayload();
        Map<String, Object> context = new HashMap<>(payload);
        context.put("userLogin", ServiceUtil.getUserLogin(event.getDispatchContext(),
                context, "system"));
        try {
            dispatcher.runSync("createProductionRunFromRequirement", context);
        } catch (GenericServiceException e) {
            Debug.logError(e, "Error invoking createProductionRunFromRequirement"
                    + " for requirementId=" + payload.get("requirementId"), MODULE);
        }
    }

    private static void handleProductAssocDeleted(DomainEvent event) {
        LocalDispatcher dispatcher = getDispatcher(event);
        if (dispatcher == null) {
            return;
        }
        Map<String, Object> payload = event.getPayload();
        Map<String, Object> context = new HashMap<>(payload);
        context.put("userLogin", ServiceUtil.getUserLogin(event.getDispatchContext(),
                context, "system"));
        try {
            dispatcher.runSync("updateLowLevelCode", context);
        } catch (GenericServiceException e) {
            Debug.logError(e, "Error invoking updateLowLevelCode"
                    + " for productId=" + payload.get("productId"), MODULE);
        }
    }

    private static void handleShipmentReceiptCreated(DomainEvent event) {
        LocalDispatcher dispatcher = getDispatcher(event);
        if (dispatcher == null) {
            return;
        }
        Map<String, Object> payload = event.getPayload();
        Map<String, Object> context = new HashMap<>(payload);
        context.put("userLogin", ServiceUtil.getUserLogin(event.getDispatchContext(),
                context, "system"));
        try {
            dispatcher.runSync("checkDecomposeInventoryItem", context);
        } catch (GenericServiceException e) {
            Debug.logError(e, "Error invoking checkDecomposeInventoryItem"
                    + " for shipmentId=" + payload.get("shipmentId"), MODULE);
        }
    }

    private static LocalDispatcher getDispatcher(DomainEvent event) {
        DispatchContext dctx = event.getDispatchContext();
        if (dctx == null) {
            Debug.logError("No DispatchContext available in event " + event.getEventType()
                    + "; cannot dispatch manufacturing service", MODULE);
            return null;
        }
        return dctx.getDispatcher();
    }
}
