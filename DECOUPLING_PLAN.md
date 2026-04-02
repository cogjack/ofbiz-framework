# OFBiz Monolith Decoupling Plan

## 1. Executive Summary

This document presents a phased implementation plan for decoupling the Apache OFBiz ERP monolith into separable domain modules. The analysis covers all 11 application modules under `applications/`, systematically cataloging four types of cross-domain coupling:

1. **Explicit service calls** (`dispatcher.runSync`/`runAsync`) in Java/Groovy code
2. **SECA (Service Event Condition Action) triggers** that reactively invoke services across module boundaries
3. **Direct Java class imports** from one application module's code into another
4. **Shared entity access** where one module's code reads/writes entities owned by another module's data model

**Key findings:**
- **Order** is the most coupled module (575 total coupling points, 388 outbound + 187 inbound), with heavy bidirectional coupling to both Product (244 outbound) and Accounting (63 outbound, 98 inbound from accounting).
- **Product** is the most depended-upon module (379 inbound coupling points), serving as the data backbone for inventory, shipment, catalog, and pricing.
- **Manufacturing** has high outbound coupling (175 points) but minimal inbound (10 points from only 3 modules), making it the safest extraction candidate.
- **Accounting** acts primarily as a reactive listener (96 inbound coupling points, mostly via ledger SECAs), but also has significant outbound coupling (193 points) to order and party.

**Recommended extraction order:**
1. **Phase 1: Manufacturing** — Unidirectional dependencies, minimal inbound coupling, well-defined service boundary.
2. **Phase 2: Accounting** — Break reactive SECA listeners into event-bus consumers, isolate ledger operations.
3. **Phase 3: Order** — Hardest extraction due to bidirectional coupling with Product and Accounting; requires the event bus and API infrastructure from Phases 1-2.

---

## 2. Current Architecture Overview

OFBiz is a service-oriented monolith. All modules run in a single JVM, sharing:
- A **ServiceDispatcher** that routes `runSync`/`runAsync` calls to any service defined in any module's `servicedef/services*.xml`.
- A **Delegator** that provides direct database access to any entity defined in any module's `entitydef/entitymodel*.xml`.
- **SECA rules** (`servicedef/secas*.xml`) that act as in-process event triggers, invoking services reactively when other services complete.

### Application Modules

| Module | Java Files | Services Defined | SECA Files | Role |
|--------|-----------|-----------------|------------|------|
| **accounting** | 45 | 809 | 4 (secas.xml, secas_invoice.xml, secas_ledger.xml, secas_payment.xml) | Financial transactions, GL, invoicing, payments |
| **order** | 42 | 467 | 1 (secas.xml) | Order management, cart, checkout, returns, quotes, requirements |
| **product** | 80 | 872 | 2 (secas.xml, secas_shipment.xml) | Catalog, inventory, shipment, facility, pricing, promos |
| **party** | 11 | 266 | 1 (secas.xml) | People, organizations, contacts, communication events |
| **content** | 53 | 292 | 1 (secas.xml) | CMS, documents, surveys, data resources |
| **workeffort** | 12 | 167 | 1 (secas.xml) | Work efforts, timesheets, project management |
| **humanres** | 1 | 131 | 0 | HR, employment, positions |
| **marketing** | 4 | 107 | 1 (secas.xml) | Campaigns, contact lists, tracking codes, SFA |
| **manufacturing** | 13 | 81 | 1 (secas.xml) | BOM, MRP, production runs, routing |
| **commonext** | 0 | 6 | 1 (secas.xml) | Shared utilities (notes, geo) |
| **securityext** | 2 | 1 | 0 | Login/security extensions |

Entity definitions reside in `applications/datamodel/entitydef/` with domain-specific files (e.g., `accounting-entitymodel.xml`, `order-entitymodel.xml`). Total entities mapped: **1,124** across all domains.

---

## 3. Cross-Domain Dependency Matrix

### 3.1 Combined Matrix (all coupling types)

Each cell shows: `Dispatcher | SECA | Imports | Entity = TOTAL`

| Source \ Target | accounting | content | manufacturing | marketing | order | party | product | workeffort |
|---|---|---|---|---|---|---|---|---|
| **accounting** | — | 0\|0\|0\|4=**4** | — | 0\|0\|0\|1=**1** | 19\|1\|29\|49=**98** | 9\|3\|6\|28=**46** | 1\|0\|13\|30=**44** | — |
| **content** | — | — | — | — | 0\|0\|0\|2=**2** | 1\|4\|0\|8=**13** | 0\|0\|0\|6=**6** | 0\|0\|0\|2=**2** |
| **manufacturing** | 2\|0\|0\|2=**4** | 0\|0\|1\|0=**1** | — | 0\|0\|0\|2=**2** | 2\|0\|6\|25=**33** | — | 29\|0\|4\|39=**72** | 31\|0\|0\|32=**63** |
| **marketing** | — | — | — | — | — | 2\|0\|4\|3=**9** | 0\|0\|1\|0=**1** | — |
| **order** | 30\|5\|4\|24=**63** | 3\|1\|4\|7=**15** | 3\|2\|0\|2=**7** | 0\|1\|1\|0=**2** | — | 0\|5\|28\|19=**52** | 37\|8\|106\|93=**244** | 1\|0\|0\|3=**4** |
| **party** | 0\|2\|7\|0=**9** | 4\|0\|3\|0=**7** | — | 2\|2\|0\|4=**8** | — | — | 0\|1\|1\|4=**6** | 1\|1\|0\|0=**2** |
| **product** | 1\|17\|0\|0=**18** | 33\|1\|12\|25=**71** | 0\|3\|0\|0=**3** | — | 2\|8\|19\|19=**48** | 5\|7\|10\|19=**41** | — | 0\|2\|0\|0=**2** |
| **workeffort** | 0\|2\|0\|0=**2** | 0\|0\|3\|0=**3** | — | — | 0\|6\|0\|0=**6** | 0\|2\|0\|2=**4** | 0\|0\|0\|2=**2** | — |

*(Cells with zero coupling on both sides omitted for clarity. commonext, humanres, securityext omitted due to minimal coupling.)*

### 3.2 Module Coupling Summary

