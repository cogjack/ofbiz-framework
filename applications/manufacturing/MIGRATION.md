# Manufacturing Schema Separation — Migration Guide

## Phase 1G: Database Schema Separation

This document describes the migration procedure for separating manufacturing-owned
entities into a dedicated database schema, and the rollback procedure if issues arise.

## Overview

Phase 1G introduces:
1. **Entity group configuration** — Maps 7 manufacturing-owned entities to a dedicated entity group
2. **Port-mediated entity access** — New port methods and DTOs for cross-domain entity reads
3. **Cross-domain access audit** — Complete catalog of 109 entity accesses requiring migration

## Prerequisites

- All previous phases (1A through 1F) must be deployed and stable
- Database backup taken before migration
- Application downtime window scheduled (recommended: 30 minutes)

## Manufacturing-Owned Entities

The following entities are migrated to the `org.apache.ofbiz.manufacturing` entity group:

| Entity | Description |
|--------|-------------|
| TechDataCalendar | Manufacturing calendar definitions |
| TechDataCalendarWeek | Weekly calendar patterns |
| TechDataCalendarExcDay | Calendar day exceptions |
| TechDataCalendarExcWeek | Calendar week exceptions |
| MrpEvent | MRP planning events |
| MrpEventType | MRP event type definitions |
| ProductManufacturingRule | BOM substitution rules |

## Migration Procedure

### Step 1: Deploy Updated Code

Deploy the application with Phase 1G code changes. The entity group configuration
in `applications/manufacturing/entitydef/entitygroup.xml` is loaded automatically.

```bash
# Build and deploy
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
./gradlew build
```

### Step 2: Configure Manufacturing Datasource (Optional)

To actually separate the schema, add a dedicated datasource in `entityengine.xml`:

```xml
<!-- In framework/entity/config/entityengine.xml -->
<delegator name="default" entity-model-reader="main" entity-group-reader="main">
    <!-- Existing default datasource -->
    <group-map group-name="org.apache.ofbiz" datasource-name="localderby"/>
    <!-- New manufacturing-specific datasource -->
    <group-map group-name="org.apache.ofbiz.manufacturing" datasource-name="localderby-manufacturing"/>
</delegator>

<!-- Define the manufacturing datasource -->
<datasource name="localderby-manufacturing"
        helper-name="derby"
        schema-name="MANUFACTURING"
        ...>
    <!-- Same configuration as localderby but with different schema -->
</datasource>
```

**Note:** This step is optional. Without it, manufacturing entities remain in the
default datasource, which is safe and fully functional. The entity group configuration
prepares for separation but does not require it.

### Step 3: Migrate Data (If Using Separate Schema)

If you configured a separate datasource in Step 2:

```sql
-- Create the manufacturing schema
CREATE SCHEMA MANUFACTURING;

-- Migrate manufacturing tables
-- For Derby:
RENAME TABLE APP.TECH_DATA_CALENDAR TO MANUFACTURING.TECH_DATA_CALENDAR;
RENAME TABLE APP.TECH_DATA_CALENDAR_WEEK TO MANUFACTURING.TECH_DATA_CALENDAR_WEEK;
RENAME TABLE APP.TECH_DATA_CALENDAR_EXC_DAY TO MANUFACTURING.TECH_DATA_CALENDAR_EXC_DAY;
RENAME TABLE APP.TECH_DATA_CALENDAR_EXC_WEEK TO MANUFACTURING.TECH_DATA_CALENDAR_EXC_WEEK;
RENAME TABLE APP.MRP_EVENT TO MANUFACTURING.MRP_EVENT;
RENAME TABLE APP.MRP_EVENT_TYPE TO MANUFACTURING.MRP_EVENT_TYPE;
RENAME TABLE APP.PRODUCT_MANUFACTURING_RULE TO MANUFACTURING.PRODUCT_MANUFACTURING_RULE;

-- For PostgreSQL:
-- ALTER TABLE tech_data_calendar SET SCHEMA manufacturing;
-- ALTER TABLE tech_data_calendar_week SET SCHEMA manufacturing;
-- ALTER TABLE tech_data_calendar_exc_day SET SCHEMA manufacturing;
-- ALTER TABLE tech_data_calendar_exc_week SET SCHEMA manufacturing;
-- ALTER TABLE mrp_event SET SCHEMA manufacturing;
-- ALTER TABLE mrp_event_type SET SCHEMA manufacturing;
-- ALTER TABLE product_manufacturing_rule SET SCHEMA manufacturing;
```

### Step 4: Verify

```bash
# Start the application
./gradlew ofbiz

# Verify manufacturing operations work:
# 1. Create a production run
# 2. Run MRP
# 3. Check BOM operations
# 4. Verify tech data calendar lookups
```

## Rollback Procedure

### If Code Rollback Needed

1. Redeploy the previous version (Phase 1F code)
2. The entity group configuration will be ignored since the file won't exist
3. All entities continue to work from the default datasource

