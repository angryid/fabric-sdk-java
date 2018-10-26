package com.ule.merchant.chaincode.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.ule.merchant.chaincode.dto.BaseChainCodeResponse;
import com.ule.merchant.chaincode.dto.DeployAndInitChainCodeRequest;
import com.ule.merchant.chaincode.dto.RegisterUserChainCodeRequest;
import com.ule.merchant.chaincode.dto.SendChainCodeRequest;
import com.ule.merchant.chaincode.model.ChainCodeUser;
import com.ule.merchant.chaincode.service.IChainCodeInterface;
import com.ule.merchant.chaincode.util.PropertyUtil;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hyperledger.fabric.protos.peer.Query;
import org.hyperledger.fabric.sdk.*;
import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.hyperledger.fabric.sdk.exception.TransactionException;
import org.hyperledger.fabric.sdk.helper.Config;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric_ca.sdk.HFCAClient;
import org.hyperledger.fabric_ca.sdk.RegistrationRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.security.PrivateKey;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.ule.merchant.chaincode.model.ChainCodeUser.getPrivateKeyFromBytes;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hyperledger.fabric.sdk.Channel.PeerOptions.createPeerOptions;
import static org.hyperledger.fabric.sdk.Channel.TransactionOptions.createTransactionOptions;

@Service
public class ChainCodeJavaImpl implements IChainCodeInterface {

    private static final Logger log = LogManager.getLogger();

    private Properties config;
    private int waitTime;

    @PostConstruct
    public void init() {
        //加载配置文件 ...
        config = PropertyUtil.get("/chaincode/fabric-config.properties");
        Assert.notEmpty(config, "fabric-config配置不能为空");
        System.setProperty(Config.GENESISBLOCK_WAIT_TIME, config.getProperty("fabric.genesisblock.wait.time"));
        waitTime = Integer.parseInt(config.getProperty("fabric.wait.time"));
    }

    @Override
    public BaseChainCodeResponse registerUserIfRequired(RegisterUserChainCodeRequest request) {
        String userName = request.getUserName();
        String affiliation = request.getAffiliation();
        String mspId = request.getMspId();
        try {
            log.info("注册智能合约用户开始 userName=" + userName + " affiliation=" + affiliation + " mspId=" + mspId);
            HFCAClient caClient = getCAClient();
            Enrollment adminEnrollment = caClient.enroll(config.getProperty("admin.name"), config.getProperty("admin.password"));
            ChainCodeUser admin = new ChainCodeUser(config.getProperty("admin.name"), config.getProperty("admin.mspId"), adminEnrollment);
            //注册用户
            RegistrationRequest rr = new RegistrationRequest(userName, affiliation);
            String enrollmentSecret = caClient.register(rr, admin);
            log.info("注册智能合约用户成功 userName=" + userName);
            return BaseChainCodeResponse.success(1, enrollmentSecret);
        } catch (Exception e) {
            log.error("注册智能合约用户异常 userName=" + userName, e);
            return BaseChainCodeResponse.error(-1, e.getMessage());
        }
    }

