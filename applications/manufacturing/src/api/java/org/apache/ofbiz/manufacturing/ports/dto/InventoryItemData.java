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
package org.apache.ofbiz.manufacturing.ports.dto;

import java.math.BigDecimal;

/**
 * Minimal projection DTO for InventoryItem entity data needed by Manufacturing.
 * Used for inventory lookups during production run processing and decomposition.
 */
public final class InventoryItemData {

    private final String inventoryItemId;
    private final String inventoryItemTypeId;
    private final String productId;
    private final String facilityId;
    private final String lotId;
    private final String uomId;
    private final BigDecimal unitCost;
    private final String currencyUomId;
    private final BigDecimal quantityOnHandTotal;
    private final BigDecimal availableToPromiseTotal;
    private final String ownerPartyId;

    public InventoryItemData(String inventoryItemId, String inventoryItemTypeId,
            String productId, String facilityId, String lotId, String uomId,
            BigDecimal unitCost, String currencyUomId, BigDecimal quantityOnHandTotal,
            BigDecimal availableToPromiseTotal, String ownerPartyId) {
        this.inventoryItemId = inventoryItemId;
        this.inventoryItemTypeId = inventoryItemTypeId;
        this.productId = productId;
        this.facilityId = facilityId;
        this.lotId = lotId;
        this.uomId = uomId;
        this.unitCost = unitCost;
        this.currencyUomId = currencyUomId;
        this.quantityOnHandTotal = quantityOnHandTotal;
        this.availableToPromiseTotal = availableToPromiseTotal;
        this.ownerPartyId = ownerPartyId;
    }

    public String getInventoryItemId() {
        return inventoryItemId;
    }

    public String getInventoryItemTypeId() {
        return inventoryItemTypeId;
    }

    public String getProductId() {
        return productId;
    }

    public String getFacilityId() {
        return facilityId;
    }

    public String getLotId() {
        return lotId;
    }

    public String getUomId() {
        return uomId;
    }

    public BigDecimal getUnitCost() {
        return unitCost;
    }

    public String getCurrencyUomId() {
        return currencyUomId;
    }

    public BigDecimal getQuantityOnHandTotal() {
        return quantityOnHandTotal;
    }

    public BigDecimal getAvailableToPromiseTotal() {
        return availableToPromiseTotal;
    }

    public String getOwnerPartyId() {
        return ownerPartyId;
    }
}
