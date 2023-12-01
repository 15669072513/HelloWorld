package com.antgroup.oss.gitprepushhook.faas;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.antgroup.oss.gitprepushhook.faas.po.FaasResult;
import lombok.SneakyThrows;
import okhttp3.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author yinzhennan
 */
public class FaasClient<T,R> {
    private final static OkHttpClient OK_HTTP_CLIENT = new OkHttpClient.Builder()
            .retryOnConnectionFailure(false)
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .build();
    String functionName;
    String functionNamespace;
    String env;

    TypeReference<FaasResult<R>> typeReference;

    public FaasClient(String functionName,String functionNamespace, String env, TypeReference<FaasResult<R>> typeReference) {
        this.functionName = functionName;
        this.functionNamespace = functionNamespace;
        this.env = env;
        this.typeReference = typeReference;
    }


    @SneakyThrows
    public R invoke() {
        return invoke("{}");
    }
    @SneakyThrows
    public R invoke(T param) {
        String jsonString = JSON.toJSONString(param);
        return invoke(jsonString);
    }
    @SneakyThrows
    private R invoke(String jsonString) {
        Map<String, Object> requestBodyJSON = new HashMap<>();
        requestBodyJSON.put("functionName", functionName);
        requestBodyJSON.put("functionNamespace", functionNamespace);
        requestBodyJSON.put("async", false);
        requestBodyJSON.put("env", env);
        requestBodyJSON.put("targetUrlMap", new HashMap<>());
        requestBodyJSON.put("params", jsonString);
        RequestBody body = RequestBody.create(JSON.toJSONString(requestBodyJSON), MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url("https://function.alipay.com/webapi/function/exe")
                .post(body)
                .build();
        try (Response response = OK_HTTP_CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                System.err.println("Save record to database failed:" + response.code());
                throw new FaasException("Http invoke got error:" + response.code());
            } else {
                if (response.body() != null) {
                    String responseString = response.body().string();
                    FaasResult<R> faasResult = JSON.parseObject(responseString, typeReference);
                    if (! faasResult.isSuccess()) {
                        throw new FaasException("Faas return error:"+faasResult.getMsg());
                    }
                    return faasResult.getData();
                }else{
                    throw new FaasException("Http return nothing:" + response.code());
                }
            }
        } catch (IOException e) {
            System.out.println(functionName+"调用FAAS失败"+functionNamespace+";"+env);
            throw new RuntimeException(e);
        }
    }

}
