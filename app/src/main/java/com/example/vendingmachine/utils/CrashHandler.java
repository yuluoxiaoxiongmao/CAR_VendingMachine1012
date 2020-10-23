package com.example.vendingmachine.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import com.example.vendingmachine.ui.activity.LoginActivity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * HE 2018-09-07.
 */
public class CrashHandler implements Thread.UncaughtExceptionHandler {
    private Context mContext;
    private Thread.UncaughtExceptionHandler mDefaultHandler;
    private static CrashHandler INSTANCE = new CrashHandler();
    private Map<String, String> info = new HashMap<String, String>();

     /**
     * 保证只有一个CrashHandler实例*/
    private CrashHandler() {}
     /**
     * 获取CrashHandler实例 ,单例模式*/
    public static CrashHandler getInstance() {
        return INSTANCE;
    }
     /**
     * 初始化
     */
    public void init(Context context) {
        mContext = context.getApplicationContext();
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();// 获取系统默认的UncaughtException处理器
        Thread.setDefaultUncaughtExceptionHandler(this);// 设置该CrashHandler为程序的默认处理器
    }

    /**
     * 当UncaughtException发生时会转入该重写的方法来处理*/
    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        if (mDefaultHandler != null) {
            handleException(ex);// 如果自定义的没有处理则让系统默认的异常处理器来处理
            restartApp();
            mDefaultHandler.uncaughtException(thread, ex);// 退出程序
            android.os.Process.killProcess(android.os.Process.myPid());//结束进程之前可以把你程序的注销或者退出代码放在这段代码之前
        }
    }


    public boolean handleException(final Throwable ex) {
        if (ex == null) return false;
        Log.e("TAG", " uncaught exception !");
        // 收集设备参数信息
        collectDeviceInfo(mContext);
        // 保存日志文件
        final String exString = getCrashInfoString(ex);
        saveCrashInfo2File(exString, mContext);
        return true;
    }


    public void collectDeviceInfo(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(context.getPackageName(), PackageManager.GET_ACTIVITIES);
            if (pi != null) {
                String versionName = pi.versionName == null ? "null" : pi.versionName;
                String versionCode = pi.versionCode + "";
                info.put("versionName", versionName);
                info.put("versionCode", versionCode);
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("TAG","Error: " + e);
        }
        Field[] fields = Build.class.getDeclaredFields();
        for (Field field : fields) {
            try {
                field.setAccessible(true);
                info.put(field.getName(), field.get("").toString());
            } catch (IllegalArgumentException e) {
                Log.e("TAG","Error: " + e);
            } catch (IllegalAccessException e) {
                Log.e("TAG","Error: " + e);
            }
        }
    }

    private String getCrashInfoString(Throwable ex) {
        StringBuffer sb = new StringBuffer();
        for (Map.Entry<String, String> entry : info.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            sb.append(key).append("=").append(value).append("\r\n");
        }
        Writer writer = new StringWriter();
        PrintWriter pw = new PrintWriter(writer);
        ex.printStackTrace(pw);
        Throwable cause = ex.getCause();
        // 循环着把所有的异常信息写入writer中
        while (cause != null) {
            cause.printStackTrace(pw);
            cause = cause.getCause();
        }
        pw.close();// 记得关闭
        String result = writer.toString();
        sb.append(result);
        return sb.toString();
    }

    @SuppressLint("SimpleDateFormat")
    public static String saveCrashInfo2File(String exString, Context context) {
        long timetamp = System.currentTimeMillis();
        String time = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date());
        String fileName = "-crash-" + time + "-" + timetamp + ".log";
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            try {
                //File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "SynChinese");
                File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + ".crash" + File.separator + context.getPackageName());
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                File outfile = new File(dir, fileName);
                FileOutputStream fos = new FileOutputStream(outfile);
                fos.write(exString.getBytes());
                fos.close();
                return outfile.getAbsolutePath();
            } catch (FileNotFoundException e) {
                Log.e("TAG","Error: " + e);
            } catch (IOException e) {
                Log.e("TAG","Error: " + e);
            }
        }
        return null;
    }

    private void restartApp(){
        Intent intent = new Intent(mContext,LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);
        android.os.Process.killProcess(android.os.Process.myPid());
    }
}
