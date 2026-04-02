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

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.manufacturing.ports.ProductPort;
import org.apache.ofbiz.manufacturing.ports.dto.CostComponentCreatedResult;
import org.apache.ofbiz.manufacturing.ports.dto.InventoryAvailableResult;
import org.apache.ofbiz.manufacturing.ports.dto.InventoryItemCreatedResult;
import org.apache.ofbiz.manufacturing.ports.dto.LotCreatedResult;
import org.apache.ofbiz.manufacturing.ports.dto.MktgPackagesAvailableResult;
import org.apache.ofbiz.manufacturing.ports.dto.ProductCostResult;
import org.apache.ofbiz.manufacturing.ports.dto.ProductVariantResult;
import org.apache.ofbiz.manufacturing.ports.dto.ShipmentPackageCreatedResult;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

/**
 * Dispatcher-backed implementation of {@link ProductPort}.
 * Wraps {@code LocalDispatcher.runSync()} calls for all Product/Inventory service invocations
 * originating from the Manufacturing module.
 */
public final class DispatcherProductPort implements ProductPort {

    private final LocalDispatcher dispatcher;
    private final GenericValue userLogin;

    public DispatcherProductPort(LocalDispatcher dispatcher, GenericValue userLogin) {
        this.dispatcher = dispatcher;
        this.userLogin = userLogin;
    }

    @Override
    public CostComponentCreatedResult createCostComponent(String workEffortId, String costComponentTypeId,
            String costComponentCalcId, String costUomId, BigDecimal cost, String fixedAssetId) {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("workEffortId", workEffortId);
        ctx.put("costComponentTypeId", costComponentTypeId);
        ctx.put("costUomId", costUomId);
        ctx.put("cost", cost);
        ctx.put("userLogin", userLogin);
        if (costComponentCalcId != null) {
            ctx.put("costComponentCalcId", costComponentCalcId);
        }
        if (fixedAssetId != null) {
            ctx.put("fixedAssetId", fixedAssetId);
        }
        try {
            Map<String, Object> result = dispatcher.runSync("createCostComponent", ctx);
            if (ServiceUtil.isError(result)) {
                throw new RuntimeException("createCostComponent failed: " + ServiceUtil.getErrorMessage(result));
            }
            return new CostComponentCreatedResult((String) result.get("costComponentId"));
        } catch (GenericServiceException e) {
            throw new RuntimeException("Error calling createCostComponent", e);
        }
    }

    @Override
    public InventoryItemCreatedResult createInventoryItem(String productId, String inventoryItemTypeId,
            String facilityId, String statusId, BigDecimal unitCost, String currencyUomId,
            String lotId, String uomId, String locationSeqId, Timestamp datetimeReceived,
            Timestamp datetimeManufactured, String comments, String isReturned) {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("productId", productId);
        ctx.put("inventoryItemTypeId", inventoryItemTypeId);
        ctx.put("facilityId", facilityId);
        ctx.put("datetimeReceived", datetimeReceived);
        ctx.put("datetimeManufactured", datetimeManufactured);
        ctx.put("userLogin", userLogin);
        if (statusId != null) {
            ctx.put("statusId", statusId);
        }
        if (unitCost != null) {
            ctx.put("unitCost", unitCost);
        }
        if (currencyUomId != null) {
            ctx.put("currencyUomId", currencyUomId);
        }
        if (lotId != null) {
            ctx.put("lotId", lotId);
        }
        if (uomId != null) {
            ctx.put("uomId", uomId);
        }
        if (locationSeqId != null) {
            ctx.put("locationSeqId", locationSeqId);
        }
        if (comments != null) {
            ctx.put("comments", comments);
        }
        if (isReturned != null) {
            ctx.put("isReturned", isReturned);
        }
        try {
            Map<String, Object> result = dispatcher.runSync("createInventoryItem", ctx);
            if (ServiceUtil.isError(result)) {
                throw new RuntimeException("createInventoryItem failed: " + ServiceUtil.getErrorMessage(result));
            }
            return new InventoryItemCreatedResult((String) result.get("inventoryItemId"));
        } catch (GenericServiceException e) {
            throw new RuntimeException("Error calling createInventoryItem", e);
        }
    }

