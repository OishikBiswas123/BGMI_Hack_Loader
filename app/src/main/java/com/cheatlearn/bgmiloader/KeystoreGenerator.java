package com.cheatlearn.bgmiloader;

import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Date;

public class KeystoreGenerator {

    private static final String TAG = "KeystoreGenerator";
    private static final String[] BC_CLASSES = {
        "com.android.org.bouncycastle.x509.X509V3CertificateGenerator",
        "org.bouncycastle.x509.X509V3CertificateGenerator"
    };

    public static void generate(File outputFile, String password, String alias) throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048, new SecureRandom());
        KeyPair kp = kpg.generateKeyPair();

        Log.i(TAG, "Generating self-signed X509 certificate...");
        X509Certificate cert = createSelfSignedCert(kp);

        Log.i(TAG, "Storing as PKCS12 keystore...");
        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(null, null);
        ks.setKeyEntry(alias, kp.getPrivate(), password.toCharArray(),
                new X509Certificate[]{cert});

        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            ks.store(fos, password.toCharArray());
        }
        Log.i(TAG, "Keystore generated: " + outputFile.getAbsolutePath());
    }

    private static X509Certificate createSelfSignedCert(KeyPair kp) throws Exception {
        Class<?> clazz = findBCClass();
        Object certGen = clazz.getDeclaredConstructor().newInstance();
        String dn = "CN=Loader, O=CheatLearn, C=IN";

        setField(certGen, "setSerialNumber", BigInteger.valueOf(System.currentTimeMillis()));
        setIssuerAndSubject(certGen, dn);
        long now = System.currentTimeMillis();
        setField(certGen, "setNotBefore", new Date(now - 86400000L));
        setField(certGen, "setNotAfter", new Date(now + 365L * 86400000L * 20));
        setField(certGen, "setPublicKey", kp.getPublic());
        setField(certGen, "setSignatureAlgorithm", "SHA256WithRSAEncryption");

        X509Certificate cert = generateCert(certGen, kp);

        cert.verify(kp.getPublic());
        Log.i(TAG, "Self-signed certificate generated and verified");
        return cert;
    }

    private static Class<?> findBCClass() throws ClassNotFoundException {
        for (String name : BC_CLASSES) {
            try {
                return Class.forName(name);
            } catch (ClassNotFoundException e) {
                continue;
            }
        }
        throw new ClassNotFoundException("BouncyCastle X509V3CertificateGenerator not found on this device");
    }

    private static void setField(Object obj, String methodName, Object value) throws Exception {
        for (Method m : obj.getClass().getMethods()) {
            if (m.getName().equals(methodName) && m.getParameterCount() == 1) {
                Class<?> paramType = m.getParameterTypes()[0];
                if (paramType.isInstance(value)) {
                    m.invoke(obj, value);
                    return;
                }
                if (value instanceof BigInteger && paramType.equals(BigInteger.class)) {
                    m.invoke(obj, value);
                    return;
                }
            }
        }
        throw new NoSuchMethodException(methodName + " with compatible param not found");
    }

    private static void setIssuerAndSubject(Object certGen, String dn) throws Exception {
        javax.security.auth.x500.X500Principal x500 =
                new javax.security.auth.x500.X500Principal(dn);
        for (Method m : certGen.getClass().getMethods()) {
            if (m.getName().equals("setIssuerDN") && m.getParameterCount() == 1) {
                Class<?> pType = m.getParameterTypes()[0];
                if (pType.equals(javax.security.auth.x500.X500Principal.class)) {
                    m.invoke(certGen, x500);
                    break;
                }
                if (pType.equals(String.class)) {
                    m.invoke(certGen, dn);
                    break;
                }
                if (pType.equals(Object.class)) {
                    m.invoke(certGen, x500);
                    break;
                }
            }
        }
        for (Method m : certGen.getClass().getMethods()) {
            if (m.getName().equals("setSubjectDN") && m.getParameterCount() == 1) {
                Class<?> pType = m.getParameterTypes()[0];
                if (pType.equals(javax.security.auth.x500.X500Principal.class)) {
                    m.invoke(certGen, x500);
                    break;
                }
                if (pType.equals(String.class)) {
                    m.invoke(certGen, dn);
                    break;
                }
                if (pType.equals(Object.class)) {
                    m.invoke(certGen, x500);
                    break;
                }
            }
        }
    }

    private static X509Certificate generateCert(Object certGen, KeyPair kp) throws Exception {
        for (Method m : certGen.getClass().getMethods()) {
            if (!m.getName().equals("generate")) continue;
            int pc = m.getParameterCount();
            if (pc == 1 && m.getParameterTypes()[0].equals(java.security.PrivateKey.class)) {
                return (X509Certificate) m.invoke(certGen, kp.getPrivate());
            }
            if (pc == 2
                    && m.getParameterTypes()[0].equals(java.security.PrivateKey.class)
                    && m.getParameterTypes()[1].equals(String.class)) {
                return (X509Certificate) m.invoke(certGen, kp.getPrivate(), "BC");
            }
        }
        throw new NoSuchMethodException("generate(PrivateKey) not found");
    }
}
