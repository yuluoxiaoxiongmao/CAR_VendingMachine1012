package com.example.vendingmachine.bean;

import java.io.Serializable;
import java.util.List;

/**
 * Created by Tony on 2018-11-14.
 */

public class LoginBean implements Serializable{


    /**
     * status : 1
     * info : 成功
     * message : []
     */

    private String status;
    private String info;
//    private List<?> message;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

//    public List<?> getMessage() {
//        return message;
//    }
//
//    public void setMessage(List<?> message) {
//        this.message = message;
//    }
}
