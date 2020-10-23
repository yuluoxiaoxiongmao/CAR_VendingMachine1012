package com.example.vendingmachine.db;

import android.content.Context;

/**
 * 同一个数据库的DBHelpr
 */

public abstract class BaseQLDBHelper extends BaseDBHelper {
    private static final String DB_NAME = "goods_info.db";  //数据库名字
    public static final int DATABASE_VERSION = 3;
    public BaseQLDBHelper(Context context) {
        //参数:上下文,数据库名称,cursor factory,数据库版本.
        super(context, DB_NAME, DATABASE_VERSION);
    }
}
