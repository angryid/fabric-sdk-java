package com.ule.merchant.chaincode.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;

public class SendChainCodeRequest{

    @NotBlank
    private String userName;
    @NotBlank
    private String password;
    @NotBlank
    private String mspId;
    @NotBlank
    private String chainCodeName;
    @NotBlank
    private String chainCodeVersion;
    private String chainCodePath;
    @NotBlank
    private String chainCodeType;
    @NotBlank
    private String methodName;
    @NotEmpty
    private String[] params;


    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getMspId() {
        return mspId;
    }

    public void setMspId(String mspId) {
        this.mspId = mspId;
    }

    public String getChainCodeName() {
        return chainCodeName;
    }

    public void setChainCodeName(String chainCodeName) {
        this.chainCodeName = chainCodeName;
    }

    public String getChainCodeVersion() {
        return chainCodeVersion;
    }

    public void setChainCodeVersion(String chainCodeVersion) {
        this.chainCodeVersion = chainCodeVersion;
    }

    public String getChainCodeType() {
        return chainCodeType;
    }

    public void setChainCodeType(String chainCodeType) {
        this.chainCodeType = chainCodeType;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String[] getParams() {
        return params;
    }

    public void setParams(String[] params) {
        this.params = params;
    }

    public String getChainCodePath() {
        return chainCodePath;
    }

    public void setChainCodePath(String chainCodePath) {
        this.chainCodePath = chainCodePath;
    }
}
