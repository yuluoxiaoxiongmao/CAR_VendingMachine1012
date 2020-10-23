package com.example.vendingmachine.utils;

/**
 * Created by Tony on 2020-05-09.
 */

public class StringUtil {
    //  十六进制的字符串转换成byte数组
    public static byte[] HexCommandtoByte(byte[] data) {
        if (data == null) {
            return null;
        }
        int nLength = data.length;

        String strTemString = new String(data, 0, nLength);
        String[] strings = strTemString.split(" ");
        nLength = strings.length;
        data = new byte[nLength];
        for (int i = 0; i < nLength; i++) {
            if (strings[i].length() != 2) {
                data[i] = 00;
                continue;
            }
            try {
                data[i] = (byte)Integer.parseInt(strings[i], 16);
            } catch (Exception e) {
                data[i] = 00;
                continue;
            }
        }

        return data;
    }
    //4位 转 int
    public static int money(){
        byte[] b=new byte[4];
        b[0] = (byte) 0x00;
        b[1] = (byte) 0x00;
        b[2] = (byte) 0xBA;
        b[3] = (byte) 0x14;
        int [] b1 =new int [4];
        for(int i=0;i<4;i++){
            if(b[i]<0){
                b1[i] = (int)(b[i] &0x7f)  + 128;
            }else {
                b1[i]= (int) b[i];
            }
        }
        return ((b1[0]) << 24) + ((b1[1])<< 16) + ((b1[2]) << 8) + b1[3];
    }
    public static String stringToAscii(String value)
    {
        StringBuffer sbu = new StringBuffer();
        char[] chars = value.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if(i != chars.length - 1)
            {
                sbu.append((int)chars[i]).append(" ");
            }
            else {
                sbu.append((int)chars[i]);
            }
        }
        return sbu.toString();
    }

    public static String asciiToString(String value)
    {
        StringBuffer sbu = new StringBuffer();
        String[] chars = value.split(" ");
        for (int i = 0; i < chars.length; i++) {
            sbu.append((char) Integer.parseInt(chars[i]));
        }
        return sbu.toString();
    }

    //1字节转2个Hex字符
    public static String Byte2Hex(Byte inByte) {
        return String.format("%02x", new Object[]{inByte}).toUpperCase();
    }
}
