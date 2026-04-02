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

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Map;

import org.apache.ofbiz.manufacturing.ports.dto.CostComponentCreatedResult;
import org.apache.ofbiz.manufacturing.ports.dto.InventoryAvailableResult;
import org.apache.ofbiz.manufacturing.ports.dto.InventoryItemCreatedResult;
import org.apache.ofbiz.manufacturing.ports.dto.LotCreatedResult;
import org.apache.ofbiz.manufacturing.ports.dto.MktgPackagesAvailableResult;
import org.apache.ofbiz.manufacturing.ports.dto.ProductCostResult;
import org.apache.ofbiz.manufacturing.ports.dto.ProductVariantResult;
import org.apache.ofbiz.manufacturing.ports.dto.ShipmentPackageCreatedResult;

/**
 * Port interface abstracting all outbound dispatcher calls from Manufacturing to the Product module.
 * Covers 29 dispatcher calls across 13 method signatures originating from
 * {@code ProductionRunServices.java}, {@code MrpServices.java}, {@code BOMNode.java},
 * and {@code BOMServices.java}.
 *
 * <p>In Phase 1B, this interface will be backed by a dispatcher-based implementation.
 * In later phases, it will be replaced by a REST/gRPC client.</p>
 */
public interface ProductPort {

    /**
     * Creates a cost component record for a work effort (production run task).
     * <p>Abstracts the {@code createCostComponent} service.
     * Called from {@code ProductionRunServices.changeProductionRunTaskStatus} and
     * {@code ProductionRunServices.createProductionRunTaskCosts}.</p>
     *
     * @param workEffortId          the work effort this cost belongs to
     * @param costComponentTypeId   cost type (e.g. ACTUAL_MAT_COST, ACTUAL_ROUTE_COST, ACTUAL_LABOR_COST)
     * @param costComponentCalcId   calculation reference (nullable)
     * @param costUomId             currency UOM
     * @param cost                  the cost amount
     * @param fixedAssetId          associated fixed asset (nullable)
     * @return result containing the generated costComponentId
     */
    CostComponentCreatedResult createCostComponent(String workEffortId, String costComponentTypeId,
            String costComponentCalcId, String costUomId, BigDecimal cost, String fixedAssetId);

    /**
     * Creates a new inventory item for produced goods.
     * <p>Abstracts the {@code createInventoryItem} service.
     * Called from {@code ProductionRunServices.productionRunProduce} and
     * {@code ProductionRunServices.productionRunTaskProduce}.</p>
     *
     * @param productId             the product
     * @param inventoryItemTypeId   type (SERIALIZED_INV_ITEM or NON_SERIAL_INV_ITEM)
     * @param facilityId            facility where inventory is stored
     * @param statusId              initial status (e.g. INV_AVAILABLE) (nullable)
     * @param unitCost              unit cost (nullable)
     * @param currencyUomId         currency of the unit cost (nullable)
     * @param lotId                 lot identifier (nullable)
     * @param uomId                 unit of measure (nullable)
     * @param locationSeqId         location within the facility (nullable)
     * @param datetimeReceived      when the item was received
     * @param datetimeManufactured  when the item was manufactured
     * @param comments              optional comments
     * @param isReturned            whether this is a returned item (nullable, Y/N)
     * @return result containing the generated inventoryItemId
     */
    InventoryItemCreatedResult createInventoryItem(String productId, String inventoryItemTypeId,
            String facilityId, String statusId, BigDecimal unitCost, String currencyUomId,
            String lotId, String uomId, String locationSeqId, Timestamp datetimeReceived,
            Timestamp datetimeManufactured, String comments, String isReturned);

    /**
     * Creates a detail record for an inventory item, adjusting ATP and QOH.
     * <p>Abstracts the {@code createInventoryItemDetail} service.
     * Called from {@code ProductionRunServices.productionRunProduce} and
     * {@code ProductionRunServices.productionRunTaskProduce}.</p>
     *
     * @param inventoryItemId        the inventory item
     * @param workEffortId           associated work effort
     * @param availableToPromiseDiff ATP adjustment
     * @param quantityOnHandDiff     QOH adjustment
     */
    void createInventoryItemDetail(String inventoryItemId, String workEffortId,
            BigDecimal availableToPromiseDiff, BigDecimal quantityOnHandDiff);

    /**
     * Rebalances inventory reservations after production.
     * <p>Abstracts the {@code balanceInventoryItems} service.
     * Called from {@code ProductionRunServices.productionRunProduce} and
     * {@code ProductionRunServices.productionRunTaskProduce}.</p>
     *
     * @param inventoryItemId         the inventory item to rebalance
     * @param priorityOrderId         priority order for reservations (nullable)
     * @param priorityOrderItemSeqId  priority order item (nullable)
     */
    void balanceInventoryItems(String inventoryItemId, String priorityOrderId, String priorityOrderItemSeqId);

