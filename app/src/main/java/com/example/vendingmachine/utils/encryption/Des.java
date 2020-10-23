package com.example.vendingmachine.utils.encryption;

/**
 * Created by Tony on 2020-05-18.
 */

import android.util.Log;

import com.example.vendingmachine.serialport.SerialDataUtils;
import com.example.vendingmachine.utils.InterceptString;
import com.example.vendingmachine.utils.YctUtils;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.SecretKeySpec;

import retrofit2.Converter;

import static com.example.vendingmachine.platform.common.SocketClient2.bytesToHexString;
import static com.example.vendingmachine.platform.common.SocketClient2.listTobyte;
import static com.example.vendingmachine.serialport.CarSerialPortUtil.check_CARD;

/***
 * @Description: des加密/解密类
 * @author zhouya
 * @version V1.0
 *
 */
public class Des {
    /**
     * 加密（使用DES算法）
     *
     * @param txt 需要加密的文本
     * @return 成功加密的文本
     * @throws InvalidKeySpecException
     * @throws InvalidKeyException
     * @throws NoSuchPaddingException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     */
    public static String enCrypto(String txt, String key2, byte[] key1)
            throws InvalidKeySpecException, InvalidKeyException,
            NoSuchPaddingException, IllegalBlockSizeException,
            BadPaddingException {
        StringBuffer sb = new StringBuffer();
        DESKeySpec desKeySpec = new DESKeySpec(key1);
        SecretKeyFactory skeyFactory = null;
        Cipher cipher = null;
        try {
            skeyFactory = SecretKeyFactory.getInstance("DES");
            cipher = Cipher.getInstance("DES");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        SecretKey deskey = skeyFactory.generateSecret(desKeySpec);
        cipher.init(Cipher.ENCRYPT_MODE, deskey);
        byte[] cipherText = cipher.doFinal(txt.getBytes());
        for (int n = 0; n < cipherText.length; n++) {
            String stmp = (java.lang.Integer.toHexString(cipherText[n] & 0XFF));

            if (stmp.length() == 1) {
                sb.append("0" + stmp);
            } else {
                sb.append(stmp);
            }
        }
        return sb.toString().toUpperCase();
    }

    /**
     * 解密（使用DES算法）
     *
     * @param txt 需要解密的文本
     * @param
     * @return 成功解密的文本
     * @throws InvalidKeyException
     * @throws InvalidKeySpecException
     * @throws NoSuchPaddingException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     */
    public static String deCrypto(String txt, String key2, byte[] key1)
            throws InvalidKeyException, InvalidKeySpecException,
            NoSuchPaddingException, IllegalBlockSizeException,
            BadPaddingException {
        DESKeySpec desKeySpec = new DESKeySpec(key1);
        SecretKeyFactory skeyFactory = null;
        Cipher cipher = null;
        try {
            skeyFactory = SecretKeyFactory.getInstance("DES");
            cipher = Cipher.getInstance("DES");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        SecretKey deskey = skeyFactory.generateSecret(desKeySpec);
        cipher.init(Cipher.DECRYPT_MODE, deskey);
        byte[] btxts = new byte[txt.length() / 2];
        for (int i = 0, count = txt.length(); i < count; i += 2) {
            btxts[i / 2] = (byte) Integer.parseInt(txt.substring(i, i + 2), 16);
        }
        return (new String(cipher.doFinal(btxts)));
    }

    public static byte[] enCrypto_2(byte[] txt, byte[] key)
            throws InvalidKeySpecException, InvalidKeyException,
            NoSuchPaddingException, IllegalBlockSizeException,
            BadPaddingException {
        StringBuffer sb = new StringBuffer();
        DESKeySpec desKeySpec = new DESKeySpec(key);
        SecretKeyFactory skeyFactory = null;
        Cipher cipher = null;
        try {
            skeyFactory = SecretKeyFactory.getInstance("DES");
            cipher = Cipher.getInstance("DES/ECB/NoPadding");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        SecretKey deskey = skeyFactory.generateSecret(desKeySpec);
        cipher.init(Cipher.ENCRYPT_MODE, deskey);
        byte[] cipherText = cipher.doFinal(txt);
        for (int n = 0; n < cipherText.length; n++) {
            String stmp = (java.lang.Integer.toHexString(cipherText[n] & 0XFF));

            if (stmp.length() == 1) {
                sb.append("0" + stmp);
            } else {
                sb.append(stmp);
            }
        }
        return cipherText;
    }

    /**
     * 解密（使用DES算法）
     *
     * @param txt 需要解密的文本
     * @param key 密钥
     * @return 成功解密的文本
     * @throws InvalidKeyException
     * @throws InvalidKeySpecException
     * @throws NoSuchPaddingException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     */
    public static byte[] deCrypto_2(byte[] txt, byte[] key)
            throws InvalidKeyException, InvalidKeySpecException,
            NoSuchPaddingException, IllegalBlockSizeException,
            BadPaddingException {
        DESKeySpec desKeySpec = new DESKeySpec(key);
        SecretKeyFactory skeyFactory = null;
        Cipher cipher = null;
        try {
            skeyFactory = SecretKeyFactory.getInstance("DES");
            cipher = Cipher.getInstance("DES/ECB/NoPadding");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        SecretKey deskey = skeyFactory.generateSecret(desKeySpec);
        cipher.init(Cipher.DECRYPT_MODE, deskey);
//        byte[] btxts = new byte[txt.length() / 2];
//        for (int i = 0, count = txt.length(); i < count; i += 2) {
//            btxts[i / 2] = (byte) Integer.parseInt(txt.substring(i, i + 2), 16);
//        }
        return cipher.doFinal(txt);
    }

    public static void main() throws InvalidKeyException,
            NoSuchAlgorithmException, InvalidKeySpecException,
            NoSuchPaddingException, IllegalBlockSizeException,
            BadPaddingException {

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
        String test_info = "54 2B 92 BC 10 A0 DB 55 A5 61 25 DC 26 21 8D 6E 7A CC 02 E0 CD 91 78 3F FE 34 14 26 61 80 CD D5 9E AE FA 0B 9B 82 8B 3D BF C6 2C 09 60 95 42 D3 E8 A5 F1 96 E4 45 C4 B9 4A 9C 2B C1 99 A6 78 E9 4A 9C 2B C1 99 A6 78 E9 4A 9C 2B C1 99 A6 78 E9 4A 9C 2B C1 99 A6 78 E9 4A 9C 2B C1 99 A6 78 E9 4A 9C 2B C1 99 A6 78 E9 7F 14 92 B6 FE 3B C3 EC 2C D7 C8 EC 00 8D C2 27 A8 D3 3B B9 C2 ED FA BB 25 8B EC D9 A5 4F 6C BD A9 23 A3 CB A2 3E CB D2";
        List manger_num = new ArrayList<>();
        String[] manger = test_info.split(" ");
        Log.e("manger_123", manger.length + "");
        for (int i = 0; i < test_info.split(" ").length; i++) {
            manger_num.add(manger[i]);
        }

        Log.e("aaa1_明文", bytesToHexString(check_CARD(manger_num, manger_num.size())));
        Log.e("aaa1_key", bytesToHexString(key_1));
        Log.e("aaa1_加密2", bytesToHexString(enCrypto_2(key_des, key_1)));
        Log.e("aaa1_解密2", bytesToHexString(deCrypto_2(check_CARD(manger_num, manger_num.size()), key_1)));
    }

    public static void main_19_65() throws InvalidKeyException,
            NoSuchAlgorithmException, InvalidKeySpecException,
            NoSuchPaddingException, IllegalBlockSizeException,
            BadPaddingException {

        /**
         * D0 A5 10 05 04 80 00 48 FF FF 01 00 99 99 32 40 17 62 1B BA 51 C0 08 AD 72 4B BE AB BA 03 AA B1 AB A3 A4 CD FB 5D 02 09 D3 B1 77 44 AA C6 23 23 B1 1A C1 18 A9 AC F9 5C E3 E4 7A B4 E7 81 81 95 D9 2F 99 8A 71 73 C9 1A 29 33 AD 51 85 C2 AB 78
         */
        byte[] CAR_SIGN_19 = new byte[8];
        //C1 A1 AA 9F 9C 4E 5D 3C
//        CAR_SIGN_19[0] = (byte) 0x11;
//        CAR_SIGN_19[1] = (byte) 0x11;
//        CAR_SIGN_19[2] = (byte) 0x1A;
//        CAR_SIGN_19[3] = (byte) 0x1F;
//        CAR_SIGN_19[4] = (byte) 0x1C;
//        CAR_SIGN_19[5] = (byte) 0x1E;
//        CAR_SIGN_19[6] = (byte) 0x1D;
//        CAR_SIGN_19[7] = (byte) 0x1C;
        byte[] CAR_SIGN_65 = new byte[8];
//        // 8F 94 75 48 36 21 77 5E          PayStringTobyte
//        //F5 CA 8C F2 FA B7 14 1F
//        CAR_SIGN_65[0] = (byte) 0xF5;
//        CAR_SIGN_65[1] = (byte) 0xCA;
//        CAR_SIGN_65[2] = (byte) 0x8C;
//        CAR_SIGN_65[3] = (byte) 0xF2;
//        CAR_SIGN_65[4] = (byte) 0xFA;
//        CAR_SIGN_65[5] = (byte) 0xB7;
//        CAR_SIGN_65[6] = (byte) 0x14;
//        CAR_SIGN_65[7] = (byte) 0x1F;
//        D1 3D 13 F5 B0 F0 9D 42
        String tes_key = "8380DC763E7F51A1".replace(" ","");
        CAR_SIGN_65 = SerialDataUtils.hexString2Bytes(tes_key);
//        String test_info= "36 14 CC 86 EC 6E A4 A8 07 7D 45 4C 6F 41 4E 43 05 BA E6 86 05 B6 9C 51 5A C1 E5 FC 19 1F 88 DC 3C 6E EB 7A 63 84 D5 59 C0 1B 2A 8D 83 98 31 60 F5 45 D2 F5 45 5A F7 40 06 53 3B BC 21 AD 67 27 18 2E 58 FF AE 2E AA FE 18 2E 58 FF AE 2E AA FE 18 2E 58 FF AE 2E AA FE 18 2E 58 FF AE 2E AA FE 18 2E 58 FF AE 2E AA FE 18 2E 58 FF AE 2E AA FE 18 2E 58 FF AE 2E AA FE 18 2E 58 FF AE 2E AA FE 18 2E 58 FF AE 2E AA FE 2C B7 CA 09 3E 68 33 72";
        /**
         D0 A5 10 05 04 80 00 48 FF FF 01 00 99 99 32 40 17 62 1B BA 51 C0 08 AD 72 4B BE AB BA 03 AA B1 AB A3 A4 CD FB 5D 02 09 D3 B1 77 44 AA C6 23 23 B1 1A C1 18 A9 AC F9 5C E3 E4 7A B4 E7 81 81 95 D9 2F 99 8A 71 73 C9 1A 29 33 AD 51 85 C2 AB 78
         */
//        String test_info = "E0 5F 34 07 17 85 57 67 9E DC CD 14 EB 6B 37 6C 3E BB BB 57 38 F7 1B E9 86 E4 2A 3A 5F 3E B3 44 B1 1A C1 18 A9 AC F9 5C E3 E4 7A B4 E7 81 81 95 06 7B B3 71 ED E9 2B 01 5B 38 17 E4 8B BE B5 28";
//        String jm_info = "D0 B1 10 05 04 80 00 A8 FF FF 01 00 99 99 32 40 00 00 00 00 00 00 00 00 00 00 00 00 00 02 88 01 20 00 11 11 00 00 00 00 00 00 13 37 42 74 86 58 07 10 94 74 05 00 00 00 00 01 09 13 06 01 00 00 00 02 51 00 00 02 99 88 87 80 2C 67 AC 93 99 99 32 40 20 20 06 11 09 34 01 99 99 32 40 20 20 06 11 09 34 01 00 00 00 02 04 36 28 17 00 16 9D 99 99 32 40 20 20 06 11 09 34 01 30 30 30 35 66 60 31 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 84 14 5E A7 63 54 34 BF";
//        String test_info = "58 7A 2E D3 5B 1A 65 82 A7 42 47 D3 29 F2 F6 BB DB 77 C7 43 0C E8 67 FF 7F 65 47 0C CD 81 DC D5 41 38 C9 C3 05 38 82 96 AF 14 B7 27 3A 64 8A E0 5B B6 58 3D A7 FA 95 2D 17 7C 67 91 FA E3 57 35 33 7B 06 35 0F 03 63 3C 7E BA F2 14 7E 5A 0A CB";
        String test_info="0A 6B EE 65 08 67 89 B4 93 07 DB EC 35 8F 21 73 B8 E1 E9 43 55 52 7E DE 52 3B CB 96 20 A2 97 2D";
        List manger_num = new ArrayList<>();
        String[] manger = test_info.split(" ");
        Log.e("aaa65_manger_123", manger.length + "");
        for (int i = 0; i < test_info.split(" ").length; i++) {
            manger_num.add(manger[i]);
        }

        Log.e("aaa65_明文", bytesToHexString(check_CARD(manger_num, manger_num.size())));
        Log.e("aaa65_key", bytesToHexString(CAR_SIGN_65));
//        Log.e("aaa65_加密2", bytesToHexString(enCrypto_2(check_CARD(manger_num,manger_num.size()), CAR_SIGN_65)));
        Log.e("aaa65_解密2", bytesToHexString(deCrypto_2(check_CARD(manger_num, manger_num.size()), CAR_SIGN_65)));
        byte[] test = new byte[4];
        //E2 37 BB D3 BF 70 C2 CC       PayStringTobyte
        test[0] = (byte) 0x00;
        test[1] = (byte) 0x00;
        test[2] = (byte) 0xAA;
        test[3] = (byte) 0x66;
        Log.e("btoi",btoi(test)[0]+"");
        String info_1  = btoi(test)[0]+"";
        String info_2 = "00000000"+info_1;
//        info_2.substring(info_2.length()-8,info_2.length());
        Log.e("btoi2",info_2.substring(info_2.length()-8,info_2.length()));
        String info_3 = info_2.substring(info_2.length()-8,info_2.length());
        String[] str_test = new String[4];
        for (int i = 0;i<4;i++){
            str_test[i] = info_3.substring(i*2,(i+1)*2);
        }
        byte[] test2 = new byte[4];
        //E2 37 BB D3 BF 70 C2 CC       PayStringTobyte
//        test2[0] = (byte) Integer.parseInt(str_test[0]);
//        test2[1] = (byte) Integer.parseInt(str_test[1]);
//        test2[2] = (byte) Integer.parseInt(str_test[2]);
//        test2[3] = (byte) Integer.parseInt(str_test[3]);

        test2[0] = InterceptString.Companion.moneyByteToByte(Integer.parseInt(str_test[0]));
        test2[1] = InterceptString.Companion.moneyByteToByte(Integer.parseInt(str_test[1]));
        test2[2] = InterceptString.Companion.moneyByteToByte(Integer.parseInt(str_test[2]));
        test2[3] = InterceptString.Companion.moneyByteToByte(Integer.parseInt(str_test[3]));

        Log.e("btoi3",str_test.length+"  "+str_test[0]+"  "+str_test[1]+"  "+str_test[2]+"  "+str_test[3]);
        Log.e("btoi4",InterceptString.Companion.moneyByteToByte(Integer.parseInt(str_test[2]))+"");
//        moneyByteToByte

    }

    public static void A8_des(){
        String info = "D0A81005048000e8FFFF010099993240000000000000000000000000000186641917000147640000000000000000000000000200000006020100999932400000016E202008101339015100000401136376C1B207133E4DF82100000002000000020000EBBF0617000F00E000000000000100999932402020081013390101009999324008100847010300FF00000000020100999932400000016F202008101340015100000401136376C1B207133E4DF82100000002000000020000EBBD0617000F00E100000000000100999932402020081013400101009999324008101339010300FF00000000000000000000000000";
        byte[] info_byte = SerialDataUtils.hexString2Bytes(info);
        byte[] info_byte_mac = new byte[info_byte.length-8];
        for (int i=0;i<info_byte.length-8;i++){
            info_byte_mac[i]=info_byte[i+8];
        }
        byte[] info_byte_mac_des = null;
        //开始做mac运算，不够8位补充8位
        if((info_byte_mac.length%8) ==0){
            //刚好够8位不用填充，当前数据直接做mac运算
            info_byte_mac_des = new byte[info_byte_mac.length];
            info_byte_mac_des = info_byte_mac;
        }else {
//            91 09 81 75 00 01 47 68   结算流水号
            //F4 C2 39 EC E3 BA 62 79    k19
            //4B 53 08 46 0A 4F 97 6D    k65
            info_byte_mac_des = new byte[((info_byte_mac.length/8)+1)*8];
            for (int i=0;i<info_byte_mac.length;i++){
                info_byte_mac_des[i] = info_byte_mac[i];
            }
        }
        Log.e("A8__byte_mac_des1",bytesToHexString(info_byte_mac));
        Log.e("A8__byte_mac_des2",bytesToHexString(info_byte_mac_des));

        Log.e("A8__byte_mac_des11",info_byte_mac.length+"  ==  "  + info_byte_mac.length/8);
        Log.e("A8__byte_mac_des22",info_byte_mac_des.length+"");

        //商户上传密码  k19加密
        //商户上传密码  8位
        //需要先进行 Des加密
        byte[] key_des_mm = new byte[8];
        key_des_mm[0] = (byte) 0x00;
        key_des_mm[1] = (byte) 0x00;
        key_des_mm[2] = (byte) 0x00;
        key_des_mm[3] = (byte) 0x00;
        key_des_mm[4] = (byte) 0x00;
        key_des_mm[5] = (byte) 0x00;
        key_des_mm[6] = (byte) 0x00;
        key_des_mm[7] = (byte) 0x00;
        //K19 密钥
        //00 00 00 00 00 00
        // 00 00 09 00
        // 00 00 00 01
        // 88 01 20 00 11 11
        // 86 64 19 17 00 01 47 64
        // 01 02
        // 03 03 01 05 01 04 00 03 00 02 06 02 06 05 03 03
        // 20 20 08 06 17 48 54 00 00 00 7F 65 42 B6 47 41 F8 42
        byte[] CAR_SIGN_19 = SerialDataUtils.hexString2Bytes("AC174FCBA105594E");
        byte[] CAR_SIGN_65 = SerialDataUtils.hexString2Bytes("8380DC763E7F51A1");
        try {
            key_des_mm = Des.enCrypto_2(key_des_mm, CAR_SIGN_19);
            for (int i=0;i<8;i++){
                info_byte_mac_des[31 + i] = (byte) key_des_mm[i];
            }
        }catch (Exception e){}

        //MAC  6-10 km DES运算
        //8个一组进行加密 ，上次加密的结果为下个加密的密钥
        //下一次密钥存储
        byte[] key_des = new byte[8];
        List mList = new ArrayList();
        for (int i = 0; i < info_byte_mac_des.length; i++) {
            mList.add(info_byte_mac_des[i]);
        }
//        00 00 00 00 00 00 6E  7D56EC6BF8E1EB
        List<List<Byte>> mEndList = new ArrayList<>();
        //把数据切割成 8个字节数组 用于加密
        for (int i = 0; i < (info_byte_mac.length/8)-1; i++) {
            mEndList.add(mList.subList(8 * (i + 1), (8 * (i + 2))));
        }
        //第一次加密数据
        byte[] key_des_1 = new byte[8];
        key_des[0] = (byte) 0x00;
        key_des[1] = (byte) 0x00;
        key_des[2] = (byte) 0x00;
        key_des[3] = (byte) 0x00;
        key_des[4] = (byte) 0x00;
        key_des[5] = (byte) 0x00;
        key_des[6] = (byte) 0x00;
        key_des[7] = (byte) 0x00;

        try {
            key_des = Des.enCrypto_2(key_des_1, CAR_SIGN_19);
        } catch (Exception e) {
        }

        for (int i = 0; i < info_byte_mac.length/8-1; i++) {
            try {
//                Log.e("agree_key_des_info+112 +26  "+i ,bytesToHexString(key_des)+"  上一个加密密钥====>>  "+bytesToHexString(Des.enCrypto_2(listTobyte(mEndList.get(i)),key_des))+"  新密钥====>>  "+bytesToHexString(listTobyte(mEndList.get(i)))+"  == 加密数据");
                key_des = Des.enCrypto_2(listTobyte(mEndList.get(i)), key_des);
            } catch (Exception e) {
            }
        }

        //MAC 加密数据结果key_des
//        pack[151] = (byte) 0x00;
        for (int i = 0; i < 8; i++) {
            info_byte[info_byte.length-8 + i] = key_des[i];
        }
        Log.e("A8_body_des0", bytesToHexString(info_byte));
        //数据体 7-11进行DES加密
        byte[] body = new byte[info_byte.length-16];
        for (int i = 0; i < info_byte.length-16; i++) {
            body[i] = info_byte[16 + i];
        }
        Log.e("A8_body_des1", bytesToHexString(body));
        try { 
            //7-11 des加密结果  k65
            byte[] body_des = Des.enCrypto_2(body, CAR_SIGN_65);
            int index = 8;
            for (int i = 0; i < body_des.length; i++) {
                info_byte[16 + i] = body_des[i];
            }
            Log.e("A8_body_des2", bytesToHexString(deCrypto_2(body_des, CAR_SIGN_65)));
            Log.e("A8_body_pack", bytesToHexString(info_byte));
            Log.e("A8_body_length", info_byte.length+"");
        } catch (Exception e) {
            Log.e("A8_eeee_body", e.getMessage().toString());
        }
    }

    /**
     * 使用DES对数据解密
     *
     * @param bytes utf8编码的二进制数据
     * @param key   密钥（16字节）
     * @return 解密结果
     * @throws Exception
     */
    public static byte[] desDecrypt(byte[] bytes, byte[] key) throws Exception {
        if (bytes == null || key == null)
            return null;
        Cipher cipher = Cipher.getInstance("DES/ECB/NoPadding");
//        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key.getBytes("utf-8"), "DES"));
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "DES"));
        bytes = cipher.doFinal(bytes);
        return bytes;
    }

    /**
     * 使用base64解决乱码
     *
     * @param secretKey 加密后的字节码
     */
    public static String jdkBase64String(byte[] secretKey) {
        BASE64Encoder encoder = new BASE64Encoder();
        return encoder.encode(secretKey);
    }

    /**
     * 使用jdk的base64 解密字符串 返回为null表示解密失败
     *
     * @throws IOException
     */
    public static byte[] jdkBase64Decoder(String str) throws IOException {
        BASE64Decoder decoder = new BASE64Decoder();
        return decoder.decodeBuffer(str);
    }

    public static int unsignedByteToInt(byte b) {
        return (int) b & 0xFF;
    }

    public  static int []  btoi(byte  [] btarr)
    {
        if(btarr.length%4!=0)
        {
            return null;
        }
        int [] intarr  =new int [btarr.length/4];

        int i1,i2,i3,i4;
        for(int j=0,k=0;j<intarr.length;j++,k+=4)//j循环int		k循环byte数组
        {
            i1=btarr[k];
            i2=btarr[k+1];
            i3=btarr[k+2];
            i4=btarr[k+3];

            if(i1<0)
            {
                i1+=256;
            }
            if(i2<0)
            {
                i2+=256;
            }
            if(i3<0)
            {
                i3+=256;
            }
            if(i4<0)
            {
                i4+=256;
            }
            intarr[j]=(i1<<24)+(i2<<16)+(i3<<8)+(i4<<0);//保存Int数据类型转换

        }
        return intarr;
    }

    public static void main1(String args)
    {
        int ten_num = Integer.parseInt(args.substring(0,1));
        int hex_1 = ten_num*36;
        int hex_2 = Integer.parseInt(args.substring(1,2));
        int hex_all = hex_1+hex_2;
        Log.e("hex_all",hex_all+"");
    }

}
