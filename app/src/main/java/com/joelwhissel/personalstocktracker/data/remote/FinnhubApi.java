package com.joelwhissel.personalstocktracker.data.remote;

import com.joelwhissel.personalstocktracker.data.remote.dto.QuoteDto;
import com.joelwhissel.personalstocktracker.data.remote.dto.SearchDto;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Finnhub REST API (free tier: 60 calls per minute, US stocks and ETFs).
 *
 * <p>The API key is added by {@link ApiClient} as the {@code X-Finnhub-Token} header, so it
 * never appears in a URL. Call {@code execute()} off the main thread.
 */
public interface FinnhubApi {

    /**
     * Current quote for one symbol. An unknown symbol is not an HTTP error: Finnhub returns
     * {@code c = 0} and {@code pc = 0}.
     */
    @GET("quote")
    Call<QuoteDto> getQuote(@Query("symbol") String symbol);

    /**
     * Symbol search. Returns partial matches too, so keep only the exact symbol with
     * {@link SearchDto#findExactMatch(String)}.
     */
    @GET("search")
    Call<SearchDto> search(@Query("q") String query);
}
