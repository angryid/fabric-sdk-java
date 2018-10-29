package com.ule.merchant.chaincode.dto;

import javax.validation.constraints.NotBlank;

public class RegisterUserChainCodeRequest {

    @NotBlank
    private String userName;
    @NotBlank
    private String affiliation;
    @NotBlank
    private String mspId;


    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getAffiliation() {
        return affiliation;
    }

    public void setAffiliation(String affiliation) {
        this.affiliation = affiliation;
    }

    public String getMspId() {
        return mspId;
    }

    public void setMspId(String mspId) {
        this.mspId = mspId;
    }
}
