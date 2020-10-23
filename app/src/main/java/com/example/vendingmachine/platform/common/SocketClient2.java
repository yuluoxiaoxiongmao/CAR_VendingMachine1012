package com.example.vendingmachine.platform.common;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.example.vendingmachine.App;
import com.example.vendingmachine.serialport.CarSerialPortUtil;
import com.example.vendingmachine.serialport.SerialDataUtils;
import com.example.vendingmachine.ui.activity.DrinkMacActivity;
import com.example.vendingmachine.ui.activity.ReplenishActivity;
import com.example.vendingmachine.utils.InterceptString;
import com.example.vendingmachine.utils.MD5;
import com.example.vendingmachine.utils.encryption.Des;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.example.vendingmachine.utils.encryption.Des.deCrypto_2;

/**
 * 客户端
 */
public class SocketClient2 {
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
    private String socket_mac_id = "";

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
        }, 1000, 5000);
    }

    public SocketClient2(String mhost, int mport, DrinkMacActivity context, String mac_id) {
        this.context = context;
        socket_mac_id = mac_id;
        host = mhost.substring(mhost.lastIndexOf("/") + 1, mhost.length());
        Log.i("socket", "run: server ip:" + host);
        port = mport;
        heart = "1";
//        LinkedHashMap<String, String> map = new LinkedHashMap<>();
//        Log.e("TAG_ID","ID"+  mac_id);
//
//        map.put("id", mac_id);
//        map.put("check","1");
//        Gson gson = new GsonBuilder().enableComplexMapKeySerialization().create();
//        heart = gson.toJson(map);
        connect();
        start_heart_timer();

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
                    Log.e("socket_is_connect", socket.isConnected() + "");
                    //socket.connect(new InetSocketAddress("192.168.0.7",80),5000); //连接超时5s
                    //作用：每隔一段时间检查服务器是否处于活动状态，如果服务器端长时间没响应，自动关闭客户端socket
                    socket.setKeepAlive(true);//开启保持活动状态的套接字
                    //客户端socket在接收数据时，有两种超时：
                    // 1.连接服务器超时，即连接超时；
                    // 2.连接服务器成功后，接收服务器数据超时，即接收超时
                    //*设置socket 读取数据流的超时时间
                    socket.setSoTimeout(1000);
                    //设置输出流的发送缓冲区大小，默认是8KB，即8096字节
                    socket.setSendBufferSize(8096);
                    //设置输入流的接收缓冲区大小，默认是8KB，即8096字节
                    socket.setReceiveBufferSize(8096);

                    buffWrite = new PrintWriter(socket.getOutputStream());  //将输出流包装成打印流
                    start_read_thread();
                    start_write_thread();
//                    sendData(heart);
//                    a();
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

    private String showInfo = "";
    private int showState = 0;
    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                    InterceptString(data);
                    break;
                case 2:
//                    mActivity.showTest(msg_info);
//                    start_read_thread();
//                    start_write_thread();
                    is_stop_write = true;
                    connect();
                    //传递数据
                    break;
                case 3:
                    is_stop_write = true;
                    break;
                case 4:
                    mActivity.showInfo(showInfo, showState);
                    break;
                case 5:
                    mActivity.showUpInfo(showInfo, showState);
                    break;
                case 6:
                    mActivity.upload_cz_info(bytesToHexString(A8_byte_nfo).replace(" ", ""), money_all_upload + "");
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
            int dianji = Integer.parseInt(trim);
            if (dianji == 0) {
                context.startActivityForResult(new Intent(context, ReplenishActivity.class), 0);
            }
        }
    }

    private int upload_num = 1;//如果上传不成功，重复上传3次
    private boolean upload_is = false;//如果上传不成功，重复上传3次
    private List<Byte> read_buffer = new ArrayList<>();
    private long lastReadTime = 0;
    InputStream is;
    private static int num_order = 1;//传送序号 自增

    void start_read_thread() {
        readThread = new Thread() {
            public void run() {
                Log.i("socket read", "run: start read");
//                 String h="#abc@#{\"login\":{\"user\":\"my\",\"passwd\":\"123\"}}@#def@";
                byte[] buffer = new byte[0];//上一次剩余的数据
                // java.util.Arrays.fill(buffer, (byte) 0);
                while (readFlag) {
                    try {
                        is = socket.getInputStream();//读取服务器端向客户端的输入流
                        int size = is.available();
                        if (size > 0) {//读到数据//
                            getReceiveCnt = CNT;
                            byte[] temp = new byte[size];
                            is.read(temp);//读取缓冲区-
                            //转换数据类型方便查看
                            Log.i("socket_read_car0", "read data: " + bytesToHexString(temp));
                            if (temp[0] == (byte) 0xD0) {
                                switch (temp[1]) {
                                    case (byte) 0xBA:
                                        Log.i("socket_read_1car1", "read data: ");
                                        //签到数据2 ，加密状态，传递解密
                                        CarSerialPortUtil.getInstance().pay_check_sign_3(temp);
                                        break;
                                    case (byte) 0xB0:
                                        Log.i("socket_read_car1_b0", "read data: ");
                                        //解密授权接口数据
                                        pay_des_buy(temp);
                                        break;
                                    case (byte) 0xB1:
                                        upload_is = true;
                                        //解密上传数据接口
                                        money_all_upload += money_all;
                                        num_order++;
                                        Constant.Companion.setCAR_ORDER_NUM(num_order);
//                                        int money_end = Constant.Companion.getCAR_ORDER_MONEY()+money_all;
//                                        Constant.Companion.setCAR_ORDER_MONEY(money_end);
                                        Log.i("socket_read_car1_b1", "read data: " + money_all_upload);
                                        pay_des_upload(temp, "b1");
//                                        num_order++;
                                        handler.removeMessages(2);
                                        break;
                                    case (byte) 0xB4:
                                        //解密上传数据接口
                                        if (state_b4 == 0) {
                                            pay_des_upload(temp, "b4");
                                        } else if (state_b4 == 1) {
                                            pay_des_upload_b4(temp, "b4");
                                        }
//                                        pay_des_upload(temp,"b4");
                                        handler.removeMessages(2);
                                        break;
                                    case (byte) 0xA5:
                                        pay_des_upload(temp, "A5");
                                        handler.removeMessages(2);
                                        break;
                                }
                            }
                            if (temp[0] == (byte) 0xFE) {
                                //收到F4的数据，00 认证成功，FF失败
                                if (temp[1] == (byte) 0x04) {
                                    if (temp[size - 1] == (byte) 0x00) {

                                    } else {
                                        //认证失败重新签到注册'
                                        CarSerialPortUtil.getInstance().test_card();
                                    }
                                } else if (temp[1] == (byte) 0x02) {
                                    //发送f2数据,获取DE3
                                    CarSerialPortUtil.getInstance().test_sign_in_2(temp);
                                }
                            } else {

                            }
                        } else {
//                            Log.i("socket", "run: not get heart ");
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    } catch (IOException e) {
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

    private byte[] scoket_cmd = null;
    private boolean is_stop_write = true;

    public void start_write_thread() {
        writeThread = new Thread() {
            public void run() {
                String str;
                Log.i("socket write", "run: start write");
                while (writeFlag) {
                    try {
                        str = queue.take();//获取输出流，向服务器端发送信息
                        if (!str.isEmpty() && socket != null && socket.isConnected()) {
                            //注：此处字符串最后必须包含“\r\n\r\n”，告诉服务器HTTP头已经结束，可以处理数据，否则会造成下面的读取数据出现阻塞
//                            buffWrite.write(scoket_cmd);//+"\r\n"
//                            buffWrite.flush();
//                            Log.i("socket_send", "sendData: " + test_scoket.replace(" ","").trim());
                            try {
                                Log.e("scoket_cmd_pay", bytesToHexString(scoket_cmd) + "");
                                if (is_stop_write) {
                                    OutputStream stream = socket.getOutputStream();
                                    stream.write(scoket_cmd);
                                    stream.flush();
                                    is_stop_write = false;
                                } else {
                                    Log.e("writer_eee", "no write");
                                }

                            } catch (Exception e) {
                                Log.e("writer_eee", e.getMessage().toString());
                            }

                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        writeThreadStatus = false;
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

    public static DrinkMacActivity mActivity = null;

    public void setmActivity(DrinkMacActivity mActivity) {
        this.mActivity = mActivity;
    }

    public static String bytesToHexString(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        String result = "";
        for (int i = 0; i < bytes.length; i++) {
            String hexString = Integer.toHexString(bytes[i] & 0xFF);
            if (hexString.length() == 1) {
                hexString = '0' + hexString;
            }
            result += (hexString.toUpperCase() + " ");
        }
        return result;
    }

    //存储卡类型  01 M1 卡   02 cpu卡
    private byte car_state = (byte) 0x01;
    private byte[] socket_b4 = null;
    //0 是正常结算  1是服务器发送数据结算
    private int state_b4 = 0;

    public void getSendSocketInfo(byte[] cmd, int state, String ship_info) {
        Log.e("111_info3_socket_info", state + "");
        switch (state) {
            case 108:
                //读卡数据，处理数据，发送扣费，圈存报文
                //读卡成功,获取卡物理信息
//                scoket_cmd =
                check_CARD(App.Companion.getSpUtil().getString(Constant.Companion.getCAR_SIGN_INFO(), ""), ship_info, cmd);
                handler.sendEmptyMessage(2);
                break;
            case 103:
                //签到，认证，已经处理，直接发送
                Log.e("103_scoket", bytesToHexString(cmd) + "");
                scoket_cmd = new byte[cmd.length];
                scoket_cmd = cmd;
                handler.sendEmptyMessage(2);
                break;
            case 104:
                scoket_cmd = new byte[cmd.length];
                scoket_cmd = cmd;
                handler.sendEmptyMessage(2);
                break;
            case 109:
                //签到认证
                scoket_cmd = new byte[check_pay_sign_1(cmd).length];
                scoket_cmd = check_pay_sign_1(cmd);
                break;
            case 110:
                //读卡传递的数据，用于B0发送
                Log.e("110_b4", bytesToHexString(cmd));
                car_state = cmd[4];
                scoket_cmd = new byte[check_pay_agree_1(cmd, ship_info).length];
                scoket_cmd = check_pay_agree_1(cmd, ship_info);
                is_stop_write = true;
                start_write_thread();
//                int time_110 = 100;
//                for (int i =0;i<3;i++){
//                    time_110 += time_110;
//                    handler.sendMessageDelayed(handler.obtainMessage(2), time_110);
//                }
//                handler.sendEmptyMessage(2);
                break;
            case 111:
                //交易记录上传  成功
                Log.e("111_info_1", bytesToHexString(cmd));
//                if (car_state == (byte) 0x01) {
//                    scoket_cmd = new byte[check_pay_upload_M1_Test2(cmd).length];
//                } else {
//                    scoket_cmd = new byte[check_pay_upload_CPU(cmd).length];
//                    scoket_cmd = check_pay_upload_CPU(cmd);
//                }
//                int time = 2000;
//                for (int i =0;i<3;i++){
//                    time += time;
//                    handler.sendMessageDelayed(handler.obtainMessage(2), time);
//                }
//                check_pay_upload_M12(cmd);
                if (car_state == (byte) 0x01) {
                    scoket_cmd = new byte[check_pay_upload_M12(cmd).length];
                    scoket_cmd = check_pay_upload_M12(cmd);
                } else {
                    scoket_cmd = new byte[check_pay_upload_CPU(cmd).length];
                    scoket_cmd = check_pay_upload_CPU(cmd);
                }
                int time = 1000;
                for (int i = 0; i < 3; i++) {
                    time += time;
                    handler.sendMessageDelayed(handler.obtainMessage(2), time);
                }
//                socket_b4 = upload_B4(cmd,ship_info);

//                //快速结算 b4
//                socket_b4 = upload_B4(cmd,ship_info);
//                int time1 = 1000;
//                for (int i =0;i<3;i++){
//                    time1 += time1;
//                    handler.sendMessageDelayed(handler.obtainMessage(2), time1);
//                }
                break;
            case 1111:
                //交易记录上传  失败
                break;
            case 113:
                //读取卡应用信息成功，开始查询交易类型
                type_search(cmd);
//                scoket_cmd = new byte[type_search(cmd).length];
//                scoket_cmd = type_search(cmd);
//                handler.sendEmptyMessage(2);
                break;
            case 114:
                state_b4 = 0;
                scoket_cmd = new byte[upload_B4().length];
                scoket_cmd = upload_B4();
                int time1 = 1000;
                for (int i = 0; i < 3; i++) {
                    time1 += time1;
                    handler.sendMessageDelayed(handler.obtainMessage(2), time1);
                }
                break;
            case 115:
                scoket_cmd = new byte[upload_A5((num_order - 1) + "", money_all_upload + "").length];
                scoket_cmd = upload_A5((num_order - 1) + "", money_all_upload + "");
                int time2 = 1000;
                for (int i = 0; i < 3; i++) {
                    time2 += time2;
                    handler.sendMessageDelayed(handler.obtainMessage(2), time2);
                }
                break;
            case 116:
//                manger_num = new ArrayList<>();
//                String[] manger = "D0 B4 10 05 04 80 00 58 FF FF 01 00 99 99 32 40 3F D6 00 AC DF 55 6C 71 3D DD 4E 0F 7D 12 B2 A6 CF 32 71 6D 99 E7 20 38 0B E4 CC 24 C9 D0 62 F4 39 4C 77 0D 82 49 B9 6D 7D 96 EE 4C 58 FF 17 82 95 D8 4E 9C 5B 46 5E 7C 3F D6 00 AC DF 55 6C 71 3F D6 00 AC DF 55 6C 71 B2 26 A2 A0 31 B8 74 3A".split(" ");
//                for (int i = 0; i < 4; i++) {
//                    manger_num.add(manger[i]);
//                }
//                scoket_cmd = new byte[check_CARD(manger_num,manger_num.size()).length];
//                scoket_cmd = check_CARD(manger_num,manger_num.size());
//                int time6 = 1000;
//                for (int i =0;i<3;i++){
//                    time6 += time6;
//                    handler.sendMessageDelayed(handler.obtainMessage(2), time6);
//                }
                break;
            case 118:
                scoket_cmd = new byte[upload_ALL_A8(cmd).length];
                scoket_cmd = upload_ALL_A8(cmd);
                int time3 = 1000;
                for (int i = 0; i < 3; i++) {
                    time3 += time3;
                    handler.sendMessageDelayed(handler.obtainMessage(2), time3);
                }
                break;
        }
    }

    private static String b4_key = "";

    public void SendB4SocketInfo(String data, int state, String key_65, String key_19, String key_km) {
        Log.e("b4_socket_info", state + "");
        switch (state) {
            case 119:
//                byte[] b4_info = SerialDataUtils.hexString2Bytes(data.replace(" ",""));
//                scoket_cmd = new byte[b4_info.length];
//                scoket_cmd = b4_info;
//                int time6 = 1000;
//                for (int i =0;i<3;i++){
//                    time6 += time6;
//                    handler.sendMessageDelayed(handler.obtainMessage(2), time6);
//                }
                byte[] A5_info = upload_A5(data, key_km);
//                mActivity.upload_b4_yct();
                break;
        }
    }

    private List<String> manger_num = null;

    public static byte[] check_CARD(List<String> num, int size) {
        byte[] pack = new byte[size];

        for (int i = 0; i < size; i++) {
            int num_info = Integer.valueOf(num.get(i), 16);
            pack[i] = (byte) num_info;
//            test_info += (" " + num.get(i));
        }
//        Log.e("test_sign_in_2", test_info + "");
        return pack;
    }

//    public void getCardInfo(byte[] cmd, int state, String ship_info) {
//        Log.e("111_info3_socket_info", state + "");
//                //交易记录上传  成功
//                Log.e("111_info_1", bytesToHexString(cmd));
//                if (car_state == (byte) 0x01) {
////                    check_pay_upload_M1(cmd);
//                    scoket_cmd = new byte[check_pay_upload_M1(cmd).length];
//                    scoket_cmd = check_pay_upload_M1(cmd);
//                    handler.sendEmptyMessage(3);
////                    handler.sendMessageDelayed(handler.obtainMessage(2),1000);
//                } else {
//                    check_pay_upload_CPU(cmd);
//                }
//    }
    //组装交易类型数据  扣费查询 P_PURCHASE_QRY

    /**
     * BD B9 C9 00   包头  0-3
     * E3 00 2A DB B1 C8 00 32    握手流水号 4-11
     * 3D EF 18 84 7D 90 90 5F 7A 62 5A BC 54 78 F6 A6 D1 10 24 EB A9 BF D7 7E 74 74 B9 DE A4 F8 DB 69   会话密钥（取前16.AES密钥） 12-43
     * 20 05 15 09 01 08
     * <p>
     * 89 00 71 44
     *
     * @param sign_info
     * @return
     */
    public static byte[] check_CARD(String sign_info, String shid_info, byte[] cmd) {
        Log.e("shid_info", shid_info);
        byte[] pack = new byte[128];
        pack[0] = (byte) 0xE0;
        pack[1] = (byte) 0x11;                                           //包头E0 11 2字节
        pack[2] = (byte) 0x00;
        pack[3] = (byte) 0x01;                                          //len 2字节
        pack[4] = (byte) 0x02;                                          //加密算法 1字节
        pack[5] = (byte) 0x80;                                          //排序方式 1字节
        pack[6] = (byte) 0x00;                                          //报文长度
        pack[7] = (byte) 0x78;                                          //报文长度 2字节
        String[] manger = sign_info.split(" ");
        String test_info = "";
        int index = 8;
        for (int i = 0; i < 8; i++) {
            int num_info = Integer.valueOf(manger[i + 4], 16);
            pack[index + i] = (byte) num_info;
//            test_info += (" " + manger[i+4]);
        }

        int tu5 = Constant.Companion.getCAR_ORDER_NUM();
        byte[] bytes5 = new byte[4];
        bytes5[3] = (byte) (tu5 & 0xFF);
        bytes5[2] = (byte) (tu5 >> 8 & 0xFF);
        bytes5[1] = (byte) (tu5 >> 16 & 0xFF);
        bytes5[0] = (byte) (tu5 >> 24 & 0xFF);
        pack[16] = (byte) bytes5[0];
        pack[17] = (byte) bytes5[1];
        pack[18] = (byte) bytes5[2];
        pack[19] = (byte) bytes5[3];//报文序号，自增
        //PKI管理卡号 4位
        String[] manger_shid = shid_info.trim().split(" ");
        int index_shid = 20;
        Log.e("manger_shid_len", manger_shid.length + "");
        for (int i = 0; i < 4; i++) {
            int num_info_1 = Integer.valueOf(manger_shid[i], 16);
            pack[index_shid + i] = (byte) num_info_1;
            test_info += (" =" + manger_shid[i]);
        }
        // 物理卡号，逻辑卡号拼接 共16位
        int index_car = 24;
        for (int i = 0; i < 16; i++) {
            pack[index_car + i] = cmd[i + 4];
        }
        //应用类型00 普通消费  01卡间转移  02扩展应用  03退卡应用
        pack[40] = (byte) 0x00;
        //扣费金额  4位  01 02 填0    44
        pack[41] = (byte) 0x00;
        pack[42] = (byte) 0x00;
        pack[43] = (byte) 0x00;
        pack[44] = (byte) 0x01;
        //物理卡信息1  32位，读卡操作返回数据
        int index_car_1 = 35;
        for (int i = 0; i < 32; i++) {
            pack[index_car_1 + i] = cmd[i + 21];
        }
        //扩展应用流水  8位
        int index_car_2 = 76;
        for (int i = 0; i < 8; i++) {
            pack[index_car_2 + i] = (byte) 0x00;
        }
        //RFU 扩展位，暂时没用，填充0x00 39
        int index_car_3 = 84;
        for (int i = 0; i < 39; i++) {
            pack[index_car_3 + i] = (byte) 0x00;
        }
        //校验码4位
        pack[124] = (byte) 0x00;
        pack[125] = (byte) 0x00;
        pack[126] = (byte) 0x00;
        byte sum = 0;
        for (int i = 0; i < 128; i++) {//校验和
            sum ^= pack[i];
//            pack[i] ^= pack[i+1];
//            Log.e("pack_info_sum",sum+"  ==  "+i);
        }
        pack[127] = (byte) sum;
        Log.e("socket_1042_test_sign", test_info + "");
        StringBuilder sb = new StringBuilder();//非线程安全
        for (int i = 0; i < pack.length; i++) {
            sb.append(SerialDataUtils.Byte2Hex((pack[i]))).append(" ");
        }
        Log.e("TAG", "sendCmds_car： " + sb);
        return pack;
    }

    private static byte[] key_des = null;
    private static byte[] pack_order = null;

    private static byte[] check_pay_sign_1(byte[] cmd) {
        Log.e("check_pay_sign_1", " =========" + cmd.length);
        pack_order = new byte[6];//存储握手流水号
        byte[] pack = new byte[160];
        pack[0] = (byte) 0xD0;
        pack[1] = (byte) 0xBA;                                           //包头D0 BA 2字节
        pack[2] = (byte) 0x10;
        pack[3] = (byte) 0x05;                                          //len 2字节
        pack[4] = (byte) 0x04;                                          //加密算法 1字节
        pack[5] = (byte) 0x80;                                          //排序方式 1字节
        pack[6] = (byte) 0x00;
        pack[7] = (byte) 0x98;                                          //报文长度 2字节
        //握手流水号，前两个填充FF，后面字节PASM卡号  8位
        pack[8] = (byte) 0xFF;
        pack[9] = (byte) 0xFF;
        for (int i = 0; i < 6; i++) {
            pack[10 + i] = (byte) cmd[i + 4];
            pack_order[i] = cmd[i + 4];
        }
        Log.e("order_1", bytesToHexString(pack_order));
        //报文序号 保留 填充0x00
        pack[16] = (byte) 0x00;
        pack[17] = (byte) 0x00;
        pack[18] = (byte) 0x00;
        pack[19] = (byte) 0x00;
        //RFU1 保留  填充0x00
        pack[20] = (byte) 0x00;
        pack[21] = (byte) 0x00;
        //DE1 签到1数据 128位
        for (int i = 0; i < 128; i++) {
            pack[22 + i] = (byte) cmd[i + 10];
        }
        //RFU2 保留  填充0x00
        pack[150] = (byte) 0x00;
        pack[151] = (byte) 0x00;
        //MAC  6-10 km DES运算
        //8个一组进行加密 ，上次加密的结果为下个加密的密钥
        //下一次密钥存储
        key_des = new byte[8];
//        List mList = Arrays.asList(pack);
//        byte[] mList = new byte[8];
        List mList = new ArrayList();
        for (int i = 0; i < pack.length - 8; i++) {
            mList.add(pack[i]);
        }
//        00 00 00 00 00 00 6E  7D56EC6BF8E1EB
        List<List<Byte>> mEndList = new ArrayList<>();
        //把数据切割成 8个字节数组 用于加密
        for (int i = 0; i < 18; i++) {
            mEndList.add(mList.subList(8 * (i + 1), (8 * (i + 2))));
            Log.e("mlist +  " + i, bytesToHexString(listTobyte(mEndList.get(i))));
        }
        //第一次加密数据
        byte[] key_des_1 = new byte[8];
        key_des[0] = (byte) 0x00;
        key_des[1] = (byte) 0x00;
        key_des[2] = (byte) 0x00;
        key_des[3] = (byte) 0x00;
        key_des[4] = (byte) 0x00;
        key_des[5] = (byte) 0x00;
        key_des[6] = (byte) 0x00;
        key_des[7] = (byte) 0x00;
        //KM 密钥
        byte[] key_km = new byte[8];
        key_km[0] = (byte) Constant.PasswordNumber1;
        key_km[1] = (byte) Constant.PasswordNumber2;
        key_km[2] = (byte) Constant.PasswordNumber3;
        key_km[3] = (byte) Constant.PasswordNumber4;
        key_km[4] = (byte) Constant.PasswordNumber5;
        key_km[5] = (byte) Constant.PasswordNumber6;
        key_km[6] = (byte) Constant.PasswordNumber7;
        key_km[7] = (byte) Constant.PasswordNumber8;
        try {
            key_des = Des.enCrypto_2(key_des_1, key_km);
//            Log.e("key_des_info1111",bytesToHexString(Des.enCrypto_2(key_des_1, key_km)));
//            Log.e("key_des_info1111",bytesToHexString(Des.enCrypto_2(listTobyte(mEndList.get(0)), key_1)));
        } catch (Exception e) {
        }

        for (int i = 0; i < 18; i++) {
            try {
//                if (i == 0) {
//                    key_des = Des.enCrypto_2(listTobyte(mEndList.get(0)), key_1);
//                } else {
//                Log.e("key_des_info+  "+i ,bytesToHexString(key_des)+"  上一个加密密钥====>>  "+bytesToHexString(Des.enCrypto_2(listTobyte(mEndList.get(i)),key_des))+"  新密钥====>>  "+bytesToHexString(listTobyte(mEndList.get(i)))+"  == 加密数据");
                key_des = Des.enCrypto_2(listTobyte(mEndList.get(i)), key_des);
//                }

            } catch (Exception e) {
            }
        }
//        Des.enCrypto_2(listTobyte(mEndList.get(i)),key_des)
        //MAC 加密数据结果key_des
//        pack[151] = (byte) 0x00;
        for (int i = 0; i < 8; i++) {
            pack[152 + i] = key_des[i];
        }
        Log.e("body_des0", bytesToHexString(pack));
        //数据体 7-11进行DES加密
        byte[] body = new byte[144];
        for (int i = 0; i < 144; i++) {
            body[i] = pack[16 + i];
        }
        Log.e("body_des1", bytesToHexString(body));
        try {
            //7-11 des加密结果
            byte[] body_des = Des.enCrypto_2(body, key_km);
            int index = 8;
            for (int i = 0; i < body_des.length; i++) {
                pack[16 + i] = body_des[i];
            }
            Log.e("body_des2", bytesToHexString(body_des));
            Log.e("body_pack", bytesToHexString(pack));
        } catch (Exception e) {
            Log.e("eeee_body", e.getMessage().toString());
        }

//        byte sum = 0;
//        for (int i = 0; i < 128; i++) {//校验和
//            sum ^= pack[i];
//        }
        //D0 BA 10 05 04 80 00 98 FF FF 01 00 99 99 32 40
//        pack[127] = (byte) sum;
        StringBuilder sb = new StringBuilder();//非线程安全
        for (int i = 0; i < pack.length; i++) {
            sb.append(SerialDataUtils.Byte2Hex((pack[i]))).append(" ");
        }
        Log.e("TAG", "sendCmds_car： " + sb);

        return pack;
    }

    //存储所有交易金额
    private static int money_all = 0;
    private static int money_all_upload = 0;

    /**
     * 联机消费授权串口
     *
     * @param cmd
     * @return
     */
    private static byte[] check_pay_agree_1(byte[] cmd, String money) {
        Log.e("test_123__11", " =========" + Constant.Companion.getCAR_SIGN_19().toString());
        Log.e("test_123__22", " =========" + Constant.Companion.getCAR_SIGN_65().toString());
//        byte[] CAR_SIGN_19 = App.Companion.getSpUtil().getString("CAR_SIGN_19","").getBytes();
//        byte[] CAR_SIGN_65 = App.Companion.getSpUtil().getString("CAR_SIGN_65","").getBytes();
        Log.e("PayStringTobyte1", bytesToHexString(PayStringTobyte(1)));
        Log.e("PayStringTobyte2", bytesToHexString(PayStringTobyte(2)));
        money_all = Integer.parseInt(money);
        byte[] CAR_SIGN_19 = new byte[8];
        CAR_SIGN_19 = PayStringTobyte(1);
        //10 D0 C6 60 5E F0 42 5E
//        CAR_SIGN_19[0] = (byte)0x10;
//        CAR_SIGN_19[1] = (byte)0xD0;
//        CAR_SIGN_19[2] = (byte)0xC6;
//        CAR_SIGN_19[3] = (byte)0x60;
//        CAR_SIGN_19[4] = (byte)0xF0;
//        CAR_SIGN_19[5] = (byte)0xF0;
//        CAR_SIGN_19[6] = (byte)0x42;
//        CAR_SIGN_19[7] = (byte)0x5E;
        byte[] CAR_SIGN_65 = new byte[8];
        CAR_SIGN_65 = PayStringTobyte(2);
        //8A A2 3B 61 6D 25 E2 F6
//        CAR_SIGN_65[0] = (byte)0x8A;
//        CAR_SIGN_65[1] = (byte)0xA2;
//        CAR_SIGN_65[2] = (byte)0x3B;
//        CAR_SIGN_65[3] = (byte)0x61;
//        CAR_SIGN_65[4] = (byte)0x6D;
//        CAR_SIGN_65[5] = (byte)0x25;
//        CAR_SIGN_65[6] = (byte)0xE2;
//        CAR_SIGN_65[7] = (byte)0xF6;
        byte[] pack = new byte[128];
        pack[0] = (byte) 0xD0;
        pack[1] = (byte) 0xB0;                                           //包头D0 B0 2字节
        pack[2] = (byte) 0x10;
        pack[3] = (byte) 0x05;                                          //报文版本
        pack[4] = (byte) 0x04;                                          //加密算法 1字节
        pack[5] = (byte) 0x80;                                          //排序方式 1字节
        pack[6] = (byte) 0x00;
        pack[7] = (byte) 0x78;                                          //报文长度 2字节
        //握手流水号，前两个填充FF，后面字节PASM卡号  8位
        pack[8] = (byte) 0xFF;
        pack[9] = (byte) 0xFF;
        for (int i = 0; i < 6; i++) {
            pack[10 + i] = (byte) pack_order[i];
        }
        //01 00 99 99 32 40
        Log.e("agree_pack_order", bytesToHexString(pack_order));
        //报文序号 保留 填充0x00
        pack[16] = (byte) 0x00;
        pack[17] = (byte) 0x00;
        pack[18] = (byte) 0x00;
        pack[19] = (byte) 0x00;
        //RFU1 保留  填充0x00
        pack[20] = (byte) 0x00;
        pack[21] = (byte) 0x00;

        //RFU2 保留  填充0x00
        pack[22] = (byte) 0x00;
        pack[23] = (byte) 0x00;
        pack[24] = (byte) 0x00;
        pack[25] = (byte) 0x00;

        //传送序号 终端自动加1 4位
        int tu5 = Constant.Companion.getCAR_ORDER_NUM();
        byte[] bytes5 = new byte[4];
        bytes5[3] = (byte) (tu5 & 0xFF);
        bytes5[2] = (byte) (tu5 >> 8 & 0xFF);
        bytes5[1] = (byte) (tu5 >> 16 & 0xFF);
        bytes5[0] = (byte) (tu5 >> 24 & 0xFF);
        Log.e("agree_agree_num", bytesToHexString(bytes5));
        pack[26] = (byte) bytes5[0];
        pack[27] = (byte) bytes5[1];
        pack[28] = (byte) bytes5[2];
        pack[29] = (byte) bytes5[3];
        //终端编号
        pack[30] = (byte) Constant.PortNumber1;
        ;
        pack[31] = (byte) Constant.PortNumber2;
        ;
        pack[32] = (byte) Constant.PortNumber3;
        ;
        pack[33] = (byte) Constant.PortNumber4;
        ;
        pack[34] = (byte) Constant.PortNumber5;
        ;
        pack[35] = (byte) Constant.PortNumber6;
        ;
        //交易类型  0A = 消费
        pack[36] = (byte) 0x0A;
        //交易查询信息
        for (int i = 0; i < 52; i++) {
            pack[37 + i] = (byte) cmd[i + 4];
        }
        //交易金额  高位在前  4位 分
//        pack[89] = byte4ByInt(1)[0];
//        pack[90] = byte4ByInt(1)[1];
//        pack[91] = byte4ByInt(1)[2];
//        pack[92] = byte4ByInt(1)[3];
//        pack[89] = (byte) 0x00;
//        pack[90] = (byte) 0x00;
//        pack[91] = (byte) 0x00;
//        pack[92] = (byte) 0x02;
        byte[] money_byte = byte4ByInt(Integer.parseInt(money));
        pack[89] = (byte) money_byte[0];
        pack[90] = (byte) money_byte[1];
        pack[91] = (byte) money_byte[2];
        pack[92] = (byte) money_byte[3];

        //RFU 保留  填充0x00  27位
        for (int i = 0; i < 27; i++) {
            pack[93 + i] = (byte) 0x00;
        }
        //MAC  6-15密钥运算
        pack[120] = 0x11;
        pack[121] = 0x11;
        pack[122] = 0x11;
        pack[123] = 0x11;
        pack[124] = 0x11;
        pack[125] = 0x11;
        pack[126] = 0x11;
        pack[127] = 0x11;

        //MAC  6-10 km DES运算
        //8个一组进行加密 ，上次加密的结果为下个加密的密钥
        //下一次密钥存储
        key_des = new byte[8];
//        List mList = Arrays.asList(pack);
//        byte[] mList = new byte[8];
        List mList = new ArrayList();
        for (int i = 0; i < pack.length - 8; i++) {
            mList.add(pack[i]);
        }
//        00 00 00 00 00 00 6E  7D56EC6BF8E1EB
        List<List<Byte>> mEndList = new ArrayList<>();
        //把数据切割成 8个字节数组 用于加密
        for (int i = 0; i < 14; i++) {
            mEndList.add(mList.subList(8 * (i + 1), (8 * (i + 2))));
        }
        //第一次加密数据
        byte[] key_des_1 = new byte[8];
        key_des[0] = (byte) 0x00;
        key_des[1] = (byte) 0x00;
        key_des[2] = (byte) 0x00;
        key_des[3] = (byte) 0x00;
        key_des[4] = (byte) 0x00;
        key_des[5] = (byte) 0x00;
        key_des[6] = (byte) 0x00;
        key_des[7] = (byte) 0x00;
        //K19 密钥
        try {
            key_des = Des.enCrypto_2(key_des_1, CAR_SIGN_19);
        } catch (Exception e) {
        }

        for (int i = 0; i < 14; i++) {
            try {
//                Log.e("agree_key_des_info+112 +26  "+i ,bytesToHexString(key_des)+"  上一个加密密钥====>>  "+bytesToHexString(Des.enCrypto_2(listTobyte(mEndList.get(i)),key_des))+"  新密钥====>>  "+bytesToHexString(listTobyte(mEndList.get(i)))+"  == 加密数据");
                key_des = Des.enCrypto_2(listTobyte(mEndList.get(i)), key_des);
            } catch (Exception e) {
            }
        }
//        Des.enCrypto_2(listTobyte(mEndList.get(i)),key_des)
        //MAC 加密数据结果key_des
//        pack[151] = (byte) 0x00;
        for (int i = 0; i < 8; i++) {
            pack[120 + i] = key_des[i];
        }
        Log.e("agree_body_des0", bytesToHexString(pack));
        //数据体 7-11进行DES加密
        byte[] body = new byte[112];
        for (int i = 0; i < 112; i++) {
            body[i] = pack[16 + i];
        }
        Log.e("agree_body_des1", bytesToHexString(body));
        try {
            //7-11 des加密结果  k65
            byte[] body_des = Des.enCrypto_2(body, CAR_SIGN_65);
            int index = 8;
            for (int i = 0; i < body_des.length; i++) {
                pack[16 + i] = body_des[i];
            }
            /*
            00 00 00 00 00 00 00 00 00 00 00 00 00 01 88 01 20 00 11 11 0A 01 01 72 06 13 00 00 00 00
            51 00 00 05 00 11 21 12 00 00 00 00 00 01 03 00 00 01 86 A0 00 00 00 00 28 00 04 00 00 00
            00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 00 00 00 00
            00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 38 79 6A 28 CF 92 D2 9F
             */
            Log.e("agree_body_des2", bytesToHexString(deCrypto_2(body_des, CAR_SIGN_65)));
            Log.e("agree_body_pack", bytesToHexString(pack));
        } catch (Exception e) {
            Log.e("agree_eeee_body", e.getMessage().toString());
        }

//        byte sum = 0;
//        for (int i = 0; i < 128; i++) {//校验和
//            sum ^= pack[i];
//        }
        //D0 BA 10 05 04 80 00 98 FF FF 01 00 99 99 32 40
//        pack[127] = (byte) sum;
        StringBuilder sb = new StringBuilder();//非线程安全
        for (int i = 0; i < pack.length; i++) {
            sb.append(SerialDataUtils.Byte2Hex((pack[i]))).append(" ");
        }
        Log.e("TAG", pack.length + " == agree_sendCmds_car： " + sb);

        return pack;
    }

    /**
     * 联机消费 上传交易数据串口
     *
     * @param cmd
     * @return
     */

    //存储交易数据 ， 用于A8 批量交易数据上传
    private static String A8_upload_item = "";
    //临时存储，用于A8数据上传
    private static byte[] A8_byte_nfo = null;

    private static byte[] check_pay_upload_M12(byte[] cmd) {
        Log.e("M1_123__load", " =========" + bytesToHexString(cmd));
        Log.e("M1_PayStringTobyte1", bytesToHexString(PayStringTobyte(1)));
        Log.e("M1_PayStringTobyte2", bytesToHexString(PayStringTobyte(2)));
        byte[] CAR_SIGN_19 = new byte[8];
        CAR_SIGN_19 = PayStringTobyte(1);
        byte[] CAR_SIGN_65 = new byte[8];
        CAR_SIGN_65 = PayStringTobyte(2);
        byte[] pack = new byte[176];
        pack[0] = (byte) 0xD0;
        pack[1] = (byte) 0xB1;                                           //包头D0 B0 2字节
        pack[2] = (byte) 0x10;
        pack[3] = (byte) 0x05;                                          //报文版本
        pack[4] = (byte) 0x04;                                          //加密算法 1字节
        pack[5] = (byte) 0x80;                                          //排序方式 1字节
        pack[6] = (byte) 0x00;
        pack[7] = (byte) 0xA8;                                          //报文长度 2字节
        //握手流水号，前两个填充FF，后面字节PASM卡号  8位
        pack[8] = (byte) 0xFF;
        pack[9] = (byte) 0xFF;
        for (int i = 0; i < 6; i++) {
            pack[10 + i] = (byte) pack_order[i];
        }
        Log.e("M1_pack_load", bytesToHexString(pack_order));
        //报文序号 保留 填充0x00
        pack[16] = (byte) 0x00;
        pack[17] = (byte) 0x00;
        pack[18] = (byte) 0x00;
        pack[19] = (byte) 0x00;
        //RFU1 保留  填充0x00
        pack[20] = (byte) 0x00;
        pack[21] = (byte) 0x00;

        //RFU2 保留  填充0x00
        pack[22] = (byte) 0x00;
        pack[23] = (byte) 0x00;
        pack[24] = (byte) 0x00;
        pack[25] = (byte) 0x00;

        //传送序号 终端自动加1 4位
        int tu5 = Constant.Companion.getCAR_ORDER_NUM();
        byte[] bytes5 = new byte[4];
        bytes5[3] = (byte) (tu5 & 0xFF);
        bytes5[2] = (byte) (tu5 >> 8 & 0xFF);
        bytes5[1] = (byte) (tu5 >> 16 & 0xFF);
        bytes5[0] = (byte) (tu5 >> 24 & 0xFF);
        Log.e("M1_agree_num", bytesToHexString(bytes5));
        pack[26] = (byte) bytes5[0];
        pack[27] = (byte) bytes5[1];
        pack[28] = (byte) bytes5[2];
        pack[29] = (byte) bytes5[3];
        //终端编号
        pack[30] = (byte) Constant.PortNumber1;
        ;
        pack[31] = (byte) Constant.PortNumber2;
        ;
        pack[32] = (byte) Constant.PortNumber3;
        ;
        pack[33] = (byte) Constant.PortNumber4;
        ;
        pack[34] = (byte) Constant.PortNumber5;
        ;
        pack[35] = (byte) Constant.PortNumber6;
        ;
        //===========
        //交易状态信息  上传数据 拼接
        pack[36] = (byte) 0x00;                  //交易数据格式
        //系统流水号  8位
        //系统授权码  8位
        //交易批次号  8位
        //到 票卡交易数
        byte[] test_24 = new byte[24];

        for (int i = 0; i < 24; i++) {
            pack[37 + i] = (byte) system_order_b0[i + 28];
            test_24[i] = system_order_b0[i + 28];
        }
        Log.e("system_order_b0", bytesToHexString(system_order_b0));
        Log.e("system_order_b01", bytesToHexString(test_24));
        pack[61] = (byte) 0x01;                  //交易数据格式
//        pack[62] = (byte) bytes5[0];
//        pack[63] = (byte) bytes5[1];
//        pack[64] = (byte) bytes5[2];
//        pack[65] = (byte) bytes5[3];             //脱机交易流水号，自定义

        pack[62] = (byte) cmd[4];
        pack[63] = (byte) cmd[5];
        pack[64] = (byte) cmd[6];
        pack[65] = (byte) cmd[7];             //脱机交易流水号，自定义

        //票卡号（逻辑卡号）  8位    票物理卡号（物理卡号） 4位
        //到 本地金额 42
        for (int i = 0; i < 42; i++) {
            pack[66 + i] = (byte) cmd[i + 8];
        }
        byte[] hex_money = new byte[4];
        hex_money[0] = pack[100];
        hex_money[1] = pack[101];
        hex_money[2] = pack[102];
        hex_money[3] = pack[103];
        byte[] hex_ye = new byte[4];
        hex_ye[0] = pack[104];
        hex_ye[1] = pack[105];
        hex_ye[2] = pack[106];
        hex_ye[3] = pack[107];
        Log.e("hex_moey_ye", bytesToHexString(hex_money) + " == " + bytesToHexString(hex_ye));
        Log.e("hex_moey_ye1", intTointByte(BCD4(hex_ye)[2]) + "");
        int[] int_money = BCD4(hex_money);
        int[] int_ye = BCD4(hex_ye);
        pack[100] = (byte) intTointByte(int_money[0]);
        pack[101] = (byte) intTointByte(int_money[1]);
        pack[102] = (byte) intTointByte(int_money[2]);
        pack[103] = (byte) intTointByte(int_money[3]);
        pack[104] = (byte) intTointByte(int_ye[0]);
        pack[105] = (byte) intTointByte(int_ye[1]);
        pack[106] = (byte) intTointByte(int_ye[2]);
        pack[107] = (byte) intTointByte(int_ye[3]);

        //交易类型
        pack[108] = cmd[54];
        //票卡交易数
        pack[109] = (byte) 0x00;
        pack[110] = cmd[55];
        pack[111] = cmd[56];
        byte[] hex_pk = new byte[4];
        hex_pk[0] = 0x00;
        hex_pk[1] = pack[109];
        hex_pk[2] = pack[110];
        hex_pk[3] = pack[111];
        int[] int_pk = BCD4(hex_pk);
        pack[109] = (byte) intTointByte(int_pk[1]);
        pack[110] = (byte) intTointByte(int_pk[2]);
        pack[111] = (byte) intTointByte(int_pk[3]);
        //本次交易入口设备编号  4位  入口交易日期 7 位
        for (int i = 0; i < 11; i++) {
            pack[112 + i] = (byte) cmd[i + 62];
        }
        //分账信息  ’00‘ 字符串  2位
//        pack[123] = (byte) 0x00;
//        pack[124] = (byte) 0x00;
//        //校验码   ’0‘ 字符串  1位
//        pack[125] = (byte) 0x00;
        //分账信息  ’00‘ 字符串  2位
        pack[123] = (byte) 0x30;
        pack[124] = (byte) 0x30;
        //校验码   ’0‘ 字符串  1位
        pack[125] = (byte) 0x30;
        //交易验证码  4 位
        for (int i = 0; i < 4; i++) {
            pack[126 + i] = (byte) cmd[i + 73];
        }
//        pack[62] -  pack[129]

        //填充数据  19位
        for (int i = 0; i < 19; i++) {
            pack[130 + i] = (byte) 0x00;
        }
//        pack[127]
        //填充数据  19位
        for (int i = 0; i < 19; i++) {
            pack[149 + i] = (byte) 0x00;
        }
        //MAC
        pack[168] = (byte) 0x11;
        pack[169] = (byte) 0x11;
        pack[170] = (byte) 0x11;
        pack[171] = (byte) 0x11;
        pack[172] = (byte) 0x11;
        pack[173] = (byte) 0x11;
        pack[174] = (byte) 0x11;
        pack[175] = (byte) 0x11;
        Log.e("M1_body_des_pack", bytesToHexString(pack));
        //暂时存储 68位数据 用于A8数据上传  拼接
        A8_byte_nfo = new byte[69];
        for (int i = 0; i < A8_byte_nfo.length; i++) {
            A8_byte_nfo[i] = (byte) pack[i + 61];
        }

        //02 01 00 99 99 32 40 00 00 00 AD 20 20 07 27 19 47 01 51 00 00 07 50 00 77 63 F1 61 06 13 0E 9E F9 21 00 00 00 02 00 00 00 02 00 00 04 08 06 17 00 15 00 09 00 00 00 00 00 01 00 99 99 32 40 20 20 07 27 19 47 01 01 00 99 99 32 40 07 27 19 42 01 03 00 FF 00 00 00 00
//        mActivity.upload_cz_info(bytesToHexString(A8_byte_nfo));
        //MAC  6-10 km DES运算
        //8个一组进行加密 ，上次加密的结果为下个加密的密钥
        //下一次密钥存储
        key_des = new byte[8];
        List mList = new ArrayList();
        for (int i = 0; i < pack.length - 8; i++) {
            mList.add(pack[i]);
        }
        List<List<Byte>> mEndList = new ArrayList<>();
        //把数据切割成 8个字节数组 用于加密
        for (int i = 0; i < 20; i++) {
            mEndList.add(mList.subList(8 * (i + 1), (8 * (i + 2))));
        }
        //第一次加密数据
        byte[] key_des_1 = new byte[8];
        key_des[0] = (byte) 0x00;
        key_des[1] = (byte) 0x00;
        key_des[2] = (byte) 0x00;
        key_des[3] = (byte) 0x00;
        key_des[4] = (byte) 0x00;
        key_des[5] = (byte) 0x00;
        key_des[6] = (byte) 0x00;
        key_des[7] = (byte) 0x00;
        //K19 密钥
        try {
            key_des = Des.enCrypto_2(key_des_1, CAR_SIGN_19);
        } catch (Exception e) {
        }
        //160位
        for (int i = 0; i < 20; i++) {
            try {
                Log.e("up_key_des  " + i, bytesToHexString(key_des) + "  上一个加密密钥====>>  " + bytesToHexString(Des.enCrypto_2(listTobyte(mEndList.get(i)), key_des)) + "  新密钥====>>  " + bytesToHexString(listTobyte(mEndList.get(i))) + "  == 加密数据");
                key_des = Des.enCrypto_2(listTobyte(mEndList.get(i)), key_des);
            } catch (Exception e) {
            }
        }
//        Des.enCrypto_2(listTobyte(mEndList.get(i)),key_des)
        //MAC 加密数据结果key_des
        for (int i = 0; i < 8; i++) {
            pack[168 + i] = key_des[i];
        }
        Log.e("M1_body_des0", bytesToHexString(pack));
        //数据体 7-11进行DES加密
        byte[] body = new byte[160];
        for (int i = 0; i < 160; i++) {
            body[i] = pack[16 + i];
        }
        Log.e("M1_body_des1", bytesToHexString(body));
        try {
            //7-11 des加密结果  k65
            byte[] body_des = Des.enCrypto_2(body, CAR_SIGN_65);
            int index = 8;
            for (int i = 0; i < body_des.length; i++) {
                pack[16 + i] = body_des[i];
            }
            Log.e("M1_body_des2", bytesToHexString(deCrypto_2(body_des, CAR_SIGN_65)));
            Log.e("M1_body_pack", bytesToHexString(pack));
        } catch (Exception e) {
            Log.e("M1_eeee_body", e.getMessage().toString());
        }
        StringBuilder sb = new StringBuilder();//非线程安全
        for (int i = 0; i < pack.length; i++) {
            sb.append(SerialDataUtils.Byte2Hex((pack[i]))).append(" ");
        }
        Log.e("TAG", pack.length + " == M1_sendCmds_car： " + sb);

        return pack;
    }

    /**
     * 联机消费 上传交易数据串口
     *
     * @param cmd
     * @return
     */

    private static byte[] check_pay_upload_CPU(byte[] cmd) {
        Log.e("CPU_123__load", " =========" + bytesToHexString(cmd));
        Log.e("CPU_PayStringTobyte1", bytesToHexString(PayStringTobyte(1)));
        Log.e("CPU_PayStringTobyte2", bytesToHexString(PayStringTobyte(2)));
        byte[] CAR_SIGN_19 = new byte[8];
        CAR_SIGN_19 = PayStringTobyte(1);
        byte[] CAR_SIGN_65 = new byte[8];
        CAR_SIGN_65 = PayStringTobyte(2);
        byte[] pack = new byte[176];
        pack[0] = (byte) 0xD0;
        pack[1] = (byte) 0xB1;                                           //包头D0 B0 2字节
        pack[2] = (byte) 0x10;
        pack[3] = (byte) 0x05;                                          //报文版本
        pack[4] = (byte) 0x04;                                          //加密算法 1字节
        pack[5] = (byte) 0x80;                                          //排序方式 1字节
        pack[6] = (byte) 0x00;
        pack[7] = (byte) 0xA8;                                          //报文长度 2字节
        //握手流水号，前两个填充FF，后面字节PASM卡号  8位
        pack[8] = (byte) 0xFF;
        pack[9] = (byte) 0xFF;
        for (int i = 0; i < 6; i++) {
            pack[10 + i] = (byte) pack_order[i];
        }
        Log.e("CPU_pack_load", bytesToHexString(pack_order));
        //报文序号 保留 填充0x00
        pack[16] = (byte) 0x00;
        pack[17] = (byte) 0x00;
        pack[18] = (byte) 0x00;
        pack[19] = (byte) 0x00;
        //RFU1 保留  填充0x00
        pack[20] = (byte) 0x00;
        pack[21] = (byte) 0x00;

        //RFU2 保留  填充0x00
        pack[22] = (byte) 0x00;
        pack[23] = (byte) 0x00;
        pack[24] = (byte) 0x00;
        pack[25] = (byte) 0x00;

        //传送序号 终端自动加1 4位
        int tu5 = Constant.Companion.getCAR_ORDER_NUM();
        byte[] bytes5 = new byte[4];
        bytes5[3] = (byte) (tu5 & 0xFF);
        bytes5[2] = (byte) (tu5 >> 8 & 0xFF);
        bytes5[1] = (byte) (tu5 >> 16 & 0xFF);
        bytes5[0] = (byte) (tu5 >> 24 & 0xFF);
        Log.e("M1_agree_num", bytesToHexString(bytes5));
        pack[26] = (byte) bytes5[0];
        pack[27] = (byte) bytes5[1];
        pack[28] = (byte) bytes5[2];
        pack[29] = (byte) bytes5[3];
        //终端编号
        pack[30] = (byte) Constant.PortNumber1;
        ;
        pack[31] = (byte) Constant.PortNumber2;
        ;
        pack[32] = (byte) Constant.PortNumber3;
        ;
        pack[33] = (byte) Constant.PortNumber4;
        ;
        pack[34] = (byte) Constant.PortNumber5;
        ;
        pack[35] = (byte) Constant.PortNumber6;
        ;
        //===========
        //交易状态信息  上传数据 拼接
        pack[36] = (byte) 0x00;                  //交易数据格式,00成功，0F失败  0A黑名单  09保留未用
        //系统流水号  8位
        //系统授权码  8位
        //交易批次号  8位
        //到 票卡交易数
        byte[] test_24 = new byte[24];

        for (int i = 0; i < 24; i++) {
            pack[37 + i] = (byte) system_order_b0[i + 28];
            test_24[i] = system_order_b0[i + 28];
        }
        Log.e("system_order_b0", bytesToHexString(system_order_b0));
        Log.e("system_order_b01", bytesToHexString(test_24));
        pack[61] = (byte) 0x02;                  //交易数据格式  2  CPU
        for (int i = 0; i < 87; i++) {
            pack[62 + i] = (byte) cmd[i + 4];
        }
        A8_byte_nfo = new byte[88];
        for (int i = 0; i < A8_byte_nfo.length; i++) {
            A8_byte_nfo[i] = (byte) pack[i + 61];
        }
        //填充数据  19位
        for (int i = 0; i < 19; i++) {
            pack[149 + i] = (byte) 0x00;
        }
        //MAC
        pack[168] = (byte) 0x11;
        pack[169] = (byte) 0x11;
        pack[170] = (byte) 0x11;
        pack[171] = (byte) 0x11;
        pack[172] = (byte) 0x11;
        pack[173] = (byte) 0x11;
        pack[174] = (byte) 0x11;
        pack[175] = (byte) 0x11;
        Log.e("CPU_body_des0", bytesToHexString(pack));
        //MAC  6-10 km DES运算
        //8个一组进行加密 ，上次加密的结果为下个加密的密钥
        //下一次密钥存储
        key_des = new byte[8];
        List mList = new ArrayList();
        for (int i = 0; i < pack.length - 8; i++) {
            mList.add(pack[i]);
        }
        List<List<Byte>> mEndList = new ArrayList<>();
        //把数据切割成 8个字节数组 用于加密
        for (int i = 0; i < 20; i++) {
            mEndList.add(mList.subList(8 * (i + 1), (8 * (i + 2))));
        }
        //第一次加密数据
        byte[] key_des_1 = new byte[8];
        key_des[0] = (byte) 0x00;
        key_des[1] = (byte) 0x00;
        key_des[2] = (byte) 0x00;
        key_des[3] = (byte) 0x00;
        key_des[4] = (byte) 0x00;
        key_des[5] = (byte) 0x00;
        key_des[6] = (byte) 0x00;
        key_des[7] = (byte) 0x00;
        //K19 密钥
        try {
            key_des = Des.enCrypto_2(key_des_1, CAR_SIGN_19);
        } catch (Exception e) {
        }
        //160位
        for (int i = 0; i < 20; i++) {
            try {
//                Log.e("up_key_des  " + i, bytesToHexString(key_des) + "  上一个加密密钥====>>  " + bytesToHexString(Des.enCrypto_2(listTobyte(mEndList.get(i)), key_des)) + "  新密钥====>>  " + bytesToHexString(listTobyte(mEndList.get(i))) + "  == 加密数据");
                key_des = Des.enCrypto_2(listTobyte(mEndList.get(i)), key_des);
            } catch (Exception e) {
            }
        }
//        Des.enCrypto_2(listTobyte(mEndList.get(i)),key_des)
        //MAC 加密数据结果key_des
        for (int i = 0; i < 8; i++) {
            pack[168 + i] = key_des[i];
        }
        Log.e("M1_body_des0", bytesToHexString(pack));
        //数据体 7-11进行DES加密
        byte[] body = new byte[160];
        for (int i = 0; i < 160; i++) {
            body[i] = pack[16 + i];
        }
        Log.e("M1_body_des1", bytesToHexString(body));
        try {
            //7-11 des加密结果  k65
            byte[] body_des = Des.enCrypto_2(body, CAR_SIGN_65);
            int index = 8;
            for (int i = 0; i < body_des.length; i++) {
                pack[16 + i] = body_des[i];
            }
            Log.e("M1_body_des2", bytesToHexString(deCrypto_2(body_des, CAR_SIGN_65)));
            Log.e("M1_body_pack", bytesToHexString(pack));
        } catch (Exception e) {
            Log.e("M1_eeee_body", e.getMessage().toString());
        }
        StringBuilder sb = new StringBuilder();//非线程安全
        for (int i = 0; i < pack.length; i++) {
            sb.append(SerialDataUtils.Byte2Hex((pack[i]))).append(" ");
        }
        Log.e("TAG", pack.length + " == M1_sendCmds_car： " + sb);

        return pack;
    }

    private static byte[] check_pay_upload_Black(byte[] cmd) {
        Log.e("black_123__load", " =========" + bytesToHexString(cmd));
        Log.e("black_PayStringTobyte1", bytesToHexString(PayStringTobyte(1)));
        Log.e("black_PayStringTobyte2", bytesToHexString(PayStringTobyte(2)));
        byte[] CAR_SIGN_19 = new byte[8];
        CAR_SIGN_19 = PayStringTobyte(1);

        byte[] CAR_SIGN_65 = new byte[8];
        CAR_SIGN_65 = PayStringTobyte(2);
        byte[] pack = new byte[176];
        pack[0] = (byte) 0xD0;
        pack[1] = (byte) 0xB1;                                           //包头D0 B0 2字节
        pack[2] = (byte) 0x10;
        pack[3] = (byte) 0x05;                                          //报文版本
        pack[4] = (byte) 0x04;                                          //加密算法 1字节
        pack[5] = (byte) 0x80;                                          //排序方式 1字节
        pack[6] = (byte) 0x00;
        pack[7] = (byte) 0xA8;                                          //报文长度 2字节
        //握手流水号，前两个填充FF，后面字节PASM卡号  8位
        pack[8] = (byte) 0xFF;
        pack[9] = (byte) 0xFF;
        for (int i = 0; i < 6; i++) {
            pack[10 + i] = (byte) pack_order[i];
        }
        Log.e("black_pack_load", bytesToHexString(pack_order));
        //报文序号 保留 填充0x00
        pack[16] = (byte) 0x00;
        pack[17] = (byte) 0x00;
        pack[18] = (byte) 0x00;
        pack[19] = (byte) 0x00;
        //RFU1 保留  填充0x00
        pack[20] = (byte) 0x00;
        pack[21] = (byte) 0x00;

        //RFU2 保留  填充0x00
        pack[22] = (byte) 0x00;
        pack[23] = (byte) 0x00;
        pack[24] = (byte) 0x00;
        pack[25] = (byte) 0x00;

        //传送序号 终端自动加1 4位
        int tu5 = Constant.Companion.getCAR_ORDER_NUM();
        byte[] bytes5 = new byte[4];
        bytes5[3] = (byte) (tu5 & 0xFF);
        bytes5[2] = (byte) (tu5 >> 8 & 0xFF);
        bytes5[1] = (byte) (tu5 >> 16 & 0xFF);
        bytes5[0] = (byte) (tu5 >> 24 & 0xFF);
        Log.e("M1_agree_num", bytesToHexString(bytes5));
        pack[26] = (byte) bytes5[0];
        pack[27] = (byte) bytes5[1];
        pack[28] = (byte) bytes5[2];
//        pack[29] = (byte) bytes5[3];
        pack[29] = (byte) 1;
        //终端编号
        pack[30] = (byte) Constant.PortNumber1;
        ;
        pack[31] = (byte) Constant.PortNumber2;
        ;
        pack[32] = (byte) Constant.PortNumber3;
        ;
        pack[33] = (byte) Constant.PortNumber4;
        ;
        pack[34] = (byte) Constant.PortNumber5;
        ;
        pack[35] = (byte) Constant.PortNumber6;
        ;
        //===========
        //交易状态信息  上传数据 拼接
        pack[36] = (byte) 0x00;                  //交易数据格式,00成功，0F失败  0A黑名单  09保留未用
        //系统流水号  8位
        //系统授权码  8位
        //交易批次号  8位
        //到 票卡交易数
        byte[] test_24 = new byte[24];

        for (int i = 0; i < 24; i++) {
            pack[37 + i] = (byte) system_order_b0[i + 28];
            test_24[i] = system_order_b0[i + 28];
        }
        Log.e("system_order_b0", bytesToHexString(system_order_b0));
        Log.e("system_order_b01", bytesToHexString(test_24));
        pack[61] = (byte) 0x01;                  //交易数据格式
//        pack[62] = (byte) bytes5[0];
//        pack[63] = (byte) bytes5[1];
//        pack[64] = (byte) bytes5[2];
//        pack[65] = (byte) bytes5[3];             //脱机交易流水号，自定义
        pack[62] = (byte) 0x00;
        pack[63] = (byte) 0x00;
        pack[64] = (byte) 0x00;
        pack[65] = (byte) 0x00;             //脱机交易流水号，自定义

        //票卡号（逻辑卡号）  8位    票物理卡号（物理卡号） 4位
        //到 本地金额 42
        for (int i = 0; i < 42; i++) {
            pack[66 + i] = (byte) cmd[i + 8];
        }
        //交易类型
        pack[108] = cmd[54];
        //票卡交易数
        pack[109] = (byte) 0x00;
        pack[110] = cmd[55];
        pack[111] = cmd[56];

        //本次交易入口设备编号  4位  入口交易日期 7 位
        for (int i = 0; i < 11; i++) {
            pack[112 + i] = (byte) cmd[i + 62];
        }
        //分账信息  ’00‘ 字符串  2位
        pack[123] = (byte) 0x30;
        pack[124] = (byte) 0x30;
        //校验码   ’0‘ 字符串  1位
        pack[125] = (byte) 0x30;
        //交易验证码  4 位
        for (int i = 0; i < 4; i++) {
            pack[126 + i] = (byte) cmd[i + 74];
        }
        //填充数据  19位
        for (int i = 0; i < 19; i++) {
            pack[130 + i] = (byte) 0x00;
        }
//        pack[127]
        //填充数据  19位
        for (int i = 0; i < 19; i++) {
            pack[149 + i] = (byte) 0x00;
        }
        //MAC
        pack[168] = (byte) 0x11;
        pack[169] = (byte) 0x11;
        pack[170] = (byte) 0x11;
        pack[171] = (byte) 0x11;
        pack[172] = (byte) 0x11;
        pack[173] = (byte) 0x11;
        pack[174] = (byte) 0x11;
        pack[175] = (byte) 0x11;

        //MAC  6-10 km DES运算
        //8个一组进行加密 ，上次加密的结果为下个加密的密钥
        //下一次密钥存储
        key_des = new byte[8];
        List mList = new ArrayList();
        for (int i = 0; i < pack.length - 8; i++) {
            mList.add(pack[i]);
        }
        List<List<Byte>> mEndList = new ArrayList<>();
        //把数据切割成 8个字节数组 用于加密
        for (int i = 0; i < 20; i++) {
            mEndList.add(mList.subList(8 * (i + 1), (8 * (i + 2))));
        }
        //第一次加密数据
        byte[] key_des_1 = new byte[8];
        key_des[0] = (byte) 0x00;
        key_des[1] = (byte) 0x00;
        key_des[2] = (byte) 0x00;
        key_des[3] = (byte) 0x00;
        key_des[4] = (byte) 0x00;
        key_des[5] = (byte) 0x00;
        key_des[6] = (byte) 0x00;
        key_des[7] = (byte) 0x00;
        //K19 密钥
        try {
            key_des = Des.enCrypto_2(key_des_1, CAR_SIGN_19);
        } catch (Exception e) {
        }
        //160位
        for (int i = 0; i < 20; i++) {
            try {
                Log.e("up_key_des  " + i, bytesToHexString(key_des) + "  上一个加密密钥====>>  " + bytesToHexString(Des.enCrypto_2(listTobyte(mEndList.get(i)), key_des)) + "  新密钥====>>  " + bytesToHexString(listTobyte(mEndList.get(i))) + "  == 加密数据");
                key_des = Des.enCrypto_2(listTobyte(mEndList.get(i)), key_des);
            } catch (Exception e) {
            }
        }
//        Des.enCrypto_2(listTobyte(mEndList.get(i)),key_des)
        //MAC 加密数据结果key_des
        for (int i = 0; i < 8; i++) {
            pack[168 + i] = key_des[i];
        }
        Log.e("M1_body_des0", bytesToHexString(pack));
        //数据体 7-11进行DES加密
        byte[] body = new byte[160];
        for (int i = 0; i < 160; i++) {
            body[i] = pack[16 + i];
        }
        Log.e("M1_body_des1", bytesToHexString(body));
        try {
            //7-11 des加密结果  k65
            byte[] body_des = Des.enCrypto_2(body, CAR_SIGN_65);
            int index = 8;
            for (int i = 0; i < body_des.length; i++) {
                pack[16 + i] = body_des[i];
            }
            Log.e("M1_body_des2", bytesToHexString(deCrypto_2(body_des, CAR_SIGN_65)));
            Log.e("M1_body_pack", bytesToHexString(pack));
        } catch (Exception e) {
            Log.e("M1_eeee_body", e.getMessage().toString());
        }
        StringBuilder sb = new StringBuilder();//非线程安全
        for (int i = 0; i < pack.length; i++) {
            sb.append(SerialDataUtils.Byte2Hex((pack[i]))).append(" ");
        }
        Log.e("TAG", pack.length + " == M1_sendCmds_car： " + sb);

        return pack;
    }

    public static byte[] listTobyte(List<Byte> list) {
        if (list == null || list.size() < 0)
            return null;
        byte[] bytes = new byte[list.size()];
        int i = 0;
        Iterator<Byte> iterator = list.iterator();
        while (iterator.hasNext()) {
            bytes[i] = iterator.next();
            i++;
        }
        return bytes;
    }

    public static byte[] byte4ByInt(int money) {
        byte[] bytes5 = new byte[4];
        bytes5[3] = (byte) (money & 0xFF);
        bytes5[2] = (byte) (money >> 8 & 0xFF);
        bytes5[1] = (byte) (money >> 16 & 0xFF);
        bytes5[0] = (byte) (money >> 24 & 0xFF);
        return bytes5;
    }

    public static byte[] PayStringTobyte(int state) {
        String[] manger = null;
        byte[] byte8 = new byte[8];
        if (state == 1) {
            manger = Constant.Companion.getCAR_SIGN_19().toString().split(" ");
        } else {
            manger = Constant.Companion.getCAR_SIGN_65().toString().split(" ");
        }
        List manger_num = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            manger_num.add(manger[i]);
        }
        return getByteInfo(manger_num, 8);
    }

    //获取 PKI 管理卡号
    public static byte[] CZStringTobyte() {
        String[] manger = null;
        manger = Constant.Companion.getPKI_ID().toString().split(" ");
        List manger_num = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            manger_num.add(manger[i]);
        }
        return getByteInfo(manger_num, 4);
    }

    public static byte[] CZStringTobyteORDER() {
        String[] manger = null;
        manger = Constant.Companion.getCZ_SIGN_2_INFO().toString().split(" ");
        List manger_num = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            manger_num.add(manger[i]);
        }
        return getByteInfo(manger_num, 8);
    }

    private static byte[] getByteInfo(List<String> num, int size) {
        byte[] pack = new byte[size];

        for (int i = 0; i < size; i++) {
            int num_info = Integer.valueOf(num.get(i), 16);
            pack[i] = (byte) num_info;
//            test_info += (" " + num.get(i));
        }
//        Log.e("test_sign_in_2", test_info + "");
        return pack;
    }

    //存储系统流水号
    private static byte[] system_order_b0 = null;
    //存储授权码
    private byte[] system_agree_num = null;
    //存储交易批次号
    private byte[] system_pay_num = null;

    //解析数据暂时存储
    private static byte[] des_order_b1 = null;

    public void pay_des_buy(byte[] temp) {
        //des 解密密钥
        byte[] CAR_SIGN_65 = new byte[8];
        int iPacketCode = 0;
        String str;
        CAR_SIGN_65 = PayStringTobyte(2);
        byte[] des_body = new byte[temp.length - 16];
        for (int i = 0; i < (temp.length - 16); i++) {
            des_body[i] = temp[i + 16];
        }

        try {
            Log.e("des_0_buy", des_body.length + "");
            des_body = deCrypto_2(des_body, CAR_SIGN_65);
            Log.e("des_1_buy", des_body.length + "");
            Log.e("des_2_buy", bytesToHexString(des_body));
            system_order_b0 = new byte[des_body.length];
            system_order_b0 = des_body;

            iPacketCode = (int) des_body[4] << 8;
            iPacketCode |= (int) des_body[5];

            switch (iPacketCode) {
                case 0x0001: {
                    //允许消费，购买流程开始
                    showInfo = "读卡成功";
                    showState = 0;
                    break;
                }
                case 0x000A: {
                    //开始黑名单捕获流程
                    showInfo = "000A 系统黑名单卡";
                    showState = 1;
                    break;
                }
                case 0x000E: {
                    showInfo = "000E 禁止消费";
                    showState = 2;
                    break;
                }
                case 0x000D: {
                    showInfo = "000D 卡片超出离线有效期";
                    showState = 3;
                    break;
                }
                case 0xE001: {
                    showInfo = "E001 此卡片未注册";
                    showState = 3;
                    break;
                }
                default: {
                    str = "";
                    str = SerialDataUtils.Bytes2HexString(des_body, 4, 2);
                    showInfo = str + " 其他错误";
                    showState = 2;
                    break;
                }
            }
            handler.sendEmptyMessage(4);
//            if (des_body[4] == (byte) 0x00 && des_body[5] == (byte) 0x01) {
//                //允许消费，购买流程开始
//                showInfo = "读卡成功";
//                showState = 0;
//            } else if (des_body[4] == (byte) 0x00 && des_body[5] == (byte) 0x0A) {
//                //开始黑名单捕获流程
//                showInfo = "黑名单捕获流程";
//                showState = 1;
//            } else if (des_body[4] == (byte) 0x00 && des_body[5] == (byte) 0x0E) {
//                showInfo = "禁止消费";
//                showState = 2;
//            } else if (des_body[4] == (byte) 0x00 && des_body[5] == (byte) 0x0D) {
//                showInfo = "卡片超出离线有效期";
//                showState = 3;
//            } else if (des_body[4] == (byte) 0xE0 && des_body[5] == (byte) 0x01) {
//                showInfo = "此卡片未注册";
//                showState = 3;
//            } else {
//                showInfo = "交易异常，请重新刷卡";
//                showState = 2;
//            }
//            handler.sendEmptyMessage(4);
        } catch (Exception e) {
        }

    }

    private byte[] b4_upload_info = null;

    public void pay_des_upload(byte[] temp, String state) {
        //des 解密密钥
        byte[] CAR_SIGN_65 = new byte[8];
        CAR_SIGN_65 = PayStringTobyte(2);
        byte[] CAR_SIGN_19 = new byte[8];
        CAR_SIGN_19 = PayStringTobyte(1);
        byte[] des_body = new byte[temp.length - 16];
        for (int i = 0; i < (temp.length - 16); i++) {
            des_body[i] = temp[i + 16];
        }
        byte[] key_km = new byte[8];
        key_km[0] = (byte) Constant.PasswordNumber1;
        key_km[1] = (byte) Constant.PasswordNumber2;
        key_km[2] = (byte) Constant.PasswordNumber3;
        key_km[3] = (byte) Constant.PasswordNumber4;
        key_km[4] = (byte) Constant.PasswordNumber5;
        key_km[5] = (byte) Constant.PasswordNumber6;
        key_km[6] = (byte) Constant.PasswordNumber7;
        key_km[7] = (byte) Constant.PasswordNumber8;

        byte[] key_mac = new byte[6];
        //终端编号
        key_mac[0] = (byte) Constant.PortNumber1;
        ;
        key_mac[1] = (byte) Constant.PortNumber2;
        ;
        key_mac[2] = (byte) Constant.PortNumber3;
        ;
        key_mac[3] = (byte) Constant.PortNumber4;
        ;
        key_mac[4] = (byte) Constant.PortNumber5;
        ;
        key_mac[5] = (byte) Constant.PortNumber6;
        ;
        try {
            if (state == "A5") {
                des_body = deCrypto_2(des_body, key_km);
                Log.e("des_2_upload6" + state, bytesToHexString(des_body));
//                upload_ALL_A8(des_body);
                getSendSocketInfo(des_body, 118, "123");
            }

            Log.e("des_2_upload1_65  " + state, bytesToHexString(CAR_SIGN_65));
            Log.e("des_2_upload1_body  " + state, bytesToHexString(des_body));
            des_body = deCrypto_2(des_body, CAR_SIGN_65);
            Log.e("des_2_upload_info_pay", money_all_upload + " == num == " + num_order);
            Log.e("des_2_upload2  " + state, bytesToHexString(des_body));

            Log.e("des_2_upload2 Km " + state, bytesToHexString(des_body));
            if (state == "b4") {
                if (des_body[4] == (byte) 0x00 && des_body[5] == (byte) 0x00) {
                    //b4快速结算成功
                    money_all = 0;
                    num_order = 1;
                    money_all_upload = 0;
                    Constant.Companion.setCAR_ORDER_NUM(num_order);
                    Log.e("des_2_upload3  " + state, bytesToHexString(des_body));
                } else {
                    //b4快速结算失败 使用脱机交易结算
                    showInfo = "数据上传错误";
                    showState = 4;
                    Log.e("des_2_upload4  " + state, bytesToHexString(des_body));
                    Log.e("des_2_upload5  " + state, bytesToHexString(des_body));
//                    upload_A5((num_order)+"",money_all_upload+"");
                    getSendSocketInfo(null, 115, "123");
                    mActivity.upload_b4_wd(bytesToHexString(upload_B4()), money_all_upload + "", (num_order) + "", bytesToHexString(CAR_SIGN_65), bytesToHexString(CAR_SIGN_19), bytesToHexString(key_km), bytesToHexString(key_mac));
                }

            } else {
//                Log.e("des_2_upload  " +state, bytesToHexString(des_body));
                des_order_b1 = new byte[des_body.length];
                des_order_b1 = des_body;
                if (des_body[4] == (byte) 0x00 && des_body[5] == (byte) 0x00) {
                    //提交成功
                    showInfo = "提交成功";
                    showState = 0;
                    handler.sendEmptyMessage(6);
                    mActivity.upload_b4_wd(bytesToHexString(upload_B4()),
                            money_all_upload + "", (num_order) + "",
                            bytesToHexString(CAR_SIGN_65), bytesToHexString(CAR_SIGN_19), bytesToHexString(key_km), bytesToHexString(key_mac));
                } else if (des_body[4] == (byte) 0x00 && des_body[5] == (byte) 0x02) {
                    //此记录已冲正
                    showInfo = "此记录已冲正";
                    showState = 1;
                } else if (des_body[4] == (byte) 0x00 && des_body[5] == (byte) 0x0F) {
                    showInfo = "联机授权错误";
                    showState = 2;
                } else if (des_body[4] == (byte) 0x00 && des_body[5] == (byte) 0x04) {
                    showInfo = "交易金额错误";
                    showState = 3;
                } else if (des_body[4] == (byte) 0xE0 && des_body[5] == (byte) 0x01) {
                    showInfo = "上传数据错误";
                    showState = 3;
                }
                handler.sendEmptyMessageDelayed(5,0);
            }

        } catch (Exception e) {

        }

    }

    public void pay_des_upload_b4(byte[] temp, String state) {
        //des 解密密钥
        byte[] CAR_SIGN_65 = new byte[8];
//        CAR_SIGN_65 = PayStringTobyte(2);
        CAR_SIGN_65 = SerialDataUtils.hexString2Bytes(b4_key);
        byte[] des_body = new byte[temp.length - 16];
        for (int i = 0; i < (temp.length - 16); i++) {
            des_body[i] = temp[i + 16];
        }

        try {
            Log.e("b4des_2_upload1_65  " + state, bytesToHexString(CAR_SIGN_65));
            Log.e("b4des_2_upload1_body  " + state, bytesToHexString(des_body));
            des_body = deCrypto_2(des_body, CAR_SIGN_65);

            Log.e("b4des_2_upload_info_pay", money_all_upload + " == num == " + num_order);
            Log.e("b4des_2_upload2  " + state, bytesToHexString(des_body));
            if (state == "b4") {
                if (des_body[4] == (byte) 0x00 && des_body[5] == (byte) 0x00) {
                    //b4快速结算成功
                    money_all = 0;
                    num_order = 1;
                    money_all_upload = 0;
                    Constant.Companion.setCAR_ORDER_NUM(num_order);
                    Log.e("b4des_2_upload3  " + state, bytesToHexString(des_body));
                } else {
                    //b4快速结算失败 使用脱机交易结算
                    showInfo = "数据上传错误";
                    showState = 4;
                    Log.e("b4des_2_upload5  " + state, bytesToHexString(des_body));
                    upload_A5((num_order) + "", money_all_upload + "");
                }

            }
        } catch (Exception e) {
        }

    }


    /**
     * b4 上传结算串口
     *
     * @return
     */

    public static byte[] upload_B4() {
        Log.e("PayStringTobyte1b4", bytesToHexString(PayStringTobyte(1)));
        Log.e("PayStringTobyte2b4", bytesToHexString(PayStringTobyte(2)));
        //获取系统的日期
        Calendar calendar = Calendar.getInstance();
//年
        int year = calendar.get(Calendar.YEAR);
//月
        int month = calendar.get(Calendar.MONTH) + 1;
//日
        int day = calendar.get(Calendar.DAY_OF_MONTH);
//获取系统时间
//小时
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
//分钟
        int minute = calendar.get(Calendar.MINUTE);
//秒
        int second = calendar.get(Calendar.SECOND);
        byte[] CAR_SIGN_19 = new byte[8];
        CAR_SIGN_19 = PayStringTobyte(1);
        byte[] CAR_SIGN_65 = new byte[8];
        CAR_SIGN_65 = PayStringTobyte(2);
        byte[] pack = new byte[96];
        pack[0] = (byte) 0xD0;
        pack[1] = (byte) 0xB4;                                           //包头D0 B0 2字节
        pack[2] = (byte) 0x10;
        pack[3] = (byte) 0x05;                                          //报文版本
        pack[4] = (byte) 0x04;                                          //加密算法 1字节
        pack[5] = (byte) 0x80;                                          //排序方式 1字节
        pack[6] = (byte) 0x00;
        pack[7] = (byte) 0x58;                                          //报文长度 2字节
        //握手流水号，前两个填充FF，后面字节PASM卡号  8位
        pack[8] = (byte) 0xFF;
        pack[9] = (byte) 0xFF;
        for (int i = 0; i < 6; i++) {
            pack[10 + i] = (byte) pack_order[i];
        }
        //01 00 99 99 32 40
        Log.e("agree_pack_order", bytesToHexString(pack_order));
        //报文序号 保留 填充0x00
        pack[16] = (byte) 0x00;
        pack[17] = (byte) 0x00;
        pack[18] = (byte) 0x00;
        pack[19] = (byte) 0x00;
        //RFU1 保留  填充0x00
        pack[20] = (byte) 0x00;
        pack[21] = (byte) 0x00;

        //RFU2 保留  填充0x00
        pack[22] = (byte) 0x00;
        pack[23] = (byte) 0x00;
        pack[24] = (byte) 0x00;
        pack[25] = (byte) 0x00;

        //传送序号 终端自动加1 4位
//        int tu5 = Constant.Companion.getCAR_ORDER_NUM();
//        byte[] bytes5 = new byte[4];
//        bytes5[3] = (byte) (tu5 & 0xFF);
//        bytes5[2] = (byte) (tu5 >> 8 & 0xFF);
//        bytes5[1] = (byte) (tu5 >> 16 & 0xFF);
//        bytes5[0] = (byte) (tu5 >> 24 & 0xFF);

//        pack[26] = (byte) bytes5[0];
//        pack[27] = (byte) bytes5[1];
//        pack[28] = (byte) bytes5[2];
//        pack[29] = (byte) bytes5[3];
        pack[26] = (byte) 0x00;
        pack[27] = (byte) 0x00;
        pack[28] = (byte) 0x00;
        pack[29] = (byte) 0x00;

        int tu5 = num_order - 1;
        Log.e("b4_num", num_order + "  ==  " + money_all_upload);//2 == num == 1
        byte[] bytes5 = new byte[4];
        bytes5[3] = (byte) (tu5 & 0xFF);
        bytes5[2] = (byte) (tu5 >> 8 & 0xFF);
        bytes5[1] = (byte) (tu5 >> 16 & 0xFF);
        bytes5[0] = (byte) (tu5 >> 24 & 0xFF);

//        pack[26] = (byte) bytes5[0];
//        pack[27] = (byte) bytes5[1];
//        pack[28] = (byte) bytes5[2];
//        pack[29] = (byte) bytes5[3];
        //终端编号
        pack[30] = (byte) Constant.PortNumber1;
        ;
        pack[31] = (byte) Constant.PortNumber2;
        ;
        pack[32] = (byte) Constant.PortNumber3;
        ;
        pack[33] = (byte) Constant.PortNumber4;
        ;
        pack[34] = (byte) Constant.PortNumber5;
        ;
        pack[35] = (byte) Constant.PortNumber6;
        ;

        //商户上传密码  8位
        //需要先进行 Des加密
        byte[] key_des_mm = new byte[8];
        key_des_mm[0] = (byte) 0x00;
        key_des_mm[1] = (byte) 0x00;
        key_des_mm[2] = (byte) 0x00;
        key_des_mm[3] = (byte) 0x00;
        key_des_mm[4] = (byte) 0x00;
        key_des_mm[5] = (byte) 0x00;
        key_des_mm[6] = (byte) 0x00;
        key_des_mm[7] = (byte) 0x00;
        try {
            key_des_mm = Des.enCrypto_2(key_des_mm, CAR_SIGN_19);
            for (int i = 0; i < 8; i++) {
                pack[36 + i] = (byte) key_des_mm[i];
            }
        } catch (Exception e) {
        }

        //交易批次号  8位
        for (int i = 0; i < 8; i++) {
            pack[44 + i] = (byte) system_order_b0[i + 44];
        }
        //交易结算信息   BCD
        //交易批次号  4位
        //传送序号 终端自动加1 4位
        pack[52] = (byte) bytes5[0];
        pack[53] = (byte) bytes5[1];
        pack[54] = (byte) bytes5[2];
        pack[55] = (byte) bytes5[3];
//        pack[55] = (byte) 1;
        //交易总金额  4位  money_all
        byte[] money_b4 = byte4ByInt(Integer.parseInt(money_all_upload + ""));
//        byte[] money_b4 = byte4ByInt(Integer.parseInt((money_all_upload+2)+"")); //测试出错情况
        byte[] hex_money = new byte[4];
        hex_money[0] = money_b4[0];
        hex_money[1] = money_b4[1];
        hex_money[2] = money_b4[2];
        hex_money[3] = money_b4[3];
        int[] int_money = BCD4(hex_money);
        pack[56] = (byte) intTointByte(int_money[0]);
        pack[57] = (byte) intTointByte(int_money[1]);
        pack[58] = (byte) intTointByte(int_money[2]);
        pack[59] = (byte) intTointByte(int_money[3]);

        //终端当前日期
        pack[60] = (byte) intTointByte(20);                                    //终端时间        YY
        pack[61] = (byte) intTointByte(Integer.parseInt((year + "").substring(2, 4)));                                    //终端时间        YY
        pack[62] = (byte) intTointByte(month);                                    //终端时间        MM
        pack[63] = (byte) intTointByte(day);                                    //终端时间        DD
        pack[64] = (byte) intTointByte(hour);                                    //终端时间        HH
        pack[65] = (byte) intTointByte(minute);                                    //终端时间        MI
        pack[76] = (byte) intTointByte(second);                                     //终端时间        SS

        //RFU 保留  填充0x00  21位
        for (int i = 0; i < 21; i++) {
            pack[67 + i] = (byte) 0x00;
        }
        //MAC  6-15密钥运算
        pack[88] = 0x00;
        pack[89] = 0x00;
        pack[90] = 0x00;
        pack[91] = 0x00;
        pack[92] = 0x00;
        pack[93] = 0x00;
        pack[94] = 0x00;
        pack[95] = 0x00;

        //MAC  6-10 km DES运算
        //8个一组进行加密 ，上次加密的结果为下个加密的密钥
        //下一次密钥存储
        key_des = new byte[8];
//        List mList = Arrays.asList(pack);
//        byte[] mList = new byte[8];
        List mList = new ArrayList();
        for (int i = 0; i < pack.length - 8; i++) {
            mList.add(pack[i]);
        }
//        00 00 00 00 00 00 6E  7D56EC6BF8E1EB
        List<List<Byte>> mEndList = new ArrayList<>();
        //把数据切割成 8个字节数组 用于加密
        for (int i = 0; i < 10; i++) {
            mEndList.add(mList.subList(8 * (i + 1), (8 * (i + 2))));
        }
        //第一次加密数据
        byte[] key_des_1 = new byte[8];
        key_des[0] = (byte) 0x00;
        key_des[1] = (byte) 0x00;
        key_des[2] = (byte) 0x00;
        key_des[3] = (byte) 0x00;
        key_des[4] = (byte) 0x00;
        key_des[5] = (byte) 0x00;
        key_des[6] = (byte) 0x00;
        key_des[7] = (byte) 0x00;
        //K19 密钥
        try {
            key_des = Des.enCrypto_2(key_des_1, CAR_SIGN_19);
        } catch (Exception e) {
        }

        for (int i = 0; i < 10; i++) {
            try {
//                Log.e("agree_key_des_info+112 +26  "+i ,bytesToHexString(key_des)+"  上一个加密密钥====>>  "+bytesToHexString(Des.enCrypto_2(listTobyte(mEndList.get(i)),key_des))+"  新密钥====>>  "+bytesToHexString(listTobyte(mEndList.get(i)))+"  == 加密数据");
                key_des = Des.enCrypto_2(listTobyte(mEndList.get(i)), key_des);
            } catch (Exception e) {
            }
        }
//        Des.enCrypto_2(listTobyte(mEndList.get(i)),key_des)
        //MAC 加密数据结果key_des
//        pack[151] = (byte) 0x00;
        for (int i = 0; i < 8; i++) {
            pack[88 + i] = key_des[i];
        }
        Log.e("b4_body_des0", bytesToHexString(pack));
        //数据体 7-11进行DES加密
        byte[] body = new byte[80];
        for (int i = 0; i < 80; i++) {
            body[i] = pack[16 + i];
        }
        Log.e("b4_body_des1", bytesToHexString(body));
        try {
            //7-11 des加密结果  k65
            byte[] body_des = Des.enCrypto_2(body, CAR_SIGN_65);
            int index = 8;
            for (int i = 0; i < body_des.length; i++) {
                pack[16 + i] = body_des[i];
            }
            /*
            00 00 00 00 00 00 00 00 00 00 00 00 00 01 88 01 20 00 11 11 0A 01 01 72 06 13 00 00 00 00
            51 00 00 05 00 11 21 12 00 00 00 00 00 01 03 00 00 01 86 A0 00 00 00 00 28 00 04 00 00 00
            00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 00 00 00 00
            00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 38 79 6A 28 CF 92 D2 9F
             */
            Log.e("b4_body_des2", bytesToHexString(deCrypto_2(body_des, CAR_SIGN_65)));
            Log.e("b4_body_pack", bytesToHexString(pack));
        } catch (Exception e) {
            Log.e("b4_eeee_body", e.getMessage().toString());
        }

//        byte sum = 0;
//        for (int i = 0; i < 128; i++) {//校验和
//            sum ^= pack[i];
//        }
        //D0 BA 10 05 04 80 00 98 FF FF 01 00 99 99 32 40
//        pack[127] = (byte) sum;
        StringBuilder sb = new StringBuilder();//非线程安全
        for (int i = 0; i < pack.length; i++) {
            sb.append(SerialDataUtils.Byte2Hex((pack[i]))).append(" ");
        }
        Log.e("TAG", pack.length + " == B4_sendCmds_car： " + sb);

        return pack;
    }

    /**
     * A5 脱机消费交易结算串口
     *
     * @return
     */

    private static byte[] upload_A5(String num, String money) {
        Log.e("PayStringTobyte1A5", bytesToHexString(PayStringTobyte(1)));
        Log.e("PayStringTobyte2A5", bytesToHexString(PayStringTobyte(2)));
        //获取系统的日期
        Calendar calendar = Calendar.getInstance();
//年
        int year = calendar.get(Calendar.YEAR);
//月
        int month = calendar.get(Calendar.MONTH) + 1;
//日
        int day = calendar.get(Calendar.DAY_OF_MONTH);
//获取系统时间
//小时
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
//分钟
        int minute = calendar.get(Calendar.MINUTE);
//秒
        int second = calendar.get(Calendar.SECOND);
//        byte[] CAR_SIGN_19 = new byte[8];
//        CAR_SIGN_19 = PayStringTobyte(1);
//        byte[] CAR_SIGN_65 = new byte[8];
//        CAR_SIGN_65 = PayStringTobyte(2);
        byte[] key_km = new byte[8];
        key_km[0] = (byte) Constant.PasswordNumber1;
        key_km[1] = (byte) Constant.PasswordNumber2;
        key_km[2] = (byte) Constant.PasswordNumber3;
        key_km[3] = (byte) Constant.PasswordNumber4;
        key_km[4] = (byte) Constant.PasswordNumber5;
        key_km[5] = (byte) Constant.PasswordNumber6;
        key_km[6] = (byte) Constant.PasswordNumber7;
        key_km[7] = (byte) Constant.PasswordNumber8;
        byte[] pack = new byte[80];
        pack[0] = (byte) 0xD0;
        pack[1] = (byte) 0xA5;                                           //包头D0 B0 2字节
        pack[2] = (byte) 0x10;
        pack[3] = (byte) 0x05;                                          //报文版本
        pack[4] = (byte) 0x04;                                          //加密算法 1字节
        pack[5] = (byte) 0x80;                                          //排序方式 1字节
        pack[6] = (byte) 0x00;
        pack[7] = (byte) 0x48;                                          //报文长度 2字节
        //握手流水号，前两个填充FF，后面字节PASM卡号  8位
        pack[8] = (byte) 0xFF;
        pack[9] = (byte) 0xFF;
        for (int i = 0; i < 6; i++) {
            pack[10 + i] = (byte) pack_order[i];
        }
        //01 00 99 99 32 40
        Log.e("agree_pack_order", bytesToHexString(pack_order));
        //报文序号 保留 填充0x00
        pack[16] = (byte) 0x00;
        pack[17] = (byte) 0x00;
        pack[18] = (byte) 0x00;
        pack[19] = (byte) 0x00;
        //RFU1 保留  填充0x00
        pack[20] = (byte) 0x00;
        pack[21] = (byte) 0x00;

        //RFU2 保留  填充0x00
        pack[22] = (byte) 0x00;
        pack[23] = (byte) 0x00;
        pack[24] = (byte) 0x00;
        pack[25] = (byte) 0x00;

        //传送序号 终端自动加1 4位
        int tu5 = num_order - 1;
        byte[] bytes5 = new byte[4];
        bytes5[3] = (byte) (tu5 & 0xFF);
        bytes5[2] = (byte) (tu5 >> 8 & 0xFF);
        bytes5[1] = (byte) (tu5 >> 16 & 0xFF);
        bytes5[0] = (byte) (tu5 >> 24 & 0xFF);

        pack[26] = (byte) bytes5[0];
        pack[27] = (byte) bytes5[1];
        pack[28] = (byte) bytes5[2];
        pack[29] = (byte) bytes5[3];
//        pack[26] = (byte) 0x00;
//        pack[27] = (byte) 0x00;
//        pack[28] = (byte) 0x00;
//        pack[29] = (byte) 0x00;
        //终端编号
        pack[30] = (byte) Constant.PortNumber1;
        ;
        pack[31] = (byte) Constant.PortNumber2;
        ;
        pack[32] = (byte) Constant.PortNumber3;
        ;
        pack[33] = (byte) Constant.PortNumber4;
        ;
        pack[34] = (byte) Constant.PortNumber5;
        ;
        pack[35] = (byte) Constant.PortNumber6;
        ;
//终端当前日期
        pack[36] = (byte) intTointByte(20);                                    //终端时间        YY
        pack[37] = (byte) intTointByte(Integer.parseInt((year + "").substring(2, 4)));                                    //终端时间        YY
        pack[38] = (byte) intTointByte(month);                                    //终端时间        MM
        pack[39] = (byte) intTointByte(day);                                    //终端时间        DD
        pack[40] = (byte) intTointByte(hour);                                    //终端时间        HH
        pack[41] = (byte) intTointByte(minute);                                    //终端时间        MI
        pack[42] = (byte) intTointByte(second);                                     //终端时间        SS
        //结算类型
        pack[43] = (byte) 0x0A;
        //RFU 8位
        pack[44] = (byte) 0x00;
        pack[45] = (byte) 0x00;
        pack[46] = (byte) 0x00;
        pack[47] = (byte) 0x00;
        pack[48] = (byte) 0x00;
        pack[49] = (byte) 0x00;
        pack[50] = (byte) 0x00;
        pack[51] = (byte) 0x00;
        //交易结算信息   BCD
        //交易批次号  4位
        //传送序号 终端自动加1 4位
//        int tu6 = Integer.parseInt(num)-1;
//        byte[] bytes6 = new byte[4];
//        bytes6[3] = (byte) (tu6 & 0xFF);
//        bytes6[2] = (byte) (tu6 >> 8 & 0xFF);
//        bytes6[1] = (byte) (tu6 >> 16 & 0xFF);
//        bytes6[0] = (byte) (tu6 >> 24 & 0xFF);
        pack[52] = (byte) bytes5[0];
        pack[53] = (byte) bytes5[1];
        pack[54] = (byte) bytes5[2];
        pack[55] = (byte) bytes5[3];
        //交易总金额  4位  money_all
        byte[] money_b4 = byte4ByInt(Integer.parseInt(money_all_upload + ""));
        byte[] hex_money = new byte[4];
        hex_money[0] = money_b4[0];
        hex_money[1] = money_b4[1];
        hex_money[2] = money_b4[2];
        hex_money[3] = money_b4[3];
        int[] int_money = BCD4(hex_money);
        pack[56] = (byte) intTointByte(int_money[0]);
        pack[57] = (byte) intTointByte(int_money[1]);
        pack[58] = (byte) intTointByte(int_money[2]);
        pack[59] = (byte) intTointByte(int_money[3]);

        //RFU 保留  填充0x00  21位
        for (int i = 0; i < 12; i++) {
            pack[60 + i] = (byte) 0x00;
        }
        //MAC  6-15密钥运算
        pack[72] = 0x00;
        pack[73] = 0x00;
        pack[74] = 0x00;
        pack[75] = 0x00;
        pack[76] = 0x00;
        pack[77] = 0x00;
        pack[78] = 0x00;
        pack[79] = 0x00;

        //MAC  6-10 km DES运算
        //8个一组进行加密 ，上次加密的结果为下个加密的密钥
        //下一次密钥存储
        key_des = new byte[8];
//        List mList = Arrays.asList(pack);
//        byte[] mList = new byte[8];
        List mList = new ArrayList();
        for (int i = 0; i < pack.length - 8; i++) {
            mList.add(pack[i]);
        }

        List<List<Byte>> mEndList = new ArrayList<>();
        //把数据切割成 8个字节数组 用于加密
        for (int i = 0; i < 8; i++) {
            mEndList.add(mList.subList(8 * (i + 1), (8 * (i + 2))));
        }
        //第一次加密数据
        byte[] key_des_1 = new byte[8];
        key_des[0] = (byte) 0x00;
        key_des[1] = (byte) 0x00;
        key_des[2] = (byte) 0x00;
        key_des[3] = (byte) 0x00;
        key_des[4] = (byte) 0x00;
        key_des[5] = (byte) 0x00;
        key_des[6] = (byte) 0x00;
        key_des[7] = (byte) 0x00;
        //K19 密钥
        try {
            key_des = Des.enCrypto_2(key_des_1, key_km);
        } catch (Exception e) {
        }

        for (int i = 0; i < 8; i++) {
            try {
//                Log.e("agree_key_des_info+112 +26  "+i ,bytesToHexString(key_des)+"  上一个加密密钥====>>  "+bytesToHexString(Des.enCrypto_2(listTobyte(mEndList.get(i)),key_des))+"  新密钥====>>  "+bytesToHexString(listTobyte(mEndList.get(i)))+"  == 加密数据");
                key_des = Des.enCrypto_2(listTobyte(mEndList.get(i)), key_des);
            } catch (Exception e) {
            }
        }
        //MAC 加密数据结果key_des
        for (int i = 0; i < 8; i++) {
            pack[72 + i] = key_des[i];
        }
        Log.e("A5_body_des0", bytesToHexString(pack));
        //数据体 7-11进行DES加密
        byte[] body = new byte[64];
        for (int i = 0; i < 64; i++) {
            body[i] = pack[16 + i];
        }
        Log.e("A5_body_des1", bytesToHexString(body));
        try {
            //7-11 des加密结果  k65
            byte[] body_des = Des.enCrypto_2(body, key_km);
            for (int i = 0; i < body_des.length; i++) {
                pack[16 + i] = body_des[i];
            }

            Log.e("A5_body_des2", bytesToHexString(deCrypto_2(body_des, key_km)));
            Log.e("A5_body_pack", bytesToHexString(pack));
        } catch (Exception e) {
            Log.e("A5_eeee_body", e.getMessage().toString());
        }
        StringBuilder sb = new StringBuilder();//非线程安全
        for (int i = 0; i < pack.length; i++) {
            sb.append(SerialDataUtils.Byte2Hex((pack[i]))).append(" ");
        }
        Log.e("TAG", pack.length + " == A5_sendCmds_car： " + sb);

        return pack;
    }

    private static byte[] upload_ALL_A8(byte[] cmd) {
        Log.e("A8_123__load", " =========" + bytesToHexString(cmd));
        Log.e("A8_PayStringTobyte1", bytesToHexString(PayStringTobyte(1)));
        Log.e("A8_PayStringTobyte2", bytesToHexString(PayStringTobyte(2)));
        byte[] CAR_SIGN_19 = new byte[8];
        CAR_SIGN_19 = PayStringTobyte(1);
        byte[] CAR_SIGN_65 = new byte[8];
        CAR_SIGN_65 = PayStringTobyte(2);
        byte[] pack = new byte[96];
        pack[0] = (byte) 0xD0;
        pack[1] = (byte) 0xA8;                                           //包头D0 B0 2字节
        pack[2] = (byte) 0x10;
        pack[3] = (byte) 0x05;                                          //报文版本
        pack[4] = (byte) 0x04;                                          //加密算法 1字节
        pack[5] = (byte) 0x80;                                          //排序方式 1字节
        pack[6] = (byte) 0x00;
        //报文长度 = 本身
        pack[7] = (byte) 0x58;                                          //报文长度 2字节
        //握手流水号，前两个填充FF，后面字节PASM卡号  8位
        pack[8] = (byte) 0xFF;
        pack[9] = (byte) 0xFF;
        for (int i = 0; i < 6; i++) {
            pack[10 + i] = (byte) pack_order[i];
        }
        //01 00 99 99 32 40
        Log.e("agree_pack_order", bytesToHexString(pack_order));
        //报文序号 保留 填充0x00
        pack[16] = (byte) 0x00;
        pack[17] = (byte) 0x00;
        pack[18] = (byte) 0x00;
        pack[19] = (byte) 0x00;
        //RFU1 保留  填充0x00
        pack[20] = (byte) 0x00;
        pack[21] = (byte) 0x00;

        //RFU2 保留  填充0x00
        pack[22] = (byte) 0x00;
        pack[23] = (byte) 0x00;
        pack[24] = (byte) 0x00;
        pack[25] = (byte) 0x00;

        //传送序号 终端自动加1 4位
        int tu5 = Constant.Companion.getCAR_ORDER_NUM();
        byte[] bytes5 = new byte[4];
        bytes5[3] = (byte) (tu5 & 0xFF);
        bytes5[2] = (byte) (tu5 >> 8 & 0xFF);
        bytes5[1] = (byte) (tu5 >> 16 & 0xFF);
        bytes5[0] = (byte) (tu5 >> 24 & 0xFF);

        pack[26] = (byte) bytes5[0];
        pack[27] = (byte) bytes5[1];
        pack[28] = (byte) bytes5[2];
        pack[29] = (byte) bytes5[3];
        //结算流水号  从A5 获取  8位
        for (int i = 0; i < 8; i++) {
            pack[30 + i] = (byte) cmd[i];
        }

        //后续标识   1位   超过980位 有，否则无
        pack[38] = (byte) 0x00;

        //商户上传密码  8位
        //需要先进行 Des加密
        byte[] key_des_mm = new byte[8];
        key_des_mm[0] = (byte) 0x00;
        key_des_mm[1] = (byte) 0x00;
        key_des_mm[2] = (byte) 0x00;
        key_des_mm[3] = (byte) 0x00;
        key_des_mm[4] = (byte) 0x00;
        key_des_mm[5] = (byte) 0x00;
        key_des_mm[6] = (byte) 0x00;
        key_des_mm[7] = (byte) 0x00;
        try {
            key_des_mm = Des.enCrypto_2(key_des_mm, CAR_SIGN_19);
            for (int i = 0; i < 8; i++) {
                pack[39 + i] = (byte) key_des_mm[i];
            }
        } catch (Exception e) {
        }

        //交易批次号  8位
        //交易流水信息   BCD
        //交易批次号  4位
        pack[47] = (byte) bytes5[0];
        pack[48] = (byte) bytes5[1];
        pack[49] = (byte) bytes5[2];
        pack[50] = (byte) bytes5[3];
        //交易总金额  4位  money_all
        byte[] money_b4 = byte4ByInt(Integer.parseInt(money_all_upload + ""));
        byte[] hex_money = new byte[4];
        hex_money[0] = money_b4[0];
        hex_money[1] = money_b4[1];
        hex_money[2] = money_b4[2];
        hex_money[3] = money_b4[3];
        int[] int_money = BCD4(hex_money);
        pack[51] = (byte) intTointByte(int_money[0]);
        pack[52] = (byte) intTointByte(int_money[1]);
        pack[53] = (byte) intTointByte(int_money[2]);
        pack[54] = (byte) intTointByte(int_money[3]);

        //交易流水数据 《=9803
        //A8_byte_nfo
        for (int i = 0; i < 88; i++) {
            pack[55 + i] = (byte) A8_byte_nfo[i];
        }
        //MAC  6-15密钥运算
        pack[143] = 0x00;
        pack[144] = 0x00;
        pack[145] = 0x00;
        pack[146] = 0x00;
        pack[147] = 0x00;
        pack[148] = 0x00;
        pack[149] = 0x00;
        pack[150] = 0x00;

        //MAC  6-10 km DES运算
        //8个一组进行加密 ，上次加密的结果为下个加密的密钥
        //下一次密钥存储00
        key_des = new byte[8];
//        List mList = Arrays.asList(pack);
//        byte[] mList = new byte[8];
        List mList = new ArrayList();
        for (int i = 0; i < pack.length - 8; i++) {
            mList.add(pack[i]);
        }
//        00 00 00 00 00 00 6E  7D56EC6BF8E1EB
        List<List<Byte>> mEndList = new ArrayList<>();
        //把数据切割成 8个字节数组 用于加密
        for (int i = 0; i < 10; i++) {
            mEndList.add(mList.subList(8 * (i + 1), (8 * (i + 2))));
        }
        //第一次加密数据
        byte[] key_des_1 = new byte[8];
        key_des[0] = (byte) 0x00;
        key_des[1] = (byte) 0x00;
        key_des[2] = (byte) 0x00;
        key_des[3] = (byte) 0x00;
        key_des[4] = (byte) 0x00;
        key_des[5] = (byte) 0x00;
        key_des[6] = (byte) 0x00;
        key_des[7] = (byte) 0x00;
        //K19 密钥
        try {
            key_des = Des.enCrypto_2(key_des_1, CAR_SIGN_19);
        } catch (Exception e) {
        }

        for (int i = 0; i < 10; i++) {
            try {
//                Log.e("agree_key_des_info+112 +26  "+i ,bytesToHexString(key_des)+"  上一个加密密钥====>>  "+bytesToHexString(Des.enCrypto_2(listTobyte(mEndList.get(i)),key_des))+"  新密钥====>>  "+bytesToHexString(listTobyte(mEndList.get(i)))+"  == 加密数据");
                key_des = Des.enCrypto_2(listTobyte(mEndList.get(i)), key_des);
            } catch (Exception e) {
            }
        }
//        Des.enCrypto_2(listTobyte(mEndList.get(i)),key_des)
        //MAC 加密数据结果key_des
//        pack[151] = (byte) 0x00;
        for (int i = 0; i < 8; i++) {
            pack[88 + i] = key_des[i];
        }
        Log.e("A8_body_des0", bytesToHexString(pack));
        //数据体 7-11进行DES加密
        byte[] body = new byte[80];
        for (int i = 0; i < 80; i++) {
            body[i] = pack[16 + i];
        }
        Log.e("A8_body_des1", bytesToHexString(body));
        try {
            //7-11 des加密结果  k65
            byte[] body_des = Des.enCrypto_2(body, CAR_SIGN_65);
            int index = 8;
            for (int i = 0; i < body_des.length; i++) {
                pack[16 + i] = body_des[i];
            }
            Log.e("A8_body_des2", bytesToHexString(deCrypto_2(body_des, CAR_SIGN_65)));
            Log.e("A8_body_pack", bytesToHexString(pack));
        } catch (Exception e) {
            Log.e("A8_eeee_body", e.getMessage().toString());
        }

//        byte sum = 0;
//        for (int i = 0; i < 128; i++) {//校验和
//            sum ^= pack[i];
//        }
        //D0 BA 10 05 04 80 00 98 FF FF 01 00 99 99 32 40
//        pack[127] = (byte) sum;
        StringBuilder sb = new StringBuilder();//非线程安全
        for (int i = 0; i < pack.length; i++) {
            sb.append(SerialDataUtils.Byte2Hex((pack[i]))).append(" ");
        }
        Log.e("TAG", pack.length + " == A8_sendCmds_car： " + sb);

        return pack;
    }


    //=============================   充值相关   =====================================
    //交易类型查询  113
    private static byte[] type_search(byte[] cmd) {
        Log.e("CZStringTobyte1", bytesToHexString(cmd));
        byte[] PKI_ID = new byte[4]; //管理卡号
        PKI_ID = CZStringTobyte();
        byte[] CZ_ORDER = new byte[8];//握手流水号 8位
        CZ_ORDER = CZStringTobyteORDER();
        byte[] pack = new byte[112];
        pack[0] = (byte) 0xAA;
        pack[1] = (byte) 0x51;                                           //包头AA 51 2字节
        pack[2] = (byte) 0x00;
        pack[3] = (byte) 0x03;                                          //报文版本
        pack[4] = (byte) 0x02;                                          //加密算法 1字节
        pack[5] = (byte) 0x80;                                          //排序方式 1字节
        pack[6] = (byte) 0x00;
        pack[7] = (byte) 0x68;                                          //报文长度 2字节
        //握手流水号，前两个填充FF，后面字节PASM卡号  8位
        for (int i = 0; i < 8; i++) {
            pack[8 + i] = (byte) CZ_ORDER[i];
        }

        //报文序号 保留 填充0x00
        pack[16] = (byte) 0x00;
        pack[17] = (byte) 0x00;
        pack[18] = (byte) 0x00;
        pack[19] = (byte) 0x00;
        //PKI管理卡号  4位
        pack[20] = (byte) PKI_ID[0];
        pack[21] = (byte) PKI_ID[1];
        pack[22] = (byte) PKI_ID[2];
        pack[23] = (byte) PKI_ID[3];
//        物理卡号 8位  逻辑卡号 8位   16位
        for (int i = 0; i < 16; i++) {
            pack[24 + i] = (byte) cmd[i + 4];
        }
        //SAK  1位
        pack[40] = (byte) 0x00;
        //物理卡信息 32位
        for (int i = 0; i < 32; i++) {
            pack[41 + i] = (byte) cmd[i + 4];
        }

        /**
         * 0x00  执行余额查询操作
         * 0x01  执行普通充值操作
         * 0x02  执行撤销操作
         * 0x03  执行扩展应用操作
         * 0x04  执行用户账户充值
         * 0x05  执行异常信息提交
         */
        //执行操作  1位
        pack[73] = 0x01;
        //RFU 填充 0x00
        for (int i = 0; i < 34; i++) {
            pack[74 + i] = (byte) 0x00;
        }
        //校验码  4位
        for (int i = 0; i < 4; i++) {
            pack[108 + i] = (byte) 0x00;
        }

        byte[] md5_info = new byte[100];
        MD5.MD5_sign(md5_info);

        StringBuilder sb = new StringBuilder();//非线程安全
        for (int i = 0; i < pack.length; i++) {
            sb.append(SerialDataUtils.Byte2Hex((pack[i]))).append(" ");
        }
        Log.e("TAG113", pack.length + " == agree_sendCmds_car： " + sb);

        return pack;
    }

    //4位 byte  转  4位 int

    private static int[] BCD4(byte[] cmd) {
        String info_1 = btoi(cmd)[0] + "";
        String info_2 = "00000000" + info_1;
//        info_2.substring(info_2.length()-8,info_2.length());
        Log.e("btoi2", info_2.substring(info_2.length() - 8, info_2.length()));
        String info_3 = info_2.substring(info_2.length() - 8, info_2.length());
        String[] str_test = new String[4];
        for (int i = 0; i < 4; i++) {
            str_test[i] = info_3.substring(i * 2, (i + 1) * 2);
        }
        int[] test2 = new int[4];
        //E2 37 BB D3 BF 70 C2 CC       PayStringTobyte
        //InterceptString.Companion.moneyByteToByte(Integer.parseInt(str_test[0]));
        test2[0] = InterceptString.Companion.moneyByteToByte(Integer.parseInt(str_test[0]));
        test2[1] = InterceptString.Companion.moneyByteToByte(Integer.parseInt(str_test[1]));
        test2[2] = InterceptString.Companion.moneyByteToByte(Integer.parseInt(str_test[2]));
        test2[3] = InterceptString.Companion.moneyByteToByte(Integer.parseInt(str_test[3]));
//        Log.e("btoi3",str_test.length+"  "+str_test[0]+"  "+str_test[1]+"  "+str_test[2]+"  "+str_test[3]);
//        Log.e("btoi4",test2[0]+"  ==  "+test2[1]+"  ==  "+test2[2]+"  ==  "+test2[3]+"  ==  ");
        return test2;
    }

    private static int[] BCD3(byte[] cmd) {
        String info_1 = btoi(cmd)[0] + "";
        String info_2 = "000000" + info_1;
//        info_2.substring(info_2.length()-8,info_2.length());
        String info_3 = info_2.substring(info_2.length() - 6, info_2.length());
        String[] str_test = new String[3];
        for (int i = 0; i < 3; i++) {
            str_test[i] = info_3.substring(i * 2, (i + 1) * 2);
        }
        int[] test2 = new int[3];
        //E2 37 BB D3 BF 70 C2 CC       PayStringTobyte
        //InterceptString.Companion.moneyByteToByte(Integer.parseInt(str_test[0]));
        test2[0] = InterceptString.Companion.moneyByteToByte(Integer.parseInt(str_test[0]));
        test2[1] = InterceptString.Companion.moneyByteToByte(Integer.parseInt(str_test[1]));
        test2[2] = InterceptString.Companion.moneyByteToByte(Integer.parseInt(str_test[2]));
        Log.e("btoi3", str_test.length + "  " + str_test[0] + "  " + str_test[1] + "  " + str_test[2]);
        Log.e("btoi4", test2[0] + "  ==  " + test2[1] + "  ==  " + test2[2]);
        return test2;
    }

    public static int[] btoi(byte[] btarr) {
        if (btarr.length % 4 != 0) {
            return null;
        }
        int[] intarr = new int[btarr.length / 4];

        int i1, i2, i3, i4;
        for (int j = 0, k = 0; j < intarr.length; j++, k += 4)//j循环int		k循环byte数组
        {
            i1 = btarr[k];
            i2 = btarr[k + 1];
            i3 = btarr[k + 2];
            i4 = btarr[k + 3];

            if (i1 < 0) {
                i1 += 256;
            }
            if (i2 < 0) {
                i2 += 256;
            }
            if (i3 < 0) {
                i3 += 256;
            }
            if (i4 < 0) {
                i4 += 256;
            }
            intarr[j] = (i1 << 24) + (i2 << 16) + (i3 << 8) + (i4 << 0);//保存Int数据类型转换

        }
        return intarr;
    }

    public void getTest(byte[] cmd) {
        Log.e("test_111", cmd[2] + "");

    }

    public static int intTointByte(int args) {
        int hex_all = 0;
        if (args > 10) {
            int ten_num = Integer.parseInt((args + "").substring(0, 1));
            int hex_1 = ten_num * 16;
            int hex_2 = Integer.parseInt((args + "").substring(1, 2));
            hex_all = hex_1 + hex_2;
        } else {
            hex_all = args;
        }
        Log.e("hex_all", hex_all + "");
        return hex_all;
    }

    //按10条上传一次，最后一条长度不应该 大于  980 -88 = 892   上传一次
    private static byte[] spilt_980() {
        byte[] allbyte = new byte[1080];
        List mList = new ArrayList();
        for (int i = 0; i < allbyte.length; i++) {
            mList.add(allbyte[i]);
        }
        List<List<byte[]>> up_list = new ArrayList<>();
        List<byte[]> item_list = new ArrayList<>();
        int num = Constant.Companion.getCAR_ORDER_NUM();
        int num_all = num / 10;
//        if(){}
        if (num <= 10) {
            item_list.add(allbyte);
            up_list.add(0, item_list);
        } else if (10 < num & num <= 20) {
            item_list.add(allbyte);
            up_list.add(1, item_list);
        } else if (20 < num & num <= 30) {
            item_list.add(allbyte);
            up_list.add(2, item_list);
        } else if (30 < num & num <= 40) {
            item_list.add(allbyte);
            up_list.add(3, item_list);
        } else if (40 < num & num <= 50) {
            item_list.add(allbyte);
            up_list.add(4, item_list);
        }
//        Log.e("",)
//        List<List<Byte>> mEndList = new ArrayList<>();
//        //把数据切割成 8个字节数组 用于加密
//        for (int i = 0; i < 20; i++) {
//            mEndList.add(mList.subList(980 * (i + 1), (980 * (i + 2))));
//        }
        return allbyte;
    }


    //按全部循环上传 上传n+1次
//    private  static byte[] spilt_980(byte[] allbyte){
//        List mList = new ArrayList();
//        for (int i = 0; i < allbyte.length; i++) {
//            mList.add(allbyte[i]);
//        }
//        //循环次数，980一组
//        if(980-88 > allbyte.length){
//            //如果在980-88 范围内，算一条
//        }
//        List<List<Byte>> mEndList = new ArrayList<>();
//        //把数据切割成 8个字节数组 用于加密
//        for (int i = 0; i < 20; i++) {
//            mEndList.add(mList.subList(980 * (i + 1), (980 * (i + 2))));
//        }
//        return ;
//    }
}

