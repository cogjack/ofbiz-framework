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

import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.ofbiz.base.util.Debug;

/**
 * Factory for obtaining {@link EventBus} instances.
 * Currently returns a singleton {@link LocalEventBus} for in-process event delivery.
 * In later phases, this factory can be extended to return broker-backed implementations
 * (e.g. Kafka, RabbitMQ) based on configuration.
 *
 * <p>Subscriber modules are discovered via {@link java.util.ServiceLoader} and
 * initialized automatically on the first call to {@link #getEventBus()}.</p>
 */
public final class EventBusFactory {

    private static final String MODULE = EventBusFactory.class.getName();
    private static final EventBus INSTANCE = new LocalEventBus();
    private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);

    private EventBusFactory() { }

    /**
     * Returns the default EventBus instance.
     * On first invocation, discovers and initializes all {@link EventSubscriberModule}
     * implementations via ServiceLoader.
     * @return the singleton EventBus
     */
    public static EventBus getEventBus() {
        if (INITIALIZED.compareAndSet(false, true)) {
            initializeSubscriberModules();
        }
        return INSTANCE;
    }

    private static void initializeSubscriberModules() {
        ServiceLoader<EventSubscriberModule> loader =
                ServiceLoader.load(EventSubscriberModule.class);
        for (EventSubscriberModule subscriberModule : loader) {
            try {
                subscriberModule.registerSubscriptions(INSTANCE);
                Debug.logInfo("Initialized event subscriber module: "
                        + subscriberModule.getClass().getName(), MODULE);
            } catch (Exception e) {
                Debug.logError(e, "Failed to initialize event subscriber module: "
                        + subscriberModule.getClass().getName(), MODULE);
            }
        }
    }
}
