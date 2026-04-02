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
package org.apache.ofbiz.service.event;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.ofbiz.base.util.Debug;

/**
 * In-process {@link EventBus} implementation using a simple pub/sub pattern.
 * Handlers are invoked synchronously in the publishing thread.
 * This is suitable for Phase 1 (single-JVM deployment); later phases may replace
 * this with a Kafka- or RabbitMQ-backed implementation.
 */
public class LocalEventBus implements EventBus {

    private static final String MODULE = LocalEventBus.class.getName();

    private final Map<String, List<EventHandler>> subscribers = new ConcurrentHashMap<>();

    @Override
    public void publish(DomainEvent event) {
        List<EventHandler> handlers = subscribers.get(event.getEventType());
        if (handlers != null) {
            for (EventHandler handler : handlers) {
                try {
                    handler.onEvent(event);
                } catch (Exception e) {
                    Debug.logError(e, "Error handling event " + event.getEventType()
                            + " in handler " + handler.getClass().getName(), MODULE);
                }
            }
        }
        if (Debug.verboseOn()) {
            int count = (handlers != null) ? handlers.size() : 0;
            Debug.logVerbose("Published event " + event.getEventType()
                    + " to " + count + " handler(s)", MODULE);
        }
    }

    @Override
    public void subscribe(String eventType, EventHandler handler) {
        subscribers.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(handler);
        Debug.logInfo("Subscribed handler " + handler.getClass().getName()
                + " to event type " + eventType, MODULE);
    }
}
