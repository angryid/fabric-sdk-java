package com.ule.merchant.chaincode.dto;

import javax.validation.constraints.NotBlank;

public class PutMerchantInfoRequest {

    @NotBlank
    private String merchantId;
    @NotBlank
    private String merchantInfo;

    public String getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }

    public String getMerchantInfo() {
        return merchantInfo;
    }

    public void setMerchantInfo(String merchantInfo) {
        this.merchantInfo = merchantInfo;
    }
}
