package com.example.vendingmachine.db;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import java.io.Serializable;

/**
 *  2018-10-15.
 */
@DatabaseTable(tableName = "par_user")
public class ParameterUser implements Serializable {
    @DatabaseField(generatedId = true)
    private int ids;
    @DatabaseField
    private String setting_time;
    @DatabaseField
    private String Y_stepping;
    @DatabaseField
    private String height_1;
    @DatabaseField
    private String height_2;
    @DatabaseField
    private String height_3;
    @DatabaseField
    private String height_4;
    @DatabaseField
    private String height_5;
    @DatabaseField
    private String height_6;
    @DatabaseField
    private String reset_sd;
    @DatabaseField
    private String highest_sd;
    @DatabaseField
    private String acceleration;
    @DatabaseField
    private String deceleration;
    @DatabaseField
    private String sptime;
    @DatabaseField
    private String timeout;
    @DatabaseField
    private String taketype;
    @DatabaseField
    private String taketime;

    public ParameterUser() {}

    public ParameterUser(int ids, String setting_time, String y_stepping, String height_1, String height_2,
                         String height_3, String height_4, String height_5, String height_6, String reset_sd, String highest_sd,
                         String acceleration, String deceleration,String sptime,String timeout,String taketype,String taketime) {
        this.ids = ids;
        this.setting_time = setting_time;
        Y_stepping = y_stepping;
        this.height_1 = height_1;
        this.height_2 = height_2;
        this.height_3 = height_3;
        this.height_4 = height_4;
        this.height_5 = height_5;
        this.height_6 = height_6;
        this.reset_sd = reset_sd;
        this.highest_sd = highest_sd;
        this.acceleration = acceleration;
        this.deceleration = deceleration;
        this.sptime = sptime;
        this.timeout = timeout;
        this.taketype = taketype;
        this.taketime = taketime;
    }

    public ParameterUser(String setting_time, String y_stepping, String height_1, String height_2,
                         String height_3, String height_4, String height_5, String height_6, String reset_sd, String highest_sd,
                         String acceleration, String deceleration,String sptime,String timeout,String taketype,String taketime) {
        this.setting_time = setting_time;
        Y_stepping = y_stepping;
        this.height_1 = height_1;
        this.height_2 = height_2;
        this.height_3 = height_3;
        this.height_4 = height_4;
        this.height_5 = height_5;
        this.height_6 = height_6;
        this.reset_sd = reset_sd;
        this.highest_sd = highest_sd;
        this.acceleration = acceleration;
        this.deceleration = deceleration;
        this.sptime = sptime;
        this.timeout = timeout;
        this.taketype = taketype;
        this.taketime = taketime;
    }

    public void setIds(int ids) {
        this.ids = ids;
    }

    public void setSetting_time(String setting_time) {
        this.setting_time = setting_time;
    }

    public void setY_stepping(String y_stepping) {
        Y_stepping = y_stepping;
    }

    public void setHeight_1(String height_1) {
        this.height_1 = height_1;
    }

    public void setHeight_2(String height_2) {
        this.height_2 = height_2;
    }

    public void setHeight_3(String height_3) {
        this.height_3 = height_3;
    }

    public void setHeight_4(String height_4) {
        this.height_4 = height_4;
    }

    public void setHeight_5(String height_5) {
        this.height_5 = height_5;
    }

    public void setHeight_6(String height_6) {
        this.height_6 = height_6;
    }

    public void setReset_sd(String reset_sd) {
        this.reset_sd = reset_sd;
    }

    public void setHighest_sd(String highest_sd) {
        this.highest_sd = highest_sd;
    }

    public void setAcceleration(String acceleration) {
        this.acceleration = acceleration;
    }

    public void setDeceleration(String deceleration) {
        this.deceleration = deceleration;
    }

    public int getIds() {
        return ids;
    }

    public String getSetting_time() {
        return setting_time;
    }

    public String getY_stepping() {
        return Y_stepping;
    }

    public String getHeight_1() {
        return height_1;
    }

    public String getHeight_2() {
        return height_2;
    }

    public String getHeight_3() {
        return height_3;
    }

    public String getHeight_4() {
        return height_4;
    }

    public String getHeight_5() {
        return height_5;
    }

    public String getHeight_6() {
        return height_6;
    }

    public String getReset_sd() {
        return reset_sd;
    }

    public String getHighest_sd() {
        return highest_sd;
    }

    public String getAcceleration() {
        return acceleration;
    }

    public String getDeceleration() {
        return deceleration;
    }

    public String getSptime() {
        return sptime;
    }

    public String getTimeout() {
        return timeout;
    }

    public String getTaketype() {
        return taketype;
    }

    public String getTaketime() {
        return taketime;
    }

    public void setSptime(String sptime) {
        this.sptime = sptime;
    }

    public void setTimeout(String timeout) {
        this.timeout = timeout;
    }

    public void setTaketype(String taketype) {
        this.taketype = taketype;
    }

    public void setTaketime(String taketime) {
        this.taketime = taketime;
    }

    @Override
    public String toString() {
        return "ParameterUser{" +
                "ids=" + ids +
                ", setting_time='" + setting_time + '\'' +
                ", Y_stepping='" + Y_stepping + '\'' +
                ", height_1='" + height_1 + '\'' +
                ", height_2='" + height_2 + '\'' +
                ", height_3='" + height_3 + '\'' +
                ", height_4='" + height_4 + '\'' +
                ", height_5='" + height_5 + '\'' +
                ", height_6='" + height_6 + '\'' +
                ", reset_sd='" + reset_sd + '\'' +
                ", highest_sd='" + highest_sd + '\'' +
                ", acceleration='" + acceleration + '\'' +
                ", deceleration='" + deceleration + '\'' +
                '}';
    }
}
