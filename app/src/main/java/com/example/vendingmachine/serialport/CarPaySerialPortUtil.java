//package com.example.vendingmachine.serialport;
//
///**
// * HE SUN 2018/1/16.
// */
//
//import android.util.Log;
//
//import com.example.vendingmachine.platform.common.Constant;
//import com.example.vendingmachine.utils.TextLog;
//
//import java.io.File;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.OutputStream;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Calendar;
//import java.util.List;
//
//public class CarPaySerialPortUtil {
//    private static final String TAG = "CarPaySerialPortUtil";
//    private SerialPort mSerialPort = null;
//    private OutputStream mOutputStream = null;
//    private InputStream mInputStream = null;
//    private ReadThread mReadThread = null;
//    private String path = "/dev/ttyS4";       //这个是我们要读取的串口路径，这个硬件开发人员会告诉我们的
//    //    private int baudrate = 9600;              //这个参数，硬件开发人员也会告诉我们的
//    private int baudrate = 57600;              //这个参数，硬件开发人员也会告诉我们的
//    private static CarPaySerialPortUtil portUtil = null;
//    private boolean isStop = false;
//    public static boolean isPutOuting = false;//正在出货
//
//    private TransformFile transformFile = null;
//    public static int boardVersion = 0;
//
//    /**
//     * 回调接口
//     */
//    public interface OnDataReceiveListener {
//        // void onDataReceive(int cmd, int data1, int data2, int data3);//数据正确
////        void onDataReceiveCar(int cmd, int data);//数据正确
//        void onDataReceiveCar(int cmd, String data);//数据正确
//        void onDataReceiveCar(int state, byte[] cmd);//数据错误
//        //void onDataError(List<Integer> data);//数据错误
//        void onDataToast(String str);        //提示toast
//    }
//
//    /**
//     * 回调方法
//     */
//    private OnDataReceiveListener onDataReceiveListener = null;
//
//    public void setOnDataReceiveListener(OnDataReceiveListener dataReceiveListener) {
//        onDataReceiveListener = dataReceiveListener;
//    }
//
//    /**
//     * 单例
//     */
//    public static CarPaySerialPortUtil getInstance() {
//        if (null == portUtil) {
//            portUtil = new CarPaySerialPortUtil();
//            portUtil.onCreate();
//        }
//        return portUtil;
//    }
//
//    /**
//     * 初始化串口信息
//     */
//    public void onCreate() {
//        try {
//            mSerialPort = new SerialPort(new File(path), baudrate, 0);
//            mOutputStream = mSerialPort.getOutputStream();
//            mInputStream = mSerialPort.getInputStream();
//            mReadThread = new ReadThread();
//            isStop = false;
//            mReadThread.start();
//            transformFile = new TransformFile();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    private static final int DATA_PACK_START = -67;  // 包头
//    private static final int DATA_PACK_END = -70;  // 包尾
//    private static final int SEND_PACK = 0x01; //发送包
//    private static final int ACK_PACK = 0x07; //响应包
//
//    public static final int bagHead = 0;
//    public static final int bagSign = 1;
//    public static final int bagLen = 2;
//    public static final int bagCmd = 3;
//    public static final int bagData = 4;
//
//    public static final int CMD_ERROR = 0X00; //错误指令
//    public static final int ERROR_CODE_DATA_LEN = 0XFE; //数据长度错误
//    public static final int ERROR_CODE_PACK_END = 0XFB; //包尾错误
//    public static final int ERROR_CODE_CHECK = 0XFD; //校验码错误
//
//    public static final int CMD_TRY = 0X01;   //尝试通信
//
//    public void send_try() {
//        cmd_package(CMD_TRY);
//    }
//
//    public static final int CMD_OPEN = 0X02;   // 出货
//    public static final int DEVICE_TYPE_COMMON = 0x00; //不带升降、不带掉货检测
//    public static final int DEVICE_TYPE_COMMON_AND_CHECK = 0x01; //不带升降、带掉货检测
//    public static final int DEVICE_TYPE_RAISE_SPRING = 0x02; //升降弹簧、不带掉货检测
//    public static final int DEVICE_TYPE_RAISE_SPRING_AND_CHECK = 0x03; //升降弹簧、带掉货检测； 出货时间为电机旋转后等待的时间
//    public static final int DEVICE_TYPE_RAISE_TRACK = 0x04; //升降履带、不带掉货检测；出货时间为电机检测到之后再转的时间
//    public static final int DEVICE_TYPE_RAISE_TRACK_AND_CHECK = 0x05; //升降履带、带掉货检测；出货时间为电机旋转后等待的时间
//    public static final int DEVICE_TYPE_RAISE_TRACK_AND_INFRARED = 0x06; //升降履带、红外对管；出货时间为对管检测到之后再转的时间
//    public static final int DEVICE_TYPE_RAISE_TRACK_AND_TIME = 0x07; //升降履带、电机按出货时间旋转
//    public static final int DEVICE_TYPE_GRID = 0x08; //格子机，不带反馈
//    public static final int DEVICE_TYPE_GRID_AND_CHECK = 0x09; //格子机，带反馈
//
//    public static final int GET_OUT_TYPE_COMMON = 0x00; //直接取货，没有取货门控制
//    public static final int GET_OUT_TYPE_AND_LOCK = 0x01; //取货门电磁锁控制，不带检测；对应取货超时为开锁时间
//    public static final int GET_OUT_TYPE_AND_LOCK_CHECK = 0x02; //取货门电插锁控制，带检测；对应取货超时为最长取货时间
//
//    public static final int OPEN_RESULT_RECEVICE_SUCCESS = 0x00; //接收成功
//    public static final int OPEN_RESULT_PUTOUT_SUCCESS = 0x01; //出货成功（提示请取走）
//    public static final int OPEN_RESULT_PUTOUT_FINISH = 0x02; //出货完成（出货结束）
//    public static final int OPEN_RESULT_PUTOUT_FAIL = 0x03; //出货失败（掉货检测故障）
//    public static final int OPEN_RESULT_RAISE_BREAKDOWN = 0x04; //升降故障
//    public static final int OPEN_RESULT_MOTOR_BREAKDOWN = 0x05; //出货电机故障
//    public static final int OPEN_RESULT_GETOUT_TIMEOUT = 0x06; //取货超时
//    public static final int OPEN_RESULT_PUTOUTING = 0x07; //正在出货（上次出货未完成）
//
//    //宏祥出货
//    public void send_open(int electric) {
//        byte[] bytes = cmd_package((byte) 0X02, (byte) electric);
//        sendCmds(bytes);
//    }
//
//    //宏祥校正货道
//    public void send_adjust_channel(int channel) {
//        byte[] bytes = cmd_package(0x09, channel);
//        sendCmds(bytes);
//    }
//
//    //宏祥打包
//    private byte[] cmd_package(int cmd, int data) {
//        byte[] pack = new byte[7];
//        pack[0] = (byte) 0xAB;                                   //包头AB
//        pack[1] = 0x01;                                          //包标识
//        pack[2] = 0x02;                                          //数据长度1
//        pack[3] = (byte) cmd;                                     //指令
//        pack[4] = (byte) data;                                    //数据
//        pack[5] = (byte) (pack[1] + pack[2] + pack[3] + pack[4]);//校验和
//        pack[6] = (byte) 0xBA;                                   //包尾BA
//        return pack;
//    }
//
//    /**
//     * 出货
//     */
//    public void send_open(int box, int mac_id, int deviceType, int ch_time, int cs_time, int getoutType, int getoutTime) {
//        byte[] temp = new byte[7];
//        temp[0] = (byte) box;
//        temp[1] = (byte) mac_id;
//        temp[2] = (byte) deviceType;
//        temp[3] = (byte) ch_time;
//        temp[4] = (byte) cs_time;
//        temp[5] = (byte) getoutType;
//        temp[6] = (byte) getoutTime;
//        cmd_package(CMD_OPEN, temp);
//    }
//
//    public static final int CMD_RESET = 0X03; //电机测试
//    public static final int RESET_RESULT_SUCCESS = 0x01; //成功
//    public static final int RESET_RESULT_FAIL = 0x03; //失败
//
//    /**
//     * 电机校正
//     */
//    public void send_reset(int box, int time, int mac_id) {
//        byte[] temp = new byte[3];
//        temp[0] = (byte) box;
//        temp[1] = (byte) time;
//        temp[2] = (byte) mac_id;
//        cmd_package(CMD_RESET, temp);//CMD_RESET
//    }
//
//    public static final int CMD_GET_TEMPERATURE = 0X04;   //温度
//    public static final int CMD_SET_TEMPERATURE = 0X05;   //温度
//
//    /**
//     * 获取温度
//     */
//    public void send_get_temperature() {
//        cmd_package(CMD_GET_TEMPERATURE);
//    }
//
//    /**
//     * 设置温度
//     */
//    public void send_set_temperature(int min, int max) {
//        byte[] temp = new byte[2];
//        temp[0] = (byte) min;
//        temp[1] = (byte) max;
//        cmd_package(CMD_SET_TEMPERATURE, temp);
//    }
//
//    public static final int CMD_DOOR = 0X06;   //门
//    public static final int DOOR_OPEN = 0X01;   //开
//    public static final int DOOR_CLODE = 0X01;   //关
//
//    public static final int REQUST_DOOR_ = 0X08;   //门
//
//    /**
//     * 升降设置
//     */
//    public static final int CMD_SET_SPEED = 0x07;   //设置升降速度
//
//    /**
//     * 设置升降参数
//     *
//     * @param speed 最高速度，复位速度，加速度，减速度
//     */
//    public void send_set_speed(int[] speed) {
//        if (speed.length < 4) {
//            return;
//        }
//        byte[] temp = new byte[6];
//        temp[0] = (byte) (speed[0] >> 8);//2字节，高位在前
//        temp[1] = (byte) (speed[0] >> 0);
//        temp[2] = (byte) (speed[1] >> 8);
//        temp[3] = (byte) (speed[1] >> 0);
//        temp[4] = (byte) speed[2];
//        temp[5] = (byte) speed[3];
//        cmd_package(CMD_SET_SPEED, temp);
//    }
//
//    public static final int CMD_SET_HEIGHT = 0x08;   //设置升降高度
//
//    /**
//     * 每层高度
//     *
//     * @param height //1层  -->  6层
//     */
//    public void send_set_height(int[] height) {
//        if (height.length < 6) {
//            return;
//        }
//        byte[] temp = new byte[6 * 4];
//        //1层  -->  6层     4字节，高位在前
//        for (int i = 0; i < 6; i++) {
//            for (int j = 0; j < 4; j++) {
//                temp[i * 4 + j] = (byte) (height[i] >> ((3 - j) * 8));
//            }
//        }
//        cmd_package(0x12, temp);//CMD_SET_HEIGHT
//    }
//
//    /**
//     * 发送指令到串口
//     *
//     * @param cmd
//     * @return
//     */
//    public boolean sendCmds(byte[] cmd) {
//        TextLog.writeTxtToFile("发送串口数据:" + Arrays.toString(cmd), Constant.Companion.getFilePath(), Constant.fileName);
//        boolean result = true;
//        if (!isStop) {
//            try {
//                if (mOutputStream != null) {
//                    mOutputStream.write(cmd, 0, cmd.length);
//                    mOutputStream.flush();
//                    StringBuilder sb = new StringBuilder();//非线程安全
//                    for (int i = 0; i < cmd.length; i++) {
//                        sb.append(SerialDataUtils.Byte2Hex((cmd[i]))).append(" ");
//                    }
//                    Log.e("TAG", "CZ_sendCmds_car： " + sb);
//                    try {
//                        Thread.sleep(100);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                } else {
//                    result = false;
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//                result = false;
//                Log.i("req112", "sendCmds： " + e);
//            }
//        } else {
//            System.out.println("sendCmds serialPort isClose");
//            result = false;
//        }
//        return result;
//    }
//
//    public boolean sendCmds(byte[] cmd, int len) {
//        boolean result = true;
//        if (!isStop) {
//            try {
//                if (mOutputStream != null) {
//                    len = len > cmd.length ? cmd.length : len;
//                    mOutputStream.write(cmd, 0, cmd.length);
//                    mOutputStream.flush();
//                    StringBuilder sb = new StringBuilder();//非线程安全
//                    for (int i = 0; i < cmd.length; i++) {
//                        sb.append(SerialDataUtils.Byte2Hex((cmd[i]))).append(" ");
//                    }
//                    Log.i("req", "sendCmds_car： " + sb);
//                    try {
//                        Thread.sleep(100);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                } else {
//                    result = false;
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//                result = false;
//                Log.i("req112", "sendCmds： " + e);
//            }
//        } else {
//            System.out.println("sendCmds serialPort isClose");
//            result = false;
//        }
//        return result;
//    }
//
//    /**
//     * 指令
//     *
//     * @param cmd
//     * @return
//     */
//    private byte[] cmd_package(int cmd) {
//        byte[] pack = new byte[6];
//        pack[0] = (byte) DATA_PACK_START;              //包头AB
//        pack[1] = (byte) 0x01;                         //包标识
//        pack[2] = (byte) 0x01;                         //数据长度1
//        pack[3] = (byte) cmd;                          //指令
//        pack[4] = (byte) (pack[1] + pack[2] + pack[3]);//校验和
//        pack[5] = (byte) DATA_PACK_END;                //包尾BA
//        sendCmds(pack);
//        return pack;
//    }
//
//    /**
//     * 指令
//     *
//     * @param cmd
//     * @param data
//     * @return
//     */
//    private byte[] cmd_package(int cmd, byte[] data) {
//        int len = 6 + data.length;
//        byte[] pack = new byte[len];
//        pack[0] = (byte) DATA_PACK_START;    //包头AB
//        pack[1] = (byte) 0x01;               //包标识
//        pack[2] = (byte) (data.length + 1);    //数据长度1
//        pack[3] = (byte) cmd;                 //指令
//
//        int index = 4;
//        for (int i = 0; i < data.length; i++) {
//            pack[index + i] = data[i];
//        }
//
//        int sum = 0;
//        for (int i = 1; i <= len - 3; i++) {//校验和
//            sum += pack[i];
//        }
//        pack[len - 2] = (byte) sum;
//        pack[len - 1] = (byte) DATA_PACK_END;  //包尾BA
//        sendCmds(pack);
//        return pack;
//    }
//
//    /**
//     * 接收数据的线程
//     */
//    private List<Byte> read_buffer = new ArrayList<>();
//    private long lastReadTime = 0;
//
//    private class ReadThread extends Thread {
//        @Override
//        public void run() {
//            super.run();
//            Log.i("req", "ReadThread start");//&& !isInterrupted()
//            while (true) {
//                Log.i("req", "ReadThread " + isInterrupted());
//                int size;
//                try {
//                    if (mInputStream == null) {
//                        Log.i(TAG, "ReadThread.run return");
//                        return;
//                    }
//                    byte[] temp = new byte[512];
//                    size = mInputStream.read(temp);//该方法读不到数据时，会阻塞在这里
//                    if (size > 0) {
//                        long now = System.currentTimeMillis();
//                        if (now - lastReadTime > 500 || read_buffer.size() > 512) {
//                            read_buffer.clear();
//                        }
//                        lastReadTime = now;
//
//                        for (int i = 0; i < size; i++) {
//                            read_buffer.add(temp[i]);
//                        }
//                        String ss = "";
//                        car_manger_info = "";
//                        StringBuffer sb = new StringBuffer();
//                        for (int i = 0; i < read_buffer.size(); i++) {
//                            sb.append(SerialDataUtils.Byte2Hex(read_buffer.get(i))).append(" - ");
//                        }
//                        car_manger_info = sb.toString();
////                        Log.i("sendC_req1123_car", "read data: " + read_buffer);
//                        read_buffer = data_prser2(read_buffer);
//                    } else {
//                        Thread.sleep(50);//延时 50 毫秒
//                    }
//                } catch (Exception e) {
//                    Log.i("sendC_req1123_e", "ReadThread.run  e.printStackTrace() : " + e);
//                    e.printStackTrace();
//                }
//            }
//        }
//    }
//
//    public List<Byte> data_prser2(List<Byte> pack) {
//        final List<Integer> data = new ArrayList<>();// 提取后的数据
//        data.clear();
//        while (pack.size() != 0) {
//
//            int data_start = -1;
//            data_processing_my(pack, pack.size());
//            for (int i = 0; i < pack.size(); i++) {
////                Log.e("DATA_PACK_START", pack.get(i) + "");
//                if (pack.get(i) == DATA_PACK_START) {//包头ab
//                    data_start = i;
////                    Log.e("DATA_PACK_START222", data_start + "");
////                    data_processing_my(pack, pack.size());
//                    break;
//                }
//            }
//        }
//        return pack;
//    }
//
//    /**
//     * 发送控制板程序
//     */
//    public void send_file() {
//        transformFile.send_file_start();
//    }
//
//    //测试羊城通，签到
//    //BA 11 10 02 88 01 20 00 11 10 00 00 00 00 00 00 00 00 11
//    // 返回 BD 07 10 00 89 00 71 44 16
//    //拿到管理卡号   89 00 71 44
//    public void test_card() {
//        byte[] pack = new byte[19];
//        pack[0] = (byte) 0xBA;                                   //包头BA 1字节
//        pack[1] = 0x11;                                          //len 1字节
//        pack[2] = 0x10;                                          //command 1字节
//        pack[3] = (byte) 0x02;                                     //data 数据内容  GPK类型 0x02 :PKI  ，其他保留
//        pack[4] = (byte) 0x88;                                    //GPK卡PIN 6    1
//        pack[5] = (byte) 0x01;                                    //GPK卡PIN 6     2
//        pack[6] = (byte) 0x20;                                    //GPK卡PIN 6     3
//        pack[7] = (byte) 0x00;                                    //GPK卡PIN 6     4
//        pack[8] = (byte) 0x11;                                    //GPK卡PIN 6     5
//        pack[9] = (byte) 0x10;                                    //GPK卡PIN 6     6
//        pack[10] = (byte) 0x00;                                    //INIT_KEY 8      1
//        pack[11] = (byte) 0x00;                                    //INIT_KEY 8      2
//        pack[12] = (byte) 0x00;                                    //INIT_KEY 8      3
//        pack[13] = (byte) 0x00;                                    //INIT_KEY 8      4
//        pack[14] = (byte) 0x00;                                    //INIT_KEY 8      5
//        pack[15] = (byte) 0x00;                                    //INIT_KEY 8      6
//        pack[16] = (byte) 0x00;                                    //INIT_KEY 8      7
//        pack[17] = (byte) 0x00;                                    //INIT_KEY 8      8
//        pack[18] = (byte) (pack[0] ^ pack[1] ^ pack[2] ^ pack[3] ^ pack[4] ^ pack[5] ^ pack[6] ^ pack[7] ^ pack[8] ^ pack[9] ^ pack[10] ^ pack[11] ^ pack[12] ^ pack[13] ^ pack[14] ^ pack[15] ^ pack[16] ^ pack[17]);
//        //checksun 从header到data（包含头尾）所有字节的XOR 1字节 ^ pack[18] ^ pack[19]
//        sendCmds(pack);
//    }
//
//    //测试羊城通
//    public void test_sign_in_1() {
//        //获取系统的日期
//        Calendar calendar = Calendar.getInstance();
////年
//        int year = calendar.get(Calendar.YEAR);
////月
//        int month = calendar.get(Calendar.MONTH) + 1;
////日
//        int day = calendar.get(Calendar.DAY_OF_MONTH);
////获取系统时间
////小时
//        int hour = calendar.get(Calendar.HOUR_OF_DAY);
////分钟
//        int minute = calendar.get(Calendar.MINUTE);
////秒
//        int second = calendar.get(Calendar.SECOND);
//
////        time2.setText("Calendar获取当前日期" + year + "年" + month + "月" + day + "日" + hour + ":" + minute + ":" + second);
//        Log.e("Calendar获取当前日期",Byte.parseByte ((year+"").substring(2,4))+"");
//        byte[] pack = new byte[21];
//        pack[0] = (byte) 0xBA;                                   //包头BA 1字节
//        pack[1] = (byte) 0x13;                                          //len 1字节
//        pack[2] = (byte) 0xC8;                                          //command 1字节
//        pack[3] = Byte.parseByte(manger_num.get(0));                                     //data 数据内容  GPK类型 0x02 :PKI  ，其他保留，管理卡号    1
//        pack[4] = Byte.parseByte(manger_num.get(1));                                    //管理卡号    2
//        pack[5] = Byte.parseByte(manger_num.get(2));                                    //管理卡号      3
//        pack[6] = Byte.parseByte(manger_num.get(3));                                    //管理卡号    4
//        pack[7] = (byte) 0x88;                                    //终端编号        1
//        pack[8] = (byte) 0x01;                                    //终端编号        2
//        pack[9] = (byte) 0x20;                                    //终端编号        3
//        pack[10] = (byte) 0x00;                                    //终端编号        4
//        pack[11] = (byte) 0x11;                                    //终端编号        5
//        pack[12] = (byte) 0x11;                                    //终端编号        6
//        pack[13] = Byte.parseByte ("20");                                    //终端时间        YY
//        pack[14] = Byte.parseByte ((year+"").substring(2,4));                                    //终端时间        YY
//        pack[15] = (byte) month;                                    //终端时间        MM
//        pack[16] = (byte) day;                                    //终端时间        DD
//        pack[17] = (byte) hour;                                    //终端时间        HH
//        pack[18] = (byte) minute;                                    //终端时间        MI
//        pack[19] = (byte) second;                                    //终端时间        SS
//        pack[20] = (byte) (pack[0] ^ pack[1] ^ pack[2] ^ pack[3] ^ pack[4] ^ pack[5] ^ pack[6] ^ pack[7] ^ pack[8] ^ pack[9] ^ pack[10] ^ pack[11] ^ pack[12] ^ pack[13] ^ pack[14] ^ pack[15] ^ pack[16] ^ pack[17] ^ pack[18] ^ pack[19]);
//
//        sendCmds(pack);
//        //checksun 从header到data（包含头尾）所有字节的XOR 1字节 ^ pack[18] ^ pack[19]
//    }
//
//    //测试羊城通  签到步骤2
//    //BD 8B C8 00
//    // FE 01 00 01 83 80 00 80 49 15 10 CA F2 4F B6 CA 49 15 10 CA F2 4F B6 CA 87
//    // E9 7E 13 FF 05 22 FB B7 31 5A AC 92 AD D8 CD F1 D0 24 57 FB 97 04 E2 49 15 10 CA F2 4F
//    // B6 CA 49 15 10 CA F2 4F B6 CA 49 15 10 CA F2 4F B6 CA 49 15 10 CA F2 4F B6 CA 49 15 10
//    // CA F2 4F B6 CA 49 15 10 CA F2 4F B6 CA 49 15 10 CA F2 4F B6 CA 49 15 10 CA F2 4F B6 CA
//    // 49 15 10 CA F2 4F B6 CA 49 15 10 CA F2 4F B6 CA 49 15 10 CA F2 4F B6 CA
//    // 66
//    public void test_sign_in_2(byte[] num) {
//        byte[] pack = new byte[140];
//        pack[0] = (byte) 0xBA;                                   //包头BA 1字节
//        pack[1] = (byte)0x8a;                                          //len 1字节
//        pack[2] = (byte)0xC9;                                          //command 1字节0
//        int index = 3;
//        Log.e("test_sign_in_2",num.length+"");
//        for (int i = 0; i < 136; i++) {
////            int num_info = Integer.valueOf(num.get(i),16);
////            pack[index + i] = (byte)num_info;
//            pack[index + i] = num[i];
//        }
////        for (int i = 1; i < 3; i++) {//校验和
////            pack[139] ^= pack[i];
////        }
//        byte sum = 0;
//        for (int i = 0; i < 139; i++) {//校验和
//            sum ^= pack[i];
////            pack[i] ^= pack[i+1];
////            Log.e("pack_info_sum",sum+"  ==  "+i);
//        }
////        pack[139] = (byte)sum;
////        int num_info_2 = Integer.valueOf("-103",16);
////        BigInteger bi = new BigInteger("-103", 16);
////        Integer.toHexString(((-103) & 0x000000FF) | 0xFFFFFF00).substring(6);
////        Log.e("pack_info_sum",bi.intValue()+"  ==  "+(byte)num_info_2+" == "+Integer.toHexString(((-103) & 0x000000FF) | 0xFFFFFF00).substring(6));
//        //checksun 从header到data（包含头尾）所有字节的XOR 1字节 ^ pack[18] ^ pack[19]
////        Log.e("pack_info",pack.length+" == "+(pack[139] ^= pack[0]));
//        pack[139] = (byte) (pack[0] ^ pack[1] ^ pack[2] ^ pack[3] ^ pack[4] ^ pack[5] ^ pack[6] ^ pack[7] ^ pack[8] ^ pack[9] ^ pack[10] ^ pack[11] ^ pack[12] ^ pack[13] ^ pack[14] ^ pack[15] ^ pack[16] ^ pack[17]^ pack[18] ^ pack[19] ^ pack[20] ^ pack[21] ^ pack[22] ^ pack[23] ^ pack[24] ^ pack[25] ^ pack[26] ^ pack[27] ^ pack[28] ^ pack[29] ^ pack[30] ^ pack[31] ^ pack[32] ^ pack[33] ^ pack[34] ^ pack[35]^
//                pack[36] ^ pack[37] ^ pack[38] ^ pack[39] ^ pack[40] ^ pack[41] ^ pack[42] ^ pack[43] ^ pack[44] ^ pack[45] ^ pack[46] ^ pack[47] ^ pack[48] ^ pack[49] ^ pack[50] ^ pack[51] ^ pack[52] ^ pack[53]^ pack[54] ^ pack[55] ^ pack[56] ^ pack[57] ^ pack[58] ^ pack[59] ^ pack[60] ^ pack[61] ^ pack[62] ^ pack[63] ^ pack[64] ^ pack[65] ^ pack[66] ^ pack[67] ^ pack[68] ^ pack[69] ^ pack[70] ^ pack[71]^
//                pack[72] ^ pack[73] ^ pack[74] ^ pack[75] ^ pack[76] ^ pack[77] ^ pack[78] ^ pack[79] ^ pack[80] ^ pack[81] ^ pack[82] ^ pack[83] ^ pack[84] ^ pack[85] ^ pack[86] ^ pack[87] ^ pack[88] ^ pack[89]^ pack[90] ^ pack[91] ^ pack[92] ^ pack[93] ^ pack[94] ^ pack[95] ^ pack[96] ^ pack[97] ^ pack[98] ^ pack[99] ^ pack[100] ^ pack[101] ^ pack[102] ^ pack[103] ^ pack[104] ^ pack[105] ^ pack[106] ^ pack[107]^
//                pack[108] ^ pack[109] ^ pack[110] ^ pack[111] ^ pack[112] ^ pack[113] ^ pack[114] ^ pack[115] ^ pack[116] ^ pack[117] ^ pack[118] ^ pack[119] ^ pack[120] ^ pack[121] ^ pack[122] ^ pack[123] ^ pack[124] ^ pack[125]^ pack[126] ^ pack[127] ^ pack[128] ^ pack[129] ^ pack[130] ^ pack[131] ^ pack[132] ^ pack[133] ^ pack[134] ^ pack[135] ^ pack[136] ^ pack[137] ^ pack[138]);
//        Log.e("pack_info_sum",sum+"  ==  "+pack[139]);
//                sendCmds(pack);
//    }
//
//    //测试羊城通
//    public void test_card4() {
//        byte[] pack = new byte[19];
//        pack[0] = (byte) 0xBA;                                   //包头BA 1字节
//        pack[1] = 0x17;                                          //len 1字节
//        pack[2] = 0x10;                                          //command 1字节
//        pack[3] = (byte) 0x02;                                     //data 数据内容  GPK类型 0x02 :PKI  ，其他保留
//        pack[4] = (byte) 0x01;                                    //GPK卡PIN 6    1
//        pack[5] = (byte) 0x00;                                    //GPK卡PIN 6     2
//        pack[6] = (byte) 0x99;                                    //GPK卡PIN 6     3
//        pack[7] = (byte) 0x99;                                    //GPK卡PIN 6     4
//        pack[8] = (byte) 0x32;                                    //GPK卡PIN 6     5
//        pack[9] = (byte) 0x29;                                    //GPK卡PIN 6     6
//        pack[10] = (byte) 0x31;                                    //INIT_KEY 8      1
//        pack[11] = (byte) 0x32;                                    //INIT_KEY 8      2
//        pack[12] = (byte) 0x33;                                    //INIT_KEY 8      3
//        pack[13] = (byte) 0x34;                                    //INIT_KEY 8      4
//        pack[14] = (byte) 0x35;                                    //INIT_KEY 8      5
//        pack[15] = (byte) 0x36;                                    //INIT_KEY 8      6
//        pack[16] = (byte) 0x37;                                    //INIT_KEY 8      7
//        pack[17] = (byte) 0x38;                                    //INIT_KEY 8      8
//        pack[18] = (byte) (pack[0] ^ pack[1] ^ pack[2] ^ pack[3] ^ pack[4] ^ pack[5] ^ pack[6] ^ pack[7] ^ pack[8] ^ pack[9] ^ pack[10] ^ pack[11] ^ pack[12] ^ pack[13] ^ pack[14] ^ pack[15] ^ pack[16] ^ pack[17]);
//        //checksun 从header到data（包含头尾）所有字节的XOR 1字节 ^ pack[18] ^ pack[19]
//        sendCmds(pack);
//    }
//
//    //测试羊城通 获取监控状态
//    public void card_state() {
//        byte[] pack = new byte[4];
//        pack[0] = (byte) 0xBA;                                   //包头BA 1字节
//        pack[1] = 0x02;                                          //len 1字节
//        pack[2] = 0x65;                                          //command 1字节
//        pack[3] = (byte) (pack[0] ^ pack[1] ^ pack[2]);
//        //checksun 从header到data（包含头尾）所有字节的XOR 1字节 ^ pack[18] ^ pack[19]
//        sendCmds(pack);
//    }
//
//    //测试羊城通 寻卡
//    public void test_card_search() {
//        byte[] pack = new byte[5];
//        pack[0] = (byte) 0xBA;                                   //包头BA 1字节
//        pack[1] = 0x03;                                          //len 1字节
//        pack[2] = 0x45;                                          //command 1字节
//        pack[3] = (byte) 0x52;                                     //data 数据内容  Para 0x52
//        pack[4] = (byte) (pack[0] ^ pack[1] ^ pack[2] ^ pack[3]);
//        //checksun 从header到data（包含头尾）所有字节的XOR 1字节 ^ pack[18] ^ pack[19]
//        sendCmds(pack);
//    }
//
//    //测试羊城通 读卡
//    public void test_card_read() {
//        byte[] pack = new byte[4];
//        pack[0] = (byte) 0xBA;                                   //包头BA 1字节
//        pack[1] = 0x02;                                          //len 1字节
//        pack[2] = (byte) 0x93;                                          //command 1字节
//        pack[3] = (byte) (pack[0] ^ pack[1] ^ pack[2]);
//        //checksun 从header到data（包含头尾）所有字节的XOR 1字节 ^ pack[18] ^ pack[19]
//        sendCmds(pack);
//    }
//
//    /**
//     * 关闭串口
//     */
//    public void closeSerialPort() {
//        isStop = true;
//        if (mReadThread != null) {
//            mReadThread.interrupt();
//            try {
//                mReadThread.wait(500);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
//        if (mSerialPort != null) {
//            mSerialPort.close();
//            portUtil = null;
//        }
//    }
//
//    private static int CARD_SEARCH_LEN = 2;
//    private static int CARD_CHECK_LEN = 11;
//    private static int CARD_READ_LEN = 12;
//    String car_manger_info = "";
//    String car_manger_info_1 = "";
//    private static int CAR_STATUS = 3;//状态码 0 成功， 其他异常
//    private static int CAR_COMMAND_LEN = 1;//指令长度
//    private static int CAR_COMMAND = 2;//命令符
//
//    private static int CAR_INIT = 16;//初始化读卡器 0x10
//    private static int CAR_SIGN_1 = -56;//签到1
//    private static int CAR_SIGN_2 = -55;//签到2
//    private static int CAR_SEARCH = 69;//寻卡
//    private static int CAR_GET_STATE = 101;//获取当前状态
//    private static int CAR_GET_NUM = -109;//读卡
//    private List<String> manger_num = null;
//    private List<String> car_defult_info = null;
//    private byte[] mRestart = null;
//    private void data_processing_my(final List<Byte> data, final int len) {
////        Log.i("sendC_xxxx21", data.get(CAR_COMMAND)+"   "  +(data.get(CAR_COMMAND) == CAR_INIT) +  "   " +data.get(CAR_STATUS) + "   " + (data.get(CAR_STATUS)==0));
//        Log.i("CZ_sendC_xxxx", car_manger_info.replace(" - "," "));
//        String[] manger = car_manger_info.split(" - ");
//        Log.i("11——car_info", manger.length+"");
//        //初始化
//        if (data.get(CAR_COMMAND) == CAR_INIT) {
//            if (onDataReceiveListener != null) {
//                if (data.get(CAR_STATUS)==0) {
//                    String shid_info = "";
//                    manger_num = new ArrayList<>();
//                    for (int i = 0; i < 4; i++) {
//                        manger_num.add(manger[i + 4]);
//                        shid_info += (" "+manger[i + 4]);
//                    }
//                    onDataReceiveListener.onDataReceiveCar(101, shid_info);
//                    Log.e("car_manger_info_122", manger_num.size() + "  ");
////                    App.Companion.getSpUtil().putString(Constant.Companion.getCAR_LOGIN_INFO(), shid_info);
//                    Log.e("car_manger_info_1223", shid_info);
//                    read_buffer.clear();
//                    car_manger_info = "";
//                    car_manger_info_1 = "";
////                    test_sign_in_1();
//                } else {
//                    onDataReceiveListener.onDataReceiveCar(1011, "0");
//                }
//            }
//        } else if (data.get(CAR_COMMAND) == CAR_SIGN_1) {
//            //签到1指令
//            if (onDataReceiveListener != null) {
//                if (data.get(CAR_STATUS).toString().equals("0")) {
//
//                    manger_num = new ArrayList<>();
//                    Log.e("manger", manger.length + "");
//                    for (int i = 0; i < manger.length-5; i++) {
//                        manger_num.add(manger[i + 4]);
//                        car_manger_info_1+=(" "+manger[i + 4]);
//                    }
//                    Log.e("sendC_xxxx", car_manger_info_1+"");
//                    onDataReceiveListener.onDataReceiveCar(102, car_manger_info_1);
////                    manger_num.set(1,"2");
////                    manger_num.set(4,"81");
////                    Log.e("manger_num", manger_num.size() + "");
//                    Log.e("sendC_xxxx", car_manger_info_1+"");
////                    Log.e("car_manger_info_1", SerialDataUtils.hexString2Bytes("2B44EFD9")[0]+"");
////                    mRestart = StringUtil.HexCommandtoByte("FE FE 68 04 04 68 53 FD 50 00 A0 16".getBytes());
////                    Log.e("test_16",mRestart[0]+"");
//                    onDataReceiveListener.onDataReceiveCar(103,check_DE1(manger_num));
////                    test_sign_in_2(manger_num);
//                    read_buffer.clear();
//                    car_manger_info = "";
//                    car_manger_info_1 = "";
//                } else {
//                    onDataReceiveListener.onDataReceiveCar(1031, "0");
//                }
//            }
//
//        } else if (data.get(CAR_COMMAND) == CAR_SIGN_2) {
//            //签到2指令
////            Log.e("socket_1042_info",App.Companion.getSpUtil().getString(Constant.Companion.getCAR_LOGIN_INFO(), ""));
//            if (onDataReceiveListener != null) {
//                if (data.get(CAR_STATUS).toString().equals("0")) {
//                    manger_num = new ArrayList<>();
//                    Log.e("manger_2", manger.length + "");
//                    for (int i = 0; i < manger.length-51; i++) {
//                        manger_num.add(manger[i+50]);
//                        car_manger_info_1+=(" "+manger[i+50]);
//                    }
//                    car_defult_info = new ArrayList<>();
//                    onDataReceiveListener.onDataReceiveCar(1042,car_manger_info.replace(" - "," "));
//                    Log.e("sendC_xxxx2", car_manger_info_1+"");
//                    onDataReceiveListener.onDataReceiveCar(104,check_DE1(manger_num));
//                    read_buffer.clear();
//                    car_manger_info = "";
//                    car_manger_info_1 = "";
//                } else {
//                    onDataReceiveListener.onDataReceiveCar(1041, "0");
//                }
//            }
//            read_buffer.clear();
//            car_manger_info = "";
//            car_manger_info_1 = "";
//        }else if (data.get(CAR_COMMAND) == CAR_GET_STATE) {
//            //查询当前状态
//            if (onDataReceiveListener != null) {
//                Log.e("sendC_xxxx6", (data.get(CAR_STATUS)== 0)+"");
//                if (data.get(CAR_STATUS)== 0) {
//                    Log.e("sendC_xxxx6", "123");
//                    onDataReceiveListener.onDataReceiveCar(106,"0");
//                    read_buffer.clear();
//                    car_manger_info = "";
//                    car_manger_info_1 = "";
//                } else {
//                    onDataReceiveListener.onDataReceiveCar(1061, "0");
//                }
//            }
//            read_buffer.clear();
//            car_manger_info = "";
//            car_manger_info_1 = "";
//        }else if (data.get(CAR_COMMAND) == CAR_SEARCH) {
//            //寻卡反馈
//            if (onDataReceiveListener != null) {
//                if (data.get(CAR_STATUS) == 0) {
//                    Log.e("sendC_xxxx7", "777");
//                    onDataReceiveListener.onDataReceiveCar(107,"0");
//                    read_buffer.clear();
//                    car_manger_info = "";
//                    car_manger_info_1 = "";
//                } else {
//                    onDataReceiveListener.onDataReceiveCar(1071, "0");
//                }
//            }
//            read_buffer.clear();
//            car_manger_info = "";
//            car_manger_info_1 = "";
//        }else if (data.get(CAR_COMMAND) == CAR_SEARCH) {
//            //寻卡反馈
//            if (onDataReceiveListener != null) {
//                if (data.get(CAR_STATUS) == 0) {
//                    Log.e("sendC_xxxx7", "777");
//                    onDataReceiveListener.onDataReceiveCar(107,"0");
//                    read_buffer.clear();
//                    car_manger_info = "";
//                    car_manger_info_1 = "";
//                } else {
//                    onDataReceiveListener.onDataReceiveCar(1071, "0");
//                }
//            }
//            read_buffer.clear();
//            car_manger_info = "";
//            car_manger_info_1 = "";
//        }else if (data.get(CAR_COMMAND) == CAR_GET_NUM) {
//            //读卡操作
//            if (onDataReceiveListener != null) {
//                if (data.get(CAR_STATUS) == 0) {
//                    Log.e("sendC_xxxx8", "888");
//                    manger_num = new ArrayList<>();
//
//                    for (int i = 0; i < manger.length; i++) {
//                        manger_num.add(manger[i]);
//                        car_manger_info_1+=(" "+manger[i]);
//                    }
//                    Log.e("manger", car_manger_info_1 + "");
//                    onDataReceiveListener.onDataReceiveCar(108,check_CARD(manger_num,manger.length));
//                    read_buffer.clear();
//                    car_manger_info = "";
//                    car_manger_info_1 = "";
//                } else {
//                    onDataReceiveListener.onDataReceiveCar(1081, check_CARD(manger_num,manger.length));
//                }
//            }
//            read_buffer.clear();
//            car_manger_info = "";
//            car_manger_info_1 = "";
//        }
//    }
//    private static String test_info = "";
////校验和…… 计算方法,获取16进制的信封数据
//    public static byte[] check_DE1(List<String> num) {
//        byte[] pack = new byte[136];
//
//        for (int i = 0; i < 136; i++) {
//            int num_info = Integer.valueOf(num.get(i),16);
//            pack[i] = (byte)num_info;
//            test_info +=(" "+num.get(i));
//        }
//        Log.e("test_sign_in_2",test_info+"");
//        return pack;
//    }
//
//    public static byte[] check_CARD(List<String> num,int size) {
//        byte[] pack = new byte[size];
//
//        for (int i = 0; i < size; i++) {
//            int num_info = Integer.valueOf(num.get(i),16);
//            pack[i] = (byte)num_info;
//            test_info +=(" "+num.get(i));
//        }
//        Log.e("test_sign_in_2",test_info+"");
//        return pack;
//    }
//
//}
//
////JNICALL 的函数名为Java_包名_类名_函数名；所以jni的.C函数必须与一致。并重新编译动态库。
