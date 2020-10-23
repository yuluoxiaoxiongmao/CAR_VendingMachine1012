package com.example.vendingmachine.serialport;

/**
 * Created by mayn on 2018/2/7.
 */


import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

/**
 * 串口数据转换工具类
 * Created by Administrator on 2016/6/2.
 */
public class SerialDataUtils {
    private final static byte[] hex = "0123456789ABCDEF".getBytes();

    private final static String hexStr = "0123456789ABCDEF";

    private static String[] binaryArray = {"0000", "0001", "0010", "0011",
            "0100", "0101", "0110", "0111",
            "1000", "1001", "1010", "1011",
            "1100", "1101", "1110", "1111"};

    //-------------------------------------------------------
    // 判断奇数或偶数，位运算，最后一位是1则为奇数，为0是偶数
    public static int isOdd(int num) {
        return num & 1;
    }

    //-------------------------------------------------------
    //Hex字符串转int
    public static int HexToInt(String inHex) {
        return Integer.parseInt(inHex, 16);
    }

    //-------------------------------------------------------
    //Hex字符串转byte
    public static byte HexToByte(String inHex) {
        return (byte) Integer.parseInt(inHex, 16);
    }

    //-------------------------------------------------------
    //1字节转2个Hex字符
    public static String Byte2Hex(Byte inByte) {
        return String.format("%02x", new Object[]{inByte}).toUpperCase();
    }

    //-------------------------------------------------------
    //字节数组转转hex字符串
    public static String ByteArrToHex(byte[] inBytArr) {
        StringBuilder strBuilder = new StringBuilder();
        for (byte valueOf : inBytArr) {
            strBuilder.append(Byte2Hex(Byte.valueOf(valueOf)));
            strBuilder.append(" ");
        }
        return strBuilder.toString();
    }

    //-------------------------------------------------------
    //字节数组转转hex字符串，可选长度
    public static String ByteArrToHex(byte[] inBytArr, int offset, int byteCount) {
        StringBuilder strBuilder = new StringBuilder();
        int j = byteCount;
        for (int i = offset; i < j; i++) {
            strBuilder.append(Byte2Hex(Byte.valueOf(inBytArr[i])));
        }
        return strBuilder.toString();
    }

    //-------------------------------------------------------
    //转hex字符串转字节数组
    public static byte[] HexToByteArr(String inHex) {
        byte[] result;
        int hexlen = inHex.length();
        if (isOdd(hexlen) == 1) {
            hexlen++;
            result = new byte[(hexlen / 2)];
            inHex = "0" + inHex;
        } else {
            result = new byte[(hexlen / 2)];
        }
        int j = 0;
        for (int i = 0; i < hexlen; i += 2) {
            result[j] = HexToByte(inHex.substring(i, i + 2));
            j++;
        }
        return result;
    }


// char转byte

    public static byte[] toBytes(char[] chars) {
        Charset cs = Charset.forName("UTF-8");
        CharBuffer cb = CharBuffer.allocate(chars.length);
        cb.put(chars);
        cb.flip();
        ByteBuffer bb = cs.encode(cb);

        return bb.array();

    }

// byte转char

    public static char[] toChars(byte[] bytes) {
        Charset cs = Charset.forName("UTF-8");
        ByteBuffer bb = ByteBuffer.allocate(bytes.length);
        bb.put(bytes);
        bb.flip();
        CharBuffer cb = cs.decode(bb);

        return cb.array();
    }

    /**
     * int 转 byte 数组
     *
     * @param a
     * @return
     */
    public static byte[] intToByteArray(int a) {
        return new byte[]{
                (byte) ((a >> 24) & 0xFF),
                (byte) ((a >> 16) & 0xFF),
                (byte) ((a >> 8) & 0xFF),
                (byte) (a & 0xFF)
        };
    }


    //byte 与 int 的相互转换
    public static byte intToByte(int x) {
        return (byte) x;
    }


