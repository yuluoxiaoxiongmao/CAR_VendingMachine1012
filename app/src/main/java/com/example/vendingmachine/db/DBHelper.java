package com.example.vendingmachine.db;

import android.content.Context;
import android.widget.VideoView;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * DBHelper,
 */
public class DBHelper extends BaseQLDBHelper {
    public DBHelper(Context context) {
        super(context);
    }

    @Override
    protected <T extends Serializable> List<T> getEntityClasses() {
        List list = new ArrayList();
        list.add(GoodsUser.class);
        list.add(ParameterUser.class);
        list.add(VideoUser.class);
        return list;
    }
//    //实现一个单例返回DbHelper实例
//    private static DBHelper helper;
//
//    public static DBHelper getHelper(Context context) {
//        if (helper == null) {
//            helper = new DBHelper(context);
//        }
//        return helper;
//    }
}