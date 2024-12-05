package eu.kapalka.http.request;

import eu.kapalka.http.response.HttpStatus;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Pattern;

/**
 * HTTP 1.0/1.1 request parser.
 */
public class RequestParser {

    private static final Pattern PROTOCOL_REGEX = Pattern.compile("HTTP/1.(\\d)");

    // Not specified in any standard but seems in line with what web browsers enforce
    private static final int MAX_URI_LENGTH = 2048;

    private final LineReader lineReader;
    private ValidRequest.Builder requestBuilder;
    private Long bodyLength;

    public RequestParser(InputStream inputStream) {
        this.lineReader = new LineReader(inputStream);
    }

    public Request parse() throws IOException {
        try {
            requestBuilder = ValidRequest.builder();
            parseStartLine();
            parseHeaders();
            skipBody(); // We don't store body data (but adding this feature would be easy)
            return requestBuilder.build();
        } catch (InvalidRequestException ex) {
            return new InvalidRequest(ex.statusCode, ex.getMessage());
        }
    }

    private void parseStartLine() throws IOException {
        lineReader.nextLine();

        var methodName = lineReader.readToken();
        if (methodName == null) {
            throw invalidRequest("Missing HTTP method");
        }
        var method = HttpMethod.of(methodName);
        if (method == null) {
            throw invalidRequest("Invalid method: %s".formatted(methodName));
        }
        requestBuilder.method(method);

        var path = lineReader.readToken();
        if (path == null) {
            throw invalidRequest("Missing path (URI)");
        }
        if (path.length() > MAX_URI_LENGTH) {
            throw invalidRequest(HttpStatus.URI_TOO_LONG,
                    "URI length exceeds the allowed maximum of %d bytes".formatted(MAX_URI_LENGTH));
        }
        requestBuilder.uri(parsePathURI(path));

        var protocol = lineReader.readToken();
        if (protocol == null) {
            throw invalidRequest("Missing protocol");
        }
        var protocolMatcher = PROTOCOL_REGEX.matcher(protocol);
        if (!protocolMatcher.matches()) {
            throw invalidRequest("Unsupported protocol: %s".formatted(protocol));
        }
        // Regex matched a single digit, so we cannot get a NumberFormatException here
        requestBuilder.httpMinorVersion(Byte.parseByte(protocolMatcher.group(1)));
    }

    private URI parsePathURI(String pathURI) {
        try {
            return new URI(pathURI);
        } catch (URISyntaxException e) {
            throw invalidRequest("Path %s is not a valid URI".formatted(pathURI));
        }
    }

    private void parseHeaders() throws IOException {
        while(true) {
            lineReader.nextLine();
            if (lineReader.isLineEmpty()) {
                return;
            }

            var headerKey = lineReader.readToken();
            var headerValue = lineReader.readToken();
            if (lineReader.isLineTruncated()) {
                throw invalidRequest(HttpStatus.REQUEST_HEADER_TOO_LARGE, "HTTP header %s too large".formatted(headerKey));
            }
            if (!headerKey.endsWith(":") || headerValue == null) {
                throw invalidRequest("Invalid HTTP header key: %s".formatted(headerKey));
            }

            var normalizedHeaderKey = headerKey.substring(0, headerKey.length() - 1).toLowerCase();
            switch (normalizedHeaderKey) {
                case "connection" -> parseConnectionHeader(headerValue);
                case "content-length" -> parseContentLength(headerValue);
                // We can add here more headers that require specific treatment...
            }
        }
    }

    private void parseConnectionHeader(String connection) {
        // According to Mozilla MDN Web Docs: "Connection" header can have value "close" or any comma-separated list
        // of HTTP headers (usually "keep-alive" only). The latter case means that the client would like to keep
        // the connection open.
        if (connection.equals("close")) {
            requestBuilder.keepAlive(false);
        } else {
            requestBuilder.keepAlive(true);
        }
    }

    private void parseContentLength(String headerValue) {
        try {
            bodyLength = Long.parseLong(headerValue.trim());
        } catch (NumberFormatException ex) {
            throw invalidRequest("Invalid value of Content-Length header: %s".formatted(headerValue));
        }
    }

    private void skipBody() throws IOException {
        if (bodyLength == null) {
            return;
        }

        lineReader.skipBytes(bodyLength);
    }

    private InvalidRequestException invalidRequest(String message) {
        return invalidRequest(HttpStatus.BAD_REQUEST, message);
    }

    private InvalidRequestException invalidRequest(HttpStatus statusCode, String message) {
        return new InvalidRequestException(statusCode, message);
    }

    private static class InvalidRequestException extends RuntimeException {

        private final HttpStatus statusCode;

        private InvalidRequestException(HttpStatus statusCode, String message) {
            super(message);
            this.statusCode = statusCode;
        }
    }
}
