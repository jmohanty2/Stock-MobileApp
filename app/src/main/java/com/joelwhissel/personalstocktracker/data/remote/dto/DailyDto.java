package com.joelwhissel.personalstocktracker.data.remote.dto;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

/**
 * Alpha Vantage {@code TIME_SERIES_DAILY} response. Alpha Vantage sends all
 * numbers as strings.
 *
 * <pre>
 * {"Meta Data": {...},
 *  "Time Series (Daily)": {
 *    "2026-11-26": {"1. open":"188.00","2. high":"190.32","3. low":"187.50",
 *                   "4. close":"189.84","5. volume":"41234567"}, ...}}
 * </pre>
 *
 * An unknown symbol returns {@code {"Error Message": "..."}} with HTTP 200. Rate-limit bodies
 * ({@code "Note"} / {@code "Information"}) never reach this class: ApiErrorInterceptor
 * throws first.
 */
public class DailyDto {

    /** Date (yyyy-MM-dd) to that day's prices; null when the body is an error. */
    @SerializedName("Time Series (Daily)")
    public Map<String, Bar> timeSeries;

    @SerializedName("Error Message")
    public String errorMessage;

    public static class Bar {
        @SerializedName("1. open")
        public String open;

        @SerializedName("2. high")
        public String high;

        @SerializedName("3. low")
        public String low;

        @SerializedName("4. close")
        public String close;

        @SerializedName("5. volume")
        public String volume;
    }

    /** True for an "Error Message" body or a body without a time series. */
    public boolean hasError() {
        return errorMessage != null || timeSeries == null;
    }
}
