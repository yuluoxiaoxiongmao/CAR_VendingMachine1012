package com.example.vendingmachine.platform.http.download;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;
import com.example.vendingmachine.App;
import com.example.vendingmachine.platform.common.Constant;
import com.example.vendingmachine.platform.http.ApiManager;
import com.example.vendingmachine.utils.FileUtil;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import rx.Observer;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 *  He 2018/5/3.
 */

public class HttpUtils {
    private static int indx = 0;
    private static OkHttpClient client = null;
    private HttpUtils() {}
    public static OkHttpClient getInstance() {
        if (client == null) {
            synchronized (HttpUtils.class) {
                if (client == null)
                    client = new OkHttpClient();
            }
        }
        return client;
    }

    public static void doPostJson(String url,String json, Callback callback) {
        //FormBody.Builder builder = new FormBody.Builder();
        RequestBody body = FormBody.create(MediaType.parse("application/json; charset=utf-8"), json);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        Call call = getInstance().newCall(request);
        call.enqueue(callback);
    }

    /**
     *上传json
     */
    public static void getBanner(String json,Observer observer) {
        RequestBody requestBody=RequestBody.create(MediaType.parse("application/json; charset=utf-8"),json);
        ApiManager.getService().uploadStock(requestBody)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(observer);
        }


    /**
     * 文件下载
     */
    public static void downFile(String url, final String fileName, final OnDownloadListener listener) {
        final File file = FileUtil.createApkFile(App.Companion.getMContext(), fileName);
        Request request = new Request.Builder().url(url).addHeader("Accept-Encoding", "identity").build();
        Call call = getInstance().newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("TAG",e.toString());
                listener.onDownloadFailed();
            }
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                InputStream is = null;
                byte[] buf = new byte[2048];
                int len;
                FileOutputStream fos = null;
                long total = response.body().contentLength();
                listener.onDownloadLength(total,1);
                try {
                    long current = 0;
                    is = response.body().byteStream();
                    fos = new FileOutputStream(file);
                    while ((len = is.read(buf)) != -1) {
                        current += len;
                        fos.write(buf, 0, len);
                        int progress = (int) (current * 1.0f / total * 100);
                        listener.onDownloading(progress);
                    }
                    fos.flush();
                    listener.onDownloadSuccess();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (is != null) is.close();
                    if (fos != null) fos.close();
                }
            }
        });
    }

    /**
     * 下载商品视频
     */
    public static void downAddFilevideo(final Context context, final ArrayList<String> url, final ArrayList<String> fileName, final OnDownloadListener listener) {
        boolean isExists = FileUtil.fileIsExists(Constant.Companion.getVIDEO_PATH()+"/"+fileName.get(indx)+".mp4");
        Log.e("TAG","视频文件是否一样："+isExists+"===="+fileName.get(indx)+".mp4");
        if (isExists){
            if (url.size() - 1 > indx) {
                indx++;
                downAddFilevideo(context, url, fileName, listener);
            } else {
                indx = 0;
                listener.onDownloadSuccess();
            }
        }else {
            final File file = FileUtil.createFileVideo(context, fileName.get(indx));
            Request request = new Request.Builder().url(url.get(indx)).addHeader("Accept-Encoding", "identity").build();
            Call call = getInstance().newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    listener.onDownloadFailed();
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    InputStream is = null;
                    byte[] buf = new byte[2048];
                    int len;
                    FileOutputStream fos = null;
                    long total = response.body().contentLength();
                    listener.onDownloadLength(total, url.size());
                    try {
                        long current = 0;
                        is = response.body().byteStream();
                        fos = new FileOutputStream(file);
                        while ((len = is.read(buf)) != -1) {
                            current += len;
                            fos.write(buf, 0, len);
                            int progress = (int) (current * 1.0f / total * 100);
                            listener.onDownloading(progress);
                            //Log.e("TAG", "current------>" + current);
                        }
                        fos.flush();
                        Log.w("TAG", file.getName() + "视频下载完成");
                        if (url.size() - 1 > indx) {
                            indx++;
                            downAddFilevideo(context, url, fileName, listener);
                        } else {
                            Log.w("TAG", "没有视频了");
                            listener.onDownloadSuccess();
                            indx = 0;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        if (is != null) is.close();
                        if (fos != null) fos.close();
                    }
                }
            });
        }
    }


    public interface OnDownloadListener {
        /**
         * 下载成功
         */
        void onDownloadSuccess();

        /**
         * 下载进度
         */
        void onDownloading(long progress);

        /**
         * 文件大小
         */
        void onDownloadLength(long max, int size);

        /**
         * 下载失败
         */
        void onDownloadFailed();
    }

}
