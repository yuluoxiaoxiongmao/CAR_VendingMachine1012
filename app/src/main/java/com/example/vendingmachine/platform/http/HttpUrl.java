package com.example.vendingmachine.platform.http;

/**
 * HE 2018-10-31.
 */

public interface HttpUrl {
    /**
     *服务器域名
     */
//    String SERVER_URL = "http://hxsj.veiding.com/";
//    String SERVER_URL = "http://wl.veiding.com/";
    String SERVER_URL = "http://yct.veiding.com.cn/";
    /**
     *登陆接口
     */
//    String LOGIN_URL = "api/index/machineCheck";
    String LOGIN_URL = "api/yct/machine_login";
    /**
     *获取商品信息接口
     */
//    String GOODS_INFO = "api/resource/get_info";
    String GOODS_INFO = "api/yct/goods";
    /**
     *获取视频信息接口
     */
//    String ADV_VIDEO = "api/resource/get_video";
    String ADV_VIDEO = "api/yct/video";

    /**
     *  微信支付宝请求支付二维码接口
     */
//    String PAY_CODE = "pay1/index/index";
    String PAY_CODE = "pay/index/index";
    String PAY_CODE_CZ = "api/qrcode/pay";//二维码生成
    /**
     *微信支付宝请求支付二维码接口
     */
//    String PAY_QUERY = "pay1/index/paycheck";
    String PAY_QUERY = "api/yct/query";
    String PAY_QUERY_CZ = "api/qrcode/query";//羊城通服务器充值查询订单
    /**
     *微信支付宝BSC主扫支付,请求接口
     */
    String PASSIVE_COED = "pay/index/bar1";
    /**
     *出货完成上传结果
     */
//    String UPLOAD_SHIPMENTS = "pay1/index/out";
    String UPLOAD_SHIPMENTS = "api/yct/out_good";

    /**
     *上传补货接口
     */
    String UPLOAD_STOCK = "api/resource/goods_add";
    /**
     *版本更新接口
     */
    String VERSION_UPDATE = "update_fudaiapk/check.php";
    /**
     *定时请求在线接口
     */
    String TIMING_ON_LINE = "api/resource/is_online";
    /**
     *订单取消接口
     */
    String CANCEL_ORDER = "pay1/index/overtime";

    /**
     *获取主页，无人屏保图片
     */
    String WD_BANNER = "api/yct/banner";

    /**
     *  羊城通 数据上传接口
     */
    String YCT_UPLOAD = "api/yct/yct_send";

    /**
     * 羊城通请求支付二维码接口
     */
    String PAY_CODE_YCT = "pay/index/yct_charge";
    String PAY_CODE_YCT_CZ = "api/qrcode/pay";//羊城通服务器

    /**
     * 羊城通 消费 发送上传数据到后台
     */
    String PAY_YCT_UPLOAD_DATA = "api/yct/receive_data";

    /**
     * 羊城通 充值 成功推送
     */
    String CZ_YCT_CHARGE_OUT = "api/yct/charge_out_good";

    /**
     * 羊城通 拉取服务器数据  用于消费服务器上传
     */
    String CZ_YCT_GET_DATA = "api/yct/get_data";

    /**
     * 上传羊城通服务器成功后，用于通知本地服务器存储数据。重置接收数据
     */
    String CZ_YCT_UPLOAD_WD = "api/yct/upload_yct";
}
