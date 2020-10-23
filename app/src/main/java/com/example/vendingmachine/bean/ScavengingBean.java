package com.example.vendingmachine.bean;

import java.io.Serializable;

/**
 *  2018-11-06.
 */

public class ScavengingBean implements Serializable{

    /**
     * tip : 0
     * pay_way : 微信
     * out_trade_no : 2018110615483223065
     * msg : success
     * channel : 4
     */

    private String tip;
    private String pay_way;
    private String out_trade_no;
    private String msg;
    private String channel;

    public String getTip() {
        return tip;
    }

    public void setTip(String tip) {
        this.tip = tip;
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

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    @Override
    public String toString() {
        return "ScavengingBean{" +
                "tip='" + tip + '\'' +
                ", pay_way='" + pay_way + '\'' +
                ", out_trade_no='" + out_trade_no + '\'' +
                ", msg='" + msg + '\'' +
                ", channel='" + channel + '\'' +
                '}';
    }
}
