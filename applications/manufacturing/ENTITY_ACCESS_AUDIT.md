# Manufacturing Module: Cross-Domain Entity Access Audit

## Phase 1G — Schema Separation

This document catalogs all direct entity accesses from manufacturing code to entities
owned by other domains. These accesses must eventually be replaced by port-mediated
API calls to complete the manufacturing module decoupling.

## Summary

| Domain | Entity Count | Access Count | Priority |
|--------|-------------|-------------|----------|
| Product | 12 | 39 | HIGH |
| WorkEffort | 10 | 32 | HIGH |
| Order | 10 | 25 | MEDIUM |
| Accounting | 3 | 8 | LOW |
| Party/Facility | 3 | 5 | LOW |
| **Total** | **38** | **109** | |

## Manufacturing-Owned Entities (No Migration Needed)

These entities belong to the manufacturing domain and are accessed legitimately:

| Entity | Package | Notes |
|--------|---------|-------|
| TechDataCalendar | manufacturing.techdata | Calendar definitions |
| TechDataCalendarWeek | manufacturing.techdata | Weekly calendar patterns |
| TechDataCalendarExcDay | manufacturing.techdata | Calendar day exceptions |
| TechDataCalendarExcWeek | manufacturing.techdata | Calendar week exceptions |
| MrpEvent | manufacturing.mrp | MRP planning events |
| MrpEventType | manufacturing.mrp | MRP event type definitions |
| MrpEventView | manufacturing.mrp | MRP event view entity |
| ProductManufacturingRule | manufacturing.bom | BOM substitution rules |

---

## Product Domain Accesses (39 total)

### Entity: Product (14 accesses)

| File | Line | Pattern | Priority | Recommended Port Method |
|------|------|---------|----------|------------------------|
| ProductionRunServices.java | 210 | Read by PK | HIGH | `ProductPort.getProduct()` |
| ProductionRunServices.java | 1319 | Read by PK | HIGH | `ProductPort.getProduct()` |
| ProductionRunServices.java | 1386 | Read by PK | HIGH | `ProductPort.getProduct()` |
| BOMTree.java | 97 | Read by PK | HIGH | `ProductPort.getProduct()` |
| BOMTree.java | 116 | Read by PK | HIGH | `ProductPort.getProduct()` |
| BOMTree.java | 135 | Read by PK | HIGH | `ProductPort.getProduct()` |
| BOMNode.java | 91 | Read by PK | HIGH | `ProductPort.getProduct()` |
| BOMNode.java | 300 | Read by PK (variant) | HIGH | `ProductPort.getProduct()` |
| BOMServices.java | 145 | Read by PK | HIGH | `ProductPort.getProduct()` |
| BOMServices.java | 161 | Read by PK (virtual) | HIGH | `ProductPort.getProduct()` |
| BOMServices.java | 204 | Read by PK (variant) | HIGH | `ProductPort.getProduct()` |
| BOMServices.java | 231 | Read all products | LOW | Read-model replica |

### Entity: ProductAssoc (9 accesses)

| File | Line | Pattern | Priority | Recommended Port Method |
|------|------|---------|----------|------------------------|
| ProductionRunServices.java | 2323 | Read by productId, type, date | HIGH | `ProductPort.getProductAssocs()` |
| BOMHelper.java | 73 | Read by productId, type, date | HIGH | `ProductPort.getProductAssocs()` |
| BOMHelper.java | 119 | Read by productIdTo, type, date | MEDIUM | Extended port method |
| BOMNode.java | 109 | Read by productId, type, date | HIGH | `ProductPort.getProductAssocs()` |
| BOMNode.java | 117 | Read by productIdTo, type, date | MEDIUM | Extended port method |
| BOMNode.java | 338 | Read by productId, type, date | HIGH | `ProductPort.getProductAssocs()` |
| BOMNode.java | 346 | Read by productIdTo, type, date | MEDIUM | Extended port method |
| BOMServices.java | 154 | Read by productIdTo, type | MEDIUM | Extended port method |
| BOMServices.java | 199 | Read by productId, type | HIGH | `ProductPort.getProductAssocs()` |
| BOMTree.java | 170 | Read by productId, type, date | HIGH | `ProductPort.getProductAssocs()` |

