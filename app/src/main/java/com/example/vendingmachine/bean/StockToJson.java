package com.example.vendingmachine.bean;

import java.io.Serializable;
import java.util.List;

/**
 *  2018-11-12.
 */

public class StockToJson implements Serializable{

    private String mac_id;
    private List<ContentBean> content;

    public String getMac_id() {
        return mac_id;
    }

    public void setMac_id(String mac_id) {
        this.mac_id = mac_id;
    }

    public List<ContentBean> getContent() {
        return content;
    }

    public void setContent(List<ContentBean> content) {
        this.content = content;
    }

    public static class ContentBean implements Serializable{

        public ContentBean(String box, int store) {
            this.box = box;
            this.store = store;
        }

        private String box;
        private int store;

        public String getBox() {
            return box;
        }

        public void setBox(String box) {
            this.box = box;
        }

        public int getStore() {
            return store;
        }

        public void setStore(int store) {
            this.store = store;
        }
    }
}
