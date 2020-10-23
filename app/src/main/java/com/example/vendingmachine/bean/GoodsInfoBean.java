package com.example.vendingmachine.bean;

import java.io.Serializable;
import java.util.List;

/**
 * Created by Tony on 2018-11-01.
 */

public class GoodsInfoBean implements Serializable{

    /**
     * status : 1
     * message : [{"id":151,"box":"A1","name":"清咖啡","pic_url":"/uploads/picture/20180201/92b5550cc4a29a8a1de4a82ee4b3e052.png","price":"6.00","store":3,"motonum":1},{"id":152,"box":"A2","name":"意式浓缩咖啡","pic_url":"/uploads/picture/20180201/0a0ebdc50b46226f03f365e744500a41.jpg","price":"6.00","store":3,"motonum":3},{"id":153,"box":"A3","name":"清咖啡","pic_url":"/uploads/picture/20180201/92b5550cc4a29a8a1de4a82ee4b3e052.png","price":"6.00","store":4,"motonum":5},{"id":154,"box":"A4","name":"意式浓缩咖啡","pic_url":"/uploads/picture/20180201/0a0ebdc50b46226f03f365e744500a41.jpg","price":"6.00","store":4,"motonum":7},{"id":155,"box":"A5","name":"意式浓缩咖啡","pic_url":"/uploads/picture/20180201/0a0ebdc50b46226f03f365e744500a41.jpg","price":"6.00","store":5,"motonum":11},{"id":156,"box":"A6","name":"清咖啡","pic_url":"/uploads/picture/20180201/92b5550cc4a29a8a1de4a82ee4b3e052.png","price":"6.00","store":2,"motonum":13},{"id":157,"box":"A7","name":"卡布奇诺","pic_url":"/uploads/picture/20180201/13b898804486beb60c4e4d36e10a635c.jpg","price":"6.00","store":2,"motonum":15},{"id":158,"box":"A8","name":"清咖啡","pic_url":"/uploads/picture/20180201/92b5550cc4a29a8a1de4a82ee4b3e052.png","price":"6.00","store":2,"motonum":17},{"id":159,"box":"A9","name":"意式浓缩咖啡","pic_url":"/uploads/picture/20180201/0a0ebdc50b46226f03f365e744500a41.jpg","price":"6.00","store":3,"motonum":21},{"id":160,"box":"A10","name":"清咖啡","pic_url":"/uploads/picture/20180201/92b5550cc4a29a8a1de4a82ee4b3e052.png","price":"6.00","store":2,"motonum":23},{"id":161,"box":"A11","name":"意式浓缩咖啡","pic_url":"/uploads/picture/20180201/0a0ebdc50b46226f03f365e744500a41.jpg","price":"6.00","store":2,"motonum":25},{"id":162,"box":"A12","name":"清咖啡","pic_url":"/uploads/picture/20180201/92b5550cc4a29a8a1de4a82ee4b3e052.png","price":"6.00","store":3,"motonum":27},{"id":163,"box":"A13","name":"清咖啡","pic_url":"/uploads/picture/20180201/92b5550cc4a29a8a1de4a82ee4b3e052.png","price":"6.00","store":1,"motonum":31},{"id":164,"box":"A14","name":"清咖啡","pic_url":"/uploads/picture/20180201/92b5550cc4a29a8a1de4a82ee4b3e052.png","price":"6.00","store":1,"motonum":32},{"id":165,"box":"A15","name":"意式浓缩咖啡","pic_url":"/uploads/picture/20180201/0a0ebdc50b46226f03f365e744500a41.jpg","price":"6.00","store":1,"motonum":33},{"id":166,"box":"A16","name":"卡布奇诺","pic_url":"/uploads/picture/20180201/13b898804486beb60c4e4d36e10a635c.jpg","price":"6.00","store":4,"motonum":34},{"id":167,"box":"A17","name":"卡布奇诺","pic_url":"/uploads/picture/20180201/13b898804486beb60c4e4d36e10a635c.jpg","price":"6.00","store":3,"motonum":35},{"id":168,"box":"A18","name":"意式浓缩咖啡","pic_url":"/uploads/picture/20180201/0a0ebdc50b46226f03f365e744500a41.jpg","price":"6.00","store":4,"motonum":36},{"id":169,"box":"A19","name":"意式浓缩咖啡","pic_url":"/uploads/picture/20180201/0a0ebdc50b46226f03f365e744500a41.jpg","price":"6.00","store":5,"motonum":37},{"id":170,"box":"A20","name":"卡布奇诺","pic_url":"/uploads/picture/20180201/13b898804486beb60c4e4d36e10a635c.jpg","price":"6.00","store":1,"motonum":38},{"id":171,"box":"A21","name":"清咖啡","pic_url":"/uploads/picture/20180201/92b5550cc4a29a8a1de4a82ee4b3e052.png","price":"6.00","store":2,"motonum":41},{"id":172,"box":"A22","name":"意式浓缩咖啡","pic_url":"/uploads/picture/20180201/0a0ebdc50b46226f03f365e744500a41.jpg","price":"6.00","store":3,"motonum":42},{"id":173,"box":"A23","name":"清咖啡","pic_url":"/uploads/picture/20180201/92b5550cc4a29a8a1de4a82ee4b3e052.png","price":"6.00","store":3,"motonum":43},{"id":174,"box":"A24","name":"卡布奇诺","pic_url":"/uploads/picture/20180201/13b898804486beb60c4e4d36e10a635c.jpg","price":"6.00","store":2,"motonum":44},{"id":175,"box":"A25","name":"意式浓缩咖啡","pic_url":"/uploads/picture/20180201/0a0ebdc50b46226f03f365e744500a41.jpg","price":"6.00","store":3,"motonum":45},{"id":176,"box":"A26","name":"意式浓缩咖啡","pic_url":"/uploads/picture/20180201/0a0ebdc50b46226f03f365e744500a41.jpg","price":"6.00","store":3,"motonum":46},{"id":177,"box":"A27","name":"清咖啡","pic_url":"/uploads/picture/20180201/92b5550cc4a29a8a1de4a82ee4b3e052.png","price":"6.00","store":3,"motonum":47},{"id":178,"box":"A28","name":"意式浓缩咖啡","pic_url":"/uploads/picture/20180201/0a0ebdc50b46226f03f365e744500a41.jpg","price":"6.00","store":2,"motonum":48},{"id":179,"box":"A29","name":"意式浓缩咖啡","pic_url":"/uploads/picture/20180201/0a0ebdc50b46226f03f365e744500a41.jpg","price":"6.00","store":3,"motonum":51},{"id":180,"box":"A30","name":"意式浓缩咖啡","pic_url":"/uploads/picture/20180201/0a0ebdc50b46226f03f365e744500a41.jpg","price":"6.00","store":1,"motonum":52},{"id":181,"box":"A31","name":"清咖啡","pic_url":"/uploads/picture/20180201/92b5550cc4a29a8a1de4a82ee4b3e052.png","price":"6.00","store":1,"motonum":53},{"id":182,"box":"A32","name":"清咖啡","pic_url":"/uploads/picture/20180201/92b5550cc4a29a8a1de4a82ee4b3e052.png","price":"6.00","store":1,"motonum":54},{"id":183,"box":"A33","name":"意式浓缩咖啡","pic_url":"/uploads/picture/20180201/0a0ebdc50b46226f03f365e744500a41.jpg","price":"6.00","store":1,"motonum":55},{"id":184,"box":"A34","name":"卡布奇诺","pic_url":"/uploads/picture/20180201/13b898804486beb60c4e4d36e10a635c.jpg","price":"6.00","store":2,"motonum":56},{"id":185,"box":"A35","name":"意式浓缩咖啡","pic_url":"/uploads/picture/20180201/0a0ebdc50b46226f03f365e744500a41.jpg","price":"6.00","store":3,"motonum":57},{"id":186,"box":"A36","name":"意式浓缩咖啡","pic_url":"/uploads/picture/20180201/0a0ebdc50b46226f03f365e744500a41.jpg","price":"6.00","store":1,"motonum":58}]
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
         * id : 151
         * box : A1
         * name : 清咖啡
         * pic_url : /uploads/picture/20180201/92b5550cc4a29a8a1de4a82ee4b3e052.png
         * price : 6.00
         * store : 3
         * motonum : 1
         * motonum : gid
         */

        private int id;
        private String box;
        private String name;
        private String pic_url;
        private String price;
        private int store;
        private int motonum;
        private int g_id;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getBox() {
            return box;
        }

        public void setBox(String box) {
            this.box = box;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getPic_url() {
            return pic_url;
        }

        public void setPic_url(String pic_url) {
            this.pic_url = pic_url;
        }

        public String getPrice() {
            return price;
        }

        public void setPrice(String price) {
            this.price = price;
        }

        public int getStore() {
            return store;
        }

        public void setStore(int store) {
            this.store = store;
        }

        public int getMotonum() {
            return motonum;
        }

        public void setMotonum(int motonum) {
            this.motonum = motonum;
        }

        public int getG_id() {
            return g_id;
        }

        public void setG_id(int g_id) {
            this.g_id = g_id;
        }

        @Override
        public String toString() {
            return "MessageBean{" +
                    "id=" + id +
                    ", box='" + box + '\'' +
                    ", name='" + name + '\'' +
                    ", pic_url='" + pic_url + '\'' +
                    ", price='" + price + '\'' +
                    ", store=" + store +
                    ", motonum=" + motonum +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "GoodsInfoBean{" +
                "status='" + status + '\'' +
                ", message=" + message +
                '}';
    }
}
