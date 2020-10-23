package com.example.vendingmachine.platform.http;

import com.example.vendingmachine.bean.AdvVideoBean;
import com.example.vendingmachine.bean.CzRequestCodeBean;
import com.example.vendingmachine.bean.HomeBannerBean;
import com.example.vendingmachine.bean.GoodsInfoBean;
import com.example.vendingmachine.bean.LoginBean;
import com.example.vendingmachine.bean.RequestBean;
import com.example.vendingmachine.bean.RequestCodeBean;
import com.example.vendingmachine.bean.ScavengingBean;
import com.example.vendingmachine.bean.VersionUpdateBean;
import com.example.vendingmachine.bean.YctUpLoadBean;

import okhttp3.RequestBody;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import rx.Observable;

/**
 * He sun 2018-10-31.
 */

public interface ApiService {

    @FormUrlEncoded
    @POST(HttpUrl.LOGIN_URL)
    Observable<LoginBean> login(@Field("code") String CODE,
                                @Field("mac_id") String MAC_ID,
                                @Field("machinePass") String Password);

    @FormUrlEncoded
    @POST(HttpUrl.GOODS_INFO)
    Observable<GoodsInfoBean> getGoodsData(@Field("code") String CODE,
                                           @Field("mac_id") String MAC_ID);

    @FormUrlEncoded
    @POST(HttpUrl.PAY_CODE)
    Observable<RequestCodeBean> getCode(@Field("code") String CODE,
                                        @Field("way") String WAY,
                                        @Field("mac_id") String MAC_ID,
                                        @Field("box") String BOC,
                                        @Field("g_id") String G_ID);

    @FormUrlEncoded
    @POST(HttpUrl.PAY_QUERY)
    Observable<RequestBean> queryPayResults(@Field("code") String CODE,
                                            @Field("way") String WAY,
                                            @Field("trade_no") String ORDER);

    @FormUrlEncoded
    @POST(HttpUrl.PAY_QUERY_CZ)
    Observable<RequestBean> queryPayResultsCZ(@Field("code") String CODE,
                                            @Field("way") String WAY,
                                            @Field("trade_no") String ORDER);

    @FormUrlEncoded
    @POST(HttpUrl.PASSIVE_COED)
    Observable<ScavengingBean> scavengingCode(@Field("authcode") String AUTHCODE,
                                              @Field("box") String BOX,
                                              @Field("mac_id") String MAC_ID);

    @FormUrlEncoded
    @POST(HttpUrl.ADV_VIDEO)
    Observable<AdvVideoBean> getAdvVideo(@Field("code") String CODE,
                                         @Field("mac_id") String MAC_ID);

    @FormUrlEncoded
    @POST(HttpUrl.UPLOAD_SHIPMENTS)
    Observable<String> uploadShipments(@Field("type") String CODE,
                                       @Field("status") String STATUS,
                                       @Field("out_trade_no") String TRADE);

    @FormUrlEncoded
    @POST(HttpUrl.TIMING_ON_LINE)
    Observable<String> timingRequest(@Field("mac_id") String MAC_ID);

    @FormUrlEncoded
    @POST(HttpUrl.CANCEL_ORDER)
    Observable<String> cancelOrder(@Field("code") String CODE,
                                   @Field("trade_no") String trade_no,
                                   @Field("is_overtime") String is_overtime);


    @Headers({"Content-Type: application/json", "Accept: application/json"})
    @POST(HttpUrl.UPLOAD_STOCK)
    Observable<RequestBean> uploadStock(@Body RequestBody body);


    @GET(HttpUrl.VERSION_UPDATE)
    Observable<VersionUpdateBean> getVersionUpdate();

    @FormUrlEncoded
    @POST(HttpUrl.WD_BANNER)
    Observable<HomeBannerBean> getBanner(@Field("mac_id") String MAC_ID);

    @FormUrlEncoded
    @POST(HttpUrl.YCT_UPLOAD)
    Observable<String> yctUpload(@Field("mac_id") String mac_id,
                                 @Field("motonum") String motonum,
                                 @Field("status") String status);

    @FormUrlEncoded
    @POST(HttpUrl.PAY_CODE_YCT)
    Observable<RequestCodeBean> getyctCode(
            @Field("mac_id") String MAC_ID,
            @Field("g_id") String G_ID,
            @Field("way") String WAY,
            @Field("card_num") String card_num,
            @Field("money") String money);

    @FormUrlEncoded
    @POST(HttpUrl.PAY_CODE_YCT_CZ)
    Observable<CzRequestCodeBean> getyctCodeCZ(
//            @Field("mac_id") String MAC_ID
            @Field("card_num") String card_num,
            @Field("money") String money);

    @FormUrlEncoded
    @POST(HttpUrl.PAY_YCT_UPLOAD_DATA)
    Observable<String> uploadyctData(
            @Field("mac_id") String MAC_ID,
            @Field("trade_no") String trade_no,
            @Field("receive_data") String receive_data,
            @Field("money") String money);

    //CZ_YCT_CHARGE_OUT
    @FormUrlEncoded
    @POST(HttpUrl.CZ_YCT_CHARGE_OUT)
    Observable<String> yctUpload_Cz(@Field("mac_id") String mac_id,
                                 @Field("status") String status);
    //拉取数据用于上传
    @FormUrlEncoded
    @POST(HttpUrl.CZ_YCT_GET_DATA)
    Observable<YctUpLoadBean> yct_Cz_getdata(@Field("mac_id") String mac_id);

    //上传羊城通服务器成功后，用于通知本地服务器存储数据。重置接收数据
    @FormUrlEncoded
    @POST(HttpUrl.CZ_YCT_UPLOAD_WD)
    Observable<String> cz_yct_upload_wd(@Field("mac_id") String mac_id
    ,@Field("result") String result);
}
