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
import com.example.vendingmachine.utils.MD5;
import com.example.vendingmachine.utils.StringUtil;
import com.example.vendingmachine.utils.encryption.Des;
import com.example.vendingmachine.utils.encryption.SecretUtils;
import com.example.vendingmachine.utils.encryption.YCTMD5Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.example.vendingmachine.platform.common.SocketClient2.intTointByte;
import static com.example.vendingmachine.utils.encryption.AESUtils.decrypt128_3;
import static com.example.vendingmachine.utils.encryption.AESUtils.encrypt128_3;
import static com.example.vendingmachine.utils.encryption.AESUtils.encrypt128_4;
import static com.example.vendingmachine.utils.encryption.Des.deCrypto_2;

/**
 * 客户端
 */
public class AddSocketClient {
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
    private byte[] g_SYSTEMSEQ_LOAD = new byte[8];//系统流水号
    private byte[] bPacketStatus = new byte[]{0x00, 0x00, 0x00, 0x00};
    private byte[] bLoginStatus = new byte[]{(byte) 0xAA, (byte) 0x03, (byte) 0xBB, (byte) 0x01};
    private String strPacketStatus = null;
    private byte[] bPacketHead = new byte[8];
    private BufferedReader in = null;
    private int iResult = 0;
    private String strResult;
    private byte[] buff = new byte[512];
    private static byte RollBackStatus = 0;
    private int socketStatus = 0;
    private static int g_PACKET_INDEX = 0;//充值报文序号

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

    public static void setRollBackStatus(byte rbstatus) {
        RollBackStatus = rbstatus;
    }

    public AddSocketClient(String mhost, int mport, DrinkMacActivity context, String mac_id) {
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
                    socket.setSoTimeout(12000);
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
    //充值金的余额
    private int vMoney = 0;
    @SuppressLint("HandlerLeak")
    Handler handler = new Handler() {
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
                    socketStatus = 0;
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
                    //充值金查询成功以后跳转到新的界面
                    mActivity.setFlyingCharge(vMoney);
                    break;
            }
        }
    };


    public void IntoDrilMac(){
        //主界面的初始化操作
        mActivity.FlyingChargeRelate();
    }

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


    public static int bytesToInt(byte[] ary, int offset) {
        int value;
        value = (int) ((ary[offset] & 0xFF)
                | ((ary[offset + 1] << 8) & 0xFF00)
                | ((ary[offset + 2] << 16) & 0xFF0000)
                | ((ary[offset + 3] << 24) & 0xFF000000));
        return value;
    }


    private List<Byte> read_buffer = new ArrayList<>();
    private long lastReadTime = 0;
    InputStream is;

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
                            Log.i("scoket_cmd_read", "read data: " + bytesToHexString(temp));

                            if ((temp[0] == (byte) 0xAA) || (temp[0] == (byte) 0xFA) || (temp[0] == (byte) 0xAB)) {
                                g_PACKET_INDEX += 1;
                            }
                            Log.i("scoket_cmd_read", "socket read index: " + g_PACKET_INDEX);
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
                                        Log.i("socket_read_car1_b1", "read data: ");
                                        //解密上传数据接口