    @Override
    public BaseChainCodeResponse deployAndInitChaincodeIfRequired(DeployAndInitChainCodeRequest request) {
        String chainCodeName = request.getChainCodeName();
        String chainCodeVersion = request.getChainCodeVersion();
        InputStream chainCodeInputStream = request.getChainCodeInputStream();
        String chainCodeType = request.getChainCodeType();
        String chainCodePath = request.getChainCodePath();
        String[] initParams = request.getInitParams();
        InputStream chaincodeendorsementInputStream = request.getChaincodeendorsementpolicy();
        try {
            log.info("部署和实例化链码开始 chainCodeName=" + chainCodeName);
            ChainCodeUser peerAdmin = getPeerAdmin();
            HFClient client = getClient(peerAdmin);
            Channel channel = getOrCreateChannel(peerAdmin, client);

            ChaincodeID ccid = ChaincodeID.newBuilder().setName(chainCodeName).setVersion(chainCodeVersion).build();
            //检测所有peer节点是否已经安装过链码
            for (Peer peer : channel.getPeers()) {
                List<Query.ChaincodeInfo> ccInfoList = client.queryInstalledChaincodes(peer);
                if (!CollectionUtils.isEmpty(ccInfoList)) {
                    for (Query.ChaincodeInfo chaincodeInfo : ccInfoList) {
                        if (chaincodeInfo.getName().equals(chainCodeName)) {
                            log.warn("检测到peer节点" + peer.getName() + "已存在链码 chainCodeName=" + chainCodeName);
                            return BaseChainCodeResponse.result(false, 0, "检测到peer节点 " + peer.getName() + "=" + peer.getUrl() + " 已经安装chainCode，继续安装会报错，建议跳过安装初始化，直接执行交易和查询");
                        }
                    }
                }
            }

            //安装chainCode
            InstallProposalRequest ipr = client.newInstallProposalRequest();
            ipr.setProposalWaitTime(waitTime);
            ipr.setChaincodeID(ccid);
            ipr.setChaincodeInputStream(chainCodeInputStream);

            TransactionRequest.Type type = TransactionRequest.Type.valueOf(chainCodeType.toUpperCase());
            if (type == TransactionRequest.Type.GO_LANG)
                ipr.setChaincodePath(chainCodePath);
            ipr.setChaincodeLanguage(type);

            Collection<ProposalResponse> ires = client.sendInstallProposal(ipr, channel.getPeers());

            Collection<ProposalResponse> successful = new LinkedList<>();
            for (ProposalResponse ire : ires) {
                if (ire.getStatus() == ProposalResponse.Status.SUCCESS) {
                    successful.add(ire);
                }
            }
            log.info("安装chaincode成功，successFulSize=" + successful.size());
            successful.clear();

            // 初始化chaincode
            InstantiateProposalRequest instantiateProposalRequest = client.newInstantiationProposalRequest();
            instantiateProposalRequest.setProposalWaitTime(waitTime);
            instantiateProposalRequest.setChaincodeID(ccid);
            instantiateProposalRequest.setChaincodeLanguage(type);
            instantiateProposalRequest.setFcn("init");
            instantiateProposalRequest.setArgs(initParams);
            instantiateProposalRequest.setUserContext(peerAdmin);
            Map<String, byte[]> tm = new HashMap<>();
            tm.put("HyperLedgerFabric", "InstantiateProposalRequest:JavaSDK".getBytes(UTF_8));
            tm.put("method", "InstantiateProposalRequest".getBytes(UTF_8));
            instantiateProposalRequest.setTransientMap(tm);

            ChaincodeEndorsementPolicy chaincodeEndorsementPolicy = new ChaincodeEndorsementPolicy();
            chaincodeEndorsementPolicy.fromStream(chaincodeendorsementInputStream);
            instantiateProposalRequest.setChaincodeEndorsementPolicy(chaincodeEndorsementPolicy);

            Collection<ProposalResponse> sip = channel.sendInstantiationProposal(instantiateProposalRequest, channel.getPeers());
            for (ProposalResponse response : sip) {
                if (response.getStatus() == ProposalResponse.Status.SUCCESS) {
                    successful.add(response);
                }
            }

            log.info("初始化chainCode成功，successFulSize=" + successful.size());
            channel.sendTransaction(successful, createTransactionOptions() //提交事务
                    .userContext(client.getUserContext()) //设置用户
                    .shuffleOrders(false) // 不分先后顺序
                    .orderers(channel.getOrderers()))
                    .get(TimeUnit.MILLISECONDS.toSeconds(waitTime), TimeUnit.SECONDS);

            log.info("部署和初始化链码成功 ccid=" + ccid);
            return BaseChainCodeResponse.success(1, "安装部署成功 ccid=" + ccid);
        } catch (Exception e) {
            log.info("部署和初始化chaincode异常 chainCodeName" + chainCodeName, e);
            return BaseChainCodeResponse.error(-1, e.getMessage());
        }
    }

