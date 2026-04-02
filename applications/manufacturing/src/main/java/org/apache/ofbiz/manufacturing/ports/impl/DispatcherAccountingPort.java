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

import java.util.Map;

import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.manufacturing.ports.AccountingPort;
import org.apache.ofbiz.manufacturing.ports.dto.AccountingPreferencesResult;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

/**
 * Dispatcher-backed implementation of {@link AccountingPort}.
 * Wraps {@code LocalDispatcher.runSync()} calls for all Accounting service invocations
 * originating from the Manufacturing module.
 */
public final class DispatcherAccountingPort implements AccountingPort {

    private final LocalDispatcher dispatcher;
    private final GenericValue userLogin;

    public DispatcherAccountingPort(LocalDispatcher dispatcher, GenericValue userLogin) {
        this.dispatcher = dispatcher;
        this.userLogin = userLogin;
    }

    @Override
    public AccountingPreferencesResult getPartyAccountingPreferences(String organizationPartyId) {
        Map<String, Object> ctx = UtilMisc.<String, Object>toMap(
                "organizationPartyId", organizationPartyId,
                "userLogin", userLogin);
        try {
            Map<String, Object> result = dispatcher.runSync("getPartyAccountingPreferences", ctx);
            if (ServiceUtil.isError(result)) {
                throw new RuntimeException("getPartyAccountingPreferences failed: " + ServiceUtil.getErrorMessage(result));
            }
            GenericValue partyAccountingPreference = (GenericValue) result.get("partyAccountingPreference");
            if (partyAccountingPreference == null) {
                return new AccountingPreferencesResult(null);
            }
            return new AccountingPreferencesResult(partyAccountingPreference.getString("baseCurrencyUomId"));
        } catch (GenericServiceException e) {
            throw new RuntimeException("Error calling getPartyAccountingPreferences", e);
        }
    }
}
