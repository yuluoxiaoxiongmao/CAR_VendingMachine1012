package com.example.vendingmachine.db;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import java.io.Serializable;

/**
 *  He Sun 2018-09-20.
 */

@DatabaseTable(tableName = "video_user")
public class VideoUser implements Serializable {
    @DatabaseField(generatedId = true)
    private int ids;
    @DatabaseField
    private String v_url;

    public VideoUser() {}

    public VideoUser(String v_url) {
        this.v_url = v_url;
    }

    public VideoUser(int ids, String v_url) {
        this.ids = ids;
        this.v_url = v_url;
    }

    public void setIds(int ids) {
        this.ids = ids;
    }

    public void setV_url(String v_url) {
        this.v_url = v_url;
    }

    public int getIds() {
        return ids;
    }

    public String getV_url() {
        return v_url;
    }
}