### If Schema Migration Rollback Needed

If you performed Step 3 and need to rollback:

```sql
-- Move tables back to default schema
-- For Derby:
RENAME TABLE MANUFACTURING.TECH_DATA_CALENDAR TO APP.TECH_DATA_CALENDAR;
RENAME TABLE MANUFACTURING.TECH_DATA_CALENDAR_WEEK TO APP.TECH_DATA_CALENDAR_WEEK;
RENAME TABLE MANUFACTURING.TECH_DATA_CALENDAR_EXC_DAY TO APP.TECH_DATA_CALENDAR_EXC_DAY;
RENAME TABLE MANUFACTURING.TECH_DATA_CALENDAR_EXC_WEEK TO APP.TECH_DATA_CALENDAR_EXC_WEEK;
RENAME TABLE MANUFACTURING.MRP_EVENT TO APP.MRP_EVENT;
RENAME TABLE MANUFACTURING.MRP_EVENT_TYPE TO APP.MRP_EVENT_TYPE;
RENAME TABLE MANUFACTURING.PRODUCT_MANUFACTURING_RULE TO APP.PRODUCT_MANUFACTURING_RULE;

-- For PostgreSQL:
-- ALTER TABLE manufacturing.tech_data_calendar SET SCHEMA public;
-- (repeat for all tables)

-- Remove the schema
DROP SCHEMA MANUFACTURING;
```

Then remove the manufacturing datasource configuration from `entityengine.xml`.

### Emergency Rollback

If immediate rollback is needed without code changes:

1. Remove the `group-map` entry for `org.apache.ofbiz.manufacturing` from `entityengine.xml`
2. Restart the application
3. Manufacturing entities will fall back to the default datasource

## New Port Methods

Phase 1G adds the following port methods for cross-domain entity access:

| Port | Method | Returns | Replaces |
|------|--------|---------|----------|
| ProductPort | `getProduct(productId)` | ProductData | Direct Product entity lookups |
| ProductPort | `getProductAssocs(productId, typeId, date)` | List\<ProductAssocData\> | Direct ProductAssoc queries |
| ProductPort | `getInventoryItem(inventoryItemId)` | InventoryItemData | Direct InventoryItem lookups |
| WorkEffortPort | `getWorkEffort(workEffortId)` | WorkEffortData | Direct WorkEffort lookups |
| WorkEffortPort | `getWorkEffortAssocs(fromId, typeId)` | List\<WorkEffortAssocData\> | Direct WorkEffortAssoc queries |
| OrderPort | `getOrderHeader(orderId)` | OrderHeaderData | Direct OrderHeader lookups |
| OrderPort | `getOrderItems(orderId)` | List\<OrderItemData\> | Direct OrderItem queries |

## Files Changed

### New Files
- `applications/manufacturing/entitydef/entitygroup.xml` — Entity group configuration
- `applications/manufacturing/data/MfgSchemaSetup.xml` — Seed data placeholder
- `applications/manufacturing/ENTITY_ACCESS_AUDIT.md` — Cross-domain access audit
- `applications/manufacturing/MIGRATION.md` — This file
- `applications/manufacturing/src/api/java/.../ports/dto/ProductData.java` — Product DTO
- `applications/manufacturing/src/api/java/.../ports/dto/ProductAssocData.java` — ProductAssoc DTO
- `applications/manufacturing/src/api/java/.../ports/dto/InventoryItemData.java` — InventoryItem DTO
- `applications/manufacturing/src/api/java/.../ports/dto/WorkEffortData.java` — WorkEffort DTO
- `applications/manufacturing/src/api/java/.../ports/dto/WorkEffortAssocData.java` — WorkEffortAssoc DTO
- `applications/manufacturing/src/api/java/.../ports/dto/OrderHeaderData.java` — OrderHeader DTO
- `applications/manufacturing/src/api/java/.../ports/dto/OrderItemData.java` — OrderItem DTO

### Modified Files
- `applications/manufacturing/src/api/java/.../ports/ProductPort.java` — 3 new methods
- `applications/manufacturing/src/api/java/.../ports/WorkEffortPort.java` — 2 new methods
- `applications/manufacturing/src/api/java/.../ports/OrderPort.java` — 2 new methods
- `applications/manufacturing/src/main/java/.../ports/impl/DispatcherProductPort.java` — 3 implementations
- `applications/manufacturing/src/main/java/.../ports/impl/DispatcherWorkEffortPort.java` — 2 implementations
- `applications/manufacturing/src/main/java/.../ports/impl/DispatcherOrderPort.java` — 2 implementations

## Monitoring

After deployment, monitor:

1. **Application logs** — Watch for `GenericEntityException` or entity group resolution errors
2. **Manufacturing operations** — Test production run creation, MRP execution, BOM traversal
3. **Performance** — Monitor query response times for manufacturing entity lookups
4. **Feature flag** (from Phase 1F) — Ensure REST cutover flag remains stable
