package com.example.vendingmachine.utils.encryption;

import android.util.Base64;
import android.util.Log;

import com.example.vendingmachine.platform.common.SocketClient2;
import com.example.vendingmachine.utils.MD5;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import static com.example.vendingmachine.utils.encryption.AESUtils.encrypt128_3;

/**
 * Created by Tony on 2019-10-30.
 */

/**
 * Android AES 16字节加解密
 */
public class AESUtils {
//     加密
    public static String encrypt128_2(byte[] sSrc, byte[] sKey) throws Exception {
        if (sKey == null) {
            return null;
        }
        // 判断Key是否为16位
        if (sKey.length != 16) {
            return null;
        }
//        byte[] raw = sKey.getBytes("utf-8");
        SecretKeySpec skeySpec = new SecretKeySpec(sKey, "AES");
//        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");//"算法/模式/补码方式"
        Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");//"算法/模式/补码方式"
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
        byte[] encrypted = cipher.doFinal(sSrc);

        return Base64.encodeToString(encrypted, Base64.DEFAULT);//此处使用BASE64做转码功能，同时能起到2次加密的作用。
    }

    // 解密
    public static String decrypt128_2(String sSrc, byte[] sKey) throws Exception {
        try {
            // 判断Key是否正确
            if (sKey == null) {
                return null;
            }
            // 判断Key是否为16位
            if (sKey.length != 16) {
                return null;
            }
            Log.e("1111",(sKey.length != 16)+"");
//            byte[] raw = sKey.getBytes("utf-8");
            SecretKeySpec skeySpec = new SecretKeySpec(sKey, "AES");
            // "AES/ECB/PKCS5Padding"解密 android 4.3以上有bug,详情见：
            // https://www.cnblogs.com/suxiaoqi/p/7874635.html
            //Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");//"算法/模式/补码方式"
            cipher.init(Cipher.DECRYPT_MODE, skeySpec);
            byte[] encrypted1 = Base64.decode(sSrc.getBytes("utf-8"), Base64.DEFAULT);
            try {
                byte[] original = cipher.doFinal(encrypted1);
                String originalString = new String(original, "utf-8");
                return originalString;
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println(e.toString());
                return null;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println(ex.toString());
            return null;
        }
    }

    public static byte[] encrypt128_3(byte[] sSrc, byte[] sKey) throws Exception {
        if (sKey == null) {
            return null;
        }
        // 判断Key是否为16位
        if (sKey.length != 16) {
            return null;
        }
//        byte[] raw = sKey.getBytes("utf-8");
        SecretKeySpec skeySpec = new SecretKeySpec(sKey, "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");//"算法/模式/补码方式"
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
        byte[] encrypted = cipher.doFinal(sSrc);
        return encrypted;
    }

    public static byte[] decrypt128_3(byte[] sSrc, byte[] sKey) throws Exception {
        try {
            // 判断Key是否正确
            if (sKey == null) {
                return null;
            }
            // 判断Key是否为16位
            if (sKey.length != 16) {
                return null;
            }
            Log.e("1111",(sKey.length != 16)+"");
//            byte[] raw = sKey.getBytes("utf-8");
            SecretKeySpec skeySpec = new SecretKeySpec(sKey, "AES");
            // "AES/ECB/PKCS5Padding"解密 android 4.3以上有bug,详情见：
            // https://www.cnblogs.com/suxiaoqi/p/7874635.html
            //Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
//            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec);
            byte[] encrypted1 = sSrc;
            try {
                byte[] original = cipher.doFinal(encrypted1);
                Log.e("aes_length",original.length+"");
//                String originalString = new String(original, "utf-8");
                return original;
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println(e.toString());
                Log.e("1112_e1",e.getMessage().toString());
                return null;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println(ex.toString());
            Log.e("1112_e2",ex.getMessage().toString());
            return null;
        }
    }

    public static byte[] encrypt128_4(byte[] sSrc, byte[] sKey) {
        if (sKey == null) {
            return null;
        }
        // 判断Key是否为16位
        if (sKey.length != 16) {
            return null;
        }
        try {
            SecretKeySpec skeySpec = new SecretKeySpec(sKey, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");//"算法/模式/补码方式"
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
            byte[] encrypted = cipher.doFinal(sSrc);
            Log.e("eee_aes11",encrypted.length+"");
            return encrypted;
        }catch (Exception e){
            Log.e("eee_aes12",e.getMessage().toString());
        }

        return null;
    }

    public static String main(String args) throws Exception {
//        String Code = "{\"mac_id\":\"2019102901\",\"code\":\""+args+"a\"}".trim();
//        String key = "jiana154uiud"
        //9f65e37e
//        String info = "EE 11 00 01 02 80 00 78 CF B0 61 10 8D 7A 00 32 00 00 00 01 89 00 71 44 01 72 06 13 00 00 00 00 51 00 00 00 00 00 00 01 51 00 00 05 03 02 00 11 21 12 EA E0 D7 24 27 12 D9 6B 97 BC 44 AE FD 98 FF 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 16 9f 65 e3 7e";
        String info = "00 00 00 01 89 00 71 44 E1 30 07 13 00 00 00 00 51 00 00 04 01 13 71 53 28 00 00 00 00 01 51 00 00 04 03 02 01 13 71 53 2D 02 28 D2 25 12 5C 8B 31 39 96 46 9C CC FF 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 5C 9C A4 79";
        String[] manger_aes = info.trim().split(" ");
        byte[] key_info = new byte[manger_aes.length];
        for (int i = 0; i < manger_aes.length; i++) {
            int num_info_1 = Integer.valueOf(manger_aes[i], 16);
            key_info[i] = (byte) num_info_1;
        }

        byte[] key_16 = new byte[16];
        String key = "DE 97 83 48 9D 28 EC ED 9F 49 94 66 6E 29 AB 94";
        String[] manger_shid = key.trim().split(" ");
        for (int i = 0; i < 16; i++) {
            int num_info_1 = Integer.valueOf(manger_shid[i], 16);
            key_16[i] = (byte) num_info_1;
        }
        Log.e("eee_manger_info_len", manger_aes.length + "  == ");
        Log.e("eee_manger_key_len", manger_shid.length + "  == ");

        byte[] codE;
        codE = AESUtils.encrypt128_3(key_info, key_16);

        Log.e("eee_11", "原文：" + info);
        Log.e("eee_22", "密钥：" + key);
        Log.e("eee_33", "密文：" + codE.length);
        Log.e("eee_44", "解密：" + AESUtils.decrypt128_3(codE, key_16).length);
        //转换数据类型方便查看
        Log.i("eee_read_111", "read data: " + SocketClient2.bytesToHexString(codE));
        Log.i("eee_read_112", "read data: " + SocketClient2.bytesToHexString(AESUtils.decrypt128_3(codE, key_16)));
        return info + codE;
    }

    public static void md5_info(){
        //FA 62 00 02 02 80 00 28 7E 1E 85 34 69 31 A7 D7 68 81 46 64 87 40 FB FD 6D 49 66 76 E3 CE E4 9D C9 38 F3 C5 4B FD 3D F1 97 2F AD 28 45 F0 3F 14
        //9A A5 68 87 AA 01 BB 02 B1 A8 CE C4 D0 A3 D1 E9 C2 EB B4 ED CE F3 00 00 00 00 00 00 FC F5 98 11
        String info1 = "1E F1 53 C3 6A 91 2E A5 89 00 71 44 AA 01 BB 02 B1 A8 CE C4 D0 A3 D1 E9 C2 EB B4 ED CE F3 00 00 00 00 00 00 00 00 00 00";
        String[] manger_shid = info1.trim().split(" ");
        byte[] mad_info = new byte[manger_shid.length];
        for (int i = 0; i < manger_shid.length; i++) {
            int num_info_1 = Integer.valueOf(manger_shid[i], 16);
            mad_info[i] = (byte) num_info_1;
        }
        //1001d2f8ec478bf9abd23521dc1521b7
        Log.e("eee_md5_info1",MD5.MD5_sign(mad_info)  +"  ==  " +mad_info.length);
        Log.e("eee_md5_info2",SocketClient2.bytesToHexString(MD5.MD5_sign_byte(mad_info))  +"  ==  " +mad_info.length);
//        md5_new
        Log.e("eee_md5_info3",MD5.md5_new_byte(mad_info));
        Log.e("eee_md5_info4",SocketClient2.bytesToHexString(mad_info));
        String info2 = "1E F1 53 C3 6A 91 2E A5 89 00 71 44 AA 01 BB 02 B1 A8 CE C4 D0 A3 D1 E9 C2 EB B4 ED CE F3 00 00 00 00 00 00 00 00 00 00";
        Log.e("eee_md5_info5_32",MD5.md5_new(info2.replace(" ","")));
        Log.e("eee_md5_info5_16",MD5.md5_new(info2.replace(" ","")).substring(8,24));
//        md5
//        Log.e("eee_md5_info6",MD5.md5(info2));
    }

    public static void test_cz_jm(){
        //FA 61 00 02 02 80 00 28 25 4B CD 23 58 DD 98 29 00 00 00 01 89 00 71 44 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
        //d4cf9638
        String test_info = "00 00 00 01 89 00 71 44 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 D4 DF 96 38";
        //CZ_SK_16
        String key = "63 29 AB 4D 00 9C 8E 68 E8 D9 DD BF 6B 2D CE 88";
        String[] manger_aes = test_info.trim().split(" ");
        byte[] key_info = new byte[manger_aes.length];
        for (int i = 0; i < manger_aes.length; i++) {
            int num_info_1 = Integer.valueOf(manger_aes[i], 16);
            key_info[i] = (byte) num_info_1;
        }
        byte[] key_16 = new byte[16];
        String[] manger_shid = key.trim().split(" ");
        for (int i = 0; i < 16; i++) {
            int num_info_1 = Integer.valueOf(manger_shid[i], 16);
            key_16[i] = (byte) num_info_1;
        }
        try {
            //88 50 55 F5 89 DA EC 58 00 00 00 01 89 00 71 44 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
            Log.e("eee_111", "原文：" + test_info);
            Log.e("eee_222", "密钥：" + key);
            Log.e("eee_333", SocketClient2.bytesToHexString(encrypt128_3(key_info,key_16)));
            Log.e("eee_444", "解密：" + SocketClient2.bytesToHexString(AESUtils.decrypt128_3(key_info, key_16)));
        }catch (Exception e){}

//        String info = "B1 A8 CE C4 D0 A3 D1 E9 C2 EB B4 ED CE F3 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00";
//        Log.e("cnmd1",asciiToString(info));

    }

    public static void jiemiAES(){
        String test_info = "84 12 45 F9 1F 53 17 4E 17 87 10 EA E9 9A 4D 3D CC 45 FD 2F 16 29 34 D5 2B B5 16 BD 10 79 1D 9D C6 F6 54 4C 35 E3 3F 7E DE 81 15 6C 0F 64 22 DC C3 90 EB A4 29 FF 5E 14 A2 EB F7 F3 A8 87 6B 92 2C DA 4C 8E C1 81 9A 00 5A 01 3F AA 2F 2A C2 43 CA 60 AB 34 5C 49 D1 BC 79 28 82 D7 67 DB 9D 81 5D DA A2 AB 8D 94 71 A8 90 F1 3C 95 E5 2A 96 19 5D DA A2 AB 8D 94 71 A8 90 F1 3C 95 E5 2A 96 19 5D DA A2 AB 8D 94 71 A8 90 F1 3C 95 E5 2A 96 19 5D DA A2 AB 8D 94 71 A8 90 F1 3C 95 E5 2A 96 19 24 8D 93 2B 4C 89 90 B1 23 4A 60 1A E9 EE 0E BC";
        //CZ_SK_16
        String key = "C0 E7 31 19 7B 18 CA 3A 41 09 54 33 56 2D 8B 0C";
        String[] manger_aes = test_info.trim().split(" ");
        byte[] key_info = new byte[manger_aes.length];
        for (int i = 0; i < manger_aes.length; i++) {
            int num_info_1 = Integer.valueOf(manger_aes[i], 16);
            key_info[i] = (byte) num_info_1;
        }
        byte[] key_16 = new byte[16];
        String[] manger_shid = key.trim().split(" ");
        for (int i = 0; i < 16; i++) {
            int num_info_1 = Integer.valueOf(manger_shid[i], 16);
            key_16[i] = (byte) num_info_1;
        }
        try {
            Log.e("eee_81", SocketClient2.bytesToHexString(decrypt128_3(key_info,key_16)));
        }catch (Exception e){}

    }

    public static String asciiToString(String value) {
        StringBuffer sbu = new StringBuffer();
        String[] chars = value.split(" ");
        Log.e("cnmd2",chars.length+"");
        for (int i = 0; i < chars.length; i++) {
            sbu.append((char) Integer.parseInt(chars[i]));
        }
        Log.e("cnmd2",sbu+"");
        return sbu.toString();
    }

}
