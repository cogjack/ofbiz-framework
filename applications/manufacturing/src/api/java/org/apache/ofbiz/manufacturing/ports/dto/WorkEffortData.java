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
 * Minimal projection DTO for WorkEffort entity data needed by Manufacturing.
 * Used for production run header and task lookups.
 */
public final class WorkEffortData {

    private final String workEffortId;
    private final String workEffortTypeId;
    private final String workEffortPurposeTypeId;
    private final String currentStatusId;
    private final String workEffortName;
    private final String workEffortParentId;
    private final String facilityId;
    private final Timestamp estimatedStartDate;
    private final Timestamp estimatedCompletionDate;
    private final Timestamp actualStartDate;
    private final Timestamp actualCompletionDate;
    private final BigDecimal quantityToProduce;
    private final BigDecimal quantityProduced;
    private final BigDecimal quantityRejected;
    private final String fixedAssetId;
    private final Double estimatedMilliSeconds;
    private final Double estimatedSetupMillis;
    private final Double actualMilliSeconds;
    private final Double actualSetupMillis;
    private final Long priority;

    private WorkEffortData(Builder builder) {
        this.workEffortId = builder.workEffortId;
        this.workEffortTypeId = builder.workEffortTypeId;
        this.workEffortPurposeTypeId = builder.workEffortPurposeTypeId;
        this.currentStatusId = builder.currentStatusId;
        this.workEffortName = builder.workEffortName;
        this.workEffortParentId = builder.workEffortParentId;
        this.facilityId = builder.facilityId;
        this.estimatedStartDate = builder.estimatedStartDate;
        this.estimatedCompletionDate = builder.estimatedCompletionDate;
        this.actualStartDate = builder.actualStartDate;
        this.actualCompletionDate = builder.actualCompletionDate;
        this.quantityToProduce = builder.quantityToProduce;
        this.quantityProduced = builder.quantityProduced;
        this.quantityRejected = builder.quantityRejected;
        this.fixedAssetId = builder.fixedAssetId;
        this.estimatedMilliSeconds = builder.estimatedMilliSeconds;
        this.estimatedSetupMillis = builder.estimatedSetupMillis;
        this.actualMilliSeconds = builder.actualMilliSeconds;
        this.actualSetupMillis = builder.actualSetupMillis;
        this.priority = builder.priority;
    }

    public String getWorkEffortId() {
        return workEffortId;
    }

    public String getWorkEffortTypeId() {
        return workEffortTypeId;
    }

    public String getWorkEffortPurposeTypeId() {
        return workEffortPurposeTypeId;
    }

    public String getCurrentStatusId() {
        return currentStatusId;
    }

    public String getWorkEffortName() {
        return workEffortName;
    }

    public String getWorkEffortParentId() {
        return workEffortParentId;
    }

    public String getFacilityId() {
        return facilityId;
    }

    public Timestamp getEstimatedStartDate() {
        return estimatedStartDate;
    }

    public Timestamp getEstimatedCompletionDate() {
        return estimatedCompletionDate;
    }

    public Timestamp getActualStartDate() {
        return actualStartDate;
    }

    public Timestamp getActualCompletionDate() {
        return actualCompletionDate;
    }

    public BigDecimal getQuantityToProduce() {
        return quantityToProduce;
    }

    public BigDecimal getQuantityProduced() {
        return quantityProduced;
    }

    public BigDecimal getQuantityRejected() {
        return quantityRejected;
    }

    public String getFixedAssetId() {
        return fixedAssetId;
    }

    public Double getEstimatedMilliSeconds() {
        return estimatedMilliSeconds;
    }

    public Double getEstimatedSetupMillis() {
        return estimatedSetupMillis;
    }

    public Double getActualMilliSeconds() {
        return actualMilliSeconds;
    }

    public Double getActualSetupMillis() {
        return actualSetupMillis;
    }

    public Long getPriority() {
        return priority;
    }

    public static Builder builder(String workEffortId) {
        return new Builder(workEffortId);
    }

    public static final class Builder {
        private final String workEffortId;
        private String workEffortTypeId;
        private String workEffortPurposeTypeId;
        private String currentStatusId;
        private String workEffortName;
        private String workEffortParentId;
        private String facilityId;
        private Timestamp estimatedStartDate;
        private Timestamp estimatedCompletionDate;
        private Timestamp actualStartDate;
        private Timestamp actualCompletionDate;
        private BigDecimal quantityToProduce;
        private BigDecimal quantityProduced;
        private BigDecimal quantityRejected;
        private String fixedAssetId;
        private Double estimatedMilliSeconds;
        private Double estimatedSetupMillis;
        private Double actualMilliSeconds;
        private Double actualSetupMillis;
        private Long priority;

        private Builder(String workEffortId) {
            this.workEffortId = workEffortId;
        }

        public Builder workEffortTypeId(String val) {
            workEffortTypeId = val;
            return this;
        }

        public Builder workEffortPurposeTypeId(String val) {
            workEffortPurposeTypeId = val;
            return this;
        }

        public Builder currentStatusId(String val) {
            currentStatusId = val;
            return this;
        }

        public Builder workEffortName(String val) {
            workEffortName = val;
            return this;
        }

        public Builder workEffortParentId(String val) {
            workEffortParentId = val;
            return this;
        }

        public Builder facilityId(String val) {
            facilityId = val;
            return this;
        }

        public Builder estimatedStartDate(Timestamp val) {
            estimatedStartDate = val;
            return this;
        }

        public Builder estimatedCompletionDate(Timestamp val) {
            estimatedCompletionDate = val;
            return this;
        }

        public Builder actualStartDate(Timestamp val) {
            actualStartDate = val;
            return this;
        }

        public Builder actualCompletionDate(Timestamp val) {
            actualCompletionDate = val;
            return this;
        }

        public Builder quantityToProduce(BigDecimal val) {
            quantityToProduce = val;
            return this;
        }

        public Builder quantityProduced(BigDecimal val) {
            quantityProduced = val;
            return this;
        }

        public Builder quantityRejected(BigDecimal val) {
            quantityRejected = val;
            return this;
        }

        public Builder fixedAssetId(String val) {
            fixedAssetId = val;
            return this;
        }

        public Builder estimatedMilliSeconds(Double val) {
            estimatedMilliSeconds = val;
            return this;
        }

        public Builder estimatedSetupMillis(Double val) {
            estimatedSetupMillis = val;
            return this;
        }

        public Builder actualMilliSeconds(Double val) {
            actualMilliSeconds = val;
            return this;
        }

        public Builder actualSetupMillis(Double val) {
            actualSetupMillis = val;
            return this;
        }

        public Builder priority(Long val) {
            priority = val;
            return this;
        }

        public WorkEffortData build() {
            return new WorkEffortData(this);
        }
    }
}
