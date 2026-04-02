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

import org.apache.ofbiz.manufacturing.ports.dto.AccountingPreferencesResult;

/**
 * Port interface abstracting all outbound dispatcher calls from Manufacturing to the Accounting module.
 * Covers 2 dispatcher calls (same service, different call sites) via 1 method signature
 * originating from {@code ProductionRunServices.java}.
 *
 * <p>In Phase 1B, this interface will be backed by a dispatcher-based implementation.
 * In later phases, manufacturing may cache accounting preferences locally or call
 * an Accounting API endpoint.</p>
 */
public interface AccountingPort {

    /**
     * Retrieves the accounting preferences for an organization party, primarily
     * used to determine the base currency for production costing.
     * <p>Abstracts the {@code getPartyAccountingPreferences} service.
     * Called from {@code ProductionRunServices.changeProductionRunTaskStatus} (line 1023)
     * and {@code ProductionRunServices.productionRunProduce} (line 1884).</p>
     *
     * @param organizationPartyId the organization whose preferences to retrieve
     * @return result containing the base currency UOM ID, or null if no preferences found
     */
    AccountingPreferencesResult getPartyAccountingPreferences(String organizationPartyId);
}
