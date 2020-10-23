package com.example.vendingmachine.bean;

import java.io.Serializable;
import java.util.List;

/**
 * Created by Tony on 2018-11-14.
 */

public class HomeBannerBean implements Serializable{

    /**
     * status : 1
     * info : 成功
     * message : {"banner":[{"id":1,"pic_url":"/uploads/banner/20200521/c10cfe713c91917940494a743f0d3877.png"}],"main":[{"id":1,"pic_url":"http://yct.veiding.com.cn/uploads/banner/20200521/c10cfe713c91917940494a743f0d3877.png"},{"id":2,"pic_url":"http://yct.veiding.com.cn/uploads/banner/20200521/782e1cbbd0bdbf98cb0bcd01c6446aa8.png"}]}
     */

    private String status;
    private String info;
    private MessageBean message;

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

    public MessageBean getMessage() {
        return message;
    }

    public void setMessage(MessageBean message) {
        this.message = message;
    }

    public static class MessageBean {
        private List<BannerBean> banner;
        private List<MainBean> main;

        public List<BannerBean> getBanner() {
            return banner;
        }

        public void setBanner(List<BannerBean> banner) {
            this.banner = banner;
        }

        public List<MainBean> getMain() {
            return main;
        }

        public void setMain(List<MainBean> main) {
            this.main = main;
        }

        public static class BannerBean {
            /**
             * id : 1
             * pic_url : /uploads/banner/20200521/c10cfe713c91917940494a743f0d3877.png
             */

            private int id;
            private String pic_url;

            public int getId() {
                return id;
            }

            public void setId(int id) {
                this.id = id;
            }

            public String getPic_url() {
                return pic_url;
            }

            public void setPic_url(String pic_url) {
                this.pic_url = pic_url;
            }
        }

        public static class MainBean {
            /**
             * id : 1
             * pic_url : http://yct.veiding.com.cn/uploads/banner/20200521/c10cfe713c91917940494a743f0d3877.png
             */

            private int id;
            private String pic_url;

            public int getId() {
                return id;
            }

            public void setId(int id) {
                this.id = id;
            }

            public String getPic_url() {
                return pic_url;
            }

            public void setPic_url(String pic_url) {
                this.pic_url = pic_url;
            }
        }
    }
}