//                                        pay_des_upload(temp);
//                                        num_order++;
//                                        Constant.Companion.setCAR_ORDER_NUM(num_order);
                                        break;
                                }
                            }
                            if (temp[0] == (byte) 0xFE) {
                                //收到F4的数据，00 认证成功，FF失败
                                if (temp[1] == (byte) 0x04) {
                                    if (temp[size - 1] == (byte) 0x00) {
                                        g_PACKET_INDEX = 0;
                                        Log.i("yct info:", "recharge sign in success!");
                                    } else {
                                        //认证失败重新签到注册'
                                        CarSerialPortUtil.getInstance().test_card();
                                    }
                                } else if (temp[1] == (byte) 0x02) {
                                    //发送f2数据,获取DE3
                                    CarSerialPortUtil.getInstance().test_sign_in_2(temp);
                                }
                            }
                            if (temp[0] == (byte) 0xAA) {
                                if (temp[1] == (byte) 0x52) {
                                    CZ_des_info(temp, "52");
                                    handler.removeMessages(2);
                                } else if (temp[1] == (byte) 0x62) {
                                    CZ_des_info(temp, "62");
                                    handler.removeMessages(2);
                                } else if (temp[1] == (byte) 0x64) {
                                    CZ_des_info(temp, "64");
                                    handler.removeMessages(2);
                                } else if (temp[1] == (byte) 0x66) {
                                    CZ_des_info(temp, "66");
                                    handler.removeMessages(2);
                                } else if (temp[1] == (byte) 0x82) {
                                    CZ_des_info(temp, "82");
                                    handler.removeMessages(2);
                                } else if (temp[1] == (byte) 0x84) {
                                    CZ_des_info(temp, "84");
                                    handler.removeMessages(2);
                                } else if (temp[1] == (byte) 0x86) {
                                    CZ_des_info(temp, "86");
                                    handler.removeMessages(2);
                                }
                            }

                            if (temp[0] == (byte) 0xAB) {
                                //使用羊城通充值
                                Log.i("scoket_cmd_read", "使用羊城通充值: " + bytesToHexString(temp));
                                if (temp[1] == (byte) 0x32) {
                                    CZ_des_info(temp, "32");
                                    handler.removeMessages(2);
                                }

                            }
                            if (temp[0] == (byte) 0xFA) {
                                CZ_des_info(temp, "FA");
                            }
                        } else {
//                            Log.i("socket", "run: not get heart ");
                            //处理报文没响应的情况 2020.08.26

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
                                if (is_stop_write) {
                                    OutputStream stream = socket.getOutputStream();
                                    Log.e("CZ_scoket_cmd_send", bytesToHexString(scoket_cmd) + "");
                                    stream.write(scoket_cmd);
                                    stream.flush();
                                    is_stop_write = false;
                                    socketStatus = 1;
                                } else {
                                    Log.e("writer_eee", "no write");
                                    socketStatus = 2;
                                }

                            } catch (Exception e) {
                                Log.e("writer_eee", e.getMessage().toString());
                                socketStatus = 3;
                            }

                        } else {
                            socketStatus = 4;
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        writeThreadStatus = false;
                        Log.i("socket write", "run: stop write");
                        socketStatus = 5;
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
     * 解析TCP的数据
     *
     * @param temp
     */
    private String msg_info = "";

    byte[] parse_valid_data(byte[] temp) {
        byte[] buffer = new byte[0];
        while (temp.length > 0) {
            Log.i("socket", "parse_valid_data:" + new String(temp));
            int startIndex = -1;
            int stopIndex = -1;
            //查找数据头、数据尾
            for (int i = 0; i < temp.length; i++) {
                if (temp[i] == '#') {
                    startIndex = i;
                } else if (temp[i] == '@') {
                    stopIndex = i;
                }
                if (startIndex >= 0 && stopIndex >= 0) {
                    break;
                }
            }
            if (startIndex >= 0) {
                if (stopIndex >= 0) {
                    if (startIndex < stopIndex) {
                        if (temp[startIndex] == '#' && temp[stopIndex] == '&') {
                            byte[] validDdata = Arrays.copyOfRange(temp, startIndex + 1, stopIndex);//从下标startIndex+1开始复制，复制到下标stopIndex，不包括stopIndex
                            Log.i("socket", "run: start index " + startIndex);
                            Log.i("socket", "run: stop index " + stopIndex);
                            Log.i("socket", "run:  index " + (stopIndex - startIndex));
                            Log.i("socket", "run: parse " + new String(validDdata));
                            String validStr = new String(validDdata);//转换为字符串
                            Log.e("TAG", "服务器数据6667： " + validStr);
                            msg_info = validStr;
                            handler.sendEmptyMessage(2);
                            order(validStr);
                            //parse_json(validStr);
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

    private void order(String order) {
        try {
//            SocketOrederBean msgben = new SocketOrederBean();
//            Gson gson = new Gson();
//            msgben = gson.fromJson(order,SocketOrederBean.class);
//            LinkedHashMap<String, String> map2 = new LinkedHashMap<>();
//            map2.put("id", Constant.Companion.getMAC_ID());
//            map2.put("order",msgben.getTrade_no());
//            Gson gson2 = new GsonBuilder().enableComplexMapKeySerialization().create();
//            sendData(gson2.toJson(map2));
//            Log.e("TAG","order_sn"+ gson2.toJson(map2));
        } catch (Exception e) {
        }
    }

    public static DrinkMacActivity mActivity = null;

    //
    public void setmActivity(DrinkMacActivity mActivity) {
        this.mActivity = mActivity;
    }

    public static String bytesToHexString(byte[] bytes) {
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
    private String cz_ship_info = ""; //PKI管理卡号
    private byte[] cz_card_info = null;
    private static byte[] R_CPU_LOAD_INFO = null;
    private static int num_order = 0;//传送序号 自增
    byte[] mcmd;

    public void getSendSocketInfo(byte[] cmd, int state, String ship_info, String money) {
        Log.e("111_info3_socket_info", state + "  " + bytesToHexString(cmd) + "  " + money);
        switch (state) {
            case 108:
                mcmd = cmd;
                scoket_cmd = new byte[112];
                scoket_cmd = cz_search_pay_type(App.Companion.getSpUtil().getString
                        (Constant.Companion.getCAR_SIGN_INFO(), ""), ship_info, cmd);
                Log.e("TAG", "scoket_cmd:" + scoket_cmd);
                is_stop_write = true;
                start_write_thread();
                int time0 = 100;
                for (int i = 0; i < 3; i++) {
                    time0 += time0;
                    handler.sendMessageDelayed(handler.obtainMessage(2), time0);
                }
                cz_ship_info = ship_info;
                cz_card_info = new byte[cmd.length];
                cz_card_info = cmd;
                break;
            case 1008:
                scoket_cmd = new byte[80];
                scoket_cmd = cz_search_top_up_gold(App.Companion.getSpUtil().getString
                        (Constant.Companion.getCAR_SIGN_INFO(), ""), ship_info, mcmd);
                Log.e("TAG", "scoket_cmd:" + scoket_cmd);
                is_stop_write = true;
                start_write_thread();
                int times = 100;
                for (int i = 0; i < 3; i++) {
                    times += times;
                    handler.sendMessageDelayed(handler.obtainMessage(2), times);
                }
                cz_ship_info = ship_info;
                cz_card_info = new byte[cmd.length];
                cz_card_info = cmd;
                break;
            case 103:
                //签到，认证，已经处理，直接发送
                scoket_cmd = new byte[cmd.length];
                scoket_cmd = cmd;
                handler.sendEmptyMessage(2);
                break;
            case 104:
                //充值认证结束
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
                scoket_cmd = new byte[check_pay_agree_1(cmd).length];
                scoket_cmd = check_pay_agree_1(cmd);
                handler.sendEmptyMessage(2);
                break;
            case 111:
                //交易记录上传  成功
                Log.e("111_info_1", bytesToHexString(cmd));
                if (car_state == (byte) 0x01) {
//                    check_pay_upload_M1(cmd);
                    scoket_cmd = new byte[check_pay_upload_M1(cmd).length];
                    scoket_cmd = check_pay_upload_M1(cmd);
//                    handler.sendEmptyMessage(2);
                    handler.sendMessageDelayed(handler.obtainMessage(2), 2000);
                } else {
                    check_pay_upload_CPU(cmd);
                }
                break;
            case 1111:
                //交易记录上传  失败
                break;
            case 300:
                //BD 32 79 00 89 00 71 44 C1 B2 07 13 3E 4D F8 21 51 00 00 04 01 13 63 76
                // 00 00 BC D7 00 01 86 A0 03 54 F1 23 E5 07 52 D8 A4 93 D4 A2 4A D1 8A E6 68 90 00 3A
                //充值获取CPU信息成功，开始R_CPU_LOAD_INIT
                int iRes;
                byte[] money_byte = byte4ByInt(Integer.parseInt(money));
                iRes = SerialDataUtils.toBdInt(money_byte, 0, 4);
                CarSerialPortUtil.set_fare(iRes);//备份充值金额
                //AA81->AA82
                scoket_cmd = new byte[144];
                scoket_cmd = cz_cpu_load_qry(App.Companion.getSpUtil().
                        getString(Constant.Companion.getCAR_SIGN_INFO(), ""), cmd, money);
                start_read_thread();
                start_write_thread();
                int time = 100;
                for (int i = 0; i < 3; i++) {
                    time += time;
                    handler.sendMessageDelayed(handler.obtainMessage(2), time);
                }
                break;
            case 400:
                //充值开始  P_M1_LOAD_QRY  AA61->AA62
                scoket_cmd = new byte[160];
                byte[] m_money_byte = byte4ByInt(Integer.parseInt(money));
                iRes = SerialDataUtils.toBdInt(m_money_byte, 0, 4);
                CarSerialPortUtil.set_fare(iRes);//备份充值金额
                scoket_cmd = P_M1_LOAD_QRY(App.Companion.getSpUtil().
                        getString(Constant.Companion.getCAR_SIGN_INFO(), ""), cz_ship_info, cmd, money);
                start_read_thread();
                start_write_thread();
                int time3 = 100;
                for (int i = 0; i < 3; i++) {
                    time3 += time3;
                    handler.sendMessageDelayed(handler.obtainMessage(2), time3);
                }
                break;
            case 301:
                //充值CPU初始化成功，开始P_CPU_LOAD_QRY
                scoket_cmd = new byte[160];
                scoket_cmd = P_CPU_LOAD(App.Companion.getSpUtil().getString(Constant.Companion.getCAR_SIGN_INFO(), ""), cmd);
                start_read_thread();
                start_write_thread();
                int time1 = 100;
                for (int i = 0; i < 3; i++) {
                    time1 += time1;
                    handler.sendMessageDelayed(handler.obtainMessage(2), time1);
                }
                break;
            case 302:
                //充值开始P_CPU_LOAD_QRY信息成功，开始提交充值P_CPU_LOAD_SUBMIT AA85->AA86
                scoket_cmd = new byte[120];
                scoket_cmd = P_CPU_LOAD_SUBMIT2(App.Companion.getSpUtil().getString(Constant.Companion.getCAR_SIGN_INFO(), ""), cmd, RollBackStatus);
                //start_read_thread();
                //start_write_thread();
                int time2 = 100;
                for (int i = 0; i < 3; i++) {
                    time2 += time2;
                    handler.sendMessageDelayed(handler.obtainMessage(2), time2);
                }
                break;
            case 303:
                R_CPU_LOAD_INFO = new byte[cmd.length];
                R_CPU_LOAD_INFO = cmd;
                break;

            case 401:
                //P_M1_LOAD AA63->AA64
                scoket_cmd = new byte[160];
                scoket_cmd = P_M1_LOAD(App.Companion.getSpUtil().getString(Constant.Companion.getCAR_SIGN_INFO(), ""), cmd);
                start_read_thread();
                start_write_thread();
                int time4 = 100;
                for (int i = 0; i < 3; i++) {
                    time4 += time4;
                    handler.sendMessageDelayed(handler.obtainMessage(2), time4);
                }
                break;
            case 403:
                //P_M1_ROLLBACK AA65->AA66
                scoket_cmd = new byte[176];
                scoket_cmd = P_M1_ROLLBACK2(App.Companion.getSpUtil().getString(Constant.Companion.getCAR_SIGN_INFO(), ""), cmd, RollBackStatus);
                int time6 = 100;
                for (int i = 0; i < 3; i++) {
                    time6 += time6;
                    handler.sendMessageDelayed(handler.obtainMessage(2), time6);
                }
                break;
            case 405:
                scoket_cmd = new byte[114];
                scoket_cmd = P_MONITOR(App.Companion.getSpUtil().getString(Constant.Companion.getCAR_SIGN_INFO(), ""), cmd);
                int time5 = 1000;
                for (int i = 0; i < 3; i++) {
                    time5 += time5;
                    handler.sendMessageDelayed(handler.obtainMessage(2), time5);
                }
                break;

        }
    }

    /**
     * BD B9 C9 00   包头  0-3
     * E3 00 2A DB B1 C8 00 32    握手流水号 4-11
     * 3D EF 18 84 7D 90 90 5F 7A 62 5A BC 54 78 F6 A6 D1 10 24 EB A9 BF D7 7E 74 74 B9 DE A4 F8 DB 69   会话密钥（取前16.AES密钥） 12-43
     * 20 05 15 09 01 08
     * <p>
     * 89 00 71 44
     *
     * @param sign_info/
     * @return
     */
    /**
     * 扣费查询串口  E0 11
     */
    public static byte[] CZ_E0_11_CARD(String sign_info, String shid_info, byte[] cmd) {
        Log.e("shid_info", shid_info);
        String[] manger = sign_info.split(" ");
        CZ_SK_16 = new byte[16];
        for (int i = 0; i < 16; i++) {
            int num_info = Integer.valueOf(manger[i + 12], 16);
            CZ_SK_16[i] = (byte) num_info;
        }
        byte[] pack = new byte[128];
        pack[0] = (byte) 0xE0;
        pack[1] = (byte) 0x11;                                           //包头
        pack[2] = (byte) 0x00;
        pack[3] = (byte) 0x01;                                          //len 2字节
        pack[4] = (byte) 0x02;                                          //加密算法 1字节
        pack[5] = (byte) 0x80;                                          //排序方式 1字节
        pack[6] = (byte) 0x00;                                          //报文长度
        pack[7] = (byte) 0x78;                                          //报文长度 2字节
//        String[] manger = sign_info.split(" ");
        String test_info = "";
        //握手流水号 8位
        int index = 8;
        for (int i = 0; i < 8; i++) {
            int num_info = Integer.valueOf(manger[i + 4], 16);
            pack[index + i] = (byte) num_info;
//            test_info += (" " + manger[i+4]);
        }
        pack[16] = (byte) 0x00;
        pack[17] = (byte) 0x00;
        pack[18] = (byte) 0x00;
        pack[19] = (byte) 0x01;//报文序号，自增
        //PKI管理卡号 4位
        String[] manger_shid = shid_info.trim().split(" ");
        int index_shid = 20;
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
        Log.e("E0_cmd_card_info", bytesToHexString(cmd));
        //应用类型00 普通消费  01卡间转移  02扩展应用  03退卡应用
        pack[40] = (byte) 0x00;
        //扣费金额  4位  01 02 填0    44
        pack[41] = (byte) 0x00;
        pack[42] = (byte) 0x00;
        pack[43] = (byte) 0x00;
        pack[44] = (byte) 0x01;
        //物理卡信息1  32位，读卡操作返回数据
        int index_car_1 = 45;
        for (int i = 0; i < 32; i++) {
            pack[index_car_1 + i] = cmd[i + 21];
        }
        //扩展应用流水  8位
        int index_car_2 = 77;
        for (int i = 0; i < 8; i++) {
            pack[index_car_2 + i] = (byte) 0x00;
        }
        //RFU 扩展位，暂时没用，填充0x00 39
        int index_car_3 = 85;
        for (int i = 0; i < 39; i++) {
            pack[index_car_3 + i] = (byte) 0x00;
        }
        //校验码4位
        //报文体内除  校验码   全部进行MD5   返回16个字节，截取前4个作为校验码
        byte[] sign_jym = new byte[116];
        for (int i = 0; i < 116; i++) {
            sign_jym[i] = pack[8 + i];
        }
        byte[] sign_jym_end = MD5.MD5_sign_byte(sign_jym);
        //校验码4位
        pack[124] = (byte) sign_jym_end[0];
        pack[125] = (byte) sign_jym_end[1];
        pack[126] = (byte) sign_jym_end[2];
        pack[127] = (byte) sign_jym_end[3];
        Log.e("E0_1042_test_sign", bytesToHexString(pack) + "");
        //AES 报文体加密  除握手流水号之外
        byte[] sign_aes_all = new byte[112];
        for (int i = 0; i < 112; i++) {
            sign_aes_all[i] = pack[16 + i];
        }
        Log.e("E0_11_sign_1", bytesToHexString(CZ_SK_16));
        Log.e("E0_11_sign_2", bytesToHexString(sign_aes_all));
        try {
            byte[] sign_end = decrypt128_3(sign_aes_all, CZ_SK_16);
            for (int i = 0; i < 112; i++) {
                pack[16 + i] = sign_end[i];
            }
            Log.e("E0_11_aes_3", sign_end.length + "");
        } catch (Exception e) {
            Log.e("E0_11_sign_e", e.getMessage().toString());
        }
        StringBuilder sb = new StringBuilder();//非线程安全
        for (int i = 0; i < pack.length; i++) {
            sb.append(SerialDataUtils.Byte2Hex((pack[i]))).append(" ");
        }
        //E1 30 07 13 00 00 00 00 51 00 00 04 01 13 71 53
        Log.e("TAG", "E0_sendCmds_car： " + sb);
        return pack;
    }

    /**
     * 交易类型查询
     *
     * @param sign_info
     * @param shid_info
     * @param cmd
     * @return
     */
    private static byte[] CZ_SK_16 = null;

    //P_TRAN_QRY
    public static byte[] cz_search_pay_type(String sign_info, String shid_info, byte[] cmd) {
        Log.e("shid_info", "cz_search_pay_type=" + sign_info);
        String[] manger = sign_info.split(" ");
        String test_info = "";
        //AES 秘钥
        CZ_SK_16 = new byte[16];
        for (int i = 0; i < 16; i++) {
            int num_info = Integer.valueOf(manger[i + 12], 16);
            CZ_SK_16[i] = (byte) num_info;
        }
//        Log.e("CZ_SK_16", bytesToHexString(CZ_SK_16));
        byte[] pack = new byte[112];
        pack[0] = (byte) 0xAA;
        pack[1] = (byte) 0x51;                                           //包头
        pack[2] = (byte) 0x00;

        pack[3] = (byte) 0x03;                                          //len 2字节
        pack[4] = (byte) 0x02;                                          //加密算法 1字节
        pack[5] = (byte) 0x80;                                          //排序方式 1字节
        pack[6] = (byte) 0x00;                                          //报文长度
        pack[7] = (byte) 0x68;                                          //报文长度 2字节
        //握手流水号 8位
//        int index = 8;
//        for (int i = 0; i < 8; i++) {
//            int num_info = Integer.valueOf(manger[i + 4], 16);
//            pack[index + i] = (byte) num_info;
////            test_info += (" " + manger[i+4]);
//        }
        //握手流水号 8位
        byte[] order8 = new byte[8];
        int index = 8;
        for (int i = 0; i < 8; i++) {
            int num_info = Integer.valueOf(manger[i + 4], 16);
            pack[index + i] = (byte) num_info;
            order8[i] = (byte) num_info;
        }
//        try {
//            order8 = getOrderCK_3DES(order8);
//            for (int i = 0; i < 8; i++) {
//                pack[index + i] = (byte) order8[i];
//            }
//        }catch (Exception e){}
//        int tu5 = num_order;
//        byte[] bytes5 = new byte[4];
//        bytes5[3] = (byte) (tu5 & 0xFF);
//        bytes5[2] = (byte) (tu5 >> 8 & 0xFF);
//        bytes5[1] = (byte) (tu5 >> 16 & 0xFF);
//        bytes5[0] = (byte) (tu5 >> 24 & 0xFF);
//        pack[16] = (byte) bytes5[0];
//        pack[17] = (byte) bytes5[1];
//        pack[18] = (byte) bytes5[2];
//        pack[19] = (byte) bytes5[3];//报文序号，自增
        pack[16] = (byte) ((g_PACKET_INDEX >> 24) & 0x000000FF);
        pack[17] = (byte) ((g_PACKET_INDEX >> 16) & 0x000000FF);
        pack[18] = (byte) ((g_PACKET_INDEX >> 8) & 0x000000FF);
        pack[19] = (byte) (g_PACKET_INDEX & 0x000000FF);

        Log.e("AA51 g_PACKET_INDEX:", " " + g_PACKET_INDEX);
//        pack[16] = (byte) 0x00;
//        pack[17] = (byte) 0x00;
//        pack[18] = (byte) 0x00;
//        pack[19] = (byte) 0x00;//报文序号，自增

        //PKI管理卡号 4位
        String[] manger_shid = shid_info.trim().split(" ");
        int index_shid = 20;
        for (int i = 0; i < 4; i++) {
            int num_info_1 = Integer.valueOf(manger_shid[i], 16);
            pack[index_shid + i] = (byte) num_info_1;
            test_info += (" =" + manger_shid[i]);
        }
        // 物理卡号，逻辑卡号拼接 共16位
        int index_car = 24;
        for (int i = 0; i < 49; i++) {
            pack[index_car + i] = cmd[i + 4];
        }
//        00  余额查询  01 普通充值 02 撤销  03 扩展应用  04 账户充值  05异常信息提交
        //pack[73] = (byte) 0x01;
        pack[73] = (byte) 0x04;   //0x04：执行用户帐户充值  2020-08-29

        //RFU 扩展位，暂时没用，填充0x00 39
        int index_car_3 = 74;
        for (int i = 0; i < 34; i++) {
            pack[index_car_3 + i] = (byte) 0x00;
        }
        //校验码4位
        //报文体内除  校验码   全部进行MD5   返回16个字节，截取前4个作为校验码
        byte[] sign_jym = new byte[100];
        for (int i = 0; i < 100; i++) {
            sign_jym[i] = pack[8 + i];
        }
        byte[] sign_jym_end = MD5.MD5_sign_byte(sign_jym);
        pack[108] = (byte) sign_jym_end[0];
        pack[109] = (byte) sign_jym_end[1];

        pack[110] = (byte) sign_jym_end[2];
        pack[111] = (byte) sign_jym_end[3];
        Log.e("CZ_AA_51_start", bytesToHexString(pack));
        try {
            order8 = getOrderCK_3DES(order8);
            for (int i = 0; i < 8; i++) {
                pack[index + i] = (byte) order8[i];
            }
        } catch (Exception e) {
        }
        //AES 报文体加密  除握手流水号之外
        byte[] sign_aes_all = new byte[96];
        for (int i = 0; i < 96; i++) {
            sign_aes_all[i] = pack[16 + i];
        }
        Log.e("E0_11_aes_length", "CZ_SK_16=" + bytesToHexString(CZ_SK_16));
        try {
            byte[] sign_end = encrypt128_3(sign_aes_all, CZ_SK_16);
            for (int i = 0; i < 96; i++) {
                pack[16 + i] = sign_end[i];
            }
            Log.e("E0_11_aes_length", sign_end.length + "");
        } catch (Exception e) {
            Log.e("E0_11_sign_e", e.getMessage().toString());
        }
        StringBuilder sb = new StringBuilder();//非线程安全
        for (int i = 0; i < pack.length; i++) {
            sb.append(SerialDataUtils.Byte2Hex((pack[i]))).append(" ");
        }
        //E1 30 07 13 00 00 00 00 51 00 00 04 01 13 71 53
        Log.e("E0_11_enddd", bytesToHexString(pack));
        return pack;
    }

    /**
     * 充值金账户查询
     *
     * @param sign_info
     * @param shid_info
     * @param cmd
     * @return
     */
    //P_ACCT_QRY
    public static byte[] cz_search_top_up_gold(String sign_info, String shid_info, byte[] cmd) {
        Log.e("shid_info", sign_info);
        String[] manger = sign_info.split(" ");
        String test_info = "";
        //AES 秘钥
        CZ_SK_16 = new byte[16];
        for (int i = 0; i < 16; i++) {
            int num_info = Integer.valueOf(manger[i + 12], 16);
            CZ_SK_16[i] = (byte) num_info;
        }

        byte[] pack = new byte[80];
        pack[0] = (byte) 0xAB;
        pack[1] = (byte) 0x31;                                           //包头
        pack[2] = (byte) 0x00;
        pack[3] = (byte) 0x01;                                          //len 2字节

        pack[4] = (byte) 0x02;                                          //加密算法 1字节
        pack[5] = (byte) 0x80;                                          //排序方式 1字节
        pack[6] = (byte) 0x00;                                          //报文长度
        pack[7] = (byte) 0x48;                                         //报文长度 2字节

        //握手流水号 8位
        byte[] order8 = new byte[8];
        int index = 8;
        for (int i = 0; i < 8; i++) {
            int num_info = Integer.valueOf(manger[i + 4], 16);
            pack[index + i] = (byte) num_info;
            order8[i] = (byte) num_info;
        }
        pack[16] = (byte) ((g_PACKET_INDEX >> 24) & 0x000000FF);
        pack[17] = (byte) ((g_PACKET_INDEX >> 16) & 0x000000FF);
        pack[18] = (byte) ((g_PACKET_INDEX >> 8) & 0x000000FF);
        pack[19] = (byte) (g_PACKET_INDEX & 0x000000FF);

        Log.e("AA51 g_PACKET_INDEX:", " " + g_PACKET_INDEX);
        //PKI管理卡号 4位
        String[] manger_shid = shid_info.trim().split(" ");
        int index_shid = 20;
        for (int i = 0; i < 4; i++) {
            int num_info_1 = Integer.valueOf(manger_shid[i], 16);
            pack[index_shid + i] = (byte) num_info_1;
            test_info += (" =" + manger_shid[i]);
        }

        pack[24] = (byte) 0x02;

        // 羊城通卡物理卡号，羊城通卡逻辑卡号拼接 共16位
        int index_car1 = 25;
        for (int i = 0; i < 8; i++) {
            pack[index_car1 + i] = cmd[i + 12];
        }

        int index_car2 = 33;
        for (int i = 0; i < 8; i++) {
            pack[index_car2 + i] = cmd[i + 4];
        }

        //RFU 扩展位，暂时没用，填充0x00 39
        int index_car_3 = 41;
        for (int i = 0; i < 35; i++) {
            pack[index_car_3 + i] = (byte) 0x00;
        }

        //校验码4位
        //报文体内除  校验码   全部进行MD5   返回16个字节，截取前4个作为校验码
        byte[] sign_jym = new byte[68];
        for (int i = 0; i < 68; i++) {
            sign_jym[i] = pack[8 + i];
        }
        byte[] sign_jym_end = MD5.MD5_sign_byte(sign_jym);
        pack[76] = (byte) sign_jym_end[0];
        pack[77] = (byte) sign_jym_end[1];

        pack[78] = (byte) sign_jym_end[2];
        pack[79] = (byte) sign_jym_end[3];

        Log.e("CZ_AA_51_start", bytesToHexString(pack));
        try {
            order8 = getOrderCK_3DES(order8);
            for (int i = 0; i < 8; i++) {
                pack[index + i] = (byte) order8[i];
            }
        } catch (Exception e) {
        }
        //AES 报文体加密  除握手流水号之外
        byte[] sign_aes_all = new byte[64];
        for (int i = 0; i < 64; i++) {
            sign_aes_all[i] = pack[16 + i];
        }
        Log.e("E0_11_aes_length", "CZ_SK_16=" + bytesToHexString(CZ_SK_16));
        try {
            byte[] sign_end = encrypt128_3(sign_aes_all, CZ_SK_16);
            for (int i = 0; i < 64; i++) {
                pack[16 + i] = sign_end[i];
            }
            Log.e("E0_11_aes_length", sign_end.length + "");
        } catch (Exception e) {
            Log.e("E0_11_sign_e", e.getMessage().toString());
        }
        StringBuilder sb = new StringBuilder();//非线程安全
        Log.e("E0_11_aes_length", "pack.length======" + pack.length);
        for (int i = 0; i < pack.length; i++) {
            sb.append(SerialDataUtils.Byte2Hex((pack[i]))).append(" ");
        }
        //E1 30 07 13 00 00 00 00 51 00 00 04 01 13 71 53
        Log.e("E0_11_enddd", bytesToHexString(pack));
        return pack;
    }

    /**
     * 联机充值  CPU
     * P_CPU_LOAD_QRY
     */
    private static byte[] AA_81_CARD_PKI = null;//存储用于后续的使用

    public static byte[] cz_cpu_load_qry(String sign_info, byte[] cmd, String money) {
        Log.e("shid_info", sign_info);
        String[] manger = sign_info.split(" ");
        String test_info = "";
        //AES 秘钥
        CZ_SK_16 = new byte[16];
        for (int i = 0; i < 16; i++) {
            int num_info = Integer.valueOf(manger[i + 12], 16);
            CZ_SK_16[i] = (byte) num_info;
        }
        Log.e("AA_81_SK_16", bytesToHexString(CZ_SK_16));
        byte[] pack = new byte[144];
        pack[0] = (byte) 0xAA;
        pack[1] = (byte) 0x81;                                           //包头
        pack[2] = (byte) 0x00;
        pack[3] = (byte) 0x01;                                          //len 2字节
        pack[4] = (byte) 0x02;                                          //加密算法 1字节
        pack[5] = (byte) 0x80;                                          //排序方式 1字节
        pack[6] = (byte) 0x00;                                          //报文长度
        pack[7] = (byte) 0x88;                                          //报文长度 2字节
        //握手流水号 8位  15
        byte[] order8 = new byte[8];
        int index = 8;
        for (int i = 0; i < 8; i++) {
            int num_info = Integer.valueOf(manger[i + 4], 16);
            pack[index + i] = (byte) num_info;
            order8[i] = (byte) num_info;
        }
//报文序号，自增  4  19
        pack[16] = (byte) ((g_PACKET_INDEX >> 24) & 0x000000FF);
        pack[17] = (byte) ((g_PACKET_INDEX >> 16) & 0x000000FF);
        pack[18] = (byte) ((g_PACKET_INDEX >> 8) & 0x000000FF);
        pack[19] = (byte) (g_PACKET_INDEX & 0x000000FF);
        Log.e("AA81 g_PACKET_INDEX:", " " + g_PACKET_INDEX);
//        pack[16] = (byte) 0x00;
//        pack[17] = (byte) 0x00;
//        pack[18] = (byte) 0x00;
//        pack[19] = (byte) 0x00;
        //PKI管理卡号 4位  23
        AA_81_CARD_PKI = new byte[20];
        for (int i = 0; i < 20; i++) {
            pack[20 + i] = cmd[i + 4];
            AA_81_CARD_PKI[i] = cmd[i + 4];
        }
//        String[] manger_shid = shid_info.trim().split(" ");
//        int index_shid = 20;
//        for (int i = 0; i < 4; i++) {
//            int num_info_1 = Integer.valueOf(manger_shid[i], 16);
//            pack[index_shid + i] = (byte) num_info_1;
//            test_info += (" =" + manger_shid[i]);
//        }
//
//        // 物理卡号，逻辑卡号拼接 共16位  39
//        int index_car = 24;
//        for (int i = 0; i < 16; i++) {
//            pack[index_car + i] = cmd[i + 4];
//        }
//       卡状态 - 记数信息  17位
//        pack[40] = (byte) 0x01;
//        //记数信息  16位
        for (int i = 0; i < 17; i++) {
            pack[40 + i] = cmd[32 + i];
        }

        //应用类型  00 商户圈存  01 自助圈存  20 充值金账户充值  22签约接口扣款  30卡间余额转移  40异常信息提交  50扩展应用
        // pack[57] = (byte) 0x00;
        pack[57] = (byte) 0x20;//这里不能修改，必须是充值金账户充值 2020.9.2
        //交易金额   50的整数
        byte[] money_byte = byte4ByInt(Integer.parseInt(money));
        pack[58] = (byte) money_byte[0];
        pack[59] = (byte) money_byte[1];
        pack[60] = (byte) money_byte[2];
        pack[61] = (byte) money_byte[3];

        //银行账户 16位 HEX 填充0x00
        for (int i = 0; i < 16; i++) {
            pack[62 + i] = (byte) 0x00;
        }
        //外部系统附加信息 16位 HEX 填充0x00  自定义 用于后期对账，后台是干嘛的？
        for (int i = 0; i < 16; i++) {
            pack[78 + i] = (byte) 0x00;
        }
        //扩展应用流水 8位
        for (int i = 0; i < 8; i++) {
            pack[86 + i] = (byte) 0x00;
        }
        //充值密码
        for (int i = 0; i < 16; i++) {
            pack[102 + i] = (byte) 0x00;
        }
        //RFU  22位
        for (int i = 0; i < 22; i++) {
            pack[118 + i] = (byte) 0x00;
        }
        //校验码4位
        //报文体内除  校验码   全部进行MD5   返回16个字节，截取前4个作为校验码  144-12
        byte[] sign_jym = new byte[132];
        for (int i = 0; i < 132; i++) {
            sign_jym[i] = pack[8 + i];
        }
        Log.e("AA_81_start1", bytesToHexString(pack));
        byte[] sign_jym_end = MD5.MD5_sign_byte(sign_jym);
        pack[140] = (byte) sign_jym_end[0];
        pack[141] = (byte) sign_jym_end[1];

        pack[142] = (byte) sign_jym_end[2];
        pack[143] = (byte) sign_jym_end[3];
        Log.e("AA_81_start", bytesToHexString(pack));
        try {
            order8 = getOrderCK_3DES(order8);
            for (int i = 0; i < 8; i++) {
                pack[index + i] = (byte) order8[i];
            }
        } catch (Exception e) {
            Log.e("AA_81_order_e", bytesToHexString(pack));
        }
        //AES 报文体加密  除握手流水号之外  144-16
        byte[] sign_aes_all = new byte[128];
        for (int i = 0; i < 128; i++) {
            sign_aes_all[i] = pack[16 + i];
        }

        try {
            byte[] sign_end = encrypt128_3(sign_aes_all, CZ_SK_16);
            for (int i = 0; i < 128; i++) {
                pack[16 + i] = sign_end[i];
            }
            Log.e("AA_81_aes_length", sign_end.length + "");
        } catch (Exception e) {
            Log.e("AA_81_sign_e", e.getMessage().toString());
        }
        StringBuilder sb = new StringBuilder();//非线程安全
        for (int i = 0; i < pack.length; i++) {
            sb.append(SerialDataUtils.Byte2Hex((pack[i]))).append(" ");
        }
        //E1 30 07 13 00 00 00 00 51 00 00 04 01 13 71 53
        Log.e("AA_81_enddd", bytesToHexString(pack));
        return pack;
    }


    /**
     * 联机充值  CPU
     * P_CPU_LOAD
     */
    public static byte[] P_CPU_LOAD(String sign_info, byte[] cmd) {
        Log.e("shid_info", sign_info);
        String[] manger = sign_info.split(" ");
        //AES 秘钥
        CZ_SK_16 = new byte[16];
        for (int i = 0; i < 16; i++) {
            int num_info = Integer.valueOf(manger[i + 12], 16);
            CZ_SK_16[i] = (byte) num_info;
        }
        Log.e("AA_83_SK_16", bytesToHexString(CZ_SK_16));
        byte[] pack = new byte[160];// len = 160-8
        pack[0] = (byte) 0xAA;
        pack[1] = (byte) 0x83;                                           //包头
        pack[2] = (byte) 0x00;
        pack[3] = (byte) 0x01;                                          //len 2字节
        pack[4] = (byte) 0x02;                                          //加密算法 1字节
        pack[5] = (byte) 0x80;                                          //排序方式 1字节
        pack[6] = (byte) 0x00;                                          //报文长度
        pack[7] = (byte) 0x98;                                          //报文长度 2字节
        //握手流水号 8位  15
        byte[] order8 = new byte[8];
        int index = 8;
        for (int i = 0; i < 8; i++) {
            int num_info = Integer.valueOf(manger[i + 4], 16);
            pack[index + i] = (byte) num_info;
            order8[i] = (byte) num_info;
        }
//报文序号，自增  4  19
        pack[16] = (byte) ((g_PACKET_INDEX >> 24) & 0x000000FF);
        pack[17] = (byte) ((g_PACKET_INDEX >> 16) & 0x000000FF);
        pack[18] = (byte) ((g_PACKET_INDEX >> 8) & 0x000000FF);
        pack[19] = (byte) (g_PACKET_INDEX & 0x000000FF);
        Log.e("AA83 g_PACKET_INDEX:", " " + g_PACKET_INDEX);
//        pack[16] = (byte) 0x00;
//        pack[17] = (byte) 0x00;
//        pack[18] = (byte) 0x00;
//        pack[19] = (byte) 0x00;
        //PKI管理卡号 4位  23
        for (int i = 0; i < 20; i++) {
            pack[20 + i] = AA_81_CARD_PKI[i];
        }
        //系统流水号  AA82返回数据  8位
        for (int i = 0; i < 8; i++) {
            pack[40 + i] = AA_82_ORDERNUM[i];
        }
        //卡信息 80位  卡片返回码 2位
        for (int i = 0; i < 82; i++) {
            pack[48 + i] = cmd[i + 6];
        }
        // 读卡器返回码 1位  默认00  ，不成功不会走这里，为什么还会有其他错误码？zz
        pack[130] = (byte) 0x00;
        //REU 25位
        for (int i = 0; i < 25; i++) {
            pack[131 + i] = (byte) 0x00;
        }

        //校验码4位
        //报文体内除  校验码   全部进行MD5   返回16个字节，截取前4个作为校验码  160-12
        byte[] sign_jym = new byte[148];
        for (int i = 0; i < 148; i++) {
            sign_jym[i] = pack[8 + i];
        }
        Log.e("AA_83_start1", bytesToHexString(pack));
        byte[] sign_jym_end = MD5.MD5_sign_byte(sign_jym);
        pack[156] = (byte) sign_jym_end[0];
        pack[157] = (byte) sign_jym_end[1];

        pack[158] = (byte) sign_jym_end[2];
        pack[159] = (byte) sign_jym_end[3];
        Log.e("AA_83_start", bytesToHexString(pack));
        try {
            order8 = getOrderCK_3DES(order8);
            for (int i = 0; i < 8; i++) {
                pack[index + i] = (byte) order8[i];
            }
        } catch (Exception e) {
            Log.e("AA_83_order_e", bytesToHexString(pack));
        }
        //AES 报文体加密  除握手流水号之外  160-16
        byte[] sign_aes_all = new byte[144];
        for (int i = 0; i < 144; i++) {
            sign_aes_all[i] = pack[16 + i];
        }

        try {
            byte[] sign_end = encrypt128_3(sign_aes_all, CZ_SK_16);
            for (int i = 0; i < 144; i++) {
                pack[16 + i] = sign_end[i];
            }
            Log.e("AA_83_aes_length", sign_end.length + "");
        } catch (Exception e) {
            Log.e("AA_83_sign_e", e.getMessage().toString());
        }
        StringBuilder sb = new StringBuilder();//非线程安全
        for (int i = 0; i < pack.length; i++) {
            sb.append(SerialDataUtils.Byte2Hex((pack[i]))).append(" ");
        }
        //E1 30 07 13 00 00 00 00 51 00 00 04 01 13 71 53
        Log.e("AA_83_enddd", bytesToHexString(pack));
        return pack;
    }

    /**
     * 联机充值  CPU
     * P_CPU_LOAD_SUBMIT
     */
    public static byte[] P_CPU_LOAD_SUBMIT(String sign_info, byte[] cmd) {
        Log.e("shid_info", sign_info);
        String[] manger = sign_info.split(" ");
        //AES 秘钥
        CZ_SK_16 = new byte[16];
        for (int i = 0; i < 16; i++) {
            int num_info = Integer.valueOf(manger[i + 12], 16);
            CZ_SK_16[i] = (byte) num_info;
        }
        Log.e("AA_85_SK_16", bytesToHexString(CZ_SK_16));

        byte[] pack = new byte[128];// len = 128-8
//        pack[0] = (byte) 0xAA;
//        pack[1] = (byte) 0x85;                                           //包头
//        pack[2] = (byte) 0x00;
//        pack[3] = (byte) 0x01;                                          //len 2字节
//        pack[4] = (byte) 0x02;                                          //加密算法 1字节
//        pack[5] = (byte) 0x80;                                          //排序方式 1字节
//        pack[6] = (byte) 0x00;                                          //报文长度
//        pack[7] = (byte) 0x78;                                          //报文长度 2字节
        System.arraycopy(cmd, 0, pack, 0, 8);
        //握手流水号 8位  15
        byte[] order8 = new byte[8];
        int index = 8;
        for (int i = 0; i < 8; i++) {
            int num_info = Integer.valueOf(manger[i + 4], 16);
            pack[index + i] = (byte) num_info;
            order8[i] = (byte) num_info;
        }
//报文序号，自增  4  19
        pack[16] = (byte) ((g_PACKET_INDEX >> 24) & 0x000000FF);
        pack[17] = (byte) ((g_PACKET_INDEX >> 16) & 0x000000FF);
        pack[18] = (byte) ((g_PACKET_INDEX >> 8) & 0x000000FF);
        pack[19] = (byte) (g_PACKET_INDEX & 0x000000FF);
        Log.e("AA85 g_PACKET_INDEX:", " " + g_PACKET_INDEX);
//        pack[16] = (byte) 0x00;
//        pack[17] = (byte) 0x00;
//        pack[18] = (byte) 0x00;
//        pack[19] = (byte) 0x00;
        //PKI管理卡号 4位  23
        for (int i = 0; i < 4; i++) {
            pack[20 + i] = AA_81_CARD_PKI[i];
        }
        //系统流水号  AA82返回数据  8位
        for (int i = 0; i < 8; i++) {
            pack[24 + i] = AA_82_ORDERNUM[i];
        }
        //物理卡号  逻辑卡号
        for (int i = 0; i < 16; i++) {
            pack[32 + i] = AA_81_CARD_PKI[i + 4];
        }
        //圈存状态  00 正常提交  01 网络故障  未收到AA84报文 或 未向读卡器发起充值指令
        pack[48] = (byte) 0x00;
        //交易验证码 4 位  卡片状态 2位
        for (int i = 0; i < 6; i++) {
            pack[49 + i] = R_CPU_LOAD_INFO[i + 4];
        }
        //读卡器返回码  R_CPU_LOAD 返回码
        pack[55] = R_CPU_LOAD_INFO[3];
        //充值凭证  4位  01状态下位0x00
//        AA_84_INFO
        for (int i = 0; i < 4; i++) {
            pack[56 + i] = AA_84_INFO[i + 55];
        }
        //充值金额
        for (int i = 0; i < 4; i++) {
            pack[60 + i] = AA_84_INFO[i + 51];
        }

        //BD 32 79 00           3
        // 89 00 71 44          7
        // F1 61 06 13 0E 9E F9 21   15
        // 51 00 00 07 50 00 77 63   23
        // 00 00 00 FA               27
        // 00                        28
        // 01 86 A0                  31
        // 03                        32
        // 5F EE EF C7 EA E3 36 BB F8 B9 B0 14 4C 86 EF 9D 90 00 9B
        //钱包余额 4 位  金额下限 1位  金额上限 3位
        for (int i = 0; i < 8; i++) {
            pack[64 + i] = cmd[i + 24];
        }
        // 计数信息 16位
        for (int i = 0; i < 16; i++) {
            pack[72 + i] = AA_84_INFO[i + 33];
        }
        //REU 36位
        for (int i = 0; i < 36; i++) {
            pack[88 + i] = (byte) 0x00;
        }

        //校验码4位
        //报文体内除  校验码   全部进行MD5   返回16个字节，截取前4个作为校验码  128-12
        byte[] sign_jym = new byte[116];
        for (int i = 0; i < 116; i++) {
            sign_jym[i] = pack[8 + i];
        }
        Log.e("AA_85_start1", bytesToHexString(pack));
        byte[] sign_jym_end = MD5.MD5_sign_byte(sign_jym);
        pack[124] = (byte) sign_jym_end[0];
        pack[125] = (byte) sign_jym_end[1];

        pack[126] = (byte) sign_jym_end[2];
        pack[127] = (byte) sign_jym_end[3];
        Log.e("AA_85_start", bytesToHexString(pack));
        try {
            order8 = getOrderCK_3DES(order8);
            for (int i = 0; i < 8; i++) {
                pack[index + i] = (byte) order8[i];
            }
        } catch (Exception e) {
            Log.e("AA_83_order_e", bytesToHexString(pack));
        }
        //AES 报文体加密  除握手流水号之外  128-16
        byte[] sign_aes_all = new byte[112];
        for (int i = 0; i < 112; i++) {
            sign_aes_all[i] = pack[16 + i];
        }

        try {
            byte[] sign_end = encrypt128_3(sign_aes_all, CZ_SK_16);
            for (int i = 0; i < 112; i++) {
                pack[16 + i] = sign_end[i];
            }
            Log.e("AA_85_aes_length", sign_end.length + "");
        } catch (Exception e) {
            Log.e("AA_85_sign_e", e.getMessage().toString());
        }
        StringBuilder sb = new StringBuilder();//非线程安全
        for (int i = 0; i < pack.length; i++) {
            sb.append(SerialDataUtils.Byte2Hex((pack[i]))).append(" ");
        }
        //E1 30 07 13 00 00 00 00 51 00 00 04 01 13 71 53
        Log.e("AA_85_enddd", bytesToHexString(pack));
        return pack;
    }

    public static byte[] P_CPU_LOAD_SUBMIT2(String sign_info, byte[] cmd, byte submittype) {
        Log.e("shid_info", sign_info);
        String[] manger = sign_info.split(" ");
        //AES 秘钥
        CZ_SK_16 = new byte[16];
        for (int i = 0; i < 16; i++) {
            int num_info = Integer.valueOf(manger[i + 12], 16);
            CZ_SK_16[i] = (byte) num_info;
        }
        Log.e("AA_85_SK_16", bytesToHexString(CZ_SK_16));
        Log.e("AA_85_cmd", bytesToHexString(cmd));

        byte[] pack = new byte[128];// len = 128-8
        System.arraycopy(cmd, 0, pack, 0, 128);
//        pack[0] = (byte) 0xAA;
//        pack[1] = (byte) 0x85;                                           //包头
//        pack[2] = (byte) 0x00;
//        pack[3] = (byte) 0x01;                                          //len 2字节
//        pack[4] = (byte) 0x02;                                          //加密算法 1字节
//        pack[5] = (byte) 0x80;                                          //排序方式 1字节
//        pack[6] = (byte) 0x00;                                          //报文长度
//        pack[7] = (byte) 0x78;                                          //报文长度 2字节
        //System.arraycopy(cmd, 0, pack, 0, 8);
        //握手流水号 8位  15
        byte[] order8 = new byte[8];
        int index = 8;
        for (int i = 0; i < 8; i++) {
            int num_info = Integer.valueOf(manger[i + 4], 16);
            pack[index + i] = (byte) num_info;
            order8[i] = (byte) num_info;
        }
//报文序号，自增  4  19
        pack[16] = (byte) ((g_PACKET_INDEX >> 24) & 0x000000FF);
        pack[17] = (byte) ((g_PACKET_INDEX >> 16) & 0x000000FF);
        pack[18] = (byte) ((g_PACKET_INDEX >> 8) & 0x000000FF);
        pack[19] = (byte) (g_PACKET_INDEX & 0x000000FF);
        Log.e("AA85 g_PACKET_INDEX:", "报文序号: " + g_PACKET_INDEX);
        Log.e("AA85 PACKET INFO:", "PKI卡号: " + SerialDataUtils.Bytes2HexString(pack, 20, 4));
        Log.e("AA85 PACKET INFO:", "AA82系统流水号: " + SerialDataUtils.Bytes2HexString(pack, 24, 8));
        Log.e("AA85 PACKET INFO:", "物理卡号: " + SerialDataUtils.Bytes2HexString(pack, 32, 8));
        Log.e("AA85 PACKET INFO:", "逻辑卡号: " + SerialDataUtils.Bytes2HexString(pack, 40, 8));
//        pack[16] = (byte) 0x00;
//        pack[17] = (byte) 0x00;
//        pack[18] = (byte) 0x00;
//        pack[19] = (byte) 0x00;
        //PKI管理卡号 4位  23
//        for (int i =0;i<4;i++){
//            pack[20+i] = AA_81_CARD_PKI[i];
//        }
        //System.arraycopy(cmd, 20, pack, 20, 4);
        //系统流水号  AA82返回数据  8位
//        for (int i =0;i<8;i++){
//            pack[24+i]=AA_82_ORDERNUM[i];
//        }
        //System.arraycopy(cmd, 24, pack, 24, 8);
        //物理卡号  逻辑卡号
//        for (int i =0;i<16;i++){
//            pack[32+i] = AA_81_CARD_PKI[i+4];
//        }
        //圈存状态  00 正常提交  01 网络故障  未收到AA84报文 或 未向读卡器发起充值指令
        //pack[48] = (byte) 0x00;
        pack[48] = (byte) submittype;
        Log.e("AA85 PACKET INFO:", "圈存状态: " + SerialDataUtils.Bytes2HexString(pack, 48, 1));
        Log.e("AA85 PACKET INFO:", "交易认证码: " + SerialDataUtils.Bytes2HexString(pack, 49, 4));
        Log.e("AA85 PACKET INFO:", "卡片返回码: " + SerialDataUtils.Bytes2HexString(pack, 53, 2));
        Log.e("AA85 PACKET INFO:", "读卡器返回码: " + SerialDataUtils.Bytes2HexString(pack, 55, 1));
        Log.e("AA85 PACKET INFO:", "充值凭证码: " + SerialDataUtils.Bytes2HexString(pack, 56, 4));
        Log.e("AA85 PACKET INFO:", "充值金额: " + SerialDataUtils.Bytes2HexString(pack, 60, 4));
        Log.e("AA85 PACKET INFO:", "钱包余额: " + SerialDataUtils.Bytes2HexString(pack, 64, 4));
        Log.e("AA85 PACKET INFO:", "计数信息: " + SerialDataUtils.Bytes2HexString(pack, 68, 16));
        //交易验证码 4 位  卡片状态 2位
//        for (int i=0;i<6;i++){
//            pack[49+i] = R_CPU_LOAD_INFO[i+4];
//        }
        //System.arraycopy(cmd, 49, pack, 49, 6);//交易验证码 4   卡片返回码2
        //读卡器返回码  R_CPU_LOAD 返回码
        // pack[55] = R_CPU_LOAD_INFO[3];
        //pack[55] = cmd[55];
        //充值凭证  4位  01状态下位0x00
//        AA_84_INFO
//        for (int i=0;i<4;i++){
//            pack[56+i] = AA_84_INFO[i+55];
//        }
        //充值金额
//        for (int i=0;i<4;i++){
//            pack[60+i] = AA_84_INFO[i+51];
//        }

        //BD 32 79 00           3
        // 89 00 71 44          7
        // F1 61 06 13 0E 9E F9 21   15
        // 51 00 00 07 50 00 77 63   23
        // 00 00 00 FA               27
        // 00                        28
        // 01 86 A0                  31
        // 03                        32
        // 5F EE EF C7 EA E3 36 BB F8 B9 B0 14 4C 86 EF 9D 90 00 9B
        //钱包余额 4 位  金额下限 1位  金额上限 3位
//        for (int i=0;i<8;i++){
//            pack[64+i] = cmd[i+24];
//        }
        // 计数信息 16位
//        for (int i=0;i<16;i++){
//            pack[72+i] = AA_84_INFO[i+33];
//        }
        //REU 36位
        for (int i = 0; i < 36; i++) {
            pack[88 + i] = (byte) 0x00;
        }

        //校验码4位
        //报文体内除  校验码   全部进行MD5   返回16个字节，截取前4个作为校验码  128-12
        byte[] sign_jym = new byte[116];
        for (int i = 0; i < 116; i++) {
            sign_jym[i] = pack[8 + i];
        }
        Log.e("AA_85_start1", bytesToHexString(pack));
        byte[] sign_jym_end = MD5.MD5_sign_byte(sign_jym);
        pack[124] = (byte) sign_jym_end[0];
        pack[125] = (byte) sign_jym_end[1];

        pack[126] = (byte) sign_jym_end[2];
        pack[127] = (byte) sign_jym_end[3];
        Log.e("AA_85_start", bytesToHexString(pack));
        try {
            order8 = getOrderCK_3DES(order8);
            for (int i = 0; i < 8; i++) {
                pack[index + i] = (byte) order8[i];
            }
        } catch (Exception e) {
            Log.e("AA_83_order_e", bytesToHexString(pack));
        }
        //AES 报文体加密  除握手流水号之外  128-16
        byte[] sign_aes_all = new byte[112];
        for (int i = 0; i < 112; i++) {
            sign_aes_all[i] = pack[16 + i];
        }

        try {
            byte[] sign_end = encrypt128_3(sign_aes_all, CZ_SK_16);
            for (int i = 0; i < 112; i++) {
                pack[16 + i] = sign_end[i];
            }
            Log.e("AA_85_aes_length", sign_end.length + "");
        } catch (Exception e) {
            Log.e("AA_85_sign_e", e.getMessage().toString());
        }
        StringBuilder sb = new StringBuilder();//非线程安全
        for (int i = 0; i < pack.length; i++) {
            sb.append(SerialDataUtils.Byte2Hex((pack[i]))).append(" ");
        }
        //E1 30 07 13 00 00 00 00 51 00 00 04 01 13 71 53
        Log.e("AA_85_enddd", bytesToHexString(pack));
        return pack;
    }

    /**
     * 联机充值  M1
     * P_M1_LOAD_QRY
     */
    public static byte[] P_M1_LOAD_QRY(String sign_info, String shid_info, byte[] cmd, String money) {
        Log.e("shid_info", sign_info);
        String[] manger = sign_info.split(" ");
        String test_info = "";
        //AES 秘钥
        CZ_SK_16 = new byte[16];
        for (int i = 0; i < 16; i++) {
            int num_info = Integer.valueOf(manger[i + 12], 16);
            CZ_SK_16[i] = (byte) num_info;
        }
        Log.e("AA_61_SK_16", bytesToHexString(CZ_SK_16));
        byte[] pack = new byte[160];
        pack[0] = (byte) 0xAA;
        pack[1] = (byte) 0x61;                                           //包头
        pack[2] = (byte) 0x00;
        pack[3] = (byte) 0x01;                                          //len 2字节
        pack[4] = (byte) 0x02;                                          //加密算法 1字节
        pack[5] = (byte) 0x80;                                          //排序方式 1字节
        pack[6] = (byte) 0x00;                                          //报文长度
        pack[7] = (byte) 0x98;                                          //报文长度 2字节
        //握手流水号 8位
//        int index = 8;
//        for (int i = 0; i < 8; i++) {
//            int num_info = Integer.valueOf(manger[i + 4], 16);
//            pack[index + i] = (byte) num_info;
////            test_info += (" " + manger[i+4]);
//        }
        //握手流水号 8位  15
        byte[] order8 = new byte[8];
        int index = 8;
        for (int i = 0; i < 8; i++) {
            int num_info = Integer.valueOf(manger[i + 4], 16);
            pack[index + i] = (byte) num_info;
            order8[i] = (byte) num_info;
        }
//报文序号，自增  4  19
        pack[16] = (byte) ((g_PACKET_INDEX >> 24) & 0x000000FF);
        pack[17] = (byte) ((g_PACKET_INDEX >> 16) & 0x000000FF);
        pack[18] = (byte) ((g_PACKET_INDEX >> 8) & 0x000000FF);
        pack[19] = (byte) (g_PACKET_INDEX & 0x000000FF);
        Log.e("AA61 g_PACKET_INDEX:", " " + g_PACKET_INDEX);
//        pack[16] = (byte) 0x00;
//        pack[17] = (byte) 0x00;
//        pack[18] = (byte) 0x00;
//        pack[19] = (byte) 0x00;
        //PKI管理卡号 4位  23
        String[] manger_shid = shid_info.trim().split(" ");
        int index_shid = 20;
        for (int i = 0; i < 4; i++) {
            int num_info_1 = Integer.valueOf(manger_shid[i], 16);
            pack[index_shid + i] = (byte) num_info_1;
            test_info += (" =" + manger_shid[i]);
        }

        // 物理卡号，逻辑卡号拼接 共16位  39
        int index_car = 24;
        for (int i = 0; i < 16; i++) {
            pack[index_car + i] = cmd[i + 4];
        }
//       卡物理信息1  32位  71
        for (int i = 0; i < 32; i++) {
            pack[40 + i] = cmd[i + 21];
        }
        //应用类型  00 商户圈存  01 自助圈存  20 充值金账户充值  22签约接口扣款  30卡间余额转移  40异常信息提交  50扩展应用
        //pack[72] = (byte) 0x00;
        pack[72] = (byte) 0x20; //充值金账户充值   这里不能修改，一定是充值金账户充值 2020.9.2
        //交易金额   50的整数
        byte[] money_byte = byte4ByInt(Integer.parseInt(money));
        pack[73] = (byte) money_byte[0];
        pack[74] = (byte) money_byte[1];
        pack[75] = (byte) money_byte[2];
        pack[76] = (byte) money_byte[3];

        //银行账户 16位 HEX 填充0x00
        for (int i = 0; i < 16; i++) {
            pack[77 + i] = (byte) 0x00;
        }
        //外部系统附加信息 16位 HEX 填充0x00  自定义 用于后期对账，后台是干嘛的？
        for (int i = 0; i < 16; i++) {
            pack[93 + i] = (byte) 0x00;
        }
        //扩展应用流水 8位
        for (int i = 0; i < 8; i++) {
            pack[109 + i] = (byte) 0x00;
        }
        //充值密码
        for (int i = 0; i < 16; i++) {
            pack[117 + i] = (byte) 0x00;
        }
        //RFU  23位
        for (int i = 0; i < 23; i++) {
            pack[133 + i] = (byte) 0x00;
        }
        //校验码4位
        //报文体内除  校验码   全部进行MD5   返回16个字节，截取前4个作为校验码  160-12
        byte[] sign_jym = new byte[148];
        for (int i = 0; i < 148; i++) {
            sign_jym[i] = pack[8 + i];
        }
        Log.e("AA_61_start1", bytesToHexString(pack));
        byte[] sign_jym_end = MD5.MD5_sign_byte(sign_jym);
        pack[156] = (byte) sign_jym_end[0];
        pack[157] = (byte) sign_jym_end[1];

        pack[158] = (byte) sign_jym_end[2];
        pack[159] = (byte) sign_jym_end[3];
        Log.e("CZ_AA_61_start", bytesToHexString(pack));
        try {
            order8 = getOrderCK_3DES(order8);
            for (int i = 0; i < 8; i++) {
                pack[index + i] = (byte) order8[i];
            }
        } catch (Exception e) {
            Log.e("AA_61_order_e", bytesToHexString(pack));
        }
        //AES 报文体加密  除握手流水号之外  160-16
        byte[] sign_aes_all = new byte[144];
        for (int i = 0; i < 144; i++) {
            sign_aes_all[i] = pack[16 + i];
        }

        try {
            byte[] sign_end = encrypt128_3(sign_aes_all, CZ_SK_16);
            for (int i = 0; i < 144; i++) {
                pack[16 + i] = sign_end[i];
            }
            Log.e("AA_61_aes_length", sign_end.length + "");
        } catch (Exception e) {
            Log.e("AA_61_sign_e", e.getMessage().toString());
        }
        StringBuilder sb = new StringBuilder();//非线程安全
        for (int i = 0; i < pack.length; i++) {
            sb.append(SerialDataUtils.Byte2Hex((pack[i]))).append(" ");
        }
        //E1 30 07 13 00 00 00 00 51 00 00 04 01 13 71 53
        Log.e("AA_61_enddd", bytesToHexString(pack));
        return pack;
    }

    /**
     * 联机充值  CPU
     * P_M1_LOAD
     */
    public static byte[] P_M1_LOAD(String sign_info, byte[] cmd) {
        Log.e("shid_info", bytesToHexString(cmd));
        //2C 67 AC 93
        String[] manger = sign_info.split(" ");
        //AES 秘钥
        CZ_SK_16 = new byte[16];
        for (int i = 0; i < 16; i++) {
            int num_info = Integer.valueOf(manger[i + 12], 16);
            CZ_SK_16[i] = (byte) num_info;
        }
        Log.e("AA_63_SK_16", bytesToHexString(CZ_SK_16));

        Log.e("AA_63_SK_num", bytesToHexString(AA_62_ORDER));
        byte[] pack = new byte[160];// len = 160-8
        pack[0] = (byte) 0xAA;
        pack[1] = (byte) 0x63;                                           //包头
        pack[2] = (byte) 0x00;
        pack[3] = (byte) 0x01;                                          //len 2字节
        pack[4] = (byte) 0x02;                                          //加密算法 1字节
        pack[5] = (byte) 0x80;                                          //排序方式 1字节
        pack[6] = (byte) 0x00;                                          //报文长度
        pack[7] = (byte) 0x98;                                          //报文长度 2字节
        //握手流水号 8位  15
        byte[] order8 = new byte[8];
        int index = 8;
        for (int i = 0; i < 8; i++) {
            int num_info = Integer.valueOf(manger[i + 4], 16);
            pack[index + i] = (byte) num_info;
            order8[i] = (byte) num_info;
        }
//报文序号，自增  4  19
        pack[16] = (byte) ((g_PACKET_INDEX >> 24) & 0x000000FF);
        pack[17] = (byte) ((g_PACKET_INDEX >> 16) & 0x000000FF);
        pack[18] = (byte) ((g_PACKET_INDEX >> 8) & 0x000000FF);
        pack[19] = (byte) (g_PACKET_INDEX & 0x000000FF);
        Log.e("AA63 g_PACKET_INDEX:", " " + g_PACKET_INDEX);
//        pack[16] = (byte) 0x00;
//        pack[17] = (byte) 0x00;
//        pack[18] = (byte) 0x00;
//        pack[19] = (byte) 0x00;
        //PKI管理卡号 4位  23
        for (int i = 0; i < 12; i++) {
            pack[20 + i] = cmd[i];
        }
        //系统流水号  AA62返回数据  8位
        for (int i = 0; i < 8; i++) {
            pack[32 + i] = AA_62_ORDER[i];
        }
        //交易信息 88位  卡片返回码 2位
        for (int i = 0; i < 88; i++) {
            pack[40 + i] = cmd[i + 20];
        }
        //REU 25位
        for (int i = 0; i < 28; i++) {
            pack[128 + i] = (byte) 0x00;
        }

        //校验码4位
        //报文体内除  校验码   全部进行MD5   返回16个字节，截取前4个作为校验码  160-12
        byte[] sign_jym = new byte[148];
        for (int i = 0; i < 148; i++) {
            sign_jym[i] = pack[8 + i];
        }
        Log.e("AA_63_start1", bytesToHexString(pack));
        byte[] sign_jym_end = MD5.MD5_sign_byte(sign_jym);
        pack[156] = (byte) sign_jym_end[0];
        pack[157] = (byte) sign_jym_end[1];

        pack[158] = (byte) sign_jym_end[2];
        pack[159] = (byte) sign_jym_end[3];
        Log.e("AA_63_start", bytesToHexString(pack));
        try {
            order8 = getOrderCK_3DES(order8);
            for (int i = 0; i < 8; i++) {
                pack[index + i] = (byte) order8[i];
            }
        } catch (Exception e) {
            Log.e("AA_63_order_e", bytesToHexString(pack));
        }
        //AES 报文体加密  除握手流水号之外  160-16
        byte[] sign_aes_all = new byte[144];
        for (int i = 0; i < 144; i++) {
            sign_aes_all[i] = pack[16 + i];
        }

        try {
            byte[] sign_end = encrypt128_3(sign_aes_all, CZ_SK_16);
            for (int i = 0; i < 144; i++) {
                pack[16 + i] = sign_end[i];
            }
            Log.e("AA_63_aes_length", sign_end.length + "");
        } catch (Exception e) {
            Log.e("AA_63_sign_e", e.getMessage().toString());
        }
        StringBuilder sb = new StringBuilder();//非线程安全
        for (int i = 0; i < pack.length; i++) {
            sb.append(SerialDataUtils.Byte2Hex((pack[i]))).append(" ");
        }
        //E1 30 07 13 00 00 00 00 51 00 00 04 01 13 71 53
        Log.e("AA_63_enddd", bytesToHexString(pack));
        return pack;
    }

    /**
     * 联机充值  CPU  冲正
     * P_M1_ROLLBACK
     */
    public static byte[] P_M1_ROLLBACK(String sign_info, byte[] cmd, String money) {
        Log.e("shid_info", sign_info);
        String[] manger = sign_info.split(" ");
        //AES 秘钥
        CZ_SK_16 = new byte[16];
        for (int i = 0; i < 16; i++) {
            int num_info = Integer.valueOf(manger[i + 12], 16);
            CZ_SK_16[i] = (byte) num_info;
        }
        Log.e("AA_65_SK_16", bytesToHexString(CZ_SK_16));
        byte[] pack = new byte[176];// len = 160-8
        pack[0] = (byte) 0xAA;
        pack[1] = (byte) 0x65;                                           //包头
        pack[2] = (byte) 0x00;
        pack[3] = (byte) 0x01;                                          //len 2字节
        pack[4] = (byte) 0x02;                                          //加密算法 1字节
        pack[5] = (byte) 0x80;                                          //排序方式 1字节
        pack[6] = (byte) 0x00;                                          //报文长度
        pack[7] = (byte) 0xA8;                                          //报文长度 2字节

        //握手流水号 8位  15
        byte[] order8 = new byte[8];
//        int index = 8;
        for (int i = 0; i < 8; i++) {
            int num_info = Integer.valueOf(manger[i + 4], 16);
            pack[8 + i] = (byte) num_info;
            order8[i] = (byte) num_info;
        }
        //报文序号
        pack[16] = (byte) ((g_PACKET_INDEX >> 24) & 0x000000FF);
        pack[17] = (byte) ((g_PACKET_INDEX >> 16) & 0x000000FF);
        pack[18] = (byte) ((g_PACKET_INDEX >> 8) & 0x000000FF);
        pack[19] = (byte) (g_PACKET_INDEX & 0x000000FF);
        Log.e("AA65 g_PACKET_INDEX:", " " + g_PACKET_INDEX);
//        pack[16] = (byte) 0x00;
//        pack[17] = (byte) 0x00;
//        pack[18] = (byte) 0x00;
//        pack[19] = (byte) 0x00;
//系统流水号，对应 AA62的系统流水号  8位
        for (int i = 0; i < 8; i++) {
            pack[20 + i] = AA_62_ORDER[i];
        }
        //PKI管理卡号 4位 物理卡号 8位 逻辑卡号 8位
        for (int i = 0; i < 20; i++) {
            pack[28 + i] = cmd[i + 4];
        }

        //冲正原因 1位   01写卡充值失败  02未收到62的报文或未向读卡器发送充值指令
        pack[48] = (byte) 0x00;
//        读卡器返回码
        pack[49] = (byte) 0x00;
        //冲正金额  4位 50的整数  112
//        byte[] money_byte = byte4ByInt(Integer.parseInt(money));
//        pack[50] = (byte) money_byte[0];
//        pack[51] = (byte) money_byte[1];
//        pack[52] = (byte) money_byte[2];
//        pack[53] = (byte) money_byte[3];

        pack[50] = (byte) cmd[112];
        pack[51] = (byte) cmd[113];
        pack[52] = (byte) cmd[114];
        pack[53] = (byte) cmd[115];

        //充值凭证号  4位
        for (int i = 0; i < 4; i++) {
            pack[54 + i] = (byte) AA_64_ORDER[44 + i];
        }

        //交易信息 88位  卡片返回码 2位
        for (int i = 0; i < 88; i++) {
            pack[58 + i] = cmd[i + 24];
        }
        //REU 25位
        for (int i = 0; i < 26; i++) {
            pack[146 + i] = (byte) 0x00;
        }

        //校验码4位
        //报文体内除  校验码   全部进行MD5   返回16个字节，截取前4个作为校验码  176-12
        byte[] sign_jym = new byte[164];
        for (int i = 0; i < 164; i++) {
            sign_jym[i] = pack[8 + i];
        }
        Log.e("AA_65_start1", bytesToHexString(pack));
        byte[] sign_jym_end = MD5.MD5_sign_byte(sign_jym);
        pack[172] = (byte) sign_jym_end[0];
        pack[173] = (byte) sign_jym_end[1];

        pack[174] = (byte) sign_jym_end[2];
        pack[175] = (byte) sign_jym_end[3];
        Log.e("AA_65_start", bytesToHexString(pack));
        try {
            order8 = getOrderCK_3DES(order8);
            for (int i = 0; i < 8; i++) {
                pack[8 + i] = (byte) order8[i];
            }
        } catch (Exception e) {
            Log.e("AA_65_order_e", bytesToHexString(pack));
        }
        //AES 报文体加密  除握手流水号之外  176-16
        byte[] sign_aes_all = new byte[160];
        for (int i = 0; i < 160; i++) {
            sign_aes_all[i] = pack[16 + i];
        }

        try {
            byte[] sign_end = encrypt128_3(sign_aes_all, CZ_SK_16);
            for (int i = 0; i < 160; i++) {
                pack[16 + i] = sign_end[i];
            }
            Log.e("AA_65_aes_length", sign_end.length + "");
        } catch (Exception e) {
            Log.e("AA_65_sign_e", e.getMessage().toString());
        }
        StringBuilder sb = new StringBuilder();//非线程安全
        for (int i = 0; i < pack.length; i++) {
            sb.append(SerialDataUtils.Byte2Hex((pack[i]))).append(" ");
        }
        //E1 30 07 13 00 00 00 00 51 00 00 04 01 13 71 53
        Log.e("AA_65_enddd", bytesToHexString(pack));
        return pack;
    }

    //修改后的M1冲正接口  rollbackbtype: 冲正原因
    public static byte[] P_M1_ROLLBACK2(String sign_info, byte[] cmd, byte rollbackbtype) {
        Log.e("AA_65_shid_info", sign_info);
        String[] manger = sign_info.split(" ");
        //AES 秘钥
        CZ_SK_16 = new byte[16];

        for (int i = 0; i < 16; i++) {
            int num_info = Integer.valueOf(manger[i + 12], 16);
            CZ_SK_16[i] = (byte) num_info;
        }
        Log.e("AA_65_SK_16", bytesToHexString(CZ_SK_16));
        byte[] pack = new byte[176];// len = 160-8
        System.arraycopy(cmd, 0, pack, 0, 176);
//        pack[0] = (byte) 0xAA;
//        pack[1] = (byte) 0x65;                                           //包头
//        pack[2] = (byte) 0x00;
//        pack[3] = (byte) 0x01;                                          //len 2字节
//        pack[4] = (byte) 0x02;                                          //加密算法 1字节
//        pack[5] = (byte) 0x80;                                          //排序方式 1字节
//        pack[6] = (byte) 0x00;                                          //报文长度
//        pack[7] = (byte) 0xA8;                                          //报文长度 2字节

        //握手流水号 8位  15
        byte[] order8 = new byte[8];
//        int index = 8;
        for (int i = 0; i < 8; i++) {
            int num_info = Integer.valueOf(manger[i + 4], 16);
            pack[8 + i] = (byte) num_info;
            order8[i] = (byte) num_info;
        }
        //报文序号
        pack[16] = (byte) ((g_PACKET_INDEX >> 24) & 0x000000FF);
        pack[17] = (byte) ((g_PACKET_INDEX >> 16) & 0x000000FF);
        pack[18] = (byte) ((g_PACKET_INDEX >> 8) & 0x000000FF);
        pack[19] = (byte) (g_PACKET_INDEX & 0x000000FF);
        Log.e("AA65 g_PACKET_INDEX:", "报文序号： " + g_PACKET_INDEX);
        Log.e("AA65 PACKET INFO:", "AA62的系统流水号:" + SerialDataUtils.Bytes2HexString(pack, 20, 8));
        Log.e("AA65 PACKET INFO:", "PKI卡号:" + SerialDataUtils.Bytes2HexString(pack, 28, 4));
        Log.e("AA65 PACKET INFO:", "物理卡号:" + SerialDataUtils.Bytes2HexString(pack, 32, 8));
        Log.e("AA65 PACKET INFO:", "逻辑卡号:" + SerialDataUtils.Bytes2HexString(pack, 40, 8));
//        pack[16] = (byte) 0x00;
//        pack[17] = (byte) 0x00;
//        pack[18] = (byte) 0x00;
//        pack[19] = (byte) 0x00;
//系统流水号，对应 AA62的系统流水号  8位
//        for (int i =0;i<8;i++){
//            pack[20+i]=AA_62_ORDER[i];
//        }
        //PKI管理卡号 4位 物理卡号 8位 逻辑卡号 8位
//        for (int i =0;i<20;i++){
//            pack[28+i] = cmd[i+4];
//        }

        //冲正原因 1位   01写卡充值失败  02未收到62的报文或未向读卡器发送充值指令
        pack[48] = (byte) rollbackbtype;
        Log.e("AA65 PACKET INFO:", "冲正原因:" + SerialDataUtils.Bytes2HexString(pack, 48, 1));
        Log.e("AA65 PACKET INFO:", "读卡器返回码:" + SerialDataUtils.Bytes2HexString(pack, 49, 1));
        Log.e("AA65 PACKET INFO:", "冲正金额:" + SerialDataUtils.Bytes2HexString(pack, 50, 4));
        Log.e("AA65 PACKET INFO:", "充值凭证号:" + SerialDataUtils.Bytes2HexString(pack, 54, 4));
//        读卡器返回码
        //pack[49] = (byte)0x00;
        //冲正金额  4位 50的整数  112
//        byte[] money_byte = byte4ByInt(Integer.parseInt(money));
//        pack[50] = (byte) money_byte[0];
//        pack[51] = (byte) money_byte[1];
//        pack[52] = (byte) money_byte[2];
//        pack[53] = (byte) money_byte[3];

//        pack[50] = (byte) cmd[112];
//        pack[51] = (byte) cmd[113];
//        pack[52] = (byte) cmd[114];
//        pack[53] = (byte) cmd[115];

        //充值凭证号  4位
//        for (int i=0;i<4;i++){
//            pack[54+i] = (byte)AA_64_ORDER[44+i];
//        }

        if (rollbackbtype == 0x02) {
            byte[] binfo = new byte[88];
            SerialDataUtils.SetByteArray(binfo, (byte) 0x00, 88);
            System.arraycopy(binfo, 0, pack, 58, 88);//交易信息 88
        }
        Log.e("AA65 PACKET INFO:", "交易信息:" + SerialDataUtils.Bytes2HexString(pack, 58, 88));
        //交易信息 88位  卡片返回码 2位
//        for (int i=0;i<88;i++){
//            pack[58+i] =cmd[i+24];
//        }
        //REU 25位
//        for (int i=0;i<26;i++){
//            pack[146+i] = (byte)0x00;
//        }

        //校验码4位
        //报文体内除  校验码   全部进行MD5   返回16个字节，截取前4个作为校验码  176-12
        byte[] sign_jym = new byte[164];
        System.arraycopy(pack, 8, sign_jym, 0, 164);
//        for (int i = 0; i < 164; i++) {
//            sign_jym[i] = pack[8 + i];
//        }
        Log.e("AA_65_start1", bytesToHexString(pack));
        byte[] sign_jym_end = MD5.MD5_sign_byte(sign_jym);
        System.arraycopy(sign_jym_end, 0, pack, 172, 4);
//        pack[172] = (byte) sign_jym_end[0];
//        pack[173] = (byte) sign_jym_end[1];
//        pack[174] = (byte) sign_jym_end[2];
//        pack[175] = (byte) sign_jym_end[3];
        Log.e("AA_65_start", bytesToHexString(pack));
        try {
            order8 = getOrderCK_3DES(order8);
            System.arraycopy(order8, 0, pack, 8, 8);
//            for (int i = 0; i < 8; i++) {
//                pack[8 + i] = (byte) order8[i];
//            }
        } catch (Exception e) {
            Log.e("AA_65_order_e", bytesToHexString(pack));
        }
        //AES 报文体加密  除握手流水号之外  176-16
        byte[] sign_aes_all = new byte[160];
        System.arraycopy(pack, 16, sign_aes_all, 0, 160);
//        for (int i = 0; i < 160; i++) {
//            sign_aes_all[i] = pack[16 + i];
//        }

        try {
            byte[] sign_end = encrypt128_3(sign_aes_all, CZ_SK_16);
            System.arraycopy(sign_end, 0, pack, 16, 160);
//            for (int i = 0; i < 160; i++) {
//                pack[16 + i] = sign_end[i];
//            }
            Log.e("AA_65_aes_length", sign_end.length + "");
        } catch (Exception e) {
            Log.e("AA_65_sign_e", e.getMessage().toString());
        }
        StringBuilder sb = new StringBuilder();//非线程安全
        for (int i = 0; i < pack.length; i++) {
            sb.append(SerialDataUtils.Byte2Hex((pack[i]))).append(" ");
        }
        //E1 30 07 13 00 00 00 00 51 00 00 04 01 13 71 53
        Log.e("AA_65_enddd", bytesToHexString(pack));
        return pack;
    }
    //统计信息查询

    public static byte[] cz_search_tjinfo(String sign_info, String shid_info, byte[] cmd) {
        Log.e("shid_info", sign_info);
        String[] manger = sign_info.split(" ");
        String test_info = "";        //AES 秘钥
        CZ_SK_16 = new byte[16];
        for (int i = 0; i < 16; i++) {
            int num_info = Integer.valueOf(manger[i + 12], 16);
            CZ_SK_16[i] = (byte) num_info;
        }
        Log.e("CZ_SK_16", bytesToHexString(CZ_SK_16));
        byte[] pack = new byte[48];
        pack[0] = (byte) 0xFA;
        pack[1] = (byte) 0x61;                                           //包头
        pack[2] = (byte) 0x00;
        pack[3] = (byte) 0x02;                                          //len 2字节
        pack[4] = (byte) 0x02;                                          //加密算法 1字节
        pack[5] = (byte) 0x80;                                          //排序方式 1字节
        pack[6] = (byte) 0x00;                                          //报文长度
        pack[7] = (byte) 0x28;                                          //报文长度 2字节
        //握手流水号 8位
        byte[] order8 = new byte[8];
        int index = 8;
        for (int i = 0; i < 8; i++) {
            int num_info = Integer.valueOf(manger[i + 4], 16);
            pack[index + i] = (byte) num_info;
//            test_info += (" " + manger[i+4]);
            order8[i] = (byte) num_info;
        }
//        Log.e("3des_原文",bytesToHexString(order8));
//        try {
//            order8 = getOrderCK_3DES(order8);
//            for (int i = 0; i < 8; i++) {
//                pack[index + i] = (byte) order8[i];
//            }
//        }catch (Exception e){
//            Log.e("3des_e",e.getMessage().toString());
//        }
        Log.e("3des_密文", bytesToHexString(order8));
        pack[16] = (byte) ((g_PACKET_INDEX >> 24) & 0x000000FF);
        pack[17] = (byte) ((g_PACKET_INDEX >> 16) & 0x000000FF);
        pack[18] = (byte) ((g_PACKET_INDEX >> 8) & 0x000000FF);
        pack[19] = (byte) (g_PACKET_INDEX & 0x000000FF);
        Log.e("FA61 g_PACKET_INDEX:", " " + g_PACKET_INDEX);
//        pack[16] = (byte) 0x00;
//        pack[17] = (byte) 0x00;
//        pack[18] = (byte) 0x00;
//        pack[19] = (byte) 0x00;//报文序号，自增
        //PKI管理卡号 4位
        String[] manger_shid = shid_info.trim().split(" ");
        int index_shid = 20;
        for (int i = 0; i < 4; i++) {
            int num_info_1 = Integer.valueOf(manger_shid[i], 16);
            pack[index_shid + i] = (byte) num_info_1;
            test_info += (" =" + manger_shid[i]);
        }
        // RFU
        for (int i = 0; i < 20; i++) {
            pack[24 + i] = (byte) 0x00;
        }
        //校验码4位
        //报文体内除  校验码   全部进行MD5   返回16个字节，截取前4个作为校验码
        byte[] sign_jym = new byte[36];
        for (int i = 0; i < 36; i++) {
            sign_jym[i] = pack[8 + i];
        }
        Log.e("md5_jym1", bytesToHexString(sign_jym));
//        Log.e("md5_jym2",MD5.MD5_sign(sign_jym)) ;
        Log.e("md5_jym3", YCTMD5Util.MD5Encode(SerialDataUtils.bytes2HexString(sign_jym), "UTF-8"));
        Log.e("md5_jym4", bytesToHexString(SerialDataUtils.hexString2Bytes(YCTMD5Util.MD5Encode(SerialDataUtils.bytes2HexString(sign_jym), "UTF-8"))));
        // FC 7B B8 23
        //E7 56 76 6E AF D3 BE AE
        //16位 取前4位
//        byte[] sign_jym_16 = md5_16_deal(sign_jym);
//
//        pack[44] = (byte) sign_jym_16[0];
//        pack[45] = (byte) sign_jym_16[1];
//        pack[46] = (byte) sign_jym_16[2];
//        pack[47] = (byte) sign_jym_16[3];

        //32位 取前4位
        byte[] sign_jym_end = MD5.MD5_sign_byte(sign_jym);
//        byte[] sign_jym_end = SerialDataUtils.hexString2Bytes(YCTMD5Util.MD5Encode(SerialDataUtils.bytes2HexString(sign_jym),"UTF-8"));
        pack[44] = (byte) sign_jym_end[0];
        pack[45] = (byte) sign_jym_end[1];
        pack[46] = (byte) sign_jym_end[2];
        pack[47] = (byte) sign_jym_end[3];

        Log.e("FA_61_3des_原文", bytesToHexString(pack));
        try {
            order8 = getOrderCK_3DES(order8);
            for (int i = 0; i < 8; i++) {
                pack[index + i] = (byte) order8[i];
            }
        } catch (Exception e) {
            Log.e("3des_e", e.getMessage().toString());
        }

        Log.e("FA_61_aes_info", bytesToHexString(pack));
        //AES 报文体加密  除握手流水号之外
        byte[] sign_aes_all = new byte[32];
        for (int i = 0; i < 32; i++) {
            sign_aes_all[i] = pack[16 + i];
        }

        try {
            byte[] sign_end = encrypt128_3(sign_aes_all, CZ_SK_16);
            for (int i = 0; i < 32; i++) {
                pack[16 + i] = sign_end[i];
            }
            Log.e("FA_61_aes_length", sign_end.length + "");
        } catch (Exception e) {
            Log.e("FA_61_sign_e", e.getMessage().toString());
        }
        StringBuilder sb = new StringBuilder();//非线程安全
        for (int i = 0; i < pack.length; i++) {
            sb.append(SerialDataUtils.Byte2Hex((pack[i]))).append(" ");
        }
        //E1 30 07 13 00 00 00 00 51 00 00 04 01 13 71 53
        Log.e("FA_61_enddd", bytesToHexString(pack));
        return pack;
    }


    public static byte[] cz_search_tjinfo_test() {
//        Log.e("shid_info", sign_info);
        String[] manger = "C6 F4 0C F2 5F AF D4 95 4D 44 58 46 6B D9 3A B3".split(" ");
        String test_info = "";
        //AES 秘钥
        CZ_SK_16 = new byte[16];
        for (int i = 0; i < 16; i++) {
            int num_info = Integer.valueOf(manger[i + 12], 16);
            CZ_SK_16[i] = (byte) num_info;
        }
        Log.e("CZ_SK_16", bytesToHexString(CZ_SK_16));
        byte[] pack = new byte[48];
        pack[0] = (byte) 0xFA;
        pack[1] = (byte) 0x61;                                           //包头
        pack[2] = (byte) 0x00;
        pack[3] = (byte) 0x02;                                          //len 2字节
        pack[4] = (byte) 0x02;                                          //加密算法 1字节
        pack[5] = (byte) 0x80;                                          //排序方式 1字节
        pack[6] = (byte) 0x00;                                          //报文长度
        pack[7] = (byte) 0x28;                                          //报文长度 2字节
        pack[0] = (byte) 0x00;
        pack[1] = (byte) 0x02;                                           //包头
        pack[2] = (byte) 0x88;
        pack[3] = (byte) 0xE4;                                          //len 2字节
        pack[4] = (byte) 0x1E;                                          //加密算法 1字节
        pack[5] = (byte) 0xFC;                                          //排序方式 1字节
        pack[6] = (byte) 0xBF;                                          //报文长度
        pack[7] = (byte) 0xBD;                                          //报文长度 2字节

        pack[0] = (byte) 0xFF;
        pack[1] = (byte) 0x3B;                                           //包头
        pack[2] = (byte) 0x3B;
        pack[3] = (byte) 0x30;                                          //len 2字节
        pack[4] = (byte) 0x1E;                                          //加密算法 1字节
        pack[5] = (byte) 0xFC;                                          //排序方式 1字节
        pack[6] = (byte) 0xBF;                                          //报文长度
        pack[7] = (byte) 0xBD;                                          //报文长度 2字节


        Log.e("FA_61_aes_info", bytesToHexString(pack));
        //AES 报文体加密  除握手流水号之外
        byte[] sign_aes_all = new byte[32];
        for (int i = 0; i < 32; i++) {
            sign_aes_all[i] = pack[16 + i];
        }

        try {
            byte[] sign_end = encrypt128_3(sign_aes_all, CZ_SK_16);
            for (int i = 0; i < 32; i++) {
                pack[16 + i] = sign_end[i];
            }
            Log.e("FA_61_aes_length", sign_end.length + "");
        } catch (Exception e) {
            Log.e("FA_61_sign_e", e.getMessage().toString());
        }
        StringBuilder sb = new StringBuilder();//非线程安全
        for (int i = 0; i < pack.length; i++) {
            sb.append(SerialDataUtils.Byte2Hex((pack[i]))).append(" ");
        }
        //E1 30 07 13 00 00 00 00 51 00 00 04 01 13 71 53
        Log.e("FA_61_enddd", bytesToHexString(pack));
        return pack;
    }

//    //交易类型查询  113
//    private static byte[] type_search(byte[] cmd) {
//        Log.e("CZStringTobyte1", bytesToHexString(cmd));
//        byte[] PKI_ID = new byte[4]; //管理卡号
//        PKI_ID = CZStringTobyte();
//        byte[] CZ_ORDER = new byte[8];//握手流水号 8位
//        CZ_ORDER = CZStringTobyteORDER();
//        byte[] pack = new byte[112];
//        pack[0] = (byte) 0xAA;
//        pack[1] = (byte) 0x51;                                           //包头AA 51 2字节
//        pack[2] = (byte) 0x00;
//        pack[3] = (byte) 0x03;                                          //报文版本
//        pack[4] = (byte) 0x02;                                          //加密算法 1字节
//        pack[5] = (byte) 0x80;                                          //排序方式 1字节
//        pack[6] = (byte) 0x00;
//        pack[7] = (byte) 0x68;                                          //报文长度 2字节
//        //握手流水号，前两个填充FF，后面字节PASM卡号  8位
//        for (int i = 0; i < 8; i++) {
//            pack[8 + i] = (byte) CZ_ORDER[i];
//        }
//
//        //报文序号 保留 填充0x00
//        pack[16] = (byte) 0x00;
//        pack[17] = (byte) 0x00;
//        pack[18] = (byte) 0x00;
//        pack[19] = (byte) 0x00;
//        //PKI管理卡号  4位
//        pack[20] = (byte) PKI_ID[0];
//        pack[21] = (byte) PKI_ID[1];
//        pack[22] = (byte) PKI_ID[2];
//        pack[23] = (byte) PKI_ID[3];
////        物理卡号 8位  逻辑卡号 8位   16位
//        for (int i = 0; i < 16; i++) {
//            pack[24 + i] = (byte) cmd[i + 4];
//        }
//        //SAK  1位
//        pack[40] = (byte) 0x00;
//        //物理卡信息 32位
//        for (int i = 0; i < 32; i++) {
//            pack[41 + i] = (byte) cmd[i + 4];
//        }
//
//        /**
//         * 0x00  执行余额查询操作
//         * 0x01  执行普通充值操作
//         * 0x02  执行撤销操作
//         * 0x03  执行扩展应用操作
//         * 0x04  执行用户账户充值
//         * 0x05  执行异常信息提交
//         */
//        //执行操作  1位
//        pack[73] = 0x01;
//        //RFU 填充 0x00
//        for (int i = 0; i < 34; i++) {
//            pack[74 + i] = (byte) 0x00;
//        }
//        //校验码  4位
//        for (int i = 0; i < 4; i++) {
//            pack[108 + i] = (byte) 0x00;
//        }
//
//        byte[] md5_info = new byte[100];
//        MD5.MD5_sign(md5_info);
//
//        StringBuilder sb = new StringBuilder();//非线程安全
//        for (int i = 0; i < pack.length; i++) {
//            sb.append(SerialDataUtils.Byte2Hex((pack[i]))).append(" ");
//        }
//        Log.e("TAG113", pack.length + " == agree_sendCmds_car： " + sb);
//
//        return pack;
//    }

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

    /**
     * 联机消费授权串口
     *
     * @param cmd
     * @return
     */
    private static byte[] check_pay_agree_1(byte[] cmd) {
        Log.e("test_123__11", " =========" + Constant.Companion.getCAR_SIGN_19().toString());
        Log.e("test_123__22", " =========" + Constant.Companion.getCAR_SIGN_65().toString());
//        byte[] CAR_SIGN_19 = App.Companion.getSpUtil().getString("CAR_SIGN_19","").getBytes();
//        byte[] CAR_SIGN_65 = App.Companion.getSpUtil().getString("CAR_SIGN_65","").getBytes();
        Log.e("PayStringTobyte1", bytesToHexString(PayStringTobyte(1)));
        Log.e("PayStringTobyte2", bytesToHexString(PayStringTobyte(2)));
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
        pack[31] = (byte) Constant.PortNumber2;
        pack[32] = (byte) Constant.PortNumber3;
        pack[33] = (byte) Constant.PortNumber4;
        pack[34] = (byte) Constant.PortNumber5;
        pack[35] = (byte) Constant.PortNumber6;
        //交易类型  0A = 消费
        pack[36] = (byte) 0x0A;
        //交易查询信息
        /**
         * 模拟数据
         * BD 37 B4 00
         * 01 01 72 06 13 00 00 00 00 51 00 00 05 00 11 21 12 00 00 00 00 00 01 03 00 00 01
         * 86 A0 00 00 00 00 28 00 04 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
         * 26
         */
        for (int i = 0; i < 52; i++) {
            pack[37 + i] = (byte) cmd[i + 4];
        }
        //交易金额  高位在前  4位 分
        pack[89] = byte4ByInt(1)[0];
        pack[90] = byte4ByInt(1)[1];
        pack[91] = byte4ByInt(1)[2];
        pack[92] = byte4ByInt(1)[3];

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
    private static byte[] check_pay_upload_M1(byte[] cmd) {
        Log.e("M1_123__load", " =========" + bytesToHexString(cmd));
        Log.e("M1_PayStringTobyte1", bytesToHexString(PayStringTobyte(1)));
        Log.e("M1_PayStringTobyte2", bytesToHexString(PayStringTobyte(2)));
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
        pack[30] = (byte)  Constant.PortNumber1;
        pack[31] = (byte)  Constant.PortNumber2;
        pack[32] = (byte)  Constant.PortNumber3;
        pack[33] = (byte)  Constant.PortNumber4;
        pack[34] = (byte)  Constant.PortNumber5;
        pack[35] = (byte)  Constant.PortNumber6;
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

    private static byte[] check_pay_upload_CPU(byte[] cmd) {
        Log.e("CPU_123__load", " =========" + bytesToHexString(cmd));
        Log.e("CPU_PayStringTobyte1", bytesToHexString(PayStringTobyte(1)));
        Log.e("CPU_PayStringTobyte2", bytesToHexString(PayStringTobyte(2)));
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
        pack[30] = (byte)  Constant.PortNumber1;
        pack[31] = (byte)  Constant.PortNumber2;
        pack[32] = (byte)  Constant.PortNumber3;
        pack[33] = (byte)  Constant.PortNumber4;
        pack[34] = (byte)  Constant.PortNumber5;
        pack[35] = (byte)  Constant.PortNumber6;
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

    private static byte[] check_pay_upload_Black(byte[] cmd) {
        Log.e("black_123__load", " =========" + bytesToHexString(cmd));
        Log.e("black_PayStringTobyte1", bytesToHexString(PayStringTobyte(1)));
        Log.e("black_PayStringTobyte2", bytesToHexString(PayStringTobyte(2)));
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
        pack[29] = (byte) bytes5[3];
        //终端编号
        pack[30] = (byte)  Constant.PortNumber1;
        pack[31] = (byte)  Constant.PortNumber2;
        pack[32] = (byte)  Constant.PortNumber3;
        pack[33] = (byte)  Constant.PortNumber4;
        pack[34] = (byte)  Constant.PortNumber5;
        pack[35] = (byte)  Constant.PortNumber6;
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

    private static byte[] listTobyte(List<Byte> list) {
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

    public void pay_des_buy(byte[] temp) {
        //des 解密密钥
        byte[] CAR_SIGN_65 = new byte[8];
        CAR_SIGN_65 = PayStringTobyte(2);
        byte[] des_body = new byte[temp.length - 16];
        for (int i = 0; i < (temp.length - 16); i++) {
            des_body[i] = temp[i + 16];
        }

        try {
            des_body = deCrypto_2(des_body, CAR_SIGN_65);
            Log.e("des_buy_1", des_body.length + "");
            Log.e("des_buy_2", bytesToHexString(des_body));
            system_order_b0 = new byte[des_body.length];
            system_order_b0 = des_body;
            if (des_body[4] == (byte) 0x00 && des_body[5] == (byte) 0x01) {
                //允许消费，购买流程开始
                showInfo = "读卡成功";
                showState = 0;
            } else if (des_body[4] == (byte) 0x00 && des_body[5] == (byte) 0x0A) {
                //开始黑名单捕获流程
                showInfo = "黑名单捕获流程";
                showState = 1;
            } else if (des_body[4] == (byte) 0x00 && des_body[5] == (byte) 0x0E) {
                showInfo = "此卡片禁止消费";
                showState = 2;
            } else if (des_body[4] == (byte) 0x00 && des_body[5] == (byte) 0x0D) {
                showInfo = "此卡片需要重新激活";
                showState = 3;
            } else if (des_body[4] == (byte) 0xE0 && des_body[5] == (byte) 0x01) {
                showInfo = "此卡片未注册";
                showState = 3;
            } else {
                showInfo = "此卡片禁止消费";
                showState = 2;
            }
            handler.sendEmptyMessage(4);
        } catch (Exception e) {
        }

    }

//    public void pay_des_upload(byte[] temp) {
//        //des 解密密钥
//        byte[] CAR_SIGN_65 = new byte[8];
//        CAR_SIGN_65 = PayStringTobyte(2);
//        byte[] des_body = new byte[temp.length - 16];
//        for (int i = 0; i < (temp.length - 16); i++) {
//            des_body[i] = temp[i + 16];
//        }
//
//        try {
//            des_body = deCrypto_2(des_body, CAR_SIGN_65);
//            Log.e("des_upload_1", des_body.length + "");
//            Log.e("des_upload_2", bytesToHexString(des_body));
//            system_order_b0 = new byte[des_body.length];
//            system_order_b0 = des_body;
//            if (des_body[4] == (byte) 0x00 && des_body[5] == (byte) 0x00) {
//                //提交成功
//                showInfo = "提交成功";
//                showState = 0;
//            } else if (des_body[4] == (byte) 0x00 && des_body[5] == (byte) 0x02) {
//                //此记录已冲正
//                showInfo = "此记录已冲正";
//                showState = 1;
//            } else if (des_body[4] == (byte) 0x00 && des_body[5] == (byte) 0x0F) {
//                showInfo = "联机授权错误";
//                showState = 2;
//            } else if (des_body[4] == (byte) 0x00 && des_body[5] == (byte) 0x04) {
//                showInfo = "交易金额错误";
//                showState = 3;
//            }
//            handler.sendEmptyMessage(5);
//        } catch (Exception e) {
//        }
//
//    }

    //存储 AA82返回数据，CPU握手流水号 系统流水号
    private static byte[] AA_82_ORDERNUM = null;
    //存储 AA84返回数据
    private static byte[] AA_84_INFO = null;

    // 存储AA62的发挥数据 M1握手流水号  系统流水号
    private static byte[] AA_62_ORDER = null;

    //存储AA64返回数据  用于M1充值最后的冲正操作
    private static byte[] AA_64_ORDER = null;

    private static long lErrCode = 0;

    public void CZ_des_info(byte[] temp, String state) {
        //des 解密密钥
        byte[] des_body = new byte[temp.length - 16];
        for (int i = 0; i < (temp.length - 16); i++) {
            des_body[i] = temp[i + 16];
        }
        Log.i("CZ_des_info", "temp: " + bytesToHexString(temp));
        try {
            des_body = decrypt128_3(des_body, CZ_SK_16);
//            Log.e("CZ_des_1",  state +" : " + bytesToHexString(CZ_SK_16));
            Log.e("CZ_des_2", state + " : " + bytesToHexString(des_body));
            //if (bytesToHexString(des_body).replace(" ", "").startsWith("00000000")) {
            //bytesToHexString(des_body).replace(" ", "");

            Log.i("CZ_des_info", bytesToHexString(des_body));
            System.arraycopy(des_body, 0, buff, 0, des_body.length);
            Log.i("CZ_des_info", bytesToHexString(buff));
            if (state.equals("52")) {
                int iResult = SerialDataUtils.ByteMemCmp(des_body, 0, bPacketStatus, 0, 4);//报文状态码
                if (iResult != 0) {
                    String strPacketStatus = SerialDataUtils.Bytes2HexString(des_body, 0, 4);
                    Log.e("yct error info", "交易类型查询失败！ AA52状态码:0x" + strPacketStatus);
                    showInfo = "交易类型查询失败！ AA52状态码:0x" + strPacketStatus;
                    showState = 9;
                    handler.sendEmptyMessage(5);
                    iResult = SerialDataUtils.ByteMemCmp(des_body, 0, bLoginStatus, 0, 4);
                    if (iResult == 0) {
                        //重新签到
                        Log.e("yct error info", "报文序号错误，需要重新签到!");
                        CarSerialPortUtil.getInstance().test_card();
                    }
                } else {
                    //des_body[4] :交易类型
                    if (des_body[4] == (byte) 0x00) {
                        //M1钱包--现金充值
                        //CarSerialPortUtil.getInstance().R_PUB_SET_READCARDINFO(des_body,"m1",5);
                    } else if (des_body[4] == (byte) 0x02) {
                        //M1 账户充值
                        CarSerialPortUtil.getInstance().R_PUB_SET_READCARDINFO(des_body, "m1", 5);
                    } else if (des_body[4] == (byte) 0x20) {
                        //CPU 账户充值
                        //CPU卡设置  从此开始
                        //CarSerialPortUtil.getInstance().R_PUB_SET_READCARDINFO(des_body,"cpu",5);
                    } else if (des_body[4] == (byte) 0x21) {
                        //OC3.CPU 圈存（M1余额转移）
                    } else if (des_body[4] == (byte) 0x22) {
                        //OC3.CPU 圈存 CPU用户账户充值
                        CarSerialPortUtil.getInstance().R_PUB_SET_READCARDINFO(des_body, "cpu", 5);
                    } else if (des_body[4] == (byte) 0x30) {
                        //执行扩展应用操作
                    } else if (des_body[4] == (byte) 0x31) {
                        //执行扩展应用查询
                    } else if (des_body[4] == (byte) 0x40) {
                        //执行黑名单处理
                        showInfo = "黑名单卡，充值失败！";
                        showState = 9;
                        handler.sendEmptyMessage(5);
                    } else if (des_body[4] == (byte) 0x60) {
                        //CPU卡同步
                        //卡片需要同步，请到羊城通客服点操作!
                        showInfo = "卡片需要同步，请到羊城通客服点操作!";
                        showState = 9;  //?
                        handler.sendEmptyMessage(5);   //?
                    }
                }

            } else if (state.equals("62")) {
                Log.e("yct info", "正在充值中，请勿移动卡片...");
                showInfo = "正在充值中，请勿移动卡片...";
                showState = 9;
                handler.sendEmptyMessage(5);
                int iResult = SerialDataUtils.ByteMemCmp(des_body, 8, bPacketStatus, 0, 4);//报文状态码
                if (iResult != 0) {
                    String strPacketStatus = SerialDataUtils.Bytes2HexString(des_body, 8, 4);
                    showInfo = "充值失败！ AA62状态码:0x" + strPacketStatus;
                    showState = 9;
                    handler.sendEmptyMessage(5);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    showInfo = "充值失败!\n请您到[飞充]\n继续完成卡片充值!\n";
                    showState = 9;
                    handler.sendEmptyMessage(5);
                } else {
                    //m1卡操作 设置信息 跟CPU一样
                    AA_62_ORDER = new byte[des_body.length];
                    AA_62_ORDER = des_body; //从系统流水号开始

                    System.arraycopy(AA_62_ORDER, 0, g_SYSTEMSEQ_LOAD, 0, g_SYSTEMSEQ_LOAD.length);//AA62 系统流水号
                    CarSerialPortUtil.set_SYSTEMSEQ_LOAD(SerialDataUtils.Bytes2HexString(g_SYSTEMSEQ_LOAD, 0, g_SYSTEMSEQ_LOAD.length));//保存系统流水号
                    Log.i("AA62 系统流水号:", SerialDataUtils.Bytes2HexString(g_SYSTEMSEQ_LOAD, 0, g_SYSTEMSEQ_LOAD.length));

                    CarSerialPortUtil.getInstance().R_PUB_SET_READCARDINFO(des_body, "m1_2", 19);
                    //6B 70 BA 6D 58 85 FF 86 67 A9 68 9A B9 5F F5 2B D5 30 BD 58 06 DF 74 7E DF 2A 1D 0A 00 6B D2 44 D5 30 BD 58 06 DF 74 7E D5 30 BD 58 06 DF 74 7E
//                    BA 3A 95 51 00 00 02 99 88 87 80 7F EA 6A 11 19 14 07 B3 26 53 EF 71 EC 5C 69 B6 B2 39 B6 2B 44 6D 69 DB 6C 9F 5C 86 15 E5 56 26 B2 39 B6 2B 44 6D 69 DB B2 39 B6 2B 44 6D 69 DB A7
//                    00 00 00 00 00 46 87 A2 00 00 00 00 20 20 07 22 15 09 51 7F EA 6A 11 19 14 07 B3 26 53 EF 71 EC 5C 69 B6 B2 39 B6 2B 44 6D 69 DB 6C 9F 5C 86 15 E5 56 26 B2 39 B6 2B 44 6D 69 DB B2 39 B6 2B 44 6D 69 DB 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 6D 98 AC 43
                }

            } else if (state.equals("64")) {
                Log.e("yct info", "正在充值中，请勿移动卡片...");
                showInfo = "正在充值中，请勿移动卡片...";
                showState = 9;
                handler.sendEmptyMessage(5);
                if (null == des_body)//处理解包异常的情况
                {
                    //接收数据异常需要发起冲正，冲正原因02
                    //logger.info("冲正中。。。");
                    //logger.info("2发送.接收数据异常需要发起冲正，冲正原因02..");
                    Log.i("yct error info:", "2发送.接收数据异常需要发起冲正，冲正原因02..");
                    String strRecord = CarSerialPortUtil.ReadRollBackRecord();
                    if (null != strRecord) {
                        byte[] str_buff = new byte[200];
                        str_buff = SerialDataUtils.HexString2Bytes(strRecord);
                        byte[] ucTempData = new byte[]{(byte) 0xAA, 0x65, 0x00, 0x01, 0x02, (byte) 0x80, 0x00, (byte) 0xA8};
                        if (SerialDataUtils.ByteMemCmp(str_buff, 0, ucTempData, 0, bPacketHead.length) != 0) {
                            //写冲正记录时，记录没写进去，或者数据乱了
                            Log.i("yct error info:", "冲正记录异常，无法做M1冲正!");
                            showInfo = "冲正记录异常,冲正失败！";
                            showState = 9;
                            handler.sendEmptyMessage(5);
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            showInfo = "充值失败!\n请联系羊城通公司！\n";
                            showState = 9;
                            handler.sendEmptyMessage(5);
                        } else {
                            RollBackStatus = 0x02;//冲正状态
                            getSendSocketInfo(str_buff, 403, "0", "0");
                        }
                    } else {
                        Log.i("yct error info:", "冲正记录读取异常，未能做M1冲正!");
                        Log.i("yct error info:", "系统异常!");
                        Log.i("yct error info:", "系统需要维护!");
                        showInfo = "冲正记录异常,冲正失败！";
                        showState = 9;
                        handler.sendEmptyMessage(5);
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        showInfo = "充值失败!\n请联系羊城通公司！\n";
                        showState = 9;
                        handler.sendEmptyMessage(5);
                    }
                } else {
                    String ssstr = null;
                    ssstr = bytesToHexString(des_body);
                    byte[] ttemp = new byte[des_body.length];
                    Log.i("yct show info1:", SerialDataUtils.bytes2HexString(ttemp));
                    Log.i("yct show info2:", ssstr);
                    int ires = 0;
                    ires = SerialDataUtils.ByteMemCmp(des_body, 8, bPacketStatus, 0, 4);//报文状态码
                    if (ires != 0) {
                        //报文状态码明确是失败的，帐户余额不减少，不用发冲正。
                        CarSerialPortUtil.ClearRollBackRecord();//删除未完整记录
                        String strPacketStatus = SerialDataUtils.Bytes2HexString(des_body, 8, 4);
                        showInfo = "充值失败！ AA64状态码:0x" + strPacketStatus;
                        showState = 9;
                        handler.sendEmptyMessage(5);
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        showInfo = "充值失败!\n请您到[飞充]\n继续完成卡片充值!\n";
                        showState = 9;
                        handler.sendEmptyMessage(5);
                    } else {
                        //m1卡操作 R_M1_LOAD  圈存写卡开始
                        String sstr;
                        sstr = SerialDataUtils.Bytes2HexString(des_body, 43, 4);//充值凭证号
                        CarSerialPortUtil.set_LOADNO(sstr);//保存充值凭证号
                        sstr = SerialDataUtils.Bytes2HexString(des_body, 36, 7);//充值时间
                        CarSerialPortUtil.setRechargeTime(sstr);//保存充值时间

                        Log.e("yct info", "正在充值中，请勿移动卡片...");
                        showInfo = "正在充值中，请勿移动卡片...";
                        //showState = 7;
                        showState = 9;
                        handler.sendEmptyMessage(5);
                        AA_64_ORDER = new byte[des_body.length];
                        AA_64_ORDER = des_body;


                        //测试点3，不发起0xBA充值，直接操作0x79
                        //int itest=0;  //不发起0xBA充值，直接操作0x79
                        int itest = 1;  //发起0xBA充值
                        if (itest == 0) {

                            CarSerialPortUtil.getInstance().R_M1_GET_CARDINFO(2);
                        } else {
                            CarSerialPortUtil.getInstance().R_M1_LOAD(des_body);//0xBA
                        }
                        /////////////////////////////////////
                        //CarSerialPortUtil.getInstance().R_M1_LOAD(des_body);
                    }
                }

            } else if (state.equals("66")) {
//                    if(des_body[8] ==(byte)0x00){
//                        //m1卡操作 R_M1_LOAD  圈存写卡开始
//                        showInfo = "冲正成功";
//                        showState = 0;
//                    }else {
//                        showInfo = "充值数据错误 0x66";
//                        showState = 0;
//                    }
//                    handler.sendEmptyMessage(5);
                if (null == des_body)//处理解包异常的情况
                {
                    Log.i("yct error info:", "AA66报文内容错。。。");//报文内容错
                    int rb_sta = 0;
                    rb_sta = CarSerialPortUtil.get_rollbackup_static();
                    if (rb_sta == 0) {
                        showInfo = "冲正失败！ AA66报文内容错";
                        showState = 9;
                        handler.sendEmptyMessage(5);
                    } else {
                        CarSerialPortUtil.set_rollbackup_static(0);
                    }
                } else {
                    int rb_sta = 0;
                    rb_sta = CarSerialPortUtil.get_rollbackup_static();
                    int iResult = SerialDataUtils.ByteMemCmp(des_body, 8, bPacketStatus, 0, 4);//报文状态码
                    if (iResult != 0) {
                        String strPacketStatus = SerialDataUtils.Bytes2HexString(des_body, 8, 4);
                        Log.e("yct info:", "AA66状态码:0x" + strPacketStatus);
                        CarSerialPortUtil.ClearRollBackRecord();//这里后台返回数据说明不需要冲正了，应删除充正记录

                        if (rb_sta == 0) {
                            showInfo = "充值失败!\n请您到[飞充]\n继续完成卡片充值!\n";
                            showState = 9;
                            handler.sendEmptyMessage(5);
                        } else
                            CarSerialPortUtil.set_rollbackup_static(0);

                    } else {
                        Log.i("yct error info:", "冲正成功,删除冲正记录。。。");
                        CarSerialPortUtil.ClearRollBackRecord();//冲正成功,删除冲正记录
                        if (rb_sta == 0) {
                            showInfo = "充值失败! 已冲正";
                            showState = 9;
                            handler.sendEmptyMessage(5);
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            showInfo = "充值失败!\n请您到[飞充]\n继续完成卡片充值!\n";
                            showState = 9;
                            handler.sendEmptyMessage(5);
                        } else {
                            CarSerialPortUtil.set_rollbackup_static(0);
                        }

                    }
                }


            } else if (state.equals("82")) {
                Log.e("yct info", "正在充值中，请勿移动卡片...");
                showInfo = "正在充值中，请勿移动卡片...";
                showState = 9;
                handler.sendEmptyMessage(5);
                //P_CPU_LOAD_QRY圈存查询
                int iResult = SerialDataUtils.ByteMemCmp(des_body, 8, bPacketStatus, 0, 4);//报文状态码
                if (iResult != 0) {
                    String strPacketStatus = SerialDataUtils.Bytes2HexString(des_body, 8, 4);
                    Log.e("yct error info", "圈存查询失败！ AA82状态码:0x" + strPacketStatus);
                    showInfo = "圈存查询失败！ AA82状态码:0x" + strPacketStatus;
                    showState = 9;
                    handler.sendEmptyMessage(5);

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    showInfo = "充值失败!\n请您到[飞充]\n继续完成卡片充值!\n";
                    showState = 9;
                    handler.sendEmptyMessage(5);
//                        showInfo = "充值数据错误 0x82";
//                        showState = 0;
//                        handler.sendEmptyMessage(5);
                } else {
                    //CPU操作 ，R_CPU_LOAD_INIT  先进行圈存初始化，获取充值需要的数据，再进行充值操作
                    AA_82_ORDERNUM = new byte[des_body.length];
                    AA_82_ORDERNUM = des_body;
                    System.arraycopy(des_body, 0, g_SYSTEMSEQ_LOAD, 0, g_SYSTEMSEQ_LOAD.length);//AA82系统流水号
                    CarSerialPortUtil.set_SYSTEMSEQ_LOAD(SerialDataUtils.Bytes2HexString(g_SYSTEMSEQ_LOAD, 0, g_SYSTEMSEQ_LOAD.length));//保存系统流水号
                    Log.e("AA82 系统流水号:", SerialDataUtils.Bytes2HexString(g_SYSTEMSEQ_LOAD, 0, g_SYSTEMSEQ_LOAD.length));
                    CarSerialPortUtil.getInstance().R_CPU_LOAD_INIT(des_body);//0x7A
                }

            } else if (state.equals("84")) {
                Log.e("yct info", "正在充值中，请勿移动卡片...");
                showInfo = "正在充值中，请勿移动卡片...";
                showState = 9;
                handler.sendEmptyMessage(5);
                if (null == des_body)//处理解包异常的情况
                {
                    //接收错误，将本次圈存状态提交给服务器
                    //返回失败，提示充值失败
                    //将本次圈存状态提交给服务器
                    Log.i("yct error info:", "2.解包失败,将本次圈存状态提交给服务器，圈存状态01..");
                    String strRecord = CarSerialPortUtil.ReadCpuSubmitData();
                    if (null != strRecord) {
                        byte[] strbuff = new byte[200];
                        strbuff = SerialDataUtils.HexString2Bytes(strRecord);
                        byte[] ucTempData = new byte[]{(byte) 0xAA, (byte) 0x85, 0x00, 0x01, 0x02, (byte) 0x80, 0x00, (byte) 0x78};
                        if (SerialDataUtils.ByteMemCmp(strbuff, 0, ucTempData, 0, bPacketHead.length) != 0) {
                            Log.i("yct error info:", "圈存数据异常，无法做圈存状态提交!");
                        } else {
                            RollBackStatus = 0x01;//圈存状态
                            getSendSocketInfo(strbuff, 302, "0", "0");
                        }

                    } else {
                        Log.i("yct error info:", "圈存数据异常，无法做圈存状态提交!");
                        Log.i("yct error info:", "系统异常!");
                        Log.i("yct error info:", "系统需要维护!");
                        showInfo = "圈存数据异常，无法做圈存状态提交!";
                        showState = 9;
                        handler.sendEmptyMessage(5);
                    }
                } else {//解包正常
                    //P_CPU_LOAD圈存
                    int iResult = SerialDataUtils.ByteMemCmp(des_body, 8, bPacketStatus, 0, 4);//报文状态码
                    if (iResult != 0) {
                        //终端收到充值服务器返回的错误报文，直接报错提示充值失败, 并且删除不完整交易记录,无需后面的流程。
                        CarSerialPortUtil.ClearCpuSubmitData();
                        String strPacketStatus = SerialDataUtils.Bytes2HexString(des_body, 8, 4);
                        Log.i("yct error info", "圈存失败！ AA84状态码:0x" + strPacketStatus);
                        showInfo = "圈存失败！ AA84状态码:0x" + strPacketStatus;
                        showState = 9;
                        handler.sendEmptyMessage(5);
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        showInfo = "充值失败!\n请您到[飞充]\n继续完成卡片充值!\n";
                        showState = 9;
                        handler.sendEmptyMessage(5);
//                        showInfo = "充值数据错误 0x84";
//                        showState = 0;
//                        handler.sendEmptyMessage(5);
                    } else {
                        //CPU操作 ，P_CPU_LOAD 返回成功，开始下一步 R_CPU_LOAD  ,往读卡器写入数据
                        AA_84_INFO = new byte[des_body.length];
                        AA_84_INFO = des_body;
                        byte[] tmp = new byte[4];
                        String str = SerialDataUtils.Bytes2HexString(des_body, 55, 4);//g_LOADNO
                        // Log.e("yct info:","设置的凭证号是什么:"+str);
                        CarSerialPortUtil.set_LOADNO(str);
                        String sstr;
                        sstr = SerialDataUtils.Bytes2HexString(des_body, 44, 7);//充值时间
                        CarSerialPortUtil.setRechargeTime(sstr);//保存充值时间
                        //String strprin = SerialDataUtils.Bytes2HexString(des_body, 0, des_body.length);
                        // Log.e("yct info:","xxxxdesbody:"+strprin);
                        //测试点4============================================================
                        //int itest =0;  //不发7B指令，不写卡，直接执行79指令
                        int itest = 1;    //发7B写卡指令
                        if (itest == 0) {
                            CarSerialPortUtil.getInstance().CPU_GET_CARDINFO(1);//0x79
                        } else {
                            CarSerialPortUtil.getInstance().R_CPU_LOAD(des_body);//0x7B
                        }
                        //===================================================================

                        //CarSerialPortUtil.getInstance().R_CPU_LOAD(des_body);//0x7B
                    }
                }


            } else if (state.equals("86")) {
                int iResult = SerialDataUtils.ByteMemCmp(des_body, 8, bPacketStatus, 0, 4);//报文状态码
                if (iResult != 0) {
                    //状态码明确是失败，不用再提交
                    String strPacketStatus = SerialDataUtils.Bytes2HexString(des_body, 8, 4);
                    Log.i("yct error info", "圈存提交失败！ AA86状态码:0x" + strPacketStatus);
                }
                CarSerialPortUtil.ClearCpuSubmitData();
//                    if(des_body[8] ==(byte)0x00){
//                        //CPU操作 ，P_CPU_LOAD 返回成功，开始下一步 R_CPU_LOAD  ,往读卡器写入数据
//                        showInfo = "充值成功";
//                        showState = 8;
//                    }else {
//                        showInfo = "充值失败 0x86";
//                        showState = 0;
//                    }
//                    handler.sendEmptyMessage(5);
            } else if (state.equals("32")) {
                Log.e("yct info", "正在查询中，请勿移动卡片...");
                int iResult = SerialDataUtils.ByteMemCmp(des_body, 12, bPacketStatus, 0, 4);
                Log.e("yct info", "iResult==========" + iResult);
                if (iResult != 0) {
                    //   AA01E201:充值金账户余额为零。
                    String strPacketStatus = SerialDataUtils.Bytes2HexString(des_body, 12, 4);
                    Log.e("yct info", "strPacketStatus==========" + strPacketStatus);
                    if (strPacketStatus.equals("AA01E201")) {
                        showInfo = "充值金账户余额不足！";
                    } else
                        showInfo = "余额查询失败！ AB32状态码:0x" + strPacketStatus;
                    showState = 9;
                    handler.sendEmptyMessage(5);
                } else {
                    int intmoney = SerialDataUtils.toBdInt(des_body, 16, 4);
                    vMoney = intmoney;
                    handler.sendEmptyMessage(6);
                    Log.e("TAG", "余额数==================" + intmoney);
                }
            }

            //}

        } catch (Exception e) {
        }
        byte[] des_test = new byte[des_body.length - 8];
        for (int i = 0; i < (des_body.length - 8); i++) {
            des_test[i] = des_body[i + 4];
//            Log.e("22222", des_test[i] + "");
        }
        try {
//            Log.e("CZ_des_3", URLEncoder.encode(des_test, "UTF-8") + "");
//            Log.e("CZ_des_3", state +" : " +bytesToHexString(des_test));

        } catch (Exception e) {
        }


    }

    private static byte[] getOrderCK_3DES(byte[] info) throws Exception {
        byte[] pack = new byte[16];
        pack[0] = (byte) 0x11;
        pack[1] = (byte) 0x22;
        pack[2] = (byte) 0x33;
        pack[3] = (byte) 0x44;
        pack[4] = (byte) 0x55;
        pack[5] = (byte) 0x66;
        pack[6] = (byte) 0x77;
        pack[7] = (byte) 0x88;
        pack[8] = (byte) 0x88;
        pack[9] = (byte) 0x77;
        pack[10] = (byte) 0x66;
        pack[11] = (byte) 0x55;
        pack[12] = (byte) 0x44;
        pack[13] = (byte) 0x33;
        pack[14] = (byte) 0x22;
        pack[15] = (byte) 0x11;

        //13 71 E7 96 A4 AD 1D 5F
        Log.e("3des_des_new1_秘钥", bytesToHexString(pack));
        Log.e("3des_des_new1_加密", bytesToHexString(SecretUtils.encryptMode(info, pack)));
        Log.e("3des_des_new2_解密", bytesToHexString(SecretUtils.decrypt3DES_byte(SecretUtils.encryptMode(info, pack), pack)));

        return SecretUtils.encrypt3DES_byte(info, pack);
    }

    /**
     * 终端监控
     * P_MONITOR
     */
    public static byte[] P_MONITOR(String sign_info, byte[] cmd) {
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
        String[] manger = sign_info.split(" ");
        //AES 秘钥
        CZ_SK_16 = new byte[16];
        for (int i = 0; i < 16; i++) {
            int num_info = Integer.valueOf(manger[i + 12], 16);
            CZ_SK_16[i] = (byte) num_info;
        }
        Log.e("FD_01_SK_16", bytesToHexString(CZ_SK_16));
        //Constant.Companion.getPKI_ID()
        Log.e("FD_01_SK_161", Constant.Companion.getPKI_ID());
        byte[] pack = new byte[114];// len = 114-8
        pack[0] = (byte) 0xFD;
        pack[1] = (byte) 0x01;                                           //包头
        pack[2] = (byte) 0x00;
        pack[3] = (byte) 0x02;                                          //len 2字节
        pack[4] = (byte) 0x00;                                          //加密算法 1字节
        pack[5] = (byte) 0x80;                                          //排序方式 1字节
        pack[6] = (byte) 0x00;                                          //报文长度

        pack[7] = (byte) 0x6A;                                          //报文长度 2字节
        //握手流水号 8位  15
        byte[] order8 = new byte[8];
        int index = 8;
        for (int i = 0; i < 8; i++) {
            int num_info = Integer.valueOf(manger[i + 4], 16);
            pack[index + i] = (byte) num_info;
            order8[i] = (byte) num_info;
        }
        //管理卡版本号
        pack[16] = (byte) 0x01;
        //PKI管理卡号 4位  23

        String[] manger_shid = Constant.Companion.getPKI_ID().trim().split(" ");
        for (int i = 0; i < 4; i++) {
            int num_info_1 = Integer.valueOf(manger_shid[i], 16);
            pack[17 + i] = (byte) num_info_1;
        }
        //时间戳 24小时 7位
//终端当前日期
        pack[21] = (byte) intTointByte(20);                                    //终端时间        YY
        pack[22] = (byte) intTointByte(Integer.parseInt((year + "").substring(2, 4)));                                    //终端时间        YY
        pack[23] = (byte) intTointByte(month);                                     //终端时间        MM
        pack[24] = (byte) intTointByte(day);                                    //终端时间        DD
        pack[25] = (byte) intTointByte(hour);                                    //终端时间        HH
        pack[26] = (byte) intTointByte(minute);                                    //终端时间        MI
        pack[27] = (byte) intTointByte(second);                                     //终端时间        SS
        //最后一笔交易状态 1位  00 成功  01 失败  02 读卡器未响应  03 服务器拒绝
        pack[28] = (byte) 0x00;
        //最后一笔交易流水号  8位
        for (int i = 0; i < 8; i++) {
            pack[29 + i] = (byte) 0x00;
        }
//        读卡器版本   R_PUB_GETVERSION 返回码
        for (int i = 0; i < 60; i++) {
            pack[37 + i] = cmd[i + 4];
        }
        //终端软件版本号
        String[] manger1 = StringUtil.stringToAscii("WD-1.00-20200720").split(" ");
        //AES 秘钥
        for (int i = 0; i < 16; i++) {
//            pack[i+97] = (byte)intTointByte(Integer.parseInt(manger1[i]));
            pack[i + 97] = (byte) 0x00;
        }
        //监控状态  R_PUB_GET_PKISTATE 读卡器获取状态  00 PKI正常  01 PKI异常
        pack[113] = (byte) 0x00;
        Log.e("FD_01_start", bytesToHexString(pack));
        try {
            order8 = getOrderCK_3DES(order8);
            for (int i = 0; i < 8; i++) {
                pack[index + i] = (byte) order8[i];
            }
        } catch (Exception e) {
            Log.e("FD_01_order_e", bytesToHexString(pack));
        }
        //AES 报文体加密  除握手流水号之外  114-16
        byte[] sign_aes_all = new byte[98];
        for (int i = 0; i < 98; i++) {
            sign_aes_all[i] = pack[16 + i];
        }

        try {
            byte[] sign_end = encrypt128_4(sign_aes_all, CZ_SK_16);
            for (int i = 0; i < 98; i++) {
                pack[16 + i] = sign_end[i];

            }
            Log.e("FD_01_aes_length", sign_end.length + "");
        } catch (Exception e) {
            Log.e("FD_01_sign_e", e.getMessage().toString());
        }
        StringBuilder sb = new StringBuilder();//非线程安全
        for (int i = 0; i < pack.length; i++) {
            sb.append(SerialDataUtils.Byte2Hex((pack[i]))).append(" ");
        }
        //E1 30 07 13 00 00 00 00 51 00 00 04 01 13 71 53
        Log.e("FD_01_enddd", bytesToHexString(pack));
        return pack;
    }

    public void yct_showinfo(String strinfo, int status) {
        showInfo = strinfo;
        showState = status;
        handler.sendEmptyMessage(5);
    }
}