    @Override
    public BaseChainCodeResponse sendTransaction(SendChainCodeRequest request) {
        String userName = request.getUserName();
        String password = request.getPassword();
        String mspId = request.getMspId();
        String chainCodeName = request.getChainCodeName();
        String chainCodeVersion = request.getChainCodeVersion();
        String chainCodeType = request.getChainCodeType();
        String methodName = request.getMethodName();
        String[] params = request.getParams();
        try {
            log.info(userName + "发送交易请求开始，chainCodeName=" + chainCodeName);
            ChainCodeUser peerAdmin = getPeerAdmin();
            HFClient client = getClient(peerAdmin);
            Channel channel = getOrCreateChannel(peerAdmin, client);

            ChainCodeUser hfUser = new ChainCodeUser(userName, mspId, getCAClient().enroll(userName, password));
            client.setUserContext(hfUser);

            ChaincodeID ccid = ChaincodeID.newBuilder().setName(chainCodeName).setVersion(chainCodeVersion).build();

            TransactionProposalRequest transactionProposalRequest = client.newTransactionProposalRequest();
            transactionProposalRequest.setChaincodeID(ccid);
            transactionProposalRequest.setChaincodeLanguage(TransactionRequest.Type.valueOf(chainCodeType.toUpperCase()));
            transactionProposalRequest.setFcn(methodName);
            transactionProposalRequest.setProposalWaitTime(waitTime);
            transactionProposalRequest.setArgs(params);

            Map<String, byte[]> tm2 = new HashMap<>();
            tm2.put("HyperLedgerFabric", "TransactionProposalRequest:JavaSDK".getBytes(UTF_8));
            tm2.put("method", "TransactionProposalRequest".getBytes(UTF_8));
            tm2.put("result", ":)".getBytes(UTF_8));
            tm2.put("event", "!".getBytes(UTF_8));
            transactionProposalRequest.setTransientMap(tm2);

            Collection<ProposalResponse> successful = new LinkedList<>();

            Collection<ProposalResponse> transactionPropResp = channel.sendTransactionProposal(transactionProposalRequest, channel.getPeers());
            for (ProposalResponse response : transactionPropResp) {
                if (response.getStatus() == ProposalResponse.Status.SUCCESS) {
                    successful.add(response);
                }
            }
            log.info("执行交易成功，successFulSize=" + successful.size());
            channel.sendTransaction(successful).get(120, TimeUnit.SECONDS);
            return BaseChainCodeResponse.success(1, "执行交易成功");
        } catch (Exception e) {
            log.error(userName + "执行交易失败，chainCodeName=" + chainCodeName, e);
            return BaseChainCodeResponse.error(-1, e.getMessage());
        }
    }

    @Override
    public BaseChainCodeResponse queryByChainCode(SendChainCodeRequest request) {
        String userName = request.getUserName();
        String password = request.getPassword();
        String mspId = request.getMspId();
        String chainCodeName = request.getChainCodeName();
        String chainCodeVersion = request.getChainCodeVersion();
        String chainCodeType = request.getChainCodeType();
        String methodName = request.getMethodName();
        String[] params = request.getParams();
        try {
            log.info(userName + "查询链码请求开始，chainCodeName=" + chainCodeName);
            ChainCodeUser peerAdmin = getPeerAdmin();
            HFClient client = getClient(peerAdmin);
            Channel channel = getOrCreateChannel(peerAdmin, client);

            ChainCodeUser hfUser = new ChainCodeUser(userName, mspId, getCAClient().enroll(userName, password));
            client.setUserContext(hfUser);

            ChaincodeID ccid = ChaincodeID.newBuilder().setName(chainCodeName).setVersion(chainCodeVersion).build();

            //查询合约信息
            QueryByChaincodeRequest queryByChaincodeRequest = client.newQueryProposalRequest();
            queryByChaincodeRequest.setArgs(params);
            queryByChaincodeRequest.setFcn(methodName);
            queryByChaincodeRequest.setChaincodeID(ccid);
            queryByChaincodeRequest.setChaincodeLanguage(TransactionRequest.Type.valueOf(chainCodeType.toUpperCase()));
            Map<String, byte[]> tm3 = new HashMap<>();
            tm3.put("HyperLedgerFabric", "QueryByChaincodeRequest:JavaSDK".getBytes(UTF_8));
            tm3.put("method", "QueryByChaincodeRequest".getBytes(UTF_8));
            queryByChaincodeRequest.setTransientMap(tm3);
            Collection<ProposalResponse> queryProposals = channel.queryByChaincode(queryByChaincodeRequest, channel.getPeers());
            JSONObject msg = new JSONObject();
            for (ProposalResponse proposalResponse : queryProposals) {
                if (proposalResponse.isVerified() && proposalResponse.getStatus() == ProposalResponse.Status.SUCCESS) {
                    msg.put(proposalResponse.getPeer().getName(), proposalResponse.getProposalResponse().getResponse().getPayload().toStringUtf8());
                }
            }
            log.info("查询chainCode成功");
            return BaseChainCodeResponse.success(1, msg.toJSONString());
        } catch (Exception e) {
            log.error(userName + "查询chainCode异常，chainCodeName=" + chainCodeName, e);
            return BaseChainCodeResponse.error(-1, e.getMessage());
        }
    }


