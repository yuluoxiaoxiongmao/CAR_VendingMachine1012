package com.example.vendingmachine.ui.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.vendingmachine.platform.common.Constant
import com.example.vendingmachine.ui.activity.LoginActivity

/**
 * HE 2018/5/8.
 */

class StartBootComplete : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Constant.ACTION_BOOT) {
            val intent1 = Intent(context, LoginActivity::class.java)
            intent1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent1)
        }
    }
}
