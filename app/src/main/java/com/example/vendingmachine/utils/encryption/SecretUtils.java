package com.example.vendingmachine.utils.encryption;

/**
 * Created by 10751 on 2020-06-24.
 */

import android.util.Base64;

import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**

 * SecretUtils {3DES加密解密的工具类 }

 */

public class SecretUtils {

 //定义加密算法，有DES、DESede(即3DES)、Blowfish

//    private static final String Algorithm = "DESede"+ "/ECB/NoPadding";
    private static final String PASSWORD_CRYPT_KEY = "wanghnping12345123123pingwanghanpissdf";

 /**

  * 加密

  * @param data 加密数据

  * @param key 加密密钥

  * @return

  * @throws Exception

  */

         public static String encrypt3DES(byte[] data, byte[] key) throws Exception {

             SecretKey secretKey = new SecretKeySpec(key, Algorithm);

             // Cipher完成加密

             Cipher cipher = Cipher.getInstance(Algorithm);

             // cipher初始化

             cipher.init(Cipher.ENCRYPT_MODE, secretKey);

             byte[] encrypt = cipher.doFinal(data);

             //转码 base64转码

             return new String(Base64.encode(encrypt, Base64.DEFAULT), "UTF-8");
         }

 /**

  * 解密

  * @param data 加密后的字符串

  * @param key 加密密码

  * @return

  * @throws Exception

  */

         public static String decrypt3DES(String data, byte[] key) throws Exception {

            // 恢复密钥

            SecretKey secretKey = new SecretKeySpec(key, Algorithm);
            // Cipher完成解密

            Cipher cipher = Cipher.getInstance(Algorithm);

            // 初始化cipher

            cipher.init(Cipher.DECRYPT_MODE, secretKey);

           //转码

            byte[] bytes = Base64.decode(data.getBytes("UTF-8"), Base64.DEFAULT);

           //解密

            byte[] plain = cipher.doFinal(bytes);

           //解密结果转码

            return new String(plain, "utf-8");

         }


    public static byte[] encrypt3DES_byte(byte[] data, byte[] key) throws Exception {

        SecretKey secretKey = new SecretKeySpec(key, Algorithm);

        // Cipher完成加密

        Cipher cipher = Cipher.getInstance(Algorithm);

        // cipher初始化
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);

        byte[] encrypt = cipher.doFinal(data);

        //转码 base64转码

        return encrypt;
    }

    public static byte[] decrypt3DES_byte(byte[] data, byte[] key) throws Exception {

        // 恢复密钥

        SecretKey secretKey = new SecretKeySpec(key, Algorithm);
        // Cipher完成解密

        Cipher cipher = Cipher.getInstance(Algorithm);

        // 初始化cipher

        cipher.init(Cipher.DECRYPT_MODE, secretKey);

        //转码

//        byte[] bytes = Base64.decode(data.getBytes("UTF-8"), Base64.DEFAULT);

        //解密

        byte[] plain = cipher.doFinal(data);

        //解密结果转码

        return plain;

    }

    /* 定义加密方式, DESede:加密算法; ECB:工作模式 ; NOPadding:填充方式 */
    private static final String Algorithm = "DESede/ECB/NOPadding";

    /**
     * 说明 :3DES加密
     *
     * @param keybyte 密钥
     * @return
     * @key data     明文
     */
    public static byte[] encryptMode(byte[] data, byte[] keybyte) {
        try {
            // 生成密钥
            SecretKey deskey = new SecretKeySpec(keybyte, "DESede");
            // 加密
            Cipher c1 = Cipher.getInstance(Algorithm);
            c1.init(Cipher.ENCRYPT_MODE, deskey);
            byte result[] = c1.doFinal(data);
            return result;
        } catch (NoSuchAlgorithmException e1) {
            e1.printStackTrace();
        } catch (javax.crypto.NoSuchPaddingException e2) {
            e2.printStackTrace();
        } catch (Exception e3) {
            e3.printStackTrace();
        }
        return null;
    }

    /**
     * 说明 :3DES解密
     *
     * @param data    密文
     * @param keybyte 密钥
     * @return
     */

    public static byte[] decryptMode(byte[] data, byte[] keybyte) {
        try {
            // 生成密钥
            SecretKey deskey = new SecretKeySpec(keybyte, "DESede");
            // 解密
            Cipher c1 = Cipher.getInstance(Algorithm);
            c1.init(Cipher.DECRYPT_MODE, deskey);
            byte[] result = c1.doFinal(data);
            return result;

        } catch (NoSuchAlgorithmException e1) {
            e1.printStackTrace();
        } catch (javax.crypto.NoSuchPaddingException e2) {
            e2.printStackTrace();
        } catch (Exception e3) {
            e3.printStackTrace();
        }
        return null;
    }

}


