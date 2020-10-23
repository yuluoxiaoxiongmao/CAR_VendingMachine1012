package com.example.vendingmachine.serialport;

/**
 * HE SUN 2018/1/16.
 */
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class SerialPortUtil_Money {
    private static final String TAG = "SerialPortUtil";
    private SerialPort mSerialPort     = null;
    private OutputStream mOutputStream = null;
    private InputStream mInputStream   = null;
    private ReadThread mReadThread     = null;
    private String path = "/dev/ttyS1";       //这个是我们要读取的串口路径，这个硬件开发人员会告诉我们的
    private int baudrate = 9600;              //这个参数，硬件开发人员也会告诉我们的
    private static SerialPortUtil_Money portUtil = null;
    private boolean isStop = false;
    public static int boardVersion = 0;

    /**
     * 回调接口
     */
    public interface OnDataReceiveListener {
        void onDataReceive_coin(float money);//投入硬币
        void onDataReceive_note(int money);  //投入纸币

        void onDataToast_Money(String str);        //提示toast
        //void onDataError(List<Integer> data);//数据错误
    }

    /**
     * 回调方法
     * @param dataReceiveListener
     */
    private OnDataReceiveListener onDataReceiveListener = null;
    public void setOnDataReceiveListener(OnDataReceiveListener dataReceiveListener) {
        onDataReceiveListener = dataReceiveListener;
    }

    /**
     * 单例
     * @return
     */
    public static SerialPortUtil_Money getInstance() {
        if (null == portUtil) {
            portUtil = new SerialPortUtil_Money();
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 硬币器
     */
    public static int coin_0_5 = 0;//当前钱箱可退币的0.5元的个数
    public static int coin_1 = 0;  //当前钱箱可退币的1元的个数
    public void coin_clear(){
        coin_0_5 = 0;
        coin_1 = 0;
    }

    public static final int COIN_RESET      = 0X08; //重启
    public static final int COIN_STATUS     = 0X09; //状态要求
    public static final int COIN_TUBE       = 0X0A; //钱筒状态
    public static final int COIN_POLL       = 0X0B; //工作状态
    public static final int COIN_COIN_TYPE  = 0X0C; //使用硬币的设定
    public static final int COIN_DISPENSE   = 0X0D; //硬币找零
    public static final int COIN_MONEY_1    = 51; //投入1元
    public static final int COIN_MONEY_0_5  = 50; //投入0.5元
    /**
     * 纸币器
     */
    public static final int NOTE_STATUS     = 30; //状态要求
    public static final int NOTE_END        = 9; //
    public static final int NOTE_MONEY_1    = 80; //投入1元
    public static final int NOTE_MONEY_5    = 81; //投入5元
    public static final int NOTE_MONEY_10   = 82; //投入10元
    public static final int NOTE_MONEY_20   = 83; //投入20元

    public static final int NOTE_ERROR_SENSE      = 0X02; //传感器有问题
    public static final int NOTE_RESET_DISCERMENT = 0X06; //识别器复位
    public static final int NOTE_ERROR_BOX        = 0X08; //现金盒偏移
    public static final int NOTE_ERROR_DISCERMENT = 0X09; //识别器不可用
    public static final int NOTE_ERROR_REJECT     = 0X0B; //纸币拒收

    public void send_coin_reset(){
        byte[] temp = new byte[1];
        temp[0] = (byte)COIN_RESET;
        sendCmds(temp);
    }
    public void send_coin_status(){
        byte[] temp = new byte[1];
        temp[0] = (byte)COIN_STATUS;
        sendCmds(temp);
    }
    public void send_coin_tube(){
        byte[] temp = new byte[1];
        temp[0] = (byte)COIN_TUBE;
        sendCmds(temp);
    }
    public void send_coin_type(){
        byte[] temp = new byte[5];
        temp[0] = (byte)COIN_COIN_TYPE;
        temp[1] = 0x00;
        temp[2] = 0x03;//5毛
        temp[3] = 0x00;
        temp[4] = 0x03;//1元
        sendCmds(temp);
    }

    /**
     * 硬币找零
     * @param num  数量
     * @param type 退币的硬币类型  0.5 --》 0.5元  1 --》 1元
     */
    public void send_coin_dispense(int num,float type){
        byte[] temp = new byte[2];
        temp[0] = (byte)COIN_DISPENSE;
        if (type == 0.5){
            temp[1] = (byte)(((num << 4) | 0x00)&0xFF);
            sendCmds(temp);
        }else if (type == 1){
            temp[1] = (byte)(((num << 4) | 0x01)&0xFF);
            sendCmds(temp);
        }
    }

    /**
     * 退币
     * @param money
     * @return
     */
    public boolean coin_return(float money){
        if (money <= coin_0_5*0.5+coin_1){
            int num_1 = 0;
            int num_0_5 = 0;
            if (money*10%10 != 0){//有0.5元
                num_0_5 +=1;
            }
            if (money <= coin_1){
                num_1 = (int)money;
            }else{
                num_1 = coin_1;
                float remain = money - num_1 - num_0_5*0.5f;
                num_0_5 += (int)(remain / 0.5);
            }

            if (num_1 <= coin_1 && num_0_5 <= coin_0_5) {
                CoinRunnable runnable = new CoinRunnable();
                runnable.num_1 = num_1;
                runnable.num_0_5 = num_0_5;
                new Thread(runnable).start();
                return true;
            }else{
                Log.i(TAG, "coin_return: 零钱不足");
            }
        }
        return false;
    }

    class CoinRunnable implements Runnable {
        int num_1 = 0;
        int num_0_5 = 0;
        @Override
        public void run() {
            if (num_1 > 0) {
                send_coin_dispense(num_1, 1);
                try {
                    Thread.sleep(2000);//延时 2000 毫秒
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (num_0_5 > 0) {
                send_coin_dispense(num_0_5, 0.5f);
            }
            coin_1 -= num_1;
            coin_0_5 -= num_0_5;
        }
    }

    /**
     * 发送指令到串口
     * @param cmd
     * @return
     */
    public boolean sendCmds(byte[] cmd) {
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
                           // sb.append(read_buffer.get(i)).append(" - ");
                        }
                        Log.i("req1123", "read data: " + sb);
                        read_buffer = data_prser(read_buffer);
                    } else {
                        Thread.sleep(50);//延时 50 毫秒
                    }
                } catch (Exception e) {
                    Log.i("req", "ReadThread.run  e.printStackTrace() : " + e);
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 提取有效数据   ---   根据包头包尾截取
     * @param pack // 原数据
     * @return
     */
    public List<Byte> data_prser(List<Byte> pack) {
        List<Byte> data = new ArrayList<>();// 提取后的数据
        data.clear();

        while (pack.size() != 0) {
            int data_end = -1;
            for (int i = 1; i < pack.size()-1; i++) {
                if (pack.get(i) == 0X0D && pack.get(i+1) == 0X0A) {//包头ab
                    data_end = i+1;
                    break;
                }
            }

            if (data_end >= 0) {
                for (int i = 0; i <= data_end; i++) {
                    data.add(pack.get(i));
                }
                //System.arraycopy(pack,0,data,0,data_end);// 提取数据赋值
                for (int i = 0; i <= data_end; i++) {       //删除[beg,end)区间的数据，传回下一个数据的位置
                    pack.remove(0);
                }
                data_processing(data);
            } else {//无包尾BA
                return pack;
            }
        }
        return pack;
    }
    public void data_processing(final List<Byte> data) {
        if (data.size() < 3){
            return;
        }
        StringBuffer buffer = new StringBuffer();
        List<Integer> pack = new ArrayList<>();

        for (int i = 2; i <= data.size()-2; i+=3) {
                int temp1 = (data.get(i-2)-48)&0XFF;
                int temp2 = (data.get(i-1)-48)&0XFF;
                pack.add(temp1*10+temp2);
                buffer.append(temp1);
                buffer.append(temp2);
                buffer.append("-");
        }
        Log.i(TAG, "data_processing: "+buffer);
        buffer = new StringBuffer();
        Log.i(TAG, "data_processing: "+buffer);
            /**
             * 纸币
             */
            if (pack.size() == 3 ) {
                if (pack.get(0) == NOTE_STATUS ) {//&& pack.get(2) == NOTE_END
                    if (onDataReceiveListener != null) {
                        int temp = pack.get(1);
                        switch (temp) {
                            case NOTE_MONEY_1:
                                onDataReceiveListener.onDataReceive_note(1);
                                break;
                            case NOTE_MONEY_5:
                                onDataReceiveListener.onDataReceive_note(5);
                                break;
                            case NOTE_MONEY_10:
                                onDataReceiveListener.onDataReceive_note(10);
                                break;
                            case NOTE_MONEY_20:
                                onDataReceiveListener.onDataReceive_note(20);
                                break;
                            case NOTE_ERROR_REJECT:
                                onDataReceiveListener.onDataToast_Money("纸币拒收");
                                break;
                            case NOTE_ERROR_BOX:
                                onDataReceiveListener.onDataToast_Money("现金盒偏移");
                                break;
                            case NOTE_ERROR_SENSE:
                                onDataReceiveListener.onDataToast_Money("传感器故障");
                                break;
                            case NOTE_RESET_DISCERMENT:
                                onDataReceiveListener.onDataToast_Money("识别器复位");
                                break;
                            case NOTE_ERROR_DISCERMENT:
                                onDataReceiveListener.onDataToast_Money("识别器故障");
                                break;
                        }
                    }
                    for (int i = 0; i < 3; i++) {//3
                        pack.remove(0);
                    }
                } else if (pack.get(0) == COIN_RESET) {
                    if (onDataReceiveListener != null) {
                        switch (pack.get(1)) {
                            case COIN_MONEY_0_5:
                                onDataReceiveListener.onDataReceive_coin(0.5f);
                                coin_0_5 = pack.get(2);
                                break;
                            case COIN_MONEY_1:
                                onDataReceiveListener.onDataReceive_coin(1);
                                coin_1 = pack.get(2);
                                break;
                            case COIN_RESET:
                                onDataReceiveListener.onDataToast_Money("硬币器复位");
                                break;
                        }
                    }
                    for (int i = 0; i < 3; i++) {
                        pack.remove(0);
                    }
                }
            }else if(pack.size() == 2){
                if (pack.get(0) == NOTE_STATUS ) {//&& pack.get(2) == NOTE_END
                    if (onDataReceiveListener != null) {
                        int temp = pack.get(1);
                        switch (temp) {
                            case NOTE_MONEY_1:
                                onDataReceiveListener.onDataReceive_note(1);
                                break;
                            case NOTE_MONEY_5:
                                onDataReceiveListener.onDataReceive_note(5);
                                break;
                            case NOTE_MONEY_10:
                                onDataReceiveListener.onDataReceive_note(10);
                                break;
                            case NOTE_MONEY_20:
                                onDataReceiveListener.onDataReceive_note(20);
                                break;
                            case NOTE_ERROR_REJECT:
                                onDataReceiveListener.onDataToast_Money("纸币拒收");
                                break;
                            case NOTE_ERROR_BOX:
                                onDataReceiveListener.onDataToast_Money("现金盒偏移");
                                break;
                            case NOTE_ERROR_SENSE:
                                onDataReceiveListener.onDataToast_Money("传感器故障");
                                break;
                            case NOTE_RESET_DISCERMENT:
                                onDataReceiveListener.onDataToast_Money("识别器复位");
                                break;
                            case NOTE_ERROR_DISCERMENT:
                                onDataReceiveListener.onDataToast_Money("识别器故障");
                                break;
                        }
                    }
                    for (int i = 0; i < 2; i++) {//3
                        pack.remove(0);
                    }
                }
            } else if (pack.size() == 2){
                if (onDataReceiveListener != null) {
                    if (pack.get(0) == COIN_RESET) {
                        switch (pack.get(1)) {
                            case 0x02:
                                onDataReceiveListener.onDataToast_Money("退币成功");
                                break;
                        }
                    }
                }
                for (int i = 0; i < 2; i++) {
                    pack.remove(0);
                }
            }else{
                for (int i = 0; i < pack.size(); i++) {
                    pack.remove(0);
                }
            }
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
