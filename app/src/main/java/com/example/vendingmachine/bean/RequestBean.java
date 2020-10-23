package com.example.vendingmachine.bean;

import java.io.Serializable;
import java.util.List;

/**
 * Created by Tony on 2018-11-01.
 */

public class RequestBean implements Serializable{
    /**
     * status : 1
     * info : 成功
     * message : []
     */

    private int status;
    private String info;
//    private List<?> message;

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
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

//    private String status;
//    private String message;
//
//    public void setStatus(String status) {
//        this.status = status;
//    }
//
//    public void setMessage(String message) {
//        this.message = message;
//    }
//
//    public String getStatus() {
//        return status;
//    }
//
//    public String getMessage() {
//        return message;
//    }
//
//    @Override
//    public String toString() {
//        return "RequestBean{" +
//                "status='" + status + '\'' +
//                ", message='" + message + '\'' +
//                '}';
//    }
}
