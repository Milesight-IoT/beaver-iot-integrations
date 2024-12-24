package com.milesight.beaveriot.gateway.util;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
public class AESUtil {
    public static void main(String[] args) throws Exception {
        String plaintext = "password";
        String secretKey = "1111111111111111";
        String iv = "2222222222222222";
        // 加密
        String encryptedText = encrypt(plaintext, secretKey, iv);
        System.out.println("加密后的密文: " + encryptedText);
    }

    public static String encrypt(String plaintext, String key, String iv) throws Exception {
        // 创建AES密钥
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");

        // 创建IV参数
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv.getBytes(StandardCharsets.UTF_8));

        // 创建Cipher实例
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);

        // 加密
        byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

        // 将加密后的字节数组转换为Base64字符串
        return Base64.getEncoder().encodeToString(encrypted);
    }
}

