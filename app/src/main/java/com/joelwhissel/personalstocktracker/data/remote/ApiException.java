package com.joelwhissel.personalstocktracker.data.remote;

import java.io.IOException;
import java.io.Serial;

/**
 * An HTTP 429 or 5xx response, or an Alpha Vantage rate-limit body.
 *
 * <p>It is an {@link IOException}, so the repository maps it to
 * {@code ErrorType.NETWORK_ERROR} in the same catch block as a timeout or lost connection.
 */
public class ApiException extends IOException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final int httpCode;
    private final boolean rateLimited;

    public ApiException(int httpCode, boolean rateLimited, String message) {
        super(message);
        this.httpCode = httpCode;
        this.rateLimited = rateLimited;
    }

    /** HTTP status of the response (200 for an Alpha Vantage rate-limit body). */
    public int getHttpCode() {
        return httpCode;
    }

    /** True for HTTP 429 and for an Alpha Vantage "Note" / "Information" body. */
    public boolean isRateLimited() {
        return rateLimited;
    }
}
