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

/**
 * Minimal projection DTO for Product entity data needed by Manufacturing.
 * Contains only the fields that manufacturing code actually reads from Product entities.
 */
public final class ProductData {

    private final String productId;
    private final String productTypeId;
    private final String productName;
    private final String internalName;
    private final String isVirtual;
    private final String isVariant;
    private final String billOfMaterialLevel;
    private final String facilityId;
    private final String configId;
    private final BigDecimal quantityIncluded;
    private final String quantityUomId;
    private final BigDecimal productWeight;
    private final String weightUomId;
    private final BigDecimal productHeight;
    private final BigDecimal productWidth;
    private final BigDecimal productDepth;

    private ProductData(Builder builder) {
        this.productId = builder.productId;
        this.productTypeId = builder.productTypeId;
        this.productName = builder.productName;
        this.internalName = builder.internalName;
        this.isVirtual = builder.isVirtual;
        this.isVariant = builder.isVariant;
        this.billOfMaterialLevel = builder.billOfMaterialLevel;
        this.facilityId = builder.facilityId;
        this.configId = builder.configId;
        this.quantityIncluded = builder.quantityIncluded;
        this.quantityUomId = builder.quantityUomId;
        this.productWeight = builder.productWeight;
        this.weightUomId = builder.weightUomId;
        this.productHeight = builder.productHeight;
        this.productWidth = builder.productWidth;
        this.productDepth = builder.productDepth;
    }

    public String getProductId() {
        return productId;
    }

    public String getProductTypeId() {
        return productTypeId;
    }

    public String getProductName() {
        return productName;
    }

    public String getInternalName() {
        return internalName;
    }

    public String getIsVirtual() {
        return isVirtual;
    }

    public String getIsVariant() {
        return isVariant;
    }

    public String getBillOfMaterialLevel() {
        return billOfMaterialLevel;
    }

    public String getFacilityId() {
        return facilityId;
    }

    public String getConfigId() {
        return configId;
    }

    public BigDecimal getQuantityIncluded() {
        return quantityIncluded;
    }

    public String getQuantityUomId() {
        return quantityUomId;
    }

    public BigDecimal getProductWeight() {
        return productWeight;
    }

    public String getWeightUomId() {
        return weightUomId;
    }

    public BigDecimal getProductHeight() {
        return productHeight;
    }

    public BigDecimal getProductWidth() {
        return productWidth;
    }

    public BigDecimal getProductDepth() {
        return productDepth;
    }

    public static Builder builder(String productId) {
        return new Builder(productId);
    }

    public static final class Builder {
        private final String productId;
        private String productTypeId;
        private String productName;
        private String internalName;
        private String isVirtual;
        private String isVariant;
        private String billOfMaterialLevel;
        private String facilityId;
        private String configId;
        private BigDecimal quantityIncluded;
        private String quantityUomId;
        private BigDecimal productWeight;
        private String weightUomId;
        private BigDecimal productHeight;
        private BigDecimal productWidth;
        private BigDecimal productDepth;

        private Builder(String productId) {
            this.productId = productId;
        }

        public Builder productTypeId(String val) {
            productTypeId = val;
            return this;
        }

        public Builder productName(String val) {
            productName = val;
            return this;
        }

        public Builder internalName(String val) {
            internalName = val;
            return this;
        }

        public Builder isVirtual(String val) {
            isVirtual = val;
            return this;
        }

        public Builder isVariant(String val) {
            isVariant = val;
            return this;
        }

        public Builder billOfMaterialLevel(String val) {
            billOfMaterialLevel = val;
            return this;
        }

        public Builder facilityId(String val) {
            facilityId = val;
            return this;
        }

        public Builder configId(String val) {
            configId = val;
            return this;
        }

        public Builder quantityIncluded(BigDecimal val) {
            quantityIncluded = val;
            return this;
        }

        public Builder quantityUomId(String val) {
            quantityUomId = val;
            return this;
        }

        public Builder productWeight(BigDecimal val) {
            productWeight = val;
            return this;
        }

        public Builder weightUomId(String val) {
            weightUomId = val;
            return this;
        }

        public Builder productHeight(BigDecimal val) {
            productHeight = val;
            return this;
        }

        public Builder productWidth(BigDecimal val) {
            productWidth = val;
            return this;
        }

        public Builder productDepth(BigDecimal val) {
            productDepth = val;
            return this;
        }

        public ProductData build() {
            return new ProductData(this);
        }
    }
}
