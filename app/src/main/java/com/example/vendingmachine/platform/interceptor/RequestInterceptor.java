package com.example.vendingmachine.platform.interceptor;

import com.example.vendingmachine.utils.MD5;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.TreeMap;
import okhttp3.FormBody;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 *  2017/8/10.
 */

public class RequestInterceptor implements Interceptor {
    private static String TAG = "RequestInterceptor";

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();

       /* String requestUrl = request.url().toString();
        if (requestUrl.contains(HttpUrl.UPLOAD_PIC) || requestUrl.contains(HttpUrl.UPLOAD_VIDEO)) {
            return chain.proceed(request);
        }*/

        JsonObject requestJsonObj = new JsonObject();
        //请求时间
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String timeStr = sdf.format(System.currentTimeMillis());
        requestJsonObj.addProperty("createTime", timeStr);

        //获取data数据
        JsonObject dataJsonObj = getDataJsonObject(request.body());
        String dataString = dataJsonObj.toString().replaceAll("\\\\", "");
        requestJsonObj.add("data", dataJsonObj);

        //签名=MD5(time + data)
        requestJsonObj.addProperty("sign", MD5.getMessageDigest((timeStr + dataString).getBytes()));


        RequestBody newRequestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), requestJsonObj.toString().replaceAll("\\\\", ""));
        Request.Builder builder = request.newBuilder();
        builder.post(newRequestBody);
        return chain.proceed(builder.build());
    }

    private JsonObject getDataJsonObject(RequestBody requestBody) {
        TreeMap<String, String> sortedMap = new TreeMap<>();
        JsonObject jsonObject = new JsonObject();
        if (requestBody instanceof FormBody) {
            FormBody formBody = (FormBody) requestBody;
            for (int i = 0; i < formBody.size(); i++) {
                sortedMap.put(formBody.name(i), formBody.value(i));
            }

            for (String key : sortedMap.keySet()) {
                jsonObject.addProperty(key, sortedMap.get(key));
            }
        }
        return jsonObject;
    }
}
