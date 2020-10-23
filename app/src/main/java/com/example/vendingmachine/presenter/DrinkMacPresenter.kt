package com.example.vendingmachine.presenter

import android.util.Log
import com.example.vendingmachine.App
import com.example.vendingmachine.bean.*
import com.example.vendingmachine.db.GoodsUser
import com.example.vendingmachine.db.VideoUser
import com.example.vendingmachine.model.BasicModel
import com.example.vendingmachine.platform.common.Constant
import com.example.vendingmachine.platform.http.ApiManager
import com.example.vendingmachine.platform.http.Response
import com.example.vendingmachine.utils.TextLog

/**
 * Sun 2018-11-05.
 */
class DrinkMacPresenter<V> : BasePresenter<V, BasicModel>() {

    override fun initModel() {
        mModel = BasicModel()
    }

    fun requestCode(code: String, way: String, mac_id: String, box: String, g_id: String) {
        Log.e("TAG", "code========" + code + "  " + way + "  " + mac_id)
        if (mView !is RequestCodeView) return
        val view = mView as RequestCodeView

        mModel?.getNormalRequestData(ApiManager.getService().getCode(code, way, mac_id, box, g_id),
                object : Response<RequestCodeBean>(mContext, false) {
                    override fun _onNext(result: RequestCodeBean?) {
                        TextLog.writeTxtToFile("获取二维码接口:" + result.toString(), Constant.filePath, Constant.fileName)
                        if (result?.tip == "0") {
                            view.requestCodeSuccess(result)
                        } else {
                            view.requestCodeFail(result?.msg.toString())
                        }
                    }
                })
    }

    fun requestYCTCode(mac_id: String, g_id: String, way: String, card_num: String, money: String) {
        if (mView !is RequestCodeView) return
        val view = mView as RequestCodeView

        mModel?.getNormalRequestData(ApiManager.getService().getyctCode(mac_id, g_id, way, card_num, money),
                object : Response<RequestCodeBean>(mContext, false) {
                    override fun _onNext(result: RequestCodeBean?) {
                        TextLog.writeTxtToFile("获取YCT二维码接口:" + result.toString(), Constant.filePath, Constant.fileName)
                        if (result?.tip == "0") {
                            view.requestCodeSuccess(result)
                        } else {
                            view.requestCodeFail(result?.msg.toString())
                        }
                    }
                })
    }

    //羊城通充值二维码请求
    fun requestYCTCodeCZ(mac_id: String, g_id: String, way: String, card_num: String, money: String) {
        if (mView !is RequestCodeView) return
        val view = mView as RequestCodeView

//        mModel?.getNormalRequestData(ApiManager.getService().getyctCodeCZ(mac_id,g_id,way,card_num,money),
        mModel?.getNormalRequestData(ApiManager.getService().getyctCodeCZ(card_num, money),
                object : Response<CzRequestCodeBean>(mContext, false) {
                    override fun _onNext(result: CzRequestCodeBean) {
                        Log.e("cz_qr", result.qrcode_url);
                        TextLog.writeTxtToFile("CZ获取YCT二维码接口:" + result.toString(), Constant.filePath, Constant.fileName)
//                        if (result?.tip == "0"){
                        val bean = RequestCodeBean()
                        bean.msg = "success"
                        bean.out_trade_no = result.trade_no
                        bean.pay_way = way
                        bean.tip = "0"
                        bean.url = result.qrcode_url
                        view.requestCodeSuccess(bean)
                    }
                })
    }

    fun queryPayResults(code: String, way: String, order: String) {
        if (mView !is QueryPayResultsView) return
        val view = mView as QueryPayResultsView

        mModel?.getNormalRequestData(ApiManager.getService().queryPayResults(code, way, order),
                object : Response<RequestBean>(mContext, false) {
                    override fun _onNext(result: RequestBean?) {
                        TextLog.writeTxtToFile("查询支付是否成功接口:订单号" + order + result.toString(), Constant.filePath, Constant.fileName)
                        Log.e("TAG", "order:" + order + result.toString())
                        if (result?.status == 1) {
                            view.queryPayResultsSuccess()
                        }
                    }
                })
    }

    fun queryPayResultsCZ(code: String, way: String, order: String) {
        if (mView !is QueryPayResultsView) return
        val view = mView as QueryPayResultsView

        mModel?.getNormalRequestData(ApiManager.getService().queryPayResultsCZ(code, way, order),
                object : Response<RequestBean>(mContext, false) {
                    override fun _onNext(result: RequestBean?) {
                        TextLog.writeTxtToFile("CZ查询支付是否成功接口:订单号" + order + result.toString(), Constant.filePath, Constant.fileName)
                        Log.e("TAG", "order:" + order + result.toString())
                        if (result?.status == 1) {
                            view.queryPayResultsSuccess()
                        }
                    }
                })
    }

