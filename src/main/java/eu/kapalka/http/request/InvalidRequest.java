package eu.kapalka.http.request;

import eu.kapalka.http.response.HttpStatus;

/**
 * Information about an invalid HTTP request, which is to be sent back to the HTTP client.
 */
public record InvalidRequest(HttpStatus statusCode, String errorMessage) implements Request {
}