| Module | Outbound | Inbound | Total | Extraction Difficulty |
|--------|----------|---------|-------|----------------------|
| **order** | 388 | 187 | **575** | Hardest |
| **product** | 183 | 379 | **562** | Very Hard (most depended-upon) |
| **accounting** | 193 | 96 | **289** | Hard (bidirectional with order) |
| **party** | 38 | 171 | **209** | Hard (foundation entity) |
| **manufacturing** | 175 | 10 | **185** | **Easiest** (unidirectional) |
| **content** | 23 | 101 | **124** | Moderate |
| **workeffort** | 17 | 73 | **90** | Moderate |
| **marketing** | 10 | 13 | **23** | Easy (after order/party) |
| **commonext** | 1 | 7 | **8** | Trivial |
| **humanres** | 5 | 0 | **5** | Easy (leaf module) |
| **securityext** | 4 | 0 | **4** | Easy (leaf module) |

### 3.3 Key Coupling Hotspots

1. **order→product: 244 points** — Order module uses 106 product imports (ShoppingCart depends heavily on Product classes), 37 dispatcher calls (price calculation, inventory checks), 93 entity accesses (29 unique product entities), and 8 SECA triggers.
2. **accounting→order: 98 points** — Accounting imports OrderReadHelper (29 imports), accesses 15 order entities (49 accesses), and makes 19 dispatcher calls to order services.
3. **manufacturing→product: 72 points** — Manufacturing calls 29 product services (inventory, cost components), accesses 17 product entities.
4. **manufacturing→workeffort: 63 points** — Production runs are built on top of WorkEffort entities; manufacturing calls 31 workeffort services directly.
5. **order→accounting: 63 points** — Order calls 30 accounting services (payment auth, billing, invoicing), plus 5 SECA triggers.
6. **order→party: 52 points** — Order imports 28 party classes and accesses 11 party entities.
7. **product→content: 71 points** — Product image management calls 33 content services.
8. **product→order: 48 points** — Bidirectional: product accesses 8 order entities, imports 19 order classes.

---

## 4. Phase 1: Manufacturing Extraction (Detailed)

### 4.1 Why Manufacturing First

Manufacturing is the safest extraction candidate because:
- **Only 10 inbound coupling points** from just 3 modules (order: 7, product: 3)
- **No other module imports manufacturing Java classes**
- **Inbound calls are limited to 3 dispatcher calls and 6 SECA triggers**
- **175 outbound coupling points** are all from manufacturing *calling out* to other modules — these can be replaced with API calls without affecting other modules' code

### 4.2 Service Boundary Definition

Manufacturing defines **81 services** in `applications/manufacturing/servicedef/services*.xml`. These would become the public API of the extracted module.

**Service categories:**

| Category | File | Count | Key Services |
|----------|------|-------|-------------|
| Production Runs | services_production_run.xml | 30 | `createProductionRun`, `changeProductionRunStatus`, `issueProductionRunTask`, `productionRunProduce`, `quickRunAllProductionRunTasks`, `cancelProductionRun` |
| BOM Management | services_bom.xml | 14 | `getBOMTree`, `getManufacturingComponents`, `updateLowLevelCode`, `createBOMAssoc` |
| MRP | services_mrp.xml | 9 | `executeMrp`, `initMrpEvents`, `findProductMrpQoh`, `setEstimatedDeliveryDates` |
| Calendar/Scheduling | services_calendar.xml | 11 | `createCalendar`, `createCalendarWeek`, `createCalendarExceptionDay` |
| Routing | services_routing.xml | 5 | `getProductRouting`, `getEstimatedTaskTime`, `checkRoutingTaskAssoc` |
| Formulas | services_formula.xml | 5 | `interfaceBomFormula`, `exampleComponentFormula`, `interfaceTaskFormula` |
| Shipment Packaging | services.xml | 1 | `createShipmentPackages` |
| Permissions | services_calendar.xml | 1 | `manufacturingPermissionService` |

### 4.3 Outbound Dependency Catalog

Manufacturing makes **64 explicit dispatcher calls** to services in 4 other modules:

#### 4.3.1 Manufacturing → WorkEffort (31 calls)

All calls originate from `ProductionRunServices.java`. Production runs are modeled as WorkEfforts internally.

| Service Called | Line(s) | Purpose |
|---------------|---------|---------|
| `createWorkEffort` | :283, :348, :1719, :3505 | Create production run header and routing tasks |
| `updateWorkEffort` | :115, :141, :431, :637, :652, :677, :692, :737, :762, :785, :800, :895, :983, :2066, :2527 | Update status, dates, quantities on production run tasks |
| `createWorkEffortAssoc` | :368 | Link sub-production runs |
| `createWorkEffortGoodStandard` | :306, :407, :1532 | Associate products with work efforts |
| `updateWorkEffortGoodStandard` | :1610 | Update product associations |
| `assignPartyToWorkEffort` | :1757 | Assign workers to tasks |
| `createWorkEffortInventoryProduced` | :1987, :2039, :2219, :2272 | Record inventory produced by tasks |
| `createTimeEntry` | :2496 | Log time against tasks |
| `createWorkEffortNote` | :2773 | Add notes to production runs |

**Decoupling approach:** This is the heaviest dependency. Manufacturing uses WorkEffort as its core persistence mechanism for production runs. Two options:
- **Option A (Recommended):** Manufacturing retains WorkEffort as a shared kernel entity, accessed via a `WorkEffortPort` interface (anti-corruption layer). The interface is backed by direct calls initially, replaced by REST/gRPC calls after WorkEffort extraction.
- **Option B:** Manufacturing internalizes its own production-run persistence model, migrating data from WorkEffort tables to manufacturing-owned tables. Higher initial cost but cleaner long-term boundary.

#### 4.3.2 Manufacturing → Product (29 calls)

| Service Called | File:Line | Purpose |
|---------------|-----------|---------|
| `createCostComponent` | ProductionRunServices.java:1071, :1236, :1283, :1318, :1359 | Record manufacturing costs |
| `createInventoryItem` | ProductionRunServices.java:1967, :2019, :2200, :2252 | Create finished goods inventory |
| `createInventoryItemDetail` | ProductionRunServices.java:1979, :2031, :2211, :2264 | Detail inventory movements |
| `balanceInventoryItems` | ProductionRunServices.java:1995, :2052, :2228, :2281 | Rebalance inventory after production |
| `getProductInventoryAvailable` | MrpServices.java:529 | MRP inventory check |
| `getInventoryAvailableByFacility` | MrpServices.java:531, ProductionRunServices.java:2862 | Facility-level inventory check |
| `getProductCost` | ProductionRunServices.java:1893, :3535, :3575 | Look up product costs for production costing |
| `createProductFacility` | ProductionRunServices.java:1937 | Associate produced product with facility |
| `createLot` | ProductionRunServices.java:1854 | Create lot tracking records |
| `getMktgPackagesAvailable` | ProductionRunServices.java:2888 | Check marketing package availability |
| `getProductVariant` | BOMNode.java:297 | Resolve product variants in BOM |
| `createShipmentPackage` | BOMServices.java:727 | Create packages for BOM shipments |
| `createShipmentPackageContent` | BOMServices.java:755 | Associate content with packages |

