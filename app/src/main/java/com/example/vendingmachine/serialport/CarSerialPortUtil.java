package com.example.vendingmachine.serialport;

/**
 * HE SUN 2018/1/16.
 */

import android.util.Log;
import android.widget.Toast;

import com.airiche.sunchip.control.ViewEventNotifier;
import com.airiche.sunchip.control.ViewEventObject;
import com.example.vendingmachine.App;
import com.example.vendingmachine.platform.common.AddSocketClient;
import com.example.vendingmachine.platform.common.Constant;
import com.example.vendingmachine.utils.TextLog;
import com.example.vendingmachine.utils.encryption.Des;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import static com.example.vendingmachine.platform.common.SocketClient2.bytesToHexString;

public class CarSerialPortUtil {
    private static final String TAG = "CArSerialPortUtil";
    private SerialPort mSerialPort = null;
    private OutputStream mOutputStream = null;
    private InputStream mInputStream = null;
    private ReadThread mReadThread = null;
    //        private String path = "/dev/ttyS0";       //这个是我们要读取的串口路径，这个硬件开发人员会告诉我们的
    //    private int baudrate = 9600;              //这个参数，硬件开发人员也会告诉我们的
    private String path = "/dev/ttyS1";       //测试   这个是我们要读取的串口路径，这个硬件开发人员会告诉我们的
    private int baudrate = 57600;              // 测试  这个参数，硬件开发人员也会告诉我们的
    private static CarSerialPortUtil portUtil = null;
    private boolean isStop = false;
    public static boolean isPutOuting = false;//正在出货

    private TransformFile transformFile = null;
    public static int boardVersion = 0;

    //2020.8.25 add by wen
    private byte[] buff = new byte[1024];
    private byte[] buff_data = new byte[64];
    private String str_g_PKI_NO;  //PKI卡号
    private String str_g_FNO;     //物理卡号
    private String str_g_LNO;     //逻辑卡号
    private static String str_g_SYSTEMSEQ_LOAD;  //系统流水号
    private static String str_g_LOADNO;          //充值凭证号
    private static String str_PacketHead;
    private static String str_recharge_time;//充值时间

    private int iResult = 0;
    private byte[] g_PKI_NO = new byte[4];
    private byte[] g_FNO = new byte[8];
    private byte[] g_LNO = new byte[8];
    private byte[] g_SYSTEMSEQ_LOAD = new byte[8];
    private static byte[] g_LOADNO = new byte[4];
    private byte[] g_7B_TACSW = new byte[6];//交易认证码4+SW1SW2 2
    private byte[] g_BalanceLimit = new byte[8];//钱包余额4+金额下限1+金额上限3
    private byte[] g_CountInfo = new byte[16];//计数信息16

    private int g_TrnasMoney = 0;
    private int g_Consume_Balance = 0;
    private int g_DLimit = 0;


    private static int g_FARE = 0; //充值金额
    private String strResult;
    private int g_BALANCE_FRONT = 0;//充值前卡余额
    private int g_BALANCE_LAST = 0;//充值前后卡余额
    private static int com_recv_len = 0;//串口接收到的数据长度
    private static byte com_send_cmd = 0; //串口发送的命令
    private static byte g_LOADWRITECARDCODE = 0;//读卡器状态码

    private byte bWalletType = 0;//钱包类别为“0x01使用M1钱包”  钱包类别为“0x02使用CPU钱包”
    private int ulBalance = 0;//本次余额
    private int ulTransMoney = 0;//交易金额
    private static int rollbackup = 0;
    private int bComsume = 0;

    public static void setRechargeTime(String str) {
        str_recharge_time = str;
    }

    public String get_SYSTEMSEQ_LOAD() {
        return str_g_SYSTEMSEQ_LOAD;
    }

    public String get_PKI_NO() {
        return str_g_PKI_NO;
    }

    public String get_FNO() {
        return str_g_FNO;
    }

    public String get_LNO() {
        return str_g_LNO;
    }

    public String get_LOADNO() {
        return str_g_LOADNO;
    }

    public static void set_SYSTEMSEQ_LOAD(String str) {
        str_g_SYSTEMSEQ_LOAD = str;
    }

    public static void set_LOADNO(String str) {
        byte[] temp = new byte[4];
        str_g_LOADNO = str;
        temp = SerialDataUtils.HexString2Bytes(str_g_LOADNO);
        System.arraycopy(temp, 0, g_LOADNO, 0, 4);
    }

    public static void set_fare(int money) {
        g_FARE = money;
    }

    public static String get_PacketHead() {
        return str_PacketHead;
    }

    public static void set_rollbackup_static(int sta) {
        rollbackup = sta;
    }

    public static int get_rollbackup_static() {
        return rollbackup;
    }

    /**
     * 回调接口
     */
    public interface OnDataReceiveListener {
        // void onDataReceive(int cmd, int data1, int data2, int data3);//数据正确
//        void onDataReceiveCar(int cmd, int data);//数据正确
        void onDataReceiveCar(int cmd, String data);//数据正确

        void onDataReceiveCar(int state, byte[] cmd);//数据错误

        //void onDataError(List<Integer> data);//数据错误
        void onDataToast(String str);        //提示toast
    }

    /**
     * 回调方法
     */
    private OnDataReceiveListener onDataReceiveListener = null;

    public void setOnDataReceiveListener(OnDataReceiveListener dataReceiveListener) {
        onDataReceiveListener = dataReceiveListener;
    }

    /**
     * 单例
     */
    public static CarSerialPortUtil getInstance() {
        if (null == portUtil) {
            portUtil = new CarSerialPortUtil();
            portUtil.onCreate();
        }
        return portUtil;
    }

