package com.example.vendingmachine.ui

import android.app.Activity
import android.content.Context
import android.content.IntentFilter
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v7.app.AppCompatActivity
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import com.example.vendingmachine.R
import com.example.vendingmachine.serialport.SerialPortUtil
import com.example.vendingmachine.ui.broadcast.NetBroadcastReceiver
import com.example.vendingmachine.ui.customview.MyVideoView
import com.example.vendingmachine.utils.AlertDialogUtils
import com.example.vendingmachine.utils.AlertDialogUtils.Companion.getAlertDialogUtils
import com.example.vendingmachine.utils.PlayVideoUtils
import com.example.vendingmachine.utils.widget.CustomToast

/**
 * He sun 2018-10-31.
 */
abstract class BaseActivity : AppCompatActivity(), NetBroadcastReceiver.NetEvevt {

    private var mMediaPlayer: MediaPlayer? = null
    private var intentFilter: IntentFilter? = null
    private var netBroadcastReceiver: NetBroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        super.onCreate(savedInstanceState)
        setContentView(getBaseLayout())
        onViewAttach()
        Looper.myQueue().addIdleHandler {
            initView(bundle = savedInstanceState)
            registerWebcast()
            false
        }
        mMediaPlayer = MediaPlayer.create(this, R.raw.pay_success)
    }

    protected abstract fun getBaseLayout(): Int
    protected open fun onViewAttach() {}
    protected open fun onViewDetach() {}
    protected abstract fun initView(bundle: Bundle?)
    protected open fun onNetwork(state: Boolean) {}
    protected open fun onNoNetwork() {}

    override fun onDestroy() {
        super.onDestroy()
        onViewDetach()
        releaseAudio()
        unregisterReceiver(netBroadcastReceiver)
    }

    fun toastShow(msg: String) = toastShow(msg, false)

    fun toastShow(msg: String, success: Boolean) {
        //应客户需求 延长正在充值,充值成功,支付成功等文本
        if (msg.contains("正在充值") || msg.contains("充值成功")
                || msg.contains("支付成功") || msg.contains("充值失败")
                || msg.contains("正在读卡") || msg.contains("余额不足")
                || msg.contains("余额查询失败"))
            getAlertDialogUtils().ToastDialog(this!!, msg)
        else {
            if (msg.contains("提交成功") || msg.contains("出货完成")) {
                getAlertDialogUtils().DissDialog("msg.contains(提交成功)|| msg.contains(出货完成)")
            }
            CustomToast.getInstance().show(this, msg, success)
        }
    }

    fun playAudio(res: Int, Is_Audio: Boolean, video: MyVideoView) {
        if (Is_Audio) {
            try {
                PlayVideoUtils.setVolume(0f, video)
                mMediaPlayer?.reset()
                mMediaPlayer = MediaPlayer.create(this, res)
                mMediaPlayer!!.start()
                mMediaPlayer?.setOnCompletionListener { PlayVideoUtils.setVolume(1f, video) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    open fun releaseAudio() {
        if (mMediaPlayer != null && mMediaPlayer!!.isPlaying) mMediaPlayer!!.stop()
        mMediaPlayer!!.release()
    }

    open fun shipmentCode(Electric: Int) {
        /*val macType = App.spUtil!!.getInt(Constant.MAC_TYPE_KEY,0)
        val list = App.sjdao?.queryForAll()
        SerialPortUtil.getInstance().send_open(Electric,0,macType, list!![0].sptime.toInt(),
                list[0].timeout.toInt(),list[0].taketype.toInt(),list[0].taketime.toInt())*/

        SerialPortUtil.getInstance().send_open(Electric)//宏祥出货
    }

    fun yhidekeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS)
    }

    private fun registerWebcast() {
        intentFilter = IntentFilter()
        intentFilter?.addAction("android.net.conn.CONNECTIVITY_CHANGE")
        netBroadcastReceiver = NetBroadcastReceiver()
        registerReceiver(netBroadcastReceiver, intentFilter)
        netBroadcastReceiver?.onNetCallback(this)
    }

    override fun onNetChange(netMobile: Int) {
        when (netMobile) {
            0 -> onNoNetwork()
            1 -> onNetwork(false)
            2 -> onNetwork(true)
        }
    }

}