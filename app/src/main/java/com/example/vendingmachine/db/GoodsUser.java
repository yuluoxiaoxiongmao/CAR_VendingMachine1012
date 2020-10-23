package com.example.vendingmachine.db;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import java.io.Serializable;

/**
 *  2018-11-01.
 */
@DatabaseTable(tableName = "goods_user")
public class GoodsUser implements Serializable{
    @DatabaseField(generatedId = true)
    private int ids;
    @DatabaseField
    private int id;
    @DatabaseField
    private String box;
    @DatabaseField
    private String name;
    @DatabaseField
    private String pic_url;
    @DatabaseField
    private String price;
    @DatabaseField
    private int store;
    @DatabaseField
    private int motonum;

    public GoodsUser() {}

    public GoodsUser(int id, String box, String name, String pic_url, String price, int store, int motonum) {
        this.id = id;
        this.box = box;
        this.name = name;
        this.pic_url = pic_url;
        this.price = price;
        this.store = store;
        this.motonum = motonum;
    }

    public GoodsUser(int ids, int id, String box, String name, String pic_url, String price, int store, int motonum) {
        this.ids = ids;
        this.id = id;
        this.box = box;
        this.name = name;
        this.pic_url = pic_url;
        this.price = price;
        this.store = store;
        this.motonum = motonum;
    }

    public void setIds(int ids) {
        this.ids = ids;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setBox(String box) {
        this.box = box;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPic_url(String pic_url) {
        this.pic_url = pic_url;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public void setStore(int store) {
        this.store = store;
    }

    public void setMotonum(int motonum) {
        this.motonum = motonum;
    }

    public int getIds() {
        return ids;
    }

    public int getId() {
        return id;
    }

    public String getBox() {
        return box;
    }

    public String getName() {
        return name;
    }

    public String getPic_url() {
        return pic_url;
    }

    public String getPrice() {
        return price;
    }

    public int getStore() {
        return store;
    }

    public int getMotonum() {
        return motonum;
    }
}