    /**
     * 初始化串口信息
     */
    public void onCreate() {
        try {
            mSerialPort = new SerialPort(new File(path), baudrate, 0);
            mOutputStream = mSerialPort.getOutputStream();
            mInputStream = mSerialPort.getInputStream();
            mReadThread = new ReadThread();
            isStop = false;
            mReadThread.start();
            transformFile = new TransformFile();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static final int DATA_PACK_START = -67;  // 包头 0xBD
    private static final int DATA_PACK_END = -70;  // 包尾 0xBA

    //宏祥打包
    private byte[] cmd_package(int cmd, int data) {
        byte[] pack = new byte[7];
        pack[0] = (byte) 0xAB;                                   //包头AB
        pack[1] = 0x01;                                          //包标识
        pack[2] = 0x02;                                          //数据长度1
        pack[3] = (byte) cmd;                                     //指令
        pack[4] = (byte) data;                                    //数据
        pack[5] = (byte) (pack[1] + pack[2] + pack[3] + pack[4]);//校验和
        pack[6] = (byte) 0xBA;                                   //包尾BA
        return pack;
    }

    /**
     * 发送指令到串口
     *
     * @param cmd
     * @return
     */
    public boolean sendCmds(byte[] cmd) {
        TextLog.writeTxtToFile("发送串口数据:" + Arrays.toString(cmd), Constant.Companion.getFilePath(), Constant.fileName);
        boolean result = true;
        if (!isStop) {
            try {
                if (mOutputStream != null) {
                    mOutputStream.write(cmd, 0, cmd.length);
                    mOutputStream.flush();
                    StringBuilder sb = new StringBuilder();//非线程安全
                    for (int i = 0; i < cmd.length; i++) {
                        sb.append(SerialDataUtils.Byte2Hex((cmd[i]))).append(" ");
                    }
                    Log.e("TAG", "sendCmds_car： " + sb);
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    result = false;
                }
            } catch (IOException e) {
                e.printStackTrace();
                result = false;
                Log.i("req112", "sendCmds： " + e);
            }
        } else {
            System.out.println("sendCmds serialPort isClose");
            result = false;
        }
        return result;
    }

    /**
     * 接收数据的线程
     */
    private List<Byte> read_buffer = new ArrayList<>();
    private long lastReadTime = 0;

    private class ReadThread extends Thread {
        @Override
        public void run() {
            super.run();
            Log.i("req", "ReadThread start");//&& !isInterrupted()
            while (true) {
                Log.i("req", "ReadThread " + isInterrupted());
                int size;
                try {
                    if (mInputStream == null) {
                        //读不到串口数据
                        Log.i(TAG, "ReadThread.run return");
                        return;
                    }
                    byte[] temp = new byte[512];
                    size = mInputStream.read(temp);//该方法读不到数据时，会阻塞在这里
                    if (size > 0) {
                        com_recv_len = size;
                        long now = System.currentTimeMillis();
                        if (now - lastReadTime > 500 || read_buffer.size() > 512) {
                            read_buffer.clear();
                        }
                        lastReadTime = now;

                        for (int i = 0; i < size; i++) {
                            read_buffer.add(temp[i]);
                        }
                        String ss = "";
                        car_manger_info = "";
                        StringBuffer sb = new StringBuffer();
                        for (int i = 0; i < read_buffer.size(); i++) {
                            sb.append(SerialDataUtils.Byte2Hex(read_buffer.get(i))).append(" - ");
                        }
                        car_manger_info = sb.toString();
                        Log.i("sendC_req1123_car", "read data: " + read_buffer);
                        read_buffer = data_prser2(read_buffer);
                    } else {
                        com_recv_len = 0;
                        //yct_com_recvnull_pro();//发送羊城通读卡器命令没返回数据处理
                        Thread.sleep(50);//延时 50 毫秒
                    }
                } catch (Exception e) {
                    Log.i("sendC_req1123_e", "ReadThread.run  e.printStackTrace() : " + e);
                    e.printStackTrace();
                }
            }
        }
    }

    public List<Byte> data_prser2(List<Byte> pack) {
        final List<Integer> data = new ArrayList<>();// 提取后的数据
        data.clear();
        com_recv_len = pack.size();//串口接收到的数据长度
        while (pack.size() != 0) {

            int data_start = -1;
            data_processing_my(pack, pack.size());
            for (int i = 0; i < pack.size(); i++) {
//                Log.e("DATA_PACK_START", pack.get(i) + "");
                if (pack.get(i) == DATA_PACK_START) {//包头ab
                    data_start = i;
//                    Log.e("DATA_PACK_START222", data_start + "");
//                    data_processing_my(pack, pack.size());
                    break;
                }
            }
        }
        return pack;
    }

    //发送羊城通读卡器命令没返回数据处理
//    public void yct_com_recvnull_pro() {
//        if (com_send_cmd == (byte) 0xB9) {
//            if (M1_CARDINFO_TYPE == 2)//M1卡取卡信息第二次
//            {
//                if (g_LOADWRITECARDCODE == 0)//0xBA的状态码
//                {
//                    //疑是成功
//                    ClearRollBackRecord();
//                    Log.e("yct error info", "充值疑是成功!");
//                    onDataReceiveListener.onDataReceiveCar(1121, "充值疑是成功!");
//                } else {
//                    Log.e("yct error info", "充值失败!");
//                    //onDataReceiveListener.onDataReceiveCar(1121, "充值失败!");
//                    ShowRechargeFail();
//                }
//            }
//        } else if (com_send_cmd == (byte) 0xB6) {
//
//        }
//        com_send_cmd = 0;
//    }

    /**
     * 发送控制板程序
     */
    public void send_file() {
        transformFile.send_file_start();
    }

    //测试羊城通
    public void pay_check_sign_3(byte[] temp) {
        //des 解密密钥
        byte[] key_1 = new byte[8];
        key_1[0] = (byte) 0x31;
        key_1[1] = (byte) 0x32;
        key_1[2] = (byte) 0x33;
        key_1[3] = (byte) 0x34;
        key_1[4] = (byte) 0x35;
        key_1[5] = (byte) 0x36;
        key_1[6] = (byte) 0x37;
        key_1[7] = (byte) 0x38;
        byte[] pack = new byte[132];
        pack[0] = (byte) 0xBA;                                   //包头BA 1字节
        pack[1] = (byte) 0x82;                                          //len 1字节
        pack[2] = (byte) 0x7D;                                          //command 1字节
        byte[] des_body = new byte[temp.length - 16];
        for (int i = 0; i < (temp.length - 16); i++) {
            des_body[i] = temp[i + 16];
        }
        Log.e("sign_3_", des_body.length + "");
        Log.e("sign_3_0", bytesToHexString(des_body));
        try {
            Log.e("sign_3_1", bytesToHexString(Des.deCrypto_2(des_body, key_1)));
            for (int i = 0; i < 128; i++) {
                pack[i + 3] = Des.deCrypto_2(des_body, key_1)[i + 6];
            }
            Log.e("sign_3_2", bytesToHexString(pack));
            byte sum = 0;
            for (int i = 0; i < 131; i++) {//校验和
                sum ^= pack[i];
            }
            pack[131] = (byte) sum;
            Log.e("sign_3_3", bytesToHexString(pack));
        } catch (Exception e) {
        }
        //checksun 从header到data（包含头尾）所有字节的XOR 1字节 ^ pack[18] ^ pack[19]
        sendCmds(pack);
    }

    private int login_title = 1;

    //认证1发送数据 k19 密钥标识
    public void pay_check_login_1(byte[] temp, int state) {
        login_title = state;
        byte[] pack = new byte[21];
        pack[0] = (byte) 0xBA;                                   //包头BA 1字节
        pack[1] = (byte) 0x13;                                          //len 1字节
        pack[2] = (byte) 0x7E;                                          //command 1字节
        if (state == 1) {
            //签到1发送数据
            pack[3] = (byte) (temp[28] + (byte) 0x19);                        //28位，k19 版本号.29位k65
            login_title = 1;
        } else {
            pack[3] = (byte) (temp[29] + (byte) 0x42);                        //28位，k19 版本号.29位k65
            login_title = 2;
        }

        byte[] des_body = new byte[16];
        for (int i = 0; i < 16; i++) {
            pack[4 + i] = temp[i + 30];
            des_body[i] = temp[i + 30];
        }
        byte sum = 0;
        for (int i = 0; i < 21; i++) {//校验和
            sum ^= pack[i];
        }
        pack[20] = (byte) sum;
        Log.e("login_1_", bytesToHexString(temp));
        Log.e("login_1_0", bytesToHexString(pack));
        Log.e("login_1_1", bytesToHexString(des_body));
        //checksun 从header到data（包含头尾）所有字节的XOR 1字节
        sendCmds(pack);
    }

    //认证1发送数据 k65 密钥标识
    public void pay_check_login_2(byte[] temp, int state) {
        login_title = state;
        byte[] pack = new byte[21];
        pack[0] = (byte) 0xBA;                                   //包头BA 1字节
        pack[1] = (byte) 0x13;                                          //len 1字节
        pack[2] = (byte) 0x7E;                                          //command 1字节
        if (state == 1) {
            //签到1发送数据
            pack[3] = (byte) (temp[28] + (byte) 0x19);                        //28位，k19 版本号.29位k65
            login_title = 1;
        } else {
            pack[3] = (byte) (temp[29] + (byte) 0x42);                        //28位，k19 版本号.29位k65
            login_title = 2;
        }

        byte[] des_body = new byte[16];
        for (int i = 0; i < 16; i++) {
            pack[4 + i] = temp[i + 30];
            des_body[i] = temp[i + 30];
        }
        byte sum = 0;
        for (int i = 0; i < 21; i++) {//校验和
            sum ^= pack[i];
        }
        pack[20] = (byte) sum;
        Log.e("login_1_", bytesToHexString(temp));
        Log.e("login_1_0", bytesToHexString(pack));
        Log.e("login_1_1", bytesToHexString(des_body));
        //checksun 从header到data（包含头尾）所有字节的XOR 1字节
        sendCmds(pack);
    }

    /*
    读卡器消费参数设置
     BA 03 B3 80 8A
     */
    public void pay_card_set() {
        byte[] pack = new byte[5];
        pack[0] = (byte) 0xBA;                                   //包头BA 1字节
        pack[1] = (byte) 0x03;                                          //len 1字节
        pack[2] = (byte) 0xB3;                                          //command 1字节
        pack[3] = (byte) 0x80;                                   //默认 80 正常消费  ，C0禁止CPU卡模拟M1卡使用 E0 禁止M卡使用
        pack[4] = (byte) (pack[0] ^ pack[1] ^ pack[2] ^ pack[3]);
        //checksun 从header到data（包含头尾）所有字节的XOR 1字节 ^ pack[18] ^ pack[19]
        sendCmds(pack);
    }

    /*
    读卡器消费  寻卡
     BA 03 B3 80 8A
     */
    //card_serch  0 查询余额进行消费   1 查询余额功能，不做其他操作
    private static int card_serch = 0;

    public void pay_card_serch(int state) {
        card_serch = state;
        byte[] pack = new byte[4];
        pack[0] = (byte) 0xBA;                                   //包头BA 1字节
        pack[1] = (byte) 0x02;                                          //len 1字节
        pack[2] = (byte) 0xB4;                                          //command 1字节
        pack[3] = (byte) (pack[0] ^ pack[1] ^ pack[2]);
        //checksun 从header到data（包含头尾）所有字节的XOR 1字节 ^ pack[18] ^ pack[19]
        sendCmds(pack);
    }

    /**
     * 关闭串口
     */
    public void closeSerialPort() {
        isStop = true;
        if (mReadThread != null) {
            mReadThread.interrupt();
            try {
                mReadThread.wait(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (mSerialPort != null) {
            mSerialPort.close();
            portUtil = null;
        }
    }

    private static int CARD_SEARCH_LEN = 2;
    private static int CARD_CHECK_LEN = 11;
    private static int CARD_READ_LEN = 12;
    String car_manger_info = "";
    String car_manger_info_1 = "";
    private static int CAR_STATUS = 3;//状态码 0 成功， 其他异常
    private static int CAR_COMMAND_LEN = 1;//指令长度
    private static int CAR_COMMAND = 2;//命令符

    //充值相关
    //监控相关
    private static int CAR_GET_VERSION = 96;  //获取程序版本

    private static int CAR_INIT = 16;//初始化读卡器 0x10
    private static int CAR_SIGN_1 = -56;//签到1 0xC8
    private static int CAR_SIGN_2 = -55;//签到2 0xC9
    private static int CAR_SEARCH = 69;//寻卡 0x45
    private static int CAR_GET_STATE = 101;//获取当前状态
    private static int CAR_GET_NUM = -109;//读卡  94
    private static int CAR_GET_CARD_INFO_94 = -108;//查询卡应用信息
    //读取M1 卡信息
    private static int CAR_GET_M1_INFO = -71;//0xB9
    //R_M1_LOAD  圈存写卡开始 充值1
    private static int CAR_R_M1_LOAD = -70;//0xBA
    //设置CPU卡信息
    private static int CAR_SET_CPU_INFO = -107;//0x95 R_CPU_GET_CARDINFO
    //读取CPU卡信息
    private static int CAR_GET_CPU_INFO = 121;//0x79
    //获取CPU_INIT后数据，用于AA83  充值后续操作
    private static int CAR_GET_CPU_INIT = 122;//0x7A
    //写卡操作，充值写卡结束，开始上传结果
    private static int CAR_GET_CPU_LOAD = 123;//0x7B

    private byte[] PKI_ID = null; //PKI管理卡号
    private byte[] CZ_CARD_NUM = null; //物理卡号，逻辑卡号
    private byte[] CZ_SIGN_2_INFO = null; //握手流水号 ，SK (AES，密钥)
    private List<String> CZ_LIST_INFO = null;

    //消费相关   签到认证
    private static int PAY_SIGN_1 = 124;//消费签到1
    private static int PAY_SIGN_2 = 125;//消费签到2
    private static int PAY_LOGIN_1 = 126;//认证1
    private List<String> manger_num = null;
    private List<String> manger_num_card = null;
    private List<String> car_defult_info = null;
    private byte[] login_19_65 = null; //存储验证信息分散密钥，加密要用到
    private byte[] mRestart = null;
    private byte[] login_19 = null; //k19密钥
    private byte[] login_65 = null; //k65密钥
    //消费相关   签到认证
    private static int PAY_CARD_SEARCH = -76;//消费签到1
    private static int PAY_CARD_BUY_B5 = -75;//消费预处理
    private static int PAY_CARD_BUY_B6 = -74;//消费确认处理
    private static int PAY_CARD_BUY_B7 = -73;//B7
    private static int PAY_CARD_BUY_B8 = -72;//B8

    private List<String> pay_order_upload = null;//交易记录数据存储，用于B1上传

    private static int state_read = 0;
    private byte[] ucTemp = new byte[512];

    private void data_processing_my(final List<Byte> data, final int len) {
        Log.i("sendC_xxxx21", data.toString() + "  状态码 " + data.get(CAR_COMMAND) + "  ");
        Log.i("sendC_xxxx22", car_manger_info.replace(" - ", " "));
        String[] manger = car_manger_info.split(" - ");
        Log.i("11——car_info", manger.length + "");

        //manger : BD LEN CMD STA DATA.....XOR
        //初始化
        if (data.get(CAR_COMMAND) == CAR_INIT) {//cmd:0x10
            if (onDataReceiveListener != null) {
                if (data.get(CAR_STATUS) == 0) {
                    String shid_info = "";
                    manger_num = new ArrayList<>();
                    for (int i = 0; i < 4; i++) {
                        manger_num.add(manger[i + 4]);
                        shid_info += (" " + manger[i + 4]);
                    }
                    PKI_ID = check_CARD(manger_num, 4);
                    Constant.Companion.setPKI_ID(bytesToHexString(PKI_ID));

                    onDataReceiveListener.onDataReceiveCar(101, shid_info);
                    Log.e("car_manger_info_122", manger_num.size() + "  ");
//                    App.Companion.getSpUtil().putString(Constant.Companion.getCAR_LOGIN_INFO(), shid_info);
                    Log.e("car_manger_info_1223", shid_info);

//                    test_sign_in_1();
                } else {
                    onDataReceiveListener.onDataReceiveCar(1011, "0");
                }
            }
        } else if (data.get(CAR_COMMAND) == CAR_SIGN_1) {
            //签到1指令
            if (onDataReceiveListener != null) {
                if (data.get(CAR_STATUS).toString().equals("0")) {

                    manger_num = new ArrayList<>();
                    Log.e("manger", manger.length + "");
                    for (int i = 0; i < manger.length - 5; i++) {
                        manger_num.add(manger[i + 4]);
                        car_manger_info_1 += (" " + manger[i + 4]);
                    }
                    Log.e("sendC_sign1", bytesToHexString(check_DE1(manger_num)) + "");
                    onDataReceiveListener.onDataReceiveCar(102, car_manger_info_1);
                    onDataReceiveListener.onDataReceiveCar(103, check_DE1(manger_num));
//                    test_sign_in_2(manger_num);
                } else {
                    onDataReceiveListener.onDataReceiveCar(1031, "0");
                }
            }

        } else if (data.get(CAR_COMMAND) == CAR_SIGN_2) {
            //充值  签到2指令
            Log.e("socket_1042_info", "CAR_SIGN_2=" + CAR_SIGN_2);
            if (onDataReceiveListener != null) {
                Log.e("socket_1042_info", "onDataReceiveListener=" + onDataReceiveListener);
                if (data.get(CAR_STATUS).toString().equals("0")) {
                    Log.e("socket_1042_info", "data.get(CAR_STATUS)=" + data.get(CAR_STATUS));
                    manger_num = new ArrayList<>();
                    CZ_LIST_INFO = new ArrayList<>();
                    for (int i = 0; i < manger.length - 51; i++) {
                        manger_num.add(manger[i + 50]);
                        car_manger_info_1 += (" " + manger[i + 50]);
                    }
                    Log.e("socket_1042_info", "存储签到2步骤返回的握手流水号=" + manger.length);
                    //用于存储签到2步骤返回的握手流水号，及SK的前16位会话密钥（AES密钥）
                    for (int i = 0; i < 24; i++) {
                        CZ_LIST_INFO.add(manger[i + 4]);
                    }
                    CZ_SIGN_2_INFO = check_CARD(CZ_LIST_INFO, CZ_LIST_INFO.size());
                    Constant.Companion.setCZ_SIGN_2_INFO(bytesToHexString(CZ_SIGN_2_INFO));
                    Log.e("sign_2", car_manger_info.replace(" - ", " ") + "");
                    car_defult_info = new ArrayList<>();
                    onDataReceiveListener.onDataReceiveCar(1042, car_manger_info.replace(" - ", " "));
                    onDataReceiveListener.onDataReceiveCar(104, check_DE1(manger_num));
                } else {
                    onDataReceiveListener.onDataReceiveCar(1041, "0");
                }
            }
            read_buffer.clear();
            car_manger_info = "";
            car_manger_info_1 = "";
        } else if (data.get(CAR_COMMAND) == CAR_GET_STATE) {
            //查询当前状态
            if (onDataReceiveListener != null) {
                Log.e("sendC_xxxx6", (data.get(CAR_STATUS) == 0) + "");
                if (data.get(CAR_STATUS) == 0) {
                    Log.e("sendC_xxxx6", "123" + TYPE_STATE);
                    if (TYPE_STATE.contains("cz")) {
                        onDataReceiveListener.onDataReceiveCar(404, "0");
                    } else {
                        onDataReceiveListener.onDataReceiveCar(106, "0");
                    }
                } else {
                    onDataReceiveListener.onDataReceiveCar(1061, "0");
                }
            }
        } else if (data.get(CAR_COMMAND) == CAR_SEARCH) {
            //寻卡反馈
            if (onDataReceiveListener != null) {
                if (data.get(CAR_STATUS) == 0) {
                    Log.e("sendC_xxxx7", "777");
                    onDataReceiveListener.onDataReceiveCar(107, "0");
                } else {
                    onDataReceiveListener.onDataReceiveCar(1071, "0");
                }
            }
        }
//        else if (data.get(CAR_COMMAND) == CAR_SEARCH) {
//            //寻卡反馈
//            if (onDataReceiveListener != null) {
//                if (data.get(CAR_STATUS) == 0) {
//                    Log.e("sendC_xxxx7", "777");
//                    onDataReceiveListener.onDataReceiveCar(107, "0");
//                } else {
//                    onDataReceiveListener.onDataReceiveCar(1071, "0");
//                }
//            }
//        }
        else if (data.get(CAR_COMMAND) == CAR_GET_NUM) {
            Log.e("TAG", "读卡操作=============93======" + state_m1 + "  manger.length==" + manger.length);
            //读卡操作  93   获取物理卡号，逻辑卡号 ，SAK  R_PUB_QRY_PHYSICS_INFO
            if (onDataReceiveListener != null) {
                Log.e("TAG", "onDataReceiveListener==" + onDataReceiveListener);
                if (data.get(CAR_STATUS) == 0) {
                    Log.e("TAG", "data.get(CAR_STATUS) == 0==");
                    manger_num = new ArrayList<>();
                    for (int i = 0; i < manger.length; i++) {
                        manger_num.add(manger[i]);
                        car_manger_info_1 += (" " + manger[i]);
                    }
                    Log.e("TAG", "data.get(CAR_STATUS) == 0==");
                    manger_num_card = new ArrayList<>();
                    for (int i = 0; i < 8; i++) {
                        manger_num_card.add(manger[i + 12]);
                    }
                    //查找和执行M1充正
                    M1RollBackSubmit();
                    //查找和执行CPU提交
                    CpuLoadSubmit();
                    CZ_CARD_NUM = check_CARD(manger_num, manger_num.size());
                    Constant.Companion.setCZ_CARD_NUM(bytesToHexString(CZ_CARD_NUM));
                    //
                    System.arraycopy(CZ_CARD_NUM, 4, g_FNO, 0, g_FNO.length);
                    System.arraycopy(CZ_CARD_NUM, 12, g_LNO, 0, g_LNO.length);
                    str_g_FNO = SerialDataUtils.Bytes2HexString(g_FNO, 0, g_FNO.length);//物理卡号
                    str_g_LNO = SerialDataUtils.Bytes2HexString(g_LNO, 0, g_LNO.length);//逻辑卡号
//                    onDataReceiveListener.onDataReceiveCar(1103, str_g_LNO);
                    if (state_m1 == 0) {
                        onDataReceiveListener.onDataReceiveCar(108, check_CARD(manger_num, manger.length));
                        onDataReceiveListener.onDataReceiveCar(1082, check_CARD(manger_num_card, 8));
                    } else if (state_m1 == 1) {
                        //弹出充值
                        onDataReceiveListener.onDataReceiveCar(400, check_CARD(manger_num, manger.length));
                    }
                    Log.e("108_cmd", bytesToHexString(check_CARD(manger_num, manger.length)));
                } else {
                    onDataReceiveListener.onDataReceiveCar(1081, check_CARD(manger_num, manger.length));

                }
            }
        } else if (data.get(CAR_COMMAND) == CAR_GET_CARD_INFO_94) {
            //充值读卡应用信息 40位，用于查询余额，取卡片信息
            if (onDataReceiveListener != null) {
                if (data.get(CAR_STATUS) == 0) {
                    Log.e("sendC_xxxx94", "94");
                    manger_num = new ArrayList<>();

                    for (int i = 0; i < manger.length; i++) {
                        manger_num.add(manger[i]);
                        car_manger_info_1 += (" " + manger[i]);
                    }
                    CZ_CARD_NUM = check_CARD(manger_num, manger_num.size());
                    Constant.Companion.setCZ_CARD_NUM(bytesToHexString(CZ_CARD_NUM));
                    Log.e("manger", car_manger_info_1 + "");
                    onDataReceiveListener.onDataReceiveCar(113, check_CARD(manger_num, manger.length));
                } else {
                    onDataReceiveListener.onDataReceiveCar(1131, check_CARD(manger_num, manger.length));
                }
            }
        } else if (data.get(CAR_COMMAND) == PAY_SIGN_1) {
            //消费签到1
            if (onDataReceiveListener != null) {
                if (data.get(CAR_STATUS) == 0) {
                    manger_num = new ArrayList<>();

                    for (int i = 0; i < manger.length; i++) {
                        manger_num.add(manger[i]);
                        car_manger_info_1 += (" " + manger[i]);
                    }
                    Log.e("sendC_xxxx9", manger_num.size() + "");
                    //把握手流水号存储，其他串口要穿插用
                    onDataReceiveListener.onDataReceiveCar(109, check_CARD(manger_num, manger.length));
                } else {
                    onDataReceiveListener.onDataReceiveCar(1091, check_CARD(manger_num, manger.length));
                }
            }
            read_buffer.clear();
            car_manger_info = "";
            car_manger_info_1 = "";
        } else if (data.get(CAR_COMMAND) == PAY_SIGN_2) {
            //认证1
            if (onDataReceiveListener != null) {
                if (data.get(CAR_STATUS) == 0) {
                    manger_num = new ArrayList<>();
                    for (int i = 0; i < manger.length; i++) {
                        manger_num.add(manger[i]);
                        car_manger_info_1 += (" " + manger[i]);
                    }
                    login_19_65 = check_CARD(manger_num, manger.length);
                    pay_check_login_1(check_CARD(manger_num, manger.length), 1);
                } else {
//                    onDataReceiveListener.onDataReceiveCar(1091, check_CARD(manger_num, manger.length));
                }
            }
        } else if (data.get(CAR_COMMAND) == PAY_LOGIN_1) {
//            认证2 返回数据 截取8位密钥  K19  k65
            if (onDataReceiveListener != null) {
                if (data.get(CAR_STATUS) == 0) {
                    manger_num = new ArrayList<>();
                    String test_123 = "";
                    for (int i = 0; i < 8; i++) {
                        manger_num.add(manger[i + 4]);
                        test_123 += (" " + manger[i + 4]);
                    }
                    Log.e("test_123", test_123);
                    if (login_title == 1) {
                        login_19 = check_CARD(manger_num, 8);
                        Log.e("test_123_19", bytesToHexString(login_19));
                        //81 3E CC 20 F8 B2 F0 11
                        //68 EC DA A7 20 FA 14 AC
                        Constant.Companion.setCAR_SIGN_19(bytesToHexString(login_19));
//                        App.Companion.getSpUtil().putString("CAR_SIGN_19",bytesToHexString(login_19));
                        login_title = 2;
                        pay_check_login_2(login_19_65, 2);
                    } else {

                        login_65 = check_CARD(manger_num, 8);
                        Log.e("test_123_65", "22222222");
                        Constant.Companion.setCAR_SIGN_65(bytesToHexString(login_65));
//                        App.Companion.getSpUtil().putString("CAR_SIGN_65",login_65.toString());
                        login_title = 1;
                    }
                } else {
                    onDataReceiveListener.onDataReceiveCar(1091, check_CARD(manger_num, manger.length));
                }
            }
        } else if (data.get(CAR_COMMAND) == PAY_CARD_SEARCH) {
            if (onDataReceiveListener != null) {
                if (data.get(CAR_STATUS) == 0) {
                    manger_num = new ArrayList<>();
                    for (int i = 0; i < manger.length; i++) {
                        manger_num.add(manger[i]);
                    }

                    manger_num_card = new ArrayList<>();
                    for (int i = 0; i < 8; i++) {
                        manger_num_card.add(manger[i + 13]);
                    }

                    byte[] cardsearch = new byte[52];
                    cardsearch = check_CARD(manger_num, manger_num.size());
                    System.arraycopy(cardsearch, 0, buff, 0, 52);
                    bWalletType = (byte) buff[4];//钱包类别
                    System.arraycopy(buff, 5, g_FNO, 0, g_FNO.length);//保存物理卡号
                    System.arraycopy(buff, 13, g_LNO, 0, g_LNO.length);//保存逻辑卡号
                    String mg_LNO = SerialDataUtils.Bytes2HexString(g_LNO, 0, g_LNO.length);
                    onDataReceiveListener.onDataReceiveCar(1103, mg_LNO);
                    g_DLimit = (int) cardsearch[29] * 100; //金额下限
                    if (card_serch == 0) {
                        //B4读到卡，拿到卡中数据，拼接加密之后 ，发送服务器，B0
                        onDataReceiveListener.onDataReceiveCar(110, check_CARD(manger_num, manger.length));
                        onDataReceiveListener.onDataReceiveCar(1082, check_CARD(manger_num_card, 8));
                    } else if (card_serch == 1) {
                        //B4读到卡，拿到卡中数据 ,显示余额
                        int intmoney = SerialDataUtils.toBdInt(cardsearch, 21, 4);//卡余额
                        int dmoney = (int) cardsearch[29] * 100; //金额下限
                        int showmoney = 0;
                        String money = "";
                        if (intmoney >= dmoney)
                            money = new BigDecimal("" + (intmoney - dmoney)).movePointLeft(2) + "元";        //显示正数
                        else
                            money = "-" + new BigDecimal("" + (dmoney - intmoney)).movePointLeft(2) + "元";   //显示负数

                        onDataReceiveListener.onDataReceiveCar(1102, money);
                        // String money = manger_num.get(21)+manger_num.get(22)+manger_num.get(23)+manger_num.get(24);
                        //onDataReceiveListener.onDataReceiveCar(1102, Integer.parseInt(money, 16)+"");
                    }
                } else {
                    //未读到卡的状态
                    onDataReceiveListener.onDataReceiveCar(1101, check_CARD(manger_num, manger.length));
                }
            }
        } else if (data.get(CAR_COMMAND) == PAY_CARD_BUY_B5) {
//            消费预处理 成功开始 B6扣费  失败 提示原因 进行其他操作
            if (onDataReceiveListener != null) {
                if (data.get(CAR_STATUS) == 0) {
                    //消费预处理成功,
                    pay_order_upload = new ArrayList<>();
                    String b0_info = "";
                    for (int i = 0; i < manger.length; i++) {
                        pay_order_upload.add(manger[i]);
                        b0_info += (" " + manger[i]);
                    }
                    Log.e("b0_info", b0_info);
                    //保存消费未完整数据2020-8-31
                    //SerialDataUtils.SetByteArray(buff, (byte)0x00, buff.length);
                    byte[] card_buy = new byte[100];
                    card_buy = check_CARD(pay_order_upload, pay_order_upload.size());
                    String sstr = null;
                    String str = null;
                    if (bWalletType == 0x01)//M1钱包
                    {
                        str = SerialDataUtils.Bytes2HexString(card_buy, 4, 73);
                        sstr = "01" + str;
                    } else if (bWalletType == 0x02)//CPU钱包
                    {
                        str = SerialDataUtils.Bytes2HexString(card_buy, 4, 87);
                        sstr = "02" + str;
                    }
                    SaveNotCompleteRecord(sstr);//保存未完整消费记录
                    pay_test_buy_2();
                } else {
                    ShowReaderError(data.get(CAR_STATUS));
                }
            }
        } else if (data.get(CAR_COMMAND) == PAY_CARD_BUY_B6) {
//            扣款成功，开始B1上传数据，否则提示
            if (onDataReceiveListener != null) {
                if (data.get(CAR_STATUS) == 0) {
                    List<String> mlist = new ArrayList<>();
                    for (int i = 0; i < 4; i++) {
                        mlist.add(manger[i + 4]);
                    }
                    //消费处理成功,开始出货
                    byte[] brec = new byte[100];
                    if (bWalletType == 0x02)//CPU钱包
                    {
                        byte[] bTac = new byte[4];

                        bTac = check_CARD(mlist, 4);
                        Log.i("CPU TAC", bytesToHexString(bTac));
                        //SerialDataUtils.SetByteArray(buff, (byte)0x00, buff.length);
                        brec = check_CARD(pay_order_upload, pay_order_upload.size());
                        //pay_order_upload    BD XX B5 00 XX XX .... FF   总共87+5=92个字节  buff[4]....buff[90] 为CPU数据   其中 buff[87]--buff[90] 为CPU卡TAC码
                        //更新TAC码进去
                        //System.arraycopy(bTac, 0, brec, 87, 4);
                        upDateCpuTac(brec, bTac);//brec -> BD XX B5 00 XXXXXXXXX(87) XOR
                        g_TrnasMoney = SerialDataUtils.toBdInt(brec, 37, 4);
                        g_Consume_Balance = SerialDataUtils.toBdInt(brec, 45, 4);

                    } else {
                        brec = check_CARD(pay_order_upload, pay_order_upload.size());
                        g_TrnasMoney = SerialDataUtils.toBdInt(brec, 42, 4);
                        g_Consume_Balance = SerialDataUtils.toBdInt(brec, 46, 4);
                    }
                    ClearNotCompleteRecord();//删除未完整交易记录
                    //ConsumeSuccessShow();
                    onDataReceiveListener.onDataReceiveCar(111, check_CARD(pay_order_upload, pay_order_upload.size()));
                } else {
                    //消费处理失败
                    String str_notcomprec = ReadNotCompleteRecord();
                    if (null == str_notcomprec) {
                        //没有未完整交易记录，属于异常情况，出现的机率很低！！！
                        Log.e("yct consume info", "消费失败!");
                        onDataReceiveListener.onDataReceiveCar(1121, "消费失败!");
                    } else {
                        byte[] not_comp_rec = new byte[100];
                        not_comp_rec = SerialDataUtils.HexString2Bytes(str_notcomprec);

                        SerialDataUtils.SetByteArray(buff_data, (byte) 0x00, buff_data.length);
                        if (bWalletType == 0x01) {
                            System.arraycopy(not_comp_rec, 5, buff_data, 0, 8);//逻辑卡号8
                            System.arraycopy(not_comp_rec, 13, buff_data, 8, 4);//物理卡号4 后4个字节0
                            System.arraycopy(not_comp_rec, 52, buff_data, 16, 2);//票卡交易计数器2
                            System.arraycopy(not_comp_rec, 39, buff_data, 18, 4);//交易金额4
                            System.arraycopy(not_comp_rec, 43, buff_data, 22, 4);//本次余额4
                        } else {
                            System.arraycopy(not_comp_rec, 18, buff_data, 0, 8);//逻辑卡号8
                            System.arraycopy(not_comp_rec, 26, buff_data, 8, 8);//物理卡号8
                            System.arraycopy(not_comp_rec, 50, buff_data, 16, 2);//票卡交易计数器2
                            System.arraycopy(not_comp_rec, 34, buff_data, 18, 4);//交易金额4
                            System.arraycopy(not_comp_rec, 42, buff_data, 22, 4);//本次余额4
                        }
                        //本次余额
                        ulBalance = SerialDataUtils.toBdInt(buff_data, 22, 4);
                        //交易金额
                        ulTransMoney = SerialDataUtils.toBdInt(buff_data, 18, 4);
                        yct_purchase_reprocess(buff_data);//0xB7

                    }
                    //ClearNotCompleteRecord();//删除未完整交易记录
                    //onDataReceiveListener.onDataReceiveCar(1111, check_CARD(pay_order_upload, pay_order_upload.size()));
                }
            }
        } else if (data.get(CAR_COMMAND) == PAY_CARD_BUY_B7) {
            //未完整处理B7
            byte readerstatus = 0;
            readerstatus = (byte) data.get(CAR_STATUS);
            if (readerstatus == 0) {
                ///上次交易成功，提示交易成功 消费处理成功,开始出货
                List<String> mlist = new ArrayList<>();
                for (int i = 0; i < 4; i++) {
                    mlist.add(manger[i + 4]);
                }
                byte[] bTac = new byte[4];
                byte[] ucRecord = new byte[88];
                String str_result = null;
                if (bWalletType == 0x02)//CPU钱包
                {
                    bTac = new byte[4];
                    bTac = check_CARD(mlist, 4);
                    Log.i("yct info:", "补CPU TAC:" + bytesToHexString(bTac));
                    SerialDataUtils.SetByteArray(buff, (byte) 0x00, buff.length);
                    str_result = ReadNotCompleteRecord();
                    Log.i("yct info", "FILE RECORD:" + str_result);
                    ucRecord = SerialDataUtils.HexString2Bytes(str_result);
                    buff[0] = (byte) 0xBD;
                    buff[1] = (byte) 0x5A;
                    buff[2] = (byte) 0xB5;
                    buff[3] = (byte) 0x00;
                    System.arraycopy(ucRecord, 1, buff, 4, 87);
                    byte sum = 0;
                    for (int i = 0; i < 91; i++) {//校验和
                        sum ^= buff[i];
                    }
                    buff[91] = (byte) sum;
                    upDateCpuTac(buff, bTac);
                    g_TrnasMoney = SerialDataUtils.toBdInt(buff, 37, 4);
                    g_Consume_Balance = SerialDataUtils.toBdInt(buff, 45, 4);
                    //最后删除未完整记录
                    ClearNotCompleteRecord();//删除未完整交易记录
                } else {
                    str_result = ReadNotCompleteRecord();
                    ucRecord = SerialDataUtils.HexString2Bytes(str_result);
                    g_TrnasMoney = SerialDataUtils.toBdInt(ucRecord, 39, 4);
                    g_Consume_Balance = SerialDataUtils.toBdInt(ucRecord, 43, 4);
                    ClearNotCompleteRecord();//删除未完整交易记录
                }
                //ConsumeSuccessShow();
                bComsume = 0;
                onDataReceiveListener.onDataReceiveCar(111, check_CARD(pay_order_upload, pay_order_upload.size()));

            } else {
                //上次交易不成功，继续消费流程。。。

                if (bComsume == 0) {
                    //做完B6直接做B7的情况
                    if ((readerstatus == (byte) 0x80) || (readerstatus == (byte) 0x83) || (readerstatus == (byte) 0x84) || (readerstatus == (byte) 0x1A)) {
                        //一般是消费过程中闪卡造成，此处不删除未完整消费记录，下一次直接做未完整
                        Log.e("yct info:", "闪卡消费失败!不删除未完整消费记录，下一次直接做未完整!");
                        onDataReceiveListener.onDataReceiveCar(1121, "消费失败，请重试!");
                    } else {
                        Log.e("yct info:", "消费失败!删除未完整交易记录!");
                        ClearNotCompleteRecord();//删除未完整交易记录
                        onDataReceiveListener.onDataReceiveCar(1121, "消费失败!");
                    }


                } else {
                    //寻卡后发现有未完整记录做B7的情况
                    bComsume = 0;
                    ClearNotCompleteRecord();//删除未完整交易记录
                    pay_test_buy_1(ulTransMoney); //做消费预处理0xB5
                }

            }
        } else if (data.get(CAR_COMMAND) == PAY_CARD_BUY_B8) {
            if (onDataReceiveListener != null) {
                if (data.get(CAR_STATUS) == 0) {
                    ShowReaderError((byte) 0x94);


                }
            }

        } else if (data.get(CAR_COMMAND) == CAR_SET_CPU_INFO) {
            //0X95
            if (onDataReceiveListener != null) {
                if (data.get(CAR_STATUS) == 0) {
                    if (card_type.equals("cpu")) {
                        Log.e("send_set_cpu_info", "开始获取cpu信息");
                        R_CPU_GET_CARDINFO();//CPU卡取信息 0x79
                    } else if (card_type.equals("m1")) {
                        //获取m1卡物理信息第一次。交易类型查询结束。 再次读卡，开始圈存第一步，读卡
                        g_BALANCE_FRONT = 0;
                        g_BALANCE_LAST = 0;
                        CarSerialPortUtil.getInstance().R_PUB_QRY_PHYSICS_INFO(1);//0x93
                    } else if (card_type.equals("m1_2")) {
                        //M1 设置信息，第二次结束  开始获取M1卡数据 R_M1_GET_CARDINFO
                        Log.e("send_set_cpu_info", "第一次开始获取M1信息");
                        R_M1_GET_CARDINFO(1);//0xB9
                    }
                }
            }
        } else if (data.get(CAR_COMMAND) == CAR_GET_CPU_INFO) {
            //CPU卡取信息 0x79
            if (onDataReceiveListener != null) {
                manger_num = new ArrayList<>();
                for (int i = 0; i < manger.length; i++) {
                    manger_num.add(manger[i]);
                }

                if (data.get(CAR_STATUS) == 0) {

                    byte[] cpu_info = new byte[52];
                    cpu_info = check_CARD(manger_num, manger_num.size());
                    //System.arraycopy(cpu_info, 0, buff, 0, cpu_info.length);
//                    System.arraycopy(buff, 4, g_PKI_NO, 20, g_PKI_NO.length);
//                    System.arraycopy(buff, 8, g_FNO, 0, g_FNO.length);
//                    System.arraycopy(buff, 16, g_LNO, 0, g_LNO.length);
                    System.arraycopy(cpu_info, 4, g_PKI_NO, 0, g_PKI_NO.length);
                    System.arraycopy(cpu_info, 8, g_FNO, 0, g_FNO.length);
                    System.arraycopy(cpu_info, 16, g_LNO, 0, g_LNO.length);

                    str_g_PKI_NO = SerialDataUtils.Bytes2HexString(g_PKI_NO, 0, g_PKI_NO.length);//PKI管理卡号
                    str_g_FNO = SerialDataUtils.Bytes2HexString(g_FNO, 0, g_FNO.length);//物理卡号8
                    str_g_LNO = SerialDataUtils.Bytes2HexString(g_LNO, 0, g_LNO.length);//逻辑卡号8
                    System.arraycopy(cpu_info, 24, g_BalanceLimit, 0, g_BalanceLimit.length);//钱包余额4+金额下限1+金额上限3
                    System.arraycopy(cpu_info, 33, g_CountInfo, 0, g_CountInfo.length);//计数信息16
                    Log.e("0x79 readinfo:", "g_BalanceLimit:" + SerialDataUtils.Bytes2HexString(g_BalanceLimit, 0, g_BalanceLimit.length));


                    //充值处理信息成功,开始下一步
                    Log.e("send_get_cpu_info", "获取cpu成功，开始 P_CPU_LOAD_INIT");
                    if (state_read == 0) {
                        //正常读卡操作5
                        g_BALANCE_FRONT = SerialDataUtils.toBdInt(g_BalanceLimit, 0, 4);//充值前卡片余额
                        Log.e("0x79 readinfo:", "充值前余额:" + SerialDataUtils.Bytes2HexString(g_BalanceLimit, 0, 4));
                        onDataReceiveListener.onDataReceiveCar(300, check_CARD(manger_num, manger.length));
                    } else {
                        state_read = 0;
                        //读卡 上传充值操作
                        byte[] ucTempData = new byte[]{(byte) 0xAA, (byte) 0x85, 0x00, 0x01, 0x02, (byte) 0x80, 0x00, (byte) 0x78};
                        System.arraycopy(ucTempData, 0, ucTemp, 0, 8);
                        System.arraycopy(g_PKI_NO, 0, ucTemp, 20, 4);//PKI管理卡号
                        g_SYSTEMSEQ_LOAD = SerialDataUtils.HexString2Bytes(str_g_SYSTEMSEQ_LOAD);
                        System.arraycopy(g_SYSTEMSEQ_LOAD, 0, ucTemp, 24, 8);//系统流水号
                        System.arraycopy(g_FNO, 0, ucTemp, 32, 8);//物理卡号
                        System.arraycopy(g_LNO, 0, ucTemp, 40, 8);//逻辑卡号
                        g_BALANCE_LAST = SerialDataUtils.toBdInt(g_BalanceLimit, 0, 4);//充值后卡片余额
                        Log.e("0x79 readinfo:", "充值后余额:" + SerialDataUtils.Bytes2HexString(g_BalanceLimit, 0, 4));
                        ucTemp[48] = 0x00; //圈存状态
                        System.arraycopy(g_7B_TACSW, 0, ucTemp, 49, 6);//交易认证码+卡片返回码
                        ucTemp[55] = (byte) g_LOADWRITECARDCODE; //读卡器返回码
                        str_g_LOADNO = SerialDataUtils.Bytes2HexString(g_LOADNO, 0, 4);
                        System.arraycopy(g_LOADNO, 0, ucTemp, 56, 4);//充值凭证号
                        Log.e("yct info:", "充值凭证号" + str_g_LOADNO);

                        //交易金额
                        ucTemp[60] = (byte) ((g_FARE >> 24) & 0x000000FF);
                        ucTemp[61] = (byte) ((g_FARE >> 16) & 0x000000FF);
                        ucTemp[62] = (byte) ((g_FARE >> 8) & 0x000000FF);
                        ucTemp[63] = (byte) (g_FARE & 0x000000FF);
                        System.arraycopy(g_BalanceLimit, 0, ucTemp, 64, 8);//钱包余额4+金额下限1+金额上限3
                        System.arraycopy(g_CountInfo, 0, ucTemp, 72, 16);//计数信息
                        String str = "0080";//长度
                        String strRecord = SerialDataUtils.Bytes2HexString(ucTemp, 0, 128);
                        str += strRecord;
                        Log.e("SaveCpuSubmitData:", str);

                        SaveCpuSubmitData(str);//保存CPU提交数据

                        if (g_BALANCE_LAST == (g_BALANCE_FRONT + g_FARE)) {
                            ViewEventNotifier.INSTANCE.sendMessage(200, "充值成功=1=" + g_BALANCE_LAST);
                            //余额正确，充值成功
                            Log.e("yct success info:", "充值成功!");
                            CpuRechargeSucessShow(0);
                        } else {
                            //onDataReceiveListener.onDataReceiveCar(1121, "充值失败!\n请到其他充值点继续完成卡片充值!");
                            ShowRechargeFail();
                        }
                        AddSocketClient.setRollBackStatus((byte) 0x00);//圈存状态
                        String strCpudata = ReadCpuSubmitData();
                        byte[] sscpudata = new byte[200];
                        sscpudata = SerialDataUtils.HexString2Bytes(strCpudata);
                        onDataReceiveListener.onDataReceiveCar(302, sscpudata);
                    }
                } else {
                    //充值处理失败
                    Log.e("TAG", "state_read===========" + state_read);
                    if (state_read == 0) {
                        onDataReceiveListener.onDataReceiveCar(1121, "取CPU卡信息失败!\n请到飞充继续完成卡片充值!");
                        onDataReceiveListener.onDataReceiveCar(3001, check_CARD(manger_num, manger.length));
                    } else {
                        //state_read = 0;
                        if (state_read < 10) {
                            state_read++;
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            onDataReceiveListener.onDataReceiveCar(1121, "请将卡片放在读卡区!");
                            R_CPU_GET_CARDINFO();
                        } else {
                            state_read = 0;
                            String str = SerialDataUtils.Bytes2HexString(g_7B_TACSW, 4, 2);//SW1SW2
                            if ((g_LOADWRITECARDCODE == 0) && (str.equals("9000"))) {
                                //疑是成功
                                Log.e("yct success info:", "充值疑是成功!");
                                g_BALANCE_LAST = g_BALANCE_FRONT + g_FARE;
                                //CPU记录不完整交易记录
                                SerialDataUtils.SetByteArray(ucTemp, (byte) 0x00, ucTemp.length);
                                byte[] ucTempData = new byte[]{(byte) 0xAA, (byte) 0x85, 0x00, 0x01, 0x02, (byte) 0x80, 0x00, (byte) 0x78};
                                System.arraycopy(ucTempData, 0, ucTemp, 0, 8);
                                System.arraycopy(g_PKI_NO, 0, ucTemp, 20, 4);//PKI管理卡号
                                g_SYSTEMSEQ_LOAD = SerialDataUtils.HexString2Bytes(str_g_SYSTEMSEQ_LOAD);
                                System.arraycopy(g_SYSTEMSEQ_LOAD, 0, ucTemp, 24, 8);//系统流水号
                                System.arraycopy(g_FNO, 0, ucTemp, 32, 8);//物理卡号
                                System.arraycopy(g_LNO, 0, ucTemp, 40, 8);//逻辑卡号
                                ucTemp[48] = 0x00; //圈存状态
                                System.arraycopy(g_7B_TACSW, 0, ucTemp, 49, 6);//交易认证码+卡片返回码
                                ucTemp[55] = (byte) g_LOADWRITECARDCODE; //读卡器返回码
                                System.arraycopy(g_LOADNO, 0, ucTemp, 56, 4);//充正凭证号
                                //交易金额
                                ucTemp[60] = (byte) ((g_FARE >> 24) & 0x000000FF);
                                ucTemp[61] = (byte) ((g_FARE >> 16) & 0x000000FF);
                                ucTemp[62] = (byte) ((g_FARE >> 8) & 0x000000FF);
                                ucTemp[63] = (byte) (g_FARE & 0x000000FF);
                                //无法重新读卡全部填00
                                //System.arraycopy(g_BalanceLimit, 0, ucTemp, 64, 8);//钱包余额4+金额下限1+金额上限3
                                //System.arraycopy(g_CountInfo, 0, ucTemp, 72, 16);//计数信息

                                str = "0080";//长度
                                String strRecord = SerialDataUtils.Bytes2HexString(ucTemp, 0, 128);
                                str += strRecord;
                                SaveCpuSubmitData(str);//保存CPU提交数据
                                Log.e("yct cpu info", "终端记录不完整交易记录:" + str);

                                CpuRechargeSucessShow(1);

                                strRecord = CarSerialPortUtil.ReadCpuSubmitData();
                                if (null != strRecord) {
                                    byte[] test_buff = new byte[200];
                                    test_buff = SerialDataUtils.HexString2Bytes(strRecord);
                                    ucTempData = new byte[]{(byte) 0xAA, (byte) 0x85, 0x00, 0x01, 0x02, (byte) 0x80, 0x00, (byte) 0x78};
                                    if (SerialDataUtils.ByteMemCmp(test_buff, 0, ucTempData, 0, 8) != 0) {
                                        Log.e("yct error info:", "圈存数据异常，无法做圈存状态提交!");
                                    } else {
                                        AddSocketClient.setRollBackStatus((byte) 0x00);//圈存状态
                                        onDataReceiveListener.onDataReceiveCar(302, test_buff);//圈存提交
                                    }
                                } else {
                                    Log.e("yct error info:", "圈存数据异常，无法做圈存状态提交!");
                                    Log.e("yct error info:", "系统异常!");
                                }

                            } else {
                                //onDataReceiveListener.onDataReceiveCar(1121, "充值失败!\n请到其他充值点继续完成卡片充值!");
                                ShowRechargeFail();
                                String strRecord = CarSerialPortUtil.ReadCpuSubmitData();
                                if (null != strRecord) {
                                    byte[] test_buff = new byte[200];
                                    test_buff = SerialDataUtils.HexString2Bytes(strRecord);
                                    byte[] ucTempData = new byte[]{(byte) 0xAA, (byte) 0x85, 0x00, 0x01, 0x02, (byte) 0x80, 0x00, (byte) 0x78};
                                    if (SerialDataUtils.ByteMemCmp(test_buff, 0, ucTempData, 0, 8) != 0) {
                                        Log.e("yct error info:", "圈存数据异常，无法做圈存状态提交!");
                                    } else {
                                        AddSocketClient.setRollBackStatus((byte) 0x00);//圈存状态
                                        onDataReceiveListener.onDataReceiveCar(302, test_buff);//圈存提交
                                    }
                                } else {
                                    Log.e("yct error info:", "圈存数据异常，无法做圈存状态提交!");
                                    Log.e("yct error info:", "系统异常!");
                                }
                            }
                        }

                    }
                }
            }
        } else if (data.get(CAR_COMMAND) == CAR_GET_CPU_INIT) {
            //CPU钱包圈存初始化 0x7A
            Log.e("yct cpu info", "CPU钱包圈存初始化 0x7A!!!");
            Log.e("yct info", "正在充值中，请勿移动卡片...");
            onDataReceiveListener.onDataReceiveCar(1121, "正在充值中，请勿移动卡片...");
            if (onDataReceiveListener != null) {
                manger_num = new ArrayList<>();
                for (int i = 0; i < manger.length; i++) {
                    manger_num.add(manger[i]);
                }
                g_LOADWRITECARDCODE = data.get(CAR_STATUS);
                if (data.get(CAR_STATUS) == 0) {
                    //CPU记录不完整交易记录
                    SerialDataUtils.SetByteArray(ucTemp, (byte) 0x00, ucTemp.length);
                    byte[] ucTempData = new byte[]{(byte) 0xAA, (byte) 0x85, 0x00, 0x01, 0x02, (byte) 0x80, 0x00, (byte) 0x78};
                    System.arraycopy(ucTempData, 0, ucTemp, 0, 8);
                    System.arraycopy(g_PKI_NO, 0, ucTemp, 20, 4);//PKI管理卡号
                    g_SYSTEMSEQ_LOAD = SerialDataUtils.HexString2Bytes(str_g_SYSTEMSEQ_LOAD);
                    System.arraycopy(g_SYSTEMSEQ_LOAD, 0, ucTemp, 24, 8);//系统流水号
                    System.arraycopy(g_FNO, 0, ucTemp, 32, 8);//物理卡号
                    System.arraycopy(g_LNO, 0, ucTemp, 40, 8);//逻辑卡号
                    ucTemp[48] = 0x01; //圈存状态
                    System.arraycopy(g_7B_TACSW, 0, ucTemp, 49, 6);//交易认证码+卡片返回码
                    ucTemp[55] = g_LOADWRITECARDCODE; //读卡器返回码
                    System.arraycopy(g_LOADNO, 0, ucTemp, 56, 4);//充正凭证号
                    String strloadno = SerialDataUtils.Bytes2HexString(g_LOADNO, 0, 4);
                    Log.e("yct info:", "充值凭证号:" + strloadno);
                    //交易金额
                    ucTemp[60] = (byte) ((g_FARE >> 24) & 0x000000FF);
                    ucTemp[61] = (byte) ((g_FARE >> 16) & 0x000000FF);
                    ucTemp[62] = (byte) ((g_FARE >> 8) & 0x000000FF);
                    ucTemp[63] = (byte) (g_FARE & 0x000000FF);
                    System.arraycopy(g_BalanceLimit, 0, ucTemp, 64, 8);//钱包余额4+金额下限1+金额上限3
                    System.arraycopy(g_CountInfo, 0, ucTemp, 72, 16);//计数信息

                    String str = "0080";//长度
                    String strRecord = SerialDataUtils.Bytes2HexString(ucTemp, 0, 128);
                    str += strRecord;
                    SaveCpuSubmitData(str);//保存CPU提交数据
                    Log.e("yct cpu info", "终端记录不完整交易记录:" + str);

                    //充值处理信息成功,开始下一步
                    Log.e("send_get_cpu_info", "cpu INIT成功，开始 P_CPU_LOAD_QRY");
                    ucTempData = new byte[]{(byte) 0xAA, (byte) 0x83, 0x00, 0x01, 0x02, (byte) 0x80, 0x00, (byte) 0x98};
                    str_PacketHead = SerialDataUtils.Bytes2HexString(ucTempData, 0, 8);
                    state_read = 0;
                    onDataReceiveListener.onDataReceiveCar(301, check_CARD(manger_num, manger.length));//AA84->AA84
                } else {
                    //充值处理失败
                    Log.e("yct cpu info", "CPU钱包圈存初始化失败!");

                    onDataReceiveListener.onDataReceiveCar(3011, check_CARD(manger_num, manger.length));
                    ShowRechargeFail();
                }
            }
        } else if (data.get(CAR_COMMAND) == CAR_GET_CPU_LOAD) {
            //CPU钱包圈存 0x7B
            Log.e("yct info", "正在充值中，请勿移动卡片...");
            onDataReceiveListener.onDataReceiveCar(1121, "正在充值中，请勿移动卡片...");
            if (onDataReceiveListener != null) {
                manger_num = new ArrayList<>();
                for (int i = 0; i < manger.length; i++) {
                    manger_num.add(manger[i]);
                }
                g_LOADWRITECARDCODE = data.get(CAR_STATUS);
                if (data.get(CAR_STATUS) == 0) {
                    //充值处理信息成功,开始下一步  R_CPU_GET_CARDINFO
                    Log.e("send_get_cpu_info", "cpu load成功，开始R_CPU_GET_CARDINFO 重新读卡之后，进行上传操作");
                    //R_CPU_GET_CARDINFO();
                } else {
                    //此处不报错，继续读卡走确认流程
                    Log.e("yct error info", "--- 7B写卡报错!");
                    //充值处理失败
//                    onDataReceiveListener.onDataReceiveCar(2011, check_CARD(manger_num, manger.length));
                }
                //g_7B_TACSW : 交易认证码4+卡片状态码2
                byte[] bTacSw = new byte[8];
                bTacSw = check_CARD(manger_num, manger_num.size());
                System.arraycopy(bTacSw, 4, g_7B_TACSW, 0, 6);
                onDataReceiveListener.onDataReceiveCar(303, check_CARD(manger_num, manger.length));//保存数据的
                state_read = 1;
                R_CPU_GET_CARDINFO();
            }
        } else if (data.get(CAR_COMMAND) == CAR_GET_M1_INFO) {
            //R_M1_GET_CARDINFO(M1取卡信息0xB9)
            if (onDataReceiveListener != null) {
                manger_num = new ArrayList<>();
                for (int i = 0; i < manger.length - 4; i++) {
                    manger_num.add(manger[i + 4]);
                }

                Log.e("send_get_M1_info", "获取M1成功，开始 P_M1_LOAD====" + M1_CARDINFO_TYPE);
                if (data.get(CAR_STATUS) == 0) {
                    byte[] getinfo = new byte[122];
                    Log.e("yct info", "正在充值中，请勿移动卡片...");
                    onDataReceiveListener.onDataReceiveCar(1121, "正在充值中，请勿移动卡片...");
                    if (M1_CARDINFO_TYPE == 1)//M1卡取卡信息第一次
                    {
                        //充值处理信息成功,开始下一步
                        Log.e("send_get_M1_info", "获取M1成功，开始 P_M1_LOAD");
                        //SerialDataUtils.SetByteArray(buff, (byte)0x00, buff.length);
//                        buff = check_CARD(manger_num, manger.length);

                        getinfo = check_CARD(manger_num, manger_num.size());
                        //System.arraycopy(getinfo, 0, buff, 0, getinfo.length);
                        int iResult = SerialDataUtils.ByteMemCmp(g_LNO, 0, getinfo, 12, g_LNO.length);
                        if (iResult != 0) {
                            Log.e("比较卡片信息:", "卡号不一致。。。");
                            onDataReceiveListener.onDataReceiveCar(1121, "卡号不一致");
                            ShowRechargeFail();
                        } else {
                            SerialDataUtils.SetByteArray(buff_data, (byte) 0x00, buff_data.length);
                            //卡余额 低位在前
                            buff_data[3] = getinfo[108];
                            buff_data[2] = getinfo[109];
                            buff_data[1] = getinfo[110];
                            buff_data[0] = getinfo[111];
                            g_BALANCE_FRONT = SerialDataUtils.toBdInt(buff_data, 0, 4); //充值前卡余额
                            System.arraycopy(getinfo, 0, g_PKI_NO, 0, g_PKI_NO.length);//保存PKI卡号4
                            System.arraycopy(getinfo, 4, g_FNO, 0, g_FNO.length);//保存物理卡号8
                            System.arraycopy(getinfo, 12, g_LNO, 0, g_LNO.length);//保存逻辑卡号8
                            str_g_PKI_NO = SerialDataUtils.Bytes2HexString(g_PKI_NO, 0, g_PKI_NO.length);
                            str_g_FNO = SerialDataUtils.Bytes2HexString(g_FNO, 0, g_FNO.length);
                            str_g_LNO = SerialDataUtils.Bytes2HexString(g_LNO, 0, g_LNO.length);

                            //在P_M1_LOAD(AA63->AA64)前，终端记录不完整交易记录(冲正记录)
                            SerialDataUtils.SetByteArray(ucTemp, (byte) 0x00, ucTemp.length);
                            //byte[] ucTempData = new byte[]{(byte)0xAA,0x62,0x00,0x01,0x02,(byte)0x80,0x00,(byte)0x78};
                            byte[] ucTempData = new byte[]{(byte) 0xAA, 0x65, 0x00, 0x01, 0x02, (byte) 0x80, 0x00, (byte) 0xA8};
                            System.arraycopy(ucTempData, 0, ucTemp, 0, 8);

                            SerialDataUtils.SetByteArray(ucTempData, (byte) 0x00, ucTempData.length);
                            ucTempData = SerialDataUtils.HexString2Bytes(str_g_SYSTEMSEQ_LOAD);
                            System.arraycopy(ucTempData, 0, ucTemp, 20, 8);//系统流水号
                            SerialDataUtils.SetByteArray(ucTempData, (byte) 0x00, ucTempData.length);
                            ucTempData = SerialDataUtils.HexString2Bytes(str_g_PKI_NO);
                            System.arraycopy(g_PKI_NO, 0, ucTemp, 28, 4);//PKI卡号

                            System.arraycopy(g_FNO, 0, ucTemp, 32, 8);
                            System.arraycopy(g_LNO, 0, ucTemp, 40, 8);

                            ucTemp[48] = 0x01;//冲正原因
                            ucTemp[49] = 0;
                            ucTemp[50] = (byte) ((g_FARE >> 24) & 0x000000FF);
                            ucTemp[51] = (byte) ((g_FARE >> 16) & 0x000000FF);
                            ucTemp[52] = (byte) ((g_FARE >> 8) & 0x000000FF);
                            ucTemp[53] = (byte) (g_FARE & 0x000000FF);
                            System.arraycopy(g_LOADNO, 0, ucTemp, 54, 4);//充正凭证号
                            System.arraycopy(getinfo, 0, ucTemp, 58, 88);//交易信息88
                            String str = "00B0";//长度
                            String strRecord = SerialDataUtils.Bytes2HexString(ucTemp, 0, 176);
                            str += strRecord;
                            SaveRollBackRecord(str);//保存冲正信息
                            //bRollBack = 1;
                            Log.e("yct file:", "终端记录冲正信息:" + str);

                            Log.e("send_get_M1_info", "获取M1成功，开始 P_M1_LOAD");
                            //state:401 表示执行转到执行P_M1_LOAD AA63-->AA64
                            ucTempData = new byte[]{(byte) 0xAA, 0x63, 0x00, 0x01, 0x02, (byte) 0x80, 0x00, (byte) 0x98};
                            str_PacketHead = SerialDataUtils.Bytes2HexString(ucTempData, 0, 8);
//                            onDataReceiveListener.onDataReceiveCar(401, check_CARD(manger_num, manger.length));
                            onDataReceiveListener.onDataReceiveCar(401, check_CARD(manger_num, manger_num.size()));
                        }
                    } else//M1卡取卡信息第二次
                    {

                        //读卡成功,比较卡信息
                        Log.e("yct success info:", "读卡成功,比较卡信息");
                        getinfo = check_CARD(manger_num, manger_num.size());
                        //System.arraycopy(getinfo, 0, buff, 0, getinfo.length);
                        SerialDataUtils.SetByteArray(buff_data, (byte) 0x00, buff_data.length);
                        byte[] ucTempData = new byte[8];//{0x00,0x00,0x00,0x00};
                        ucTempData = SerialDataUtils.HexString2Bytes(str_g_LNO);
                        byte[] ucTempBlance = new byte[]{0x00, 0x00, 0x00, 0x00};
                        //卡余额 低位在前
                        ucTempBlance[3] = getinfo[108];
                        ucTempBlance[2] = getinfo[109];
                        ucTempBlance[1] = getinfo[110];
                        ucTempBlance[0] = getinfo[111];
                        g_BALANCE_LAST = SerialDataUtils.toBdInt(ucTempBlance, 0, 4); //充值后卡余额
                        int iResult = SerialDataUtils.ByteMemCmp(getinfo, 12, ucTempData, 0, 8);//比较逻辑卡号
                        if (iResult != 0) {
                            if (g_LOADWRITECARDCODE == 0) {
                                //疑是成功
                                Log.e("yct success info:", "充值疑是成功!");
                                ClearRollBackRecord();
                                RechargeSucessShow(1);
                            } else {
                                onDataReceiveListener.onDataReceiveCar(1121, "卡片前后不一致! ");
                                //onDataReceiveListener.onDataReceiveCar(1121, "充值失败!\n请到其他充值点继续完成卡片充值!");
                                ShowRechargeFail();
                            }
                        } else//iResult=0  逻辑卡号前后一致
                        {
                            //逻辑卡号前后一致
                            SerialDataUtils.SetByteArray(buff_data, (byte) 0x00, buff_data.length);
                            System.arraycopy(getinfo, 0, g_PKI_NO, 0, g_PKI_NO.length);//保存PKI卡号4
                            System.arraycopy(getinfo, 4, g_FNO, 0, g_FNO.length);//保存物理卡号8
                            System.arraycopy(getinfo, 12, g_LNO, 0, g_LNO.length);//保存逻辑卡号8
                            str_g_PKI_NO = SerialDataUtils.Bytes2HexString(g_PKI_NO, 0, g_PKI_NO.length);
                            str_g_FNO = SerialDataUtils.Bytes2HexString(g_FNO, 0, g_FNO.length);
                            str_g_LNO = SerialDataUtils.Bytes2HexString(g_LNO, 0, g_LNO.length);

                            SerialDataUtils.SetByteArray(ucTemp, (byte) 0x00, ucTemp.length);
                            //byte[] ucTempData = new byte[]{(byte)0xAA,0x62,0x00,0x01,0x02,(byte)0x80,0x00,(byte)0x78};
                            ucTempData = new byte[]{(byte) 0xAA, 0x65, 0x00, 0x01, 0x02, (byte) 0x80, 0x00, (byte) 0xA8};
                            System.arraycopy(ucTempData, 0, ucTemp, 0, 8);

                            SerialDataUtils.SetByteArray(buff_data, (byte) 0x00, 64);
                            byte[] temp_buff = new byte[8];
                            temp_buff = SerialDataUtils.HexString2Bytes(str_g_SYSTEMSEQ_LOAD);
                            System.arraycopy(temp_buff, 0, ucTemp, 20, 8);//系统流水号
                            SerialDataUtils.SetByteArray(buff_data, (byte) 0x00, buff_data.length);
                            temp_buff = new byte[4];
                            temp_buff = SerialDataUtils.HexString2Bytes(str_g_PKI_NO);
                            System.arraycopy(g_PKI_NO, 0, ucTemp, 28, 4);//PKI卡号

                            System.arraycopy(g_FNO, 0, ucTemp, 32, 8);
                            System.arraycopy(g_LNO, 0, ucTemp, 40, 8);

                            ucTemp[48] = 0x01;//冲正原因
                            ucTemp[49] = (byte) g_LOADWRITECARDCODE;
                            ucTemp[50] = (byte) ((g_FARE >> 24) & 0x000000FF);
                            ucTemp[51] = (byte) ((g_FARE >> 16) & 0x000000FF);
                            ucTemp[52] = (byte) ((g_FARE >> 8) & 0x000000FF);
                            ucTemp[53] = (byte) (g_FARE & 0x000000FF);
                            System.arraycopy(g_LOADNO, 0, ucTemp, 54, 4);//充正凭证号
                            System.arraycopy(getinfo, 0, ucTemp, 58, 88);//交易信息88
                            String str = "00B0";//长度
                            String strRecord = SerialDataUtils.Bytes2HexString(ucTemp, 0, 176);
                            str += strRecord;
                            SaveRollBackRecord(str);//保存冲正信息
                            //bRollBack = 1;
                            Log.e("yct file:", "终端记录冲正信息:" + str);

                            if (g_BALANCE_LAST == (g_BALANCE_FRONT + g_FARE)) {
                                ViewEventNotifier.INSTANCE.sendMessage(200, "充值成功=2=" + g_BALANCE_LAST);
                                //余额正确，充值成功
                                Log.e("yct success info:", "充值成功!");
                                ClearRollBackRecord();
                                RechargeSucessShow(0);
                            } else if (g_BALANCE_LAST == g_BALANCE_FRONT) {
                                //充值前与充值后金额相同，发起冲正
                                //onDataReceiveListener.onDataReceiveCar(1121, "充值失败!\n请到其他充值点继续完成卡片充值!");
                                Log.e("yct success info:", "M1卡充值前与充值后金额相同，发起冲正!");
                                strRecord = CarSerialPortUtil.ReadRollBackRecord();
                                if (null != strRecord)//冲正记录有效
                                {
                                    byte[] re_buff = new byte[200];
                                    re_buff = SerialDataUtils.HexString2Bytes(strRecord);
                                    //System.arraycopy(re_buff, 0, buff, 0, re_buff.length);
                                    ucTempData = new byte[]{(byte) 0xAA, 0x65, 0x00, 0x01, 0x02, (byte) 0x80, 0x00, (byte) 0xA8};
                                    if (SerialDataUtils.ByteMemCmp(re_buff, 0, ucTempData, 0, 8) != 0) {
                                        //写冲正记录时，记录没写进去，或者数据乱了
                                        Log.e("yct error info:", "冲正记录异常，无法做M1冲正!");
                                        onDataReceiveListener.onDataReceiveCar(1121, "冲正记录异常,冲正失败！");
                                        ShowRechargeFail();
                                    } else {
                                        Log.e("yct info:", "充值前与充值后金额相同，发起冲正...");
                                        // onDataReceiveListener.onDataReceiveCar(1121,"充值失败！");
                                        Log.e("yct error info:", "冲正记录正常，发起M1冲正!");
                                        ShowRechargeFail();
                                        //更新冲正状态
                                        re_buff[48] = 0x02;
                                        str = "00B0";//长度
                                        strRecord = "";
                                        strRecord = SerialDataUtils.Bytes2HexString(re_buff, 0, 176);
                                        str += strRecord;
                                        SaveRollBackRecord(str);//保存冲正信息
                                        AddSocketClient.setRollBackStatus((byte) 0x02);//冲正状态
                                        onDataReceiveListener.onDataReceiveCar(403, re_buff);////冲正
                                    }
                                } else//冲正记录无效
                                {
                                    Log.e("yct error info:", "冲正记录读取异常，未能做M1冲正!");
                                    Log.e("yct error info:", "系统异常!");
                                    Log.e("yct error info:", "系统需要维护!");
                                    //onDataReceiveListener.onDataReceiveCar(1121,"冲正记录异常,冲正失败！");
                                    //onDataReceiveListener.onDataReceiveCar(1121,"充值失败！");
                                    ShowRechargeFail();
                                }
                            } else {
                                Log.e("yct error info:", "充值后卡片余额异常!");
                                ShowRechargeFail();
                            }


                        }
                    }
                } else {
                    //读卡器返回码不是00，读卡失败
                    Log.e("yct error info", "M1_CARDINFO_TYPE:" + M1_CARDINFO_TYPE);
                    if (M1_CARDINFO_TYPE == 1) {
                        //获取数据处理失败
                        //onDataReceiveListener.onDataReceiveCar(4011, check_CARD(manger_num, manger.length));
                        //onDataReceiveListener.onDataReceiveCar(1121, "读卡失败!");
                        onDataReceiveListener.onDataReceiveCar(1121, "取M1卡信息失败!\n请到飞充继续完成卡片充值!");
                    } else {
                        if (M1_CARDINFO_TYPE < 10) {
                            M1_CARDINFO_TYPE++;
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            onDataReceiveListener.onDataReceiveCar(1121, "请将卡片放在读卡区!");
                            R_M1_GET_CARDINFO(M1_CARDINFO_TYPE);
                        } else {
                            //M1_CARDINFO_TYPE =10;
                            //充值需要再次获取卡数据 ，获取失败 进行冲正
                            //测试点2======================================
                            //g_LOADWRITECARDCODE = (byte)0x80;
                            //============================================
                            if (g_LOADWRITECARDCODE == 0) {
                                //疑是成功
                                Log.e("yct success info", "充值疑是成功!");
                                ClearRollBackRecord();
                                //onDataReceiveListener.onDataReceiveCar(1121, "充值疑是成功!");
                                RechargeSucessShow(1);
                            } else {
                                //状态码不是00，表示圈存失败，向充值服务器发起冲正
                                Log.e("yct error info", "冲正中。。。");
                                Log.e("yct error info", "3重读卡不成功.接收数据异常需要发起冲正,冲正原因02..");
                                String strRecord = CarSerialPortUtil.ReadRollBackRecord();
                                if (null != strRecord) {

                                    byte[] re_buff = new byte[200];
                                    re_buff = SerialDataUtils.HexString2Bytes(strRecord);
                                    Log.e("B9 RECV", "冲正金额：" + SerialDataUtils.Bytes2HexString(re_buff, 50, 4));
                                    Log.e("B9 RECV", "充值凭证号：" + SerialDataUtils.Bytes2HexString(re_buff, 54, 4));
                                    byte[] ucTempData = new byte[]{(byte) 0xAA, 0x65, 0x00, 0x01, 0x02, (byte) 0x80, 0x00, (byte) 0xA8};
                                    if (SerialDataUtils.ByteMemCmp(re_buff, 0, ucTempData, 0, 8) != 0) {
                                        //写冲正记录时，记录没写进去，或者数据乱了
                                        Log.e("yct error info:", "冲正记录异常，无法做M1冲正!");
                                        //onDataReceiveListener.onDataReceiveCar(1121,"冲正记录异常,冲正失败！");
                                        //onDataReceiveListener.onDataReceiveCar(1121,"充值失败！");
                                        onDataReceiveListener.onDataReceiveCar(1121, "充值失败!\n请到其他充值点继续完成卡片充值!");
                                    } else {
                                        //onDataReceiveListener.onDataReceiveCar(1121,"充值失败！");
                                        onDataReceiveListener.onDataReceiveCar(1121, "充值失败!\n请您到飞充\n继续完成卡片充值!\n");
                                        re_buff[48] = 0x02;
                                        byte[] M_ONE_INFO = new byte[88];
                                        SerialDataUtils.SetByteArray(M_ONE_INFO, (byte) 0x00, 88);
                                        System.arraycopy(M_ONE_INFO, 0, re_buff, 58, 88);
                                        String str = "00B0";//长度
                                        strRecord = "";
                                        strRecord = SerialDataUtils.Bytes2HexString(re_buff, 0, 176);
                                        str += strRecord;
                                        SaveRollBackRecord(str);//保存冲正信息                                       
                                        AddSocketClient.setRollBackStatus((byte) 0x02);//冲正状态
                                        onDataReceiveListener.onDataReceiveCar(403, re_buff);////冲正
                                    }
                                } else {
                                    Log.e("yct error info:", "冲正记录读取异常，未能做M1冲正!");
                                    Log.e("yct error info:", "系统异常!");
                                    Log.e("yct error info:", "系统需要维护!");
                                    //onDataReceiveListener.onDataReceiveCar(1121,"冲正记录异常,冲正失败！");
                                    //onDataReceiveListener.onDataReceiveCar(1121,"充值失败！");
                                    onDataReceiveListener.onDataReceiveCar(1121, "充值失败!\n请您到飞充\n继续完成卡片充值!\n");
                                }
                            }
                        }

                    }
                }
            }
        } else if (data.get(CAR_COMMAND) == CAR_R_M1_LOAD) {
            //R_M1_LOAD 0xBA  M1充值
            Log.e("yct info", "正在充值中，请勿移动卡片...");
            onDataReceiveListener.onDataReceiveCar(1121, "正在充值中，请勿移动卡片...");
            if (onDataReceiveListener != null) {
                manger_num = new ArrayList<>();
                for (int i = 0; i < manger.length; i++) {
                    manger_num.add(manger[i]);
                }
                if (data.get(CAR_STATUS) == 0) {
                    ClearRollBackRecord();//充值返回成功，删除M1冲正记录 2020-08-29
                    //充值处理信息成功,开始下一步
                    Log.e("R_M1_LOAD", "获取R_M1_LOAD成功，开始 R_M1_GET_CARDINFO");
                    //正常读卡操作5
//                    onDataReceiveListener.onDataReceiveCar(402, check_CARD(manger_num, manger.length));
                } else {
                    //充值处理失败
                    //onDataReceiveListener.onDataReceiveCar(4021, check_CARD(manger_num, manger.length));
                    //此处不报错，继续读卡走确认流程
                    Log.e("R_M1_LOAD", "M1->BA写卡报错!");
                    g_LOADWRITECARDCODE = data.get(CAR_STATUS);
                }
                if (M1_CARDINFO_TYPE == 1) R_M1_GET_CARDINFO(2);
                else R_M1_GET_CARDINFO(M1_CARDINFO_TYPE);


            }
            //CAR_R_M1_LOAD
        } else if (data.get(CAR_COMMAND) == CAR_GET_VERSION) {
//            CAR_PKI_STATE
            if (onDataReceiveListener != null) {
                manger_num = new ArrayList<>();
                for (int i = 0; i < manger.length; i++) {
                    manger_num.add(manger[i]);
                }
                if (data.get(CAR_STATUS) == 0) {
                    //获取程序版本成功
                    onDataReceiveListener.onDataReceiveCar(405, check_CARD(manger_num, manger.length));
                } else {
                    //充值处理失败
                    onDataReceiveListener.onDataReceiveCar(4051, check_CARD(manger_num, manger.length));
                }
            }
        }

//        CAR_GET_CPU_LOAD
        read_buffer.clear();
        car_manger_info = "";
        car_manger_info_1 = "";
    }

    public void RechargeSucessShow(int RechargeResult) {
        String strInfo = "";
        String str_time;

        String money = "";
        money = new BigDecimal("" + (g_BALANCE_LAST)).movePointLeft(2) + "元";
        onDataReceiveListener.onDataReceiveCar(1102, money);

        str_time = "";
        str_time = ChangetoTimeFormat(str_recharge_time);
        if (RechargeResult == 0) {
            //充值成功!
            strInfo = "充值成功!\n";
        } else {
            //充值疑是成功!
            strInfo = "充值疑是成功!\n";
            g_BALANCE_LAST = g_BALANCE_FRONT + g_FARE;
        }
        strInfo += "交易时间：" + str_time + "\n";
        strInfo += "系统流水号：" + Load() + "\n";
        strInfo += "逻辑卡号：" + str_g_LNO + "\n";
        strInfo += "充值前余额：" + new BigDecimal("" + g_BALANCE_FRONT).movePointLeft(2) + "元\n";
        if (RechargeResult == 0) {
            strInfo += "充值金额：" + new BigDecimal("" + g_FARE).movePointLeft(2) + "元\n";
            strInfo += "充值后余额：" + new BigDecimal("" + g_BALANCE_LAST).movePointLeft(2) + "元\n\n";
        } else {
            strInfo += "充值金额：(*)" + new BigDecimal("" + g_FARE).movePointLeft(2) + "元\n";
            strInfo += "充值后余额：(*)" + new BigDecimal("" + g_BALANCE_LAST).movePointLeft(2) + "元\n\n";
        }

        onDataReceiveListener.onDataReceiveCar(1121, strInfo);
    }

    //转化系统流水号
    public String Load() {
        String J_16 = str_g_SYSTEMSEQ_LOAD.substring(str_g_SYSTEMSEQ_LOAD.length() - 6, str_g_SYSTEMSEQ_LOAD.length());
        Log.e("TAG", "最后六位流水号=====" + J_16);
        //16位转化为10
        int j_10 = Integer.valueOf(J_16, 16);
        Log.e("TAG", "16转10=====" + j_10);
        String str_SYSTEMSEQ_LOAD = str_g_SYSTEMSEQ_LOAD.substring(0, str_g_SYSTEMSEQ_LOAD.length() - 6) + j_10;
        return str_SYSTEMSEQ_LOAD;
    }

    public void CpuRechargeSucessShow(int RechargeResult) {
        int g_dLimit = 0;
        g_dLimit = (int) g_BalanceLimit[4] * 100;//金额下限
        String strInfo = "";
        String str_time;

        String money = "";
        if (g_BALANCE_LAST >= g_dLimit)
            money = new BigDecimal("" + (g_BALANCE_LAST - g_dLimit)).movePointLeft(2) + "元";        //显示正数
        else
            money = "-" + new BigDecimal("" + (g_dLimit - g_BALANCE_LAST)).movePointLeft(2) + "元";   //显示负数
        onDataReceiveListener.onDataReceiveCar(1102, money);

        str_time = "";
        str_time = ChangetoTimeFormat(str_recharge_time);
        if (RechargeResult == 0) {
            //充值成功!
            strInfo = "充值成功!\n";
        } else {
            //充值疑是成功!
            strInfo = "充值疑是成功!\n";
            g_BALANCE_LAST = g_BALANCE_FRONT + g_FARE;
        }
        strInfo += "交易时间：" + str_time + "\n";
        strInfo += "系统流水号：" + Load() + "\n";
        strInfo += "逻辑卡号：" + str_g_LNO + "\n";
        if (g_BALANCE_FRONT >= g_dLimit)
            strInfo += "充值前余额：" + new BigDecimal("" + (g_BALANCE_FRONT - g_dLimit)).movePointLeft(2) + "元\n";
        else
            strInfo += "充值前余额：-" + new BigDecimal("" + (g_dLimit - g_BALANCE_FRONT)).movePointLeft(2) + "元\n";

        if (RechargeResult == 0) {
            strInfo += "充值金额：" + new BigDecimal("" + g_FARE).movePointLeft(2) + "元\n";
            if (g_BALANCE_LAST >= g_dLimit)
                strInfo += "充值后余额：" + new BigDecimal("" + (g_BALANCE_LAST - g_dLimit)).movePointLeft(2) + "元";
            else
                strInfo += "充值后余额：-" + new BigDecimal("" + (g_dLimit - g_BALANCE_LAST)).movePointLeft(2) + "元\n\n";
        } else {
            strInfo += "充值金额：(*)" + new BigDecimal("" + g_FARE).movePointLeft(2) + "元\n";
            if (g_BALANCE_LAST >= g_dLimit)
                strInfo += "充值后余额：(*)" + new BigDecimal("" + (g_BALANCE_LAST - g_dLimit)).movePointLeft(2) + "元";
            else
                strInfo += "充值后余额：(*)-" + new BigDecimal("" + (g_dLimit - g_BALANCE_LAST)).movePointLeft(2) + "元\n\n";
        }

        Log.e("recharge result:", strInfo);
        onDataReceiveListener.onDataReceiveCar(1121, strInfo);
        try {
            Thread.sleep(500);
        } catch (Exception e) {
            e.printStackTrace();
        }
        //onDataReceiveListener.onDataReceiveCar(1121, strInfo);
    }

    public void ConsumeSuccessShow() {
        String strInfo = "支付成功!\n";
        strInfo += "逻辑卡号：" + SerialDataUtils.Bytes2HexString(g_LNO, 0, 8) + "\n";
        strInfo += "支付金额：" + new BigDecimal("" + g_TrnasMoney).movePointLeft(2) + "元" + "\n";
        if (g_Consume_Balance >= g_DLimit) {
            strInfo += "卡片余额：" + new BigDecimal("" + (g_Consume_Balance - g_DLimit)).movePointLeft(2) + "元\n";
        } else {
            strInfo += "卡片余额：-" + new BigDecimal("" + (g_DLimit - g_Consume_Balance)).movePointLeft(2) + "元\n";
        }
        onDataReceiveListener.onDataReceiveCar(1121, strInfo);

    }

    public String ChangetoTimeFormat(String strTime) {
        String str_str;
        str_str = "";
        str_str = strTime.substring(0, 4) + "-" + strTime.substring(4, 6) + "-" + strTime.substring(6, 8) + " " + strTime.substring(8, 10) + ":" + strTime.substring(10, 12) + ":" + strTime.substring(12, 14);
        return str_str;
    }

    private static String test_info = "";

    //校验和…… 计算方法,获取16进制的信封数据
    public static byte[] check_DE1(List<String> num) {
        byte[] pack = new byte[136];
        test_info = "";
        for (int i = 0; i < 136; i++) {
            int num_info = Integer.valueOf(num.get(i), 16);
            pack[i] = (byte) num_info;
            test_info += (" " + num.get(i));
        }
        Log.e("test_sign_in_2", test_info + "");
        return pack;
    }

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

    //测试羊城通，签到
    //BA 11 10 02 88 01 20 00 11 10 00 00 00 00 00 00 00 00 11
    // 返回 BD 07 10 00 89 00 71 44 16
    //拿到管理卡号   89 00 71 44
    public void pay_test_card() {
        byte[] pack = new byte[19];
        pack[0] = (byte) 0xBA;                                   //包头BA 1字节
        pack[1] = 0x11;                                          //len 1字节
        pack[2] = 0x10;                                          //command 1字节
        pack[3] = (byte) 0x02;                                     //data 数据内容  GPK类型 0x02 :PKI  ，其他保留
        pack[4] = (byte) 0x88;                                    //GPK卡PIN 6    1
        pack[5] = (byte) 0x01;                                    //GPK卡PIN 6     2
        pack[6] = (byte) 0x20;                                    //GPK卡PIN 6     3
        pack[7] = (byte) 0x00;                                    //GPK卡PIN 6     4
        pack[8] = (byte) 0x11;                                    //GPK卡PIN 6     5
        pack[9] = (byte) 0x10;                                    //GPK卡PIN 6     6
        pack[10] = (byte) 0x00;                                    //INIT_KEY 8      1
        pack[11] = (byte) 0x00;                                    //INIT_KEY 8      2
        pack[12] = (byte) 0x00;                                    //INIT_KEY 8      3
        pack[13] = (byte) 0x00;                                    //INIT_KEY 8      4
        pack[14] = (byte) 0x00;                                    //INIT_KEY 8      5
        pack[15] = (byte) 0x00;                                    //INIT_KEY 8      6
        pack[16] = (byte) 0x00;                                    //INIT_KEY 8      7
        pack[17] = (byte) 0x00;                                    //INIT_KEY 8      8
        pack[18] = (byte) (pack[0] ^ pack[1] ^ pack[2] ^ pack[3] ^ pack[4] ^ pack[5] ^ pack[6] ^ pack[7] ^ pack[8] ^ pack[9] ^ pack[10] ^ pack[11] ^ pack[12] ^ pack[13] ^ pack[14] ^ pack[15] ^ pack[16] ^ pack[17]);
        //checksun 从header到data（包含头尾）所有字节的XOR 1字节 ^ pack[18] ^ pack[19]
        sendCmds(pack);
    }

    //消费签到 1
    public void pay_test_sign_in_1() {
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

//        time2.setText("Calendar获取当前日期" + year + "年" + month + "月" + day + "日" + hour + ":" + minute + ":" + second);
        Log.e("Calendar获取当前日期", Byte.parseByte((year + "").substring(2, 4)) + "");

        byte[] pack1 = new byte[37];
        pack1[0] = (byte) 0xBA;                                   //包头BA 1字节
        pack1[1] = (byte) 0x23;                                          //len 1字节
        pack1[2] = (byte) 0x7C;                                          //command 1字节
        pack1[3] = (byte) Constant.PortNumber1;                                    //终端编号        1
        pack1[4] = (byte) Constant.PortNumber2;                                    //终端编号        2
        pack1[5] = (byte) Constant.PortNumber3;                                    //终端编号        3
        pack1[6] = (byte) Constant.PortNumber4;                                    //终端编号        4
        pack1[7] = (byte) Constant.PortNumber5;                                    //终端编号        5
        pack1[8] = (byte) Constant.PortNumber6;                                    //终端编号        6
        pack1[9] = (byte) 0x14;                               //终端时间        YY
        pack1[10] = Byte.parseByte((year + "").substring(2, 4));                                    //终端时间        YY
        pack1[11] = (byte) month;                                    //终端时间        MM
        pack1[12] = (byte) day;                                    //终端时间        DD
        pack1[13] = (byte) hour;                                    //终端时间        HH
        pack1[14] = (byte) minute;                                    //终端时间        MI
        pack1[15] = (byte) second;                                     //终端时间        SS
        //商户编号   8位
        pack1[16] = (byte) Constant.MerchantNumber1;
        pack1[17] = (byte) Constant.MerchantNumber2;
        pack1[18] = (byte) Constant.MerchantNumber3;
        pack1[19] = (byte) Constant.MerchantNumber4;
        pack1[20] = (byte) Constant.MerchantNumber5;
        pack1[21] = (byte) Constant.MerchantNumber6;
        pack1[22] = (byte) Constant.MerchantNumber7;
        pack1[23] = (byte) Constant.MerchantNumber8;
        //密码 8位
        pack1[24] = (byte) Constant.PasswordNumber1;
        pack1[25] = (byte) Constant.PasswordNumber2;
        pack1[26] = (byte) Constant.PasswordNumber3;
        pack1[27] = (byte) Constant.PasswordNumber4;
        pack1[28] = (byte) Constant.PasswordNumber5;
        pack1[29] = (byte) Constant.PasswordNumber6;
        pack1[30] = (byte) Constant.PasswordNumber7;
        pack1[31] = (byte) Constant.PasswordNumber8;

        pack1[32] = (byte) 0x00;
        pack1[33] = (byte) 0x00;
        pack1[34] = (byte) 0x00;
        pack1[35] = (byte) 0x00;
        byte sum = 0;
        for (int i = 0; i < 35; i++) {//校验和
            sum ^= pack1[i];
        }
        pack1[36] = (byte) sum;
        sendCmds(pack1);
        //checksun 从header到data（包含头尾）所有字节的XOR 1字节 ^ pack[18] ^ pack[19]
    }

    //消费预处理
    //BA 15 B5 00 00 00 01 00 00 00 01 14 14 05 1B 0D 2C 01 0C 28
    public void pay_test_buy_1(int money) {
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

//        time2.setText("Calendar获取当前日期" + year + "年" + month + "月" + day + "日" + hour + ":" + minute + ":" + second);
        Log.e("Calendar获取当前日期", "  " + year + "年" + month + "月" + day + "日" + hour + ":" + minute + ":" + second);
        byte[] pack1 = new byte[21];
        pack1[0] = (byte) 0xBA;                                   //包头BA 1字节
        pack1[1] = (byte) 0x13;                                          //len 1字节
        pack1[2] = (byte) 0xB5;                                          //command 1字节
        byte[] money_hex = byte4ByInt(money);
        Log.e("money_hex", bytesToHexString(money_hex));
        pack1[3] = (byte) byte4ByInt(money)[0];
        pack1[4] = (byte) byte4ByInt(money)[1];
        pack1[5] = (byte) byte4ByInt(money)[2];
        pack1[6] = (byte) byte4ByInt(money)[3];                                    //原交易额  4位
        pack1[7] = (byte) byte4ByInt(money)[0];
        pack1[8] = (byte) byte4ByInt(money)[1];
        pack1[9] = (byte) byte4ByInt(money)[2];
        pack1[10] = (byte) byte4ByInt(money)[3];                                   //交易金额  4位
        pack1[11] = (byte) intTointByte(20);                                    //终端时间        YY
        pack1[12] = (byte) intTointByte(Integer.parseInt((year + "").substring(2, 4)));                                    //终端时间        YY
        pack1[13] = (byte) intTointByte(month);                                    //终端时间        MM
        pack1[14] = (byte) intTointByte(day);                                    //终端时间        DD
        pack1[15] = (byte) intTointByte(hour);                                    //终端时间        HH
        pack1[16] = (byte) intTointByte(minute);                                    //终端时间        MI
        pack1[17] = (byte) intTointByte(second);                                     //终端时间        SS

//        pack1[11] = 0x20;                                    //终端时间        YY
//        pack1[12] = 0x20;                                    //终端时间        YY
//        pack1[13] = 0x06;                                    //终端时间        MM
//        pack1[14] = 0x11;                                    //终端时间        DD
//        pack1[15] = 0x09;                                    //终端时间        HH
//        pack1[16] = 0x34;                                    //终端时间        MI
        pack1[17] = 0x01;
        //卡有效期使用
        pack1[18] = (byte) 0x00;
        //卡离线有效期，  可以设置卡的有效期
        pack1[19] = (byte) 0x00;
        byte sum = 0;
        for (int i = 0; i < 21; i++) {//校验和
            sum ^= pack1[i];
        }
        pack1[20] = (byte) sum;
        sendCmds(pack1);
        //checksun 从header到data（包含头尾）所有字节的XOR 1字节 ^ pack[18] ^ pack[19]
    }

    //消费确认，开始 扣款
    public void pay_test_buy_2() {
        byte[] pack = new byte[4];
        pack[0] = (byte) 0xBA;                                   //包头BA 1字节
        pack[1] = (byte) 0x02;                                          //len 1字节
        pack[2] = (byte) 0xB6;                                          //command 1字节
        pack[3] = (byte) (pack[0] ^ pack[1] ^ pack[2]);
        //checksun 从header到data（包含头尾）所有字节的XOR 1字节 ^ pack[18] ^ pack[19]
        com_send_cmd = (byte) 0xB6;
        sendCmds(pack);
    }

    //R_PSAM_PURCHASE_REPROCESS(未完整交易处理) 0xB7
    public void yct_purchase_reprocess(byte[] data) {
        byte[] pack = new byte[30];
        pack[0] = (byte) 0xBA;
        pack[1] = (byte) 0x1C;//长度28
        pack[2] = (byte) 0xB7;
        System.arraycopy(data, 0, pack, 3, 26);//参数26字节
        byte sum = 0;
        for (int i = 0; i < 29; i++) {//校验和
            sum ^= pack[i];
        }
        pack[29] = (byte) sum;
        sendCmds(pack);
    }

    //黑名单卡，锁卡
    public void Set_Card_Namelist() {
        byte[] pack = new byte[4];
        pack[0] = (byte) 0xBA;                                   //包头BA 1字节
        pack[1] = (byte) 0x02;                                          //len 1字节
        pack[2] = (byte) 0xB8;                                          //command 1字节
        pack[3] = (byte) (pack[0] ^ pack[1] ^ pack[2]);
        //checksun 从header到data（包含头尾）所有字节的XOR 1字节 ^ pack[18] ^ pack[19]
        sendCmds(pack);
    }

    //消费  签到步骤2
    /*
     */
    public void pay_test_sign_in_2(byte[] num) {
        byte[] pack = new byte[140];
        pack[0] = (byte) 0xBA;                                   //包头BA 1字节
        pack[1] = (byte) 0x8a;                                          //len 1字节
        pack[2] = (byte) 0xC9;                                          //command 1字节0
        int index = 3;
        Log.e("test_sign_in_2", num.length + "");
        for (int i = 0; i < 136; i++) {
//            int num_info = Integer.valueOf(num.get(i),16);
//            pack[index + i] = (byte)num_info;
            pack[index + i] = num[i];
        }
//        for (int i = 1; i < 3; i++) {//校验和
//            pack[139] ^= pack[i];
//        }
        byte sum = 0;
        for (int i = 0; i < 139; i++) {//校验和
            sum ^= pack[i];
//            pack[i] ^= pack[i+1];
//            Log.e("pack_info_sum",sum+"  ==  "+i);
        }
//        pack[139] = (byte)sum;
//        int num_info_2 = Integer.valueOf("-103",16);
//        BigInteger bi = new BigInteger("-103", 16);
//        Integer.toHexString(((-103) & 0x000000FF) | 0xFFFFFF00).substring(6);
//        Log.e("pack_info_sum",bi.intValue()+"  ==  "+(byte)num_info_2+" == "+Integer.toHexString(((-103) & 0x000000FF) | 0xFFFFFF00).substring(6));
        //checksun 从header到data（包含头尾）所有字节的XOR 1字节 ^ pack[18] ^ pack[19]
//        Log.e("pack_info",pack.length+" == "+(pack[139] ^= pack[0]));
        pack[139] = (byte) (pack[0] ^ pack[1] ^ pack[2] ^ pack[3] ^ pack[4] ^ pack[5] ^ pack[6] ^ pack[7] ^ pack[8] ^ pack[9] ^ pack[10] ^ pack[11] ^ pack[12] ^ pack[13] ^ pack[14] ^ pack[15] ^ pack[16] ^ pack[17] ^ pack[18] ^ pack[19] ^ pack[20] ^ pack[21] ^ pack[22] ^ pack[23] ^ pack[24] ^ pack[25] ^ pack[26] ^ pack[27] ^ pack[28] ^ pack[29] ^ pack[30] ^ pack[31] ^ pack[32] ^ pack[33] ^ pack[34] ^ pack[35] ^
                pack[36] ^ pack[37] ^ pack[38] ^ pack[39] ^ pack[40] ^ pack[41] ^ pack[42] ^ pack[43] ^ pack[44] ^ pack[45] ^ pack[46] ^ pack[47] ^ pack[48] ^ pack[49] ^ pack[50] ^ pack[51] ^ pack[52] ^ pack[53] ^ pack[54] ^ pack[55] ^ pack[56] ^ pack[57] ^ pack[58] ^ pack[59] ^ pack[60] ^ pack[61] ^ pack[62] ^ pack[63] ^ pack[64] ^ pack[65] ^ pack[66] ^ pack[67] ^ pack[68] ^ pack[69] ^ pack[70] ^ pack[71] ^
                pack[72] ^ pack[73] ^ pack[74] ^ pack[75] ^ pack[76] ^ pack[77] ^ pack[78] ^ pack[79] ^ pack[80] ^ pack[81] ^ pack[82] ^ pack[83] ^ pack[84] ^ pack[85] ^ pack[86] ^ pack[87] ^ pack[88] ^ pack[89] ^ pack[90] ^ pack[91] ^ pack[92] ^ pack[93] ^ pack[94] ^ pack[95] ^ pack[96] ^ pack[97] ^ pack[98] ^ pack[99] ^ pack[100] ^ pack[101] ^ pack[102] ^ pack[103] ^ pack[104] ^ pack[105] ^ pack[106] ^ pack[107] ^
                pack[108] ^ pack[109] ^ pack[110] ^ pack[111] ^ pack[112] ^ pack[113] ^ pack[114] ^ pack[115] ^ pack[116] ^ pack[117] ^ pack[118] ^ pack[119] ^ pack[120] ^ pack[121] ^ pack[122] ^ pack[123] ^ pack[124] ^ pack[125] ^ pack[126] ^ pack[127] ^ pack[128] ^ pack[129] ^ pack[130] ^ pack[131] ^ pack[132] ^ pack[133] ^ pack[134] ^ pack[135] ^ pack[136] ^ pack[137] ^ pack[138]);
        Log.e("pack_info_sum", sum + "  ==  " + pack[139]);
        sendCmds(pack);
    }

    //测试羊城通
    public void pay_test_card4() {
        byte[] pack = new byte[19];
        pack[0] = (byte) 0xBA;                                   //包头BA 1字节
        pack[1] = 0x17;                                          //len 1字节
        pack[2] = 0x10;                                          //command 1字节
        pack[3] = (byte) 0x02;                                     //data 数据内容  GPK类型 0x02 :PKI  ，其他保留
        pack[4] = (byte) 0x01;                                    //GPK卡PIN 6    1
        pack[5] = (byte) 0x00;                                    //GPK卡PIN 6     2
        pack[6] = (byte) 0x99;                                    //GPK卡PIN 6     3
        pack[7] = (byte) 0x99;                                    //GPK卡PIN 6     4
        pack[8] = (byte) 0x32;                                    //GPK卡PIN 6     5
        pack[9] = (byte) 0x29;                                    //GPK卡PIN 6     6
        pack[10] = (byte) 0x31;                                    //INIT_KEY 8      1
        pack[11] = (byte) 0x32;                                    //INIT_KEY 8      2
        pack[12] = (byte) 0x33;                                    //INIT_KEY 8      3
        pack[13] = (byte) 0x34;                                    //INIT_KEY 8      4
        pack[14] = (byte) 0x35;                                    //INIT_KEY 8      5
        pack[15] = (byte) 0x36;                                    //INIT_KEY 8      6
        pack[16] = (byte) 0x37;                                    //INIT_KEY 8      7
        pack[17] = (byte) 0x38;                                    //INIT_KEY 8      8
        pack[18] = (byte) (pack[0] ^ pack[1] ^ pack[2] ^ pack[3] ^ pack[4] ^ pack[5] ^ pack[6] ^ pack[7] ^ pack[8] ^ pack[9] ^ pack[10] ^ pack[11] ^ pack[12] ^ pack[13] ^ pack[14] ^ pack[15] ^ pack[16] ^ pack[17]);
        //checksun 从header到data（包含头尾）所有字节的XOR 1字节 ^ pack[18] ^ pack[19]
        sendCmds(pack);
    }

    //测试羊城通 获取监控状态
    public void pay_card_state() {
        byte[] pack = new byte[4];
        pack[0] = (byte) 0xBA;                                   //包头BA 1字节
        pack[1] = 0x02;                                          //len 1字节
        pack[2] = 0x65;                                          //command 1字节
        pack[3] = (byte) (pack[0] ^ pack[1] ^ pack[2]);
        //checksun 从header到data（包含头尾）所有字节的XOR 1字节 ^ pack[18] ^ pack[19]
        sendCmds(pack);
    }

    //测试羊城通 寻卡
    public void pay_test_card_search() {
        byte[] pack = new byte[5];
        pack[0] = (byte) 0xBA;                                   //包头BA 1字节
        pack[1] = 0x03;                                          //len 1字节
        pack[2] = 0x45;                                          //command 1字节
        pack[3] = (byte) 0x52;                                     //data 数据内容  Para 0x52
        pack[4] = (byte) (pack[0] ^ pack[1] ^ pack[2] ^ pack[3]);
        //checksun 从header到data（包含头尾）所有字节的XOR 1字节 ^ pack[18] ^ pack[19]
        Log.e("cccnnnn", setParamCRC(pack)[0] + "    " + setParamCRC(pack)[1]);
//        sendCmds(pack);
    }

    //测试羊城通 读卡
    public void pay_test_card_read() {
        byte[] pack = new byte[4];
        pack[0] = (byte) 0xBA;                                   //包头BA 1字节
        pack[1] = 0x02;                                          //len 1字节
        pack[2] = (byte) 0x93;                                          //command 1字节
        pack[3] = (byte) (pack[0] ^ pack[1] ^ pack[2]);
        //checksun 从header到data（包含头尾）所有字节的XOR 1字节 ^ pack[18] ^ pack[19]
        sendCmds(pack);
    }

    /**
     * 为Byte数组添加两位CRC校验
     *
     * @param buf
     * @return
     */
    public static byte[] setParamCRC(byte[] buf) {
        int MASK = 0x0001, CRCSEED = 0x0810;
        int remain = 0;

        byte val;
        for (int i = 0; i < buf.length; i++) {
            val = buf[i];
            for (int j = 0; j < 8; j++) {
                if (((val ^ remain) & MASK) != 0) {
                    remain ^= CRCSEED;
                    remain >>= 1;
                    remain |= 0x8000;
                } else {
                    remain >>= 1;
                }
                val >>= 1;
            }
        }

        byte[] crcByte = new byte[2];
        crcByte[0] = (byte) ((remain >> 8) & 0xff);
        crcByte[1] = (byte) (remain & 0xff);

        // 将新生成的byte数组添加到原数据结尾并返回
        return crcByte;
    }

    public static byte[] byte4ByInt(int money) {
        byte[] bytes5 = new byte[4];
        bytes5[3] = (byte) (money & 0xFF);
        bytes5[2] = (byte) (money >> 8 & 0xFF);
        bytes5[1] = (byte) (money >> 16 & 0xFF);
        bytes5[0] = (byte) (money >> 24 & 0xFF);
        return bytes5;
    }

    //==================================================================================
    /*
    充值相关
     */
    //测试羊城通，签到
    //BA 11 10 02 88 01 20 00 11 10 00 00 00 00 00 00 00 00 11
    // 返回 BD 07 10 00 89 00 71 44 16
    //拿到管理卡号   89 00 71 44
    public void test_card() {
        byte[] pack = new byte[19];
        pack[0] = (byte) 0xBA;                                   //包头BA 1字节
        pack[1] = 0x11;                                          //len 1字节
        pack[2] = 0x10;                                          //command 1字节
        pack[3] = (byte) 0x02;                                     //data 数据内容  GPK类型 0x02 :PKI  ，其他保留
        pack[4] = (byte) 0x88;                                    //GPK卡PIN 6    1
        pack[5] = (byte) 0x01;                                    //GPK卡PIN 6     2
        pack[6] = (byte) 0x20;                                    //GPK卡PIN 6     3
        pack[7] = (byte) 0x00;                                    //GPK卡PIN 6     4
        pack[8] = (byte) 0x11;                                    //GPK卡PIN 6     5
        pack[9] = (byte) 0x11;                                    //GPK卡PIN 6     6
        pack[10] = (byte) 0x00;                                    //INIT_KEY 8      1
        pack[11] = (byte) 0x00;                                    //INIT_KEY 8      2
        pack[12] = (byte) 0x00;                                    //INIT_KEY 8      3
        pack[13] = (byte) 0x00;                                    //INIT_KEY 8      4
        pack[14] = (byte) 0x00;                                    //INIT_KEY 8      5
        pack[15] = (byte) 0x00;                                    //INIT_KEY 8      6
        pack[16] = (byte) 0x00;                                    //INIT_KEY 8      7
        pack[17] = (byte) 0x00;                                    //INIT_KEY 8      8
        pack[18] = (byte) (pack[0] ^ pack[1] ^ pack[2] ^ pack[3] ^ pack[4] ^ pack[5] ^ pack[6] ^ pack[7] ^ pack[8] ^ pack[9] ^ pack[10] ^ pack[11] ^ pack[12] ^ pack[13] ^ pack[14] ^ pack[15] ^ pack[16] ^ pack[17]);
        //checksun 从header到data（包含头尾）所有字节的XOR 1字节 ^ pack[18] ^ pack[19]
        sendCmds(pack);
    }

    //测试羊城通
    public void test_sign_in_1() {
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

//        time2.setText("Calendar获取当前日期" + year + "年" + month + "月" + day + "日" + hour + ":" + minute + ":" + second);
        Log.e("Calendar获取当前日期", Byte.parseByte((year + "").substring(2, 4)) + "");
        byte[] pack = new byte[21];
        pack[0] = (byte) 0xBA;                                   //包头BA 1字节
        pack[1] = (byte) 0x13;                                          //len 1字节
        pack[2] = (byte) 0xC8;                                          //command 1字节
        pack[3] = PKI_ID[0];//Byte.parseByte(manger_num.get(0));                                     //data 数据内容  GPK类型 0x02 :PKI  ，其他保留，管理卡号    1
        pack[4] = PKI_ID[1];//Byte.parseByte(manger_num.get(1));                                    //管理卡号    2
        pack[5] = PKI_ID[2];//Byte.parseByte(manger_num.get(2));                                    //管理卡号      3
        pack[6] = PKI_ID[3];//Byte.parseByte(manger_num.get(3));                                    //管理卡号    4
        pack[7] = (byte) Constant.PortNumber1;                                    //终端编号        1
        pack[8] = (byte) Constant.PortNumber2;                                    //终端编号        2
        pack[9] = (byte) Constant.PortNumber3;                                    //终端编号        3
        pack[10] = (byte) Constant.PortNumber4;                                    //终端编号        4
        pack[11] = (byte) Constant.PortNumber5;                                    //终端编号        5
        pack[12] = (byte) Constant.PortNumber6;                                    //终端编号        6
        pack[13] = (byte) 0x14;                                    //终端时间        YY
        pack[14] = Byte.parseByte((year + "").substring(2, 4));                                    //终端时间        YY
        pack[15] = (byte) month;                                    //终端时间        MM
        pack[16] = (byte) day;                                    //终端时间        DD
        pack[17] = (byte) hour;                                    //终端时间        HH
        pack[18] = (byte) minute;                                    //终端时间        MI
        pack[19] = (byte) second;                                    //终端时间        SS
        pack[20] = (byte) (pack[0] ^ pack[1] ^ pack[2] ^ pack[3] ^ pack[4] ^ pack[5] ^ pack[6] ^ pack[7] ^ pack[8] ^ pack[9] ^ pack[10] ^ pack[11] ^ pack[12] ^ pack[13] ^ pack[14] ^ pack[15] ^ pack[16] ^ pack[17] ^ pack[18] ^ pack[19]);

        sendCmds(pack);
        //checksun 从header到data（包含头尾）所有字节的XOR 1字节 ^ pack[18] ^ pack[19]
    }

    //测试羊城通  签到步骤2
    //BD 8B C8 00
    // FE 01 00 01 83 80 00 80 49 15 10 CA F2 4F B6 CA 49 15 10 CA F2 4F B6 CA 87
    // E9 7E 13 FF 05 22 FB B7 31 5A AC 92 AD D8 CD F1 D0 24 57 FB 97 04 E2 49 15 10 CA F2 4F
    // B6 CA 49 15 10 CA F2 4F B6 CA 49 15 10 CA F2 4F B6 CA 49 15 10 CA F2 4F B6 CA 49 15 10
    // CA F2 4F B6 CA 49 15 10 CA F2 4F B6 CA 49 15 10 CA F2 4F B6 CA 49 15 10 CA F2 4F B6 CA
    // 49 15 10 CA F2 4F B6 CA 49 15 10 CA F2 4F B6 CA 49 15 10 CA F2 4F B6 CA
    // 66
    public void test_sign_in_2(byte[] num) {
        byte[] pack = new byte[140];
        pack[0] = (byte) 0xBA;                                   //包头BA 1字节
        pack[1] = (byte) 0x8a;                                          //len 1字节
        pack[2] = (byte) 0xC9;                                          //command 1字节0
        int index = 3;
        Log.e("test_sign_in_2", num.length + "");
        for (int i = 0; i < 136; i++) {
            pack[index + i] = num[i];
        }
        byte sum = 0;
        for (int i = 0; i < 139; i++) {//校验和
            sum ^= pack[i];
        }
        pack[139] = (byte) sum;
        sendCmds(pack);
    }

    //测试羊城通 获取监控状态
    public void card_state() {
        byte[] pack = new byte[4];
        pack[0] = (byte) 0xBA;                                   //包头BA 1字节
        pack[1] = 0x02;                                          //len 1字节
        pack[2] = 0x65;                                          //command 1字节
        pack[3] = (byte) (pack[0] ^ pack[1] ^ pack[2]);
        //checksun 从header到data（包含头尾）所有字节的XOR 1字节 ^ pack[18] ^ pack[19]
        sendCmds(pack);
    }

    //测试羊城通 寻卡
    public void test_card_search() {
        byte[] pack = new byte[5];
        pack[0] = (byte) 0xBA;                                   //包头BA 1字节
        pack[1] = 0x03;                                          //len 1字节
        pack[2] = 0x45;                                          //command 1字节
        pack[3] = (byte) 0x52;                                     //data 数据内容  Para 0x52
        pack[4] = (byte) (pack[0] ^ pack[1] ^ pack[2] ^ pack[3]);
        //checksun 从header到data（包含头尾）所有字节的XOR 1字节 ^ pack[18] ^ pack[19]
        sendCmds(pack);
    }

    //充值  查询卡物理信息 R_PUB_QRY_PHYSICS_INFO
    public void test_card_read() {
        state_m1 = 0;
        byte[] pack = new byte[4];
        pack[0] = (byte) 0xBA;                                   //包头BA 1字节
        pack[1] = 0x02;                                          //len 1字节
        pack[2] = (byte) 0x93;                                          //command 1字节
        pack[3] = (byte) (pack[0] ^ pack[1] ^ pack[2]);
        //checksun 从header到data（包含头尾）所有字节的XOR 1字节 ^ pack[18] ^ pack[19]
        sendCmds(pack);
    }

    private static int state_m1 = 0;

    //充值  查询卡物理信息 R_PUB_QRY_PHYSICS_INFO ,此查卡 用于充值流程开始
    public void R_PUB_QRY_PHYSICS_INFO(int state) {
        state_m1 = state;
        byte[] pack = new byte[4];
        pack[0] = (byte) 0xBA;                                   //包头BA 1字节
        pack[1] = 0x02;                                          //len 1字节
        pack[2] = (byte) 0x93;                                          //command 1字节
        pack[3] = (byte) (pack[0] ^ pack[1] ^ pack[2]);
        //checksun 从header到data（包含头尾）所有字节的XOR 1字节 ^ pack[18] ^ pack[19]
        sendCmds(pack);
    }

    //充值  查询卡物理信息
    public void cz_card_read_wuli() {
        byte[] pack = new byte[4];
        pack[0] = (byte) 0xBA;                                   //包头BA 1字节
        pack[1] = 0x02;                                          //len 1字节
        pack[2] = (byte) 0x93;                                          //command 1字节
        pack[3] = (byte) (pack[0] ^ pack[1] ^ pack[2]);
        //checksun 从header到data（包含头尾）所有字节的XOR 1字节 ^ pack[18] ^ pack[19]
        sendCmds(pack);
    }

    //查询卡应用信息 用于交易类型查询
    public void search_app_card_info() {
        byte[] pack = new byte[4];
        pack[0] = (byte) 0xBA;                                   //包头BA 1字节
        pack[1] = 0x02;                                          //len 1字节
        pack[2] = (byte) 0x94;                                          //command 1字节
        pack[3] = (byte) (pack[0] ^ pack[1] ^ pack[2]);
        //checksun 从header到data（包含头尾）所有字节的XOR 1字节 ^ pack[18] ^ pack[19]
        sendCmds(pack);
    }

    //设置卡信息 R_PUB_SET_READCARDINFO
    //设置卡信息，两种卡都需要
    private static String card_type = "cpu";

    public void R_PUB_SET_READCARDINFO(byte[] num, String type, int index) {
//        CZ_CARD_NUM
        card_type = type;
        byte[] pack = new byte[60];
        pack[0] = (byte) 0xBA;                                   //包头BA 1字节
        pack[1] = (byte) 0x3A;                                          //len 1字节
        pack[2] = (byte) 0x95;                                          //command 1字节
        //逻辑卡号 8位
        for (int i = 0; i < 8; i++) {
            pack[3 + i] = CZ_CARD_NUM[i + 12];
        }
        //查询信息48位
        for (int i = 0; i < 48; i++) {
            pack[11 + i] = num[i + index];
        }
        byte sum = 0;
        for (int i = 0; i < 59; i++) {//校验和
            sum ^= pack[i];
        }
        pack[59] = (byte) sum;
        //checksun 从header到data（包含头尾）所有字节的XOR 1字节 ^ pack[18] ^ pack[19]
        sendCmds(pack);
    }

    //R_CPU_GET_CARD  CPU卡取卡信息
    public void R_CPU_GET_CARDINFO() {
        byte[] pack = new byte[4];
        pack[0] = (byte) 0xBA;                                   //包头BA 1字节
        pack[1] = 0x02;                                          //len 1字节
        pack[2] = (byte) 0x79;                                          //command 1字节
        pack[3] = (byte) (pack[0] ^ pack[1] ^ pack[2]);
        //checksun 从header到data（包含头尾）所有字节的XOR 1字节 ^ pack[18] ^ pack[19]
        sendCmds(pack);
    }

    public void CPU_GET_CARDINFO(int sta) {
        state_read = sta;
        byte[] pack = new byte[4];
        pack[0] = (byte) 0xBA;                                   //包头BA 1字节
        pack[1] = 0x02;                                          //len 1字节
        pack[2] = (byte) 0x79;                                          //command 1字节
        pack[3] = (byte) (pack[0] ^ pack[1] ^ pack[2]);
        //checksun 从header到data（包含头尾）所有字节的XOR 1字节 ^ pack[18] ^ pack[19]
        sendCmds(pack);
    }

    //R_CPU_LOAD_INIT  CPU卡钱包圈存初始化
    public void R_CPU_LOAD_INIT(byte[] num) {
        byte[] pack = new byte[68];
        pack[0] = (byte) 0xBA;                                   //包头BA 1字节
        pack[1] = 0x42;                                          //len 1字节
        pack[2] = (byte) 0x7A;                                          //command 1字节

        for (int i = 0; i < 64; i++) {
            pack[3 + i] = num[i + 12];
        }
        byte sum = 0;
        for (int i = 0; i < 67; i++) {//校验和
            sum ^= pack[i];
        }
        pack[67] = (byte) sum;
        //checksun 从header到data（包含头尾）所有字节的XOR 1字节 ^ pack[18] ^ pack[19]
        sendCmds(pack);
    }

    //R_CPU_LOAD  CPU卡钱包充值写卡操作
    public void R_CPU_LOAD(byte[] num) {
        byte[] pack = new byte[36];
        pack[0] = (byte) 0xBA;                                   //包头BA 1字节
        pack[1] = 0x22;                                          //len 1字节
        pack[2] = (byte) 0x7B;                                          //command 1字节

        //交易信息32
        for (int i = 0; i < 32; i++) {
            pack[3 + i] = num[i + 12];
        }
        byte sum = 0;
        for (int i = 0; i < 35; i++) {//校验和
            sum ^= pack[i];
        }
        pack[35] = (byte) sum;
        //checksun 从header到data（包含头尾）所有字节的XOR 1字节 ^ pack[18] ^ pack[19]
        sendCmds(pack);
    }

    //R_M1_GET_CARDINFO M1卡取卡信息
    //M1_CARDINFO_TYPE   1为第一次获取卡数据  2为第二次获取数据 为了充值提交
    private static int M1_CARDINFO_TYPE = 1;

    public void R_M1_GET_CARDINFO(int state) {
        M1_CARDINFO_TYPE = state;
        byte[] pack = new byte[4];
        pack[0] = (byte) 0xBA;                                   //包头BA 1字节
        pack[1] = 0x02;                                          //len 1字节
        pack[2] = (byte) 0xB9;                                          //command 1字节
        pack[3] = (byte) (pack[0] ^ pack[1] ^ pack[2]);
        //checksun 从header到data（包含头尾）所有字节的XOR 1字节 ^ pack[18] ^ pack[19]
        com_send_cmd = (byte) 0xB9;
        sendCmds(pack);
    }

    //R_M1_LOAD  M1卡钱包充值写卡操作
    public void R_M1_LOAD(byte[] num) {
        byte[] pack = new byte[35];
        pack[0] = (byte) 0xBA;                                   //包头BA 1字节
        pack[1] = 0x21;                                          //len 1字节
        pack[2] = (byte) 0xBA;                                          //command 1字节

//        pack[3] = (byte) 0x14;                                    //终端时间        YY
//        pack[4] = Byte.parseByte((year + "").substring(2, 4));                                    //终端时间        YY
//        pack[5] = (byte) month;                                    //终端时间        MM
//        pack[6] = (byte) day;                                    //终端时间        DD
//        pack[7] = (byte) hour;                                    //终端时间        HH
//        pack[8] = (byte) minute;                                    //终端时间        MI
//        pack[9] = (byte) second;                                    //终端时间        SS

        //交易日期时间
        for (int i = 0; i < 7; i++) {
            pack[3 + i] = num[i + 36];
        }
        //充值信息24
        for (int i = 0; i < 24; i++) {
            pack[10 + i] = num[i + 12];
        }
        byte sum = 0;
        for (int i = 0; i < 34; i++) {//校验和
            sum ^= pack[i];
        }
        pack[34] = (byte) sum;
        //checksun 从header到data（包含头尾）所有字节的XOR 1字节 ^ pack[18] ^ pack[19]
        sendCmds(pack);
    }

    private static int PKI_STATE = 0;
    private static String TYPE_STATE = "cz";

    //R_PUB_GETVERSION 获取读卡器版本
    public void R_PUB_GETVERSION() {
        byte[] pack = new byte[4];
        pack[0] = (byte) 0xBA;                                   //包头BA 1字节
        pack[1] = 0x02;                                          //len 1字节
        pack[2] = (byte) 0x60;                                          //command 1字节
        pack[3] = (byte) (pack[0] ^ pack[1] ^ pack[2]);
        //checksun 从header到data（包含头尾）所有字节的XOR 1字节 ^ pack[18] ^ pack[19]
        sendCmds(pack);
    }

    //R_PUB_GET_PKISTATE 获取监控状态
    public void R_PUB_GET_PKISTATE(String info_type) {
        TYPE_STATE = info_type;
        byte[] pack = new byte[4];
        pack[0] = (byte) 0xBA;                                   //包头BA 1字节
        pack[1] = 0x02;                                          //len 1字节
        pack[2] = (byte) 0x65;                                          //command 1字节
        pack[3] = (byte) (pack[0] ^ pack[1] ^ pack[2]);
        //checksun 从header到data（包含头尾）所有字节的XOR 1字节 ^ pack[18] ^ pack[19]
        sendCmds(pack);
    }

    public static int intTointByte(int args) {
        int hex_all = 0;
        if (args >= 10) {
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

    //保存M1冲正记录  格式：AA55+len+data
    public static void SaveRollBackRecord(String str) {
        String sstr;
        int str_len = 0;
        int data_len = 0;
        byte[] buff = new byte[512];
        sstr = "AA55" + str;
        buff = SerialDataUtils.HexString2Bytes(str);
        //buff[0]~buff[1] 长度
        str_len = str.length() / 2;
        data_len = SerialDataUtils.toBdInt(buff, 0, 2);
        if (str_len != data_len + 2) {
            Log.e("err_data", "冲正数据错误,不写冲正记录!");
        } else {
            TextLog.writeYctFile(sstr, Constant.Companion.getFilePath(), Constant.yctrbfileName);
        }

    }

    //读取M1冲正数据
    public static String ReadRollBackRecord() {
        String str = null;
        int i = 0;
        int lens = 0;
        byte[] buff = new byte[512];
        str = TextLog.readYctFile(Constant.Companion.getFilePath(), Constant.yctrbfileName);
        if (null != str) {
            buff = SerialDataUtils.HexString2Bytes(str);
            if ((buff[0] == (byte) 0xAA) && (buff[1] == (byte) 0x55)) {
                String sstr = str.substring(8);//去掉AA55+长度
                lens = SerialDataUtils.toBdInt(buff, 2, 2);
                if (lens != sstr.length() / 2) {
                    return null;
                }
                return sstr;
            }
        }
        return null;
    }

    //删除M1冲正记录
    public static void ClearRollBackRecord() {
        String sstr;
        sstr = "00FF";
        TextLog.writeYctFile(sstr, Constant.Companion.getFilePath(), Constant.yctrbfileName);
    }

    //保存CPU提交数据  格式：AA55+len+data
    public static void SaveCpuSubmitData(String str) {
        String sstr;
        int str_len = 0;
        int data_len = 0;
        byte[] buff = new byte[512];
        sstr = "AA55" + str;
        buff = SerialDataUtils.HexString2Bytes(str);
        //buff[0]~buff[1] 长度
        str_len = str.length() / 2;
        data_len = SerialDataUtils.toBdInt(buff, 0, 2);
        if (str_len != data_len + 2) {
            Log.e("err_data", "CPU提交数据错误,CPU提交数据记录!");
        } else {
            TextLog.writeYctFile(sstr, Constant.Companion.getFilePath(), Constant.yctsubmitfileName);
        }
    }

    //读取CPU提交数据
    public static String ReadCpuSubmitData() {
        String str = null;
        int i = 0;
        int lens = 0;
        byte[] buff = new byte[512];
        str = TextLog.readYctFile(Constant.Companion.getFilePath(), Constant.yctsubmitfileName);
        if (null != str) {
            buff = SerialDataUtils.HexString2Bytes(str);
            if ((buff[0] == (byte) 0xAA) && (buff[1] == (byte) 0x55)) {
                String sstr = str.substring(8);//去掉AA55+长度
                lens = SerialDataUtils.toBdInt(buff, 2, 2);
                if (lens != sstr.length() / 2) {
                    return null;
                }
                return sstr;
            }
        }
        return null;
    }

    //删除CPU提交数据
    public static void ClearCpuSubmitData() {
        String sstr;
        sstr = "00FF";
        TextLog.writeYctFile(sstr, Constant.Companion.getFilePath(), Constant.yctsubmitfileName);
    }

    //保存消费未完整记录 格式：AA55+cardtype+data  M1 data: cardtype01+data(73byte) CPU data: cardtype02+data(87byte)
    public static void SaveNotCompleteRecord(String str) {
        String sstr;
        sstr = "AA55" + str;
        TextLog.writeYctFile(sstr, Constant.Companion.getFilePath(), Constant.yctprefileName);
    }

    //读消费未完整记录
    public static String ReadNotCompleteRecord() {
        String str = null;
        int i = 0;
        byte[] buff = new byte[512];
        str = TextLog.readYctFile(Constant.Companion.getFilePath(), Constant.yctprefileName);
        if (null != str) {
            buff = SerialDataUtils.HexString2Bytes(str);
            if ((buff[0] == (byte) 0xAA) && (buff[1] == (byte) 0x55)) {
                String sstr = str.substring(4);//去掉AA55
                return sstr;
            }
        }
        return null;
    }

    //删除消费未完整记录
    public static void ClearNotCompleteRecord() {
        String sstr;
        sstr = "00FF";
        TextLog.writeYctFile(sstr, Constant.Companion.getFilePath(), Constant.yctprefileName);
    }

    public void yct_pay_test_buy(int money) {
        int ista = 0;
        int ret = 0;

        ulTransMoney = money;//交易金额备份
        //判断是否有未完整交易记录
        String str = null;
        str = ReadNotCompleteRecord();
        if (null == str) {
            //没有未完整交易记录
            Log.e("yct consume info", "开始羊城通消费..");
            bComsume = 0;
            pay_test_buy_1(money); //做消费预处理0xB5
        } else {
            byte[] ssbuff = new byte[100];
            ssbuff = SerialDataUtils.HexString2Bytes(str);
            //比较钱包类别
            if ((byte) bWalletType == (byte) ssbuff[0]) ista = 1;
            else ista = 0;
            //比较物理卡号
            ret = SerialDataUtils.ByteMemCmp(ssbuff, 1, g_FNO, 0, 8);
            if (ret == 0) ista = 1;
            else ista = 0;
            //比较逻辑卡号
            ret = SerialDataUtils.ByteMemCmp(ssbuff, 9, g_LNO, 0, 8);
            if (ret == 0) ista = 1;
            else ista = 0;

            if (ista == 0) {
                //未完整交易记录不是这张卡的，不用做未完整
                bComsume = 0;
                pay_test_buy_1(money); //做消费预处理0xB5
            } else {
                SerialDataUtils.SetByteArray(buff_data, (byte) 0x00, buff_data.length);
                if (bWalletType == 0x01) {
                    System.arraycopy(ssbuff, 5, buff_data, 0, 8);//逻辑卡号8
                    System.arraycopy(ssbuff, 13, buff_data, 8, 4);//物理卡号4 后4个字节0
                    System.arraycopy(ssbuff, 52, buff_data, 16, 2);//票卡交易计数器2
                    System.arraycopy(ssbuff, 39, buff_data, 18, 4);//交易金额4
                    System.arraycopy(ssbuff, 43, buff_data, 22, 4);//本次余额4
                } else {
                    System.arraycopy(ssbuff, 18, buff_data, 0, 8);//逻辑卡号8
                    System.arraycopy(ssbuff, 26, buff_data, 8, 8);//物理卡号8
                    System.arraycopy(ssbuff, 50, buff_data, 16, 2);//票卡交易计数器2
                    System.arraycopy(ssbuff, 34, buff_data, 18, 4);//交易金额4
                    System.arraycopy(ssbuff, 42, buff_data, 22, 4);//本次余额4
                }
                //本次余额
                ulBalance = SerialDataUtils.toBdInt(buff_data, 22, 4);
                //交易金额
                ulTransMoney = SerialDataUtils.toBdInt(buff_data, 18, 4);
                bComsume = 1;
                yct_purchase_reprocess(buff_data);//0xB7
            }
        }

    }

    public void upDateCpuTac(byte[] rebuff, byte[] cputac) {
        List<String> mlist = new ArrayList<>();
        String str = "";
        System.arraycopy(cputac, 0, rebuff, 87, 4);
        pay_order_upload.clear();//清空
        for (int i = 0; i < 92; i++) {
            str += SerialDataUtils.Bytes2HexString(rebuff, i, 1) + ",";
        }
        String[] sstr = str.split(",");
        for (int i = 0; i < 92; i++) {
            pay_order_upload.add(sstr[i]);
        }
        Log.e("yct info:", "updatecputac ok");
    }

    public void ShowRechargeFail() {
        for (int i = 0; i < 3; i++) {
            try {
                Thread.sleep(1500);
            } catch (Exception e) {
                e.printStackTrace();
            }
            onDataReceiveListener.onDataReceiveCar(1121, "充值失败!\n请您到飞充\n继续完成卡片充值!\n");
        }
    }

    public List<String> stringToList(String strs) {
        String str[] = strs.split(",");
        return Arrays.asList(str);
    }

    //查找和执行M1充正
    public void M1RollBackSubmit() {
        String strRecord = CarSerialPortUtil.ReadRollBackRecord();
        if (null != strRecord) {
            byte[] re_buff = new byte[200];
            re_buff = SerialDataUtils.HexString2Bytes(strRecord);
            byte[] ucTempData = new byte[]{(byte) 0xAA, 0x65, 0x00, 0x01, 0x02, (byte) 0x80, 0x00, (byte) 0xA8};
            if (SerialDataUtils.ByteMemCmp(re_buff, 0, ucTempData, 0, 8) != 0) {
                //写冲正记录时，记录没写进去，或者数据乱了
                Log.e("yct error info:", "冲正记录异常，无法做M1冲正!");
            } else {
                set_rollbackup_static(1);
                AddSocketClient.setRollBackStatus((byte) 0x02);//冲正状态  ??确认一下这个状态
                onDataReceiveListener.onDataReceiveCar(403, re_buff);////冲正
            }
        }
    }

    //查找和执行CPU提交
    public void CpuLoadSubmit() {
        String strRecord = CarSerialPortUtil.ReadCpuSubmitData();
        if (null != strRecord) {
            byte[] re_buff = new byte[200];
            re_buff = SerialDataUtils.HexString2Bytes(strRecord);
            byte[] ucTempData = new byte[]{(byte) 0xAA, (byte) 0x85, 0x00, 0x01, 0x02, (byte) 0x80, 0x00, (byte) 0x78};
            if (SerialDataUtils.ByteMemCmp(re_buff, 0, ucTempData, 0, 8) != 0) {
                Log.e("yct error info:", "圈存数据异常，无法做圈存状态提交!");
            } else {
                set_rollbackup_static(1);
                AddSocketClient.setRollBackStatus((byte) 0x00);//圈存状态
                onDataReceiveListener.onDataReceiveCar(302, re_buff);//圈存提交
            }
        }
    }

    public void ShowReaderError(byte errcode) {
        byte[] errbuf = new byte[2];

        String str = "";
        errbuf[0] = (byte) errcode;
        String strerr = "错误码:0x" + SerialDataUtils.Bytes2HexString(errbuf, 0, 1) + "\n";
        switch (errcode) {
            case (byte) 0x00:
                str = "操作正常";
                break;
            case (byte) 0x60:
                str = "没有安装SAM卡";
                break;
            case (byte) 0x61:
                str = "SAM卡初始化错误或未初始化";
                break;
            case (byte) 0x62:
                str = "SAM卡检验PIN错误";
                break;
            case (byte) 0x63:
                str = "SAM卡类型与交易类型不匹配";
                break;
            case (byte) 0x64:
                str = "SAM卡选择文件错误";
                break;
            case (byte) 0x65:
                str = "SAM卡读错误";
                break;
            case (byte) 0x66:
                str = "SAM卡写错误";
                break;
            case (byte) 0x67:
                str = "SAM卡认证错误";
                break;
            case (byte) 0x68:
                str = "SAM卡随机数错误";
                break;
            case (byte) 0x69:
                str = "SAM卡DES计算错误";
                break;
            case (byte) 0x6A:
                str = "SAM卡生成钱包密钥错误";
                break;
            case (byte) 0x7E:
                str = "SAM卡执行APDU命令错误";
                break;
            case (byte) 0x7F:
                str = "SAM卡操作超时";
                break;
            case (byte) 0x73:
                str = "SAM卡上电出错";
                break;
            case (byte) 0xE0:
                str = "MIFARE硬件初始化错误";
                break;
            case (byte) 0xE1:
                str = "SAM硬件初始化错误";
                break;
            case (byte) 0xE2:
                str = "命令错误";
                break;
            case (byte) 0xE3:
                str = "参数错误";
                break;
            case (byte) 0xE4:
                str = "检验和错误";
                break;
            case (byte) 0xE5:
                str = "线路通讯超时";
                break;
            case (byte) 0xE6:
                str = "内部FLASH写错误";
                break;
            case (byte) 0x80:
                str = "没有卡";
                break;
            case (byte) 0x81:
                str = "选择卡片错误";
                break;
            case (byte) 0x82:
                str = "停用卡片错误";
                break;
            case (byte) 0x83:
                str = "认证卡片错误";
                break;
            case (byte) 0x84:
                str = "卡片读操作错误";
                break;
            case (byte) 0x85:
                str = "卡片写操作错误";
                break;
            case (byte) 0x86:
                str = "卡片写操作中途中断";
                break;
            case (byte) 0x1A:
                str = "物理卡号不一致";
                break;
            case (byte) 0x90:
                str = "不是本系统标准的卡片";
                break;
            case (byte) 0x91:
                str = "卡片超出有效期";
                break;
            case (byte) 0x92:
                str = "城市代码或应用代码错误";
                break;
            case (byte) 0x93:
                str = "非法卡";
                break;
            case (byte) 0x94:
                str = "黑名单卡";
                break;
            case (byte) 0x95:
                str = "钱包余额不足";
                break;
            case (byte) 0x96:
                str = "钱包余额超出上限";
                break;
            case (byte) 0x97:
                str = "钱包未启用";
                break;
            case (byte) 0x98:
                str = "钱包已停用";
                break;
            case (byte) 0x99:
                str = "钱包正本被破坏";
                break;
            case (byte) 0x9A:
                str = "钱包已停用";
                break;
            case (byte) 0x9F:
                str = "公共信息区被破坏";
                break;
            case (byte) 0xAF:
                str = "卡片操作超时";
                break;
            case (byte) 0xB0:
                str = "交易操作中途中断";
                break;
            case (byte) 0xD1:
                str = "指令中扇区号或块号无效";
                break;
            case (byte) 0x0F:
                str = "卡片超出有效期";
                break;
            case (byte) 0x3F:
                str = "不支持的命令";
                break;
            default:
                str = "未知错误!";
                break;
        }
        strerr += str;
        Log.e("yct err info:", strerr);
        onDataReceiveListener.onDataReceiveCar(1121, strerr);

    }
}

//JNICALL 的函数名为Java_包名_类名_函数名；所以jni的.C函数必须与一致。并重新编译动态库。
