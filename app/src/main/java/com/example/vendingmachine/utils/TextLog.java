package com.example.vendingmachine.utils;

import android.annotation.SuppressLint;
import android.util.Log;
import com.example.vendingmachine.platform.common.Constant;
import com.example.vendingmachine.serialport.SerialDataUtils;

import java.io.File;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.Calendar;
/**
 * HE SUN 2018-12-26.
 */

public class TextLog {
   @SuppressLint("SimpleDateFormat")
    private static SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    public static void initData(String mac_id) {
        writeTxtToFile("宏祥LOG记录:机台号："+mac_id+" === （有此LOG说明软件重启了）", Constant.Companion.getFilePath(), Constant.fileName);
    }
    // 将字符串写入到文本文件中
    public static void writeTxtToFile(String strcontent, String filePath, String fileName) {
        String time = dateFormatter.format(Calendar.getInstance().getTime());

        //生成文件夹之后，再生成文件，不然会出错
        makeFilePath(filePath, fileName);

        String strFilePath = filePath+fileName;
        // 每次写入时，都换行写
        String strContent = time+" ==== " + strcontent + "\r\n";
        try {
            File file = new File(strFilePath);
            if (!file.exists()) {
                Log.d("TestFile", "Create the file:" + strFilePath);
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
            RandomAccessFile raf = new RandomAccessFile(file, "rwd");
            raf.seek(file.length());
            raf.write(strContent.getBytes());
            raf.close();
        } catch (Exception e) {
            Log.e("TestFile", "Error on write File:" + e);
        }
    }
    //写羊城通文件
    public static void writeYctFile(String strcontent, String filePath, String fileName) {
        //生成文件夹之后，再生成文件，不然会出错
        makeFilePath(filePath, fileName);

        String strFilePath = filePath+fileName;
        String strContent = strcontent;
        try {
            File file = new File(strFilePath);
            if (!file.exists()) {
                Log.d("TestFile", "Create the file:" + strFilePath);
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
            RandomAccessFile raf = new RandomAccessFile(file, "rwd");
            //raf.seek(0);//每次覆盖写
            raf.setLength(0);//先清空文件内容
            raf.write(strContent.getBytes());
            raf.close();
        } catch (Exception e) {
            Log.e("TestFile", "Error on write File:" + e);
        }
    }
    public static String readYctFile(String filePath, String fileName) {

        String strFilePath = filePath+fileName;
        String strContent=null;
        byte[] b=new byte[1024];
        int hasRead=0;

        try {
            File file = new File(strFilePath);
            if (!file.exists()) {
                return strContent;
            }
            RandomAccessFile raf = new RandomAccessFile(file, "r");
            raf.seek(0);
            //循环读取文件
            while((hasRead=raf.read(b))>0){
                //输出文件读取的内容
                strContent = (new String(b,0,hasRead));
            }
            raf.close();
            return strContent;
        } catch (Exception e) {
            Log.e("TestFile", "Error on write File:" + e);
            strContent=null;
            return strContent;
        }
    }

    // 生成文件
    private static File makeFilePath(String filePath, String fileName) {
        File file = null;
        makeRootDirectory(filePath);
        try {
            file = new File(filePath + fileName);
            if (!file.exists()) {
                file.createNewFile();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return file;
    }

    // 生成文件夹
    public static void makeRootDirectory(String filePath) {
        File file = null;
        try {
            file = new File(filePath);
            if (!file.exists()) {
                file.mkdir();
            }
        } catch (Exception e) {
            Log.i("error:", e+"");
        }
    }

}