**Decoupling approach:** Define a `ProductPort` interface with methods like `getInventoryAvailable()`, `createCostComponent()`, `createInventoryItem()`. Initially backed by the ServiceDispatcher, later by REST/gRPC calls to the Product service.

#### 4.3.3 Manufacturing → Order (2 calls)

| Service Called | File:Line | Purpose |
|---------------|-----------|---------|
| `createRequirement` | ProposedOrder.java:268 | MRP creates purchase requirements |
| `updateRequirement` | ProductionRunServices.java:2558 | Update requirement status after production run |

**Decoupling approach:** Publish `RequirementNeeded` / `RequirementUpdated` domain events. Order module subscribes to create/update requirements.

#### 4.3.4 Manufacturing → Accounting (2 calls)

| Service Called | File:Line | Purpose |
|---------------|-----------|---------|
| `getPartyAccountingPreferences` | ProductionRunServices.java:1023, :1884 | Get cost currency for production costing |

**Decoupling approach:** Manufacturing can cache accounting preferences locally (read-model replication) or call an Accounting API endpoint.

### 4.4 Inbound Dependency Catalog

Only **10 inbound coupling points** exist for manufacturing:

#### 4.4.1 Inbound Dispatcher Calls (3 calls, all from Order)

| Caller Module | Service Called | File:Line |
|---------------|---------------|-----------|
| order | `createProductionRunFromConfiguration` | `order/src/main/java/org/apache/ofbiz/order/shoppingcart/CheckOutHelper.java:753` |
| order | `createProductionRunForMktgPkg` | `order/src/main/java/org/apache/ofbiz/order/order/OrderServices.java:1359` |
| order | `createProductionRunForMktgPkg` | `order/src/main/java/org/apache/ofbiz/order/order/OrderServices.java:1460` |

**Decoupling approach:** Order calls manufacturing via REST/gRPC API instead of direct dispatch. Manufacturing exposes `POST /api/manufacturing/production-runs` endpoint.

#### 4.4.2 Inbound SECA Triggers (6 triggers)

| Trigger Service (Owner) | Action Service (Manufacturing) | Defined In |
|--------------------------|-------------------------------|------------|
| `updateRequirement` (order) | `createProductionRunFromRequirement` | `manufacturing/servicedef/secas.xml` |
| `createRequirement` (order) | `createProductionRunFromRequirement` | `manufacturing/servicedef/secas.xml` |
| `createBOMAssoc` (manufacturing) | `updateLowLevelCode` | `manufacturing/servicedef/secas.xml` |
| `deleteProductAssoc` (product) | `updateLowLevelCode` | `manufacturing/servicedef/secas.xml` |
| `createShipmentReceipt` (product) | `checkDecomposeInventoryItem` | `product/servicedef/secas_shipment.xml` |
| `createShipmentReceipt` (product) | `checkDecomposeInventoryItem` | `product/servicedef/secas_shipment.xml` |

**Decoupling approach:**
- `createRequirement`/`updateRequirement` → Manufacturing subscribes to `RequirementCreated`/`RequirementUpdated` events from the event bus.
- `deleteProductAssoc` → Manufacturing subscribes to `ProductAssocDeleted` event.
- `createShipmentReceipt` → Manufacturing subscribes to `ShipmentReceiptCreated` event.
- `createBOMAssoc` → Internal SECA (both trigger and action are in manufacturing), no cross-domain issue.

### 4.5 Entity Dependency Catalog

Manufacturing code accesses entities from **5 other domains** (109 total accesses):

| Target Domain | Access Count | Unique Entities | Key Entities |
|---------------|-------------|-----------------|-------------|
| **product** | 39 | 17 | `Product`, `ProductAssoc`, `InventoryItem`, `InventoryItemDetail`, `CostComponent`, `Facility`, `ProductFacility`, `Lot`, `Shipment`, `ShipmentBoxType` |
| **workeffort** | 32 | 10 | `WorkEffort`, `WorkEffortAssoc`, `WorkEffortGoodStandard`, `WorkEffortPartyAssignment`, `WorkEffortAndGoods`, `WorkEffortAndInventoryAssign` |
| **order** | 25 | 13 | `OrderHeader`, `OrderItem`, `OrderItemShipGroup`, `Requirement`, `OrderDeliverySchedule`, `WorkAndOrderItemFulfillment` |
| **marketing** | 2 | 2 | `SalesForecast`, `SalesForecastDetail` |
| **accounting** | 2 | 1 | `RateAmount` |

**Decoupling approach:**
- **Product/WorkEffort entities:** Replace with API calls to Product and WorkEffort services. For read-heavy patterns (e.g., BOM traversal looking up `ProductAssoc`), consider a local read model populated via event-driven replication.
- **Order entities:** Replace with Order API calls for `OrderHeader`, `OrderItem` lookups. The `Requirement` entity should become part of a shared Order API.
- **Marketing entities:** `SalesForecast`/`SalesForecastDetail` are only used in MRP; replace with a Marketing API call.
- **Accounting entities:** `RateAmount` used for cost calculations; replace with Accounting API call.

### 4.6 Java Import Dependencies

Manufacturing imports classes from **3 other modules** (11 imports total):

| Target Module | Count | Key Classes |
|---------------|-------|-------------|
| **order** | 6 | `OrderReadHelper` (BOMServices, reports) |
| **product** | 4 | `ProductConfigWrapper`, `ProductConfigWrapper.ConfigOption`, `ProductWorker` |
| **content** | 1 | `ContentWorker` (for production run content in Groovy) |

**Decoupling approach:** These are all utility/helper class dependencies. Replace with:
- A thin adapter layer that exposes the needed methods
- DTOs returned from API calls instead of importing domain classes directly

### 4.7 Transaction Boundary Changes

Currently, manufacturing operations share transactions with WorkEffort and Product operations. Key transaction boundaries that would need to change:

| Current Flow | Current TX | Proposed Pattern |
|-------------|-----------|-----------------|
| `createProductionRun` → `createWorkEffort` + `createWorkEffortGoodStandard` | Single TX | **Saga:** Manufacturing creates local record, then calls WorkEffort API. Compensation: cancel work effort if manufacturing record fails. |
| `productionRunProduce` → `createInventoryItem` + `createInventoryItemDetail` + `createWorkEffortInventoryProduced` | Single TX | **Saga:** Manufacturing records production, then calls Product API to create inventory. Compensation: reverse inventory if production record fails. |
| `createProductionRunTaskCosts` → `createCostComponent` (×5) | Single TX | **Saga:** Manufacturing records cost data, then calls Product API. Compensation: delete cost components if manufacturing update fails. |
| `quickRunProductionRunTask` → `issueInventory` + `updateWorkEffort` + `produce` | Single TX | **Orchestrated saga** with compensation for each step. |

