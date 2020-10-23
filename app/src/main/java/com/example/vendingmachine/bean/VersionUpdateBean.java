package com.example.vendingmachine.bean;

import java.io.Serializable;

/**
 * He 2018-11-14.
 */

public class VersionUpdateBean implements Serializable{

    /**
     * msg : 获取成功
     * status : 1
     * data : {"content":"其他bug修复","url":"http://zhgz.veiding.com/update_apk/app-release2.apk","version":"2.1"}
     */

    private String msg;
    private int status;
    private DataBean data;

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public DataBean getData() {
        return data;
    }

    public void setData(DataBean data) {
        this.data = data;
    }

    public static class DataBean implements Serializable{
        /**
         * content : 其他bug修复
         * url : http://zhgz.veiding.com/update_apk/app-release2.apk
         * version : 2.1
         */

        private String content;
        private String url;
        private String version;

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }
    }
}
