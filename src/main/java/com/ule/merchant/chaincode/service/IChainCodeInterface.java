package com.ule.merchant.chaincode.service;

import com.ule.merchant.chaincode.dto.BaseChainCodeResponse;
import com.ule.merchant.chaincode.dto.DeployAndInitChainCodeRequest;
import com.ule.merchant.chaincode.dto.RegisterUserChainCodeRequest;
import com.ule.merchant.chaincode.dto.SendChainCodeRequest;

/**
 * 尽量封装更加通用的接口，以方便调用，其中内置了admin用户，账号密码：{"admin", "adminpw"}
 * <p>
 * 用户主要关注 提交交易信息和查询交易信息 【建议用户权限】
 * 注册用户和部署实例化链码权限交给peerAdmin 【建议peerAdmin权限】
 * -----------
 * 其中获取hfclient，获取channel步骤由内部方法和peerAdmin完成，无需用户参与
 * 部署和实例化链码到各个peer节点，同样由peerAdmin完成，无需用户参与
 * -----------
 * 当前fabric版本 1.2.1
 */
public interface IChainCodeInterface {

    //注册用户 如果成功，则响应结果msg为用户密码，由fabric生成
    BaseChainCodeResponse registerUserIfRequired(RegisterUserChainCodeRequest request);

    // 部署和实例化链码 其中chainCodePath为go语言类型必选项，其他类型为空，
    // 之所以封装为InputStream，而不是sourcePath，是为了调用方降低pom依赖，减少调用的复杂度和不必关注fabric-sdk的具体实现 type GO_LANG,JAVA,NODE
    BaseChainCodeResponse deployAndInitChaincodeIfRequired(DeployAndInitChainCodeRequest request);

    //执行交易
    BaseChainCodeResponse sendTransaction(SendChainCodeRequest request);

    //查询chaincode
    BaseChainCodeResponse queryByChainCode(SendChainCodeRequest request);

}
