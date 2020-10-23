package com.example.vendingmachine.presenter

import android.util.Log
import com.example.vendingmachine.App
import com.example.vendingmachine.bean.GoodsInfoBean
import com.example.vendingmachine.bean.LoginBean
import com.example.vendingmachine.bean.RequestBean
import com.example.vendingmachine.db.GoodsUser
import com.example.vendingmachine.db.ParameterUser
import com.example.vendingmachine.model.BasicModel
import com.example.vendingmachine.platform.common.Constant
import com.example.vendingmachine.platform.http.ApiManager
import com.example.vendingmachine.platform.http.Response
import com.example.vendingmachine.utils.InterceptString
import com.example.vendingmachine.utils.TextLog

/**
 * He sun 2018-10-31.
 */
class LoginPresenter<V> : BasePresenter<V, BasicModel>() {

    override fun initModel() {
        mModel = BasicModel()
    }

    fun login(code: String, mac_id: String, password: String) {
        if (mView !is ILoginView) return
        val view = mView as ILoginView

        mModel?.getNormalRequestData(ApiManager.getService().login(code, mac_id, password),
                object : Response<LoginBean>(mContext, true) {
                    override fun _onNext(result: LoginBean?) {
                        TextLog.writeTxtToFile("登录接口:" + result.toString(), Constant.filePath, Constant.fileName)
                        if (result?.status.equals("1")) {
                            App.spUtil!!.putString(Constant.MAC_ID_KEY, InterceptString.interceptSpace(mac_id))
                            view.loginSuccess()
                        } else {
                            view.loginFail(result!!.info)
                        }
                        Log.e("TAG", "登陆请求:" + result.toString())
                    }
                })

    }

    fun getGoodsInfo(code: String, mac_id: String) {
        if (mView !is IGoodsInfoView) return
        val view = mView as IGoodsInfoView

        mModel?.getNormalRequestData(ApiManager.getService().getGoodsData(code, mac_id),
                object : Response<GoodsInfoBean>(mContext, true) {
                    override fun _onNext(result: GoodsInfoBean?) {
                        TextLog.writeTxtToFile("初始化商品接口:" + result.toString(), Constant.filePath, Constant.fileName)
                        if (result?.status.equals("1")) {
                            val message = result!!.message
                            initGoodsDb(message)
                            view.goodsInfoSuccess()
                        } else {
                            view.goodsInfoFail("商品数据初始化失败")
                        }
                    }
                })
    }

    interface ILoginView {
        fun loginSuccess()
        fun loginFail(errMsg: String)
    }

    interface IGoodsInfoView {
        fun goodsInfoSuccess()
        fun goodsInfoFail(errMsg: String)
    }

    fun initGoodsDb(list: List<GoodsInfoBean.MessageBean>) {
        for (i in list.indices) {
            App.spDao!!.create(GoodsUser(list[i].g_id, list[i].box, list[i].name, list[i].pic_url, list[i].price, list[i].store, list[i].motonum))
        }
        App.spUtil!!.putBoolean(Constant.IS_LOGIN_KEY, true)
        if (!App.spUtil!!.getBoolean("IS_SET_SJ", false)) {
            App.sjdao!!.create(ParameterUser(Constant.ONE, Constant.ONE, Constant.ONE
                    , Constant.ONE, Constant.ONE, Constant.ONE, Constant.ONE, Constant.ONE, Constant.ONE, Constant.ONE,
                    Constant.ONE, Constant.ONE, Constant.ONE, Constant.ONE, Constant.ONE, Constant.ONE))
            App.spUtil!!.putBoolean("IS_SET_SJ", true)
        }
    }

}