    /**
     * Retrieves the total inventory available for a product across all facilities.
     * <p>Abstracts the {@code getProductInventoryAvailable} service.
     * Called from {@code MrpServices.findProductMrpQoh}.</p>
     *
     * @param productId the product to check
     * @return inventory availability totals
     */
    InventoryAvailableResult getProductInventoryAvailable(String productId);

    /**
     * Retrieves the inventory available for a product at a specific facility.
     * <p>Abstracts the {@code getInventoryAvailableByFacility} service.
     * Called from {@code MrpServices.findProductMrpQoh} and
     * {@code ProductionRunServices.createProductionRunForMktgPkg}.</p>
     *
     * @param productId  the product to check
     * @param facilityId the facility to check
     * @return inventory availability totals for the facility
     */
    InventoryAvailableResult getInventoryAvailableByFacility(String productId, String facilityId);

    /**
     * Retrieves the cost of a product for a given currency and cost type prefix.
     * <p>Abstracts the {@code getProductCost} service.
     * Called from {@code ProductionRunServices.productionRunProduce} and
     * {@code ProductionRunServices.decomposeInventoryItem}.</p>
     *
     * @param productId               the product
     * @param currencyUomId           the currency
     * @param costComponentTypePrefix  cost type prefix (e.g. EST_STD)
     * @return result containing the product cost
     */
    ProductCostResult getProductCost(String productId, String currencyUomId, String costComponentTypePrefix);

    /**
     * Associates a product with a facility (creates a ProductFacility record if not existing).
     * <p>Abstracts the {@code createProductFacility} service.
     * Called from {@code ProductionRunServices.productionRunProduce}.</p>
     *
     * @param productId  the product
     * @param facilityId the facility
     */
    void createProductFacility(String productId, String facilityId);

    /**
     * Creates a new lot record for inventory tracking.
     * <p>Abstracts the {@code createLot} service.
     * Called from {@code ProductionRunServices.productionRunProduce}.</p>
     *
     * @param lotId        the lot ID (nullable for auto-generation)
     * @param creationDate when the lot was created
     * @return result containing the generated or provided lotId
     */
    LotCreatedResult createLot(String lotId, Timestamp creationDate);

    /**
     * Checks availability of marketing packages that can be assembled from components.
     * <p>Abstracts the {@code getMktgPackagesAvailable} service.
     * Called from {@code ProductionRunServices.createProductionRunForMktgPkg}.</p>
     *
     * @param productId  the marketing package product
     * @param facilityId the facility to check
     * @return availability totals for the marketing package
     */
    MktgPackagesAvailableResult getMktgPackagesAvailable(String productId, String facilityId);

    /**
     * Resolves a product variant based on selected features.
     * <p>Abstracts the {@code getProductVariant} service.
     * Called from {@code BOMNode.configurator}.</p>
     *
     * @param productId        the virtual/configurable product
     * @param selectedFeatures map of feature type ID to feature ID
     * @return result containing matching variant product IDs
     */
    ProductVariantResult getProductVariant(String productId, Map<String, String> selectedFeatures);

    /**
     * Returns the aggregated instance product ID for a configured product.
     * <p>Abstracts the {@code ProductWorker.getAggregatedInstanceId} utility method.
     * Called from {@code ProductionRunServices.createProductionRunFromConfiguration}.</p>
     *
     * @param productId the configurable product ID
     * @param configId  the configuration ID
     * @return the aggregated instance product ID
     */
    String getAggregatedInstanceId(String productId, String configId);

    /**
     * Creates a shipment package.
     * <p>Abstracts the {@code createShipmentPackage} service.
     * Called from {@code BOMServices.createShipmentPackages}.</p>
     *
     * @param shipmentId        the shipment
     * @param shipmentBoxTypeId the box type
     * @return result containing the generated shipmentPackageSeqId
     */
    ShipmentPackageCreatedResult createShipmentPackage(String shipmentId, String shipmentBoxTypeId);

    /**
     * Associates content (items) with a shipment package.
     * <p>Abstracts the {@code createShipmentPackageContent} service.
     * Called from {@code BOMServices.createShipmentPackages}.</p>
     *
     * @param shipmentId           the shipment
     * @param shipmentPackageSeqId the package within the shipment
     * @param shipmentItemSeqId    the shipment item
     * @param quantity             quantity of the item in the package (nullable if subProduct)
     * @param subProductId         sub-product ID (nullable if not a sub-product)
     * @param subProductQuantity   sub-product quantity (nullable if not a sub-product)
     */
    void createShipmentPackageContent(String shipmentId, String shipmentPackageSeqId,
            String shipmentItemSeqId, BigDecimal quantity, String subProductId, BigDecimal subProductQuantity);
}
