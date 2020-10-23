package com.example.vendingmachine.bean;

import java.io.Serializable;
import java.util.List;

/**
 * Created by Tony on 2018-11-14.
 */

public class CardInfoBean implements Serializable{

    /**
     * status : 1
     * message : [{"v_url":"/uploads/video/20180607/c17945c1a0e0912823a2337e759a2908.mp4","id":1},{"v_url":"/uploads/video/20180929/ce1bb0b2e9c73322f685c7b9dcd9319f.mp4","id":2}]
     */

    private String status;
    private List<MessageBean> message;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<MessageBean> getMessage() {
        return message;
    }

    public void setMessage(List<MessageBean> message) {
        this.message = message;
    }

    public static class MessageBean implements Serializable{
        /**
         * v_url : /uploads/video/20180607/c17945c1a0e0912823a2337e759a2908.mp4
         * id : 1
         */

        private String v_url;
        private int id;

        public String getV_url() {
            return v_url;
        }

        public void setV_url(String v_url) {
            this.v_url = v_url;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }
    }
}
