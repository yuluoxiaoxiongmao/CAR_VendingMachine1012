package com.example.vendingmachine.serialport;

import android.os.Environment;
import android.util.Log;
import java.io.File;
import java.io.RandomAccessFile;
import java.util.Timer;
import java.util.TimerTask;

public class TransformFile {

    //手机内存：其实是内部存储的根目录，在ES文件浏览器对应的是根目录，路径为：Environment.getDataDirectory().getParentFile()
    //SD卡：这里的SD卡是指内置的SD卡，路径为：Environment.getExternalStorageDirectory()

    public static String filePath =  Environment.getExternalStorageDirectory().getAbsolutePath()+ File.separator+"Code2.bin";         //文件路径
    long fileSize = 0;            //文件大小
    final int blockSize = 4*1024; //块大小
    public int blockNum = 0;      //块数量
    public int blockIndex = 0;    //块标识
    byte[] buff = new byte[blockSize];
    int buffLen = -1;

    Timer timer = null;

    /**
     * 定时器开启
     */
    void timer_start(){
        timer_stop();
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                send_block_info();
            }
        },10000);
    }

    /**
     * 定时器关闭
     */
    public void timer_stop(){
        if (timer != null){
            timer.cancel();
            timer = null;
        }
    }

    /**
     * 从服务器回去文件
     * @param url
     * @param savePath
     * @return
     */
    boolean get_file_from_server(String url,String savePath){
        File file = new File(savePath);
        if (file.exists()){
            file.delete();
        }
        return true;
    }

    /**
     * 获取文件内容
     * @return
     */
    private boolean get_file_block(){
        File file = new File(filePath);
        if (!file.exists()){
            return false;
        }
        fileSize = file.length();
        blockNum = (int) Math.ceil(fileSize / (double) blockSize);//即对浮点数向上取整
        try {
//            FileInputStream in = new FileInputStream(filePath);
//            for (int i = 0; i < blockNum; i++) {
//                buffLen = in.read(buff);//遍历将大文件读入byte数组中，当byte数组读满后写入对应的小文件中
//            }
            // 打开一个随机访问文件流，按只读方式
            RandomAccessFile randomFile = new RandomAccessFile(filePath, "r");// 文件长度，字节数
            long fileLength = randomFile.length();       // 将读文件的开始位置移到位置。
            randomFile.seek(blockIndex * blockSize);// 一次读10个字节，如果文件内容不足10个字节，则读剩下的字节。
            buffLen = randomFile.read(buff);
            Log.i("123", "get_file_block: file size:"+fileSize+" blocknum:"+blockNum+"blocksize:"+
            blockSize+" index:"+blockIndex);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * 传输指令
     * @param block_index
     * @param block_size
     * @param block_check
     * @return
     */
    private int send_transform_cmd(int block_index,int block_size,int block_check){
        byte[] temp = new byte[12];
        int i=0;
        temp[i++] = (byte)0xAB;
        temp[i++] = (byte)0x01;
        temp[i++] = (byte)0x07;
        temp[i++] = (byte)0x0d;
        temp[i++] = (byte)block_index;
        temp[i++] = (byte)((block_size >> 24) & 0xFF);
        temp[i++] = (byte)((block_size >> 16) & 0xFF);
        temp[i++] = (byte)((block_size >> 8) & 0xFF);
        temp[i++] = (byte)((block_size >> 0) & 0xFF);
        temp[i++] = (byte)((block_check >> 0) & 0xFF);
        int checkSum = 0;
        for (int j = 1; j < i; j++) {
            checkSum +=temp[j];
        }
        temp[i++] = (byte)checkSum;
        temp[i++] = (byte)0xBA;
        SerialPortUtil.getInstance().sendCmds(temp);
        return 0;
    }

    /**
     * 数据块信息
     */
    public void send_block_info(){
        timer_start();
        if (get_file_block()){
            if (buffLen > 0){
                byte check = buff[0];
                for (int j = 1; j < buffLen; j++) {
                    check +=buff[j];
                }
                send_transform_cmd(blockIndex,buffLen,check);
            }
        }
    }

    /**
     * 发送数据
     * @return
     */
    public int send_file(){
        Log.i("123", "send_file: ");
        if (get_file_block() == true){
            if (buffLen == blockSize){
                SerialPortUtil.getInstance().sendCmds(buff);
            }else{
                byte[] temp = new byte[buffLen];
                System.arraycopy(buff, 0, temp, 0, buffLen);
                SerialPortUtil.getInstance().sendCmds(temp);
            }
            Log.i("123", "send_file: "+buffLen);
        }else{
            Log.i("123", "send_file: fail");
        }
        return 0;
    }

    /**
     * 开始传输文件
     */
    public void send_file_start(){
        blockNum = 0;       //块数量
        blockIndex = 0;     //块
        buffLen = -1;
        Log.i("123", "send_file_start: "+filePath+" ");
        send_file_start_cmd();
        //send_block_info();
    }

    /**
     * 发送结束
     */
    public void send_file_end(){
        blockNum = 0;       //块数量
        blockIndex = 0;     //块
        buffLen = -1;
        send_file_end_cmd();
    }
    private int send_file_start_cmd(){
        Log.i("123", "send_file_start_cmd: "+SerialPortUtil.boardVersion);
        if (SerialPortUtil.boardVersion == 1 || SerialPortUtil.boardVersion == 2){
            byte[] temp = new byte[7];
            int i = 0;
            temp[i++] = (byte) 0xAB;
            temp[i++] = (byte) 0x01;
            temp[i++] = (byte) 0x02;
            temp[i++] = (byte) 0x0E;
            temp[i++] = (byte) (SerialPortUtil.boardVersion == 1 ? 2:1);
            int checkSum = 0;
            for (int j = 1; j < i; j++) {
                checkSum += temp[j];
            }
            temp[i++] = (byte) checkSum;
            temp[i++] = (byte) 0xBA;
            SerialPortUtil.getInstance().sendCmds(temp);
        }else {
            SerialPortUtil.getInstance().send_try();
        }
        return 0;
    }
    private int send_file_end_cmd(){
        byte[] temp = new byte[7];
        int i=0;
        temp[i++] = (byte)0xAB;
        temp[i++] = (byte)0x01;
        temp[i++] = (byte)0x02;
        temp[i++] = (byte)0x0E;
        temp[i++] = (byte)(SerialPortUtil.boardVersion == 1 ? 0x22:0x11);
        int checkSum = 0;
        for (int j = 1; j < i; j++) {
            checkSum +=temp[j];
        }
        temp[i++] = (byte)checkSum;
        temp[i++] = (byte)0xBA;
        SerialPortUtil.getInstance().sendCmds(temp);
        return 0;
    }


//    /**
//     * 拆分文件
//     * @param fileName 待拆分的完整文件名
//     * @param byteSize 按多少字节大小拆分
//     * @return 拆分后的文件名列表
//     * @throws IOException
//     */
//    public List<String> splitBySize(String fileName, int byteSize)
//            throws IOException {
//        List<String> parts = new ArrayList<String>();
//        File file = new File(fileName);
//        int count = (int) Math.ceil(file.length() / (double) byteSize);
//        int countLen = (count + "").length();
//        ThreadPoolExecutor threadPool = new ThreadPoolExecutor(count,
//                count * 3, 1, TimeUnit.SECONDS,
//                new ArrayBlockingQueue<Runnable>(count * 2));
//
//        for (int i = 0; i < count; i++) {
//            String partFileName = file.getName() + "."
//                    + leftPad((i + 1) + "", countLen, '0') + ".part";
//            threadPool.execute(new SplitRunnable(byteSize, i * byteSize,
//                    partFileName, file));
//            parts.add(partFileName);
//        }
//        return parts;
//    }
//    private class SplitRunnable implements Runnable {
//        int byteSize;
//        String partFileName;
//        File originFile;
//        int startPos;
//
//        public SplitRunnable(int byteSize, int startPos, String partFileName,
//                             File originFile) {
//            this.startPos = startPos;
//            this.byteSize = byteSize;
//            this.partFileName = partFileName;
//            this.originFile = originFile;
//        }
//
//        public void run() {
//            RandomAccessFile rFile;
//            OutputStream os;
//            try {
//                rFile = new RandomAccessFile(originFile, "r");
//                byte[] b = new byte[byteSize];
//                rFile.seek(startPos);// 移动指针到每“段”开头
//                int s = rFile.read(b);
//                os = new FileOutputStream(partFileName);
//                os.write(b, 0, s);
//                os.flush();
//                os.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//    }
//
//
//    /**
//     * 合并文件
//     *
//     * @param dirPath 拆分文件所在目录名
//     * @param partFileSuffix 拆分文件后缀名
//     * @param partFileSize 拆分文件的字节数大小
//     * @param mergeFileName 合并后的文件名
//     * @throws IOException
//     */
//    public void mergePartFiles(String dirPath, String partFileSuffix,
//                               int partFileSize, String mergeFileName) throws IOException {
//        ArrayList<File> partFiles = FileUtil.getDirFiles(dirPath,
//                partFileSuffix);
//        Collections.sort(partFiles, new FileComparator());
//
//        RandomAccessFile randomAccessFile = new RandomAccessFile(mergeFileName,
//                "rw");
//        randomAccessFile.setLength(partFileSize * (partFiles.size() - 1)
//                + partFiles.get(partFiles.size() - 1).length());
//        randomAccessFile.close();
//
//        ThreadPoolExecutor threadPool = new ThreadPoolExecutor(
//                partFiles.size(), partFiles.size() * 3, 1, TimeUnit.SECONDS,
//                new ArrayBlockingQueue<Runnable>(partFiles.size() * 2));
//
//        for (int i = 0; i < partFiles.size(); i++) {
//            threadPool.execute(new MergeRunnable(i * partFileSize,
//                    mergeFileName, partFiles.get(i)));
//        }
//
//    }
//
//    private class MergeRunnable implements Runnable {
//        long startPos;
//        String mergeFileName;
//        File partFile;
//
//        public MergeRunnable(long startPos, String mergeFileName, File partFile) {
//            this.startPos = startPos;
//            this.mergeFileName = mergeFileName;
//            this.partFile = partFile;
//        }
//
//        public void run() {
//            RandomAccessFile rFile;
//            try {
//                rFile = new RandomAccessFile(mergeFileName, "rw");
//                rFile.seek(startPos);
//                FileInputStream fs = new FileInputStream(partFile);
//                byte[] b = new byte[fs.available()];
//                fs.read(b);
//                fs.close();
//                rFile.write(b);
//                rFile.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//    }
//
//    package com.cnblogs.yjmyzz;
//
//import java.io.*;
//import java.util.*;
//import java.util.concurrent.*;
//
//    /**
//     * 文件处理辅助类
//     *
//     * @author yjmyzz@126.com
//     * @version 0.2
//     * @since 2014-11-17
//     *
//     */
//    public class FileUtil {
//
//        /**
//         * 当前目录路径
//         */
//        public static String currentWorkDir = System.getProperty("user.dir") + "\\";
//
//        /**
//         * 左填充
//         *
//         * @param str
//         * @param length
//         * @param ch
//         * @return
//         */
//        public static String leftPad(String str, int length, char ch) {
//            if (str.length() >= length) {
//                return str;
//            }
//            char[] chs = new char[length];
//            Arrays.fill(chs, ch);
//            char[] src = str.toCharArray();
//            System.arraycopy(src, 0, chs, length - src.length, src.length);
//            return new String(chs);
//
//        }
//
//        /**
//         * 删除文件
//         *
//         * @param fileName
//         *            待删除的完整文件名
//         * @return
//         */
//        public static boolean delete(String fileName) {
//            boolean result = false;
//            File f = new File(fileName);
//            if (f.exists()) {
//                result = f.delete();
//
//            } else {
//                result = true;
//            }
//            return result;
//        }
//
//        /***
//         * 递归获取指定目录下的所有的文件（不包括文件夹）
//         *
//         * @param obj
//         * @return
//         */
//        public static ArrayList<File> getAllFiles(String dirPath) {
//            File dir = new File(dirPath);
//
//            ArrayList<File> files = new ArrayList<File>();
//
//            if (dir.isDirectory()) {
//                File[] fileArr = dir.listFiles();
//                for (int i = 0; i < fileArr.length; i++) {
//                    File f = fileArr[i];
//                    if (f.isFile()) {
//                        files.add(f);
//                    } else {
//                        files.addAll(getAllFiles(f.getPath()));
//                    }
//                }
//            }
//            return files;
//        }
//
//        /**
//         * 获取指定目录下的所有文件(不包括子文件夹)
//         *
//         * @param dirPath
//         * @return
//         */
//        public static ArrayList<File> getDirFiles(String dirPath) {
//            File path = new File(dirPath);
//            File[] fileArr = path.listFiles();
//            ArrayList<File> files = new ArrayList<File>();
//
//            for (File f : fileArr) {
//                if (f.isFile()) {
//                    files.add(f);
//                }
//            }
//            return files;
//        }
//
//        /**
//         * 获取指定目录下特定文件后缀名的文件列表(不包括子文件夹)
//         *
//         * @param dirPath
//         *            目录路径
//         * @param suffix
//         *            文件后缀
//         * @return
//         */
//        public static ArrayList<File> getDirFiles(String dirPath,
//                                                  final String suffix) {
//            File path = new File(dirPath);
//            File[] fileArr = path.listFiles(new FilenameFilter() {
//                public boolean accept(File dir, String name) {
//                    String lowerName = name.toLowerCase();
//                    String lowerSuffix = suffix.toLowerCase();
//                    if (lowerName.endsWith(lowerSuffix)) {
//                        return true;
//                    }
//                    return false;
//                }
//
//            });
//            ArrayList<File> files = new ArrayList<File>();
//
//            for (File f : fileArr) {
//                if (f.isFile()) {
//                    files.add(f);
//                }
//            }
//            return files;
//        }
//
//        /**
//         * 读取文件内容
//         *
//         * @param fileName
//         *            待读取的完整文件名
//         * @return 文件内容
//         * @throws IOException
//         */
//        public static String read(String fileName) throws IOException {
//            File f = new File(fileName);
//            FileInputStream fs = new FileInputStream(f);
//            String result = null;
//            byte[] b = new byte[fs.available()];
//            fs.read(b);
//            fs.close();
//            result = new String(b);
//            return result;
//        }
//
//        /**
//         * 写文件
//         *
//         * @param fileName
//         *            目标文件名
//         * @param fileContent
//         *            写入的内容
//         * @return
//         * @throws IOException
//         */
//        public static boolean write(String fileName, String fileContent)
//                throws IOException {
//            boolean result = false;
//            File f = new File(fileName);
//            FileOutputStream fs = new FileOutputStream(f);
//            byte[] b = fileContent.getBytes();
//            fs.write(b);
//            fs.flush();
//            fs.close();
//            result = true;
//            return result;
//        }
//
//        /**
//         * 追加内容到指定文件
//         *
//         * @param fileName
//         * @param fileContent
//         * @return
//         * @throws IOException
//         */
//        public static boolean append(String fileName, String fileContent)
//                throws IOException {
//            boolean result = false;
//            File f = new File(fileName);
//            if (f.exists()) {
//                RandomAccessFile rFile = new RandomAccessFile(f, "rw");
//                byte[] b = fileContent.getBytes();
//                long originLen = f.length();
//                rFile.setLength(originLen + b.length);
//                rFile.seek(originLen);
//                rFile.write(b);
//                rFile.close();
//            }
//            result = true;
//            return result;
//        }
//
//        /**
//         * 拆分文件
//         *
//         * @param fileName
//         *            待拆分的完整文件名
//         * @param byteSize
//         *            按多少字节大小拆分
//         * @return 拆分后的文件名列表
//         * @throws IOException
//         */
//        public List<String> splitBySize(String fileName, int byteSize)
//                throws IOException {
//            List<String> parts = new ArrayList<String>();
//            File file = new File(fileName);
//            int count = (int) Math.ceil(file.length() / (double) byteSize);
//            int countLen = (count + "").length();
//            ThreadPoolExecutor threadPool = new ThreadPoolExecutor(count,
//                    count * 3, 1, TimeUnit.SECONDS,
//                    new ArrayBlockingQueue<Runnable>(count * 2));
//
//            for (int i = 0; i < count; i++) {
//                String partFileName = file.getName() + "."
//                        + leftPad((i + 1) + "", countLen, '0') + ".part";
//                threadPool.execute(new SplitRunnable(byteSize, i * byteSize,
//                        partFileName, file));
//                parts.add(partFileName);
//            }
//            return parts;
//        }
//
//        /**
//         * 合并文件
//         *
//         * @param dirPath
//         *            拆分文件所在目录名
//         * @param partFileSuffix
//         *            拆分文件后缀名
//         * @param partFileSize
//         *            拆分文件的字节数大小
//         * @param mergeFileName
//         *            合并后的文件名
//         * @throws IOException
//         */
//        public void mergePartFiles(String dirPath, String partFileSuffix,
//                                   int partFileSize, String mergeFileName) throws IOException {
//            ArrayList<File> partFiles = FileUtil.getDirFiles(dirPath,
//                    partFileSuffix);
//            Collections.sort(partFiles, new FileComparator());
//
//            RandomAccessFile randomAccessFile = new RandomAccessFile(mergeFileName,
//                    "rw");
//            randomAccessFile.setLength(partFileSize * (partFiles.size() - 1)
//                    + partFiles.get(partFiles.size() - 1).length());
//            randomAccessFile.close();
//
//            ThreadPoolExecutor threadPool = new ThreadPoolExecutor(
//                    partFiles.size(), partFiles.size() * 3, 1, TimeUnit.SECONDS,
//                    new ArrayBlockingQueue<Runnable>(partFiles.size() * 2));
//
//            for (int i = 0; i < partFiles.size(); i++) {
//                threadPool.execute(new MergeRunnable(i * partFileSize,
//                        mergeFileName, partFiles.get(i)));
//            }
//
//        }
//
//        /**
//         * 根据文件名，比较文件
//         *
//         * @author yjmyzz@126.com
//         *
//         */
//        private class FileComparator implements Comparator<File> {
//            public int compare(File o1, File o2) {
//                return o1.getName().compareToIgnoreCase(o2.getName());
//            }
//        }
//
//        /**
//         * 分割处理Runnable
//         *
//         * @author yjmyzz@126.com
//         *
//         */
//        private class SplitRunnable implements Runnable {
//            int byteSize;
//            String partFileName;
//            File originFile;
//            int startPos;
//
//            public SplitRunnable(int byteSize, int startPos, String partFileName,
//                                 File originFile) {
//                this.startPos = startPos;
//                this.byteSize = byteSize;
//                this.partFileName = partFileName;
//                this.originFile = originFile;
//            }
//
//            public void run() {
//                RandomAccessFile rFile;
//                OutputStream os;
//                try {
//                    rFile = new RandomAccessFile(originFile, "r");
//                    byte[] b = new byte[byteSize];
//                    rFile.seek(startPos);// 移动指针到每“段”开头
//                    int s = rFile.read(b);
//                    os = new FileOutputStream(partFileName);
//                    os.write(b, 0, s);
//                    os.flush();
//                    os.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//
//        /**
//         * 合并处理Runnable
//         *
//         * @author yjmyzz@126.com
//         *
//         */
//        private class MergeRunnable implements Runnable {
//            long startPos;
//            String mergeFileName;
//            File partFile;
//
//            public MergeRunnable(long startPos, String mergeFileName, File partFile) {
//                this.startPos = startPos;
//                this.mergeFileName = mergeFileName;
//                this.partFile = partFile;
//            }
//
//            public void run() {
//                RandomAccessFile rFile;
//                try {
//                    rFile = new RandomAccessFile(mergeFileName, "rw");
//                    rFile.seek(startPos);
//                    FileInputStream fs = new FileInputStream(partFile);
//                    byte[] b = new byte[fs.available()];
//                    fs.read(b);
//                    fs.close();
//                    rFile.write(b);
//                    rFile.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//
//    }

}