### 4.8 Implementation Roadmap

| Step | Description | Effort | Risk |
|------|-------------|--------|------|
| 1 | Define `WorkEffortPort`, `ProductPort`, `OrderPort` interfaces in manufacturing | 1 week | Low |
| 2 | Implement interfaces using current ServiceDispatcher (no behavior change) | 1 week | Low |
| 3 | Extract manufacturing into a separate Gradle module with defined API | 2 weeks | Medium |
| 4 | Replace SECA triggers with event bus subscriptions | 2 weeks | Medium |
| 5 | Implement REST/gRPC endpoints for manufacturing's public API | 1 week | Low |
| 6 | Switch port implementations to REST/gRPC clients | 2 weeks | High |
| 7 | Migrate manufacturing entities to separate database schema | 2 weeks | High |
| 8 | Integration testing and cutover | 2 weeks | High |

**Estimated total: 13 weeks** for full extraction.

---

## 5. Phase 2: Accounting Isolation

### 5.1 Current Coupling Profile

| Direction | Dispatcher | SECA | Imports | Entity | Total |
|-----------|-----------|------|---------|--------|-------|
| **Accounting → Order** | 19 | 1 | 29 | 49 | **98** |
| **Accounting → Party** | 9 | 3 | 6 | 28 | **46** |
| **Accounting → Product** | 1 | 0 | 13 | 30 | **44** |
| **Order → Accounting** | 30 | 5 | 4 | 24 | **63** |
| **Product → Accounting** | 1 | 17 | 0 | 0 | **18** |
| **Party → Accounting** | 0 | 2 | 7 | 0 | **9** |
| **WorkEffort → Accounting** | 0 | 2 | 0 | 0 | **2** |

### 5.2 The Ledger SECA Chain (Key Decoupling Target)

The most architecturally significant coupling is Accounting's **reactive ledger SECA system** defined in `accounting/servicedef/secas_ledger.xml`. These SECAs listen to events from Product, Order, and WorkEffort and create GL accounting transactions:

| Trigger Service (Owner) | Accounting Action | Domain Event Replacement |
|--------------------------|-------------------|-------------------------|
| `createItemIssuance` (product) | `createAcctgTransForSalesShipmentIssuance` | `ItemIssued` event |
| `cancelOrderItemIssuanceFromSalesShipment` (product) | `createAcctgTransForCanceledSalesShipmentIssuance` | `ItemIssuanceCanceled` event |
| `createShipmentReceipt` (product) | `createAcctgTransForShipmentReceipt` | `ShipmentReceived` event |
| `createCostComponent` (product) | `createAcctgTransForWorkEffortCost` | `CostComponentCreated` event |
| `updateInventoryItem` (product) | `createAcctgTransForInventoryItemOwnerChange` | `InventoryOwnerChanged` event |
| `createInventoryItemDetail` (product) | `createAcctgTransForInventoryItemCostChange` | `InventoryCostChanged` event |
| `createPhysicalInventoryAndVariance` (product) | `createAcctgTransForPhysicalInventoryVariance` | `PhysicalInventoryVariance` event |
| `createItemIssuance` (product) | `createAcctgTransForFixedAssetMaintIssuance` | `FixedAssetItemIssued` event |
| `assignInventoryToWorkEffort` (workeffort) | `createAcctgTransForWorkEffortIssuance` | `WorkEffortInventoryAssigned` event |
| `createWorkEffortInventoryProduced` (workeffort) | `createAcctgTransForWorkEffortInventoryProduced` | `WorkEffortInventoryProduced` event |
| `setInvoiceStatus` (accounting) | `createAcctgTransForPurchaseInvoice` / `createAcctgTransForSalesInvoice` | Internal (same module) |
| `createPayment` / `setPaymentStatus` (accounting) | `createAcctgTransAndEntriesForIncomingPayment` / `...OutgoingPayment` | Internal (same module) |
| `createPaymentApplication` (accounting) | `createAcctgTransAndEntriesForPaymentApplication` | Internal (same module) |

**Decoupling approach:** Replace cross-domain SECA triggers with domain events published to an event bus. Accounting subscribes to these events and creates GL transactions asynchronously (eventual consistency). Internal accounting SECAs (same module trigger/action) can remain as-is initially.

### 5.3 The Shipment→Invoice SECA Chain

This is the most complex cross-domain SECA chain, defined in `product/servicedef/secas_shipment.xml`:

```
updateShipment (status=SHIPMENT_PICKED, type=SALES_SHIPMENT)
  → createInvoicesFromShipment [accounting]

updateShipment (status=SHIPMENT_PACKED, type=SALES_SHIPMENT)
  → createInvoicesFromShipment [accounting]
  → setInvoicesToReadyFromShipment [accounting]

updateShipment (status=PURCH_SHIP_RECEIVED, type=PURCHASE_SHIPMENT)
  → balanceItemIssuancesForShipment [product, internal]
  → createInvoicesFromShipment [accounting]

updateShipment (status=PURCH_SHIP_SHIPPED, type=DROP_SHIPMENT)
  → createInvoicesFromShipment [accounting]

updateShipment (status=PURCH_SHIP_RECEIVED, type=DROP_SHIPMENT)
  → createSalesInvoicesFromDropShipment [accounting]

updateShipment (status=PURCH_SHIP_RECEIVED, type=SALES_RETURN)
  → createInvoicesFromReturnShipment [accounting]

updateShipment (status=SHIPMENT_SHIPPED, type=PURCHASE_RETURN)
  → createInvoicesFromReturnShipment [accounting]
```

**Proposed replacement:** Product publishes `ShipmentStatusChanged` events with shipment type and status. Accounting subscribes and determines which invoice creation logic to invoke based on the event payload. This moves the routing logic from SECA conditions into the Accounting event handler.

### 5.4 Order→Accounting Bidirectional Coupling

#### 5.4.1 Order → Accounting (63 points)

**Dispatcher calls (30):** Primarily from checkout and return processing:
- `CheckOutHelper.java`: `authOrderPayments` (:1198), `createGiftCard` (:592), `processAuthResult` (:1133), `processCaptureResult` (:1156), `calcBillingAccountBalance` (:1746)
- `OrderReturnServices.java`: `createFinAccountForStore` (:851), `refundPayment` (:1417), `createPayment` (:1458, :1599), `createBillingAccount` (:1082)
- `OrderServices.java`: `calcTax` (:1783), `createInvoiceForOrder` (:3438, :3692), `releaseOrderPayments` (:4293)

