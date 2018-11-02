package com.ule.merchant.chaincode;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.junit.Test;

import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ChainCodeMerchantUtil {

    private static final Logger log = Logger.getLogger(ChainCodeMerchantUtil.class);
    private static final String host = "http://localhost:8080/chaincode";
    private boolean deploayAndInitChainCode = false;//是否已部署和初始化链码
    private ChainCodeDBUser user1;//是否存在用户

    private void checkIsDeploayAndInitChainCode() {
        try {
            //部署和实例化链码
            JSONObject resObject = new JSONObject();
            String res = sendByPostForApplicationJson(host + "/deployAndInitMerchantContract", new HashMap<>());
            log.info("发送部署链码请求 res=" + res);
            resObject = JSONObject.parseObject(res);
            deploayAndInitChainCode = resObject.getIntValue("code") > -1;
        } catch (Exception e) {
            log.error("部署和实例化链码异常", e);
        }
    }

    private void checkRegChainCodeUser() {
        try {
            if (user1 == null) {
                ChainCodeDBUser user = new ChainCodeDBUser();
                user.setAffiliation("org1.department1");
                user.setMspId("Org1MSP");
                user.setUserName("user1");
                user.setPassword("tOFGJEGQqemC");
                user1 = user;
            }
        } catch (Exception e) {
            log.error("注册用户异常", e);
        }
    }

    private void checkChainCodeProfile() {
        if (!deploayAndInitChainCode) {
            log.info("开始部署链码");
            checkIsDeploayAndInitChainCode();
            log.info("部署链码完毕");
        }
        if (user1 == null) {
            log.info("开始处理用户");
            checkRegChainCodeUser();
            log.info("用户处理完毕");
        }
    }

    @Test
    public void run() throws Exception {
        String res=sendTransaction("1023432","{sdafsd123421}");
        System.out.println(res);
        res=queryByChainCode("1023432");
        System.out.println(res);
    }

    public String sendTransaction(String merchantId, String merchantInfo) throws Exception {
        checkChainCodeProfile();

        Map<String, Object> transMap = new HashMap<>();
        transMap.put("chainCodeName", "uleMerchantChainCode");
        transMap.put("chainCodeType", "GO_LANG");
        transMap.put("chainCodePath", "ule.com/gocc");
        transMap.put("chainCodeVersion", "1");
        transMap.put("methodName", "addOrUpdate");
        transMap.put("params", Arrays.asList(merchantId,merchantInfo).toArray(new String[]{}));
        transMap.put("userName", user1.getUserName());
        transMap.put("password", user1.getPassword());
        transMap.put("mspId", user1.getMspId());
        return sendByPostForApplicationJson(host + "/sendTransaction",  transMap);
    }

    public String queryByChainCode(String merchantId) throws Exception {
        checkChainCodeProfile();

        Map<String, Object> transMap = new HashMap<>();
        transMap.put("chainCodeName", "uleMerchantChainCode");
        transMap.put("chainCodeType", "GO_LANG");
        transMap.put("chainCodePath", "ule.com/gocc");
        transMap.put("chainCodeVersion", "1");
        transMap.put("methodName", "query");
        transMap.put("params", Collections.singletonList(merchantId).toArray(new String[]{}));
        transMap.put("userName", user1.getUserName());
        transMap.put("password", user1.getPassword());
        transMap.put("mspId", user1.getMspId());
        return sendByPostForApplicationJson(host + "/queryByChainCode", transMap);
    }


    private static String sendByPostForApplicationJson(String httpUrl, Map<String, Object> paramMap) throws Exception {
        try {
            URL url = new URL(httpUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.setUseCaches(false);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.connect();
            OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
            //发送参数
            writer.write(JSON.toJSONString(paramMap));
            //清理当前编辑器的左右缓冲区，并使缓冲区数据写入基础流
            writer.flush();
            String result = IOUtils.toString(connection.getInputStream(),"utf-8");
            connection.disconnect();
            return result;
        } catch (Exception var14) {
            log.error("发送POST请求出错", var14);
            var14.printStackTrace();
            throw new Exception("发送POST请求出错");
        }
    }
}
