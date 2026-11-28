package com.joelwhissel.personalstocktracker.data.remote;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.regex.Pattern;

import okhttp3.Interceptor;
import okhttp3.Response;

/**
 * Turns responses that mean "try again later" into an {@link ApiException}, so callers only
 * need to catch {@link IOException}:
 *
 * <ul>
 *   <li>HTTP 429: rate limited</li>
 *   <li>HTTP 5xx: server error</li>
 *   <li>Alpha Vantage only: an HTTP 200 body whose first key is {@code "Note"} or
 *       {@code "Information"}, which is how Alpha Vantage reports its rate limit</li>
 * </ul>
 *
 * Other 4xx responses are passed through, so the caller sees {@code response.isSuccessful()}
 * is false.
 */
public class ApiErrorInterceptor implements Interceptor {

    /** Rate-limit bodies are short; a price series starts with "Meta Data". */
    private static final long PEEK_BYTES = 256;
    private static final Pattern RATE_LIMIT_BODY =
            Pattern.compile("^\\s*\\{\\s*\"(Note|Information)\"\\s*:");

    private final boolean checkAlphaVantageBody;

    private ApiErrorInterceptor(boolean checkAlphaVantageBody) {
        this.checkAlphaVantageBody = checkAlphaVantageBody;
    }

    public static ApiErrorInterceptor forFinnhub() {
        return new ApiErrorInterceptor(false);
    }

    public static ApiErrorInterceptor forAlphaVantage() {
        return new ApiErrorInterceptor(true);
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Response response = chain.proceed(chain.request());
        int code = response.code();

        if (code == 429) {
            response.close();
            throw new ApiException(code, true, "HTTP 429: rate limited");
        }
        if (code >= 500) {
            response.close();
            throw new ApiException(code, false, "HTTP " + code + ": server error");
        }
        if (checkAlphaVantageBody && response.isSuccessful()) {
            String start = response.peekBody(PEEK_BYTES).string();
            if (RATE_LIMIT_BODY.matcher(start).find()) {
                response.close();
                throw new ApiException(code, true, "Alpha Vantage rate limit reached");
            }
        }
        return response;
    }
}