### Entity: InventoryItem (3 accesses)

| File | Line | Pattern | Priority | Recommended Port Method |
|------|------|---------|----------|------------------------|
| ProductionRunServices.java | 3027 | Read by PK | HIGH | `ProductPort.getInventoryItem()` |
| ProductionRunServices.java | 3071 | Read by PK | HIGH | `ProductPort.getInventoryItem()` |

### Entity: ProductFacility (2 accesses)

| File | Line | Pattern | Priority | Recommended Port Method |
|------|------|---------|----------|------------------------|
| ProductionRunServices.java | 1674 | Read by PK | MEDIUM | Extended port method |
| MrpServices.java | 412 | Read by facilityId | MEDIUM | Extended port method |

### Entity: ProductCostComponentCalc (1 access)

| File | Line | Pattern | Priority | Recommended Port Method |
|------|------|---------|----------|------------------------|
| ProductionRunServices.java | 872 | Read by productId, type | LOW | Extended port method |

### Entity: ProductFeatureAppl (1 access)

| File | Line | Pattern | Priority | Recommended Port Method |
|------|------|---------|----------|------------------------|
| BOMTree.java | 101 | Read by productId, type | LOW | Extended port method |

### Entity: ProductAssocType (1 access)

| File | Line | Pattern | Priority | Recommended Port Method |
|------|------|---------|----------|------------------------|
| BOMServices.java | 89 | Read all BOM types | LOW | Read-model replica |

### Entity: InventoryItemDetail (1 access)

| File | Line | Pattern | Priority | Recommended Port Method |
|------|------|---------|----------|------------------------|
| ProductionRunServices.java | 1934 | Read by inventoryItemId | MEDIUM | Extended port method |

### Entity: ShipmentPackage / ShipmentItem / ShipmentBoxType (5 accesses)

| File | Line | Entity | Pattern | Priority |
|------|------|--------|---------|----------|
| BOMServices.java | 509 | ShipmentPackage | Read by shipmentId | MEDIUM |
| BOMServices.java | 519 | ShipmentItem | Read by shipmentId | MEDIUM |
| BOMServices.java | 621 | ShipmentBoxType | Read by PK | LOW |
| BOMServices.java | 651 | ShipmentBoxType | Read by PK | LOW |

### Entity: ProductStore (1 access)

| File | Line | Pattern | Priority | Recommended Port Method |
|------|------|---------|----------|------------------------|
| BOMTree.java | 344 | Read by PK | LOW | Extended port method |

### Entity: Lot (1 access)

| File | Line | Pattern | Priority | Recommended Port Method |
|------|------|---------|----------|------------------------|
| ProductionRunServices.java | 1604 | Read by PK | LOW | Extended port method |

---

## WorkEffort Domain Accesses (32 total)

### Entity: WorkEffort (12 accesses)

| File | Line | Pattern | Priority | Recommended Port Method |
|------|------|---------|----------|------------------------|
| ProductionRun.java | 80 | Read by PK | HIGH | `WorkEffortPort.getWorkEffort()` |
| ProductionRun.java | 84 | Read by PK | HIGH | `WorkEffortPort.getWorkEffort()` |
| ProductionRunHelper.java | 57 | Read by PK | HIGH | `WorkEffortPort.getWorkEffort()` |
| ProductionRunHelper.java | 80 | Read by parentId, type | HIGH | Extended port method |
| ProductionRunServices.java | 927 | Read by PK | HIGH | `WorkEffortPort.getWorkEffort()` |
| ProductionRunServices.java | 964 | Read by parentId, type | HIGH | Extended port method |
| ProductionRunServices.java | 1002 | Read by PK | HIGH | `WorkEffortPort.getWorkEffort()` |
| ProductionRunServices.java | 1454 | Read by PK | HIGH | `WorkEffortPort.getWorkEffort()` |
| ProductionRunServices.java | 2769 | Read by PK | HIGH | `WorkEffortPort.getWorkEffort()` |
| TechDataServices.java | 83 | Read by workEffortTypeId | MEDIUM | Extended port method |
| RoutingServices.java | 71 | Read by PK | HIGH | `WorkEffortPort.getWorkEffort()` |
| MrpServices.java | 831 | Read by PK | HIGH | `WorkEffortPort.getWorkEffort()` |

