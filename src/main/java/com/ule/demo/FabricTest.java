package com.ule.demo;

import org.apache.commons.io.FileUtils;
import org.hyperledger.fabric.protos.peer.Query;
import org.hyperledger.fabric.sdk.*;
import org.hyperledger.fabric.sdk.helper.Config;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric_ca.sdk.HFCAClient;
import org.hyperledger.fabric_ca.sdk.RegistrationRequest;

import java.io.File;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.ule.demo.HFUser.getPrivateKeyFromBytes;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hyperledger.fabric.sdk.Channel.PeerOptions.createPeerOptions;
import static org.hyperledger.fabric.sdk.Channel.TransactionOptions.createTransactionOptions;

public class FabricTest {


    public static void main(String[] args) throws Exception {
        //设置块等待时间两分钟
        System.setProperty(Config.GENESISBLOCK_WAIT_TIME, "120000");

        // Step 1 初始化ca客户端
        HFCAClient caClient = HFCAClient.createNewInstance("ca0", "http://192.168.113.117:7054", null);
        caClient.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
        System.out.println("初始化ca客户端成功");

        // Step 2 判断admin用户是否存在，不存在则使用ca客户端注册用户，同时序列化用户对象到硬盘文件
        HFUser admin = SerializeUtils.tryDeserialize("admin");
        if (admin == null) {
            Enrollment adminEnrollment = caClient.enroll("admin", "adminpw");
            admin = new HFUser("admin", "Org1MSP", adminEnrollment);
            SerializeUtils.serializeUser("admin", admin);
            System.out.println("admin注册成功");
        } else {
            System.out.println("反序列化admin成功");
        }

        // Step3 使用admin主体 和ca客户端 注册普通用户tester1
        String userName = "user1";
        HFUser hfUser = SerializeUtils.tryDeserialize(userName);
        if (hfUser == null) {
            RegistrationRequest rr = new RegistrationRequest(userName, "org1.department1");
            String enrollmentSecret = caClient.register(rr, admin);
            Enrollment enrollment = caClient.enroll(userName, enrollmentSecret);
            hfUser = new HFUser(userName, "Org1MSP", enrollment);
            SerializeUtils.serializeUser("user1", hfUser);
            System.out.println("普通用户注册成功");
        } else {
            System.out.println("反序列化普通用户成功");
        }

        //获取peerAdmin
        String adminMspPath = "config/crypto-config/peerOrganizations/org1.example.com/users/Admin@org1.example.com/msp";
        File privateKeyFile = Objects.requireNonNull(new File(adminMspPath + "/keystore").listFiles((dir, name) -> name.endsWith("_sk")))[0];
        File certFile = Objects.requireNonNull(new File(adminMspPath + "/signcerts").listFiles((dir, name) -> name.endsWith(".pem")))[0];
        String certificate = FileUtils.readFileToString(certFile, "utf-8");
        PrivateKey privateKey = getPrivateKeyFromBytes(FileUtils.readFileToByteArray(privateKeyFile));
        Enrollment enrollment = new HFUser.SampleStoreEnrollement(privateKey, certificate);
        HFUser peerAdmin = new HFUser("peerOrg1Admin", "Org1MSP", enrollment);

        // Step 4 获取fabric客户端实例 设置加密组件，admin用户
        HFClient client = HFClient.createNewInstance();
        client.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
        client.setUserContext(peerAdmin);
        System.out.println("获取hyperledger fabric cli客户端成功");

        // Step 5 创建channel，同时设置orders，peers
        String channelName = "bar";
        File channelBitFile = new File(".temp/" + channelName + ".channelBit");
        Channel channel;
        if (!channelBitFile.exists()) {

            Orderer orderer = client.newOrderer("orderer.example.com", "grpc://192.168.113.117:7050", null);
            ChannelConfiguration channelConfiguration = new ChannelConfiguration(new File(".temp/" + channelName + ".tx"));
            channel = client.newChannel(channelName, orderer, channelConfiguration, client.getChannelConfigurationSignature(channelConfiguration, peerAdmin));

            Peer peer0 = client.newPeer("peer0.org1.example.com", "grpc://192.168.113.117:7051", null);
            channel.joinPeer(peer0, createPeerOptions().setPeerRoles(EnumSet.of(Peer.PeerRole.ENDORSING_PEER, Peer.PeerRole.LEDGER_QUERY, Peer.PeerRole.CHAINCODE_QUERY)));

            Peer peer1 = client.newPeer("peer1.org1.example.com", "grpc://192.168.113.117:7056", null);
            channel.joinPeer(peer1, createPeerOptions().setPeerRoles(EnumSet.of(Peer.PeerRole.ENDORSING_PEER, Peer.PeerRole.LEDGER_QUERY, Peer.PeerRole.CHAINCODE_QUERY)));

            EventHub eventHub1 = client.newEventHub("peer0.org1.example.com", "grpc://192.168.113.117:7053", null);
            channel.addEventHub(eventHub1);

            EventHub eventHub2 = client.newEventHub("peer1.org1.example.com", "grpc://192.168.113.117:7058", null);
            channel.addEventHub(eventHub2);
            channel.initialize();
            byte[] serializedChannelBytes = channel.serializeChannel();
            channel.shutdown(true);
            FileUtils.writeByteArrayToFile(channelBitFile, serializedChannelBytes);
        }
        byte[] bits = FileUtils.readFileToByteArray(channelBitFile);
        channel = client.deSerializeChannel(bits).initialize();
        System.out.println("反序列化channel成功");


        Collection<ProposalResponse> successful = new LinkedList<>();

        // Step 6 安装chaincode
        String uleCodeName = "ule_cc_go1";
        final ChaincodeID ccid = ChaincodeID.newBuilder().setName(uleCodeName)
                .setVersion("1").setPath("ule.com/ule_cc").build();

        //检测服务器是否已经安装过链码
        boolean existChainCode = false;
        List<Query.ChaincodeInfo> ccInfoList = client.queryInstalledChaincodes(channel.getPeers().iterator().next());
        if (ccInfoList != null && ccInfoList.size() > 0) {
            for (Query.ChaincodeInfo chaincodeInfo : ccInfoList) {
                if (chaincodeInfo.getName().equals(uleCodeName)) {
                    existChainCode = true;
                    break;
                }
            }
        }
        //如果不存在则安装和实例化
        if (!existChainCode) {
            InstallProposalRequest ipr = client.newInstallProposalRequest();
            ipr.setChaincodeID(ccid);
            ipr.setChaincodeSourceLocation(Paths.get("config/ulechaincode").toFile());
            ipr.setChaincodePath("ule.com/ule_cc");
            ipr.setChaincodeLanguage(TransactionRequest.Type.GO_LANG);
            ipr.setChaincodeVersion("1");
            Collection<ProposalResponse> ires = client.sendInstallProposal(ipr, channel.getPeers());
            for (ProposalResponse ire : ires) {
                if (ire.getStatus() == ProposalResponse.Status.SUCCESS) {
                    successful.add(ire);
                }
            }
            System.out.println("安装chaincode");
            successful.clear();

            //Step 7 初始化chaincode
            InstantiateProposalRequest instantiateProposalRequest = client.newInstantiationProposalRequest();
            instantiateProposalRequest.setProposalWaitTime(120000);
            instantiateProposalRequest.setChaincodeID(ccid);
            instantiateProposalRequest.setChaincodeLanguage(TransactionRequest.Type.GO_LANG);
            instantiateProposalRequest.setFcn("init");
            instantiateProposalRequest.setArgs("a", UUID.randomUUID().toString());
            instantiateProposalRequest.setUserContext(peerAdmin);
            Map<String, byte[]> tm = new HashMap<>();
            tm.put("HyperLedgerFabric", "InstantiateProposalRequest:JavaSDK".getBytes(UTF_8));
            tm.put("method", "InstantiateProposalRequest".getBytes(UTF_8));
            instantiateProposalRequest.setTransientMap(tm);

            ChaincodeEndorsementPolicy chaincodeEndorsementPolicy = new ChaincodeEndorsementPolicy();
            chaincodeEndorsementPolicy.fromYamlFile(new File("config/chaincodeendorsementpolicy.yaml"));
            instantiateProposalRequest.setChaincodeEndorsementPolicy(chaincodeEndorsementPolicy);

            Collection<ProposalResponse> sip = channel.sendInstantiationProposal(instantiateProposalRequest, channel.getPeers());
            for (ProposalResponse response : sip) {
                if (response.getStatus() == ProposalResponse.Status.SUCCESS) {
                    successful.add(response);
                }
            }
            channel.sendTransaction(successful, createTransactionOptions() //提交事务
                    .userContext(client.getUserContext()) //设置用户
                    .shuffleOrders(false) // 不分先后顺序
                    .orderers(channel.getOrderers()))
                    .get(120, TimeUnit.SECONDS);
            System.out.println("初始化chainCode成功");
        }

        String merchantId = UUID.randomUUID().toString();
        String contractContent = "{" + new Random().nextInt(100000) + "}";
        try {

            successful.clear();
            client.setUserContext(hfUser);

            //Step 8 发送添加或更新合约信息
            ///////////////
            /// Send transaction proposal to all peers
            TransactionProposalRequest transactionProposalRequest = client.newTransactionProposalRequest();
            transactionProposalRequest.setChaincodeID(ccid);
            transactionProposalRequest.setChaincodeLanguage(TransactionRequest.Type.GO_LANG);
            transactionProposalRequest.setFcn("addOrUpdate");
            transactionProposalRequest.setProposalWaitTime(120000);

            transactionProposalRequest.setArgs(merchantId, contractContent);
            System.out.printf("设置参数merchantId=%s,合约内容为%s \n", merchantId, contractContent);

            Map<String, byte[]> tm2 = new HashMap<>();
            tm2.put("HyperLedgerFabric", "TransactionProposalRequest:JavaSDK".getBytes(UTF_8)); //Just some extra junk in transient map
            tm2.put("method", "TransactionProposalRequest".getBytes(UTF_8)); // ditto
            tm2.put("result", ":)".getBytes(UTF_8));  // This should be returned see chaincode why.
            tm2.put("event", "!".getBytes(UTF_8));  //This should trigger an event see chaincode why.
            transactionProposalRequest.setTransientMap(tm2);

            Collection<ProposalResponse> transactionPropResp = channel.sendTransactionProposal(transactionProposalRequest, channel.getPeers());
            for (ProposalResponse response : transactionPropResp) {
                if (response.getStatus() == ProposalResponse.Status.SUCCESS) {
                    successful.add(response);
                }
            }
            ////////////////////////////
            // Send Transaction Transaction to orderer
            channel.sendTransaction(successful).get(120, TimeUnit.SECONDS);
            System.out.println("添加或更新合约结束");
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            //Step 9 查询合约信息
            QueryByChaincodeRequest queryByChaincodeRequest = client.newQueryProposalRequest();
            queryByChaincodeRequest.setArgs(merchantId);
            queryByChaincodeRequest.setFcn("query");
            queryByChaincodeRequest.setChaincodeID(ccid);
            Map<String, byte[]> tm3 = new HashMap<>();
            tm3.put("HyperLedgerFabric", "QueryByChaincodeRequest:JavaSDK".getBytes(UTF_8));
            tm3.put("method", "QueryByChaincodeRequest".getBytes(UTF_8));
            queryByChaincodeRequest.setTransientMap(tm3);
            Collection<ProposalResponse> queryProposals = channel.queryByChaincode(queryByChaincodeRequest, channel.getPeers());
            for (ProposalResponse proposalResponse : queryProposals) {
                if (proposalResponse.isVerified() && proposalResponse.getStatus() == ProposalResponse.Status.SUCCESS) {
                    String payload = proposalResponse.getProposalResponse().getResponse().getPayload().toStringUtf8();
                    System.out.printf("获取合约信息成功：peer=value-> %s = %s\n", proposalResponse.getPeer().getName(), payload);
                }
            }
            System.out.println("结束");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
