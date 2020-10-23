package com.example.vendingmachine.utils;

/**
 */

/**
 * author：wiki on 2018/12/12
 * email：zhengweiqunemail@qq.com
 */
public class ConverUtil {

    /**
     * 16进制字符串转化为字母ASCALL码
     *
     * @param hex 要转化的16进制数，用空格隔开
     *            如：53 68 61 64 6f 77
     * @return ASCALL码
     */
    public static String convertHexToAsCall(String hex) {
        StringBuilder sb = new StringBuilder();
        String[] split = hex.split(" ");
        for (String str : split) {
            int i = Integer.parseInt(str, 16);
            if (i < 0x20 || i == 0x7F) {//过滤特殊字符
                continue;
            }
            sb.append((char) i);
        }
        return sb.toString();
    }

    /**
     * 16进制数组转String
     *
     * @param data byte数组
     * @return string
     */
    public static String formatHex2String(byte[] data) {
        final StringBuilder stringBuilder = new StringBuilder(data.length);
        for (byte byteChar : data)
            stringBuilder.append(String.format("%02X ", byteChar));
        return stringBuilder.toString();
    }

    /**
     * 16进制字符串转换为Byte值
     *
     * @param src Byte字符串，每个Byte之间没有分隔符，eg:616C6B
     * @return byte[]
     */
    public static byte[] hexStr2Bytes(String src) {
        int m = 0, n = 0;
        int l = src.length() / 2;
        System.out.println(l);
        byte[] ret = new byte[l];
        for (int i = 0; i < l; i++) {
            m = i * 2 + 1;
            n = m + 1;
            ret[i] = Byte.decode("0x" + src.substring(i * 2, m) + src.substring(m, n));
        }
        return ret;
    }

    /**
     * 字符串转换成十六进制字符串
     *
     * @param str 待转换的ASCII字符串
     * @return String 每个Byte之间没有分隔，如: [616C6B]
     */
    public static String str2HexStr(String str) {

        char[] chars = "0123456789ABCDEF".toCharArray();
        StringBuilder sb = new StringBuilder("");
        byte[] bs = str.getBytes();
        int bit;

        for (int i = 0; i < bs.length; i++) {
            bit = (bs[i] & 0x0f0) >> 4;
            sb.append(chars[bit]);
            bit = bs[i] & 0x0f;
            sb.append(chars[bit]);
//            sb.append(' ');
        }
        return sb.toString().trim();
    }

    //byte 数组与 int 的相互转换
    public static int byteArrayToInt(byte[] b) {
        return   b[3] & 0xFF |
                (b[2] & 0xFF) << 8 |
                (b[1] & 0xFF) << 16 |
                (b[0] & 0xFF) << 24;
    }

    public static byte[] intToByteArray(int a) {
        return new byte[] {
                (byte) ((a >> 24) & 0xFF),
                (byte) ((a >> 16) & 0xFF),
                (byte) ((a >> 8) & 0xFF),
                (byte) (a & 0xFF)
        };
    }

}