### Entity: WorkEffortAssoc (4 accesses)

| File | Line | Pattern | Priority | Recommended Port Method |
|------|------|---------|----------|------------------------|
| ProductionRunHelper.java | 91 | Read by workEffortIdFrom | HIGH | `WorkEffortPort.getWorkEffortAssocs()` |
| ProductionRunHelper.java | 101 | Read by workEffortIdTo | MEDIUM | Extended port method |
| ProductionRunServices.java | 596 | Read by workEffortIdFrom, type | HIGH | `WorkEffortPort.getWorkEffortAssocs()` |
| ProductionRunServices.java | 1019 | Read by workEffortIdTo, type | MEDIUM | Extended port method |
| TechDataServices.java | 126 | Read by workEffortIdFrom | MEDIUM | `WorkEffortPort.getWorkEffortAssocs()` |

### Entity: WorkEffortGoodStandard (2 accesses)

| File | Line | Pattern | Priority | Recommended Port Method |
|------|------|---------|----------|------------------------|
| ProductionRunServices.java | 129 | Read by workEffortId, type | HIGH | Extended port method |
| ProductionRunServices.java | 149 | Read by workEffortId, type | HIGH | Extended port method |

### Entity: WorkEffortInventoryAssign / WorkEffortAndInventoryAssign (4 accesses)

| File | Line | Pattern | Priority | Recommended Port Method |
|------|------|---------|----------|------------------------|
| ProductionRunServices.java | 774 | Read by workEffortId | MEDIUM | Extended port method |
| ProductionRunServices.java | 1111 | Read by workEffortId | MEDIUM | Extended port method |
| ProductionRunServices.java | 1924 | Read by workEffortId | MEDIUM | Extended port method |
| ProductionRunServices.java | 2055 | Read by workEffortId | MEDIUM | Extended port method |
| MrpServices.java | 340 | Read by workEffortId | MEDIUM | Extended port method |

### Entity: WorkEffortCostCalc (1 access)

| File | Line | Pattern | Priority | Recommended Port Method |
|------|------|---------|----------|------------------------|
| ProductionRunServices.java | 1029 | Read by workEffortId | LOW | Extended port method |

### Entity: WorkEffortPartyAssignment (1 access)

| File | Line | Pattern | Priority | Recommended Port Method |
|------|------|---------|----------|------------------------|
| ProductionRunServices.java | 1506 | Read by workEffortId, partyId | LOW | Extended port method |

### Entity: WorkEffortAndGoods (3 accesses)

| File | Line | Pattern | Priority | Recommended Port Method |
|------|------|---------|----------|------------------------|
| ProductionRunServices.java | 2999 | Read by productId, status | MEDIUM | Extended port method |
| ProductionRunServices.java | 3179 | Read by productId, status | MEDIUM | Extended port method |
| MrpServices.java | 326 | Read by productId, facilityId | MEDIUM | Extended port method |
| MrpServices.java | 367 | Read by productId, facilityId | MEDIUM | Extended port method |

### Entity: WorkEffortAndInventoryProduced (1 access)

| File | Line | Pattern | Priority | Recommended Port Method |
|------|------|---------|----------|------------------------|
| ProductionRunServices.java | 1932 | Read by workEffortId | MEDIUM | Extended port method |

### Entity: TimeEntry (1 access)

| File | Line | Pattern | Priority | Recommended Port Method |
|------|------|---------|----------|------------------------|
| ProductionRunServices.java | 1140 | Read by workEffortId | LOW | Extended port method |

---

## Order Domain Accesses (25 total)

### Entity: OrderHeader (2 accesses)

