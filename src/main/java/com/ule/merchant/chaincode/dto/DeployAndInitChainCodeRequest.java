package com.ule.merchant.chaincode.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

public class DeployAndInitChainCodeRequest implements Serializable {

    @NotBlank
    private String chainCodeName;
    @NotBlank
    private String chainCodeVersion;
    @NotBlank
    private String chainCodeType;
    private String chainCodePath;
    @NotBlank
    private String chainCodeSourceLocation;
    @NotEmpty
    private String[] initParams;
    @NotNull
    private String chaincodeendorsementpolicy;

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

    public String getChainCodePath() {
        return chainCodePath;
    }

    public void setChainCodePath(String chainCodePath) {
        this.chainCodePath = chainCodePath;
    }

    public String getChainCodeSourceLocation() {
        return chainCodeSourceLocation;
    }

    public void setChainCodeSourceLocation(String chainCodeSourceLocation) {
        this.chainCodeSourceLocation = chainCodeSourceLocation;
    }

    public String[] getInitParams() {
        return initParams;
    }

    public void setInitParams(String[] initParams) {
        this.initParams = initParams;
    }

    public String getChaincodeendorsementpolicy() {
        return chaincodeendorsementpolicy;
    }

    public void setChaincodeendorsementpolicy(String chaincodeendorsementpolicy) {
        this.chaincodeendorsementpolicy = chaincodeendorsementpolicy;
    }
}