    private ChainCodeUser getPeerAdmin() throws IOException {
        try {
            String certificate = FileUtils.readFileToString(new File(config.getProperty("peer.admin.certFilePath")), "utf-8");
            PrivateKey privateKey = getPrivateKeyFromBytes(FileUtils.readFileToByteArray(new File(config.getProperty("peer.admin.privateKeyFilePath"))));
            Enrollment enrollment = new ChainCodeUser.SampleStoreEnrollement(privateKey, certificate);
            log.info("获取peerAdmin成功");
            return new ChainCodeUser(config.getProperty("peer.admin.name"), config.getProperty("peer.admin.mspId"), enrollment);
        } catch (Exception e) {
            log.error("获取peerAdmin异常", e);
            throw e;
        }
    }

    private HFCAClient getCAClient() throws MalformedURLException, org.hyperledger.fabric_ca.sdk.exception.InvalidArgumentException, IllegalAccessException, InvocationTargetException, InvalidArgumentException, InstantiationException, NoSuchMethodException, CryptoException, ClassNotFoundException {
        HFCAClient caClient = HFCAClient.createNewInstance(config.getProperty("ca.name"), config.getProperty("ca.url"), null);
        caClient.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
        return caClient;
    }

    private HFClient getClient(ChainCodeUser peerAdmin) throws InvalidArgumentException, IllegalAccessException, InvocationTargetException, InstantiationException, NoSuchMethodException, CryptoException, ClassNotFoundException {
        HFClient client = HFClient.createNewInstance();
        client.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
        client.setUserContext(peerAdmin);
        return client;
    }

    private Channel getOrCreateChannel(ChainCodeUser peerAdmin, HFClient client) throws InvalidArgumentException, IOException, TransactionException, ProposalException {
        try {
            String channelName = config.getProperty("channel.name");
            Channel channel = client.getChannel(channelName);
            if (channel == null) {
                //设置order
                JSONObject order = JSONObject.parseObject(config.getProperty("channel.order"));
                Orderer orderer = client.newOrderer(order.getString("name"), order.getString("grpcUrl"), null);
                ChannelConfiguration channelConfiguration = new ChannelConfiguration(new File(config.getProperty("channel.txFilePath")));
                channel = client.newChannel(channelName, orderer, channelConfiguration, client.getChannelConfigurationSignature(channelConfiguration, peerAdmin));
                //设置peers
                JSONArray peers = JSONObject.parseArray(config.getProperty("channel.peers"));
                for (int i = 0; i < peers.size(); i++) {
                    JSONObject peer = peers.getJSONObject(i);
                    Peer tempPeer = client.newPeer(peer.getString("name"), peer.getString("grpcUrl"), null);
                    channel.joinPeer(tempPeer, createPeerOptions().setPeerRoles(EnumSet.of(Peer.PeerRole.ENDORSING_PEER, Peer.PeerRole.LEDGER_QUERY, Peer.PeerRole.CHAINCODE_QUERY)));
                }
                //设置eventHubs
                JSONArray eventHubs = JSONObject.parseArray(config.getProperty("channel.eventHubs"));
                for (int i = 0; i < eventHubs.size(); i++) {
                    JSONObject eventHub = eventHubs.getJSONObject(i);
                    EventHub tempEventHub = client.newEventHub(eventHub.getString("name"), eventHub.getString("grpcUrl"), null);
                    channel.addEventHub(tempEventHub);
                }
            }
            log.info("获取channel成功");
            return channel.isInitialized() ? channel : channel.initialize();
        } catch (Exception e) {
            log.error("获取或构造channel异常", e);
            throw e;
        }
    }


}
