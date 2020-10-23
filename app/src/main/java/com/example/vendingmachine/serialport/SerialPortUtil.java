package com.example.vendingmachine.serialport;

/**
 * HE SUN 2018/1/16.
 */

import android.util.Log;

import com.example.vendingmachine.platform.common.Constant;
import com.example.vendingmachine.utils.TextLog;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SerialPortUtil {
    private static final String TAG = "SerialPortUtil";
    private SerialPort mSerialPort = null;
    private OutputStream mOutputStream = null;
    private InputStream mInputStream = null;
    private ReadThread mReadThread = null;
    //    private String path = "/dev/ttyS4";       //这个是我们要读取的串口路径，这个硬件开发人员会告诉我们的
    private String path = "/dev/ttyS3";       // 测试  这个是我们要读取的串口路径，这个硬件开发人员会告诉我们的
    private int baudrate = 9600;              //这个参数，硬件开发人员也会告诉我们的
    //    private int baudrate = 57600;              //这个参数，硬件开发人员也会告诉我们的
    private static SerialPortUtil portUtil = null;
    private boolean isStop = false;
    public static boolean isPutOuting = false;//正在出货

    private TransformFile transformFile = null;
    public static int boardVersion = 0;

    /**
     * 回调接口
     */
    public interface OnDataReceiveListener {
        // void onDataReceive(int cmd, int data1, int data2, int data3);//数据正确
        void onDataReceive(int cmd, int data);//数据正确

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
    public static SerialPortUtil getInstance() {
        if (null == portUtil) {
            portUtil = new SerialPortUtil();
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

    private static final int DATA_PACK_START = -85;  // 包头
    private static final int DATA_PACK_END = -70;  // 包尾
    private static final int SEND_PACK = 0x01; //发送包
    private static final int ACK_PACK = 0x07; //响应包

    public static final int bagHead = 0;
    public static final int bagSign = 1;
    public static final int bagLen = 2;
    public static final int bagCmd = 3;
    public static final int bagData = 4;

    public static final int CMD_ERROR = 0X00; //错误指令
    public static final int ERROR_CODE_DATA_LEN = 0XFE; //数据长度错误
    public static final int ERROR_CODE_PACK_END = 0XFB; //包尾错误
    public static final int ERROR_CODE_CHECK = 0XFD; //校验码错误

    public static final int CMD_TRY = 0X01;   //尝试通信

    public void send_try() {
        cmd_package(CMD_TRY);
    }

    public static final int CMD_OPEN = 0X02;   // 出货
    public static final int DEVICE_TYPE_COMMON = 0x00; //不带升降、不带掉货检测
    public static final int DEVICE_TYPE_COMMON_AND_CHECK = 0x01; //不带升降、带掉货检测
    public static final int DEVICE_TYPE_RAISE_SPRING = 0x02; //升降弹簧、不带掉货检测
    public static final int DEVICE_TYPE_RAISE_SPRING_AND_CHECK = 0x03; //升降弹簧、带掉货检测； 出货时间为电机旋转后等待的时间
    public static final int DEVICE_TYPE_RAISE_TRACK = 0x04; //升降履带、不带掉货检测；出货时间为电机检测到之后再转的时间
    public static final int DEVICE_TYPE_RAISE_TRACK_AND_CHECK = 0x05; //升降履带、带掉货检测；出货时间为电机旋转后等待的时间
    public static final int DEVICE_TYPE_RAISE_TRACK_AND_INFRARED = 0x06; //升降履带、红外对管；出货时间为对管检测到之后再转的时间
    public static final int DEVICE_TYPE_RAISE_TRACK_AND_TIME = 0x07; //升降履带、电机按出货时间旋转
    public static final int DEVICE_TYPE_GRID = 0x08; //格子机，不带反馈
    public static final int DEVICE_TYPE_GRID_AND_CHECK = 0x09; //格子机，带反馈

    public static final int GET_OUT_TYPE_COMMON = 0x00; //直接取货，没有取货门控制
    public static final int GET_OUT_TYPE_AND_LOCK = 0x01; //取货门电磁锁控制，不带检测；对应取货超时为开锁时间
    public static final int GET_OUT_TYPE_AND_LOCK_CHECK = 0x02; //取货门电插锁控制，带检测；对应取货超时为最长取货时间

    public static final int OPEN_RESULT_RECEVICE_SUCCESS = 0x00; //接收成功
    public static final int OPEN_RESULT_PUTOUT_SUCCESS = 0x01; //出货成功（提示请取走）
    public static final int OPEN_RESULT_PUTOUT_FINISH = 0x02; //出货完成（出货结束）
    public static final int OPEN_RESULT_PUTOUT_FAIL = 0x03; //出货失败（掉货检测故障）
    public static final int OPEN_RESULT_RAISE_BREAKDOWN = 0x04; //升降故障
    public static final int OPEN_RESULT_MOTOR_BREAKDOWN = 0x05; //出货电机故障
    public static final int OPEN_RESULT_GETOUT_TIMEOUT = 0x06; //取货超时
    public static final int OPEN_RESULT_PUTOUTING = 0x07; //正在出货（上次出货未完成）

    //宏祥出货
    public void send_open(int electric) {
        byte[] bytes = cmd_package((byte) 0X02, (byte) electric);
        sendCmds(bytes);
    }

    //宏祥校正货道
    public void send_adjust_channel(int channel) {
        byte[] bytes = cmd_package(0x09, channel);
        sendCmds(bytes);
    }

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
     * 出货
     */
    public void send_open(int box, int mac_id, int deviceType, int ch_time, int cs_time, int getoutType, int getoutTime) {
        byte[] temp = new byte[7];
        temp[0] = (byte) box;
        temp[1] = (byte) mac_id;
        temp[2] = (byte) deviceType;
        temp[3] = (byte) ch_time;
        temp[4] = (byte) cs_time;
        temp[5] = (byte) getoutType;
        temp[6] = (byte) getoutTime;
        cmd_package(CMD_OPEN, temp);
    }

    public static final int CMD_RESET = 0X03; //电机测试
    public static final int RESET_RESULT_SUCCESS = 0x01; //成功
    public static final int RESET_RESULT_FAIL = 0x03; //失败

    /**
     * 电机校正
     */
    public void send_reset(int box, int time, int mac_id) {
        byte[] temp = new byte[3];
        temp[0] = (byte) box;
        temp[1] = (byte) time;
        temp[2] = (byte) mac_id;
        cmd_package(CMD_RESET, temp);//CMD_RESET
    }

    public static final int CMD_GET_TEMPERATURE = 0X04;   //温度
    public static final int CMD_SET_TEMPERATURE = 0X05;   //温度

    /**
     * 获取温度
     */
    public void send_get_temperature() {
        cmd_package(CMD_GET_TEMPERATURE);
    }

    /**
     * 设置温度
     */
    public void send_set_temperature(int min, int max) {
        byte[] temp = new byte[2];
        temp[0] = (byte) min;
        temp[1] = (byte) max;
        cmd_package(CMD_SET_TEMPERATURE, temp);
    }

    public static final int CMD_DOOR = 0X06;   //门
    public static final int DOOR_OPEN = 0X01;   //开
    public static final int DOOR_CLODE = 0X01;   //关

    public static final int REQUST_DOOR_ = 0X08;   //门

    /**
     * 升降设置
     */
    public static final int CMD_SET_SPEED = 0x07;   //设置升降速度

    /**
     * 设置升降参数
     *
     * @param speed 最高速度，复位速度，加速度，减速度
     */
    public void send_set_speed(int[] speed) {
        if (speed.length < 4) {
            return;
        }
        byte[] temp = new byte[6];
        temp[0] = (byte) (speed[0] >> 8);//2字节，高位在前
        temp[1] = (byte) (speed[0] >> 0);
        temp[2] = (byte) (speed[1] >> 8);
        temp[3] = (byte) (speed[1] >> 0);
        temp[4] = (byte) speed[2];
        temp[5] = (byte) speed[3];
        cmd_package(CMD_SET_SPEED, temp);
    }

    public static final int CMD_SET_HEIGHT = 0x08;   //设置升降高度

    /**
     * 每层高度
     *
     * @param height //1层  -->  6层
     */
    public void send_set_height(int[] height) {
        if (height.length < 6) {
            return;
        }
        byte[] temp = new byte[6 * 4];
        //1层  -->  6层     4字节，高位在前
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 4; j++) {
                temp[i * 4 + j] = (byte) (height[i] >> ((3 - j) * 8));
            }
        }
        cmd_package(0x12, temp);//CMD_SET_HEIGHT
    }

    public void set_speed(int[] speed) {
        if (speed.length < 4) {
            return;
        }
        byte[] pack = new byte[5 + 7];
        pack[0] = (byte) DATA_PACK_START;        //包头AB
        pack[1] = (byte) 0x01;                   //包标识
        pack[2] = (byte) (7);                  //数据长度
        pack[3] = (byte) 0x11;               //指令
        //2字节，高位在前
        pack[4] = (byte) (speed[0] >> 8);
        pack[5] = (byte) (speed[0] >> 0);
        pack[6] = (byte) (speed[1] >> 8);
        pack[7] = (byte) (speed[1] >> 0);

        pack[8] = (byte) speed[2];
        pack[9] = (byte) speed[3];

        int index = pack.length - 2;
        pack[index] = 0;//校验和
        for (int i = 1; i < index; i++) {
            pack[index] += pack[i];
        }
        pack[pack.length - 1] = (byte) DATA_PACK_END;//包尾BA
        sendCmds(pack);
    }


    public static final int CMD_STEP_MOTO_TEST = 0x09;   //升降测试
    public static final int TEST_UP = 0x00;   //向上
    public static final int TEST_DOWN = 0x01;   //向下

    /**
     * 升降测试
     */
    public void send_step_moto_test(int direction, int height) {
        byte[] temp = new byte[5];
        temp[0] = (byte) direction;
        temp[1] = (byte) (height >> 24);
        temp[2] = (byte) (height >> 16);
        temp[3] = (byte) (height >> 8);
        temp[4] = (byte) (height >> 0);
        cmd_package(CMD_STEP_MOTO_TEST, temp);
    }

    /**
     * 升降测试
     *
     * @param height
     */
    public void moto_test(int height) {
        byte[] pack = new byte[5 + 5];
        pack[0] = (byte) DATA_PACK_START;        //包头AB
        pack[1] = (byte) 0x01;                   //包标识
        pack[2] = (byte) (5);                    //数据长度
        pack[3] = (byte) 0x13;               //指令
        //4字节，高位在前
        pack[4] = (byte) (height >> 24);
        pack[5] = (byte) (height >> 16);
        pack[6] = (byte) (height >> 8);
        pack[7] = (byte) (height >> 0);

        int index = pack.length - 2;
        pack[index] = 0;//校验和
        for (int i = 1; i < index; i++) {
            pack[index] += pack[i];
        }
        pack[pack.length - 1] = (byte) DATA_PACK_END;//包尾BA
        sendCmds(pack);
    }

    public static final int CMD_STEP_MOTO_RESET = 0x0A;   //升降复位

    /**
     * 升降复位
     */
    public void send_moto_reset() {
        cmd_package(0x14);//CMD_STEP_MOTO_RESET
    }

    public static final int CMD_INSERT_COINS_CTRL = 0x0B;   //投币控制
    public static final int INSERT_COINS_OPEN = 0x01;   //打开
    public static final int INSERT_COINS_CLOSE = 0x00;   //关闭

    /**
     * 投币打开
     */
    public void send_insert_coins_open() {
        byte[] temp = new byte[1];
        temp[0] = (byte) INSERT_COINS_OPEN;
        cmd_package(CMD_INSERT_COINS_CTRL, temp);
    }

    /**
     * 投币打开
     */
    public void send_insert_coins_close() {
        byte[] temp = new byte[1];
        temp[0] = (byte) INSERT_COINS_CLOSE;
        cmd_package(CMD_INSERT_COINS_CTRL, temp);
    }

    public static final int CMD_INSERT_COINS = 0x0C;   //投币


    /**
     * 发送指令到串口
     *
     * @param cmd
     * @return
     */
    public boolean sendCmds(byte[] cmd) {
        TextLog.writeTxtToFile("发送串口数据:" + Arrays.toString(cmd), Constant.Companion.getFilePath(), Constant.fileName);
        Log.e("发送串口数据", Arrays.toString(cmd) + "  " + (mOutputStream != null));
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
                    Log.e("TAG", "sendCmds： " + sb);
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
        Log.i("req112", "result===================： " + result);
        return result;
    }

    public boolean sendCmds(byte[] cmd, int len) {
        boolean result = true;
        if (!isStop) {
            try {
                if (mOutputStream != null) {
                    len = len > cmd.length ? cmd.length : len;
                    mOutputStream.write(cmd, 0, cmd.length);
                    mOutputStream.flush();
                    StringBuilder sb = new StringBuilder();//非线程安全
                    for (int i = 0; i < cmd.length; i++) {
                        sb.append(SerialDataUtils.Byte2Hex((cmd[i]))).append(" ");
                    }
                    Log.i("req", "sendCmds： " + sb);
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
     * 指令
     *
     * @param cmd
     * @return
     */
    private byte[] cmd_package(int cmd) {
        byte[] pack = new byte[6];
        pack[0] = (byte) DATA_PACK_START;              //包头AB
        pack[1] = (byte) 0x01;                         //包标识
        pack[2] = (byte) 0x01;                         //数据长度1
        pack[3] = (byte) cmd;                          //指令
        pack[4] = (byte) (pack[1] + pack[2] + pack[3]);//校验和
        pack[5] = (byte) DATA_PACK_END;                //包尾BA
        sendCmds(pack);
        return pack;
    }

    /**
     * 指令
     *
     * @param cmd
     * @param data
     * @return
     */
    private byte[] cmd_package(int cmd, byte[] data) {
        int len = 6 + data.length;
        byte[] pack = new byte[len];
        pack[0] = (byte) DATA_PACK_START;    //包头AB
        pack[1] = (byte) 0x01;               //包标识
        pack[2] = (byte) (data.length + 1);    //数据长度1
        pack[3] = (byte) cmd;                 //指令

        int index = 4;
        for (int i = 0; i < data.length; i++) {
            pack[index + i] = data[i];
        }

        int sum = 0;
        for (int i = 1; i <= len - 3; i++) {//校验和
            sum += pack[i];
        }
        pack[len - 2] = (byte) sum;
        pack[len - 1] = (byte) DATA_PACK_END;  //包尾BA
        sendCmds(pack);
        return pack;
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
                        Log.i(TAG, "ReadThread.run return");
                        return;
                    }
                    byte[] temp = new byte[512];
                    size = mInputStream.read(temp);//该方法读不到数据时，会阻塞在这里
                    if (size > 0) {
                        long now = System.currentTimeMillis();
                        if (now - lastReadTime > 500 || read_buffer.size() > 512) {
                            read_buffer.clear();
                        }
                        lastReadTime = now;

                        for (int i = 0; i < size; i++) {
                            read_buffer.add(temp[i]);
                        }

                        StringBuffer sb = new StringBuffer();
                        for (int i = 0; i < read_buffer.size(); i++) {
                            sb.append(SerialDataUtils.Byte2Hex(read_buffer.get(i))).append(" - ");
                        }
                        Log.i("sendC_req1123", "read data: " + sb);
                        read_buffer = data_prser2(read_buffer);
                    } else {
                        Thread.sleep(50);//延时 50 毫秒
                    }
                } catch (Exception e) {
                    Log.i("sendC_req1123_e", "ReadThread.run  e.printStackTrace() : " + e);
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 提取有效数据   ---   根据包头包尾截取
     *
     * @param pack // 原数据
     * @return
     */
    public List<Byte> data_prser(List<Byte> pack) {
        final List<Integer> data = new ArrayList<>();// 提取后的数据
        data.clear();

        while (pack.size() != 0) {
            int data_start = -1;
            int data_end = -1;
            for (int i = 0; i < pack.size(); i++) {
                if (pack.get(i) == DATA_PACK_START) {//包头ab
                    data_start = i;
                    break;
                }
            }
            for (int i = 0; i < pack.size(); i++) {
                if (pack.get(i) == DATA_PACK_END) {//包尾ab
                    data_end = i;
                    break;
                }
            }

            if (data_start >= 0) {
                if (data_end >= 0) {
                    if (data_start < data_end) {
                        for (int i = data_start; i <= data_end; i++) {
                            data.add(Integer.valueOf(pack.get(i)));          // 提取数据赋值
                        }

                        byte sum = 0;
                        for (int i = data_start + 1; i < data_end - 1; i++) {//校验
                            sum += pack.get(i);
                        }
                        if (sum == pack.get(data_end - 1)) {   //校验正确
                            data_processing(data, data.size());
                        }
                        for (int i = 0; i <= data_end; i++) {  //删除[beg,end)区间的数据，传回下一个数据的位置
                            pack.remove(0);
                        }
                    } else {
                        for (int i = 0; i <= data_start; i++) {//删除[beg,end)区间的数据，传回下一个数据的位置
                            pack.remove(0);
                        }
                    }
                } else {//无包尾BA
                    return pack;
                }
            } else {//无包头AB
                pack.clear();
            }
        }
        return pack;
    }

    /**
     * 提取有效数据   ---   根据包头和数据长度截取
     *
     * @param pack
     * @return
     */
    public List<Byte> data_prser2(List<Byte> pack) {
        final List<Integer> data = new ArrayList<>();// 提取后的数据
        data.clear();
        while (pack.size() != 0) {
            int data_start = -1;
            for (int i = 0; i < pack.size(); i++) {
                if (pack.get(i) == DATA_PACK_START) {//包头ab
                    data_start = i;
                    break;
                }
            }
            //ab 01 02 01 00 CRC ba
            if (data_start >= 0) {                                                   //有包头
                if (pack.size() - data_start + 1 >= 6) {                             //包长至少有6位
                    int data_real_len = pack.get(data_start + 2);                    //数据至少有1位
                    if (data_real_len >= 1) {
                        int data_len = 3 + data_real_len + 2;                        //包的长度
                        int data_end = data_start + data_len - 1;                    //包的结束位置
                        if (data_end < pack.size()) {
                            if (pack.get(data_end) == DATA_PACK_END) {               //有包尾
                                byte sum = 0;
                                for (int i = data_start + 1; i < data_end - 1; i++) {//校验和
                                    sum += pack.get(i);
                                }
                                if (sum == pack.get(data_end - 1)) {                 //校验正确
                                    for (int i = data_start; i <= data_end; i++) {
                                        data.add(Integer.valueOf(pack.get(i)));      // 提取数据赋值
                                    }
                                    data_processing(data, data.size());
                                }
                            }
                            for (int i = 0; i <= data_end; i++) {                    //删除[beg,end)区间的数据
                                pack.remove(0);
                            }
                        } else {//数据未接收完整
                            break;
                        }
                    } else {
                        for (int i = 0; i <= data_start; i++) {
                            pack.remove(0);
                        }
                    }
                } else {     //数据未接收完整
                    break;
                }
            } else {//无包头AB
                pack.clear();
            }
        }
        return pack;
    }

    /**
     * 数据处理
     *
     * @param data
     * @param len
     */
    private void data_processing(final List<Integer> data, final int len) {
        if (len < 6) return;

        TextLog.writeTxtToFile("接收串口数据:" + data.toString(), Constant.Companion.getFilePath(), Constant.fileName);
        boolean flag = false;
        int data_len = data.get(bagLen);
        int pack_size = data_len + 5;
        data_len--;
        Log.e("接收串口数据", data.toString() + "数据：" + (data.get(bagSign) == ACK_PACK) + "  " + data.get(bagCmd));
        if (pack_size == data.size()) {

            if (data.get(bagSign) == ACK_PACK) {//下位机应答包
                switch (data.get(bagCmd)) {
                    case 2:
                        if (onDataReceiveListener != null) {
                            onDataReceiveListener.onDataReceive(202, 0);
                        } else {
                            onDataReceiveListener.onDataReceive(2021, 0);
                        }
                        break;
                    case 1:
                        if (onDataReceiveListener != null) {
                            onDataReceiveListener.onDataReceive(201, 0);
                        }
                        break;
                    case 3:
                        //测试电机指令00 成功  03失败
                        if (onDataReceiveListener != null) {
                            if (data.get(5) == 0) {
                                onDataReceiveListener.onDataReceive(203, state_cmd);
                            } else {
                                onDataReceiveListener.onDataReceive(2031, state_cmd);
                            }
                        }
                        break;
                }
                if (data_len == 1) {
                    Log.e("接收串口数据", "sdddsfasfadfgs");
//                    switch (data.get(bagCmd)) {    //指令码
//                        case CMD_TRY:  //尝试通信
//                        {
//                            boardVersion = data.get(bagData);
//                            if (onDataReceiveListener != null) {
//                                onDataReceiveListener.onDataReceive(data.get(bagCmd), data.get(bagData));
//                            }
//                            flag = true;
//                        }
//                        break;
//                        case CMD_OPEN:             //出货
//                            Log.e("req1123test_door1","open the door");
//                            switch (data.get(bagData)){
//                                case 0://接收成功
//                                    if (onDataReceiveListener != null) {onDataReceiveListener.onDataReceive(1,0);}//宏祥出货成功
//                                    break;
//                                case 1://出货成功（提示请取走）
//                                    //if (onDataReceiveListener != null) {onDataReceiveListener.onDataReceive(1,0);}
//                                    if (onDataReceiveListener != null) {onDataReceiveListener.onDataReceive(5,0);}//宏祥电机故障
//                                    break;
//                                case 2://出货完成（出货结束）（宏祥正在出货）
//                                    //if (onDataReceiveListener != null) {onDataReceiveListener.onDataReceive(2,0);}
//                                    break;
//                                case 3://出货失败
//                                    if (onDataReceiveListener != null) {onDataReceiveListener.onDataReceive(3,0);}
//                                    break;
//                                case 4://升降故障
//                                    //if (onDataReceiveListener != null) {onDataReceiveListener.onDataReceive(4,0);}
//                                    break;
//                                case 5://出货电机故障
//                                    //if (onDataReceiveListener != null) {onDataReceiveListener.onDataReceive(5,0);}
//                                    break;
//                                case 6://取货超时
//                                    //if (onDataReceiveListener != null) {onDataReceiveListener.onDataReceive(6,0);}
//                                    break;
//                            }
//                            break;
//                        case 0x09:            //电机校正CMD_RESET
//                            if(data.get(bagData) == 0x00){
//                            Log.e("TAG","电机校正");
//                                if (onDataReceiveListener != null) {onDataReceiveListener.onDataReceive(7,0);}
//                            }else{
//                                if (onDataReceiveListener != null) {onDataReceiveListener.onDataReceive(8,0);}
//                            }
//                            break;
//                        case CMD_SET_TEMPERATURE:  //设置温度
//                            if (data.get(bagData)==0x00){//温度设置成功
//                                if (onDataReceiveListener != null) {onDataReceiveListener.onDataReceive(10,0);}
//                            }else {//温度设置失败
//                                if (onDataReceiveListener != null) {onDataReceiveListener.onDataReceive(11,0);}
//                            }
//                            break;
//                        case 0x11:        //设置速度
//                            if(data.get(bagData) == 0){
//                                if (onDataReceiveListener != null) {onDataReceiveListener.onDataToast("速度参数设置成功");}
//                            }else{
//                                if (onDataReceiveListener != null) {onDataReceiveListener.onDataToast("速度参数设置失败");}
//                            }
//                            break;
//                        case 0x12:       //设置高度CMD_SET_HEIGHT
//                            if(data.get(bagData) == 0){
//                                if (onDataReceiveListener != null) {onDataReceiveListener.onDataToast("高度参数设置成功");}
//                            }else{
//                                if (onDataReceiveListener != null) {onDataReceiveListener.onDataToast("高度参数设置失败");}
//                            }
//                            break;
//                        case 0x13:   //步进电机测试CMD_STEP_MOTO_TEST
//                            if(data.get(bagData) == 0){
//                                if (onDataReceiveListener != null) {onDataReceiveListener.onDataReceive(7,0);}
//                                if (onDataReceiveListener != null) {onDataReceiveListener.onDataToast("测试成功!");}
//                            }else{
//                                if (onDataReceiveListener != null) {onDataReceiveListener.onDataReceive(8,0);}
//                                if (onDataReceiveListener != null) {onDataReceiveListener.onDataToast("测试失败!");}
//                            }
//                            break;
//                        case 0x14:  //步进电机复位CMD_STEP_MOTO_RESET
//                            if(data.get(bagData) == 0){
//                                if (onDataReceiveListener != null) {onDataReceiveListener.onDataToast("复位成功!");}
//                            }else{
//                                if (onDataReceiveListener != null) {onDataReceiveListener.onDataToast("复位失败!");}
//                            }
//                            break;
//                        case CMD_INSERT_COINS_CTRL://投币控制
//                            break;
//                        case CMD_INSERT_COINS:     //投币
//                            break;
//                        case CMD_ERROR:            //错误
//                            if (onDataReceiveListener != null) {
//                                onDataReceiveListener.onDataReceive(data.get(bagCmd), data.get(bagData));
//                            }
//                            flag = true;
//                            break;
//                        case 0x0d://发送程序包
//                            switch (data.get(bagData)){
//                                case 0x00:transformFile.send_file();break;      //指令接收成功
//                                case 0x01://程序接收成功，发下一个包
//                                    transformFile.blockIndex++;
//                                    if (transformFile.blockIndex >= transformFile.blockNum){
//                                        transformFile.timer_stop();
//                                        transformFile.send_file_end();
//                                        if (onDataReceiveListener != null){
//                                            onDataReceiveListener.onDataToast("发送程序包结束");
//                                        }
//                                    }else {
//                                        if (onDataReceiveListener != null){
//                                            onDataReceiveListener.onDataToast("正在发送程序包："+transformFile.blockIndex+"/"+transformFile.blockNum);
//                                        }
//                                        transformFile.send_block_info();
//                                    }
//                                break;
//                                case 0x02:{
//                                    if (onDataReceiveListener != null){
//                                        onDataReceiveListener.onDataToast("发送程序包失败，重新发送");
//                                    }
//                                    transformFile.send_block_info();
//                                }break;//程序接收失败
//                            }
//                            flag = true;
//                            break;
//                        case 0x0e://更新程序
//                            switch (data.get(bagData)){
//                                case 0x00:{
//                                    if (onDataReceiveListener != null){
//                                        onDataReceiveListener.onDataToast("开始发程序包");
//                                    }
//                                    transformFile.send_block_info();//开始发程序
//                                    break;//接收成功
//                                }
//                                case 0x01:
//                                    if (onDataReceiveListener != null){
//                                        onDataReceiveListener.onDataToast("固件相同");
//                                    }
//                                    break;//固件相同
//                                case 0x02:
//                                    if (onDataReceiveListener != null){
//                                        onDataReceiveListener.onDataToast("更新失败");
//                                    }
//                                    transformFile.timer_stop();
//                                    break;//更新失败
//                                case 0x03:
//                                    if (onDataReceiveListener != null){
//                                        onDataReceiveListener.onDataToast("更新成功");
//                                    }
//                                    transformFile.timer_stop();
//                                    break;
//                            }
//                            flag = true;
//                            break;
////                        case 8://开关门进入后台
////                            Log.e("test_door","open the door");
////                            break;
//                    }
                } else if (data_len == 3) {
                    Log.e("req1123test_door2", "open the door");
                    if (data.get(bagCmd) == 2) {
                        flag = true;
                        if (onDataReceiveListener != null) {
                            onDataReceiveListener.onDataReceive(202, 0);
                        }
                    }
                } else if (data_len == 2) {
                    if (data.get(bagCmd) == 1) {
                        flag = true;
                        if (onDataReceiveListener != null) {
                            onDataReceiveListener.onDataReceive(201, 0);
                        }//宏祥出货成功
                    }

                }
            } else if (data.get(bagSign) == SEND_PACK) {//下位机发送包

//                if (data_len == 1) {
//                    if (data.get(bagCmd) == CMD_DOOR) {
//                        flag = true;
//                        if (onDataReceiveListener != null) {
//                            onDataReceiveListener.onDataReceive(data.get(bagCmd), data.get(bagData));
//                        }
//                    }else if(data.get(bagCmd) == REQUST_DOOR_){
//                        Log.e("req1123test_door33","open the door");
//                        if (onDataReceiveListener != null) {onDataReceiveListener.onDataReceive(18,0);}//宏祥出货成功
//                    }
//                }
                if (data.get(bagCmd) == (byte) 0x0A) {
                    Log.e("req1123test_door111", "open the door");
//                    状态码：0x00开始关机
//                    0x01 停止关机
                    if (data.get(bagData) == (byte) 0x00) {
                        if (onDataReceiveListener != null) {
                            onDataReceiveListener.onDataReceive(204, data.get(bagData));
                        }
                    } else if (data.get(bagData) == (byte) 0x01) {
                        if (onDataReceiveListener != null) {
                            onDataReceiveListener.onDataReceive(2041, data.get(bagData));
                        }
                    }
                }
            }
        }

        /*if(flag == false && onDataReceiveListener != null){
            onDataReceiveListener.onDataError(data);
        }*/
        Log.i("req", "data_processing: data ok!!!");
    }

    /**
     * 发送控制板程序
     */
    public void send_file() {
        transformFile.send_file_start();
    }

    /**
     * 功能说明：测试上位机与控制板是否连接成功
     * 输入参数：电机号
     * 返回参数：电机号、确认字
     */
    public void wd_is_connect(int cmd) {
        //AB 01 02 01 00 04 BA
        byte[] pack = new byte[7];
        pack[0] = (byte) 0xAB;                                   //包头
        pack[1] = 0x01;                                          //包标识
        pack[2] = (byte) 0x02;                                    // 数据长度
        pack[3] = (byte) 0x01;                                   //指令码
        pack[4] = (byte) cmd;                                          //机台号
        pack[5] = (byte) (pack[1] + pack[2] + pack[3] + pack[4]);                                     //校验和
        pack[6] = (byte) 0xBA;              //包尾
        //checksun 从header到data（包含头尾）所有字节的XOR 1字节 ^ pack[18] ^ pack[19]
        sendCmds(pack);
    }

    /**
     * 功能说明：测试上位机与控制板是否连接成功
     * 输入参数：电机号
     * 返回参数：电机号、确认字
     * 出货指令
     */
    public void wd_out_goods(int cmd) {
        //AB 01 02 01 00 04 BA
        byte[] pack = new byte[8];
        pack[0] = (byte) 0xAB;                                   //包头
        pack[1] = 0x01;                                          //包标识
        pack[2] = (byte) 0x03;                                    // 数据长度
        pack[3] = (byte) 0x02;                                   //指令码
        pack[4] = (byte) cmd;                                          //电机号
        pack[5] = (byte) 0x01;                                          //机台号
        pack[6] = (byte) (pack[1] + pack[2] + pack[3] + pack[4] + pack[5]);                                     //校验和
        pack[7] = (byte) 0xBA;              //包尾
        //checksun 从header到data（包含头尾）所有字节的XOR 1字节 ^ pack[18] ^ pack[19]
        sendCmds(pack);
    }

    /**
     * 功能说明：测试上位机与控制板是否连接成功
     * 输入参数：电机号
     * 返回参数：电机号、确认字
     * 出货测试
     */
    //默认单个
    private static int state_cmd = 1;

    public void wd_out_goods_test(int cmd, int state) {
        //AB 01 02 01 00 04 BA
        state_cmd = state;
        byte[] pack = new byte[8];
        pack[0] = (byte) 0xAB;                                   //包头
        pack[1] = 0x01;                                          //包标识
        pack[2] = (byte) 0x03;                                    // 数据长度
        pack[3] = (byte) 0x03;                                   //指令码
        pack[4] = (byte) cmd;                                          //电机号
        pack[5] = (byte) 0x01;                                          //机台号
        pack[6] = (byte) (pack[1] + pack[2] + pack[3] + pack[4] + pack[5]);                                     //校验和
        pack[7] = (byte) 0xBA;              //包尾
        //checksun 从header到data（包含头尾）所有字节的XOR 1字节 ^ pack[18] ^ pack[19]
        sendCmds(pack);
    }

    /**
     * 功能说明：测试上位机与控制板是否连接成功
     * 输入参数：电机号
     * 返回参数：电机号、确认字
     * 灯光测试 00 关 01开
     */
    public void wd_is_light(int cmd) {
        //AB 01 02 01 00 04 BA
        byte[] pack = new byte[7];
        pack[0] = (byte) 0xAB;                                   //包头
        pack[1] = 0x01;                                          //包标识
        pack[2] = (byte) 0x02;                                    // 数据长度
        pack[3] = (byte) 0x04;                                   //指令码
        pack[4] = (byte) cmd;                                          //机台号
        pack[5] = (byte) (pack[1] + pack[2] + pack[3] + pack[4]);                                     //校验和
        pack[6] = (byte) 0xBA;              //包尾
        //checksun 从header到data（包含头尾）所有字节的XOR 1字节 ^ pack[18] ^ pack[19]
        sendCmds(pack);
    }

    /**
     * 功能说明：ACC功能  控制板检测到ACC线断开，向上位机发送准备关机指令控制板检测到ACC线断开，向上位机发送准备关机指令
     * 返回参数：返回参数：准备关机时间，确认码
     * 状态码：0x00开始关机
     * 0x01 停止关机
     * 关机时间：多少秒后断开电源
     * 确认码	：	0x00：允许断电
     * 0x01：停止断电
     */
    public void wd_is_close(int cmd) {
        byte[] pack = new byte[9];
        pack[0] = (byte) 0xAB;                                   //包头
        pack[1] = 0x07;                                          //包标识
        pack[2] = (byte) 0x04;                                    // 数据长度
        pack[3] = (byte) 0x0A;                                   //指令码
        pack[4] = (byte) 0x00;                                          //时间高位
        pack[5] = (byte) 0x1e;                                          // 时间低位
        pack[6] = (byte) cmd;                                          // 确认码
        pack[7] = (byte) (pack[1] + pack[2] + pack[3] + pack[4] + pack[5] + pack[6]);                                     //校验和
        pack[8] = (byte) 0xBA;              //包尾
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
}

//JNICALL 的函数名为Java_包名_类名_函数名；所以jni的.C函数必须与一致。并重新编译动态库。