    fun scavengingCode(authcode: String, box: String, mac_id: String) {
        if (mView !is ScavengingCodeView) return
        val view = mView as ScavengingCodeView

        mModel?.getNormalRequestData(ApiManager.getService().scavengingCode(authcode, box, mac_id),
                object : Response<ScavengingBean>(mContext, true) {
                    override fun _onNext(result: ScavengingBean?) {
                        if (result?.tip == "0") {
                            Constant.ORDER_NUMBER = result.out_trade_no
                            view.scavengingCodeSuccess()
                        } else {
                            Constant.ORDER_NUMBER = result!!.out_trade_no
                            view.scavengingCodeFail(result.msg.toString())
                        }
                    }
                })

    }

    fun uploadShipment(code: String, status: String, out_trade_no: String) {
        if (mView !is UploadShipmentView) return
        val view = mView as UploadShipmentView

        Log.e("message_indo", code + "===" + out_trade_no + "===" + status)
        mModel?.getNormalRequestData(ApiManager.getService().uploadShipments(code, status, out_trade_no),
                object : Response<String>(mContext, false) {
                    override fun _onNext(result: String?) {
                        TextLog.writeTxtToFile("上传出货结果:" + result.toString(), Constant.filePath, Constant.fileName)
                        view.uploadShipmentSuccess(result.toString())
                    }
                })
    }

    //羊城通消费上传数据
    fun uploadYCT(mac_id: String, motonum: String, state: String) {
        if (mView !is UploadYCTView) return
        val view = mView as UploadYCTView

        Log.e("message_indo", motonum + "===" + mac_id + "===")
        mModel?.getNormalRequestData(ApiManager.getService().yctUpload(mac_id, motonum, state),
                object : Response<String>(mContext, false) {
                    override fun _onNext(result: String?) {
                        TextLog.writeTxtToFile("yct消费上传出货结果:" + result.toString(), Constant.filePath, Constant.fileName)
                        view.uploadYCTSuccess(result.toString())
                    }
                })
    }

    //羊城通充值成功上传数据
    fun uploadYCT_CZ(mac_id: String, state: String) {
        if (mView !is UploadYCTView) return
        val view = mView as UploadYCTView

        Log.e("message_indo", "===" + mac_id + "===")
        mModel?.getNormalRequestData(ApiManager.getService().yctUpload_Cz(mac_id, state),
                object : Response<String>(mContext, false) {
                    override fun _onNext(result: String?) {
                        TextLog.writeTxtToFile("yct充值上传出货结果:" + result.toString(), Constant.filePath, Constant.fileName)
//                        view.uploadYCTSuccess(result.toString())
                    }
                })
    }


    fun getAdvVideo(code: String, mac_id: String, IS_Update: Boolean) {
        if (mView !is AdvVideoView) return
        val view = mView as AdvVideoView

        mModel?.getNormalRequestData(ApiManager.getService().getAdvVideo(code, mac_id),
                object : Response<AdvVideoBean>() {
                    override fun _onNext(result: AdvVideoBean?) {
                        if (result?.status == "1") {
                            setAdvVideoDao(result.message, IS_Update)
                            view.advVideoSuccess(result.message, IS_Update)
                        } else {
                            view.advVideoFail("广告视频获取失败!")
                        }
                    }
                })
    }

    fun getTimingRequest(mac_id: String) {
        if (mView !is TimingRequestView) return
        val view = mView as TimingRequestView

        mModel?.getNormalRequestData(ApiManager.getService().timingRequest(mac_id),
                object : Response<String>() {
                    override fun _onNext(result: String?) {
                        Log.e("TAG", "定时请求服务器" + result.toString())
                        view.timingRequestSuccess()
                    }
                })
    }

    fun cancelOrder(Order: String) {
        if (mView !is CancelOrderView) return
        val view = mView as CancelOrderView
        mModel?.getNormalRequestData(ApiManager.getService().cancelOrder("108", Order, "1"),
                object : Response<String>() {
                    override fun _onNext(result: String?) {
                        Log.e("TAG", "取消订单：" + result.toString())
                        TextLog.writeTxtToFile("取消订单结果:" + Order + "======" + result.toString(), Constant.filePath, Constant.fileName)
                        view.cancelOrderCode()
                    }
                })
    }

    //获取轮播图getBanner
    fun getBanner(mac_id: String) {
        if (mView !is GetBannerView) return
        val view = mView as GetBannerView
        mModel?.getNormalRequestData(ApiManager.getService().getBanner(mac_id),
                object : Response<HomeBannerBean>() {
                    override fun _onNext(result: HomeBannerBean?) {
                        Log.e("TAG", "获取banner：" + result.toString())
                        view.getBannerSuccess(result!!.message)
                    }
                })
    }

