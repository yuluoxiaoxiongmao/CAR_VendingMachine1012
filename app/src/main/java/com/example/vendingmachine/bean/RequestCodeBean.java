package com.example.vendingmachine.bean;

import java.io.Serializable;

/**
 * Created by Tony on 2018-11-05.
 */

public class RequestCodeBean implements Serializable{
    /**
     * tip : 0
     * msg : 获取成功
     * pay_way : 微信
     * out_trade_no : 2020052513422379899
     * url : https://open.huilianpay.com/pay/alipay/MjAyMDA1MjUxMzQyMjM3OTg5OToy
     */

    private String tip;
    private String msg;
    private String pay_way;
    private String out_trade_no;
    private String url;

    public String getTip() {
        return tip;
    }

    public void setTip(String tip) {
        this.tip = tip;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getPay_way() {
        return pay_way;
    }

    public void setPay_way(String pay_way) {
        this.pay_way = pay_way;
    }

    public String getOut_trade_no() {
        return out_trade_no;
    }

    public void setOut_trade_no(String out_trade_no) {
        this.out_trade_no = out_trade_no;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * status : 1
     * message : 获取成功
     * out_trade_no : 2018110517155360525
     * codeurl : weixin://wxpay/bizpayurl?pr=0gaUs7R
     * way : wx
     */
    /**
     * {"tip":"0","
     * msg":"\u83b7\u53d6\u6210\u529f",
     * "pay_way":"\u5fae\u4fe1",
     * "out_trade_no":"2020052513422379899",
     * "url":"https:\/\/open.huilianpay.com\/pay\/alipay\/MjAyMDA1MjUxMzQyMjM3OTg5OToy"}
     */

//    private String status;
//    private String message;
//    private String out_trade_no;
//    private String codeurl;
//    private String way;
//
//    public String getStatus() {
//        return status;
//    }
//
//    public void setStatus(String status) {
//        this.status = status;
//    }
//
//    public String getMessage() {
//        return message;
//    }
//
//    public void setMessage(String message) {
//        this.message = message;
//    }
//
//    public String getOut_trade_no() {
//        return out_trade_no;
//    }
//
//    public void setOut_trade_no(String out_trade_no) {
//        this.out_trade_no = out_trade_no;
//    }
//
//    public String getCodeurl() {
//        return codeurl;
//    }
//
//    public void setCodeurl(String codeurl) {
//        this.codeurl = codeurl;
//    }
//
//    public String getWay() {
//        return way;
//    }
//
//    public void setWay(String way) {
//        this.way = way;
//    }
//
//    @Override
//    public String toString() {
//        return "RequestCodeBean{" +
//                "status='" + status + '\'' +
//                ", message='" + message + '\'' +
//                ", out_trade_no='" + out_trade_no + '\'' +
//                ", codeurl='" + codeurl + '\'' +
//                ", way='" + way + '\'' +
//                '}';
//    }

}
