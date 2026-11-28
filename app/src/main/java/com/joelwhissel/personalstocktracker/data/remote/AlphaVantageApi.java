package com.joelwhissel.personalstocktracker.data.remote;

import com.joelwhissel.personalstocktracker.data.remote.dto.DailyDto;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Alpha Vantage REST API, used only for daily price history.
 *
 * <p>The API key is added by {@link ApiClient} as the {@code apikey} query parameter.
 * A rate-limit body ({@code "Note"} or {@code "Information"}) is turned into an
 * {@link ApiException} by {@link ApiErrorInterceptor}. Call {@code execute()} off the main
 * thread.
 */
public interface AlphaVantageApi {

    /**
     * Last 100 trading days for one symbol ({@code outputsize=compact}), which covers the
     * 30-day indicators and the slow-mover check.
     */
    @GET("query?function=TIME_SERIES_DAILY&outputsize=compact")
    Call<DailyDto> getDailySeries(@Query("symbol") String symbol);
}
