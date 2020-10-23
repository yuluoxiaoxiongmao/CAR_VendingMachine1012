package com.example.vendingmachine.ui.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;

import com.example.vendingmachine.App;
import com.example.vendingmachine.utils.NetUtil;

/**
 *  2018-12-17.
 */

public class NetBroadcastReceiver extends BroadcastReceiver {

    private NetEvevt evevt;
    @Override
    public void onReceive(Context context, Intent intent) {
        // 如果相等的话就说明网络状态发生了变化  
        if(intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)){
        int netWorkState= NetUtil.getNetWorkState(App.Companion.getMContext());
            if (evevt!=null){
                evevt.onNetChange(netWorkState);
            }
        }
    }


    public void onNetCallback(NetEvevt evevt){
        this.evevt = evevt;
    }
    public interface NetEvevt {
        void onNetChange(int netMobile);
    }
}
