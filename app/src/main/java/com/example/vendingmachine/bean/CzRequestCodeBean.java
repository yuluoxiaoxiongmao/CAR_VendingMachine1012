package com.example.vendingmachine.bean;

/**
 * Created by Tony on 2020-09-09.
 */

public class CzRequestCodeBean {

    /**
     * qrcode_url : weixin://wxpay/bizpayurl?pr=tO9mfnB
     * trade_no : 10121001202009091635101691581240
     */

    private String qrcode_url;
    private String trade_no;

    public String getQrcode_url() {
        return qrcode_url;
    }

    public void setQrcode_url(String qrcode_url) {
        this.qrcode_url = qrcode_url;
    }

    public String getTrade_no() {
        return trade_no;
    }

    public void setTrade_no(String trade_no) {
        this.trade_no = trade_no;
    }
}
