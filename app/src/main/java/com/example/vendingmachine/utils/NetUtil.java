package com.example.vendingmachine.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 *  2018-12-17.
 */

public class NetUtil {
    /**
     * 没有连接网络
     */
    private static final int NETWORK_NONE = 0;
    /**
     * 移动网络
     */
    private static final int NETWORK_MOBILE = 1;
    /**
     * 无线网络
     */
    private static final int NETWORK_WIFI = 2;

    public static int getNetWorkState(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            if (activeNetworkInfo != null && activeNetworkInfo.isConnected()) {
                if(activeNetworkInfo.getType()==(ConnectivityManager.TYPE_WIFI)){
                    return NETWORK_WIFI;
                }else if( activeNetworkInfo.getType() == (ConnectivityManager.TYPE_MOBILE)){
                    return NETWORK_MOBILE;
                }
            }else {
                return NETWORK_NONE;
            }
            return NETWORK_NONE;
    }

}
