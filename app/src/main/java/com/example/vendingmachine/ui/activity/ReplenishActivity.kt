package com.example.vendingmachine.ui.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.CompoundButton
import android.widget.PopupWindow
import com.example.vendingmachine.App
import com.example.vendingmachine.R
import com.example.vendingmachine.adapter.ReplenishAdapter
import com.example.vendingmachine.bean.GoodsInfoBean
import com.example.vendingmachine.bean.StockToJson
import com.example.vendingmachine.platform.common.Constant
import com.example.vendingmachine.presenter.ReplenishPresenter
import com.example.vendingmachine.serialport.SerialPortUtil
import com.example.vendingmachine.serialport.SerialPortUtil_Money
import com.example.vendingmachine.ui.BaseActivity
import com.example.vendingmachine.ui.customview.LiftingParameterPopuView
import com.example.vendingmachine.ui.customview.PasswordPopu
import com.example.vendingmachine.ui.customview.VersionUpdatePopu
import com.google.gson.Gson
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu
import kotlinx.android.synthetic.main.activity_replenish.*
import kotlinx.android.synthetic.main.slidingmenu_layout.*
import java.util.*

/**
 * 设置界面 2018-11-06.
 */
class ReplenishActivity :BaseActivity(),ReplenishPresenter.GetGoodsData,View.OnClickListener,
        CompoundButton.OnCheckedChangeListener,ReplenishAdapter.ButtonInterface,
        ReplenishPresenter.UploadStockInterface,ReplenishPresenter.VersionUpdateInterface
        ,PasswordPopu.PasswordInterface,SerialPortUtil.OnDataReceiveListener{

    private var mProcess : ReplenishPresenter<ReplenishPresenter.GetGoodsData>? = null
    private var menu : SlidingMenu? = null
    private var adapter : ReplenishAdapter? = null
    private var list : List<GoodsInfoBean.MessageBean>? = null
    private var popu : PopupWindow? = null
    private var versionPopu : PopupWindow? = null
    private var passwordPopu : PasswordPopu? = null
    private var IsTesting : Boolean = false
    private var position : Int = 0
    private var index = 0
    private var serialMsg : String = ""

    override fun getBaseLayout(): Int = R.layout.activity_replenish

    override fun onViewAttach() {
        super.onViewAttach()
        mProcess = ReplenishPresenter()
        mProcess!!.attachView(this,this)
    }

    override fun onViewDetach() {
        super.onViewDetach()
        mProcess!!.detachView()
        mProcess = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SerialPortUtil.getInstance().setOnDataReceiveListener(this)
    }
    override fun initView(bundle: Bundle?) {
        Constant.BOX_ERR = ""
        replenish_list_view.layoutManager = LinearLayoutManager(this)
        bt_re_return_button.setOnClickListener(this)
        bt_exit_procedure.setOnClickListener(this)
        bt_pv_replenishment.setOnClickListener(this)
        bt_onekey_rep.setOnClickListener(this)
        bt_onekey_testing.setOnClickListener(this)
        bt_set_parameter.setOnClickListener(this)
        bt_version_update.setOnClickListener(this)
        bt_update_data.setOnClickListener(this)
        bt_coin_return.setOnClickListener(this)

        bt_push_plate.setOnClickListener(this)
        bt_close.setOnClickListener(this)

        mProcess?.getData(Constant.CODE_ONE,Constant.MAC_ID,false)

        initMenu()
        sc_bsc.isChecked = App.spUtil!!.getBoolean(Constant.IS_BSC,false)
        sc_voice.isChecked = App.spUtil!!.getBoolean(Constant.IS_VOICE,false)
        my_spinner.setSelection(App.spUtil!!.getInt(Constant.MAC_TYPE_KEY,0),true)
        getVersionInfo()
        sc_bsc.setOnCheckedChangeListener(this)
        sc_voice.setOnCheckedChangeListener(this)
        bt_exit_mac_id.setOnClickListener(this)

    }

    fun initMenu(){
        menu = SlidingMenu(this)
        menu?.mode = SlidingMenu.LEFT
        menu?.touchModeAbove = SlidingMenu.TOUCHMODE_FULLSCREEN //设置滑动的屏幕范围，该设置为全屏区域都可以滑动
        menu?.setFadeEnabled(true)
        menu?.setBehindOffsetRes(R.dimen.menu_width) //SlidingMenu划出时主页面显示的剩余宽度
        menu?.setFadeDegree(0.35f) //SlidingMenu滑动时的渐变程度
        menu?.attachToActivity(this, SlidingMenu.SLIDING_CONTENT)  //使SlidingMenu附加在Activity上
        menu?.setMenu(R.layout.slidingmenu_layout) //设置menu的布局文件
    }

    override fun onClick(v: View?) {
        when(v){
            bt_re_return_button -> finish()
            bt_pv_replenishment -> uploadStock(false)
            bt_onekey_rep       -> uploadStock(true)
            bt_onekey_testing   -> electricTesting()
            bt_exit_procedure   -> sendReturnValue(1)
            bt_set_parameter    -> showPassword()
            bt_version_update   -> mProcess?.getVersionUpdate()
            bt_update_data      -> mProcess?.getData(Constant.CODE_ONE,Constant.MAC_ID,true)

            bt_exit_mac_id      -> cancellation()


            bt_push_plate -> SerialPortUtil.getInstance()
            bt_close -> SerialPortUtil.getInstance().closeSerialPort()

            bt_coin_return -> SerialPortUtil_Money.getInstance().coin_return(10f)

        }
    }

    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        when(buttonView){
            sc_bsc   -> App.spUtil!!.putBoolean(Constant.IS_BSC,isChecked)
            sc_voice -> App.spUtil!!.putBoolean(Constant.IS_VOICE,isChecked)
        }
    }

    override fun onclick(view: View?, position: Int, id: Int) {
        this.position = position
        when(id){
            0 -> { Log.e("TAG","电机号："+list!![position].motonum)
                //SerialPortUtil.getInstance().send_reset(list!![position].motonum,5,0)
//                SerialPortUtil.getInstance().send_adjust_channel(list!![position].motonum)//宏祥电机检测
                SerialPortUtil.getInstance().send_open(list!![position].motonum)//宏祥检测改成出货
            }
            1 -> setStock(position,true)
            2 -> setStock(position,false)
        }
    }

    override fun onDataReceive(cmd: Int, data: Int) {
        when(cmd){
            7 -> handler.sendEmptyMessageDelayed(2,500)
            8 -> handler.sendEmptyMessageDelayed(3,500)
            /*5 -> handler.sendEmptyMessage(5)
            6 -> handler.sendEmptyMessage(6)
            7 -> handler.sendEmptyMessage(7)
            8 -> handler.sendEmptyMessage(8)*/
        }
    }

    override fun onDataToast(str: String?) {
        serialMsg = str!!
        handler.sendEmptyMessage(4)
    }

    private fun sendReturnValue(msg : Int){
        val intent = intent
        intent.putExtra(Constant.RETURN_KEY,msg)
        setResult(RESULT_OK,intent)
        finish()
    }

    private val handler = @SuppressLint("HandlerLeak")
    object : Handler() {
        override fun handleMessage(msg: Message?) {
            super.handleMessage(msg)
            when(msg!!.what){
                2 -> setShipmentState(3)
                3 -> setShipmentState(4)
                4 -> toastShow(serialMsg)
                /*5 -> toastShow("复位成功!")
                6 -> toastShow("测试成功!")
                7 -> toastShow("速度设置成功!")
                8 -> toastShow("高度设置成功!")*/
            }
        }
    }

    private fun setStock(position : Int,is_add : Boolean){
        var stock = list!![position].store
        if(is_add){
            if (stock<Constant.STOCK_TOTAL){
                stock++
                list!![position].store = stock
            }else{ toastShow("库存不能大于${Constant.STOCK_TOTAL}") }
        }else{
            if (stock<=0){ toastShow(resources.getString(R.string.stocka_text)) }else{
                stock--
                list!![position].store = stock
            }
        }
        adapter?.notifyDataSetChanged()
    }

    private fun uploadStock(oneKey : Boolean){
        if (oneKey){
            for (i in list!!.indices){
                list!![i].store = Constant.STOCK_TOTAL
            }
            adapter?.notifyDataSetChanged()
        }
        val json = StockToJson()
        val data = ArrayList<StockToJson.ContentBean>()
        for (i in list!!.indices) { data.add(StockToJson.ContentBean(list!![i].box, list!![i].store)) }
        json.mac_id = Constant.MAC_ID
        json.content = data
        val s = Gson().toJson(json)
        mProcess?.uploadStock(s)
    }

    private fun electricTesting() {
        if (index < list!!.size) {
            IsTesting = true
            adapter?.setPositions(index, 2)
            val box = list!![index].motonum
            //SerialPortUtil.getInstance().send_reset(box,5,0)
            SerialPortUtil.getInstance().send_adjust_channel(box)//宏祥电机检测
        }else{
            index = 0
            IsTesting = false
        }
    }

    private fun setShipmentState(state : Int){
        if (IsTesting){
            adapter?.setPositions(index, state)
            index++
            electricTesting()
        }else{adapter?.setPositions(position, state)}
    }
    private fun cancellation() {
        App.spDao?.queryRaw("delete from goods_user")
        App.spDao?.queryRaw("update sqlite_sequence SET seq = 0 where name ='goods_user'")
        App.videoDao?.queryRaw("delete from video_user")
        App.videoDao?.queryRaw("update sqlite_sequence SET seq = 0 where name ='video_user'")
        App.spUtil?.putBoolean(Constant.IS_LOGIN_KEY,false)
        App.spUtil!!.putBoolean(Constant.IS_VIDEO_KEY, false)
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK//关掉所要到的界面中间的activity
        startActivity(intent)
    }
    private fun showParameterPopu(){
        if (popu == null){
            popu = LiftingParameterPopuView(this)
            popu!!.showAtLocation(this.window.decorView, Gravity.BOTTOM, 0, 0)
        }else{ popu!!.showAtLocation(this.window.decorView, Gravity.BOTTOM, 0, 0) }
    }
    private fun showVersionPopu(apkUrl : String){
        if (versionPopu == null){
            versionPopu = VersionUpdatePopu(this,apkUrl)
            versionPopu!!.showAtLocation(this.window.decorView,Gravity.CENTER,0,0)
        }else{
            versionPopu!!.showAtLocation(this.window.decorView,Gravity.CENTER,0,0)
        }
    }
    private fun showPassword(){
        if (passwordPopu == null){
            passwordPopu = PasswordPopu(this)
            passwordPopu!!.showAtLocation(this.window.decorView,Gravity.CENTER,0,0)
            passwordPopu!!.correctPasswordCallback(this)
        }else{
            passwordPopu!!.showAtLocation(this.window.decorView,Gravity.CENTER,0,0)
        }
    }
    override fun goodsInfoSuccess(data: List<GoodsInfoBean.MessageBean>,IsUpdate: Boolean) {
        if (IsUpdate){
            sendReturnValue(2)
        }else {
            list = data
            adapter = ReplenishAdapter(list,this)
            replenish_list_view.adapter = adapter
            adapter?.buttonSetOnclick(this)
        }
    }
    override fun uploadSuccess(msg : String) {
        toastShow(msg)
    }
    override fun versionUpdateSuccess(url : String) {
        showVersionPopu(url)
    }
    override fun versionUpdateFail(msg: String) {
        toastShow(msg)
    }
    override fun onResume() {
        super.onResume()
        //SerialPortUtil.getInstance().setOnDataReceiveListener(this)
    }
    override fun correctPassword() {
        showParameterPopu()
    }
    private var pm: PackageManager? = null
    private fun getVersionInfo() {
        // TODO Auto-generated method stub
        pm = packageManager
        try {
            // 0代表拿所有的信息 packageInfo 是一个bean对象 是对整个清单文件的封装
            // ApplicationInfo是PackageInfo的子集
            val packageInfo = pm!!.getPackageInfo(packageName, 0)
            my_version.text = packageInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }

    }

}