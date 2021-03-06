package com.alibaba.mesh.demo;

import com.alibaba.fastjson.JSON;

import okhttp3.ConnectionPool;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.commons.lang3.RandomStringUtils;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * @author yiji
 */
public class HttpInvoker {

    static Random r = new Random(System.currentTimeMillis());
    private static OkHttpClient client;
    private static int invokeTimes = 10000;
    private static int j = 0;

    static {
        init();
    }

    public static void main(String[] args) throws Exception {
        // init();
        for (int i = 0; i < invokeTimes; i++) {
            invoke();
        }

        System.out.println("Invoke times " + j + "/" + invokeTimes + "");
    }

    public static void init() {

        // 使用100个连接，默认是5个。
        // okhttp使用http 1.1，默认打开keep-alive
        ConnectionPool pool = new ConnectionPool(256, 5L, TimeUnit.MINUTES);

        client = new OkHttpClient.Builder()
                .connectionPool(pool)
                .connectTimeout(60, TimeUnit.SECONDS)       //设置连接超时
                .readTimeout(60, TimeUnit.SECONDS)          //不考虑超时
                .writeTimeout(60, TimeUnit.SECONDS)          //不考虑超时
                .retryOnConnectionFailure(true)
                .build();
    }

    public static void invoke() throws Exception {


        //String str = "abcedefghijklmnopqrstuvwxyz0123456789abcedefghijklmnopqrstuvwxyz0123456789abcedefghijklmnopqrstuvwxyz0123456789abcedefghijklmnopqrstuvwxyz0123456789";//RandomStringUtils.random(r.nextInt(1024), true, true);

        String str = RandomStringUtils.random(r.nextInt(1024), true, true);
//      String str="HdZqgR7rnv5e04BgEhiKRf24h2uIJeOMT8EDhlRJnm7ovj46vYQomSHIafZ5Xtqz1FRg3LclSyPs90bSq2P8ajhLo27ojRRgoZKDUiEKqzHEGYYxeNyV2bYr8mOVDIWNuIGWJGKvr5UQ4GODkY7YPslCK8Cpfbhu8nECeiKNyi2LQbRlHwJh7LHvcms0mLTAIAgSPSiUlVtipxkPDXh2n7ZQHgt5PUIg9LJEvI5bNnMScpBY15i7UFWWdFUgCbIzzFEdYvKvDka7Xdpszpe84MM5imVne7f9Mu3MooYCRjTre27wDtWB75SVZaxwV9zTgzKbPijeII8I4uv3Sq3SNBUgcAqTPLoG2w8RLuRwUsxsbwxs0weNKay8YjgqLYiOMewmw0sSCabOr1lEWxGMpH0z5DFhfA10hn329fNbUBc1H615si8oIqBBqqE33GIPevCqtqIcxiLYPN0";
        String url = "http://127.0.0.1:20000";

        RequestBody formBody = new FormBody.Builder()
                .add("interface", "com.alibaba.dubbo.performance.demo.provider.IHelloService")
                .add("method", "hash")
                .add("parameterTypesString", "Ljava/lang/String;")
                .add("parameter", str)
                .build();

        // String tt = "interface=com.alibaba.dubbo.performance.demo.provider.IHelloService&method=hash&parameterTypesString=Ljava/lang/String;&parameter=";

        Request request = new Request.Builder()
                .url(url)
                .post(formBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
            byte[] bytes = response.body().bytes();
            int hash = JSON.parseObject(bytes, Integer.class);
            int expectHash = str.hashCode();
            if (hash != expectHash) {
                System.out.println("----->>> expected hash: " + expectHash + " str:" + str + " ~~~~actual: " + hash);
            }
            j++;
        }
    }

}
