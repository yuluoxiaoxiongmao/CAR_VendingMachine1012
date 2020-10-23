package com.example.vendingmachine.utils.encryption;

import android.util.Log;

import com.example.vendingmachine.utils.MD5;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class YCTMD5Util {

	public static String byteArrayToHexString(byte b[]) {
		StringBuffer resultSb = new StringBuffer();
		for (int i = 0; i < b.length; i++)
			resultSb.append(byteToHexString(b[i]));

		return resultSb.toString();
	}

	private static String byteToHexString(byte b) {
		int n = b;
		if (n < 0)
			n += 256;
		int d1 = n / 16;
		int d2 = n % 16;
		return hexDigits[d1] + hexDigits[d2];
	}

	public static String MD5Encode(String origin, String charsetname) {
		String resultString = null;
		try {
			resultString = new String(origin);
			MessageDigest md = MessageDigest.getInstance("MD5");
			if (charsetname == null || "".equals(charsetname))
				resultString = byteArrayToHexString(md.digest(resultString
						.getBytes()));
			else
				resultString = byteArrayToHexString(md.digest(resultString
						.getBytes(charsetname)));
		} catch (Exception exception) {
		}
		return resultString;
	}
	
	/**
	 * 计算字符数组特定部分的MD5值.
	 * 
	 * @param info 字符数组
	 * @param start 字符数组开始计算的下标
	 * @param len 字符长度
	 * @return md5值
	 */
	public static String getMd5(byte[] info, int start, int len)
    {
    	try
    	{
    		byte []temp = new byte[len];
    		System.arraycopy(info,start, temp, 0, len);
    		MessageDigest md = MessageDigest.getInstance("MD5");
    		byte[] digest = md.digest(temp);
    		StringBuffer hexString = new StringBuffer();
    		for (int i = 0; i < digest.length; i++)
    		{
    			if ((0xff & digest[i]) < 0x10)
    			{
    				hexString.append("0" + Integer.toHexString((0xFF & digest[i])));
                }
    			else
    			{
    				hexString.append(Integer.toHexString(0xFF & digest[i]));
                }
            }
    		return hexString.toString();
    	}
    	catch(Exception ex)
    	{
    		return null;
    	}
    }

	private static final String hexDigits[] = { "0", "1", "2", "3", "4", "5",
			"6", "7", "8", "9", "a", "b", "c", "d", "e", "f" };

	public static String md5Java(String message) {
		String digest = null;
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] hash = md.digest(message.getBytes("UTF-8"));

			//converting byte array to Hexadecimal String
			StringBuilder sb = new StringBuilder(2 * hash.length);
			for (byte b : hash) {
				sb.append(String.format("%02x", b & 0xff));
			}

			digest = sb.toString();

		} catch (UnsupportedEncodingException ex) {
			//Logger.getLogger(StringReplace.class.getName()).log(Level.SEVERE, null, ex);
		} catch (NoSuchAlgorithmException ex) {
			//Logger.getLogger(StringReplace.class.getName()).log(Level.SEVERE, null, ex);
		}
		return digest;
	}


	public static void main() {
		String test_info =" 89 00 71 44 AA 03 BB 01 B1 A8 CE C4 D0 F2 BA C5 B3 F6 B4 ED A3 AC C7 EB D6 D8 D0 C2 C7 A9 B5 BD 00 00 00 00 00 00 00 00 00 00 00 00";
		String[] manger_aes = test_info.trim().split(" ");
		byte[] key_info = new byte[manger_aes.length];
		for (int i = 0; i < manger_aes.length; i++) {
			int num_info_1 = Integer.valueOf(manger_aes[i], 16);
			key_info[i] = (byte) num_info_1;
		}
		//7d ce dd 4f
		Log.e("123345",byteArrayToHexString(MD5.MD5_sign_byte(key_info)));
		System.out.println(md5Java("admin").toUpperCase());
	}

}
