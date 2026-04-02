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

### 4.8 Implementation Roadmap (Sub-Phases)

Phase 1 is broken into **7 sub-phases**, each with explicit entry/exit criteria, a human verification checkpoint, and gates for unclear spec decisions.

---

#### Sub-Phase 1A: Port Interface Definition (1 week)

**Goal:** Define anti-corruption-layer interfaces (`WorkEffortPort`, `ProductPort`, `OrderPort`, `AccountingPort`) inside the manufacturing module that abstract all outbound cross-domain calls.

**Tasks:**
1. Create a `ports/` package under `applications/manufacturing/src/main/java/org/apache/ofbiz/manufacturing/ports/`.
2. For each of the 64 outbound dispatcher calls (Section 4.3), define a method on the corresponding port interface. Group by domain:
   - `WorkEffortPort`: 12 methods (create, update, associate work efforts; create time entries/notes; assign parties; record produced inventory).
   - `ProductPort`: 13 methods (inventory queries, cost components, inventory items, lots, shipment packages, product variants).
   - `OrderPort`: 2 methods (create/update requirements).
   - `AccountingPort`: 1 method (get accounting preferences).
3. Define DTOs for cross-domain data returned by port methods — do NOT import classes from other modules in the interface definitions.

**Open spec questions (gate before proceeding):**
- ❓ **WorkEffort shared kernel vs. internalization:** Should manufacturing retain WorkEffort as its persistence model (Option A, Section 4.3.1) or create its own production-run entity model (Option B)? This decision fundamentally shapes the port interfaces. **Recommend: present both options to stakeholders with LOE estimates before committing.**
- ❓ **DTO granularity:** Should port methods return full entity-equivalent DTOs or minimal projections? Full DTOs are easier to implement but leak domain model details.

> **🔲 CHECKPOINT 1A — Human Review Required**
> - [ ] Port interfaces reviewed by architect for completeness (all 64 calls accounted for)
> - [ ] WorkEffort shared-kernel decision made and documented
> - [ ] DTO design approved (full vs. projection)
> - [ ] No `import org.apache.ofbiz.{product|order|workeffort|accounting}.*` in interface definitions

**Exit criteria:** All port interfaces compile. No production code changes yet.

---

#### Sub-Phase 1B: Dispatcher-Backed Port Implementation (1 week)

**Goal:** Implement each port interface using the existing `ServiceDispatcher` — a pure refactor with zero behavior change.

**Tasks:**
1. Create `impl/` package with `DispatcherWorkEffortPort`, `DispatcherProductPort`, `DispatcherOrderPort`, `DispatcherAccountingPort`.
2. Each implementation wraps the existing `dispatcher.runSync("serviceName", ctx)` call pattern.
3. Refactor all 64 call sites in manufacturing Java code to use the port interface instead of direct dispatcher calls.
4. Replace the 11 cross-module Java imports (Section 4.6) with port-mediated access or local DTOs.

**Verification:**
- Run full OFBiz test suite — zero behavior change expected.
- Run manufacturing-specific tests to confirm production run lifecycle works.

> **🔲 CHECKPOINT 1B — Human Review Required**
> - [ ] All existing unit and integration tests pass (no regressions)
> - [ ] Manual smoke test: create a production run, run MRP, produce inventory — all work as before
> - [ ] Code review confirms no direct `dispatcher.runSync` calls remain in manufacturing Java code (except through port implementations)
> - [ ] No new cross-module imports introduced

**Exit criteria:** Manufacturing code uses ports exclusively. All tests green. Behavior identical to pre-refactor.

---

#### Sub-Phase 1C: Gradle Module Extraction (2 weeks)

**Goal:** Extract manufacturing into a separate Gradle sub-project with an explicit dependency declaration.

