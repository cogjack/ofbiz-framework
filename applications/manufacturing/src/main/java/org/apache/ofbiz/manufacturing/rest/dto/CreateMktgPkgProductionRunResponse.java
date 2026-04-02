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
 * Response DTO for the marketing package production run creation endpoint.
 */
public final class CreateMktgPkgProductionRunResponse {

    private String productionRunId;
    private String finishedProductId;

    public CreateMktgPkgProductionRunResponse() { }

    public CreateMktgPkgProductionRunResponse(String productionRunId, String finishedProductId) {
        this.productionRunId = productionRunId;
        this.finishedProductId = finishedProductId;
    }

    public String getProductionRunId() {
        return productionRunId;
    }

    public void setProductionRunId(String productionRunId) {
        this.productionRunId = productionRunId;
    }

    public String getFinishedProductId() {
        return finishedProductId;
    }

    public void setFinishedProductId(String finishedProductId) {
        this.finishedProductId = finishedProductId;
    }
}