**SECA triggers (5):** From `order/servicedef/secas.xml`:
- `changeOrderStatus` → `releaseOrderPayments`, `createInvoiceFromOrder`, `createPaymentFromOrder` (×2)
- `updateReturnHeader` → `createInvoiceFromReturn`

**Entity access (24):** Order reads 17 accounting entities including `BillingAccount`, `CreditCard`, `FinAccount`, `PaymentMethod`, `PaymentApplication`.

#### 5.4.2 Accounting → Order (98 points)

**Dispatcher calls (19):**
- `InvoiceServices.java`: `createOrderItemBilling` (:516), `createOrderAdjustmentBilling` (:725, :2543, :2599), `createOrderPaymentPreference` (:1860), `createReturnItemBilling` (:2230)
- `GiftCertificateServices.java`: `createReturnHeader` (:1226), `createReturnItem` (:1256), `updateReturnHeader` (:1295)
- `FinAccountServices.java`: `createReturnHeader` (:422), `createReturnItem` (:442)
- `WorldPayEvents.java`/`PayPalEvents.java`: `createPaymentFromPreference`, `sendOrderConfirmation`

**Imports (29):** `OrderReadHelper` used extensively in accounting reports and payment processing.

**Entity access (49):** Accounting reads 15 order entities including `OrderHeader`, `OrderItem`, `OrderAdjustment`, `ReturnHeader`, `ReturnItem`.

#### 5.4.3 Proposed Decoupling Strategy

**Break accounting→order first** (fewer, more mechanical calls):
1. Accounting's calls to order services (`createOrderItemBilling`, `createOrderAdjustmentBilling`, etc.) are mostly "write-back" patterns where accounting writes billing records against orders. These should become **events published by accounting** (`InvoiceLineCreated`, `BillingRecordCreated`) that Order consumes.
2. Accounting's reads of order entities should be replaced by an **Order Query API** that returns DTOs.
3. Accounting's import of `OrderReadHelper` should be replaced with a local adapter that calls the Order Query API.

**Then break order→accounting:**
1. Order's payment auth/capture calls should go through a **Payment Gateway Port** interface.
2. Order's invoice creation calls should become **events** (`OrderCompleted`, `ReturnApproved`) that Accounting subscribes to.
3. Order's entity reads of billing/payment data should go through an **Accounting Query API**.

### 5.5 Implementation Roadmap

| Step | Description | Effort | Risk |
|------|-------------|--------|------|
| 1 | Convert ledger SECAs to event bus subscribers (using infrastructure from Phase 1) | 3 weeks | Medium |
| 2 | Convert shipment→invoice SECA chain to events | 2 weeks | High |
| 3 | Define Accounting Query API for order/product entity access | 2 weeks | Medium |
| 4 | Break accounting→order dispatcher calls (billing write-backs → events) | 3 weeks | High |
| 5 | Define Payment Gateway Port in order module | 1 week | Low |
| 6 | Break order→accounting dispatcher calls (payment/invoice → port + events) | 3 weeks | High |
| 7 | Migrate accounting entities to separate schema | 3 weeks | High |
| 8 | Integration testing | 3 weeks | High |

**Estimated total: 20 weeks** for full isolation.

---

## 6. Phase 3: Order Extraction

### 6.1 Current Coupling Profile

Order has the highest total coupling (575 points). Key relationships:

| Direction | Total | Primary Coupling Types |
|-----------|-------|----------------------|
| **order→product** | 244 | 106 imports, 93 entity accesses, 37 dispatcher calls, 8 SECAs |
| **order→accounting** | 63 | 30 dispatcher calls, 24 entity accesses |
| **order→party** | 52 | 28 imports, 19 entity accesses |
| **product→order** | 48 | 19 imports, 19 entity accesses, 8 SECAs |
| **accounting→order** | 98 | 29 imports, 49 entity accesses, 19 dispatcher calls |

### 6.2 Order-Product Coupling (Hardest to Break)

The 244-point coupling between Order and Product is the single largest coupling in the system.

**Key coupling patterns:**

1. **ShoppingCart ↔ Product** (most imports):
   - `ShoppingCartItem` imports `ProductConfigWrapper`, `ProductWorker`, `ProductStoreWorker`
   - Cart items call `calculateProductPrice`, `isStoreInventoryAvailableOrNotRequired`, `calculatePurchasePrice`
   - Cart directly reads `Product`, `ProductAssoc`, `ProductPrice`, `ProductFacility` entities

2. **Inventory Reservation Flow:**
   - `storeOrder` (order) → SECA → `balanceOrderItemsWithNegativeReservations` (product)
   - `storeOrder` (order) → SECA → `setOrderReservationPriority` (product)
   - `changeOrderItemStatus` (order) → SECA → `cancelOrderInventoryReservation` (product)

3. **Receipt/Shipment ↔ Order:**
   - `receiveInventoryProduct` (product) → SECA → `addProductsBackToCategory` (order), `setUnitPriceAsLastPrice` (order)
   - `createItemIssuance` (product) → SECA → `checkCreateStockRequirementQoh` (order)
   - `createShipmentReceipt` (product) → SECA → `updateReturnStatusFromReceipt` (order), `updateOrderStatusFromReceipt` (order)

**Proposed decoupling strategy:**

1. **Extract a Product Catalog API** that Order calls for pricing, availability, and product data. This replaces the 106 direct imports and 93 entity accesses.
2. **Inventory reservation becomes event-driven:** Order publishes `OrderPlaced` event; Product subscribes to reserve inventory. Product publishes `InventoryReserved`/`ReservationFailed` events.
3. **Shipment receipt → order status updates become events:** Product publishes `ShipmentReceiptCreated`; Order subscribes to update return/order statuses.

### 6.3 Implementation Roadmap

| Step | Description | Effort | Risk |
|------|-------------|--------|------|
| 1 | Define Product Catalog API (pricing, availability, product data) | 3 weeks | Medium |
| 2 | Replace ShoppingCart product imports with API-backed adapters | 4 weeks | Very High |
| 3 | Convert inventory reservation to event-driven flow | 3 weeks | High |
| 4 | Convert shipment/receipt → order SECAs to events | 2 weeks | Medium |
| 5 | Break order→party coupling (Query API for contact/party data) | 2 weeks | Medium |
| 6 | Break remaining order→accounting coupling (using Phase 2 infrastructure) | 1 week | Low |
| 7 | Migrate order entities to separate schema | 3 weeks | High |
| 8 | Integration testing and saga implementation | 4 weeks | Very High |

