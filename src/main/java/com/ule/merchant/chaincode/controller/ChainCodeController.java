package com.ule.merchant.chaincode.controller;

import com.ule.merchant.chaincode.dto.*;
import com.ule.merchant.chaincode.service.IChainCodeInterface;
import org.hyperledger.fabric.sdk.helper.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;

@RestController
@RequestMapping("/chaincode")
public class ChainCodeController {

    @Autowired
    private IChainCodeInterface chainCodeInterface;

    @PostMapping("/registerUser")
    public BaseChainCodeResponse registerUserIfRequired(@Valid @RequestBody RegisterUserChainCodeRequest request) {
        return chainCodeInterface.registerUserIfRequired(request);
    }

    /*@PostMapping("/deployAndInit")
    public BaseChainCodeResponse deployAndInitChaincodeIfRequired(@Valid @RequestBody DeployAndInitChainCodeRequest request) {
        return chainCodeInterface.deployAndInitChaincodeIfRequired(request);
    }*/

    @PostMapping("/deployAndInitMerchantContract")
    public BaseChainCodeResponse deployAndInitMerchantContractChaincodeIfRequired() {
        try {
            DeployAndInitChainCodeRequest request = new DeployAndInitChainCodeRequest();
            request.setChainCodeName("uleMerchantChainCode");
            request.setChainCodePath("ule.com/gocc");
            request.setChainCodeType("GO_LANG");
            request.setChainCodeVersion("1");
            request.setChaincodeendorsementpolicy(new FileInputStream(new File("chaincode/chaincodeendorsementpolicy.yaml")));
            request.setChainCodeSourceLocation("chaincode/ulemerchantchaincode");
            request.setInitParams(new String[]{});
            return chainCodeInterface.deployAndInitChaincodeIfRequired(request);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    @PostMapping("/sendTransaction")
    public BaseChainCodeResponse sendTransaction(@Valid @RequestBody SendChainCodeRequest request) {
        return chainCodeInterface.sendTransaction(request);
    }

    @PostMapping("/queryByChainCode")
    public BaseChainCodeResponse queryByChainCode(@Valid @RequestBody SendChainCodeRequest request) {
        return chainCodeInterface.queryByChainCode(request);
    }

    @PostMapping("/putMerchantInfo")
    public BaseChainCodeResponse putMerchantInfo(@Valid @RequestBody PutMerchantInfoRequest request){
        return chainCodeInterface.putMerchantInfo(request);
    }

    @GetMapping("/getMerchantInfo/{merchantId}")
    public BaseChainCodeResponse getMerchantInfo(@PathVariable String merchantId){
        return chainCodeInterface.getMerchantInfo(merchantId);
    }


}
