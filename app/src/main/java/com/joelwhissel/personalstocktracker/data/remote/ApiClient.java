package com.joelwhissel.personalstocktracker.data.remote;

import androidx.annotation.VisibleForTesting;

import com.joelwhissel.personalstocktracker.BuildConfig;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Builds the two Retrofit clients. One instance is shared by the app.
 * <ul>
 *   <li>API key added by {@link ApiKeyInterceptor}
 *   <li>HTTP 429, 5xx and Alpha Vantage rate-limit bodies become an {@link ApiException}
 *       through {@link ApiErrorInterceptor}</li>
 * </ul>
 *
 * <pre>
 * ApiClient api = ApiClient.create();
 * Response&lt;QuoteDto&gt; r = api.finnhub().getQuote("AAPL").execute();   // off the main thread
 * </pre>
 */
public final class ApiClient {

    public static final String FINNHUB_BASE_URL = "https://finnhub.io/api/v1/";
    public static final String ALPHA_VANTAGE_BASE_URL = "https://www.alphavantage.co/";
    public static final long TIMEOUT_MS = 10_000L;

    static final String FINNHUB_KEY_HEADER = "X-Finnhub-Token";
    static final String ALPHA_VANTAGE_KEY_PARAM = "apikey";

    private final FinnhubApi finnhubApi;
    private final AlphaVantageApi alphaVantageApi;

    public static ApiClient create() {
        return new ApiClient(FINNHUB_BASE_URL, BuildConfig.FINNHUB_API_KEY,
                ALPHA_VANTAGE_BASE_URL, BuildConfig.ALPHA_VANTAGE_API_KEY,
                BuildConfig.DEBUG, TIMEOUT_MS);
    }

    /** For tests: point both clients at a MockWebServer and use a short timeout. */
    @VisibleForTesting
    ApiClient(String finnhubBaseUrl, String finnhubKey,
              String alphaVantageBaseUrl, String alphaVantageKey,
              boolean debugLogging, long timeoutMs) {
        OkHttpClient base = new OkHttpClient.Builder()
                .connectTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .readTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .writeTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .callTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .build();

        OkHttpClient finnhubHttp = withInterceptors(base,
                ApiKeyInterceptor.header(FINNHUB_KEY_HEADER, finnhubKey),
                ApiErrorInterceptor.forFinnhub(), debugLogging);
        OkHttpClient alphaVantageHttp = withInterceptors(base,
                ApiKeyInterceptor.queryParam(ALPHA_VANTAGE_KEY_PARAM, alphaVantageKey),
                ApiErrorInterceptor.forAlphaVantage(), debugLogging);

        GsonConverterFactory gson = GsonConverterFactory.create();
        finnhubApi = new Retrofit.Builder()
                .baseUrl(finnhubBaseUrl)
                .client(finnhubHttp)
                .addConverterFactory(gson)
                .build()
                .create(FinnhubApi.class);
        alphaVantageApi = new Retrofit.Builder()
                .baseUrl(alphaVantageBaseUrl)
                .client(alphaVantageHttp)
                .addConverterFactory(gson)
                .build()
                .create(AlphaVantageApi.class);
    }

    public FinnhubApi finnhub() {
        return finnhubApi;
    }

    public AlphaVantageApi alphaVantage() {
        return alphaVantageApi;
    }

    /**
     * Both clients share the base client's connection pool and thread pool. Interceptor order:
     * key first, then error mapping, then logging.
     */
    private static OkHttpClient withInterceptors(OkHttpClient base, ApiKeyInterceptor key,
                                                 ApiErrorInterceptor errors, boolean debugLogging) {
        OkHttpClient.Builder builder = base.newBuilder()
                .addInterceptor(key)
                .addInterceptor(errors);
        if (debugLogging) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BASIC);
            logging.redactHeader(FINNHUB_KEY_HEADER);
            logging.redactQueryParams(ALPHA_VANTAGE_KEY_PARAM);
            builder.addInterceptor(logging);
        }
        return builder.build();
    }
}
