package com.example.vendingmachine.utils;

import android.content.Context;
import android.provider.Settings;
import android.telephony.TelephonyManager;

/**
 * He 2018-12-17.
 */

public class PhoneInfoUtils {
    private static String TAG = "PhoneInfoUtils";

    private TelephonyManager telephonyManager;
    //移动运营商编号
    private String NetworkOperator;
    private Context context;

    public PhoneInfoUtils(Context context) {
        this.context = context;
        telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
    }

    //获取sim卡iccid
    public String getIccid() {
        String iccid = "N/A";
        try {
            iccid = telephonyManager.getSimSerialNumber();
        } catch (SecurityException e) {
            e.printStackTrace();
        }
        return iccid;
    }

    //获取电话号码
    public String getNativePhoneNumber() {
        String nativePhoneNumber = "N/A";
        try {
            nativePhoneNumber = telephonyManager.getLine1Number();
        } catch (SecurityException e) {
            e.printStackTrace();
        }
        return nativePhoneNumber;
    }

    //获取手机服务商信息
    public String getProvidersName() {
        String providersName = "N/A";
        NetworkOperator = telephonyManager.getNetworkOperator();
        //IMSI号前面3位460是国家，紧接着后面2位00 02是中国移动，01是中国联通，03是中国电信。
        // Flog.d(TAG,"NetworkOperator=" + NetworkOperator);
        if (NetworkOperator.equals("46000") || NetworkOperator.equals("46002")) {
            providersName = "中国移动";//中国移动
        } else if (NetworkOperator.equals("46001")) {
            providersName = "中国联通";//中国联通
        } else if (NetworkOperator.equals("46003")) {
            providersName = "中国电信";//中国电信
        }
        return providersName;

    }

    public String getPhoneInfo() {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        StringBuffer sb = new StringBuffer();
        try {
            sb.append("\nLine1Number = " + tm.getLine1Number());
            sb.append("\nNetworkOperator = " + tm.getNetworkOperator());//移动运营商编号
            sb.append("\nNetworkOperatorName = " + tm.getNetworkOperatorName());//移动运营商名称
            sb.append("\nSimCountryIso = " + tm.getSimCountryIso());
            sb.append("\nSimOperator = " + tm.getSimOperator());
            sb.append("\nSimOperatorName = " + tm.getSimOperatorName());
            sb.append("\nSimSerialNumber = " + tm.getSimSerialNumber());
            sb.append("\nSubscriberId(IMSI) = " + tm.getSubscriberId());
        }catch (SecurityException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    /*** 获取手机IMEI*/
    public String getIMEI() {
        String IMEI = "";
        try {
            IMEI = telephonyManager.getDeviceId();
        } catch (SecurityException e) {
            e.printStackTrace();
        }
        return IMEI;
    }
    /*** 获取手机IMSI*/
    public String getIMSI(Context context) {
        String IMSI = "";
        try {
            IMSI = telephonyManager.getSubscriberId();
        } catch (SecurityException e) {
            e.printStackTrace();
        }
        return IMSI;
    }

    //获取本机真实的物理地址
    public String getLocalMacAddress() {
        return Settings.Secure.getString(context.getContentResolver(), "bluetooth_address");
    }

}
