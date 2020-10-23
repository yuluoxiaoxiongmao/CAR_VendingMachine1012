package com.example.vendingmachine.utils

import android.app.Activity
import android.app.AlertDialog
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.WindowManager
import android.widget.TextView
import com.airiche.sunchip.control.ViewEventNotifier.sendMessage
import com.example.vendingmachine.App
import com.example.vendingmachine.R

/**
 * Created by yingmuliang on 2020/10/15.
 */
class AlertDialogUtils {

    internal lateinit var dialogBuilder: AlertDialog.Builder

    fun HttpDialog(mContext: Activity) {
        DissDialog("HttpDialog(mContext: Activity)")
        dialogBuilder = AlertDialog.Builder(mContext, R.style.bottom_dialog)
        App.dialog = dialogBuilder.create()
        App.dialog!!.show()// show方法放在此处，如果先SetContentView之后在show会报错
        val window = App.dialog!!.window
        val lp = window!!.attributes
        window.attributes = lp
        window.setContentView(R.layout.httpdialog)
        // 因为setContentView的原因会一直隐藏键盘 所以做一下操作默认不显示，点击显示
        window.clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
        //禁止返回操作
        App.dialog!!.setOnKeyListener { dialog, keyCode, event -> true }
    }


    //提示
    fun ToastDialog(mContext: Activity, massage: String) {
        Log.e("TAG","========================ToastDialog================")
        sendMessage(10010, "")
        DissDialog("ToastDialog(mContext: Activity, massage: String)=="+massage)
        dialogBuilder = AlertDialog.Builder(mContext, R.style.bottom_dialog_alph)
        App.dialog = dialogBuilder.create()
        App.dialog!!.show()// show方法放在此处，如果先SetContentView之后在show会报错
        val window = App.dialog!!.window
        val lp = window!!.attributes
        window.attributes = lp
        window.setContentView(R.layout.custom_dialog_toast_layout)
        // 因为setContentView的原因会一直隐藏键盘 所以做一下操作默认不显示，点击显示
        window.clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
        val textView: TextView = window.findViewById(R.id.tv_toast_text)
        textView.setText(massage)
        //禁止返回操作
        App.dialog!!.setOnKeyListener { dialog, keyCode, event -> true }
        if (!massage.contains("正在充值")||!massage.contains("成功")||!massage.contains("失败")){
            handler.removeMessages(0)
            handler.sendEmptyMessageDelayed(0,6000)
        }
    }

    var handler = object :Handler(){
        override fun handleMessage(msg: Message?) {
            super.handleMessage(msg)
            DissDialog("super.handleMessage(msg)")
        }
    }



    //提示
    fun ToastDialogs(mContext: Activity, massage: String) {
        dialogBuilder = AlertDialog.Builder(mContext, R.style.bottom_dialog_alph)
        App.dialog = dialogBuilder.create()
        App.dialog!!.show()// show方法放在此处，如果先SetContentView之后在show会报错
        val window = App.dialog!!.window
        val lp = window!!.attributes
        window.attributes = lp
        window.setContentView(R.layout.custom_dialog_toast_layout)
        // 因为setContentView的原因会一直隐藏键盘 所以做一下操作默认不显示，点击显示
        window.clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
        val textView: TextView = window.findViewById(R.id.tv_toast_text)
        textView.setText(massage)
        //禁止返回操作
        App.dialog!!.setOnKeyListener { dialog, keyCode, event -> true }
    }

    //关闭dialog
    fun DissDialog(string: String) {
//        Log.e("TAG","DissDialog-=========string="+string)
        if (null != App.dialog) {
            App.dialog!!.dismiss()
        }
    }


    companion object {
        var mAlertDialogUtils: AlertDialogUtils? = null

        var TAG = "AlertDialogUtils"
        fun getAlertDialogUtils(): AlertDialogUtils {
            if (mAlertDialogUtils == null) {
                mAlertDialogUtils = AlertDialogUtils()
            }
            return mAlertDialogUtils as AlertDialogUtils
        }
    }
}