package com.ule.merchant.chaincode.controller;

import com.ule.merchant.chaincode.dto.BaseChainCodeResponse;
import com.ule.merchant.chaincode.dto.DeployAndInitChainCodeRequest;
import com.ule.merchant.chaincode.dto.RegisterUserChainCodeRequest;
import com.ule.merchant.chaincode.dto.SendChainCodeRequest;
import com.ule.merchant.chaincode.service.IChainCodeInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

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
            request.setChaincodeendorsementpolicy("chaincode/chaincodeendorsementpolicy.yaml");
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


}
