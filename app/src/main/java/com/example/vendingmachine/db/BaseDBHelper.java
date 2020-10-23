package com.example.vendingmachine.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;

/**
 * 数据库辅助类的基类，需要指定两个数据类型，第一个是数据实体类，第二个是主键ID的数据类型
 * Created by dql on 2016/10/8.
 */
public abstract class BaseDBHelper extends OrmLiteSqliteOpenHelper {

    private final String TAG = getClass().getSimpleName();
    private HashMap<String, RuntimeExceptionDao> daoMap = new HashMap<>();

    public BaseDBHelper(Context context, String dbName, int dbVersion) {
        super(context, dbName, null, dbVersion);
    }

    @Override
    public synchronized SQLiteDatabase getWritableDatabase() {
        if(getDBPath() == null){
            return super.getWritableDatabase();
        }
        return SQLiteDatabase.openDatabase(getDBPath(), null, SQLiteDatabase.OPEN_READWRITE);
    }

    public synchronized SQLiteDatabase getReadableDatabase() {
        if(getDBPath() == null){
            return super.getReadableDatabase();
        }
        return SQLiteDatabase.openDatabase(getDBPath(), null, SQLiteDatabase.OPEN_READONLY);
    }

    //第一次操作数据库时候,被调用
    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase, ConnectionSource connectionSource) {
        try {
            List list2 = getEntityClasses();
            for (int i = 0; i < list2.size(); i++) {
                Class c = (Class) list2.get(i);
                TableUtils.createTableIfNotExists(connectionSource, c);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, ConnectionSource connectionSource, int j, int i1) {
        try {
            List list2 = getEntityClasses();
            for (int i = 0; i < list2.size(); i++) {
                Class c = (Class) list2.get(i);
                TableUtils.dropTable(connectionSource, c, true);

            }
            onCreate(sqLiteDatabase, connectionSource);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the RuntimeExceptionDao (Database Access Object) version of a Dao for our T class. It will
     * create it or just give the cached value. RuntimeExceptionDao only through RuntimeExceptions.
     */
    public <T, ID> RuntimeExceptionDao<T, ID> getSimpleDataDao(Class<T> c) {
        try {
            TableUtils.createTableIfNotExists(connectionSource, c);
        } catch (SQLException e) {
            throw new RuntimeException("Could not create RuntimeExcepitionDao for class " + c);
        }
        if (daoMap != null && daoMap.get(c.getSimpleName()) != null) {
            return daoMap.get(c.getName());
        }
        RuntimeExceptionDao simpleRuntimeDao;
        try {
            Dao e = this.getDao(c);
            simpleRuntimeDao = new RuntimeExceptionDao(e);
            if(daoMap == null){
                daoMap = new HashMap<>();
            }
            daoMap.put(c.getName(), simpleRuntimeDao);
        } catch (SQLException var4) {
            throw new RuntimeException("Could not create RuntimeExcepitionDao for class " + c, var4);
        }
        return simpleRuntimeDao;
    }


    /**
     * Close the database connections and clear any cached DAOs.
     */
    @Override
    public void close() {
        super.close();
        daoMap.clear();
        daoMap = null;
    }

    /**
     * 配置数据库的存放位置，返回null为不配置
     * @return
     */
    protected String getDBPath(){
        return null;
    }

    protected abstract <T extends Serializable> List<T> getEntityClasses();

}