package com.example.vendingmachine.platform.common;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.example.vendingmachine.bean.YctB4SendBean;
import com.example.vendingmachine.serialport.SerialPortUtil;
import com.example.vendingmachine.ui.activity.DrinkMacActivity;
import com.example.vendingmachine.ui.activity.ReplenishActivity;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 客户端
 */
public class SocketClient3 {
    private String data;
    private Activity context;
    String host = "127.0.0.1";
    int port = 80;
    Socket socket = null;
    private static BlockingQueue<String> queue = new ArrayBlockingQueue<String>(256);
    boolean writeFlag = true;           //循环写
    boolean writeThreadStatus = false;  //写线程状态
    PrintWriter buffWrite = null;       //打印流
    Thread writeThread = null;          //写线程

    boolean readFlag = true;            //循环读
    boolean readThreadStatus = false;   //读线程状态
    Thread readThread = null;           //读线程

    private String heart;             //心跳字符串
    private Timer timer = null;         //定时发心跳
    private int getReceiveCnt;        //没收到返回
    private final int CNT = 2;

    private BufferedReader in = null;

    void stop_heart_timer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    void start_heart_timer() {
        stop_heart_timer();
        getReceiveCnt = CNT;
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (--getReceiveCnt <= 0) {
                    getReceiveCnt = CNT;
                    connect();
                    return;
                }
                sendData(heart);
            }
        }, 1000, 10000);
    }

    public SocketClient3(String mhost, int mport, DrinkMacActivity context) {
        this.context = context;
        host = mhost.substring(mhost.lastIndexOf("/") + 1, mhost.length());
        Log.i("socket", "run: server ip:" + host);
        port = mport;
        heart = "1";
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        Log.e("TAG", "ID" + Constant.Companion.getMAC_ID());
        map.put("id", Constant.Companion.getMAC_ID());//20200514001
//        map.put("id", "20200514001");
        map.put("check", "1");
        Gson gson = new GsonBuilder().enableComplexMapKeySerialization().create();
        heart = gson.toJson(map);
        connect();
        a();
        start_heart_timer();

    }

    private void a() {
        LinkedHashMap<String, String> map2 = new LinkedHashMap<>();
        map2.put("id", Constant.Companion.getMAC_ID());
//        map2.put("id", "20200514001");
        map2.put("on", "1");
        Gson gson2 = new GsonBuilder().enableComplexMapKeySerialization().create();
        sendData(gson2.toJson(map2));
        Log.e("device_on", gson2.toJson(map2));
    }

    /**
     * 连接
     */
    void connect() {
        new Thread() {
            @Override
            public void run() {
                try {
                    close_all();
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.i("socket close all", "run: " + e.getMessage());
                }
                try {
                    InetAddress netAddress = InetAddress.getByName(host);// 使用getByName获得sina的IP地址
                    socket = new Socket(netAddress.getHostAddress(), port);
                    //socket.connect(new InetSocketAddress("192.168.0.7",80),5000); //连接超时5s
                    //作用：每隔一段时间检查服务器是否处于活动状态，如果服务器端长时间没响应，自动关闭客户端socket
                    socket.setKeepAlive(true);//开启保持活动状态的套接字
                    //客户端socket在接收数据时，有两种超时：
                    // 1.连接服务器超时，即连接超时；
                    // 2.连接服务器成功后，接收服务器数据超时，即接收超时
                    //*设置socket 读取数据流的超时时间
                    socket.setSoTimeout(12000);
                    //设置输出流的发送缓冲区大小，默认是8KB，即8096字节
                    socket.setSendBufferSize(8096);
                    //设置输入流的接收缓冲区大小，默认是8KB，即8096字节
                    socket.setReceiveBufferSize(8096);

                    buffWrite = new PrintWriter(socket.getOutputStream());  //将输出流包装成打印流
                    start_read_thread();
                    start_write_thread();
                    sendData(heart);
                    a();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    /**
     * 关闭资源
     */
    public void close_all() throws IOException {
        stop_read_thread();
        stop_write_thread();
        if (buffWrite != null) {
            buffWrite.close();
            buffWrite = null;
        }
        if (socket != null) {
            if (!socket.isInputShutdown()) {
//                socket.shutdownInput();
            }
            if (!socket.isOutputShutdown()) {
                //           socket.shutdownOutput();//关闭输出流
            }
            socket.close();
            socket = null;
        }
    }

    /**
     * 关闭读线程
     */
    void stop_read_thread() {
        readFlag = false;
        if (readThread != null) {
            readThread.interrupt();
        }
        readThreadStatus = false;
    }

    /**
     * 关闭写线程
     */
    void stop_write_thread() {
        writeFlag = false;
        if (writeThread != null) {
            writeThread.interrupt();
        }
        writeThreadStatus = false;
    }

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
//            if (msg.what==1){
//                InterceptString(data);
//            }
            switch (msg.what) {
                case 1:
//                    InterceptString(data);
                    break;
                case 2:
//                    mActivity.showTest(msg_info);
                    mActivity.test_all();
                    break;
                case 3:
                    mActivity.update();
                    break;
                case 4:
                    break;
            }
        }
    };

    private void InterceptString(String a) {
        String regEx = "[^0-9]";
        Pattern p = Pattern.compile(regEx);
        Matcher m = p.matcher(a);
        String trim = m.replaceAll("").trim();
        if (trim.equals("")) {
        } else {
            try {
                int dianji = Integer.parseInt(trim);
                if (dianji == 0) {
                    context.startActivityForResult(new Intent(context, ReplenishActivity.class), 0);
                }
            } catch (Exception e) {
                Log.e("InterceptString_eee", e.getMessage().toString());
            }
        }
    }

    void start_read_thread() {
        readThread = new Thread() {
            public void run() {
                Log.i("socket read", "run: start read");
//                 String h="#abc@#{\"login\":{\"user\":\"my\",\"passwd\":\"123\"}}@#def@";
                byte[] buffer = new byte[0];//上一次剩余的数据
                // java.util.Arrays.fill(buffer, (byte) 0);
                InputStream is;
                while (readFlag) {
                    try {
                        if (socket != null) {

                                if (socket.getInputStream() != null) {
                                    is = socket.getInputStream();//读取服务器端向客户端的输入流
                                    int size = is.available();
                                    if (size > 0) {//读到数据//
                                        getReceiveCnt = CNT;
                                        byte[] temp = new byte[size];
                                        is.read(temp);//读取缓冲区
                                        //去除心跳
                                        temp = check_heart(buffer, temp);
                                        data = new String(temp);
                                        handler.sendEmptyMessage(1);
                                        Log.i("Web", "服务器数据：" + new String(temp));
                                        Log.i("socket", "run: delete heart after:" + new String(temp));
                                        if (temp.length <= 0) {
                                            buffer = new byte[0];
                                            continue;
                                        }
                                        //处理数据
                                        buffer = parse_valid_data(temp);
                                    }
                                } else {
//                                    Log.i("socket", "run: not get heart ");
                                    try {
                                        Thread.sleep(1000);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }

                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        readThreadStatus = false;
                        Log.i("socket read", "run: stop read");
                    }
                }
                readThreadStatus = false;
                Log.i("socket read", "run: stop read");
            }
        };
        readFlag = true;
        readThreadStatus = true;
        readThread.start();
    }

    void start_write_thread() {
        writeThread = new Thread() {
            public void run() {
                String str;
                Log.i("socket write", "run: start write");
                while (writeFlag) {
                    try {
                        str = queue.take();//获取输出流，向服务器端发送信息
                        if (!str.isEmpty() && socket != null && socket.isConnected()) {
                            //注：此处字符串最后必须包含“\r\n\r\n”，告诉服务器HTTP头已经结束，可以处理数据，否则会造成下面的读取数据出现阻塞
                            buffWrite.write(str);//+"\r\n"
                            buffWrite.flush();
                            Log.i("socket", "sendData: " + str);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        writeThreadStatus = false;
//                        Log.e("InterruptedException",e.getMessage());
                        Log.i("socket write", "run: stop write");
                    }
                }
                writeThreadStatus = false;
                Log.i("socket write", "run: stop write");
            }
        };
        writeFlag = true;
        writeThreadStatus = true;
        writeThread.start();
    }

    /**
     * 发送
     *
     * @param str
     */
    public void sendData(String str) {
        queue.offer(str);
    }

    /**
     * 是否有心跳
     *
     * @param buffer
     * @param readData
     */
    byte[] check_heart(byte[] buffer, byte[] readData) {
        byte[] allbuffer = new byte[buffer.length + readData.length];//总数据
        Log.i("socket", "run: src:" + new String(buffer));
        Log.i("socket", "run: readdate:" + new String(readData));
        System.arraycopy(buffer, 0, allbuffer, 0, buffer.length);
        System.arraycopy(readData, 0, allbuffer, buffer.length, readData.length);
        Log.i("socket", "run: allbuff:" + new String(allbuffer));
        String s = "Heart";
        byte[] heart_buff = s.getBytes();
        if (allbuffer.length < heart_buff.length) {
            return allbuffer;
        }
        int i = 0;
        int j = 0;
        int k = 0;
        //去掉Heart字符串
        while (i < allbuffer.length) {
            if (allbuffer[i] == heart_buff[j]) {
                if (++j == heart_buff.length) {//有目标字符串
                    i++;
                    j = 0;
                    k++;
                    k -= heart_buff.length;
                    sendData("1");
                    continue;
                }
            } else {
                j = 0;
            }
            allbuffer[k] = allbuffer[i];
            i++;
            k++;
        }
        if (k > 0) {
            byte[] temp = new byte[k];
            System.arraycopy(allbuffer, 0, temp, 0, k);
            return temp;
        }
        return new byte[0];
    }

    /**
     * 解析TCP的数据
     *
     * @param temp
     */
    private String msg_info = "";

    byte[] parse_valid_data(byte[] temp) {
        byte[] buffer = new byte[0];
        while (temp.length > 0) {
            Log.i("socket", "parse_valid_data:" + new String(temp));
            if (new String(temp).startsWith("")){}
            int startIndex = -1;
            int stopIndex = -1;
            //查找数据头、数据尾
            for (int i = 0; i < temp.length; i++) {
                if (temp[i] == '&') {
                    startIndex = i;
                } else if (temp[i] == '#') {
                    stopIndex = i;
                }
                if (startIndex >= 0 && stopIndex >= 0) {
                    break;
                }
            }
            if (startIndex >= 0) {
                if (stopIndex >= 0) {
                    if (startIndex < stopIndex) {
                        if (temp[startIndex] == '&' && temp[stopIndex] == '#') {
                            byte[] validDdata = Arrays.copyOfRange(temp, startIndex + 1, stopIndex);//从下标startIndex+1开始复制，复制到下标stopIndex，不包括stopIndex
                            Log.i("socket", "run: start index " + startIndex);
                            Log.i("socket", "run: stop index " + stopIndex);
                            Log.i("socket", "run:  index " + (stopIndex - startIndex));
                            Log.i("socket", "run: parse " + new String(validDdata));
                            String validStr = new String(validDdata);//转换为字符串
                            Log.e("TAG", "服务器数据6667： " + validStr);
                            msg_info = validStr;
//                            handler.sendEmptyMessage(2);
                            order(validStr);
                            parse_json(validStr);
                        }
                        //删除@之前的数据
                        if (stopIndex == temp.length - 1) {
                            buffer = new byte[0];
                            break;
                        } else {
                            stopIndex++;
                            byte[] bt = Arrays.copyOfRange(temp, stopIndex, temp.length);
                            temp = bt;
                        }
                    } else {//删除#之前的数据
                        temp = Arrays.copyOfRange(temp, startIndex, temp.length);
                    }
                } else {//无尾
                    buffer = new byte[temp.length];
                    System.arraycopy(temp, 0, buffer, 0, temp.length);
                    break;
                }
            } else {//无头
                buffer = new byte[0];
                break;
            }
        }
        return buffer;
    }

    private void parse_json(String info) {

        try {
            if (info.contains("buhuo")) {
                //更新操作
                handler.sendEmptyMessage(3);
            } else if(info.contains("key")){
                YctB4SendBean b4bean = new YctB4SendBean();
                Gson gson = new Gson();
                b4bean = gson.fromJson(info,YctB4SendBean.class);
                mActivity.upload_b4_yct(b4bean.getData(),b4bean.getKey());
            }  else {
                int box = Integer.parseInt(info.replace("[\"", "").replace("\"]", ""));

                if (box == 100) {
                    handler.sendEmptyMessage(2);
                } else if (box == 0) {
                    //更新操作
                    handler.sendEmptyMessage(3);
                } else {
                    Log.e("box_test", box + "");
                    //1 是单开测试  2是全部测试
                    SerialPortUtil.getInstance().wd_out_goods_test(box, 1);
                }
            }

        } catch (Exception e) {
        }
    }

    private void order(String order) {
        try {
//            SocketOrederBean msgben = new SocketOrederBean();
//            Gson gson = new Gson();
//            msgben = gson.fromJson(order,SocketOrederBean.class);
//            LinkedHashMap<String, String> map2 = new LinkedHashMap<>();
////            map2.put("id", Constant.Companion.getMAC_ID());
//            map2.put("id", "20200514001");
//            map2.put("order",msgben.getOut_trade_no());
//            Gson gson2 = new GsonBuilder().enableComplexMapKeySerialization().create();
//            sendData(gson2.toJson(map2));
//            Log.e("TAG","order_sn"+ gson2.toJson(map2));
        } catch (Exception e) {
        }
    }

    public void b4_order_send(String yct_data,String money,String count,String key_65,String key_19,String key_km,String key_mac) {
        try {
//            SocketOrederBean msgben = new SocketOrederBean();
//            Gson gson = new Gson();
//            msgben = gson.fromJson(order,SocketOrederBean.class);
            LinkedHashMap<String, String> map2 = new LinkedHashMap<>();

            map2.put("id", Constant.Companion.getMAC_ID());
            map2.put("yct_data",yct_data);
            map2.put("money",money);
            map2.put("count",count);
            map2.put("key_65",key_65);
            map2.put("key_19",key_19);
            map2.put("key_km",key_km);
            map2.put("key_mac",key_mac);
            Gson gson2 = new GsonBuilder().enableComplexMapKeySerialization().create();
            connect();
            sendData(gson2.toJson(map2));
            Log.e("TAG","b4_order_sn"+ gson2.toJson(map2));
        } catch (Exception e) {
        }
    }

    public void A5_order_send(String yct_data,String a5_mac_id) {
        try {
//            SocketOrederBean msgben = new SocketOrederBean();
//            Gson gson = new Gson();
//            msgben = gson.fromJson(order,SocketOrederBean.class);
            LinkedHashMap<String, String> map2 = new LinkedHashMap<>();

            map2.put("mac_id",a5_mac_id);
            map2.put("yct_a5",yct_data);
            Gson gson2 = new GsonBuilder().enableComplexMapKeySerialization().create();
            connect();
            sendData(gson2.toJson(map2));
            Log.e("TAG","a5_order_sn"+ gson2.toJson(map2));
        } catch (Exception e) {
        }
    }

    //开机启动，让后台结算上次数据
    public void b4_wd_send() {
        try {
//            SocketOrederBean msgben = new SocketOrederBean();
//            Gson gson = new Gson();
//            msgben = gson.fromJson(order,SocketOrederBean.class);
            LinkedHashMap<String, String> map2 = new LinkedHashMap<>();
            map2.put("id", Constant.Companion.getMAC_ID());
            map2.put("yct_send","123");
            Gson gson2 = new GsonBuilder().enableComplexMapKeySerialization().create();
            connect();
            sendData(gson2.toJson(map2));
            Log.e("TAG","b4_send_wd"+ gson2.toJson(map2));
        } catch (Exception e) {
        }
    }

    public static DrinkMacActivity mActivity = null;

    //
    public void setmActivity(DrinkMacActivity mActivity) {
        this.mActivity = mActivity;
    }
}