| File | Line | Pattern | Priority | Recommended Port Method |
|------|------|---------|----------|------------------------|
| ProductionRunServices.java | 2424 | Read by PK | HIGH | `OrderPort.getOrderHeader()` |
| BOMTree.java | 341 | Read by PK | HIGH | `OrderPort.getOrderHeader()` |

### Entity: OrderItem (3 accesses)

| File | Line | Pattern | Priority | Recommended Port Method |
|------|------|---------|----------|------------------------|
| ProductionRunServices.java | 2434 | Read by PK | HIGH | `OrderPort.getOrderItems()` |
| ProductionRunServices.java | 2605 | Read by PK | HIGH | `OrderPort.getOrderItems()` |
| ProductionRunServices.java | 2621 | Read by orderId | HIGH | `OrderPort.getOrderItems()` |

### Entity: Requirement (4 accesses)

| File | Line | Pattern | Priority | Recommended Port Method |
|------|------|---------|----------|------------------------|
| ProductionRunServices.java | 2148 | Read by PK | MEDIUM | Extended port method |
| ProductionRunServices.java | 2180 | Read by PK | MEDIUM | Extended port method |
| MrpServices.java | 96 | Read by statusId, type | MEDIUM | Extended port method |
| MrpServices.java | 118 | Read by statusId, type | MEDIUM | Extended port method |
| MrpServices.java | 215 | Read by statusId, type | MEDIUM | Extended port method |

### Entity: OrderHeaderItemAndShipGroup / OrderHeaderAndItems (3 accesses)

| File | Line | Pattern | Priority | Recommended Port Method |
|------|------|---------|----------|------------------------|
| MrpServices.java | 153 | Read view entity | MEDIUM | Extended port method |
| MrpServices.java | 257 | Read view entity | MEDIUM | Extended port method |
| ProductionRunServices.java | 3221 | Read view entity | MEDIUM | Extended port method |

### Entity: OrderDeliverySchedule (3 accesses)

| File | Line | Pattern | Priority | Recommended Port Method |
|------|------|---------|----------|------------------------|
| ProductionRunServices.java | 3232 | Read by orderId, itemSeqId | LOW | Extended port method |
| ProductionRunServices.java | 3238 | Read by orderId, itemSeqId | LOW | Extended port method |
| MrpServices.java | 273 | Read by orderId, itemSeqId | LOW | Extended port method |
| MrpServices.java | 298 | Read by orderId, itemSeqId | LOW | Extended port method |

### Entity: OrderShipment (1 access)

| File | Line | Pattern | Priority | Recommended Port Method |
|------|------|---------|----------|------------------------|
| BOMHelper.java | 148 | Read by orderId | LOW | Extended port method |
| BOMServices.java | 529 | Read by shipmentId | LOW | Extended port method |

### Entity: OrderItemShipGroupAssoc / OrderItemShipGroup (2 accesses)

| File | Line | Pattern | Priority | Recommended Port Method |
|------|------|---------|----------|------------------------|
| ProductionRunServices.java | 2602 | Read by orderId | LOW | Extended port method |
| ProductionRunServices.java | 3276 | Read by orderId | LOW | Extended port method |

### Entity: OrderItemAndShipGrpInvResAndItem (1 access)

| File | Line | Pattern | Priority | Recommended Port Method |
|------|------|---------|----------|------------------------|
| ProductionRunServices.java | 3270 | Read by productId | LOW | Extended port method |

### Entity: OrderItemShipGrpInvRes (1 access)

| File | Line | Pattern | Priority | Recommended Port Method |
|------|------|---------|----------|------------------------|
| ProductionRunServices.java | 3303 | Read by orderId, itemSeqId | LOW | Extended port method |

### Entity: WorkAndOrderItemFulfillment / WorkOrderItemFulfillment (3 accesses)

| File | Line | Pattern | Priority | Recommended Port Method |
|------|------|---------|----------|------------------------|
| ProductionRunServices.java | 2665 | Read view entity | LOW | Extended port method |
| ProductionRunServices.java | 2675 | Read view entity | LOW | Extended port method |
| BOMHelper.java | 153 | Read by orderId | LOW | Extended port method |

