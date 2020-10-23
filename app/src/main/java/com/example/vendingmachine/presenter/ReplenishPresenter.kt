package com.example.vendingmachine.presenter

import android.util.Log
import com.example.vendingmachine.App
import com.example.vendingmachine.bean.GoodsInfoBean
import com.example.vendingmachine.bean.RequestBean
import com.example.vendingmachine.bean.VersionUpdateBean
import com.example.vendingmachine.db.GoodsUser
import com.example.vendingmachine.model.BasicModel
import com.example.vendingmachine.platform.http.ApiManager
import com.example.vendingmachine.platform.http.Response
import com.example.vendingmachine.utils.APKVersionCodeUtils
import okhttp3.MediaType
import okhttp3.RequestBody
/**
 * He 2018-11-07.
 */

class ReplenishPresenter<V> : BasePresenter<V,BasicModel>(){

    override fun initModel() {
        mModel = BasicModel()
    }

    fun getData(code: String,mac_id: String,IsUpdate : Boolean){
        if(mView !is GetGoodsData) return
        val view = mView as GetGoodsData
        Log.e("message_indo",mac_id+"==="+code)
        mModel?.getNormalRequestData(ApiManager.getService().getGoodsData(code,mac_id),
                object : Response<GoodsInfoBean>(mContext,true){
                    override fun _onNext(result: GoodsInfoBean?) {
                        if (result?.status.equals("1")){
                            val message = result!!.message
                            if (IsUpdate){
                                initGoodsDb(message)
                                view.goodsInfoSuccess(message,true)
                            }else {
                                view.goodsInfoSuccess(message,false)
                            }
                        }
                    }
                })
    }

    fun uploadStock(json : String){
        if(mView !is UploadStockInterface) return
        val view = mView as UploadStockInterface

        mModel?.getNormalRequestData(ApiManager.getService().uploadStock(RequestBody.create(
                MediaType.parse("application/json; charset=UTF-8"),json)),
        object : Response<RequestBean>(mContext,true){
            override fun _onNext(result: RequestBean?) {
                view.uploadSuccess(result!!.info
                )
            }
        })
    }

    fun getVersionUpdate(){
        if(mView !is VersionUpdateInterface) return
        val view = mView as VersionUpdateInterface

        mModel?.getNormalRequestData(ApiManager.getService().versionUpdate,
                object : Response<VersionUpdateBean>(mContext,true){
                    override fun _onNext(result: VersionUpdateBean?) {
                        if (result?.status==1){
                            if(judgmentVersion(result.data.version.toDouble())) {
                                view.versionUpdateSuccess(result.data.url)
                            }
                        }else{
                            view.versionUpdateFail(result?.msg.toString())
                        }
                    }
                })
    }


    interface GetGoodsData{
        fun goodsInfoSuccess(data : List<GoodsInfoBean.MessageBean>,IsUpdate: Boolean)
    }

    interface UploadStockInterface{
        fun uploadSuccess(msg : String)
    }

    interface VersionUpdateInterface{
        fun versionUpdateSuccess(url : String)
        fun versionUpdateFail(msg : String)
    }

    fun judgmentVersion(apk_version : Double) : Boolean{
        val verName = java.lang.Double.parseDouble(APKVersionCodeUtils.getVerName(mContext))
        return apk_version>verName
    }

    fun initGoodsDb(list: List<GoodsInfoBean.MessageBean>) {
        App.spDao?.queryRaw("delete from goods_user")
        App.spDao?.queryRaw("update sqlite_sequence SET seq = 0 where name ='goods_user'")
        for (i in list.indices) {
            App.spDao!!.create(GoodsUser(list[i].g_id, list[i].box, list[i].name, list[i].pic_url, list[i].price, list[i].store, list[i].motonum))
        }
    }
}