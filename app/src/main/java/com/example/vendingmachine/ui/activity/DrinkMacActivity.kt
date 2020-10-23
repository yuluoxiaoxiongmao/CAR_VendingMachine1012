package com.example.vendingmachine.ui.activity

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import com.airiche.sunchip.control.ViewEventNotifier
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.vendingmachine.App
import com.example.vendingmachine.R
import com.example.vendingmachine.bean.*
import com.example.vendingmachine.db.GoodsUser
import com.example.vendingmachine.platform.common.AddSocketClient
import com.example.vendingmachine.platform.common.AddSocketClient.bytesToHexString
import com.example.vendingmachine.platform.common.Constant
import com.example.vendingmachine.platform.common.Constant.Companion.GAME_PAY_CD
import com.example.vendingmachine.platform.common.Constant.Companion.IS_PAY
import com.example.vendingmachine.platform.common.Constant.Companion.IS_QUERY
import com.example.vendingmachine.platform.common.Constant.Companion.PAY_ZF
import com.example.vendingmachine.platform.common.SocketClient2
import com.example.vendingmachine.platform.common.SocketClient3
import com.example.vendingmachine.platform.http.HttpUrl
import com.example.vendingmachine.presenter.DrinkMacPresenter
import com.example.vendingmachine.serialport.CarSerialPortUtil
import com.example.vendingmachine.serialport.SerialPortUtil
import com.example.vendingmachine.serialport.SerialPortUtil_Money
import com.example.vendingmachine.ui.BaseActivity
import com.example.vendingmachine.ui.customview.*
import com.example.vendingmachine.utils.AlertDialogUtils
import com.example.vendingmachine.utils.InterceptString
import com.example.vendingmachine.utils.TextLog
import com.example.vendingmachine.utils.ZxingUtil
import com.example.vendingmachine.utils.encryption.Des
import com.example.vendingmachine.utils.widget.FaultDialog
import com.example.vendingmachine.utils.widget.GlideImageLoader
import com.example.vendingmachine.utils.widget.ShipmentsDialog
import com.youth.banner.Banner
import com.youth.banner.BannerConfig
import com.youth.banner.Transformer
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.layout_cz.*
import kotlinx.android.synthetic.main.layout_status_bar.*
import java.util.*
import kotlin.collections.ArrayList

/**
 *  2018-11-06.
 */
