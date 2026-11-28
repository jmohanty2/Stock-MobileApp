package com.joelwhissel.personalstocktracker.data.remote;

import androidx.annotation.NonNull;

import java.io.IOException;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Adds an API key to every request, either as a header (Finnhub) or as a query parameter
 * (Alpha Vantage). Keys come from BuildConfig via {@link ApiClient#create()}
 */
public final class ApiKeyInterceptor implements Interceptor {

    private final String name;
    private final String key;
    private final boolean asHeader;

    private ApiKeyInterceptor(String name, String key, boolean asHeader) {
        this.name = name;
        this.key = key == null ? "" : key;
        this.asHeader = asHeader;
    }

    public static ApiKeyInterceptor header(String headerName, String key) {
        return new ApiKeyInterceptor(headerName, key, true);
    }

    public static ApiKeyInterceptor queryParam(String paramName, String key) {
        return new ApiKeyInterceptor(paramName, key, false);
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request request = chain.request();
        Request.Builder builder = request.newBuilder();
        if (asHeader) {
            builder.header(name, key);
        } else {
            HttpUrl url = request.url().newBuilder().setQueryParameter(name, key).build();
            builder.url(url);
        }
        return chain.proceed(builder.build());
    }
}