**Estimated total: 22 weeks** for full extraction.

---

## 7. Infrastructure Requirements

### 7.1 Event Bus / Message Broker

**Purpose:** Replace SECA cross-domain triggers with asynchronous domain events.

**Requirements:**
- At-least-once delivery guarantees (to match current SECA commit behavior)
- Topic-based routing (one topic per domain event type)
- Dead-letter queue for failed event processing
- Event replay capability for rebuilding read models

**Recommended:** Apache Kafka or RabbitMQ with durable queues. Kafka preferred for its log-based replay capability.

**Event catalog (initial, from SECA analysis):**

| Event | Publisher | Subscribers |
|-------|-----------|-------------|
| `ItemIssued` | product | accounting (GL), order (stock requirements) |
| `ShipmentStatusChanged` | product | accounting (invoicing), order (status updates) |
| `ShipmentReceiptCreated` | product | accounting (GL), order (return/order status), manufacturing (decompose) |
| `InventoryOwnerChanged` | product | accounting (GL) |
| `CostComponentCreated` | product | accounting (GL) |
| `OrderStatusChanged` | order | accounting (payment release, invoicing), product (inventory reservation) |
| `ReturnStatusChanged` | order | accounting (return invoicing), marketing (tracking) |
| `RequirementCreated` | order | manufacturing (auto-production runs) |
| `WorkEffortInventoryProduced` | workeffort | accounting (GL) |
| `ProductAssocDeleted` | product | manufacturing (BOM low-level codes) |

### 7.2 API Gateway / Inter-Service Communication

**Purpose:** Replace in-process `ServiceDispatcher` calls with network calls for extracted modules.

**Requirements:**
- Service routing by module name
- Request/response transformation (OFBiz Map-based service parameters → REST JSON)
- Circuit breaker pattern for resilience
- Request tracing (correlation IDs)

**Recommended approach:**
1. **Phase 1:** Direct REST calls between modules (Spring WebFlux or JAX-RS).
2. **Phase 2:** API gateway (e.g., Spring Cloud Gateway) for routing, auth, and rate limiting.
3. **Phase 3:** Consider gRPC for high-throughput internal calls (BOM traversal, MRP calculations).

### 7.3 Shared Authentication/Authorization

**Current state:** OFBiz uses a single `UserLogin` entity and in-process auth checks. Services check `userLogin` map parameter.

**Requirements:**
- Centralized auth service that issues JWT tokens
- Each extracted module validates JWTs locally
- Service-to-service auth via mutual TLS or API keys
- Permission model migration from OFBiz's `SecurityPermission`/`SecurityGroup` entities

**Recommended:** Extract the security framework services into a standalone Auth service. Issue JWTs containing OFBiz permission sets. Modules validate tokens using a shared JWT library.

### 7.4 Database Splitting Strategy

**Current state:** Single database with ~1,124 entities across all domains.

**Migration path:**

| Phase | Strategy | Risk |
|-------|----------|------|
| **0. Baseline** | Single shared database, all modules | Current state |
| **1. Logical separation** | Separate schemas within same DB instance per extracted module | Low |
| **2. Read replicas** | Cross-domain entity access goes through read replicas | Medium |
| **3. Physical separation** | Each module gets its own database instance | High |
| **4. Data ownership** | Cross-domain data access only via APIs, no shared tables | Highest |

**Key challenges:**
- **Shared entities:** `Party`, `Product`, `WorkEffort` are referenced by almost every module. These become shared-kernel entities with a single owner and read-only replicas.
- **View entities:** OFBiz defines many cross-domain view entities (e.g., `OrderHeaderAndItems`, `AgreementItemAndProductAppl`). These must be replaced with API-composed queries.
- **Foreign keys:** Current DB has cross-domain foreign keys. These must be relaxed and replaced with application-level referential integrity.

### 7.5 Service Registry

**Purpose:** Replace the in-process `ServiceDispatcher` which knows all services via XML definitions.

**Requirements:**
- Runtime service discovery (which module hosts which service)
- Health checking
- Version management for API evolution