    @Override
    public void createInventoryItemDetail(String inventoryItemId, String workEffortId,
            BigDecimal availableToPromiseDiff, BigDecimal quantityOnHandDiff) {
        Map<String, Object> ctx = UtilMisc.toMap(
                "inventoryItemId", inventoryItemId,
                "workEffortId", workEffortId,
                "availableToPromiseDiff", availableToPromiseDiff,
                "quantityOnHandDiff", quantityOnHandDiff,
                "userLogin", userLogin);
        try {
            Map<String, Object> result = dispatcher.runSync("createInventoryItemDetail", ctx);
            if (ServiceUtil.isError(result)) {
                throw new RuntimeException("createInventoryItemDetail failed: " + ServiceUtil.getErrorMessage(result));
            }
        } catch (GenericServiceException e) {
            throw new RuntimeException("Error calling createInventoryItemDetail", e);
        }
    }

    @Override
    public void balanceInventoryItems(String inventoryItemId, String priorityOrderId,
            String priorityOrderItemSeqId) {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("inventoryItemId", inventoryItemId);
        ctx.put("userLogin", userLogin);
        if (priorityOrderId != null) {
            ctx.put("priorityOrderId", priorityOrderId);
        }
        if (priorityOrderItemSeqId != null) {
            ctx.put("priorityOrderItemSeqId", priorityOrderItemSeqId);
        }
        try {
            Map<String, Object> result = dispatcher.runSync("balanceInventoryItems", ctx);
            if (ServiceUtil.isError(result)) {
                throw new RuntimeException("balanceInventoryItems failed: " + ServiceUtil.getErrorMessage(result));
            }
        } catch (GenericServiceException e) {
            throw new RuntimeException("Error calling balanceInventoryItems", e);
        }
    }

    @Override
    public InventoryAvailableResult getProductInventoryAvailable(String productId) {
        try {
            Map<String, Object> result = dispatcher.runSync("getProductInventoryAvailable",
                    UtilMisc.toMap("productId", productId));
            if (ServiceUtil.isError(result)) {
                throw new RuntimeException("getProductInventoryAvailable failed: " + ServiceUtil.getErrorMessage(result));
            }
            return new InventoryAvailableResult(
                    (BigDecimal) result.get("quantityOnHandTotal"),
                    (BigDecimal) result.get("availableToPromiseTotal"));
        } catch (GenericServiceException e) {
            throw new RuntimeException("Error calling getProductInventoryAvailable", e);
        }
    }

    @Override
    public InventoryAvailableResult getInventoryAvailableByFacility(String productId, String facilityId) {
        try {
            Map<String, Object> result = dispatcher.runSync("getInventoryAvailableByFacility",
                    UtilMisc.toMap("productId", productId, "facilityId", facilityId));
            if (ServiceUtil.isError(result)) {
                throw new RuntimeException("getInventoryAvailableByFacility failed: " + ServiceUtil.getErrorMessage(result));
            }
            return new InventoryAvailableResult(
                    (BigDecimal) result.get("quantityOnHandTotal"),
                    (BigDecimal) result.get("availableToPromiseTotal"));
        } catch (GenericServiceException e) {
            throw new RuntimeException("Error calling getInventoryAvailableByFacility", e);
        }
    }

    @Override
    public ProductCostResult getProductCost(String productId, String currencyUomId,
            String costComponentTypePrefix) {
        Map<String, Object> ctx = UtilMisc.toMap(
                "productId", productId,
                "currencyUomId", currencyUomId,
                "costComponentTypePrefix", costComponentTypePrefix,
                "userLogin", userLogin);
        try {
            Map<String, Object> result = dispatcher.runSync("getProductCost", ctx);
            if (ServiceUtil.isError(result)) {
                throw new RuntimeException("getProductCost failed: " + ServiceUtil.getErrorMessage(result));
            }
            return new ProductCostResult((BigDecimal) result.get("productCost"));
        } catch (GenericServiceException e) {
            throw new RuntimeException("Error calling getProductCost", e);
        }
    }

    @Override
    public void createProductFacility(String productId, String facilityId) {
        Map<String, Object> ctx = UtilMisc.toMap(
                "productId", productId,
                "facilityId", facilityId,
                "userLogin", userLogin);
        try {
            Map<String, Object> result = dispatcher.runSync("createProductFacility", ctx);
            if (ServiceUtil.isError(result)) {
                throw new RuntimeException("createProductFacility failed: " + ServiceUtil.getErrorMessage(result));
            }
        } catch (GenericServiceException e) {
            throw new RuntimeException("Error calling createProductFacility", e);
        }
    }

