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
 * Abstraction for a publish/subscribe event bus.
 * Phase 1 uses an in-process {@link LocalEventBus} implementation;
 * later phases may swap in Kafka or RabbitMQ without changing publishers or subscribers.
 */
public interface EventBus {

    /**
     * Publishes a domain event to all subscribers registered for the event's type.
     * @param event the domain event to publish
     */
    void publish(DomainEvent event);

    /**
     * Subscribes a handler for events matching the given event type.
     * @param eventType the event type to listen for (e.g. {@code "RequirementCreated"})
     * @param handler   the handler to invoke when a matching event is published
     */
    void subscribe(String eventType, EventHandler handler);
}
