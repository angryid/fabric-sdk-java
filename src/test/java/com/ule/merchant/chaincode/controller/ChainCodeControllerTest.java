package com.ule.merchant.chaincode.controller;

import com.alibaba.fastjson.JSON;
import com.ule.merchant.chaincode.dto.BaseChainCodeResponse;
import com.ule.merchant.chaincode.dto.DeployAndInitChainCodeRequest;
import com.ule.merchant.chaincode.dto.RegisterUserChainCodeRequest;
import com.ule.merchant.chaincode.dto.SendChainCodeRequest;
import org.hyperledger.fabric.sdk.helper.Utils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.UUID;

public class ChainCodeControllerTest {

    private static final RestTemplate restTemplate = new RestTemplate();
    private static final String host = "http://localhost:8080/chaincode/";

    private String password;

    @Test
    public void registerUserIfRequired() {
        RegisterUserChainCodeRequest request = new RegisterUserChainCodeRequest();
        request.setUserName("sadfsdfaa");//PRObVfTrxXAz
        request.setAffiliation("org1.department1");
        request.setMspId("Org1MSP");


        BaseChainCodeResponse res = restTemplate.postForObject(host + "/registerUser", request, BaseChainCodeResponse.class);
        Assert.notNull(res, "响应不应为空");
        password = res.getMsg();
        System.out.println("注册用户响应 res=" + JSON.toJSONString(res));
    }

    /*@Test
    public void deployAndInitChaincodeIfRequired() throws IOException {
        DeployAndInitChainCodeRequest request = new DeployAndInitChainCodeRequest();
        request.setChainCodeName("dsfafds");
        request.setChainCodePath("ule.com/gocc");
        request.setChainCodeType("GO_LANG");
        request.setChainCodeVersion("1");
        String policyPath = "D:\\idea-workspace\\fabric-sdk-java-ule\\config\\chaincodeendorsementpolicy.yaml";
        request.setChaincodeendorsementpolicy(new FileInputStream(new File(policyPath)));
        String ccSource = "D:\\idea-workspace\\fabric-sdk-java-ule\\config\\ulechaincode";
        request.setChainCodeInputStream(new ByteArrayInputStream(Utils.generateTarGz(new File(ccSource), "ule.com/gocc", null)));
        request.setInitParams(new String[]{"a", "a001"});


        BaseChainCodeResponse res = restTemplate.postForObject(host + "/deployAndInit", request, BaseChainCodeResponse.class);
        System.out.println("部署链码响应 res=" + JSON.toJSONString(res));
    }*/

    @Test
    public void deployAndInitMerchantContractChaincodeIfRequired() {
        BaseChainCodeResponse res = restTemplate.postForObject(host + "/deployAndInitMerchantContract", null, BaseChainCodeResponse.class);
        System.out.println("部署链码响应 res=" + JSON.toJSONString(res));
    }

    @Test
    public void sendTransaction() {
        SendChainCodeRequest request = new SendChainCodeRequest();
        request.setChainCodeName("uleMerchantChainCode");
        request.setChainCodeType("GO_LANG");
        request.setChainCodeVersion("1");
        request.setMethodName("addOrUpdate");
        request.setParams(new String[]{"b", "b001"});
        request.setUserName("sadfsdfaa");
        request.setPassword("cwoZrpTKeIss");
        request.setMspId("Org1MSP");


        BaseChainCodeResponse res = restTemplate.postForObject(host + "/sendTransaction", request, BaseChainCodeResponse.class);
        System.out.println("发送交易响应 res=" + JSON.toJSONString(res));
    }

    @Test
    public void queryByChainCode() {
        SendChainCodeRequest request = new SendChainCodeRequest();
        request.setChainCodeName("uleMerchantChainCode");
        request.setChainCodeType("GO_LANG");
        request.setChainCodeVersion("1");
        request.setMethodName("query");
        request.setParams(new String[]{"b"});
        request.setUserName("sadfsdfaa");
        request.setPassword("cwoZrpTKeIss");
        request.setMspId("Org1MSP");


        BaseChainCodeResponse res = restTemplate.postForObject(host + "/queryByChainCode", request, BaseChainCodeResponse.class);
        System.out.println("查询交易响应 res=" + JSON.toJSONString(res));
    }

}