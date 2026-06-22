package com.cheatlearn.bgmiloader;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.Enumeration;

public final class KeystoreDiagnostics {
    private static final String PASSWORD = "123456";
    private static final String ALIAS = "key0";

    private KeystoreDiagnostics() {}

    public static String run(Context context) {
        StringBuilder out = new StringBuilder();
        String defaultType = KeyStore.getDefaultType();
        out.append("Default type: ").append(defaultType).append('\n');

        File generated = new File(context.getCacheDir(), "diagnostic_keystore.p12");
        try {
            KeystoreGenerator.generate(generated, PASSWORD, ALIAS);
            out.append("Generated: ").append(generated.length()).append(" bytes\n");
        } catch (Throwable t) {
            out.append("Generate failed: ").append(message(t)).append('\n');
            return out.toString();
        }

        out.append(checkFile("Generated as PKCS12", generated, "PKCS12"));
        out.append(checkFile("Generated as default", generated, defaultType));

        try {
            out.append(checkAsset(context, "Asset as PKCS12", "legacy_keystore.p12", "PKCS12"));
            out.append(checkAsset(context, "Asset as default", "legacy_keystore.p12", defaultType));
        } catch (Throwable t) {
            out.append("Asset check failed: ").append(message(t)).append('\n');
        }

        return out.toString();
    }

    private static String checkFile(String label, File file, String type) {
        try (InputStream is = new FileInputStream(file)) {
            return check(label, is, type);
        } catch (Throwable t) {
            return label + ": FAIL opening file: " + message(t) + '\n';
        }
    }

    private static String checkAsset(Context context, String label, String assetName, String type) throws Exception {
        try (InputStream is = context.getAssets().open(assetName)) {
            return check(label, is, type);
        }
    }

    private static String check(String label, InputStream input, String type) {
        try {
            KeyStore ks = KeyStore.getInstance(type);
            ks.load(input, PASSWORD.toCharArray());
            boolean hasAlias = ks.containsAlias(ALIAS);
            String firstAlias = firstAlias(ks);
            return label + ": OK type=" + type
                + ", alias key0=" + hasAlias
                + ", firstAlias=" + firstAlias + '\n';
        } catch (Throwable t) {
            return label + ": FAIL type=" + type + ": " + message(t) + '\n';
        }
    }

    private static String firstAlias(KeyStore ks) throws Exception {
        Enumeration<String> aliases = ks.aliases();
        return aliases.hasMoreElements() ? aliases.nextElement() : "<none>";
    }

    private static String message(Throwable t) {
        StringBuilder sb = new StringBuilder(t.getClass().getSimpleName());
        if (t.getMessage() != null) sb.append(": ").append(t.getMessage());
        Throwable cause = t.getCause();
        if (cause != null && cause.getMessage() != null) {
            sb.append(" -> ").append(cause.getClass().getSimpleName())
                .append(": ").append(cause.getMessage());
        }
        return sb.toString();
    }
}