    //拉取本地服务器数据 用于上传数据到羊城通
//    yct_Cz_getdata
    fun getYctData(mac_id: String) {
        if (mView !is GetYCTDataView) return
        val view = mView as GetYCTDataView
        mModel?.getNormalRequestData(ApiManager.getService().yct_Cz_getdata(mac_id),
                object : Response<YctUpLoadBean>() {
                    override fun _onNext(result: YctUpLoadBean) {
                        Log.e("TAG", "获取用于上传数据：" + result.toString())
                        if (result.message.size > 0) {
                            view.getYctDataSuccess(result)
                        }
                    }
                })
    }

    //上传羊城通服务器成功后，用于通知本地服务器存储数据。重置接收数据
    fun getYctUploadToWD(mac_id: String, success: String) {
        if (mView !is GetYCTUploadToWdView) return
        val view = mView as GetYCTUploadToWdView
        mModel?.getNormalRequestData(ApiManager.getService().cz_yct_upload_wd(mac_id, success),
                object : Response<String>() {
                    override fun _onNext(result: String) {
                        Log.e("TAG", "重置接收数据：" + result.toString())
                        view.getYctToWdSuccess(result)
                    }
                })
    }

    interface GetYCTUploadToWdView {
        fun getYctToWdSuccess(info: String)
    }

    interface GetYCTDataView {
        fun getYctDataSuccess(info: YctUpLoadBean)
    }

    interface RequestCodeView {
        fun requestCodeSuccess(result: RequestCodeBean)
        fun requestCodeFail(errMsg: String)
    }

    interface RequestCodeViewCZ {
        fun requestCodeSuccessCZ(result: CzRequestCodeBean)
    }

    interface QueryPayResultsView {
        fun queryPayResultsSuccess()
    }

    interface ScavengingCodeView {
        fun scavengingCodeSuccess()
        fun scavengingCodeFail(errMsg: String)
    }

    interface UploadShipmentView {
        fun uploadShipmentSuccess(errMsg: String)
    }

    interface UploadYCTView {
        fun uploadYCTSuccess(errMsg: String)
    }


    interface AdvVideoView {
        fun advVideoSuccess(list: List<AdvVideoBean.MessageBean>, IS_Update: Boolean)
        fun advVideoFail(errMsg: String)
    }

    interface TimingRequestView {
        fun timingRequestSuccess()
    }

    interface CancelOrderView {
        fun cancelOrderCode()
    }

    interface GetBannerView {
        fun getBannerSuccess(messageBean: HomeBannerBean.MessageBean)
    }

    fun setAdvVideoDao(list: List<AdvVideoBean.MessageBean>, IS_Video: Boolean) {
        if (IS_Video) {
            App.videoDao?.queryRaw("delete from video_user")
            App.videoDao?.queryRaw("update sqlite_sequence SET seq = 0 where name ='video_user'")
            for (i in list.indices) {
                App.videoDao?.create(VideoUser(list[i].v_url))
            }
        } else {
            for (i in list.indices) {
                App.videoDao?.create(VideoUser(list[i].v_url))
            }
        }
    }

    fun getData(code: String, mac_id: String, IsUpdate: Boolean) {
        if (mView !is GetGoodsData) return
        val view = mView as GetGoodsData
        Log.e("message_indo", mac_id + "===" + code)
        mModel?.getNormalRequestData(ApiManager.getService().getGoodsData(code, mac_id),
                object : Response<GoodsInfoBean>(mContext, true) {
                    override fun _onNext(result: GoodsInfoBean?) {
                        if (result?.status.equals("1")) {
                            val message = result!!.message
                            if (IsUpdate) {
                                initGoodsDb(message)
                                view.goodsInfoSuccess(message, true)
                            } else {
                                view.goodsInfoSuccess(message, false)
                            }
                        }
                    }
                })
    }

    interface GetGoodsData {
        fun goodsInfoSuccess(data: List<GoodsInfoBean.MessageBean>, IsUpdate: Boolean)
    }

    fun getuploadYCTData(mac_id: String, trade_no: String, receive_data: String, money: String) {
        if (mView !is uploadYCTDataView) return
        val view = mView as uploadYCTDataView
        Log.e("up_yctorder_indo", mac_id + "===" + trade_no + "===" + receive_data)
        mModel?.getNormalRequestData(ApiManager.getService().uploadyctData(mac_id, trade_no, receive_data, money),
                object : Response<String>(mContext, true) {
                    override fun _onNext(result: String?) {
                        Log.e("message_updata_yct", result.toString())
                        view.uploadYctDataSuccess(result.toString())
                    }
                })
    }

    interface uploadYCTDataView {
        fun uploadYctDataSuccess(state: String)
    }

    interface UploadStockInterface {
        fun uploadSuccess(msg: String)
    }

    fun initGoodsDb(list: List<GoodsInfoBean.MessageBean>) {
        App.spDao?.queryRaw("delete from goods_user")
        App.spDao?.queryRaw("update sqlite_sequence SET seq = 0 where name ='goods_user'")
        for (i in list.indices) {
            App.spDao!!.create(GoodsUser(list[i].g_id, list[i].box, list[i].name, list[i].pic_url, list[i].price, list[i].store, list[i].motonum))
        }
    }
}