    /**
     * 16进制转换成为string类型字符串
     *
     * @param s
     * @return
     */
    public static String hexStringToString(String s) {
        if (s == null || s.equals("")) {
            return null;
        }
        s = s.replace(" ", "");
        byte[] baKeyword = new byte[s.length() / 2];
        for (int i = 0; i < baKeyword.length; i++) {
            try {
                baKeyword[i] = (byte) (0xff & Integer.parseInt(s.substring(i * 2, i * 2 + 2), 16));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try {
            s = new String(baKeyword, "UTF-8");
            new String();
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        return s;
    }

    /**
     *     * 将指定字符串src，以每两个字符分割转换为16进制形式
     *     * 如："2B44EFD9" --> byte[]{0x2B, 0x44, 0xEF, 0xD9}
     *     * @param src String
     *     * @return byte[]
     *    
     **/

//    public static byte[] HexString2Bytes(String src)
//
//    {
//        byte[] ret = new byte[272];
//        byte[] tmp = src.getBytes();
//        for (int i = 0; i < 272; i++) {
//            ret[i] = uniteBytes(tmp[i * 2], tmp[i * 2 + 1]);
//        }
//        return ret;
//    }
//
//
//    /**
//     *     * 将两个ASCII字符合成一个字节；
//     *     * 如："EF"--> 0xEF
//     *     * @param src0 byte
//     *     * @param src1 byte
//     *     * @return byte
//     *    
//     **/
//    public static byte uniteBytes(byte src0, byte src1) {
//        byte _b0 = Byte.decode("0x" + new String(new byte[]{src0})).byteValue();
//        _b0 = (byte) (_b0 << 4);
//        byte _b1 = Byte.decode("0x" + new String(new byte[]{src1})).byteValue();
//        byte ret = (byte) (_b0 ^ _b1);
//        return ret;
//    }

    /*16进制byte数组转String*/
    public static String bytes2HexString(byte[] b) {
        String r = "";

        for (int i = 0; i < b.length; i++) {
            String hex = Integer.toHexString(b[i] & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            r += hex.toUpperCase();
        }

        return r;
    }

    /*
     * 16进制字符串转字节数组
     */
    public static byte[] hexString2Bytes(String hex) {

        if ((hex == null) || (hex.equals(""))) {
            return null;
        } else if (hex.length() % 2 != 0) {
            return null;
        } else {
            hex = hex.toUpperCase();
            int len = hex.length() / 2;
            byte[] b = new byte[len];
            char[] hc = hex.toCharArray();
            for (int i = 0; i < len; i++) {
                int p = 2 * i;
                b[i] = (byte) (charToByte(hc[p]) << 4 | charToByte(hc[p + 1]));
            }
            return b;
        }

    }

    /*
     * 字符转换为字节
     */
    private static byte charToByte(char c) {
        return (byte) "0123456789ABCDEF".indexOf(c);
    }

    /**
     * 普通字符转换成16进制字符串
     *
     * @param str
     * @return
     */
    public static String str2HexStr(String str) {
        byte[] bytes = str.getBytes();
        // 如果不是宽类型的可以用Integer
        BigInteger bigInteger = new BigInteger(1, bytes);
        return bigInteger.toString(16);
    }

    /**
     * 16进制的字符串转换成16进制字符串数组
     *
     * @param src
     * @return
     */
    public static byte[] HexString2Bytes(String src) {
        int len = src.length() / 2;
        byte[] ret = new byte[len];
        byte[] tmp = src.getBytes();
        for (int i = 0; i < len; i++) {
            ret[i] = uniteBytes(tmp[i * 2], tmp[i * 2 + 1]);
        }
        return ret;
    }

    public static byte uniteBytes(byte src0, byte src1) {
        byte _b0 = Byte.decode("0x" + new String(new byte[]{src0})).byteValue();
        _b0 = (byte) (_b0 << 4);
        byte _b1 = Byte.decode("0x" + new String(new byte[]{src1})).byteValue();
        byte ret = (byte) (_b0 ^ _b1);
        return ret;
    }

    /*
     * 字节数组转16进制字符串显示
     */
    public String bytes2HexString(byte[] b, int length) {
        String r = "";

        for (int i = 0; i < length; i++) {
            String hex = Integer.toHexString(b[i] & 0xFF);
            if (hex.length() == 1) {
                hex = "0" + hex;
            }
            r += hex.toUpperCase();
        }

        return r;
    }

    //新增2020-8-25
    public static void SetByteArray(byte[] src, byte value, int len) {
        int i;
        for (i = 0; i < len; i++) src[i] = (byte) value;
    }

    //新增2020-8-25
    public static String Bytes2HexString(byte[] b, int start, int len) {
        byte[] buff = new byte[2 * len];
        for (int i = start; i < start + len; i++) {
            buff[2 * (i - start)] = hex[(b[i] >> 4) & 0x000f];
            buff[2 * (i - start) + 1] = hex[b[i] & 0x000f];
        }
        return new String(buff);
    }

    //新增2020-8-26
    public static int ByteMemCmp(byte[] src1, int srcPos, byte[] src2, int destPos, int len) {
        int i = 0;
        for (i = 0; i < len; i++) {
            if ((byte) src1[i + srcPos] != (byte) src2[i + destPos]) return 1;
        }
        return 0;
    }

    //新增2020-8-27
    public static int toBdInt(byte[] bRefArr) {
        return toBdInt(bRefArr, 0, bRefArr.length);
    }

    public static int toBdInt(byte[] bRefArr, int start, int len) {
        int result = 0;
        if (start + len > bRefArr.length) {
            return -1;
        }
        int index = 1;
        for (int i = start + len - 1; i >= start; i--) {
            result += byte2Uint(bRefArr[i]) * index;
            index = index * 256;
        }
        return result;
    }

    public static int byte2Uint(byte b) {
        return b & 0xFF;
    }


}