**Tasks:**
1. Create `applications/manufacturing/build.gradle` with explicit `implementation project(':applications:workeffort')`, etc.
2. Move port interfaces into a separate `manufacturing-api` module (or a `ports` source set) so other modules can depend on manufacturing's API without depending on its implementation.
3. Ensure manufacturing's `servicedef/`, `entitydef/`, `webapp/`, and `config/` resources are bundled with the module.
4. Fix any compile errors from implicit classpath dependencies that were previously resolved by the flat monolith classpath.
5. Audit Groovy files in manufacturing for cross-domain calls not captured in the Java analysis (Risk #5 from Section 8).

**Open spec questions (gate before proceeding):**
- ❓ **Module packaging:** Should the extracted module be a JAR (library) or a WAR (deployable)? JAR is simpler for Phase 1; WAR needed for independent deployment later.
- ❓ **Shared framework dependency:** How should manufacturing depend on `framework/*` modules (entity engine, service engine)? Direct dependency or via a `framework-api` abstraction?

> **🔲 CHECKPOINT 1C — Human Review Required**
> - [ ] `./gradlew :applications:manufacturing:build` succeeds independently
> - [ ] Full `./gradlew test` passes across all modules
> - [ ] Dependency graph review: no circular dependencies, manufacturing depends only on declared modules
> - [ ] Groovy/mini-lang audit results reviewed — any newly discovered cross-domain calls documented
> - [ ] Module packaging decision made (JAR vs. WAR)

**Exit criteria:** Manufacturing compiles and tests as a separate Gradle module. Dependency graph is explicit and acyclic.

---

#### Sub-Phase 1D: Event Bus Infrastructure + SECA Migration (2 weeks)

**Goal:** Stand up the event bus infrastructure and convert the 6 inbound SECA triggers (Section 4.4.2) to event-bus subscriptions.

**Tasks:**
1. **Event bus setup:** Deploy message broker (Kafka or RabbitMQ). Create topics for initial events: `RequirementCreated`, `RequirementUpdated`, `ProductAssocDeleted`, `ShipmentReceiptCreated`.
2. **Publisher side:** Add event publishing to the trigger services in Order and Product modules:
   - `createRequirement` → publish `RequirementCreated`
   - `updateRequirement` → publish `RequirementUpdated`
   - `deleteProductAssoc` → publish `ProductAssocDeleted`
   - `createShipmentReceipt` → publish `ShipmentReceiptCreated`
3. **Subscriber side:** Manufacturing subscribes to these events and invokes the same actions that the SECAs currently trigger (`createProductionRunFromRequirement`, `updateLowLevelCode`, `checkDecomposeInventoryItem`).
4. **Dual-run period:** Keep SECAs active alongside event-bus subscribers with idempotency guards. Verify both paths produce identical results.
5. **Disable SECAs:** Once validated, remove the cross-domain SECA entries from `manufacturing/servicedef/secas.xml` and `product/servicedef/secas_shipment.xml`.

**Open spec questions (gate before proceeding):**
- ❓ **Broker choice:** Kafka vs. RabbitMQ? Kafka provides ordered replay but requires more operational overhead. RabbitMQ is simpler but lacks replay.
- ❓ **Event schema format:** Avro, Protobuf, or JSON? JSON is simplest to start; Avro/Protobuf enable schema evolution.
- ❓ **Consistency model:** Current SECAs run synchronously within the same transaction. Moving to async means eventual consistency. Is this acceptable for `createProductionRunFromRequirement` (which currently runs in the same TX as `createRequirement`)?

> **🔲 CHECKPOINT 1D — Human Review Required**
> - [ ] Event bus is operational (health checks, topic creation verified)
> - [ ] Dual-run validation: for each of the 6 SECA triggers, confirm that the event-bus subscriber produces the same outcome as the SECA
> - [ ] Consistency model reviewed and accepted by business stakeholders (sync → eventual consistency tradeoff)
> - [ ] SECA removal diff reviewed — only cross-domain SECAs removed, internal manufacturing SECAs (`createBOMAssoc` → `updateLowLevelCode`) remain
> - [ ] Dead-letter queue monitoring in place for failed events

**Exit criteria:** All 6 cross-domain SECA triggers replaced by event-bus subscriptions. SECAs disabled. Manufacturing reacts to domain events correctly.

---

#### Sub-Phase 1E: Manufacturing Public REST API (1 week)

**Goal:** Expose manufacturing's 81 services as a versioned REST API for external callers.

**Tasks:**
1. Define REST endpoints for the 3 inbound dispatcher calls (Section 4.4.1):
   - `POST /api/v1/manufacturing/production-runs` (replaces `createProductionRun`, `createProductionRunFromConfiguration`)
   - `POST /api/v1/manufacturing/production-runs/marketing-package` (replaces `createProductionRunForMktgPkg`)
2. Implement thin REST controllers that delegate to the existing service implementations.
3. Add OpenAPI/Swagger documentation.
4. Implement request/response DTOs (not raw OFBiz Map parameters).

> **🔲 CHECKPOINT 1E — Human Review Required**
> - [ ] API design review: REST endpoints follow conventions, request/response schemas documented
> - [ ] OpenAPI spec reviewed by API consumers (Order team)
> - [ ] Integration test: Order module can call manufacturing REST API and get correct responses
> - [ ] Auth/authz: API endpoints enforce the same permissions as the current dispatcher calls

**Exit criteria:** Manufacturing has a functional REST API. Order module has been tested calling it (but still uses dispatcher in production).

---

#### Sub-Phase 1F: Cut Over Order→Manufacturing to REST (2 weeks)

**Goal:** Switch the 3 inbound dispatcher calls from Order to use the REST API instead of in-process dispatch.

**Tasks:**
1. Modify Order's `CheckOutHelper.java:753` to call manufacturing REST endpoint instead of `dispatcher.runSync("createProductionRunFromConfiguration", ...)`.
2. Modify Order's `OrderServices.java:1359, :1460` to call manufacturing REST endpoint instead of `dispatcher.runSync("createProductionRunForMktgPkg", ...)`.
3. Implement circuit breaker and retry logic for the REST calls.
4. **Feature flag:** Gate the REST path behind a feature flag so it can be toggled back to dispatcher in production if issues arise.
5. Switch manufacturing's outbound port implementations from `DispatcherXxxPort` to `RestXxxPort` (calls Product, WorkEffort, Order, Accounting via REST).

**Open spec questions (gate before proceeding):**
- ❓ **Transaction semantics:** The current `createProductionRunFromConfiguration` runs in the same transaction as order checkout. After extraction, this becomes a distributed call. What happens if manufacturing fails after order is committed? Need saga/compensation or accept the risk?
- ❓ **Latency budget:** How much additional latency from the REST call is acceptable in the checkout flow?

> **🔲 CHECKPOINT 1F — Human Review Required**
> - [ ] Feature flag tested: both dispatcher and REST paths work
> - [ ] Load testing: REST path meets latency requirements under expected load
> - [ ] Failure testing: circuit breaker triggers correctly when manufacturing service is down
> - [ ] Saga/compensation design reviewed for checkout→production-run flow
> - [ ] Staging environment validation: full order→production flow works end-to-end via REST

**Exit criteria:** Order→Manufacturing communication works via REST in staging. Feature flag allows instant rollback.

---

#### Sub-Phase 1G: Database Schema Separation + Production Cutover (2 weeks)

**Goal:** Migrate manufacturing entities to a separate database schema and complete the extraction.

**Tasks:**
1. Create a `manufacturing` schema in the database.
2. Migrate manufacturing-owned entities (8 entities: `TechDataCalendar*`, `MrpEvent*`, `ProductManufacturingRule`) to the new schema.
3. Update manufacturing's `entitydef/` to point to the new schema.
4. Replace manufacturing's remaining direct entity reads of cross-domain entities (109 accesses, Section 4.5) with port-mediated API calls.
5. Create read-model replicas for high-frequency cross-domain reads (e.g., `Product`, `WorkEffort` data needed for BOM traversal).
6. Production cutover: enable the REST path feature flag in production, monitor for 1 week.

**Open spec questions (gate before proceeding):**
- ❓ **Entity ownership clarity:** Some entities like `TechDataCalendarExcDay` are accessed by Order (Section 3.1). After migration, Order needs to call manufacturing's API for this data. Confirm this is acceptable.
- ❓ **Read-model freshness:** How stale can replicated Product/WorkEffort data be? Real-time (event-driven) vs. periodic sync?
- ❓ **Rollback plan:** If issues arise in production, what is the rollback procedure? Feature flag covers API calls, but schema migration needs a separate rollback plan.

> **🔲 CHECKPOINT 1G — Human Review Required (Production Go/No-Go)**
> - [ ] Schema migration tested in staging — all manufacturing functions work against new schema
> - [ ] Read-model replication validated — BOM traversal performance acceptable
> - [ ] Rollback plan documented and tested
> - [ ] Production monitoring dashboards in place (error rates, latency, event processing lag)
> - [ ] 1-week bake period in production with feature flag — no incidents
> - [ ] **Sign-off from engineering lead and product owner to declare Phase 1 complete**

**Exit criteria:** Manufacturing runs as a separable module in production. All cross-domain communication is via APIs and events. Feature flag removed after bake period.

**Estimated total: 11 weeks development + 1 week bake = ~12 weeks** for full Phase 1 extraction.

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

### 5.5 Implementation Roadmap (Sub-Phases)

Phase 2 is broken into **8 sub-phases**. This phase is significantly harder than Phase 1 due to bidirectional coupling and the financial-correctness requirements of accounting.

---

#### Sub-Phase 2A: Ledger SECA → Event Bus Migration (3 weeks)

**Goal:** Convert the 10 cross-domain ledger SECAs (Section 5.2) from synchronous SECA triggers to event-bus subscriptions.

**Tasks:**
1. Add event publishing to the 10 trigger services in Product and WorkEffort modules (e.g., `createItemIssuance` publishes `ItemIssued` event).
2. Create accounting event subscribers that invoke the same GL-posting logic (e.g., `ItemIssued` → `createAcctgTransForSalesShipmentIssuance`).
3. Dual-run with idempotency: run both SECA and event subscriber in parallel, verify GL entries match.
4. Disable ledger SECAs after validation.

**Open spec questions (gate before proceeding):**
- ❓ **GL consistency:** Current ledger SECAs run synchronously within the committing transaction, guaranteeing that every inventory change has a corresponding GL entry. Moving to async means GL entries could lag. Is eventual consistency acceptable for GL postings? **This is a critical business decision — may require CFO/finance stakeholder input.**
- ❓ **Reconciliation:** If events are processed out of order or duplicated, GL entries could be incorrect. What reconciliation process is needed?
- ❓ **Audit trail:** Current SECAs leave no trace of the coupling. Events provide better auditability, but the transition period needs careful logging.

> **🔲 CHECKPOINT 2A — Human Review Required**
> - [ ] Dual-run results: GL entries from SECA path match event-bus path for all 10 trigger types
> - [ ] Finance/accounting stakeholder sign-off on eventual consistency for GL postings
> - [ ] Reconciliation process documented and tested
> - [ ] Event ordering verified: GL entries created in correct chronological order
> - [ ] Dead-letter monitoring: no events stuck in DLQ after 48-hour test run

---

#### Sub-Phase 2B: Shipment→Invoice SECA Chain Migration (2 weeks)

**Goal:** Convert the 7-variant shipment→invoice SECA chain (Section 5.3) to event-driven flow.

**Tasks:**
1. Product publishes `ShipmentStatusChanged` event with `{shipmentId, shipmentTypeId, statusId}` payload.
2. Accounting subscribes and routes to the correct invoice-creation service based on shipment type and status:
   - SALES_SHIPMENT + PACKED → `createInvoicesFromShipment` + `setInvoicesToReadyFromShipment`
   - PURCHASE_SHIPMENT + RECEIVED → `createInvoicesFromShipment`
   - DROP_SHIPMENT + SHIPPED → `createInvoicesFromShipment`
   - DROP_SHIPMENT + RECEIVED → `createSalesInvoicesFromDropShipment`
   - SALES_RETURN + RECEIVED → `createInvoicesFromReturnShipment`
   - PURCHASE_RETURN + SHIPPED → `createInvoicesFromReturnShipment`
3. Dual-run validation for each of the 7 variants.
4. Disable shipment SECAs in `product/servicedef/secas_shipment.xml`.

**Open spec questions (gate before proceeding):**
- ❓ **Invoice timing:** Currently, invoices are created synchronously when shipment status changes. If the event bus introduces delay, customers could see shipment confirmations before invoices exist. Is this acceptable? What is the maximum acceptable delay?
- ❓ **SHIPMENT_PICKED vs. SHIPMENT_PACKED:** Both trigger `createInvoicesFromShipment` for SALES_SHIPMENT. Is this intentional (idempotent) or a bug in the current SECA config? Need clarification before migrating.

> **🔲 CHECKPOINT 2B — Human Review Required**
> - [ ] All 7 shipment→invoice variants validated via dual-run
> - [ ] Invoice timing requirements confirmed with business stakeholders
> - [ ] PICKED/PACKED double-trigger question resolved
> - [ ] End-to-end test: create sales order → ship → verify invoice auto-created via event bus

---

#### Sub-Phase 2C: Accounting Query API (2 weeks)

**Goal:** Create a read-only API that other modules use to query accounting data, replacing direct entity access.

**Tasks:**
1. Define REST endpoints for accounting data that other modules currently read via entity access:
   - `GET /api/v1/accounting/billing-accounts/{id}` (used by Order: 3 entity accesses)
   - `GET /api/v1/accounting/payment-methods/{id}` (used by Order: 3 entity accesses)
   - `GET /api/v1/accounting/party-accounting-preferences/{partyId}` (used by Manufacturing, Order)
   - `GET /api/v1/accounting/fin-accounts/{id}` (used by Order: 2 entity accesses)
2. Create DTOs for each endpoint response.
3. Update Order module to call Accounting Query API instead of reading accounting entities directly (24 entity accesses, Section 5.4.1).
4. Update Manufacturing to use the API for `RateAmount` lookups (2 entity accesses, Section 4.5).

> **🔲 CHECKPOINT 2C — Human Review Required**
> - [ ] API design review: all 17 accounting entity types accessed by Order are covered
> - [ ] Performance test: API response times acceptable vs. direct entity reads
> - [ ] No direct reads of accounting-owned entities remain in Order or Manufacturing code

---

#### Sub-Phase 2D: Break Accounting→Order Coupling (3 weeks)

**Goal:** Eliminate accounting's 19 dispatcher calls to order services and 49 entity accesses to order data.

**Tasks:**
1. **Billing write-backs (6 calls):** Convert `createOrderItemBilling`, `createOrderAdjustmentBilling`, `createReturnItemBilling` to events. Accounting publishes `InvoiceLineCreated`; Order subscribes to create billing records.
2. **Return management (6 calls):** Convert `createReturnHeader`, `createReturnItem`, `updateReturnHeader` calls in `GiftCertificateServices.java` and `FinAccountServices.java` to an Order Command API (`POST /api/v1/order/returns`).
3. **Payment-from-preference (3 calls):** Convert `createPaymentFromPreference` in payment gateway handlers to an event: Accounting publishes `PaymentReceived`; Order subscribes to update preference.
4. **Order confirmation (2 calls):** Convert `sendOrderConfirmation` to an event: Accounting publishes `PaymentConfirmed`; Order subscribes to send notification.
5. **Entity access replacement:** Create an Order Query API for accounting's 49 entity reads (OrderHeader, OrderItem, ReturnHeader, etc.).
6. **Import elimination:** Replace 29 imports of `OrderReadHelper`, `ShoppingCart`, etc. with local adapters backed by the Order Query API.

**Open spec questions (gate before proceeding):**
- ❓ **Return creation from accounting:** Currently, gift certificate and financial account services create returns directly. After decoupling, should accounting still be able to initiate returns, or should this be a manual process handled in the Order module?
- ❓ **Payment gateway integration ownership:** WorldPay and PayPal event handlers live in Accounting but call Order services. Should these handlers move to Order, or should they stay in Accounting and use the Order Command API?

> **🔲 CHECKPOINT 2D — Human Review Required**
> - [ ] Return-creation ownership decision documented
> - [ ] Payment gateway handler ownership decision documented
> - [ ] All 19 accounting→order dispatcher calls eliminated
> - [ ] OrderReadHelper no longer imported by accounting code
> - [ ] Integration test: full invoice lifecycle (create invoice, apply to order, post to GL) works end-to-end

---

#### Sub-Phase 2E: Payment Gateway Port in Order (1 week)

**Goal:** Abstract Order's 30 outbound calls to accounting payment services behind a `PaymentGatewayPort` interface.

**Tasks:**
1. Define `PaymentGatewayPort` in Order module with methods for: `authorizePayment`, `capturePayment`, `refundPayment`, `releasePayment`, `createGiftCard`, `calcBillingAccountBalance`, `calcTax`, `createInvoice`, `createPayment`, `createFinAccount`, etc.
2. Implement `DispatcherPaymentGatewayPort` using current ServiceDispatcher (no behavior change).
3. Refactor all 30 call sites in Order to use the port.

> **🔲 CHECKPOINT 2E — Human Review Required**
> - [ ] Port interface covers all 30 order→accounting dispatcher calls
> - [ ] All existing tests pass (zero behavior change)
> - [ ] No direct `dispatcher.runSync` calls to accounting services remain in Order code

---

#### Sub-Phase 2F: Break Order→Accounting Coupling (3 weeks)

**Goal:** Switch Order's `PaymentGatewayPort` from dispatcher-backed to REST/event-backed.

**Tasks:**
1. **Payment auth/capture (synchronous, latency-sensitive):** Implement `RestPaymentGatewayPort` calling Accounting's REST API for `authOrderPayments`, `processAuthResult`, `processCaptureResult`.
2. **Invoice creation (can be async):** Convert `createInvoiceForOrder` (3 calls) and `createInvoiceFromReturn` to events: Order publishes `OrderApproved` / `ReturnApproved`; Accounting subscribes.
3. **SECA migration:** Convert the 5 order→accounting SECAs (`changeOrderStatus` → `releaseOrderPayments`, `createInvoiceFromOrder`, `createPaymentFromOrder`) to events.
4. **Entity access replacement:** Order calls Accounting Query API (from 2C) instead of reading billing/payment entities directly.
5. Feature-flag the REST/event path for rollback capability.

**Open spec questions (gate before proceeding):**
- ❓ **Checkout latency:** Payment authorization is on the critical checkout path. REST call to accounting adds latency. What is the maximum acceptable checkout time? Should we consider keeping payment auth as a shared-kernel synchronous call?
- ❓ **Payment release timing:** `releaseOrderPayments` currently fires synchronously on `changeOrderStatus`. If async, a canceled order's payment hold could linger. Acceptable delay?

> **🔲 CHECKPOINT 2F — Human Review Required**
> - [ ] Checkout performance test: payment auth via REST within latency budget
> - [ ] All 5 order→accounting SECAs converted to events
> - [ ] Feature flag tested: both dispatcher and REST/event paths work
> - [ ] End-to-end test: place order → pay → ship → invoice → GL posting, all via APIs/events
> - [ ] Payment release timing validated with business stakeholders

---

#### Sub-Phase 2G: Accounting Schema Separation (3 weeks)

**Goal:** Migrate accounting's 200 entities to a separate database schema.

**Tasks:**
1. Create `accounting` schema.
2. Migrate accounting-owned entities (200 from `accounting-entitymodel.xml`).
3. Replace remaining cross-domain entity reads with API calls.
4. Update view entities that span accounting and other domains — replace with API-composed queries.
5. Remove cross-schema foreign keys; implement application-level referential integrity.

> **🔲 CHECKPOINT 2G — Human Review Required**
> - [ ] Schema migration validated in staging
> - [ ] All cross-domain view entities replaced or decomposed
> - [ ] No direct SQL joins between accounting and other schemas
> - [ ] Data integrity checks: no orphaned records after migration

---

#### Sub-Phase 2H: Integration Testing + Production Cutover (3 weeks)

**Goal:** Comprehensive validation and production deployment.

**Tasks:**
1. **Contract testing:** Implement Pact contract tests between Order↔Accounting and Product↔Accounting.
2. **End-to-end regression:** Full business cycle testing:
   - Sales order → payment auth → shipment → invoice → GL posting → payment capture
   - Purchase order → receipt → invoice → payment → GL posting
   - Return → refund → credit memo → GL reversal
3. **Performance testing:** Verify no degradation in transaction throughput.
4. **Chaos testing:** Kill accounting service; verify Order handles it gracefully (circuit breaker, retries).
5. **Production deployment:** Feature flags enabled, 2-week bake period.

> **🔲 CHECKPOINT 2H — Human Review Required (Production Go/No-Go)**
> - [ ] All contract tests pass
> - [ ] End-to-end regression: all 3 business cycle tests pass
> - [ ] Performance within 10% of pre-decoupling baseline
> - [ ] Chaos testing: Order survives accounting outage gracefully
> - [ ] Rollback plan tested
> - [ ] 2-week bake period with zero critical incidents
> - [ ] **Sign-off from engineering lead, finance stakeholder, and product owner**

**Estimated total: 20 weeks development + 2 weeks bake = ~22 weeks** for full Phase 2 isolation.

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

### 6.3 Implementation Roadmap (Sub-Phases)

Phase 3 is the hardest extraction due to the 244-point order→product coupling (the largest in the system). It is broken into **9 sub-phases** with aggressive checkpointing.

---

#### Sub-Phase 3A: Product Catalog Read API (3 weeks)

**Goal:** Create a comprehensive read-only Product API that Order will use instead of direct entity access and class imports.

**Tasks:**
1. Define REST endpoints covering the 29 unique product entities Order accesses (93 accesses total):
   - `GET /api/v1/product/products/{id}` (pricing, associations, features)
   - `GET /api/v1/product/inventory/available` (replaces `getProductInventoryAvailable`, `isStoreInventoryAvailableOrNotRequired`)
   - `GET /api/v1/product/pricing/calculate` (replaces `calculateProductPrice`, `calculatePurchasePrice`)
   - `GET /api/v1/product/stores/{id}` (ProductStore, ProductStoreEmailSetting)
   - `GET /api/v1/product/shipment-methods` (CarrierShipmentMethod)
   - `GET /api/v1/product/promos` (ProductPromo, ProductPromoCode)
2. Create response DTOs for each endpoint.
3. Performance-test the API against the entity-read patterns it replaces.

**Open spec questions (gate before proceeding):**
- ❓ **API surface area:** 29 entity types is a large API surface. Should we group these into coarser resources (e.g., a "product detail" endpoint that returns pricing + features + inventory in one call) to reduce HTTP round-trips?
- ❓ **Promo evaluation:** `ProductPromo` logic is deeply intertwined with `ShoppingCart`. Should promo evaluation stay in Order (with product data fetched via API) or move to Product (with cart data passed as input)?

> **🔲 CHECKPOINT 3A — Human Review Required**
> - [ ] API design review: covers all 29 entity types and 37 dispatcher calls Order makes to Product
> - [ ] Promo evaluation ownership decided
> - [ ] Performance: API response times comparable to direct entity reads for key flows (cart add, checkout)
> - [ ] OpenAPI spec reviewed by Order team

---

#### Sub-Phase 3B: ShoppingCart Adapter Layer (4 weeks)

**Goal:** Replace the 106 direct product imports in Order code with adapter-mediated access.

**Tasks:**
1. Create adapter interfaces in Order for each imported Product class:
   - `ProductCatalogAdapter` (replaces `CatalogWorker`, `ProductWorker`, `ProductStoreWorker`)
   - `ProductConfigAdapter` (replaces `ProductConfigWorker`, `ProductConfigWrapper`)
   - `ShipmentAdapter` (replaces `ShipmentReadHelper`, `ShipmentCostEstimate`)
2. Implement adapters using the Product Catalog API from Sub-Phase 3A.
3. Refactor `ShoppingCart.java`, `ShoppingCartItem.java`, `CheckOutHelper.java`, and all other Order files that import product classes.
4. This is the highest-risk refactor in the entire plan — `ShoppingCartItem` alone has 40+ product imports.

**Open spec questions (gate before proceeding):**
- ❓ **ShoppingCartItem coupling:** `ShoppingCartItem` constructs `ProductConfigWrapper` instances and calls methods on them directly. This is deep coupling, not just data access. Should we:
  - (A) Move `ProductConfigWrapper` to a shared library (quickest, but defers decoupling), or
  - (B) Create a `ProductConfigurationService` in Product that Order calls via API (clean, but requires major refactor of cart logic), or
  - (C) Duplicate the `ProductConfigWrapper` logic in Order temporarily (technical debt, but unblocks extraction)?
- ❓ **Performance budget:** Cart operations currently do many small entity reads (per-item pricing, per-item inventory checks). Batching these into fewer API calls will require significant refactoring of cart logic. What is the acceptable performance overhead?

> **🔲 CHECKPOINT 3B — Human Review Required**
> - [ ] ShoppingCartItem coupling approach decided (Option A, B, or C)
> - [ ] All 106 product imports eliminated from Order code
> - [ ] Cart performance test: add item, update quantity, checkout — all within latency budget
> - [ ] Full Order test suite passes
> - [ ] Manual smoke test: complete checkout flow with configurable products works correctly

---

#### Sub-Phase 3C: Inventory Reservation Event Flow (3 weeks)

**Goal:** Convert inventory reservation from synchronous SECA-triggered to event-driven.

**Tasks:**
1. Order publishes `OrderPlaced` event with item details and facility preferences.
2. Product subscribes, reserves inventory, publishes `InventoryReserved` or `ReservationFailed`.
3. Order subscribes to reservation results and updates order status accordingly.
4. Convert SECAs:
   - `storeOrder` → `balanceOrderItemsWithNegativeReservations` (product) → event
   - `storeOrder` → `setOrderReservationPriority` (product) → event
   - `changeOrderItemStatus` → `cancelOrderInventoryReservation` (product) → event
5. Implement compensation: if reservation fails after order is created, trigger order hold/notification.

**Open spec questions (gate before proceeding):**
- ❓ **Reservation atomicity:** Currently, `storeOrder` and `reserveStoreInventory` run in one transaction. If reservation fails, the order is not created. With events, the order is created first, then reservation is attempted asynchronously. This changes the user experience: the customer sees "order placed" but reservation might fail later. Is this acceptable? Should we keep reservation synchronous (API call, not event)?
- ❓ **Overselling risk:** Async reservation introduces a window where inventory could be oversold. What is the tolerance for overselling?

> **🔲 CHECKPOINT 3C — Human Review Required**
> - [ ] Reservation atomicity decision made and documented
> - [ ] Overselling risk assessment completed and accepted by business
> - [ ] Event flow validated: order → reserve → success/failure → order status update
> - [ ] Compensation flow tested: reservation failure triggers appropriate order hold
> - [ ] Load test: concurrent orders don't cause reservation race conditions

---

#### Sub-Phase 3D: Shipment/Receipt → Order Event Flow (2 weeks)

**Goal:** Convert Product's shipment/receipt SECAs that trigger Order actions to events.

**Tasks:**
1. Product publishes `ShipmentReceiptCreated` event.
2. Order subscribes and handles:
   - `updateReturnStatusFromReceipt` (return status updates)
   - `updateOrderStatusFromReceipt` (order status updates)
3. Product publishes `ItemIssued` event (already done in Phase 2).
4. Order subscribes for `checkCreateStockRequirementQoh`.
5. Convert remaining product→order SECAs:
   - `receiveInventoryProduct` → `setUnitPriceAsLastPrice` (order)

> **🔲 CHECKPOINT 3D — Human Review Required**
> - [ ] All product→order SECAs converted to events
> - [ ] End-to-end: receive purchase shipment → order status updates correctly
> - [ ] End-to-end: process return shipment → return status updates correctly
> - [ ] No stale order statuses observed during 48-hour test run

---

#### Sub-Phase 3E: Order→Party Decoupling (2 weeks)

**Goal:** Eliminate Order's 52-point coupling to Party (28 imports, 19 entity accesses, 5 SECAs).

**Tasks:**
1. Create Party Query API for contact/address/party data:
   - `GET /api/v1/party/parties/{id}` (Party, Person, PartyGroup)
   - `GET /api/v1/party/contact-mechs/{partyId}` (PostalAddress, ContactMech)
   - `GET /api/v1/party/relationships/{partyId}` (PartyRelationship)
2. Replace 28 party imports (`ContactHelper`, `PartyHelper`, `PartyWorker`, etc.) with adapter layer.
3. Convert 5 order→party SECAs (`ensurePartyRole`, `updateCommunicationEvent`) to events or API calls.

> **🔲 CHECKPOINT 3E — Human Review Required**
> - [ ] Party Query API covers all 11 party entity types Order accesses
> - [ ] All 28 party imports eliminated from Order code
> - [ ] Order test suite passes with party access via API

---

#### Sub-Phase 3F: Remaining Order→Accounting Cleanup (1 week)

**Goal:** Ensure all order→accounting coupling uses the infrastructure built in Phase 2.

**Tasks:**
1. Verify `PaymentGatewayPort` (from Phase 2E) covers all remaining order→accounting calls.
2. Verify Order uses Accounting Query API (from Phase 2C) for all entity reads.
3. Remove any residual direct coupling.

> **🔲 CHECKPOINT 3F — Human Review Required**
> - [ ] Zero direct dispatcher calls from Order to Accounting
> - [ ] Zero direct entity reads of accounting-owned entities from Order code
> - [ ] Integration test: checkout payment flow works via Payment Gateway Port

---

#### Sub-Phase 3G: Order Schema Separation (3 weeks)

**Goal:** Migrate Order's 167 entities to a separate schema.

**Tasks:**
1. Create `order` schema.
2. Migrate order-owned entities (167 from `order-entitymodel.xml`).
3. Replace cross-domain view entities (`OrderHeaderAndItems`, etc.) with API-composed queries.
4. Remove cross-schema foreign keys.
5. Validate data integrity post-migration.

**Open spec questions (gate before proceeding):**
- ❓ **View entity replacement strategy:** OFBiz uses ~20 cross-domain view entities involving order. Should we implement these as materialized views in the Order schema (faster queries, but stale data) or as real-time API joins (always fresh, but slower)?

> **🔲 CHECKPOINT 3G — Human Review Required**
> - [ ] Schema migration validated in staging
> - [ ] All cross-domain view entities replaced
> - [ ] View entity replacement strategy confirmed (materialized view vs. API join)
> - [ ] Data integrity: zero orphaned records

---

#### Sub-Phase 3H: Saga Implementation for Critical Flows (2 weeks)

**Goal:** Implement saga/compensation patterns for the critical distributed transactions.

**Tasks:**
1. **Checkout saga:** Order creation → Payment auth (Accounting) → Inventory reservation (Product) → Production run (Manufacturing, if configurable product). Compensations at each step.
2. **Return saga:** Return creation → Refund (Accounting) → Inventory receipt (Product). Compensations at each step.
3. **Shipment saga:** Shipment status change → Invoice creation (Accounting) → Order status update (Order). Compensations.
4. Implement saga orchestrator service or use choreography-based sagas via events.

**Open spec questions (gate before proceeding):**
- ❓ **Saga pattern:** Orchestration (central coordinator) vs. Choreography (event-driven, each service knows its next step)? Orchestration is easier to understand and debug; choreography is more resilient but harder to trace.
- ❓ **Compensation semantics:** When payment auth succeeds but inventory reservation fails, should we void the auth immediately or hold it for retry? Business rule needed.

> **🔲 CHECKPOINT 3H — Human Review Required**
> - [ ] Saga pattern choice documented (orchestration vs. choreography)
> - [ ] All 3 critical saga flows implemented and tested
> - [ ] Failure scenarios tested: payment fails, inventory fails, manufacturing fails — correct compensations triggered
> - [ ] Saga execution traced end-to-end in distributed tracing tool

---

#### Sub-Phase 3I: Integration Testing + Production Cutover (2 weeks)

**Goal:** Final validation and production deployment.

**Tasks:**
1. Full regression testing of all order flows.
2. Performance testing under production-like load.
3. Chaos testing: kill each dependent service individually, verify graceful degradation.
4. Production deployment with feature flags, 2-week bake period.

> **🔲 CHECKPOINT 3I — Human Review Required (Production Go/No-Go)**
> - [ ] Full regression: all order flows pass (place, modify, cancel, return, exchange)
> - [ ] Performance: checkout latency within 15% of pre-decoupling baseline
> - [ ] Chaos test results reviewed — all circuit breakers work correctly
> - [ ] 2-week bake period with zero critical incidents
> - [ ] **Sign-off from engineering lead, product owner, and operations team**

**Estimated total: 22 weeks development + 2 weeks bake = ~24 weeks** for full Phase 3 extraction.

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