---

## Accounting Domain Accesses (8 total)

### Entity: CostComponent (3 accesses)

| File | Line | Pattern | Priority | Recommended Port Method |
|------|------|---------|----------|------------------------|
| ProductionRunServices.java | 932 | Read by workEffortId | MEDIUM | `AccountingPort` extension |
| ProductionRunServices.java | 1650 | Read by productId | MEDIUM | `AccountingPort` extension |
| ProductionRunServices.java | 1659 | Read by productId | MEDIUM | `AccountingPort` extension |

### Entity: RateAmount (2 accesses)

| File | Line | Pattern | Priority | Recommended Port Method |
|------|------|---------|----------|------------------------|
| ProductionRunServices.java | 1147 | Read by rateTypeId | LOW | `AccountingPort` extension |
| ProductionRunServices.java | 1154 | Read by rateTypeId | LOW | `AccountingPort` extension |

### Entity: Shipment (1 access)

| File | Line | Pattern | Priority | Recommended Port Method |
|------|------|---------|----------|------------------------|
| BOMTree.java | 353 | Read by PK | LOW | Extended port method |

---

## Party/Facility Domain Accesses (5 total)

### Entity: Facility (1 access)

| File | Line | Pattern | Priority | Recommended Port Method |
|------|------|---------|----------|------------------------|
| MrpServices.java | 454 | Read by PK | LOW | Extended port method |

### Entity: FacilityContactMech (1 access)

| File | Line | Pattern | Priority | Recommended Port Method |
|------|------|---------|----------|------------------------|
| MrpServices.java | 250 | Read by facilityId | LOW | Extended port method |

### Entity: FacilityGroup (1 access)

| File | Line | Pattern | Priority | Recommended Port Method |
|------|------|---------|----------|------------------------|
| MrpServices.java | 635 | Read by PK | LOW | Extended port method |

### Entity: SalesForecast / SalesForecastDetail (2 accesses)

| File | Line | Pattern | Priority | Recommended Port Method |
|------|------|---------|----------|------------------------|
| MrpServices.java | 460 | Read by facilityId | LOW | Extended port method |
| MrpServices.java | 481 | Read by salesForecastId | LOW | Extended port method |

### Entity: CustomTimePeriod (1 access)

| File | Line | Pattern | Priority | Recommended Port Method |
|------|------|---------|----------|------------------------|
| MrpServices.java | 470 | Read by PK | LOW | Extended port method |

---

## Phase 1G Port Methods Created

The following high-priority port methods have been created in this phase:

### ProductPort (3 new methods)
- `getProduct(String productId)` -> `ProductData`
- `getProductAssocs(String productId, String productAssocTypeId, Timestamp filterByDate)` -> `List<ProductAssocData>`
- `getInventoryItem(String inventoryItemId)` -> `InventoryItemData`

### WorkEffortPort (2 new methods)
- `getWorkEffort(String workEffortId)` -> `WorkEffortData`
- `getWorkEffortAssocs(String workEffortIdFrom, String workEffortAssocTypeId)` -> `List<WorkEffortAssocData>`

### OrderPort (2 new methods)
- `getOrderHeader(String orderId)` -> `OrderHeaderData`
- `getOrderItems(String orderId)` -> `List<OrderItemData>`

## Recommended Next Steps

1. **Phase 2A**: Replace direct Product entity reads (14 accesses) with `ProductPort.getProduct()` calls
2. **Phase 2B**: Replace direct WorkEffort entity reads (12 accesses) with `WorkEffortPort.getWorkEffort()` calls
3. **Phase 2C**: Replace direct Order entity reads (5 accesses) with `OrderPort.getOrderHeader()` / `getOrderItems()` calls
4. **Phase 2D**: Create read-model replicas for high-frequency view entities (WorkEffortAndGoods, OrderHeaderItemAndShipGroup, etc.)
5. **Phase 2E**: Extend port interfaces for remaining MEDIUM-priority accesses
6. **Phase 2F**: Address LOW-priority accesses and complete entity-level decoupling
