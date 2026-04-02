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
package org.apache.ofbiz.manufacturing.ports;

import org.apache.ofbiz.entity.GenericValue;

/**
 * Thin adapter interface abstracting the {@code OrderReadHelper} from the Order module.
 * Only exposes the methods that the Manufacturing module actually consumes.
 *
 * <p>In Phase 1B, this is backed by {@code OrderReadHelperAdapter} which delegates
 * to the real {@code OrderReadHelper}. In later phases, it may be replaced by
 * a port-mediated or event-driven mechanism.</p>
 */
public interface OrderHelper {

    /**
     * Returns the party that placed the order.
     * @return the placing party GenericValue, or null if not found
     */
    GenericValue getPlacingParty();

    /**
     * Returns the order item for the given sequence ID.
     * @param orderItemSeqId the order item sequence ID
     * @return the order item GenericValue
     */
    GenericValue getOrderItem(String orderItemSeqId);
}
