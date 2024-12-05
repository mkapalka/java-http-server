package eu.kapalka.http.request;

import eu.kapalka.http.TestBase;
import eu.kapalka.http.response.HttpStatus;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static eu.kapalka.http.request.HttpMethod.GET;
import static org.assertj.core.api.Assertions.assertThat;

class RequestParserTest extends TestBase {

    @Test
    void validRequest() {
        var request = parse("""
                GET /some/file.html?id=123 HTTP/1.1\r
                Host: localhost\r
                Connection: keep-alive\r
                Some-Header: abcd\r
                \r
                """);
        assertThat(request).isEqualTo(ValidRequest.builder()
                .method(GET)
                .uri(uri("/some/file.html?id=123"))
                .httpMinorVersion((byte) 1)
                .keepAlive(true)
                .build());
    }

    @Test
    void validRequestHTTP10() {
        var request = parse("""
                GET / HTTP/1.0\r
                Host: localhost\r
                \r
                """);
        assertThat(request).isEqualTo(ValidRequest.builder()
                .method(GET)
                .uri(uri("/"))
                .httpMinorVersion((byte) 0)
                .build());
    }

    @Test
    void validRequestConnectionClose() {
        var request = parse("""
                GET / HTTP/1.1\r
                Host: localhost\r
                Connection: close\r
                \r
                """);
        assertThat(request).isEqualTo(ValidRequest.builder()
                .method(GET)
                .uri(uri("/"))
                .httpMinorVersion((byte) 1)
                .keepAlive(false)
                .build());
    }

    @Test
    void emptyRequest() {
        // Connection but no data sent, e.g., from misconfigured monitoring tools or liveness probes
        assertBadRequestError(parse(""));
    }

    @Test
    void invalidMethod() {
        assertBadRequestError(parse("JUMP / HTTP/1.1\r"));
    }

    @Test
    void invalidPath() {
        assertBadRequestError(parse("GET \\resource HTTP/1.1\r"));
    }

    @Test
    void pathTooLong() {
        var path = "/abc".repeat(1_000);
        assertHttpErrorCode(parse("GET %s HTTP/1.1\r".formatted(path)), HttpStatus.URI_TOO_LONG);
    }

    @Test
    void invalidProtocol() {
        assertBadRequestError(parse("GET / HTTP/5\r"));
    }

    @Test
    void invalidHeader() {
        var request = parse("""
                GET / HTTP/1.1\r
                Invalid-header\r
                """);
        assertBadRequestError(request);
    }

    @Test
    void emptyHeaderValue() {
        var request = parse("""
                GET / HTTP/1.1\r
                Invalid-header: \r
                """);
        assertBadRequestError(request);
    }

    @Test
    void headerTooLong() {
        var request = """
                GET / HTTP/1.1\r
                Some-header: %s\r
                """.formatted("abc".repeat(5_000));
        assertHttpErrorCode(parse(request), HttpStatus.REQUEST_HEADER_TOO_LARGE);
    }

    private Request parse(String requestContents) {
        var parser = new RequestParser(asInputStream(requestContents));
        try {
            return parser.parse();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void assertBadRequestError(Request request) {
        assertHttpErrorCode(request, HttpStatus.BAD_REQUEST);
    }

    private static void assertHttpErrorCode(Request request, HttpStatus statusCode) {
        assertThat(request).isInstanceOf(InvalidRequest.class);
        var invalidRequest = (InvalidRequest) request;
        // We don't verify the error message
        assertThat(invalidRequest.statusCode()).isEqualTo(statusCode);
    }
}
