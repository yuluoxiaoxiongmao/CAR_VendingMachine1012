package com.example.vendingmachine.platform.common

import com.example.vendingmachine.App
import com.example.vendingmachine.utils.FileUtil

/**
 * HE 2018-11-01.
 */
class Constant {
    companion object {
        const val ONE: String = "1"
        const val TOW: String = "2"
        const val CODE_ZERO: String = "100"
        const val CODE_ONE: String = "101"
        const val CODE_TOW: String = "102"
        const val CODE_THREE: String = "103"
        const val CODE_FOUR: String = "104"
        const val CODE_FIVE: String = "105"
        const val MAC_ID_KEY: String = "mac_id_key"
        const val IS_LOGIN_KEY: String = "is_login_key"
        const val IS_VIDEO_KEY: String = "is_video"
        const val SET_TIME_KEY: String = "set_time_key"
        const val RETURN_KEY: String = "RETURN_KEY"
        const val MAC_ID_TOP: String = "MACID:"
        const val IS_BSC: String = "IS_BSC_KEY"
        const val MAC_TYPE_KEY: String = "MAC_TYPE_KEY"
        const val IS_VOICE: String = "IS_VOICE_KEY"
        const val ACTION_BOOT: String = "android.intent.action.BOOT_COMPLETED"
//        const val SOCKET_URL: String = "https://121.32.31.2"
//        const val SOCKET_ID_ADD: Int = 5010//充值测试端口
//        const val SOCKET_ID: Int = 6532//消费测试端口

        const val SOCKET_URL: String = "https://121.32.31.2"
        const val SOCKET_ID_ADD: Int = 5010//充值测试端口
        const val SOCKET_ID: Int = 6532//消费测试端口

        //测试机台号
//        20200514002
//        123456

//        羊城通模块2的参数如下
//        充值：
//        终端编号：880120001111
//        PKI:89007144
//
//        消费：
//        PSAM:0100999932400


//        终端编号：880120001111
//        商户编号：0880100000000023
//        KM密钥：3132333435363738

        //终端编号配置  88 01 20 00 11 10
        const val PortNumber1 = 0x88
        const val PortNumber2 = 0x01
        const val PortNumber3 = 0x20
        const val PortNumber4 = 0x00
        const val PortNumber5 = 0x11
        const val PortNumber6 = 0x10

        //商户编号配置
        const val MerchantNumber1 = 0x08
        const val MerchantNumber2 = 0x80
        const val MerchantNumber3 = 0x10
        const val MerchantNumber4 = 0x00
        const val MerchantNumber5 = 0x00
        const val MerchantNumber6 = 0x00
        const val MerchantNumber7 = 0x00
        const val MerchantNumber8 = 0x23

        // KM密钥 3132333435363738
        const val PasswordNumber1 = 0x31
        const val PasswordNumber2 = 0x32
        const val PasswordNumber3 = 0x33
        const val PasswordNumber4 = 0x34
        const val PasswordNumber5 = 0x35
        const val PasswordNumber6 = 0x36
        const val PasswordNumber7 = 0x37
        const val PasswordNumber8 = 0x38

        const val WD_SOCKET_URL: String = "http://yct.veiding.com.cn"
        const val WD_SOCKET_ID: Int = 55822//20190316 new
        val filePath: String = FileUtil.getExternalStoragePatha(App.mContext) + "MyLog"
        const val fileName: String = "log.txt"
        const val yctrbfileName: String = "yctrollback.txt" //充值冲正文件
        const val yctsubmitfileName: String = "yctcpusubmit.txt" //CPU提交数据文件
        const val yctprefileName: String = "yctpre.txt"     //消费未完整数据
        const val yctconsumefileName: String = "yctrecord.txt"//羊城通消费记录


        const val CH_TIME: Int = 30000


        var MAC_ID: String = ""
        var PAY_ZF: String = "wx"
        var BOX: String = ""
        var ORDER_NUMBER: String = ""
        var VIDEO_PATH: String = FileUtil.getExternalStoragePatha(App.mContext) + "MyVideo"
        var G_ID: String = ""

        var SN_WIDTH: Int? = 0
        var SN_HEIGHT: Int? = 0
        var GAME_PAY_CD: Int = 0
        var STOCK_TOTAL: Int = 20
        var ELECTRIC: Int = 1

        var IS_QUERY: Boolean = false
        var IS_PAY: Boolean = false

        var totalPage: Int = 0 // 商品显示的总页数
        const val mPageSize: Int = 9

        //const val mPageSize : Int  = 24
        var BOX_ERR: String = ""

        //存储签到数据
        var CAR_SIGN_INFO: String = ""

        //存储PKI数据
        var CAR_LOGIN_INFO: String = ""

        //存储8进制 K19密钥
        var CAR_SIGN_19: String = ""

        //存储8位16进制 K65密钥
        var CAR_SIGN_65: String = ""

        //自增 消费
        var CAR_ORDER_NUM: Int = 1

        //自增 +=消费金额
        var CAR_ORDER_MONEY: Int = 0

        //=================== 充值相关========================
        //存储4位 PKI管理卡号
        var PKI_ID: String = ""

        //存储读卡所有信息
        var CZ_CARD_NUM: String = ""

        //存储签到2返回的握手流水号，SK前16位 AES密钥
        var CZ_SIGN_2_INFO: String = ""
    }
}