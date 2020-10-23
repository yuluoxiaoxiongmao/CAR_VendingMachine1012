package com.example.vendingmachine.bean;

import java.util.List;

/**
 * Created by 10751 on 2020-07-28.
 */

public class YctUpLoadBean {

    /**
     * total_money : 0
     * total_count : 1
     * message : ["02010099993240000000DB202007281710015100000401136376C1B207133E4DF82100000002000000020000B9F306170006009B00000000000100999932402020072817100101009999324007281658010300FF00000000"]
     */

    private String total_money;
    private String total_count;
    private List<String> message;

    public String getTotal_money() {
        return total_money;
    }

    public void setTotal_money(String total_money) {
        this.total_money = total_money;
    }

    public String getTotal_count() {
        return total_count;
    }

    public void setTotal_count(String total_count) {
        this.total_count = total_count;
    }

    public List<String> getMessage() {
        return message;
    }

    public void setMessage(List<String> message) {
        this.message = message;
    }
}
