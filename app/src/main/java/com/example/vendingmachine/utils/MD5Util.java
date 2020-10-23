package com.example.vendingmachine.utils;

/**
 * Created by 10751 on 2020-06-30.
 */
        import android.util.Log;

        import com.example.vendingmachine.platform.common.SocketClient2;

        import java.io.UnsupportedEncodingException;
        import java.security.MessageDigest;
        import java.security.NoSuchAlgorithmException;

/**
 * 采用MD5加密解密

 */
public class MD5Util {
    private static final String TAG = "YCTMD5Util";

    /***
     * MD5加码 生成32位md5码
     */
    public static String string2MD5(String inStr) {
        Log.e(TAG, "string2MD5: -------------------------");
//        inStr = "1E F1 53 C3 6A 91 2E A5 89 00 71 44 AA 01 BB 02 B1 A8 CE C4 D0 A3 D1 E9 C2 EB B4 ED CE F3 00 00 00 00 00 00 00 00 00 00".replace(" ","");
        MessageDigest md5 = null;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (Exception e) {
            System.out.println(e.toString());
            e.printStackTrace();
            return "";
        }
        char[] charArray = inStr.toCharArray();
        byte[] byteArray = new byte[charArray.length];

        for (int i = 0; i < charArray.length; i++)
            byteArray[i] = (byte) charArray[i];
        byte[] md5Bytes = md5.digest(byteArray);
        StringBuffer hexValue = new StringBuffer();
        for (int i = 0; i < md5Bytes.length; i++) {
            int val = ((int) md5Bytes[i]) & 0xff;
            if (val < 16)
                hexValue.append("0");
            hexValue.append(Integer.toHexString(val));
        }
        return hexValue.toString();

    }

    /**
     * 加密解密算法 执行一次加密，两次解密
     */
    public static String convertMD5(String inStr) {
        Log.e(TAG, "convertMD5: -------------------------");
        char[] a = inStr.toCharArray();
        for (int i = 0; i < a.length; i++) {
            a[i] = (char) (a[i] ^ 't');
        }
        String s = new String(a);
        return s;

    }

    /*// 测试主函数
    public static void main(String args[]) {
        String s = new String("tangfuqiang");
        System.out.println("原始：" + s);
        System.out.println("MD5后：" + string2MD5(s));
        System.out.println("加密的：" + convertMD5(s));
        System.out.println("解密的：" + convertMD5(convertMD5(s)));

    }*/

    public static String encrypt(String str) {
        // String s = new String(str);.

        str = "1E F1 53 C3 6A 91 2E A5 89 00 71 44 AA 01 BB 02 B1 A8 CE C4 D0 A3 D1 E9 C2 EB B4 ED CE F3 00 00 00 00 00 00 00 00 00 00".replace(" ","");
        // MD5
        String s1 = string2MD5(str);
        //加密
        String s2 = new String(s1);
        String s = new String(str);

        Log.e(TAG, "show: ------------原始：" + s);
        Log.e(TAG, "show: ------------MD5后：" + string2MD5(s));
        Log.e(TAG, "show: ------------加密的：" + convertMD5(s));
         Log.e(TAG, "show: ------------解密的：" + convertMD5(convertMD5(s)));
        Log.e(TAG, "show: ------------16：" + Md5_16(str));
        // return convertMD5(convertMD5(s));

        return convertMD5(s2);

    }

    private static String Md5_16(String sourceStr) {
        String result = "";
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(sourceStr.getBytes());
            byte b[] = md.digest();
            int i;
            StringBuffer buf = new StringBuffer("");
            for (int offset = 0; offset < b.length; offset++) {
                i = b[offset];
                if (i < 0)
                    i += 256;
                if (i < 16)
                    buf.append("0");
                buf.append(Integer.toHexString(i));
            }
            result = buf.toString();
            Log.e(TAG, "show: ------------32：" + SocketClient2.bytesToHexString(md.digest()));
            Log.e(TAG, "show: ------------16：" + buf.toString().substring(8,24));
        } catch (NoSuchAlgorithmException e) {
//TODO Auto-generated catch block e.printStackTrace();
        }
        return result;
    }


}

