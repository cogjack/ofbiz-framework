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

// ARCHITECTURAL DECISION: Option A — WorkEffort remains the shared persistence model for production runs.
// This port abstracts dispatcher calls only. Entity-level decoupling is deferred to a future phase.

import java.math.BigDecimal;
import java.sql.Timestamp;

import org.apache.ofbiz.manufacturing.ports.dto.WorkEffortCreatedResult;

/**
 * Port interface abstracting all outbound dispatcher calls from Manufacturing to the WorkEffort module.
 * Covers 31 dispatcher calls across 12 method signatures originating from
 * {@code ProductionRunServices.java}.
 *
 * <p>In Phase 1B, this interface will be backed by a dispatcher-based implementation.
 * In later phases, it will be replaced by a REST/gRPC client.</p>
 */
public interface WorkEffortPort {

    /**
     * Creates a new work effort (production run header or routing task).
     * <p>Abstracts the {@code createWorkEffort} service.</p>
     *
     * @param workEffortTypeId       the type (e.g. PROD_ORDER_HEADER, PROD_ORDER_TASK, TASK)
     * @param workEffortPurposeTypeId the purpose (e.g. WEPT_PRODUCTION_RUN)
     * @param currentStatusId        initial status (e.g. PRUN_CREATED, CAL_COMPLETED)
     * @param workEffortName         human-readable name
     * @param description            optional description
     * @param facilityId             facility where work is performed
     * @param estimatedStartDate     planned start
     * @param estimatedCompletionDate planned completion (nullable for headers)
     * @param quantityToProduce      quantity to produce
     * @param workEffortParentId     parent work effort ID (nullable for headers)
     * @param fixedAssetId           fixed asset used (nullable)
     * @param reservPersons          number of persons reserved (nullable)
     * @param priority               sequencing priority (nullable)
     * @param estimatedSetupMillis   estimated setup time in milliseconds (nullable)
     * @param estimatedMilliSeconds  estimated run time in milliseconds (nullable)
     * @return result containing the generated workEffortId
     */
    WorkEffortCreatedResult createWorkEffort(String workEffortTypeId, String workEffortPurposeTypeId,
            String currentStatusId, String workEffortName, String description, String facilityId,
            Timestamp estimatedStartDate, Timestamp estimatedCompletionDate, BigDecimal quantityToProduce,
            String workEffortParentId, String fixedAssetId, BigDecimal reservPersons, Long priority,
            Double estimatedSetupMillis, Double estimatedMilliSeconds);

    /**
     * Updates an existing work effort's mutable fields.
     * <p>Abstracts the {@code updateWorkEffort} service. Only non-null parameters are applied.</p>
     *
     * @param workEffortId            the work effort to update
     * @param currentStatusId          new status (nullable — no change if null)
     * @param actualMilliSeconds       actual runtime in ms (nullable)
     * @param actualSetupMillis        actual setup time in ms (nullable)
     * @param quantityProduced         quantity produced so far (nullable)
     * @param quantityRejected         quantity rejected so far (nullable)
     * @param estimatedStartDate       updated estimated start (nullable)
     * @param estimatedCompletionDate  updated estimated completion (nullable)
     * @param actualStartDate          actual start timestamp (nullable)
     * @param actualCompletionDate     actual completion timestamp (nullable)
     */
    void updateWorkEffort(String workEffortId, String currentStatusId, Double actualMilliSeconds,
            Double actualSetupMillis, BigDecimal quantityProduced, BigDecimal quantityRejected,
            Timestamp estimatedStartDate, Timestamp estimatedCompletionDate,
            Timestamp actualStartDate, Timestamp actualCompletionDate);

    /**
     * Creates an association between two work efforts.
     * <p>Abstracts the {@code createWorkEffortAssoc} service.</p>
     *
     * @param workEffortIdFrom       source work effort
     * @param workEffortIdTo         target work effort
     * @param workEffortAssocTypeId  association type (e.g. WORK_EFF_TEMPLATE, WORK_EFF_PRECEDENCY)
     */
    void createWorkEffortAssoc(String workEffortIdFrom, String workEffortIdTo, String workEffortAssocTypeId);

    /**
     * Associates a product with a work effort via a good-standard record.
     * <p>Abstracts the {@code createWorkEffortGoodStandard} service.</p>
     *
     * @param workEffortId             the work effort
     * @param productId                the product
     * @param workEffortGoodStdTypeId  type (e.g. PRUN_PROD_DELIV, PRUNT_PROD_NEEDED)
     * @param statusId                 status (e.g. WEGS_CREATED)
     * @param estimatedQuantity        estimated quantity
     * @param fromDate                 effective start date
     */
    void createWorkEffortGoodStandard(String workEffortId, String productId,
            String workEffortGoodStdTypeId, String statusId, BigDecimal estimatedQuantity, Timestamp fromDate);

    /**
     * Updates an existing work-effort–product good-standard association.
     * <p>Abstracts the {@code updateWorkEffortGoodStandard} service.</p>
     *
     * @param workEffortId             the work effort
     * @param productId                the product
     * @param workEffortGoodStdTypeId  type
     * @param fromDate                 effective date (part of composite key)
     * @param estimatedQuantity        new estimated quantity (nullable)
     */
    void updateWorkEffortGoodStandard(String workEffortId, String productId,
            String workEffortGoodStdTypeId, Timestamp fromDate, BigDecimal estimatedQuantity);

    /**
     * Assigns a party (person/organization) to a work effort.
     * <p>Abstracts the {@code assignPartyToWorkEffort} service.</p>
     *
     * @param workEffortId  the work effort
     * @param partyId       the party to assign
     * @param roleTypeId    role of the party in this assignment
     * @param fromDate      assignment start date
     * @param statusId      assignment status (nullable)
     */
    void assignPartyToWorkEffort(String workEffortId, String partyId, String roleTypeId,
            Timestamp fromDate, String statusId);

    /**
     * Records that an inventory item was produced by a work effort.
     * <p>Abstracts the {@code createWorkEffortInventoryProduced} service.</p>
     *
     * @param workEffortId    the producing work effort
     * @param inventoryItemId the produced inventory item
     */
    void createWorkEffortInventoryProduced(String workEffortId, String inventoryItemId);

    /**
     * Creates a time entry against a work effort for labor tracking.
     * <p>Abstracts the {@code createTimeEntry} service.</p>
     *
     * @param workEffortId the work effort
     * @param partyId      the party who performed the work
     * @param rateTypeId   rate type (e.g. FLC for factory labor cost)
     * @param hours        hours worked
     * @param comments     optional comments
     */
    void createTimeEntry(String workEffortId, String partyId, String rateTypeId, double hours, String comments);

    /**
     * Adds a note to a work effort (production run).
     * <p>Abstracts the {@code createWorkEffortNote} service.</p>
     *
     * @param workEffortId the work effort
     * @param noteInfo     the note text
     * @param noteName     a short title/name for the note
     * @param noteParty    the party creating the note
     * @param internalNote whether this is an internal note (Y/N)
     */
    void createWorkEffortNote(String workEffortId, String noteInfo, String noteName,
            String noteParty, String internalNote);
}
