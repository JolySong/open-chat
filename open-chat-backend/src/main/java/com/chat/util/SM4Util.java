package com.chat.util;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Hex;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.util.Base64;

public class SM4Util {
    private static final String ALGORITHM_NAME = "SM4/ECB/PKCS7Padding";
    // 密钥必须是16字节的十六进制字符串
    private static final String KEY = "31323334353637383930414243444546";

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    private static String byteArrayToHexString(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    // 加密
    public static String encrypt(String data) {
        try {
            if (data == null) return null;
            Cipher cipher = Cipher.getInstance(ALGORITHM_NAME, "BC");
            SecretKeySpec sm4Key = new SecretKeySpec(hexStringToByteArray(KEY), "SM4");
            cipher.init(Cipher.ENCRYPT_MODE, sm4Key);
            byte[] encrypted = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return byteArrayToHexString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("SM4 encryption failed", e);
        }
    }

    // 解密
    public static String decrypt(String data) {
        try {
            if (data == null) return null;
            Cipher cipher = Cipher.getInstance(ALGORITHM_NAME, "BC");
            SecretKeySpec sm4Key = new SecretKeySpec(hexStringToByteArray(KEY), "SM4");
            cipher.init(Cipher.DECRYPT_MODE, sm4Key);
            byte[] decrypted = cipher.doFinal(hexStringToByteArray(data));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("SM4 decryption failed", e);
        }
    }
} 