    @Override
    public LotCreatedResult createLot(String lotId, Timestamp creationDate) {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("creationDate", creationDate);
        ctx.put("userLogin", userLogin);
        if (lotId != null) {
            ctx.put("lotId", lotId);
        }
        try {
            Map<String, Object> result = dispatcher.runSync("createLot", ctx);
            if (ServiceUtil.isError(result)) {
                throw new RuntimeException("createLot failed: " + ServiceUtil.getErrorMessage(result));
            }
            return new LotCreatedResult((String) result.get("lotId"));
        } catch (GenericServiceException e) {
            throw new RuntimeException("Error calling createLot", e);
        }
    }

    @Override
    public MktgPackagesAvailableResult getMktgPackagesAvailable(String productId, String facilityId) {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("productId", productId);
        ctx.put("facilityId", facilityId);
        ctx.put("userLogin", userLogin);
        try {
            Map<String, Object> result = dispatcher.runSync("getMktgPackagesAvailable", ctx);
            if (ServiceUtil.isError(result)) {
                throw new RuntimeException("getMktgPackagesAvailable failed: " + ServiceUtil.getErrorMessage(result));
            }
            return new MktgPackagesAvailableResult((BigDecimal) result.get("availableToPromiseTotal"));
        } catch (GenericServiceException e) {
            throw new RuntimeException("Error calling getMktgPackagesAvailable", e);
        }
    }

    @Override
    public ProductVariantResult getProductVariant(String productId, Map<String, String> selectedFeatures) {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("productId", productId);
        ctx.put("selectedFeatures", selectedFeatures);
        try {
            Map<String, Object> result = dispatcher.runSync("getProductVariant", ctx);
            if (ServiceUtil.isError(result)) {
                throw new RuntimeException("getProductVariant failed: " + ServiceUtil.getErrorMessage(result));
            }
            List<GenericValue> products = UtilGenerics.cast(result.get("products"));
            List<String> productIds = new java.util.ArrayList<>();
            if (products != null) {
                for (GenericValue product : products) {
                    productIds.add(product.getString("productId"));
                }
            }
            return new ProductVariantResult(productIds);
        } catch (GenericServiceException e) {
            throw new RuntimeException("Error calling getProductVariant", e);
        }
    }

    @Override
    public ShipmentPackageCreatedResult createShipmentPackage(String shipmentId, String shipmentBoxTypeId) {
        Map<String, Object> ctx = UtilMisc.<String, Object>toMap(
                "shipmentId", shipmentId,
                "shipmentBoxTypeId", shipmentBoxTypeId,
                "userLogin", userLogin);
        try {
            Map<String, Object> result = dispatcher.runSync("createShipmentPackage", ctx);
            if (ServiceUtil.isError(result)) {
                throw new RuntimeException("createShipmentPackage failed: " + ServiceUtil.getErrorMessage(result));
            }
            return new ShipmentPackageCreatedResult((String) result.get("shipmentPackageSeqId"));
        } catch (GenericServiceException e) {
            throw new RuntimeException("Error calling createShipmentPackage", e);
        }
    }

    @Override
    public void createShipmentPackageContent(String shipmentId, String shipmentPackageSeqId,
            String shipmentItemSeqId, BigDecimal quantity, String subProductId, BigDecimal subProductQuantity) {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("shipmentId", shipmentId);
        ctx.put("shipmentPackageSeqId", shipmentPackageSeqId);
        ctx.put("shipmentItemSeqId", shipmentItemSeqId);
        ctx.put("userLogin", userLogin);
        if (quantity != null) {
            ctx.put("quantity", quantity);
        }
        if (subProductId != null) {
            ctx.put("subProductId", subProductId);
        }
        if (subProductQuantity != null) {
            ctx.put("subProductQuantity", subProductQuantity);
        }
        try {
            Map<String, Object> result = dispatcher.runSync("createShipmentPackageContent", ctx);
            if (ServiceUtil.isError(result)) {
                throw new RuntimeException("createShipmentPackageContent failed: " + ServiceUtil.getErrorMessage(result));
            }
        } catch (GenericServiceException e) {
            throw new RuntimeException("Error calling createShipmentPackageContent", e);
        }
    }
}
