package com.ule.merchant.chaincode.model;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.User;

import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;
import java.security.PrivateKey;
import java.security.Security;
import java.util.Set;

//用户
public class ChainCodeUser implements User, Serializable {

    private String name;
    private String mspId;
    private Enrollment enrollment;

    public ChainCodeUser(String name, String mspId, Enrollment enrollment) {
        this.name = name;
        this.mspId = mspId;
        this.enrollment = enrollment;
    }

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public static PrivateKey getPrivateKeyFromBytes(byte[] data) throws IOException {
        return getPrivateKey(data);
    }

    private static PrivateKey getPrivateKey(byte[] data) throws IOException {
        final Reader pemReader = new StringReader(new String(data));
        final PrivateKeyInfo pemPair;
        try (PEMParser pemParser = new PEMParser(pemReader)) {
            pemPair = (PrivateKeyInfo) pemParser.readObject();
        }
        return new JcaPEMKeyConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME).getPrivateKey(pemPair);
    }

    public static final class SampleStoreEnrollement implements Enrollment, Serializable {
        private static final long serialVersionUID = -2784835212445309006L;
        private final PrivateKey privateKey;
        private final String certificate;

        public SampleStoreEnrollement(PrivateKey privateKey, String certificate) {
            this.certificate = certificate;
            this.privateKey = privateKey;
        }

        @Override
        public PrivateKey getKey() {

            return privateKey;
        }

        @Override
        public String getCert() {
            return certificate;
        }

    }

    @Override
    public String getName() {
        return name;
    }

    //ignore attr
    @Override
    public Set<String> getRoles() {
        return null;
    }
    //ignore attr
    @Override
    public String getAccount() {
        return null;
    }
    //ignore attr
    @Override
    public String getAffiliation() {
        return null;
    }

    @Override
    public String getMspId() {
        return mspId;
    }

    @Override
    public Enrollment getEnrollment() {
        return enrollment;
    }

}