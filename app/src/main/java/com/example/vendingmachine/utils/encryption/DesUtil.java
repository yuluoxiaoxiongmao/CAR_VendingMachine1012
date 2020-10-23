package com.example.vendingmachine.utils.encryption;

/**
 * Created by Tony on 2020-05-18.
 */


import android.util.Log;

import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.IvParameterSpec;

import static com.example.vendingmachine.platform.common.SocketClient2.bytesToHexString;

public class DesUtil {

    /**
     * 加密 外部调用
     *
     * @param srcStr
     * @param charset
     * @param sKey
     * @return
     */

//    public static String encrypt(String srcStr, Charset charset, String sKey) {
//        byte[] src = srcStr.getBytes(charset);
//        byte[] buf = encrypt(src, sKey);
//        return parseByte2HexStr(buf);
//    }

    /**
     * 解密 外部调用
     *
     * @param hexStr
     * @param sKey
     * @return
     */

//    public static String decrypt(String hexStr, Charset charset, String sKey) throws Exception {
//        byte[] src = parseHexStr2Byte(hexStr);
//        byte[] buf = decrypt(src, sKey);
//        return new String(buf, charset);
//    }

    /**
     * 加密 内部调用
     *
     * @param data
     * @param sKey
     * @return
     */

    public static byte[] encrypt(byte[] data, String sKey,byte[] key) {
        try {
//            byte[] key1 = sKey.getBytes();
            // 初始化向量
            IvParameterSpec iv = new IvParameterSpec(key);
            DESKeySpec desKey = new DESKeySpec(key);
            // 创建一个密匙工厂，然后用它把DESKeySpec转换成securekey
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
            SecretKey securekey = keyFactory.generateSecret(desKey);
            // Cipher对象实际完成加密操作
            Cipher cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
            // 用密匙初始化Cipher对象
            cipher.init(Cipher.ENCRYPT_MODE, securekey, iv);
            // 现在，获取数据并加密
            // 正式执行加密操作
            return cipher.doFinal(data);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 解密 内部调用
     *
     * @param src
     * @param sKey
     * @return
     */

    public static byte[] decrypt(byte[] src, String sKey,byte[] key) throws Exception {
//        byte[] key = sKey.getBytes();
        // 初始化向量
        IvParameterSpec iv = new IvParameterSpec(key);
        // 创建一个DESKeySpec对象
        DESKeySpec desKey = new DESKeySpec(key);
        // 创建一个密匙工厂
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
        // 将DESKeySpec对象转换成SecretKey对象
        SecretKey securekey = keyFactory.generateSecret(desKey);
        // Cipher对象实际完成解密操作
        Cipher cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
        // 用密匙初始化Cipher对象
        cipher.init(Cipher.DECRYPT_MODE, securekey, iv);
        // 真正开始解密操作
        return cipher.doFinal(src);
    }

    /**
     * 将二进制转换成16进制
     *
     * @param buf
     * @return
     */

    public static String parseByte2HexStr(byte buf[]) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < buf.length; i++) {
            String hex = Integer.toHexString(buf[i] & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            sb.append(hex.toUpperCase());
        }
        return sb.toString();
    }

    /**
     * 将16进制转换为二进制
     *
     * @param hexStr
     * @return
     */

    public static byte[] parseHexStr2Byte(String hexStr) {
        if (hexStr.length() < 1) return null;
        byte[] result = new byte[hexStr.length() / 2];
        for (int i = 0; i < hexStr.length() / 2; i++) {
            int high = Integer.parseInt(hexStr.substring(i * 2, i * 2 + 1), 16);
            int low = Integer.parseInt(hexStr.substring(i * 2 + 1, i * 2 + 2), 16);
            result[i] = (byte) (high * 16 + low);
        }
        return result;
    }

    public static void main() throws InvalidKeyException,
            NoSuchAlgorithmException, InvalidKeySpecException,
            NoSuchPaddingException, IllegalBlockSizeException,
            BadPaddingException {
        String soureTxt = "123456";
        String key = "3132333435363738";
        String str = null;

        byte[] key_des = new byte[8];
        key_des[0] = (byte) 0x00;
        key_des[1] = (byte) 0x00;
        key_des[2] = (byte) 0x00;
        key_des[3] = (byte) 0x00;
        key_des[4] = (byte) 0x00;
        key_des[5] = (byte) 0x00;
        key_des[6] = (byte) 0x00;
        key_des[7] = (byte) 0x00;

        byte[] key_1 = new byte[8];
        key_1[0] = (byte) 0x31;
        key_1[1] = (byte) 0x32;
        key_1[2] = (byte) 0x33;
        key_1[3] = (byte) 0x34;
        key_1[4] = (byte) 0x35;
        key_1[5] = (byte) 0x36;
        key_1[6] = (byte) 0x37;
        key_1[7] = (byte) 0x38;

        Log.e("aaa_明文", key_des.toString());
        Log.e("aaa_key", key);
//        Log.e("aaa_加密",enCrypto(soureTxt, key));
//        Log.e("aaa_解密",deCrypto(str, key));

        try {
            Log.e("aaa_加密2", encrypt(key_des,"", key_1).toString());
            Log.e("aaa_解密2", decrypt(key_des, "",key_1).toString());
        }catch (Exception e){}


    }
}
