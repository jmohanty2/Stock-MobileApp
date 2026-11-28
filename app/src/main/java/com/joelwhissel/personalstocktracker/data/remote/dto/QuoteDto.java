package com.joelwhissel.personalstocktracker.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Finnhub {@code /quote} response.
 *
 * <pre>
 * {"c":189.84,"d":1.62,"dp":0.8607,"h":190.32,"l":187.5,"o":188.0,"pc":188.22,"t":1764190800}
 * </pre>
 *
 * For an unknown symbol Finnhub returns zeros and {@code null} for {@code d} and {@code dp}.
 */
public class QuoteDto {

    @SerializedName("c")
    public double currentPrice;

    /** Change in $ from the previous close; null for an unknown symbol. */
    @SerializedName("d")
    public Double change;

    /** Change in % from the previous close; null for an unknown symbol. */
    @SerializedName("dp")
    public Double changePercent;

    @SerializedName("h")
    public double dayHigh;

    @SerializedName("l")
    public double dayLow;

    @SerializedName("o")
    public double open;

    @SerializedName("pc")
    public double previousClose;

    /** Quote time in Unix seconds. */
    @SerializedName("t")
    public long timestamp;

    /** {@code c = 0} and {@code pc = 0}: Finnhub has no data, treat as Stock Not Found. */
    public boolean isNotFound() {
        return currentPrice == 0 && previousClose == 0;
    }
}
