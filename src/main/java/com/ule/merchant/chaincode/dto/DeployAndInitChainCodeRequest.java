package com.ule.merchant.chaincode.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.InputStream;
import java.io.Serializable;

public class DeployAndInitChainCodeRequest implements Serializable {

    @NotBlank
    private String chainCodeName;
    @NotBlank
    private String chainCodeVersion;
    @NotBlank
    private String chainCodeType;
    private String chainCodePath;
    @NotNull
    private InputStream chainCodeInputStream;
    @NotEmpty
    private String[] initParams;
    @NotNull
    private InputStream chaincodeendorsementpolicy;

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

    public InputStream getChainCodeInputStream() {
        return chainCodeInputStream;
    }

    public void setChainCodeInputStream(InputStream chainCodeInputStream) {
        this.chainCodeInputStream = chainCodeInputStream;
    }

    public String[] getInitParams() {
        return initParams;
    }

    public void setInitParams(String[] initParams) {
        this.initParams = initParams;
    }

    public InputStream getChaincodeendorsementpolicy() {
        return chaincodeendorsementpolicy;
    }

    public void setChaincodeendorsementpolicy(InputStream chaincodeendorsementpolicy) {
        this.chaincodeendorsementpolicy = chaincodeendorsementpolicy;
    }
}
