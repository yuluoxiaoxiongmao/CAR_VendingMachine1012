package com.example.vendingmachine.ui.activity

import android.content.Intent
import android.graphics.Bitmap
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Toast
import com.airiche.sunchip.control.ViewEventNotifier
import com.airiche.sunchip.control.ViewEventObject
import com.example.vendingmachine.App
import com.example.vendingmachine.R
import com.example.vendingmachine.platform.common.AddSocketClient
import com.example.vendingmachine.serialport.CarSerialPortUtil
import com.example.vendingmachine.ui.BaseActivity
import com.example.vendingmachine.utils.AlertDialogUtils
import kotlinx.android.synthetic.main.activity_flying_charge.*
import kotlinx.android.synthetic.main.activity_flying_charge.home_btn_100
import kotlinx.android.synthetic.main.activity_flying_charge.home_btn_50
import kotlinx.android.synthetic.main.layout_cz.*
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/*
* 飞充界面
* */
class FlyingChargeActivity : BaseActivity() {

    override fun getBaseLayout(): Int = R.layout.activity_flying_charge

    override fun initView(bundle: Bundle?) {
        ViewEventNotifier.addEventListener(this@FlyingChargeActivity)
//        Toast.makeText(this, "FlyingMoney;" + FlyingMoney, Toast.LENGTH_LONG).show()

        TopUpAccountBalance.setText("充值金账户余额:   " + (FlyingMoney!! / 100f) + "元")
        home_btn_All.setText((FlyingMoney!! / 100f).toString() + "元")
        //逻辑卡号
        CardNumberText.setText("逻辑卡号是:" + CardNumber)
        home_btn_50.setOnClickListener { chanageState(0) }
        home_btn_100.setOnClickListener { chanageState(1) }
        home_btn_All.setOnClickListener { chanageState(2) }
        FlyFinish.setOnClickListener {
            this.finish()
        }
        YangchengTongFeiChong.setOnClickListener {
            Log.e("TAG","正在充值中，请勿移动卡片...+========="+home_cz_type)
            if(FlyingMoney!! <cz_money ){
                //要充值的钱不足不让充值
                toastShow("您的余额不足")
                return@setOnClickListener
            }
            AlertDialogUtils.getAlertDialogUtils().ToastDialogs(this@FlyingChargeActivity, "正在读卡,请勿移动卡片...")
            if (home_cz_type == 0) {
                client_add!!.getSendSocketInfo(cz_cmd, 400, ship_info, (cz_money).toString())
            } else if (home_cz_type == 1) {
                client_add!!.getSendSocketInfo(cz_cmd, 300, "0", (cz_money).toString())
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun FlyingChargeEventBus(Object: ViewEventObject) {
        when (Object.event) {
            10010 -> {
                this.finish()
            }
        }
        Log.e("FlyingChargeActivity", "event=" + Object.event + " String=" + Object.obj)
    }

    fun changeInitView() {
        home_btn_50.setBackgroundResource(R.drawable.fy_re_textview_bj)
        home_btn_100.setBackgroundResource(R.drawable.fy_re_textview_bj)
        home_btn_All.setBackgroundResource(R.drawable.fy_re_textview_bj)
        home_btn_50.setTextColor(this.resources.getColor(R.color.flytext))
        home_btn_100.setTextColor(this.resources.getColor(R.color.flytext))
        home_btn_All.setTextColor(this.resources.getColor(R.color.flytext))
    }

    var cz_money: Int = 50
    fun chanageState(state: Int) {
        changeInitView()
        when (state) {
            0 -> {
                home_btn_50.setBackgroundResource(R.drawable.fy_blue_re_jc_bt_huihong_bj)
                home_btn_50.setTextColor(this.resources.getColor(R.color.white))
                cz_money = 50
            }
            1 -> {
                home_btn_100.setBackgroundResource(R.drawable.fy_blue_re_jc_bt_huihong_bj)
                home_btn_100.setTextColor(this.resources.getColor(R.color.white))
                cz_money = 10000
            }
            2 -> {
                home_btn_All.setBackgroundResource(R.drawable.fy_blue_re_jc_bt_huihong_bj)
                home_btn_All.setTextColor(this.resources.getColor(R.color.white))
                cz_money = FlyingMoney!!
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ViewEventNotifier.removeEventListener(this@FlyingChargeActivity)
    }

    companion object {
        var TAG: String = "FlyingChargeActivity"
        var FlyingMoney: Int? = null
        var CardNumber: String? = null
        var home_cz_type = 0
        var client_add: AddSocketClient? = null
        private var cz_cmd: ByteArray? = null
        var ship_info = ""
        fun getFlyingChargeActivity(mFlyingMoney: Int, mCardNumber: String, mhome_cz_type: Int,
                                    mclient_add: AddSocketClient, mcz_cmd: ByteArray, mship_info: String) {
            client_add = mclient_add
            home_cz_type = mhome_cz_type
            CardNumber = mCardNumber
            FlyingMoney = mFlyingMoney
            cz_cmd = mcz_cmd
            ship_info = mship_info
            val intent = Intent(App.mContext, FlyingChargeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            App.mContext.startActivity(intent)
        }
    }
}
