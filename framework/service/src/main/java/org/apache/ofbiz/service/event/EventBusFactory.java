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

/**
 * Factory for obtaining {@link EventBus} instances.
 * Currently returns a singleton {@link LocalEventBus} for in-process event delivery.
 * In later phases, this factory can be extended to return broker-backed implementations
 * (e.g. Kafka, RabbitMQ) based on configuration.
 */
public final class EventBusFactory {

    private static final EventBus INSTANCE = new LocalEventBus();

    private EventBusFactory() { }

    /**
     * Returns the default EventBus instance.
     * @return the singleton EventBus
     */
    public static EventBus getEventBus() {
        return INSTANCE;
    }
}
