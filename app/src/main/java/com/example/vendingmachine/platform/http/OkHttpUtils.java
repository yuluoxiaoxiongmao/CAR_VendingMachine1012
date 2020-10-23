package com.example.vendingmachine.platform.http;

import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

public class OkHttpUtils {

    private final static String RESPONSE_CACHE = "alex_http_cache";
    private final static long RESPONSE_CACHE_SIZE = 10 * 1024 * 1024;
    private final static long HTTP_CONNECT_TIMEOUT = 30;
    private final static long HTTP_READ_TIMEOUT = 30;

    private static OkHttpClient singleton;

    public static OkHttpClient getInstance() {
        if (singleton == null) {
            synchronized (OkHttpUtils.class) {
                if (singleton == null) {
                    //定义logging
                    HttpLoggingInterceptor logInterceptor = new HttpLoggingInterceptor();
                    logInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
                    OkHttpClient.Builder builder = new OkHttpClient.Builder();
                    builder.connectTimeout(HTTP_CONNECT_TIMEOUT, TimeUnit.SECONDS)
                            .readTimeout(HTTP_READ_TIMEOUT, TimeUnit.SECONDS)

                            .addInterceptor(logInterceptor);//.addInterceptor(new RequestInterceptor())

                    singleton = builder.build();
                }
            }
        }
        return singleton;
    }

}
