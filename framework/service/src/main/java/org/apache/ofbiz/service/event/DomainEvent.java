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

import java.sql.Timestamp;
import java.util.Collections;
import java.util.Map;

import org.apache.ofbiz.service.DispatchContext;

/**
 * Base class for all domain events published through the {@link EventBus}.
 * Each event carries an event type identifier, a timestamp, the source module name,
 * and an arbitrary payload of key-value pairs.
 *
 * <p>For Phase 1 (in-process delivery), events may also carry a {@link DispatchContext}
 * from the publishing service so that subscribers can invoke services without needing
 * an independently obtained dispatcher. This field will be {@code null} for events
 * delivered via an external broker in later phases.</p>
 */
public class DomainEvent {

    private final String eventType;
    private final Timestamp timestamp;
    private final String sourceModule;
    private final Map<String, Object> payload;
    private DispatchContext dispatchContext;

    public DomainEvent(String eventType, String sourceModule, Map<String, Object> payload) {
        this.eventType = eventType;
        this.timestamp = new Timestamp(System.currentTimeMillis());
        this.sourceModule = sourceModule;
        this.payload = payload != null ? Collections.unmodifiableMap(payload) : Collections.emptyMap();
    }

    /** Returns the event type identifier (e.g. {@code "RequirementCreated"}). */
    public String getEventType() {
        return eventType;
    }

    /** Returns the timestamp when this event was created. */
    public Timestamp getTimestamp() {
        return timestamp;
    }

    /** Returns the name of the module that produced this event. */
    public String getSourceModule() {
        return sourceModule;
    }

    /** Returns the event payload as an unmodifiable map. */
    public Map<String, Object> getPayload() {
        return payload;
    }

    /**
     * Returns the dispatch context from the publishing service, if available.
     * Only populated for in-process (Phase 1) event delivery.
     * @return the dispatch context, or {@code null} if not available
     */
    public DispatchContext getDispatchContext() {
        return dispatchContext;
    }

    /**
     * Sets the dispatch context for in-process event delivery.
     * @param dctx the dispatch context from the publishing service
     */
    public void setDispatchContext(DispatchContext dctx) {
        this.dispatchContext = dctx;
    }

    @Override
    public String toString() {
        return "DomainEvent[type=" + eventType + ", source=" + sourceModule + ", timestamp=" + timestamp + "]";
    }
}
