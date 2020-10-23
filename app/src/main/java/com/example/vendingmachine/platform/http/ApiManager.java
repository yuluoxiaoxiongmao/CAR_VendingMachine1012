package com.example.vendingmachine.platform.http;


import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class ApiManager {

    private static ApiManager mInstance;

    private ApiService mApiService;
    private final Retrofit mRetrofit;

    public ApiManager() {
        mRetrofit = new Retrofit.Builder()
                .baseUrl(HttpUrl.SERVER_URL)
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .client(OkHttpUtils.getInstance())
                .build();
        mApiService = mRetrofit.create(ApiService.class);
    }

    public static ApiService getService() {
        if (mInstance == null) {
            mInstance = new ApiManager();
        }
        return mInstance.mApiService;
    }


}