class DrinkMacActivity : BaseActivity(), DrinkMacPresenter.RequestCodeView, DrinkMacPresenter.QueryPayResultsView,
        SlideNavigationView.SlideViewItmInterface, PurchasePopuView.PayPopuInterface, ComputerView.ButtonInterface,
        ScavengerEditText.ScavengingTextInterface, DrinkMacPresenter.ScavengingCodeView, PasswordPopu.PasswordInterface,
        DrinkMacPresenter.AdvVideoView, DrinkMacPresenter.UploadShipmentView, ShipmentsDialog.ShippingCountdownInterface,
        DrinkMacPresenter.TimingRequestView, SerialPortUtil.OnDataReceiveListener, DrinkMacPresenter.CancelOrderView, DrinkMacPresenter.UploadYCTView,
        SerialPortUtil_Money.OnDataReceiveListener, CarSerialPortUtil.OnDataReceiveListener, DrinkMacPresenter.GetGoodsData,
        DrinkMacPresenter.GetBannerView, DrinkMacPresenter.uploadYCTDataView, DrinkMacPresenter.GetYCTDataView, DrinkMacPresenter.GetYCTUploadToWdView
        , DrinkMacPresenter.RequestCodeViewCZ {

    private var mPresenter: DrinkMacPresenter<DrinkMacPresenter.RequestCodeView>? = null
    private var paypopu: PurchasePopuView? = null
    private var buyTimer: CountDownTimer? = null
    private var list: List<GoodsUser>? = null

    private var passwordPopu: PasswordPopu? = null
    private var index: Int = 0
    private var timer: Timer? = null
    private var faultDialog: FaultDialog? = null
    private var moneyTotal: Double = 0.0
    private var moneyIndex: Double = 0.0
    private var IS_COIN: Boolean = false
    private var spMoney: Double = 0.0
    private var client2: SocketClient2? = null
    private var client_wd: SocketClient3? = null
    private var client_add: AddSocketClient? = null

    //读卡器应付金额
    private var car_spMoney: Int = 0

    //支付类型 1  二维码 2 羊城通
    private var pay_type: Int = 2

    //0 购买操作  1充值操作
    private var home_type: Int = 0

    //0 M1充值  1 CPU充值
    private var home_cz_type: Int = 0
    /*
    故障货道列表
     */
//    private var list_err: List<String> = ArrayList()
//    private var num_err: Int = 0

    //0是未发送，1是已发送
    private var is_send: Int = 0

    override fun getBaseLayout(): Int = R.layout.activity_main

    private var ship_info: String = ""

    private var mPresenterCZ: DrinkMacPresenter<DrinkMacPresenter.RequestCodeViewCZ>? = null
    override fun onViewAttach() {
        super.onViewAttach()
        mPresenter = DrinkMacPresenter()
        mPresenter!!.attachView(this, this)
    }

    override fun onViewDetach() {
        super.onViewDetach()
        mPresenter?.detachView()
        mPresenter = null
        cancelBuyTime()
//        cancelOnLine()
    }

    @SuppressLint("SetTextI18n")
    override fun initView(bundle: Bundle?) {
        //开机初始化传送序号
        Constant.CAR_ORDER_NUM = 1
        Constant.CAR_ORDER_MONEY = 0
        TextLog.initData(Constant.MAC_ID)
        list = App.spDao!!.queryForAll()
        Constant.MAC_ID = App.spUtil!!.getString(Constant.MAC_ID_KEY, "")
        tv_mac_id_text.text = Constant.MAC_ID_TOP + Constant.MAC_ID
        my_sid_view.intiData(list)
        my_scavenging_ed.requestFocus()
        my_sid_view.SlideViewItmOnclick(this)
        //my_computer_view.buttonConfirmOnclick(this)
        my_scavenging_ed.ScavengingTextCallBack(this)

        SerialPortUtil.getInstance().setOnDataReceiveListener(this)
        CarSerialPortUtil.getInstance().setOnDataReceiveListener(this)
//        list_err = ArrayList()//初始化错误货道
        tv_mac_id_text.setOnClickListener {
            //                        showPassword()
//            CarSerialPortUtil.getInstance().test_card()
//            CarSerialPortUtil.getInstance().card_state()
//            SerialPortUtil.getInstance().wd_is_connect(0)
//            SerialPortUtil.getInstance().wd_out_goods(1)
//            SerialPortUtil.getInstance().wd_out_goods_test(1)
//            CarSerialPortUtil.getInstance().pay_test_sign_in_1()
//            CarSerialPortUtil.getInstance().pay_card_serch()//消费寻卡

//            client2!!.getSendSocketInfo(null, 114, "123")

//            client2!!.getSendSocketInfo(null, 115, "123")
//            client2!!.getSendSocketInfo(null, 116, "123")
        }
//        CarSerialPortUtil.getInstance().pay_test_sign_in_1()
        SerialPortUtil.getInstance().wd_is_light(1)
//        AESUtils.main("")
        Des.main_19_65()
//        initVideo()
//        starOnLine()
        //屏保图片
//        cancelHome()
        //充值测试
//        client2 = SocketClient2(Constant.SOCKET_URL, Constant.SOCKET_ID_ADD, this, "")
        //消费
        client_add = AddSocketClient(Constant.SOCKET_URL, Constant.SOCKET_ID_ADD, this, "")
        client_add!!.setmActivity(this)
        client2 = SocketClient2(Constant.SOCKET_URL, Constant.SOCKET_ID, this, "")
        client2!!.setmActivity(this)
        client_wd = SocketClient3(Constant.WD_SOCKET_URL, Constant.WD_SOCKET_ID, this)
        client_wd!!.setmActivity(this)
        //client_add = AddSocketClient(Constant.SOCKET_URL, Constant.SOCKET_ID_ADD, this, "")
        //client_add!!.setmActivity(this)
        mPresenter?.getData("00", Constant.MAC_ID, true)
        mPresenter?.getBanner(Constant.MAC_ID)

        is_send = 0

        title_yct.setOnClickListener {
            //羊城通功能
//            toastShow("功能完善中")
//            CarSerialPortUtil.getInstance().pay_card_serch()//寻卡
//            CarSerialPortUtil.getInstance().pay_test_card()
            //充值测试,签到认证
//            CarSerialPortUtil.getInstance().test_card()
            //充值查询卡物理信息
//            CarSerialPortUtil.getInstance().test_card_read()
            //查询M1卡信息
//            CarSerialPortUtil.getInstance().R_M1_GET_CARDINFO()

            home_ll_cz.visibility = View.GONE
            my_sid_view.visibility = View.VISIBLE
            home_type = 1
            home_txt_ye.setText("")
            LogicalCardNumber.setText("")
//            client_wd!!.b4_wd_send()
//            mPresenter?.getYctUploadToWD(Constant.MAC_ID, "success")//清空服务器上传数据
//            client2!!.getSendSocketInfo(null, 114, "123")
        }
        title_goods.setOnClickListener {
            //正常购买功能
//            CarSerialPortUtil.getInstance().pay_test_card()
            //消费第一步  签到认证
//            client2 = SocketClient2(Constant.SOCKET_URL, Constant.SOCKET_ID, this, "")
//            CarSerialPortUtil.getInstance().pay_test_sign_in_1()
//            client2!!.setmActivity(this)
//            CarSerialPortUtil.getInstance().pay_test_buy_1()

            //充值测试,签到认证
//            CarSerialPortUtil.getInstance().test_card()
            //获取卡号，卡物理信息
//            CarSerialPortUtil.getInstance().cz_card_read_wuli()
            //查询卡应用信息
//            CarSerialPortUtil.getInstance().search_app_card_info()

            //查询M1卡信息
//            CarSerialPortUtil.getInstance().R_CPU_GET_CARDINFO()

            //查询PKI状态
//            CarSerialPortUtil.getInstance().R_PUB_GET_PKISTATE("cz")

            home_ll_cz.visibility = View.VISIBLE
            my_sid_view.visibility = View.GONE
            LogicalCardNumber.setText("")
            //充值测试,签到认证
//            CarSerialPortUtil.getInstance().test_card()
            home_type = 0


//            mPresenter?.getYctUploadToWD(Constant.MAC_ID,"success")//清空服务器上传数据
//            mPresenter?.getYctData(Constant.MAC_ID)//拉取服务器数据-

//            //联机消费快速结算
//            client2!!.getSendSocketInfo(null, 114, "123")

//            //脱机消费结算 A5开始  A8结束
//            client2!!.getSendSocketInfo(null, 115, "123")
        }
//        mPresenter?.getuploadYCTData(Constant.MAC_ID,"123","AA 83 00 01 02 80 00 98 7B 4D 8D 9F B4 EB 40 B4 82 1B CF 98 E7 F5 06 E6 8A 6A 9D 86 00 57 EC 18 F8 B4 FF 86 3D E0 75 18 CC BD 5A E0 E5 5A BD FB 04 56 4D AA 6F 58 82 80 0C 6F 5C D3 56 7C 78 12 70 6E 7B 86 5F 9E EB DB A0 BA 0F DC D2 9B 50 CC 91 62 A4 16 1E A8 24 80 C8 90 9F 82 01 7F FC 77 C6 01 F4 0A A5 19 57 16 78 32 3B 4E 0C DC F6 C7 DE D8 CF 83 87 8B B0 B9 7F 43 1E 6C 6C 96 B5 5B A4 94 89 CA 2D B0 91 E5 59 02 41 B7 70 27 10 30 CA 41 9E BE EF 88 65 BE 14 D3 AA 1C 97 B3 09 34".replace(" ",""))
//        mPresenter?.scavengingCode(s.toString(), Constant.BOX, Constant.MAC_ID)
        //充值测试,签到认证
//            CarSerialPortUtil.getInstance().test_card()
//消费签到认证
//        CarSerialPortUtil.getInstance().pay_test_sign_in_1()
//        CarSerialPortUtil.getInstance().pay_test_card()
//        CarSerialPortUtil.getInstance().pay_card_set()//初始化读卡器设置 默认80

        //消费相关
        //消费签到认证
        CarSerialPortUtil.getInstance().pay_test_sign_in_1()
        //充值相关
        //充值测试,签到认证
        CarSerialPortUtil.getInstance().test_card()
        initCZView()
//        Des.A8_des()
    }

    override fun onDataReceive_coin(money: Float) {
        moneyIndex = money.toDouble()
        handler.sendEmptyMessage(9)
    }

    override fun onDataReceive_note(money: Int) {

    }

    override fun onDataToast_Money(str: String?) {

    }

    private fun investMoney(money: Double) {
        moneyTotal += money
        toastShow("投入：" + money + "  ====   投入总数：" + moneyTotal)
        if (moneyTotal == spMoney) {
            IS_COIN = true
            moneyTotal = 0.0
            sendShipments(Constant.ELECTRIC)
//            SerialPortUtil.getInstance().wd_out_goods(Constant.ELECTRIC)
        } else if (moneyTotal > spMoney) {
            val i = moneyTotal - spMoney
            toastShow("需要退的钱：${moneyTotal - spMoney}")
            SerialPortUtil_Money.getInstance().coin_return(i.toFloat())
            moneyTotal = 0.0
            IS_COIN = true
            sendShipments(Constant.ELECTRIC)
//            SerialPortUtil.getInstance().wd_out_goods(Constant.ELECTRIC)
        }
    }

    private fun initVideo() {
        if (App.spUtil!!.getBoolean(Constant.IS_VIDEO_KEY, false)) {
            my_video_download.layout_xz_video.visibility = View.GONE
            my_video_download.getVideoList()
        } else {
            my_video_download.layout_xz_video.visibility = View.VISIBLE
            mPresenter?.getAdvVideo("107", Constant.MAC_ID, false)
        }
    }

    override fun OnItmInterface(position: Int) {

        if (list!![position].store == 0) {
            toastShow("库存不足")
            return
        } else {
//            showPayWindow(position,1)
//            mPresenter?.requestCode(Constant.CODE_THREE, Constant.PAY_ZF, Constant.MAC_ID, Constant.BOX, Constant.G_ID)

            if (Constant.CAR_SIGN_19 == null) {
                CarSerialPortUtil.getInstance().pay_test_sign_in_1()
            }
            showPopWayDialog(position)
        }
    }

    override fun OnPopuClickInterface(id: Int) {
        when (id) {
            0 -> {
                //取消的时候直接倒计时无操作
                NoAction()
                if (moneyTotal > 0) {
                    SerialPortUtil_Money.getInstance().coin_return(moneyTotal.toFloat())
                    moneyTotal = 0.0
                }
                cancelBuyTime()
            }
            1 -> {
                //判断是充值还是购买 充值没有支付宝支付
                if (home_type == 1) {
                    toastShow("开始查询余额")
                    client_add!!.getSendSocketInfo(cz_cmd, 1008, ship_info, (cz_money).toString())
                    cancelBuyTime()
                    AlertDialogUtils.getAlertDialogUtils().HttpDialog(this)
                } else mPresenter?.requestCode(Constant.CODE_THREE, PAY_ZF, Constant.MAC_ID, Constant.BOX, Constant.G_ID)
            }
            2 -> {
                mPresenter?.requestCode(Constant.CODE_THREE, PAY_ZF, Constant.MAC_ID, Constant.BOX, Constant.G_ID)

            }
            3 -> {
                toastShow("功能开发中")
            }
        }
    }

    override fun uploadYctDataSuccess(state: String) {
        Log.e("yctdata_state", state)

    }

    override fun getYctToWdSuccess(info: String) {
        Log.e("yctdata_upload_clear", info)
    }

    override fun getCargoLaneText(s: String?) {
        /*val index : Int = InterceptString.interceptDigital(s.toString()).toInt()
        if (index > 0 && index <= list!!.size) {
            showPayWindow(index-1)
            mPresenter?.requestCode(Constant.CODE_THREE,Constant.PAY_ZF,Constant.MAC_ID,Constant.BOX)
        }else{
            toastShow(InterceptString.getNumber(list!!.size))
        }*/
    }

    private val handler = @SuppressLint("HandlerLeak")
    object : Handler() {
        override fun handleMessage(msg: Message?) {
            super.handleMessage(msg)
            when (msg!!.what) {
                0 -> {
                    //测试所有电机，打开所有柜门
                    SerialPortUtil.getInstance().wd_out_goods_test(list!![all_index].motonum, 2)
                }
                1 -> {
                    if (paypopu != null) paypopu!!.tv_buy_countdown.text = Constant.GAME_PAY_CD.toString() + "秒"
                    if (
                            index == 2 &&
                            Constant.IS_QUERY) {
                        if (home_type == 0) {
                            mPresenter?.queryPayResults(Constant.CODE_FOUR, Constant.PAY_ZF, Constant.ORDER_NUMBER)
                        } else {
                            mPresenter?.queryPayResultsCZ(Constant.CODE_FOUR, Constant.PAY_ZF, Constant.ORDER_NUMBER)
                        }
                        index = 0
                    }
                }
                10 -> {
                    //15秒无操作回到售卖界面
                    FlyingChargeRelate()
                }

                2 -> {  /*toastShow(resources.getString(R.string.sp_success_ts))
                        playAudio(R.raw.sp_success,App.spUtil!!.getBoolean(Constant.IS_VOICE,false),my_video_download.sp_adv_video)*/
                    if (pay_type == 1) {
                        //正常二维码购买
                        spStatus(R.string.shipment_success, R.raw.sp_complete, Constant.ONE)
                        ShipmentsDialog.getInstance(this@DrinkMacActivity).shipmentSuccess(0)
                        IS_COIN = false
                    } else if (pay_type == 2) {
                        //羊城通购买 ， 显示放到B1提交成功那里进行

                    }
                    TextLog.writeTxtToFile("接收到出货成功指令：订单号：" + Constant.ORDER_NUMBER, Constant.filePath, Constant.fileName)

                }

                3 -> {
                    spStatus(R.string.shipment_success, R.raw.sp_complete, Constant.ONE)
                    ShipmentsDialog.getInstance(this@DrinkMacActivity).shipmentSuccess(0)
                }

                4 -> {
                    if (pay_type == 1) {
                        //正常二维码购买，失败
                        spStatus(R.string.shipment_fail, R.raw.sp_fail, Constant.TOW)
                        ShipmentsDialog.getInstance(this@DrinkMacActivity).shipmentFail()
                        if (IS_COIN) {
                            SerialPortUtil_Money.getInstance().coin_return(spMoney.toFloat())
                        }

                        IS_COIN = false
                    } else if (pay_type == 2) {
                        //羊城通购买 成功，出货失败， 显示放到B1提交成功那里进行
                    }
//                    var box_err = Constant.BOX
//                    Constant.BOX_ERR += box_err+"@"
//                    num_err++
//                    Log.e("err_box",Constant.BOX_ERR + " == "+num_err)
//                    TextLog.writeTxtToFile("接收到出货失败指令：订单号：" + Constant.ORDER_NUMBER, Constant.filePath, Constant.fileName)
//                    if(num_err>2){
//                        showFaultDialog()
//                    }
                }

                5 -> {
                    spStatus(R.string.sj_gz_text, R.raw.sj_fault, Constant.TOW)
                    ShipmentsDialog.getInstance(this@DrinkMacActivity).shipmentFail()
                    showFaultDialog()
                }

                6 -> {
                    spStatus(R.string.dj_gz_text, R.raw.dj_fault, Constant.TOW)
                    TextLog.writeTxtToFile("接收到电机指令：订单号：" + Constant.ORDER_NUMBER, Constant.filePath, Constant.fileName)
                    ShipmentsDialog.getInstance(this@DrinkMacActivity).shipmentFail()
//                    var box_err = Constant.BOX
//                    Constant.BOX_ERR += box_err+"@"
//                    num_err++
//                    Log.e("err_box_dj",Constant.BOX_ERR + " == "+num_err)
//                    TextLog.writeTxtToFile("接收到出货失败指令：订单号：" + Constant.ORDER_NUMBER, Constant.filePath, Constant.fileName)
//                    if(num_err>2){
//                        showFaultDialog()
//                    }
                }

                7 -> {
                    toastShow(resources.getString(R.string.qh_cs_text))
                    playAudio(R.raw.qh_timeout, App.spUtil!!.getBoolean(Constant.IS_VOICE, false), my_video_download.sp_adv_video)
                    ShipmentsDialog.getInstance(this@DrinkMacActivity).shipmentFail()
                }
                8 -> mPresenter?.getTimingRequest(Constant.MAC_ID)
                9 -> {
                    if (Constant.IS_PAY) {
                        investMoney(moneyIndex)
                    } else {
                        SerialPortUtil_Money.getInstance().coin_return(moneyIndex.toFloat())
                    }
                }
                18 -> {
                    correctPassword()
                }
                101 -> {
                    toastShow("初始化成功")
                    CarSerialPortUtil.getInstance().test_sign_in_1()
                    Log.e("1042_shid_101", App.spUtil!!.getString(Constant.CAR_LOGIN_INFO, ""))
                    ship_info = App.spUtil!!.getString(Constant.CAR_LOGIN_INFO, "")
                }
                1011 -> {
                    toastShow("初始化失败")
                }
                +
                102 -> {
//                    toastShow("签名1成功")
                }
                1021 -> {
//                    toastShow("签名1失败")
                }
                104 -> {
                    toastShow("签名2成功")
                }
                1041 -> {
                    toastShow("签名2失败")
                }
                1042 -> {
                    //获取签到2-》FE 03的基础数据
//                    toastShow("签名2存储成功")
                    Log.e("1042", App.spUtil!!.getString(Constant.CAR_SIGN_INFO, ""))
                }
                //获取当前监控状态 正常去查卡，异常
                106 -> {
                    toastShow("状态正常")
                    CarSerialPortUtil.getInstance().test_card_search()
                }
                1061 -> {
                    toastShow("异常状态")
                }
                //寻卡状态  成功开始读卡
                107 -> {
                    toastShow("寻卡正常")
                    card_timer!!.cancel()
                    CarSerialPortUtil.getInstance().test_card_read()
                }
                1071 -> {
//                    toastShow("寻卡失败")
                }
                //读卡
                108 -> {
//                    toastShow("读卡正常")
                    //下一步，查询交易类型  Socket

                }
                1081 -> {
                    toastShow("读卡异常")
                }
                //消费相关
                110 -> {
//                    toastShow("消费读卡正常")
                    if (card_timer != null) {
                        card_timer!!.cancel()
                    }
                }
                1101 -> {
                    toastShow("请放入公交卡")
                }
                1102 -> {
                    //寻卡成功之后，获取数据，显示余额
                    //home_txt_ye.setText((pay_search_money.toDouble()/100).toString()+" 元")
                    home_txt_ye.setText(pay_search_money);
                }
                1103 -> {
                    //羊城通卡插入成功 显示逻辑卡号
                    LogicalCardNumber.setText("逻辑卡号:" + str_g_LNO)
                }
                111 -> {
//                    toastShow("支付成功，正在出货")
//                    SerialPortUtil.getInstance().wd_out_goods(Constant.ELECTRIC)
                    sendShipments(Constant.ELECTRIC)
                }
                1111 -> {
                    toastShow("请放入公交卡")
                }
                105 -> {
                    //清空弹框
//                    closeStatePop()

                    if (imgList_pop == null) {
                        mPresenter?.getBanner(Constant.MAC_ID)
                    } else {
                        showPopBGDialog()
                    }
                }
                201 -> {
                    toastShow("控制板正常")
                }
                202 -> {
                    toastShow("打开柜门")
                    TextLog.writeTxtToFile("接收到出货成功指令：订单号：" + Constant.ORDER_NUMBER, Constant.filePath, Constant.fileName)
                    spStatus(R.string.shipment_success, R.raw.sp_complete, Constant.ONE)
                    ShipmentsDialog.getInstance(this@DrinkMacActivity).shipmentSuccess(0)
                    IS_COIN = false
                }
                2021 -> {
                    toastShow("柜门打开失败")
                    spStatus(R.string.shipment_fail, R.raw.sp_fail, Constant.TOW)
                    ShipmentsDialog.getInstance(this@DrinkMacActivity).shipmentFail()
                    if (IS_COIN) {
                        SerialPortUtil_Money.getInstance().coin_return(spMoney.toFloat())
                    }
                    IS_COIN = false
                }
                203 -> {

//                    if (all_index < list!!.size) {
//                        SerialPortUtil.getInstance().wd_out_goods_test(list!![all_index].motonum, 2)
//                    }
                    all_index++
                    if (all_index < 20) {
//                        SerialPortUtil.getInstance().wd_out_goods_test(all_index, 2)
//                        SerialPortUtil.getInstance().wd_out_goods_test(all_index, 1)
                    } else {
                        all_index = 1
                    }
//                    toastShow("补货打开柜门"+all_index + "  == " + list!!.size)
                    Log.e("all_index", "补货打开柜门" + all_index + "  == " + list!!.size)
                }
                2031 -> {
                    toastShow("当前柜门打开（反馈）失败")
                    all_index++
                    if (all_index < 20) {
//                        SerialPortUtil.getInstance().wd_out_goods_test(all_index, 2)
                    } else {
                        all_index = 1
                    }
                    Log.e("all_index", "失败补货打开柜门" + all_index + "  == " + list!!.size)
                }
                2032 -> {
                    toastShow("当前柜门已打开")
                }
                204 -> {
                    toastShow("系统将在30s后关闭")
                    //            //联机消费快速结算
                    client2!!.getSendSocketInfo(null, 114, "123")
                    SerialPortUtil.getInstance().wd_is_close(0)
                }
                2041 -> {
                    toastShow("系统停止关闭")
                    SerialPortUtil.getInstance().wd_is_close(1)
                }
                1121 -> {
                    if (msg_err.contains("充值成功")) {
                        FlyingChargeRelate()
                    }
                    //读卡异常提示
                    cancelBuyTime()
                    toastShow(msg_err)

//                    Handler().postDelayed({  CarSerialPortUtil.getInstance().test_card_search() }, 3000)
                }
                113 -> {
                    //读卡应用信息
                    toastShow("读卡应用信息成功!")
                }
                1131 -> {
                    toastShow("读卡应用信息失败!")
                }
                300 -> {
                    //弹出充值框
                    closeLoading()
                    showPayWindow(list!!.size - 1, 1)
                    mPresenter?.requestYCTCodeCZ(Constant.MAC_ID, Constant.G_ID, PAY_ZF,
                            card_num.replace(" ", "").trim(), "0.01")
                }
                400 -> {
                    //弹出充值框
                    closeLoading()
                    showPayWindow(list!!.size - 1, 1)
                    mPresenter?.requestYCTCodeCZ(Constant.MAC_ID, Constant.G_ID, Constant.PAY_ZF, card_num.replace(" ", "").trim(), "0.01")
                }
                3001 -> {
                    ViewEventNotifier.sendMessage(3001, "充值失败");
                    toastShow("充值失败!")
                }
                3011 -> {
                    ViewEventNotifier.sendMessage(3011, "卡片圈存初始化失败");
                    toastShow("卡片圈存初始化失败!")

                }
                404 -> {
                    CarSerialPortUtil.getInstance().R_PUB_GETVERSION()
                    Log.e("404", "获取监控信息")
                }
                22 -> {
                    //羊城通提交成功，停止显示
                    spStatus(R.string.shipment_success, R.raw.sp_complete, Constant.ONE)
                    ShipmentsDialog.getInstance(this@DrinkMacActivity).shipmentSuccess(0)
                    IS_COIN = false
                }
                44 -> {
                    //羊城通购买，支付成功，出货失败
                    spStatus(R.string.shipment_fail, R.raw.sp_fail, Constant.TOW)
                    ShipmentsDialog.getInstance(this@DrinkMacActivity).shipmentFail()
                    if (IS_COIN) {
                        SerialPortUtil_Money.getInstance().coin_return(spMoney.toFloat())
                    }

                    IS_COIN = false
                }
            }
        }
    }

    private fun spStatus(msg: Int, audio: Int, state: String) {
        toastShow(resources.getString(msg))
        playAudio(audio, App.spUtil!!.getBoolean(Constant.IS_VOICE, false), my_video_download.sp_adv_video)
        if (home_type == 0) {
            if (pay_type == 1) {
                //二维码支付
                mPresenter?.uploadShipment(pay_type.toString(), state, Constant.ORDER_NUMBER)
            } else {
                //羊城通支付
                mPresenter?.uploadYCT(Constant.MAC_ID, Constant.ELECTRIC.toString(), state)
            }
//            mPresenter?.getuploadYCTData(Constant.MAC_ID,Constant.ORDER_NUMBER,info)
        } else if (home_type == 1) {
            //羊城通充值上传结果
            mPresenter?.uploadYCT_CZ(Constant.MAC_ID, state)
        }

    }

    override fun onDataReceive(cmd: Int, data: Int) {
        when (cmd) {
            1 -> handler.sendEmptyMessage(2)
            2 -> handler.sendEmptyMessage(3)
            3 -> handler.sendEmptyMessage(4)
            4 -> handler.sendEmptyMessage(5)
            5 -> handler.sendEmptyMessage(6)
            6 -> handler.sendEmptyMessage(7)
            18 -> handler.sendEmptyMessage(18)
            201 -> handler.sendEmptyMessage(201)
            202 -> handler.sendEmptyMessage(202)
            203 -> {
                if (data == 1) {
                    //单个开
                    handler.sendEmptyMessage(2032)
                } else {
                    handler.sendEmptyMessage(203)
                }
            }
            2031 -> handler.sendEmptyMessage(2031)
            204 -> handler.sendEmptyMessage(204)
            2041 -> handler.sendEmptyMessage(2041)
        }
    }

    private var msg_err: String = ""
    private var pay_search_money: String = ""
    private var str_g_LNO: String = ""

    //公交卡
    override fun onDataReceiveCar(cmd: Int, data: String) {
        when (cmd) {
            101 -> {
                handler.sendEmptyMessage(101)
                App.spUtil!!.putString(Constant.CAR_LOGIN_INFO, data)
                Log.e("CAR_LOGIN_INFO", data)
            }
            1011 -> handler.sendEmptyMessage(1011)
            1102 -> {
                handler.sendEmptyMessage(1102)
                pay_search_money = data
            }
            1103 -> {
                //显示逻辑卡号
                str_g_LNO = data
                handler.sendEmptyMessage(1103)
            }
            102 -> {
                handler.sendEmptyMessage(102)
//                Log.e("socket_drink", data)
//                client2!!.getSendSocketInfo(data)
            }
            1021 -> handler.sendEmptyMessage(1021)
//            103 -> handler.sendEmptyMessage(103)
//            104 -> handler.sendEmptyMessage(104)
            106 -> {
                handler.sendEmptyMessage(106)
            }
            1061 -> handler.sendEmptyMessage(1061)
            107 -> {
                handler.sendEmptyMessage(107)
            }
            1071 -> handler.sendEmptyMessage(1071)
            1042 -> {
                handler.sendEmptyMessage(1042)
                App.spUtil!!.putString(Constant.CAR_SIGN_INFO, data)
            }
            1121 -> {
                //读卡异常 提示
                msg_err = data
                handler.sendEmptyMessage(1121)
            }
            404 -> {
                handler.sendEmptyMessage(404)
            }
        }
    }

    private var cz_cmd: ByteArray? = null
    override fun onDataReceiveCar(state: Int, cmd: ByteArray?) {
        when (state) {
            //签到1数据DE1，获取签到2数据DE2
            103 -> {
                handler.sendEmptyMessage(103)
                Log.e("103_cmd", cmd!!.toString())
//                client2!!.getSendSocketInfo(cmd, 103, "")
                client_add!!.getSendSocketInfo(cmd, 103, "", (cz_money).toString())
            }
            104 -> {
                handler.sendEmptyMessage(104)
//                client2!!.getSendSocketInfo(cmd, 104, "")
                client_add!!.getSendSocketInfo(cmd, 104, "", (cz_money).toString())
            }
            108 -> {
                cz_cmd = cmd
                ///充值 读卡成功，查询交易内容
                handler.sendEmptyMessage(108)
//                client2!!.getSendSocketInfo(cmd, 108, ship_info)
                client_add!!.getSendSocketInfo(cmd, 108, ship_info, (cz_money).toString())
            }
            1081 -> handler.sendEmptyMessage(1081)
            1082 -> {
                Log.e("drink_card", bytesToHexString(cmd))
                card_num = bytesToHexString(cmd)
            }
            109 -> {
                ///消费签到1，发送数据获取2数据
                handler.sendEmptyMessage(109)
                client2!!.getSendSocketInfo(cmd, 109, ship_info)
            }
            110 -> {
                ///寻卡成功之后，获取数据，进行联机授权B0
                handler.sendEmptyMessage(110)
                client2!!.getSendSocketInfo(cmd, 110, car_spMoney.toString())
            }
//            1102 ->{
//                //寻卡成功之后，获取数据，显示余额
//                handler.sendEmptyMessage(1102)
//            }
            1101 -> {
                //寻卡失败
                handler.sendEmptyMessage(1101)
            }
            111 -> {
                //扣费成功
                handler.sendEmptyMessage(111)
//                client2 = SocketClient2(Constant.SOCKET_URL, Constant.SOCKET_ID, this, "")
                client2!!.getSendSocketInfo(cmd, 111, ship_info)
                Log.e("111_info", cmd!!.size.toString())
            }
            1111 -> {
                //扣费失败
                handler.sendEmptyMessage(1111)
            }
            113 -> {
                //充值 94 查询卡应用信息，用于获取卡号，SAK ,卡应用信息  传递查询交易类型信息
                handler.sendEmptyMessage(113)
                client2!!.getSendSocketInfo(cmd, 113, ship_info)
            }
            1131 -> {
                handler.sendEmptyMessage(1131)
            }
            300 -> {
                //充值获取cpu信息成功，开始下一步
                handler.sendEmptyMessage(300)
//                client_add!!.getSendSocketInfo(cmd, 300, "0", (cz_money).toString())
                cz_cmd = cmd
                home_cz_type = 1
            }
            3001 -> {
                //充值信息获取失败，提示充值失败
                handler.sendEmptyMessage(3001)
            }
            301 -> {
                //充值获取cpu信息成功，开始下一步
                handler.sendEmptyMessage(301)
                client_add!!.getSendSocketInfo(cmd, 301, "0", (cz_money).toString())
            }
            3011 -> {
                //卡片INIT失败失败，提示充值失败
                handler.sendEmptyMessage(3011)
            }
            302 -> {
                //充值获取cpu信息成功，开始下一步
                handler.sendEmptyMessage(302)
                client_add!!.getSendSocketInfo(cmd, 302, "0", (cz_money).toString())
            }
            303 -> {
                //R_CPU_LOAD (CPU钱包圈存)
                //用于传递数据，把返回数据传递到  AA85中上传
                handler.sendEmptyMessage(303)
                client_add!!.getSendSocketInfo(cmd, 303, "0", (cz_money).toString())
            }
            400 -> {
                ///充值 读卡成功，查询交易内容 ,充值开始 ，第二次读卡
                Log.e("TAG", "读卡成功====400")
                handler.sendEmptyMessage(400)
//                client_add!!.getSendSocketInfo(cmd, 400, ship_info, (cz_money).toString())
                cz_cmd = cmd
                home_cz_type = 0
            }
            401 -> {
                ///R_M1_LOAD
                handler.sendEmptyMessage(401)
                client_add!!.getSendSocketInfo(cmd, 401, ship_info, (cz_money).toString())
            }
            403 -> {
                //再次获取M1卡数据，开始P_M1_ROLLBACK
                handler.sendEmptyMessage(403)
                client_add!!.getSendSocketInfo(cmd, 403, ship_info, (cz_money).toString())
            }
            405 -> {
                handler.sendEmptyMessage(405)
                client_add!!.getSendSocketInfo(cmd, 405, ship_info, (cz_money).toString())
            }

        }

    }

    private fun showFaultDialog() {
        if (faultDialog == null) {
            faultDialog = FaultDialog(this)
            faultDialog?.show()
            faultDialog!!.window.clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
        } else {
            faultDialog?.show()
            faultDialog!!.window.clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
        }
    }

    override fun onDataToast(str: String?) {}
    private fun showPayWindow(position: Int, state: Int) {
        Log.e("TAG", "showPayWindow=" + "position:" + position + "  state:" + state)
        Constant.IS_PAY = true
        IS_COIN = false
        Constant.BOX = list!![position].box
        Constant.G_ID = list!![position].id.toString()
        Constant.ELECTRIC = list!![position].motonum
        spMoney = list!![position].price.toDouble()
        car_spMoney = ((list!![position].price.toDouble()) * 100).toInt()
        if (paypopu == null) {
            paypopu = PurchasePopuView(this, position, state, home_type)
            paypopu!!.OnPopuItmOnclick(this)
            paypopu!!.showAtLocation(this.window.decorView, Gravity.BOTTOM, 0, 0)
            startBuyTime()
        }
    }

    private fun dismissPayWindow() {
        if (paypopu != null && paypopu!!.isShowing) {
            paypopu!!.dismiss()
            paypopu = null
        }
//        cancelHome()
    }

    //飞充 传参余额
    fun setFlyingCharge(mMoney: Int) {
        AlertDialogUtils.getAlertDialogUtils().DissDialog("飞充 传参余额")
        FlyingChargeActivity.getFlyingChargeActivity(mMoney, str_g_LNO, home_cz_type, client_add!!, cz_cmd!!, ship_info)
    }

    fun FlyingChargeRelate() {
        home_ll_cz.visibility = View.GONE
        my_sid_view.visibility = View.VISIBLE
        home_txt_ye.setText("")
        LogicalCardNumber.setText("")
    }

    private fun startBuyTime() {
        if (buyTimer == null) {
            buyTimer = object : CountDownTimer((60000 + 150).toLong(), 1000) {
                override fun onTick(l: Long) {
                    GAME_PAY_CD = l.toInt() / 1000 - 1
                    if (IS_QUERY) index++
                    handler.sendEmptyMessage(1)
                }

                override fun onFinish() {
                    cancelBuyTime()
                }
            }.start()
        }
    }

    private fun cancelBuyTime() {
        if (buyTimer != null) {
            buyTimer!!.cancel()
            buyTimer = null
        }
        if (card_timer != null) {
            card_timer!!.cancel()
            card_timer = null
        }

        dismissPayWindow()
        IS_QUERY = false
        IS_PAY = false
        PAY_ZF = "wx"
//        mPresenter?.cancelOrder(Constant.ORDER_NUMBER)
    }

    private fun sendShipments(box: Int) {
        cancelBuyTime()
        if (home_type == 0) {
            //购买操作
            //toastShow(resources.getString(R.string.pay_success))
            CarSerialPortUtil.getInstance().ConsumeSuccessShow();//2020-9-5
            playAudio(R.raw.pay_success, App.spUtil!!.getBoolean(Constant.IS_VOICE, false), my_video_download.sp_adv_video)
            SerialPortUtil.getInstance().wd_out_goods(Constant.ELECTRIC)
            ShipmentsDialog.getInstance(this).showSpDialog(0)
            ShipmentsDialog.getInstance(this).ShippingCountdownCallback(this)
        } else if (home_type == 1) {
            //充值操作
            toastShow(resources.getString(R.string.cz_pay_success))
            playAudio(R.raw.pay_success, App.spUtil!!.getBoolean(Constant.IS_VOICE, false), my_video_download.sp_adv_video)
            //ShipmentsDialog.getInstance(this).showSpDialog(1)//屏蔽2020.10.1
            //ShipmentsDialog.getInstance(this).ShippingCountdownCallback(this)//屏蔽2020.10.1
            //0 M1充值  1 CPU充值
            Log.e("TAG", "home_cz_type == 0=========" + System.currentTimeMillis())
            toastShow("正在读卡,请勿移动卡片...")
            if (home_cz_type == 0) {
                client_add!!.getSendSocketInfo(cz_cmd, 400, ship_info, (cz_money).toString())
            } else if (home_cz_type == 1) {
                client_add!!.getSendSocketInfo(cz_cmd, 300, "0", (cz_money).toString())
//
            }
        }
    }

    override fun getScavengingText(s: String?) {
        if (App.spUtil!!.getBoolean(Constant.IS_BSC, false)) {
            if (Constant.IS_PAY) {
                mPresenter?.scavengingCode(s.toString(), Constant.BOX, Constant.MAC_ID)
            } else {
                toastShow(resources.getString(R.string.please_goods))
            }
        } else {
            toastShow(resources.getString(R.string.bsc_text))
        }
    }

    override fun requestCodeSuccess(result: RequestCodeBean) {
        if (Constant.PAY_ZF == "wx") {
            paypopu!!.iv_pay_code.setImageBitmap(ZxingUtil.createQRImage(result.url, 400,
                    BitmapFactory.decodeResource(resources, R.drawable.pay_wechat_no)))
        } else {
            paypopu!!.iv_pay_code.setImageBitmap(ZxingUtil.createQRImage(result.url, 400,
                    BitmapFactory.decodeResource(resources, R.drawable.pay_alipay_no)))
        }
        Constant.ORDER_NUMBER = result.out_trade_no
        Constant.IS_QUERY = true
    }

    override fun requestCodeSuccessCZ(result: CzRequestCodeBean) {
        if (Constant.PAY_ZF == "wx") {
            paypopu!!.iv_pay_code.setImageBitmap(ZxingUtil.createQRImage(result.qrcode_url, 400,
                    BitmapFactory.decodeResource(resources, R.drawable.pay_wechat_no)))
        } else {
            paypopu!!.iv_pay_code.setImageBitmap(ZxingUtil.createQRImage(result.qrcode_url, 400,
                    BitmapFactory.decodeResource(resources, R.drawable.pay_alipay_no)))
        }
        Constant.ORDER_NUMBER = result.trade_no
        Constant.IS_QUERY = true
    }

    override fun requestCodeFail(errMsg: String) {
        toastShow(errMsg)
        cancelBuyTime()
    }

    override fun queryPayResultsSuccess() {
        if (moneyTotal > 0) {
            SerialPortUtil_Money.getInstance().coin_return(moneyTotal.toFloat())
            moneyTotal = 0.0
        }
        sendShipments(Constant.ELECTRIC)
//        SerialPortUtil.getInstance().wd_out_goods(Constant.ELECTRIC)
    }

    override fun scavengingCodeSuccess() {
        sendShipments(Constant.ELECTRIC)
//        SerialPortUtil.getInstance().wd_out_goods(Constant.ELECTRIC)
    }

    override fun scavengingCodeFail(errMsg: String) {
        toastShow(errMsg)
    }

    override fun getYctDataSuccess(info: YctUpLoadBean) {
        //获取本地服务器数据返回数据
        Log.e("get_yct_115", info.total_count + " === " + info.total_money + " === " + info.message.size)
        //脱机消费结算 A5开始  A8结束
        client2!!.getSendSocketInfo(null, 115, info.total_money)
    }

    override fun advVideoSuccess(list: List<AdvVideoBean.MessageBean>, IS_Update: Boolean) {
        val nameList: ArrayList<String> = ArrayList()
        val urlList: ArrayList<String> = ArrayList()
        val videoList = App.videoDao?.queryForAll()
        for (i in videoList!!.indices) {
            nameList.add(InterceptString.VideoName(list[i].v_url))
            urlList.add(HttpUrl.SERVER_URL + list[i].v_url)
        }
        my_video_download.layout_xz_video.visibility = View.VISIBLE
        if (IS_Update) {
            my_video_download.layout_xz_video.setBackgroundResource(R.color.transparent)
        }
        my_video_download.initData(urlList, nameList)
    }

    override fun advVideoFail(errMsg: String) {
        toastShow(errMsg)
    }

    override fun uploadShipmentSuccess(errMsg: String) {
        mPresenter?.getData(Constant.CODE_ONE, Constant.MAC_ID, true)
    }

    private fun upDate() {
        list = App.spDao!!.queryForAll()
        my_sid_view.intiData(list)
        mPresenter?.getAdvVideo("107", Constant.MAC_ID, true)
    }

    override fun onStop() {
        super.onStop()
        my_video_download.sp_adv_video.pause()
    }

    override fun onResume() {
        super.onResume()
        my_video_download.sp_adv_video.start()
        SerialPortUtil.getInstance().setOnDataReceiveListener(this)
//        num_err = 0
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 0 && resultCode == RESULT_OK) {
            val msg = data!!.getIntExtra(Constant.RETURN_KEY, 0)
            when (msg) {
                1 -> {
                    finish()
                    System.exit(0)
                }
                2 -> upDate()
            }
        }
    }

    override fun onNetwork(state: Boolean) {
        super.onNetwork(state)
        TextLog.writeTxtToFile("获取网络:" + state, Constant.filePath, Constant.fileName)
        iv_network_img.setImageResource(R.mipmap.network_yes)
        if (state) {
            tv_wl.text = resources.getString(R.string.network_wifi)
            toastShow(resources.getString(R.string.network_wifi))
            if (is_send == 0) {
                //启动自动发送  socket 通知后台计算前面数据
                client_wd!!.b4_wd_send()
            }
            is_send = 1
        } else {
            tv_wl.text = resources.getString(R.string.no_network_move)
            toastShow("2G/3G/4G网络")
            if (is_send == 0) {
                //启动自动发送  socket 通知后台计算前面数据
                client_wd!!.b4_wd_send()
            }
            is_send = 1
        }
        if (imgList_pop == null) {
            mPresenter?.getBanner(Constant.MAC_ID)
        }
    }

    override fun onNoNetwork() {
        super.onNoNetwork()
        TextLog.writeTxtToFile("网络断开", Constant.filePath, Constant.fileName)
        iv_network_img.setImageResource(R.mipmap.network_no)
        tv_wl.text = resources.getString(R.string.no_network_top)
        toastShow(resources.getString(R.string.no_network))
    }

    override fun shippingCountdown() {
        if (pay_type == 1) {
            //二维码支付
//            toastShow("串口通讯断开,正在退款!")
            mPresenter?.uploadShipment(pay_type.toString(), Constant.TOW, Constant.ORDER_NUMBER)
            TextLog.writeTxtToFile("未接收到出货反馈指令，串口通讯断开", Constant.filePath, Constant.fileName)
        } else {
            //羊城通支付
        }
    }

    private fun showPassword() {
        if (passwordPopu == null) {
            passwordPopu = PasswordPopu(this)
            passwordPopu!!.showAtLocation(this.window.decorView, Gravity.CENTER, 0, 0)
            passwordPopu!!.correctPasswordCallback(this)
        } else {
            passwordPopu!!.showAtLocation(this.window.decorView, Gravity.CENTER, 0, 0)
            yhidekeyboard()
        }
    }

    override fun correctPassword() {
        startActivityForResult(Intent(this, ReplenishActivity::class.java), 0)
    }

    private fun starOnLine() {
        if (timer == null) {
            timer = Timer()
            timer!!.schedule(object : TimerTask() {
                override fun run() {
                    handler.sendEmptyMessage(8)
                }
            }, 10000, 60000 * 2)
        }
    }

    private fun cancelOnLine() {
        if (timer != null) {
            timer!!.cancel()
            timer = null
        }
    }

    override fun timingRequestSuccess() {}
    override fun cancelOrderCode() {}

    /**
     * 2分钟自动开启屏保
     */
    private fun cancelHome() {
        try {
            handler.removeMessages(105)
        } catch (e: Exception) {
        }

        handler.sendMessageDelayed(handler.obtainMessage(105), 120000)
    }

    //统一屏保
    private var inflate: View? = null
    private var dialog: Dialog? = null
    private var dialog_img_check: ImageView? = null
    private var pop_banner: Banner? = null

    private fun showPopBGDialog() {
        dialog = Dialog(this@DrinkMacActivity, R.style.my_dialog_style)
        inflate = LayoutInflater.from(this@DrinkMacActivity).inflate(R.layout.pop_home, null)
        dialog!!.setContentView(inflate!!)
        val dialogWindow = dialog!!.window
        dialogWindow!!.setGravity(Gravity.CENTER)
        val lp = dialogWindow.attributes
        lp.width = windowManager.defaultDisplay.width
        lp.height = windowManager.defaultDisplay.height
        Log.e("ttttttt", "  " + lp.width + "  " + lp.height)
        dialog_img_check = dialog!!.findViewById(R.id.dialog_img_check)
        pop_banner = dialog!!.findViewById(R.id.pop_banner)
        dialog_img_check!!.setOnClickListener {
            //            home_edit_info.setFocusable(true)
//            cancelHome()
            dialog!!.dismiss()
        }
        if (imgList_pop != null) {
            pop_banner!!.setBannerStyle(BannerConfig.NOT_INDICATOR).setDelayTime(5000).setImages(imgList_pop)
                    .setImageLoader(GlideImageLoader()).setBannerAnimation(Transformer.Accordion).start()
            pop_banner!!.setOnClickListener {
                //                cancelHome()
                dialog!!.dismiss()
            }
        }

        dialogWindow.attributes = lp
        dialog!!.setCanceledOnTouchOutside(true)
        // 将对话框的大小按屏幕大小的百分比设置
        dialog!!.window!!.attributes = lp
        try {
            dialog!!.show()
        } catch (E: Exception) {
        }
    }

    var all_index: Int = 1

    //打开所有柜门
    public fun test_all() {
        all_index = 1
//        SerialPortUtil.getInstance().wd_out_goods_test(list!![all_index].motonum,2)
        Log.e("klog", "1000")
//        SerialPortUtil.getInstance().wd_out_goods_test(all_index, 2)
        SerialPortUtil.getInstance().wd_out_goods_test(all_index, 1)
    }

    public fun update() {
        mPresenter?.getData(Constant.CODE_ONE, Constant.MAC_ID, true)
        mPresenter?.getBanner(Constant.MAC_ID)
        if (dialog != null) {
            if (dialog!!.isShowing) {
                dialog!!.dismiss()
            }
        }
    }

    override fun goodsInfoSuccess(data: List<GoodsInfoBean.MessageBean>, IsUpdate: Boolean) {
//        toastShow("数据开始更新")
        TextLog.initData(Constant.MAC_ID)
        list = App.spDao!!.queryForAll()
        Constant.MAC_ID = App.spUtil!!.getString(Constant.MAC_ID_KEY, "")
        tv_mac_id_text.text = Constant.MAC_ID_TOP + Constant.MAC_ID
        my_sid_view.intiData(list)
        my_scavenging_ed.requestFocus()
        my_sid_view.SlideViewItmOnclick(this)
        //my_computer_view.buttonConfirmOnclick(this)
        my_scavenging_ed.ScavengingTextCallBack(this)
//        initVideo()

    }

    private var imgList: java.util.ArrayList<String>? = null
    private var imgList_pop: java.util.ArrayList<String>? = null
    override fun getBannerSuccess(messageBean: HomeBannerBean.MessageBean) {
        try {
            imgList = java.util.ArrayList()
            for (i in messageBean!!.main.indices) {
                imgList?.add(messageBean!!.main[i].pic_url)
            }
            home_banner.setBannerStyle(BannerConfig.NOT_INDICATOR).setDelayTime(5000).setImages(imgList)
                    .setImageLoader(GlideImageLoader()).setBannerAnimation(Transformer.Accordion).start()
        } catch (e: Exception) {
            Log.e("photo_333", e.message.toString())
        }
        try {
            imgList_pop = java.util.ArrayList()
            for (i in messageBean!!.banner.indices) {
                imgList_pop?.add(messageBean!!.banner[i].pic_url)
            }
        } catch (e: Exception) {
            Log.e("photo_333", e.message.toString())
        }
    }

    public fun showInfo(info: String, state: Int) {
        //弹出信息 info  ,状态码 用于其他读卡操作
        if (state == 0) {
            //读卡成功，允许消费，开始消费预处理
//            toastShow(info)
            Log.e("money_car", car_spMoney.toString())
            //CarSerialPortUtil.getInstance().pay_test_buy_1(car_spMoney)
            CarSerialPortUtil.getInstance().yct_pay_test_buy(car_spMoney)
        } else if (state == 3) {
            //此卡片未注册 ,此卡片需要重新激活
            toastShow(info)

        } else if (state == 1) {
            //黑名单捕获
            CarSerialPortUtil.getInstance().Set_Card_Namelist()
            toastShow(info)
        } else if (state == 2) {
            //禁止消费
            toastShow(info)
        }
        //如果返回码不正确，停止交易
        if (state != 0) {
            cancelBuyTime()
        }
    }

    fun showUpInfo(info: String, state: Int) {
        //弹出信息 info  ,状态码 用于其他读卡操作
        if (state == 0) {
            //提  交成功
            toastShow(info)
//            client2!!.getSendSocketInfo(null, 114, "123")
        } else if (state == 1) {
            //记录冲正
            toastShow(info)
        } else if (state == 2) {
            //联机授权错误
            toastShow(info)
        } else if (state == 3) {
            //交易金额错误
            toastShow(info)
        } else if (state == 6) {
            //充值   交易类型查询成功
            toastShow(info)
        } else if (state == 7) {
            //M1充值成功   交易类型查询成功
            toastShow(info)
            spStatus(R.string.cz_success, R.raw.sp_complete, Constant.ONE)
            ShipmentsDialog.getInstance(this@DrinkMacActivity).shipmentSuccess(1)
            CarSerialPortUtil.getInstance().pay_card_serch(1)//寻卡
        } else if (state == 8) {
            //CPU充值成功   交易类型查询成功
            toastShow(info)
            spStatus(R.string.cz_success, R.raw.sp_complete, Constant.ONE)
            ShipmentsDialog.getInstance(this@DrinkMacActivity).shipmentSuccess(1)
            CarSerialPortUtil.getInstance().pay_card_serch(1)//寻卡
        } else if (state == 9) {
            toastShow(info)
            closeLoading()
        }
    }

    private fun closeLoading() {
        try {
            if (dialog_loading != null && dialog_loading!!.isShowing) {
                dialog_loading!!.dismiss()
            }
        } catch (E: Exception) {
        }
    }

    /*
     支付方式弹窗
     */
    private var inflate_state: View? = null
    private var dialog_state: Dialog? = null
    private var pop_img_state: ImageView? = null
    private var way_img_qr: ImageView? = null
    private var way_img_yct: ImageView? = null
    private var way_img_return: ImageView? = null
    private fun showPopWayDialog(position: Int) {
        dialog_state = Dialog(this@DrinkMacActivity, R.style.my_dialog_style)
        inflate_state = LayoutInflater.from(this@DrinkMacActivity).inflate(R.layout.pop_home_way, null)
        dialog_state!!.setContentView(inflate_state!!)
        val dialogWindow = dialog_state!!.window
        dialogWindow!!.setGravity(Gravity.CENTER)
        val lp = dialogWindow.attributes
        lp.width = WindowManager.LayoutParams.MATCH_PARENT
        lp.height = Constant.SN_HEIGHT!! / 3 + 30
        way_img_qr = dialog_state!!.findViewById(R.id.way_img_qr)
        way_img_yct = dialog_state!!.findViewById(R.id.way_img_yct)
        way_img_return = dialog_state!!.findViewById(R.id.way_img_return)
        way_img_qr!!.setOnClickListener {
            pay_type = 1
            home_type = 0
            showPayWindow(position, 1)
            mPresenter?.requestCode(Constant.CODE_THREE, Constant.PAY_ZF, Constant.MAC_ID, Constant.BOX, Constant.G_ID)
//            cancelHome()
            dialog_state!!.dismiss()
        }
        way_img_yct!!.setOnClickListener {
            pay_type = 2
            home_type = 0
            showPayWindow(position, 2)
            xyy_restart()//开始循环读卡
//            mPresenter?.requestCode(Constant.CODE_THREE, Constant.PAY_ZF, Constant.MAC_ID, Constant.BOX, Constant.G_ID)
//            cancelHome()
            dialog_state!!.dismiss()
        }

        way_img_return!!.setOnClickListener {
            //            cancelHome()
//            client_add!!.getSendSocketInfo(cz_cmd, 300, "0", (cz_money).toString())  //这里测试充值
            dialog_state!!.dismiss()
        }
        dialogWindow.attributes = lp
        dialog_state!!.setCanceledOnTouchOutside(true)
        // 将对话框的大小按屏幕大小的百分比设置
        dialog_state!!.window!!.attributes = lp
        try {
            dialog_state!!.show()
        } catch (E: Exception) {
        }
    }

    private var card_timer: Timer? = null
    private var card_handler: Handler? = null

    internal fun xyy_restart() {
        card_timer = Timer()
        card_timer!!.schedule(object : TimerTask() {
            override fun run() {
                val message = Message()
                message.what = 1
                card_handler!!.sendMessage(message)
            }
            //        }, 1000, 28800000);
        }, 1000, 1000) //12小时
        card_handler = object : Handler() {
            override fun handleMessage(msg: Message) {
                when (msg.what) {
                    1 -> CarSerialPortUtil.getInstance().pay_card_serch(0)//寻卡
                }
                super.handleMessage(msg)
            }
        }
    }

    override fun uploadYCTSuccess(errMsg: String) {
        //羊城通支付成功
        mPresenter?.getData(Constant.CODE_ONE, Constant.MAC_ID, true)
    }

    fun initCZView() {
        home_btn_20.setOnClickListener {
            chanageState(0)
        }
        home_btn_50.setOnClickListener {
            chanageState(1)
        }
        home_btn_100.setOnClickListener {
            chanageState(2)
        }
        home_btn_200.setOnClickListener {
            chanageState(3)
        }

        home_btn_cancel.setOnClickListener {
            //充值撤销
        }

        home_btn_search.setOnClickListener {
            //查询余额
            CarSerialPortUtil.getInstance().pay_card_serch(1)//寻卡
//            查询余额无操作15S跳转到售货界面
            NoAction()
        }

        home_btn_cz.setOnClickListener {
            //充值
            //充值查询卡物理信息
            home_type = 1
            CarSerialPortUtil.getInstance().test_card_read()
            showPopLoadingDialog()
            //点击充值取消15秒的倒计时
            handler.removeMessages(10)
        }
    }

    //var cz_money: Int = 2000
    var cz_money: Int = 100

    fun chanageState(state: Int) {
        //有操作替换掉15秒无操作回到售卖界面
        NoAction()
        when (state) {
            0 -> {
                //充值初始数据  20
                home_btn_20.setBackgroundResource(R.drawable.re_jc_bt_huihong_bj)
                home_btn_50.setBackgroundResource(R.drawable.re_textview_bj)
                home_btn_100.setBackgroundResource(R.drawable.re_textview_bj)
                home_btn_200.setBackgroundResource(R.drawable.re_textview_bj)
                //cz_money = 2000
                //测试用100分 =  1元做测试   2020.09.02
                cz_money = 100
            }
            1 -> {
                //充值初始数据  50
                home_btn_20.setBackgroundResource(R.drawable.re_textview_bj)
                home_btn_50.setBackgroundResource(R.drawable.re_jc_bt_huihong_bj)
                home_btn_100.setBackgroundResource(R.drawable.re_textview_bj)
                home_btn_200.setBackgroundResource(R.drawable.re_textview_bj)
                cz_money = 5000
            }
            2 -> {
                //充值初始数据  100
                home_btn_20.setBackgroundResource(R.drawable.re_textview_bj)
                home_btn_50.setBackgroundResource(R.drawable.re_textview_bj)
                home_btn_100.setBackgroundResource(R.drawable.re_jc_bt_huihong_bj)
                home_btn_200.setBackgroundResource(R.drawable.re_textview_bj)
                cz_money = 10000
            }
            3 -> {
                //充值初始数据  200
                home_btn_20.setBackgroundResource(R.drawable.re_textview_bj)
                home_btn_50.setBackgroundResource(R.drawable.re_textview_bj)
                home_btn_100.setBackgroundResource(R.drawable.re_textview_bj)
                home_btn_200.setBackgroundResource(R.drawable.re_jc_bt_huihong_bj)
                cz_money = 20000
            }
        }
    }

    private fun NoAction() {
        handler.removeMessages(10)
        handler.sendEmptyMessageDelayed(10, 15000)
    }

    //获取羊城通数据，用于上传
    fun upload_cz_info(info: String, money: String) {
        Log.e("upload_cz_info", info)
//        mPresenter?.getuploadYCTData(Constant.MAC_ID, Constant.ORDER_NUMBER, info, money)
        mPresenter?.getuploadYCTData(Constant.MAC_ID, Constant.ELECTRIC.toString(), info, money)
    }

    fun upload_b4_wd(yct_data: String, money: String, count: String, key_65: String, key_19: String, key_km: String, key_mac: String) {
        client_wd!!.b4_order_send(yct_data, money, count, key_65, key_19, key_km, key_mac)
    }

    fun upload_b4_yct(yct_data: String, key: String) {
//        client2!!.SendB4SocketInfo(yct_data, 116, key)

    }

    fun upload_A5_des(yct_data: String, key: String) {
        client2!!.getSendSocketInfo(null, 119, yct_data)
//        client_wd!!.A5_order_send(yct_data)
    }

    var card_num: String = ""
    var card_info: String = ""

    //统一屏保
    private var inflate_loading: View? = null
    private var dialog_loading: Dialog? = null
    private var dialog_img_loading: ImageView? = null
    private var dialog_img_cancel: ImageView? = null

    private fun showPopLoadingDialog() {
        dialog_loading = Dialog(this@DrinkMacActivity, R.style.my_dialog_style)
        inflate_loading = LayoutInflater.from(this@DrinkMacActivity).inflate(R.layout.pop_loading, null)
        dialog_loading!!.setContentView(inflate_loading!!)
        val dialogWindow = dialog_loading!!.window
        dialogWindow!!.setGravity(Gravity.CENTER)
        dialog_img_loading = dialog_loading!!.findViewById(R.id.dialog_img_loading)
        dialog_img_cancel = dialog_loading!!.findViewById(R.id.dialog_img_cancel)
        dialog_img_cancel!!.setOnClickListener {
            NoAction()
            dialog_loading!!.dismiss()
        }
        Glide.with(this@DrinkMacActivity).load(R.mipmap.card_tip_h).asGif().diskCacheStrategy(DiskCacheStrategy.SOURCE).into(dialog_img_loading)
        val lp = dialogWindow.attributes
        lp.width = windowManager.defaultDisplay.height / 3
        lp.height = windowManager.defaultDisplay.height / 3
        dialogWindow.attributes = lp
        dialog_loading!!.setCanceledOnTouchOutside(true)
        // 将对话框的大小按屏幕大小的百分比设置
        dialog_loading!!.window!!.attributes = lp
        try {
            dialog_loading!!.show()
        } catch (E: Exception) {
        }
    }
}
