package com.joelwhissel.personalstocktracker.data.remote;

import com.joelwhissel.personalstocktracker.data.local.PriceHistoryEntity;
import com.joelwhissel.personalstocktracker.data.local.StockEntity;
import com.joelwhissel.personalstocktracker.data.remote.dto.DailyDto;
import com.joelwhissel.personalstocktracker.data.remote.dto.QuoteDto;
import com.joelwhissel.personalstocktracker.domain.DailyPrice;
import com.joelwhissel.personalstocktracker.domain.StockQuote;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Maps API DTOs to Room entities and domain objects. */
public final class RemoteMapper {

    private RemoteMapper() {
    }

    /** Copies a quote into a stock row; {@code t} (Unix s) becomes lastUpdated in ms. */
    public static void applyQuote(StockEntity entity, QuoteDto quote) {
        entity.currentPrice = quote.currentPrice;
        entity.change = orZero(quote.change);
        entity.changePercent = orZero(quote.changePercent);
        entity.open = quote.open;
        entity.dayHigh = quote.dayHigh;
        entity.dayLow = quote.dayLow;
        entity.previousClose = quote.previousClose;
        entity.lastUpdated = quote.timestamp * 1000L;
    }

    public static StockQuote toStockQuote(String ticker, String companyName, QuoteDto quote) {
        return new StockQuote(ticker, companyName, quote.currentPrice,
                orZero(quote.change), orZero(quote.changePercent),
                quote.open, quote.dayHigh, quote.dayLow, quote.previousClose,
                quote.timestamp * 1000L);
    }

    /**
     * One {@link DailyPrice} per trading day, oldest first. Days with a missing or
     * non-numeric close are skipped. Empty for an error body.
     */
    public static List<DailyPrice> toDailyPrices(DailyDto dto) {
        if (dto == null || dto.hasError()) return Collections.emptyList();
        List<DailyPrice> out = new ArrayList<>();
        // yyyy-MM-dd sorts chronologically as text
        for (Map.Entry<String, DailyDto.Bar> e : new TreeMap<>(dto.timeSeries).entrySet()) {
            DailyDto.Bar bar = e.getValue();
            if (bar == null) continue;
            Double close = parseDouble(bar.close);
            if (close == null) continue;
            out.add(new DailyPrice(e.getKey(), close,
                    orZero(parseDouble(bar.high)), orZero(parseDouble(bar.low)),
                    parseLong(bar.volume)));
        }
        return out;
    }

    /** {@link #toDailyPrices} as price_history rows for one ticker, oldest first. */
    public static List<PriceHistoryEntity> toHistoryEntities(String ticker, DailyDto dto) {
        List<PriceHistoryEntity> out = new ArrayList<>();
        for (DailyPrice p : toDailyPrices(dto)) {
            PriceHistoryEntity row = new PriceHistoryEntity();
            row.ticker = ticker;
            row.date = p.getDate();
            row.close = p.getClose();
            row.high = p.getHigh();
            row.low = p.getLow();
            row.volume = p.getVolume();
            out.add(row);
        }
        return out;
    }

    private static double orZero(Double value) {
        return value == null ? 0.0 : value;
    }

    private static Double parseDouble(String s) {
        if (s == null) return null;
        try {
            double v = Double.parseDouble(s.trim());
            return Double.isNaN(v) || Double.isInfinite(v) ? null : v;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static long parseLong(String s) {
        if (s == null) return 0L;
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
