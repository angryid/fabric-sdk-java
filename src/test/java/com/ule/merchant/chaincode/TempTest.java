package com.ule.merchant.chaincode;

import com.alibaba.fastjson.JSON;
import com.ule.merchant.chaincode.dto.BaseChainCodeResponse;

public class TempTest {

    public static void main(String[] args) {
        System.out.println(JSON.toJSONString(BaseChainCodeResponse.success(1,"1231")));
    }
}
