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
package org.apache.ofbiz.manufacturing.ports.impl;

import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.manufacturing.ports.OrderHelper;
import org.apache.ofbiz.order.order.OrderReadHelper;

/**
 * Adapter that delegates to the Order module's {@code OrderReadHelper}.
 * This class lives in {@code ports/impl/} so that the cross-module import
 * of {@code OrderReadHelper} is confined to the boundary layer.
 */
public final class OrderReadHelperAdapter implements OrderHelper {

    private final OrderReadHelper delegate;

    public OrderReadHelperAdapter(Delegator delegator, String orderId) {
        this.delegate = new OrderReadHelper(delegator, orderId);
    }

    @Override
    public GenericValue getPlacingParty() {
        return delegate.getPlacingParty();
    }

    @Override
    public GenericValue getOrderItem(String orderItemSeqId) {
        return delegate.getOrderItem(orderItemSeqId);
    }
}
