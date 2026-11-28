package com.joelwhissel.personalstocktracker.data.remote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import com.joelwhissel.personalstocktracker.data.local.PriceHistoryEntity;
import com.joelwhissel.personalstocktracker.data.local.StockEntity;
import com.joelwhissel.personalstocktracker.data.remote.dto.DailyDto;
import com.joelwhissel.personalstocktracker.data.remote.dto.QuoteDto;
import com.joelwhissel.personalstocktracker.domain.DailyPrice;
import com.joelwhissel.personalstocktracker.domain.StockQuote;

import org.junit.Test;

import java.util.List;

/** DTO to entity and domain mapping, design doc 7.2. */
public class RemoteMapperTest {

    private static final double DELTA = 1e-9;
    private final Gson gson = new Gson();

    private QuoteDto quote(String fixture) {
        return gson.fromJson(ApiClientContractTest.readFixture(fixture), QuoteDto.class);
    }

    private DailyDto daily(String fixture) {
        return gson.fromJson(ApiClientContractTest.readFixture(fixture), DailyDto.class);
    }

    @Test
    public void applyQuote_copiesFields_andConvertsSecondsToMillis() {
        StockEntity e = new StockEntity();
        e.ticker = "AAPL";
        e.inWatchlist = true;

        RemoteMapper.applyQuote(e, quote("quote_aapl.json"));

        assertEquals(189.84, e.currentPrice, DELTA);
        assertEquals(1.62, e.change, DELTA);
        assertEquals(0.8607, e.changePercent, DELTA);
        assertEquals(188.0, e.open, DELTA);
        assertEquals(190.32, e.dayHigh, DELTA);
        assertEquals(187.5, e.dayLow, DELTA);
        assertEquals(188.22, e.previousClose, DELTA);
        assertEquals(1764190800_000L, e.lastUpdated);
        assertEquals("AAPL", e.ticker);       // untouched
        assertTrue(e.inWatchlist);            // untouched
    }

    @Test
    public void toStockQuote_mapsFields() {
        StockQuote q = RemoteMapper.toStockQuote("AAPL", "APPLE INC", quote("quote_aapl.json"));

        assertEquals("AAPL", q.getTicker());
        assertEquals("APPLE INC", q.getCompanyName());
        assertEquals(189.84, q.getPrice(), DELTA);
        assertEquals(1.62, q.getChange(), DELTA);
        assertEquals(0.8607, q.getChangePercent(), DELTA);
        assertEquals(188.22, q.getPreviousClose(), DELTA);
        assertEquals(1764190800_000L, q.getLastUpdated());
    }

    @Test
    public void toStockQuote_nullChange_becomesZero() {
        StockQuote q = RemoteMapper.toStockQuote("ZZZZ", null, quote("quote_unknown.json"));

        assertEquals(0.0, q.getChange(), DELTA);
        assertEquals(0.0, q.getChangePercent(), DELTA);
    }

    @Test
    public void toDailyPrices_oldestFirst_skipsNonNumericClose() {
        List<DailyPrice> prices = RemoteMapper.toDailyPrices(daily("daily_aapl.json"));

        assertEquals(3, prices.size());
        assertEquals("2026-11-24", prices.get(0).getDate());
        assertEquals("2026-11-25", prices.get(1).getDate());
        assertEquals("2026-11-26", prices.get(2).getDate());

        DailyPrice last = prices.get(2);
        assertEquals(189.84, last.getClose(), DELTA);
        assertEquals(190.32, last.getHigh(), DELTA);
        assertEquals(187.5, last.getLow(), DELTA);
        assertEquals(41234567L, last.getVolume());
    }

    @Test
    public void toDailyPrices_errorBody_isEmpty() {
        assertTrue(RemoteMapper.toDailyPrices(daily("daily_error.json")).isEmpty());
        assertTrue(RemoteMapper.toDailyPrices(null).isEmpty());
    }

    @Test
    public void toHistoryEntities_oneRowPerDay_forTicker() {
        List<PriceHistoryEntity> rows =
                RemoteMapper.toHistoryEntities("AAPL", daily("daily_aapl.json"));

        assertEquals(3, rows.size());
        PriceHistoryEntity first = rows.get(0);
        assertEquals("AAPL", first.ticker);
        assertEquals("2026-11-24", first.date);
        assertEquals(186.5, first.close, DELTA);
        assertEquals(186.9, first.high, DELTA);
        assertEquals(184.2, first.low, DELTA);
        assertEquals(38900111L, first.volume);
    }
}
