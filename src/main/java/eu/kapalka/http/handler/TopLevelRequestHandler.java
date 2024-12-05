package eu.kapalka.http.handler;

import eu.kapalka.http.request.HttpMethod;
import eu.kapalka.http.request.InvalidRequest;
import eu.kapalka.http.request.Request;
import eu.kapalka.http.request.ValidRequest;
import eu.kapalka.http.response.HttpStatus;
import eu.kapalka.http.response.Response;
import eu.kapalka.http.response.ResponseWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Root handler of HTTP requests. For valid HTTP requests, it uses a {@link ResourceRequestHandler} attached
 * to the corresponding URI path prefix (see {@link #registerRequestHandler(String, ResourceRequestHandler)})
 * to generate the HTTP response, and then sends the response to the HTTP client. Invalid HTTP requests are handled
 * directly by this class.
 */
public class TopLevelRequestHandler {

    // List is fine assuming a small number of handlers
    private final List<RegisteredRequestHandler> requestHandlers = new ArrayList<>();

    /**
     * Register <code>requestHandler</code> that will handle all requests with URI path starting with the given
     * prefix <code>pathPrefix</code>.
     */
    public void registerRequestHandler(String pathPrefix, ResourceRequestHandler requestHandler) {
        requestHandlers.add(new RegisteredRequestHandler(pathPrefix, requestHandler));
    }

    /**
     * Processes the given HTTP request and sends a response back to the client.
     *
     * @param request parsed HTTP request
     * @param responseWriter sink where to write the HTTP response
     * @return whether the connection should be kept open after the request (keep-alive),
     * @throws IOException on issues with the network connection or data transfer (e.g., file system to socket)
     */
    public boolean handleRequest(Request request, ResponseWriter responseWriter) throws IOException {
        return switch (request) {
            case ValidRequest req -> handleValidRequest(req, responseWriter);
            case InvalidRequest req -> handleInvalidRequest(req, responseWriter);
        };
    }

    private boolean handleValidRequest(ValidRequest request, ResponseWriter responseWriter) throws IOException {
        var handlerOpt = findHandler(request);
        var response = handlerOpt.map(handler -> callRequestHandler(handler, request))
                .orElseGet(() -> noHandlerErrorResponse(request));

        var keepAlive = isKeepAliveEnabled(request);
        responseWriter.writeHeaders(response, keepAlive);

        if (request.getMethod() != HttpMethod.HEAD) {
            responseWriter.writeBody(response);
        }

        return keepAlive;
    }

    private Optional<RegisteredRequestHandler> findHandler(ValidRequest request) {
        var requestPath = request.getURI().getPath();
        return requestHandlers.stream()
                .filter(handler -> handler.pathPrefix.equals("/") || requestPath.startsWith(handler.pathPrefix))
                .findFirst();
    }

    private Response callRequestHandler(RegisteredRequestHandler handler, ValidRequest request) {
        var relativeRequestPath = request.getURI().getPath().substring(handler.pathPrefix.length());
        return handler.requestHandler.handle(request, relativeRequestPath);
    }

    private Response noHandlerErrorResponse(ValidRequest request) {
        return Response.builder()
                .status(HttpStatus.NOT_FOUND)
                .body("No handler for path %s".formatted(request.getURI().getPath()))
                .build();
    }

    private boolean isKeepAliveEnabled(ValidRequest request) {
        var keepAliveHeader = request.getKeepAliveHeader();
        if (keepAliveHeader != null) {
            return keepAliveHeader; // Keep-alive explicitly enabled or disabled by the client
        }
        if (request.getHttpMinorVersion() == 0) {
            return false; // HTTP 1.0 default
        }
        return true; // HTTP 1.1 default
    }

    private boolean handleInvalidRequest(InvalidRequest requestError, ResponseWriter responseWriter) throws IOException {
        var response = Response.builder()
                .status(requestError.statusCode())
                .body(requestError.errorMessage())
                .build();
        responseWriter.writeFull(response, false);
        return false; // No reason to keep the connection open when we receive invalid request
    }

    private record RegisteredRequestHandler(String pathPrefix, ResourceRequestHandler requestHandler) {
    }
}
