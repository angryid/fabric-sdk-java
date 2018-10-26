package com.ule.merchant.chaincode.dto;

public class BaseChainCodeResponse {

    //响应码
    private int code;
    //响应内容
    private String msg;
    //是否处理成功
    private boolean isSuccess;

    public static BaseChainCodeResponse success(int code, String msg) {
        return result(true, code, msg);
    }

    public static BaseChainCodeResponse error(int code, String msg) {
        return result(false, code, msg);
    }

    public static BaseChainCodeResponse result(boolean isSuccess, int code, String msg) {
        BaseChainCodeResponse res = new BaseChainCodeResponse();
        res.setSuccess(isSuccess);
        res.setCode(code);
        res.setMsg(msg);
        return res;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    public void setSuccess(boolean success) {
        isSuccess = success;
    }
}