**Recommended:** Start with static configuration (service→URL mapping in each module's config). Migrate to a service registry (Consul, Eureka) as more modules are extracted.

### 7.6 Distributed Tracing and Monitoring

**Requirements:**
- Correlation IDs across service calls (currently implicit via thread-local in single JVM)
- Distributed tracing (Jaeger/Zipkin)
- Centralized logging
- Health checks and dashboards per module

---

## 8. Risks and Mitigations

| # | Risk | Likelihood | Impact | Mitigation |
|---|------|-----------|--------|------------|
| 1 | **Transaction consistency loss** — Current shared transactions become distributed | High | Critical | Implement saga patterns for key flows (production→inventory, order→payment). Start with the Orchestrated Saga pattern. Design compensating transactions for each cross-domain operation. |
| 2 | **Performance degradation** — Network calls replace in-process method calls | High | High | Use async/event-driven patterns where possible. Implement caching layers for frequently-read cross-domain data (product catalog, party info). Consider gRPC for latency-sensitive internal calls. |
| 3 | **Data consistency during migration** — Dual-write issues during phased extraction | High | High | Use the Strangler Fig pattern: route calls through the new module while keeping the old code as fallback. Implement feature flags for gradual rollout. |
| 4 | **SECA ordering dependencies** — Some SECA chains depend on execution order | Medium | High | Document all SECA execution order dependencies before conversion. Event bus must support ordered processing within a partition (Kafka guarantees this per partition key). |
| 5 | **Incomplete dependency mapping** — Groovy scripts and mini-lang may contain unmapped dependencies | Medium | Medium | Audit Groovy and mini-lang files for cross-domain calls before each phase. This analysis focused on Java; Groovy files should be audited separately. |
| 6 | **Team skill gap** — Team may lack distributed systems experience | Medium | High | Start with Manufacturing (lowest risk). Use the experience to build team skills before tackling harder extractions. Consider bringing in distributed systems expertise for Phases 2-3. |
| 7 | **Plugin compatibility** — Third-party plugins may depend on monolithic architecture | Medium | Medium | Maintain a compatibility adapter layer that translates between the new API-based architecture and the old ServiceDispatcher interface for plugins. |
| 8 | **Testing complexity** — Integration testing across service boundaries | High | Medium | Invest in contract testing (Pact) between modules. Maintain an integration test environment that runs all modules together. |

---

## 9. Appendix: Full Service Dependency Catalog

### A.1 Cross-Domain Dispatcher Calls by Module

#### Accounting → Other Modules (34 outbound cross-domain calls)

| Target | Count | Key Services |
|--------|-------|-------------|
| order | 19 | `createPaymentFromPreference` (2), `sendOrderConfirmation` (2), `createReturnHeader` (2), `createReturnItem` (2), `updateReturnHeader` (3), `createOrderItemBilling`, `createOrderAdjustmentBilling` (3), `createOrderPaymentPreference`, `createReturnItemBilling`, `createSimpleNonProductSalesOrder`, `getReturnableQuantity` |
| party | 9 | `createPostalAddress`, `createPerson`, `createPartyRole`, `createPartyEmailAddress`, `createUpdatePartyTelecomNumber`, `createPartyPostalAddress`, `createPartyContactMechPurpose` (2), `getRelatedParties` |
| product | 1 | `createShipmentItemBilling` |
| framework:common | 4 | `sendMailFromScreen` (4) |

#### Manufacturing → Other Modules (64 outbound cross-domain calls)

| Target | Count | Key Services |
|--------|-------|-------------|
| workeffort | 31 | `updateWorkEffort` (15), `createWorkEffort` (4), `createWorkEffortGoodStandard` (3), `createWorkEffortInventoryProduced` (4), `createWorkEffortAssoc`, `updateWorkEffortGoodStandard`, `assignPartyToWorkEffort`, `createTimeEntry`, `createWorkEffortNote` |
| product | 29 | `createCostComponent` (5), `createInventoryItem` (4), `createInventoryItemDetail` (4), `balanceInventoryItems` (4), `getProductCost` (3), `getInventoryAvailableByFacility` (2), `getProductInventoryAvailable`, `createLot`, `createProductFacility`, `getMktgPackagesAvailable`, `getProductVariant`, `createShipmentPackage`, `createShipmentPackageContent` |
| order | 2 | `createRequirement`, `updateRequirement` |
| accounting | 2 | `getPartyAccountingPreferences` (2) |

#### Order → Other Modules (80 outbound cross-domain calls)

| Target | Count | Key Services |
|--------|-------|-------------|
| product | 37 | `calculateProductPrice` (2), `calculatePurchasePrice`, `isStoreInventoryAvailableOrNotRequired`, `getProductByComprehensiveSearch`, `reserveStoreInventory`, `reserveProductInventory`, and 30+ more |
| accounting | 30 | `authOrderPayments`, `createGiftCard`, `processAuthResult`, `processCaptureResult`, `calcBillingAccountBalance`, `calcTax`, `createInvoiceForOrder` (3), `releaseOrderPayments` (2), `refundPayment`, `createPayment` (3), and 15+ more |
| framework:common | 6 | `sendMailFromScreen` (4), `sendGenericNotificationEmail`, `createNote` |
| content | 3 | `createSurveyResponse`, `sendPrintFromScreen`, `createFileFromScreen` |
| manufacturing | 3 | `createProductionRunFromConfiguration`, `createProductionRunForMktgPkg` (2) |
| workeffort | 1 | `createWorkEffortGoodStandard` |

#### Product → Other Modules (41 outbound cross-domain calls)

| Target | Count | Key Services |
|--------|-------|-------------|
| content | 33 | `createContent` (15+), `createContentAssoc` (5+), `updateContent` (5+), and more |
| framework:common | 16 | `convertUom` (10+), `sendMailFromScreen`, and more |
| party | 5 | `getPartyEmail` (3), `ensurePartyRole`, `createContactMech` |
| order | 2 | `sendOrderBackorderNotification`, `getOrderItemInvoicedAmountAndQuantity` |
| accounting | 1 | `calcTaxForDisplay` |

### A.2 Cross-Domain SECA Triggers by Module

#### Accounting Ledger SECAs (secas_ledger.xml) — 13 cross-domain triggers

| Trigger Service | Owner | Action Service | Event |
|----------------|-------|---------------|-------|
| `createItemIssuance` | product | `createAcctgTransForSalesShipmentIssuance` | commit |
| `cancelOrderItemIssuanceFromSalesShipment` | product | `createAcctgTransForCanceledSalesShipmentIssuance` | commit |
| `createShipmentReceipt` | product | `createAcctgTransForShipmentReceipt` | commit |
| `createCostComponent` | product | `createAcctgTransForWorkEffortCost` | commit |
| `updateInventoryItem` | product | `createAcctgTransForInventoryItemOwnerChange` | commit |
| `createInventoryItemDetail` | product | `createAcctgTransForInventoryItemCostChange` | commit |
| `createPhysicalInventoryAndVariance` | product | `createAcctgTransForPhysicalInventoryVariance` | commit |
| `createItemIssuance` | product | `createAcctgTransForFixedAssetMaintIssuance` | commit |
| `assignInventoryToWorkEffort` | workeffort | `createAcctgTransForWorkEffortIssuance` | commit |
| `createWorkEffortInventoryProduced` | workeffort | `createAcctgTransForWorkEffortInventoryProduced` | commit |

#### Order SECAs (secas.xml) — 32 cross-domain triggers

| Trigger Service | Action Service | Target Module | Event |
|----------------|---------------|---------------|-------|
| `changeOrderStatus` | `releaseOrderPayments` | accounting | commit |
| `changeOrderStatus` | `createInvoiceFromOrder` | accounting | global-commit-post-run |
| `changeOrderStatus` | `createPaymentFromOrder` (×2) | accounting | commit |
| `updateReturnHeader` | `createInvoiceFromReturn` | accounting | commit |
| `changeOrderStatus` | `updateContentSubscriptionByOrder` | content | commit |
| `storeOrder` | `balanceOrderItemsWithNegativeReservations` | product | return |
| `storeOrder` | `setOrderReservationPriority` | product | return |
| `changeOrderItemStatus` | `cancelOrderInventoryReservation` | product | commit |
| `changeOrderItemStatus` | `cancleOrderItemGroupOrder` | product | commit |
| `changeOrderStatus` | `processExtendSubscriptionByOrder` | product | commit |
| `storeOrder` | `checkOrderItemForProductGroupOrder` | product | commit |
| `storeOrder` | `associateOrderWithAllocationPlans` | product | return |
| `changeOrderItemStatus` | `completeAllocationPlanItemByOrderItem` | product | commit |
| `changeOrderItemStatus` | `cancelAllocationPlanItemByOrderItem` | product | commit |
| `updateOrderItems` | `updateAllocatedQuantityOnOrderItemChange` | product | commit |
| `createCustRequest` / `updateCustRequest` | `updateCommunicationEvent` | party | commit |
| `createQuoteRole` / `createRequirementRole` / `createCustRequestParty` | `ensurePartyRole` | party | invoke |
| `createCustRequestItemNote` | `createSystemInfoNote` | commonext | commit |
| `setCustRequestStatus` (×2) / `createCustRequestItemNote` | `sendMailFromTemplateSetting` | framework:common | commit |
| `updateReturnHeader` | `createTrackingCodeOrderReturns` | marketing | commit |
| `createSalesOpportunity` / `updateSalesOpportunity` | `createSalesOpportunityAccountRole` / `createSalesOpportunityLeadRole` | marketing | commit |

#### Product SECAs — 20 cross-domain triggers

| Trigger Service | Action Service | Target Module | Defined In |
|----------------|---------------|---------------|------------|
| `updateShipment` (various statuses) | `createInvoicesFromShipment` (×4) | accounting | secas_shipment.xml |
| `updateShipment` | `setInvoicesToReadyFromShipment` | accounting | secas_shipment.xml |
| `updateShipment` | `createSalesInvoicesFromDropShipment` | accounting | secas_shipment.xml |
| `updateShipment` (×2) | `createInvoicesFromReturnShipment` | accounting | secas_shipment.xml |
| `receiveInventoryProduct` | `updateProductAverageCostOnReceiveInventory` | accounting | secas.xml |
| `createShipmentReceipt` | `checkDecomposeInventoryItem` (×2) | manufacturing | secas_shipment.xml |
| `createShipmentReceipt` | `updateReturnStatusFromReceipt` | order | secas_shipment.xml |
| `createShipmentReceipt` | `updateOrderStatusFromReceipt` | order | secas_shipment.xml |
| `createShipment` (×2) | `checkAndCreateWorkEffort` | workeffort | secas_shipment.xml |
| `addPartyToCategory` / `addPartyToFacility` / `addPartyToFacilityGroup` / `addProdCatalogToParty` | `ensurePartyRole` / `ensureNaPartyRole` | party | secas.xml |
| `createProductContent` | `createContent` | content | secas.xml |
| `deletePartyRole` | `removeImageContentApproval` (party trigger → product action) | product | secas.xml |

#### WorkEffort SECAs — 8 cross-domain triggers

| Trigger Service | Action Service | Target Module |
|----------------|---------------|---------------|
| `createWorkEffortRequest` | `createCustRequest` | order |
| `createWorkEffortRequestItem` | `createCustRequestItem` | order |
| `createWorkEffortQuote` | `createQuote` | order |
| `createWorkRequirementFulfillment` | `createRequirement` | order |
| `createShoppingListWorkEffort` | `createShoppingList` | order |
| `createWorkEffortTimeEntry` | `createTimeEntry` | order |
| `createCommunicationEventWorkEff` | `createCommunicationEvent` | party |
| `createTimesheetRole` | `ensurePartyRole` | party |

### A.3 Cross-Domain Java Imports Summary

| Source → Target | Count | Key Classes Imported |
|----------------|-------|---------------------|
| order → product | 106 | `CatalogWorker`, `ProductConfigWorker`, `ProductConfigWrapper`, `ProductStoreWorker`, `ProductWorker`, `ShipmentCostEstimate`, `ProductSearchSession` |
| accounting → order | 29 | `OrderReadHelper`, `ShoppingCart`, `ShoppingCartItem` |
| order → party | 28 | `ContactHelper`, `ContactMechWorker`, `PartyHelper`, `PartyWorker`, `PartyRelationshipHelper` |
| product → order | 19 | `ShoppingCart`, `ShoppingCartItem`, `CartItemModifyException`, `OrderReadHelper` |
| accounting → product | 13 | `ProductStoreWorker`, `ShipmentReadHelper` |
| product → party | 10 | `ContactMechWorker`, `PartyHelper`, `PartyWorker` |
| party → accounting | 7 | `PaymentWorker`, `InvoiceWorker`, `BillingAccountWorker` |
| manufacturing → order | 6 | `OrderReadHelper` |
| accounting → party | 6 | `PartyWorker`, `PartyHelper` |
| order → content | 4 | `ContentWorker`, `ContentWrapper`, `DataResourceWorker` |
| manufacturing → product | 4 | `ProductConfigWrapper`, `ProductWorker` |
| marketing → party | 4 | `PartyHelper`, `PartyWorker`, `ContactHelper` |
| order → accounting | 4 | `PaymentWorker`, `BillingAccountWorker` |
| product → content | 3 | `ContentWorker`, `LayoutWorker`, `SurveyWrapper` |
| party → content | 3 | `ContentWorker`, `ContentWrapper`, `DataResourceWorker` |
| workeffort → content | 3 | `ContentWorker`, `ContentWrapper`, `ContentKeywordIndex` |

### A.4 Cross-Domain Entity Access Summary

| Source → Target | Access Count | Unique Entities | Top Entities |
|----------------|-------------|-----------------|-------------|
| order → product | 93 | 29 | `Product` (15), `ProductStore` (8), `ProductStoreEmailSetting` (7), `ProductAssoc` (5) |
| accounting → order | 49 | 15 | `OrderHeader` (10), `OrderItem` (8), `ReturnItem` (6), `OrderAdjustment` (5) |
| manufacturing → product | 39 | 17 | `Product` (6), `InventoryItem` (5), `ProductAssoc` (4), `Facility` (3) |
| manufacturing → workeffort | 32 | 10 | `WorkEffort` (8), `WorkEffortGoodStandard` (5), `WorkEffortAssoc` (4) |
| accounting → product | 30 | 11 | `ProductStore` (6), `Product` (5), `ProductStoreEmailSetting` (4) |
| accounting → party | 28 | 11 | `Party` (5), `PostalAddress` (4), `PartyRole` (3) |
| manufacturing → order | 25 | 13 | `OrderHeader` (5), `OrderItem` (4), `Requirement` (3) |
| order → accounting | 24 | 17 | `PaymentMethod` (3), `BillingAccount` (3), `CreditCard` (2), `FinAccount` (2) |
| product → content | 25 | 6 | `Content` (10), `ContentAssoc` (5), `ContentDataResourceView` (4) |
| order → party | 19 | 11 | `Party` (3), `PostalAddress` (3), `PartyRole` (2) |
| product → order | 19 | 8 | `OrderHeader` (5), `OrderItem` (4), `OrderItemShipGroup` (3) |
| product → party | 19 | 9 | `PostalAddress` (4), `Party` (3), `ContactMech` (3) |

---

*Generated from automated analysis of the `trunk` branch of `cogjack/ofbiz-framework`. Service ownership resolved against 3,592 service definitions across all framework and application modules. Entity ownership resolved against 1,124 entity definitions in `applications/datamodel/entitydef/`.*
