package com.joelwhissel.personalstocktracker.data.local;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The stock-row deletion rule (design doc 7, UC3): removing a stock from the watchlist only
 * hides it; the row, with its alerts and history, is deleted once it is in neither the
 * watchlist nor the portfolio.
 */
@RunWith(AndroidJUnit4.class)
public class StockDeletionRuleTest {

    private AppDatabase db;
    private StockDao stockDao;
    private HoldingDao holdingDao;
    private AlertDao alertDao;
    private PriceHistoryDao historyDao;

    @Before
    public void createDb() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        stockDao = db.stockDao();
        holdingDao = db.holdingDao();
        alertDao = db.alertDao();
        historyDao = db.priceHistoryDao();
    }

    @After
    public void closeDb() {
        db.close();
    }

    @Test
    public void purgeOrphans_deletesUnwatchedUnheldRowWithItsAlertsAndHistory() {
        insertStock("AAPL", false);
        insertAlert("AAPL");
        insertHistory("AAPL", "2026-11-24");

        assertEquals(1, stockDao.purgeOrphans());

        assertNull(stockDao.getStock("AAPL"));
        assertTrue(alertDao.getActiveSync().isEmpty());
        assertNull(historyDao.latestDate("AAPL"));
    }

    @Test
    public void purgeOrphans_keepsHeldRowRemovedFromWatchlist() {
        insertStock("MSFT", false);
        insertHolding("MSFT");
        insertAlert("MSFT");

        assertEquals(0, stockDao.purgeOrphans());

        assertNotNull(stockDao.getStock("MSFT"));
        assertEquals(1, alertDao.getActiveSync().size());
    }

    @Test
    public void purgeOrphans_keepsWatchedRow() {
        insertStock("NVDA", true);

        assertEquals(0, stockDao.purgeOrphans());
        assertNotNull(stockDao.getStock("NVDA"));
    }

    @Test
    public void undoBeforePurge_keepsAlertsAndHistory() {
        insertStock("AAPL", true);
        insertAlert("AAPL");
        insertHistory("AAPL", "2026-11-24");

        stockDao.setInWatchlist("AAPL", false);   // Remove
        stockDao.setInWatchlist("AAPL", true);    // Undo within 4 s
        stockDao.purgeIfOrphan("AAPL");           // Undo window closes

        assertNotNull(stockDao.getStock("AAPL"));
        assertEquals(1, alertDao.getActiveSync().size());
        assertEquals("2026-11-24", historyDao.latestDate("AAPL"));
    }

    @Test
    public void purgeIfOrphan_onlyDeletesTheGivenTicker() {
        insertStock("AAPL", false);   // Undo window closed
        insertStock("TSLA", false);   // still inside its own Undo window

        assertEquals(1, stockDao.purgeIfOrphan("AAPL"));

        assertNull(stockDao.getStock("AAPL"));
        assertNotNull(stockDao.getStock("TSLA"));
    }

    @Test
    public void purgeIfOrphan_keepsHeldOrWatchedTicker() {
        insertStock("MSFT", false);
        insertHolding("MSFT");
        insertStock("NVDA", true);

        assertEquals(0, stockDao.purgeIfOrphan("MSFT"));
        assertEquals(0, stockDao.purgeIfOrphan("NVDA"));
    }

    @Test
    public void removingLastHolding_makesUnwatchedRowAnOrphan() {
        insertStock("MSFT", false);
        insertHolding("MSFT");

        HoldingEntity holding = holdingDao.get("MSFT");
        assertNotNull(holding);
        holdingDao.delete(holding);

        assertNull(holdingDao.get("MSFT"));
        assertEquals(1, stockDao.purgeIfOrphan("MSFT"));
    }

    @Test
    public void trackedQueries_excludeRowsInUndoWindow() {
        insertStock("AAPL", true);    // watchlist
        insertStock("MSFT", false);   // portfolio only
        insertHolding("MSFT");
        insertStock("TSLA", false);   // removed, Undo window open

        assertEquals(2, stockDao.countTracked());
        List<String> tickers = new ArrayList<>();
        for (StockEntity s : stockDao.getTracked()) {
            tickers.add(s.ticker);
        }
        assertEquals(Arrays.asList("AAPL", "MSFT"), tickers);
    }

    @Test
    public void latestDate_returnsNewestCachedDate() {
        insertStock("AAPL", true);
        assertNull(historyDao.latestDate("AAPL"));

        insertHistory("AAPL", "2026-11-20");
        insertHistory("AAPL", "2026-11-24");
        insertHistory("AAPL", "2026-11-21");

        assertEquals("2026-11-24", historyDao.latestDate("AAPL"));
    }

    // --- helpers ---

    private void insertStock(String ticker, boolean inWatchlist) {
        StockEntity s = new StockEntity();
        s.ticker = ticker;
        s.companyName = ticker + " Inc.";
        s.currentPrice = 100;
        s.inWatchlist = inWatchlist;
        stockDao.insert(s);
    }

    private void insertHolding(String ticker) {
        HoldingEntity h = new HoldingEntity();
        h.ticker = ticker;
        h.shares = 10;
        h.avgBuyPrice = 90;
        holdingDao.insert(h);
    }

    private void insertAlert(String ticker) {
        PriceAlertEntity a = new PriceAlertEntity();
        a.ticker = ticker;
        a.type = "PERCENT";
        a.direction = "UP";
        a.targetValue = 5;
        a.basePrice = 100;
        alertDao.insert(a);
    }

    private void insertHistory(String ticker, String date) {
        PriceHistoryEntity row = new PriceHistoryEntity();
        row.ticker = ticker;
        row.date = date;
        row.close = row.high = row.low = 100;
        historyDao.insertAll(Collections.singletonList(row));
    }
}
