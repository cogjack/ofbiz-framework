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

import java.sql.Timestamp;

/**
 * Minimal projection DTO for WorkEffortAssoc entity data needed by Manufacturing.
 * Used for routing task association lookups during production run processing.
 */
public final class WorkEffortAssocData {

    private final String workEffortIdFrom;
    private final String workEffortIdTo;
    private final String workEffortAssocTypeId;
    private final Timestamp fromDate;
    private final Timestamp thruDate;
    private final Long sequenceNum;

    public WorkEffortAssocData(String workEffortIdFrom, String workEffortIdTo,
            String workEffortAssocTypeId, Timestamp fromDate, Timestamp thruDate, Long sequenceNum) {
        this.workEffortIdFrom = workEffortIdFrom;
        this.workEffortIdTo = workEffortIdTo;
        this.workEffortAssocTypeId = workEffortAssocTypeId;
        this.fromDate = fromDate;
        this.thruDate = thruDate;
        this.sequenceNum = sequenceNum;
    }

    public String getWorkEffortIdFrom() {
        return workEffortIdFrom;
    }

    public String getWorkEffortIdTo() {
        return workEffortIdTo;
    }

    public String getWorkEffortAssocTypeId() {
        return workEffortAssocTypeId;
    }

    public Timestamp getFromDate() {
        return fromDate;
    }

    public Timestamp getThruDate() {
        return thruDate;
    }

    public Long getSequenceNum() {
        return sequenceNum;
    }
}
