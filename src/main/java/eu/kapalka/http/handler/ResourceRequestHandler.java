package eu.kapalka.http.handler;

import eu.kapalka.http.request.ValidRequest;
import eu.kapalka.http.response.Response;

/**
 * Represents a handler of valid HTTP requests. Each handler is bound to a specific prefix of the URL path
 * (see {@link TopLevelRequestHandler}).
 */
public interface ResourceRequestHandler {

    /**
     * Processes the given HTTP requests and returns an HTTP response that will be sent back to the client.
     * Note that <code>request</code> contains the original request URI, as sent by the HTTP client, while
     * <code>relativePath</code> is the URI path (without fragment or parameters) relative to the prefix
     * under which the handler is registered in {@link TopLevelRequestHandler}.
     *
     * @param request valid HTTP request
     * @param relativePath URI path relative to the prefix under which this handler is registered
     * @return HTTP response to be sent back to the client
     */
    Response handle(ValidRequest request, String relativePath);
}
