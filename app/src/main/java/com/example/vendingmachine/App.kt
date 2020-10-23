package com.example.vendingmachine

import android.app.AlertDialog
import android.app.Application
import android.content.Intent
import android.util.Log
import com.example.vendingmachine.db.DBHelper
import com.example.vendingmachine.db.GoodsUser
import com.example.vendingmachine.db.ParameterUser
import com.example.vendingmachine.db.VideoUser
import com.example.vendingmachine.platform.common.Constant
import com.example.vendingmachine.ui.activity.LoginActivity
import com.example.vendingmachine.utils.CrashHandler
import com.example.vendingmachine.utils.SPUtil
import com.j256.ormlite.dao.RuntimeExceptionDao
import com.tencent.bugly.Bugly
import kotlin.properties.Delegates

/**
 * He Sun 2018-10-31
 */
class App : Application(){
    companion object {
        // Used to load the 'native-lib' library on application startup.
        init {
            System.loadLibrary("com_example_vendingmachine_serialport_SerialPort")
        }
        var spUtil : SPUtil? = null
        var mContext: Application by Delegates.notNull()
        var spDao : RuntimeExceptionDao<GoodsUser, Int>?  = null
        var videoDao : RuntimeExceptionDao<VideoUser, Int>?  = null
        var sjdao : RuntimeExceptionDao<ParameterUser, Int>?  = null
        var dialog: AlertDialog? = null
    }
    val APP_ID = "c0f618371c"
    override fun onCreate() {
        super.onCreate()
        //LeakCanary.install(this)
        //BlockCanary.install(this, AppBlockCanaryContext()).start()
        init()
    }

    private fun init(){
        mContext = applicationContext as Application
        getScreenWh()
        spUtil = SPUtil(mContext,"user_info")
        val helper = DBHelper(this)
        spDao = helper.getRuntimeExceptionDao(GoodsUser::class.java)
        sjdao = helper.getRuntimeExceptionDao(ParameterUser::class.java)
        videoDao = helper.getRuntimeExceptionDao(VideoUser::class.java)
        CrashHandler.getInstance().init(this)
//        restartApp()
//        Thread.setDefaultUncaughtExceptionHandler(restartHandler) // 程序崩溃时触发线程  以下用来捕获程序崩溃异常
        Bugly.init(mContext, APP_ID, true)
    }

    private fun getScreenWh() {
        val resources = this.resources
        val dm = resources.displayMetrics
        Constant.SN_WIDTH = dm.widthPixels
        Constant.SN_HEIGHT = dm.heightPixels
        Log.e("TAG", "width:" + Constant.SN_WIDTH + "  height:" + Constant.SN_HEIGHT)
    }

    // 创建服务用于捕获崩溃异常
    private val restartHandler = Thread.UncaughtExceptionHandler { thread, throwable ->
        Log.e("TAG", "程序数据出错！即将退出重启!")
        restartApp()//发生崩溃异常时,重启应用
    }

    private fun restartApp() {
        val intent = Intent(mContext, LoginActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        mContext.startActivity(intent)
        android.os.Process.killProcess(android.os.Process.myPid())
    }

}