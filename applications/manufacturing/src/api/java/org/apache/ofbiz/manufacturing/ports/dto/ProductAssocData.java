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
import java.sql.Timestamp;

/**
 * Minimal projection DTO for ProductAssoc entity data needed by Manufacturing.
 * Used for BOM traversal and product association lookups.
 */
public final class ProductAssocData {

    private final String productId;
    private final String productIdTo;
    private final String productAssocTypeId;
    private final Timestamp fromDate;
    private final Timestamp thruDate;
    private final BigDecimal quantity;
    private final String sequenceNum;
    private final String reason;
    private final String instruction;
    private final String routingWorkEffortId;
    private final String estimateCalcMethod;
    private final String recurrenceInfoId;
    private final BigDecimal scrapFactor;

    public ProductAssocData(String productId, String productIdTo, String productAssocTypeId,
            Timestamp fromDate, Timestamp thruDate, BigDecimal quantity, String sequenceNum,
            String reason, String instruction, String routingWorkEffortId,
            String estimateCalcMethod, String recurrenceInfoId, BigDecimal scrapFactor) {
        this.productId = productId;
        this.productIdTo = productIdTo;
        this.productAssocTypeId = productAssocTypeId;
        this.fromDate = fromDate;
        this.thruDate = thruDate;
        this.quantity = quantity;
        this.sequenceNum = sequenceNum;
        this.reason = reason;
        this.instruction = instruction;
        this.routingWorkEffortId = routingWorkEffortId;
        this.estimateCalcMethod = estimateCalcMethod;
        this.recurrenceInfoId = recurrenceInfoId;
        this.scrapFactor = scrapFactor;
    }

    public String getProductId() {
        return productId;
    }

    public String getProductIdTo() {
        return productIdTo;
    }

    public String getProductAssocTypeId() {
        return productAssocTypeId;
    }

    public Timestamp getFromDate() {
        return fromDate;
    }

    public Timestamp getThruDate() {
        return thruDate;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public String getSequenceNum() {
        return sequenceNum;
    }

    public String getReason() {
        return reason;
    }

    public String getInstruction() {
        return instruction;
    }

    public String getRoutingWorkEffortId() {
        return routingWorkEffortId;
    }

    public String getEstimateCalcMethod() {
        return estimateCalcMethod;
    }

    public String getRecurrenceInfoId() {
        return recurrenceInfoId;
    }

    public BigDecimal getScrapFactor() {
        return scrapFactor;
    }
}
