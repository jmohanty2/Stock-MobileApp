package com.joelwhissel.personalstocktracker.data.remote.dto;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Locale;

/**
 * Finnhub {@code /search} response.
 *
 * <pre>
 * {"count":2,"result":[{"description":"APPLE INC","displaySymbol":"AAPL",
 *                       "symbol":"AAPL","type":"Common Stock"}, ...]}
 * </pre>
 */
public class SearchDto {

    @SerializedName("count")
    public int count;

    @SerializedName("result")
    public List<Result> result;

    public static class Result {
        /** Company name, e.g. "APPLE INC". */
        @SerializedName("description")
        public String description;

        @SerializedName("displaySymbol")
        public String displaySymbol;

        @SerializedName("symbol")
        public String symbol;

        @SerializedName("type")
        public String type;
    }

    /**
     * The result whose symbol equals {@code ticker} (case-insensitive, trimmed), or null.
     * Partial matches such as "AAPL.SW" for "AAPL" are ignored.
     */
    @Nullable
    public Result findExactMatch(String ticker) {
        if (ticker == null || result == null) return null;
        String wanted = ticker.trim().toUpperCase(Locale.US);
        for (Result r : result) {
            if (r != null && r.symbol != null
                    && r.symbol.trim().toUpperCase(Locale.US).equals(wanted)) {
                return r;
            }
        }
        return null;
    }
}
