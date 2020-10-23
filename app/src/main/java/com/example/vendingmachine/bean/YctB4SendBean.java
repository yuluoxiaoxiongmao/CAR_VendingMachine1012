package com.example.vendingmachine.bean;

import java.io.Serializable;

/**
 * Created by Tony on 2018-11-14.
 */

public class YctB4SendBean implements Serializable{


    /**
     * key : A06C9268B4A56A7F
     * data : D0B4100504800058FFFF0100999932403FD600ACDF556C713DDD4E0F7D12B2A6CF32716D99E720380BE4CC24C9D062F4394C770D8249B96D7D96EE4C58FF178295D84E9C5B465E7C3FD600ACDF556C713FD600ACDF556C71B226A2A031B8743A
     */

    private String key;
    private String data;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
