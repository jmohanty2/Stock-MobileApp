package com.joelwhissel.personalstocktracker.data.remote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.joelwhissel.personalstocktracker.data.remote.dto.DailyDto;
import com.joelwhissel.personalstocktracker.data.remote.dto.QuoteDto;
import com.joelwhissel.personalstocktracker.data.remote.dto.SearchDto;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.mockwebserver.SocketPolicy;
import retrofit2.Response;

/**
 * API contract tests: saved Finnhub and Alpha Vantage responses served by MockWebServer,
 * parsed through the real ApiClient (design doc 11.2, "API contract").
 */
public class ApiClientContractTest {

    private static final double DELTA = 1e-9;
    private static final String FINNHUB_KEY = "test-finnhub-key";
    private static final String AV_KEY = "test-av-key";
    private static final long SHORT_TIMEOUT_MS = 300;

    private MockWebServer server;
    private ApiClient api;

    @Before
    public void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        String base = server.url("/").toString();
        api = new ApiClient(base, FINNHUB_KEY, base, AV_KEY, false, SHORT_TIMEOUT_MS);
    }

    @After
    public void tearDown() throws IOException {
        server.shutdown();
    }

    // ---------------------------------------------------------------- Finnhub /quote

    @Test
    public void quote_mapsAllFields() throws Exception {
        enqueueFixture("quote_aapl.json");

        Response<QuoteDto> response = api.finnhub().getQuote("AAPL").execute();

        assertTrue(response.isSuccessful());
        QuoteDto q = response.body();
        assertNotNull(q);
        assertEquals(189.84, q.currentPrice, DELTA);
        assertEquals(1.62, q.change, DELTA);
        assertEquals(0.8607, q.changePercent, DELTA);
        assertEquals(188.0, q.open, DELTA);
        assertEquals(190.32, q.dayHigh, DELTA);
        assertEquals(187.5, q.dayLow, DELTA);
        assertEquals(188.22, q.previousClose, DELTA);
        assertEquals(1764190800L, q.timestamp);
        assertFalse(q.isNotFound());
    }

    @Test
    public void quote_sendsSymbolAndKeyHeader_notKeyInUrl() throws Exception {
        enqueueFixture("quote_aapl.json");

        api.finnhub().getQuote("AAPL").execute();

        RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(request);
        HttpUrl url = request.getRequestUrl();
        assertEquals("/quote", url.encodedPath());
        assertEquals("AAPL", url.queryParameter("symbol"));
        assertEquals(FINNHUB_KEY, request.getHeader(ApiClient.FINNHUB_KEY_HEADER));
        assertFalse(url.toString().contains(FINNHUB_KEY));
    }

    @Test
    public void quote_unknownSymbol_isNotFound() throws Exception {
        enqueueFixture("quote_unknown.json");

        QuoteDto q = api.finnhub().getQuote("ZZZZ").execute().body();

        assertNotNull(q);
        assertTrue(q.isNotFound());
        assertNull(q.change);
        assertNull(q.changePercent);
    }

    // ---------------------------------------------------------------- Finnhub /search

    @Test
    public void search_keepsOnlyExactSymbolMatch() throws Exception {
        enqueueFixture("search_aapl.json");

        SearchDto s = api.finnhub().search("AAPL").execute().body();

        assertNotNull(s);
        assertEquals(3, s.count);
        SearchDto.Result match = s.findExactMatch(" aapl ");
        assertNotNull(match);
        assertEquals("AAPL", match.symbol);
        assertEquals("APPLE INC", match.description);
        assertNull(s.findExactMatch("AAP"));
        assertEquals("/search", server.takeRequest().getRequestUrl().encodedPath());
    }

    @Test
    public void search_emptyResult_hasNoMatch() throws Exception {
        enqueueFixture("search_empty.json");

        SearchDto s = api.finnhub().search("ZZZZ").execute().body();

        assertNotNull(s);
        assertEquals(0, s.count);
        assertNull(s.findExactMatch("ZZZZ"));
    }

    // ---------------------------------------------------------------- Alpha Vantage

    @Test
    public void daily_parsesSeries_andSendsFunctionSymbolAndKey() throws Exception {
        enqueueFixture("daily_aapl.json");

        DailyDto d = api.alphaVantage().getDailySeries("AAPL").execute().body();

        assertNotNull(d);
        assertFalse(d.hasError());
        assertEquals(4, d.timeSeries.size());
        assertEquals("189.8400", d.timeSeries.get("2026-11-26").close);

        HttpUrl url = server.takeRequest().getRequestUrl();
        assertEquals("/query", url.encodedPath());
        assertEquals("TIME_SERIES_DAILY", url.queryParameter("function"));
        assertEquals("compact", url.queryParameter("outputsize"));
        assertEquals("AAPL", url.queryParameter("symbol"));
        assertEquals(AV_KEY, url.queryParameter(ApiClient.ALPHA_VANTAGE_KEY_PARAM));
    }

    @Test
    public void daily_errorMessageBody_isParsedAsError() throws Exception {
        enqueueFixture("daily_error.json");

        DailyDto d = api.alphaVantage().getDailySeries("ZZZZ").execute().body();

        assertNotNull(d);
        assertTrue(d.hasError());
        assertNotNull(d.errorMessage);
    }

    @Test
    public void daily_noteBody_throwsRateLimited() throws Exception {
        enqueueFixture("daily_note.json");
        assertRateLimited(() -> api.alphaVantage().getDailySeries("AAPL").execute());
    }

    @Test
    public void daily_informationBody_throwsRateLimited() throws Exception {
        enqueueFixture("daily_information.json");
        assertRateLimited(() -> api.alphaVantage().getDailySeries("AAPL").execute());
    }

    @Test
    public void finnhub_bodyStartingWithNote_isNotCheckedForAlphaVantageLimit() throws Exception {
        server.enqueue(new MockResponse().setBody("{\"Note\":\"not a quote\"}"));

        Response<QuoteDto> response = api.finnhub().getQuote("AAPL").execute();

        assertTrue(response.isSuccessful());
    }

    // ---------------------------------------------------------------- HTTP errors

    @Test
    public void http429_throwsRateLimitedApiException() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(429));
        assertRateLimited(() -> api.finnhub().getQuote("AAPL").execute());
    }

    @Test
    public void http503_throwsApiException_notRateLimited() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(503));
        try {
            api.alphaVantage().getDailySeries("AAPL").execute();
            fail("expected ApiException");
        } catch (ApiException e) {
            assertEquals(503, e.getHttpCode());
            assertFalse(e.isRateLimited());
        }
    }

    @Test
    public void http403_isReturned_notThrown() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(403));

        Response<QuoteDto> response = api.finnhub().getQuote("AAPL").execute();

        assertFalse(response.isSuccessful());
        assertEquals(403, response.code());
    }

    @Test
    public void noResponse_timesOut_withIOException() {
        server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));
        try {
            api.finnhub().getQuote("AAPL").execute();
            fail("expected a timeout");
        } catch (InterruptedIOException expected) {
            // SocketTimeoutException or the call timeout: both map to NETWORK_ERROR
        } catch (IOException e) {
            fail("expected a timeout, got " + e);
        }
    }

    // ---------------------------------------------------------------- helpers

    private interface ThrowingCall {
        void run() throws IOException;
    }

    private static void assertRateLimited(ThrowingCall call) {
        try {
            call.run();
            fail("expected ApiException");
        } catch (ApiException e) {
            assertTrue(e.isRateLimited());
        } catch (IOException e) {
            fail("expected ApiException, got " + e);
        }
    }

    private void enqueueFixture(String name) {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(readFixture(name)));
    }

    static String readFixture(String name) {
        InputStream in = ApiClientContractTest.class.getClassLoader()
                .getResourceAsStream("remote/" + name);
        if (in == null) throw new IllegalArgumentException("missing fixture " + name);
        try (Scanner s = new Scanner(in, StandardCharsets.UTF_8.name()).useDelimiter("\\A")) {
            return s.hasNext() ? s.next() : "";
        }
    }
}
