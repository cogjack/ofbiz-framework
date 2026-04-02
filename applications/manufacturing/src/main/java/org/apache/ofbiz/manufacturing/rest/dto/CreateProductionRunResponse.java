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
package org.apache.ofbiz.manufacturing.rest.dto;

/**
 * Response DTO for production run creation endpoints.
 */
public final class CreateProductionRunResponse {

    private String productionRunId;
    private String currentStatusId;
    private String estimatedStartDate;

    public CreateProductionRunResponse() { }

    public CreateProductionRunResponse(String productionRunId, String currentStatusId,
            String estimatedStartDate) {
        this.productionRunId = productionRunId;
        this.currentStatusId = currentStatusId;
        this.estimatedStartDate = estimatedStartDate;
    }

    public String getProductionRunId() {
        return productionRunId;
    }

    public void setProductionRunId(String productionRunId) {
        this.productionRunId = productionRunId;
    }

    public String getCurrentStatusId() {
        return currentStatusId;
    }

    public void setCurrentStatusId(String currentStatusId) {
        this.currentStatusId = currentStatusId;
    }

    public String getEstimatedStartDate() {
        return estimatedStartDate;
    }

    public void setEstimatedStartDate(String estimatedStartDate) {
        this.estimatedStartDate = estimatedStartDate;
    }
}
