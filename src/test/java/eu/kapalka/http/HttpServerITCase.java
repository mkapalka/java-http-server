package eu.kapalka.http;

import eu.kapalka.http.handler.ResourceRequestHandler;
import eu.kapalka.http.handler.StaticContentRequestHandler;
import eu.kapalka.http.repository.StaticFileRepository;
import eu.kapalka.http.request.HttpMethod;
import eu.kapalka.http.request.ValidRequest;
import eu.kapalka.http.response.HttpStatus;
import eu.kapalka.http.response.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class HttpServerITCase extends TestBase {

    private static HttpServer server;

    @BeforeAll
    static void setup() {
        var fileRepository = new StaticFileRepository(Path.of("src/test/resources/content"));
        var staticFileHandler = new StaticContentRequestHandler(fileRepository);
        server = new HttpServer("localhost", 0);
        server.registerRequestHandler("/server-error", new ThrowingTestHandler());
        server.registerRequestHandler("/", staticFileHandler);
        new Thread(server::start).start();
    }

    @AfterAll
    static void shutdown() throws IOException {
        server.stop();
    }

    @Test
    void validRequest() throws IOException, InterruptedException {
        var response = sendRequest(HttpMethod.GET, "/file.txt");
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.getCode());
        assertThat(response.body()).isEqualTo("Test content\n");
    }

    @Test
    void validHeadRequest() throws IOException, InterruptedException {
        var response = sendRequest(HttpMethod.HEAD, "/file.txt");
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.getCode());
        assertThat(response.headers().firstValueAsLong("Content-Length").getAsLong()).isEqualTo(13);
        assertThat(response.body()).isEmpty();
    }

    @Test
    void notFound() throws IOException, InterruptedException {
        var response = sendRequest(HttpMethod.GET, "/does-not-exist.txt");
        assertThat(response.statusCode()).isEqualTo(HttpStatus.NOT_FOUND.getCode());
    }

    @Test
    void serverError() throws IOException, InterruptedException {
        var response = sendRequest(HttpMethod.GET, "/server-error");
        assertThat(response.statusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.getCode());
    }

    @Test
    void invalidRequest() throws IOException {
        var response = sendRawRequest("""
                JUMP / HTTP/1.1\r
                \r
                """);
        assertThat(response).startsWith("""
                HTTP/1.1 400 Bad Request\r
                """);
    }

    @Test
    void testKeepAlive() throws IOException {
        // Two requests over the same connection (the former requests keep-alive)
        var response = sendRawRequest("""
                GET /file.txt HTTP/1.1\r
                \r
                GET /file.txt HTTP/1.1\r
                Connection: close\r
                \r
                """);
        var count = Arrays.stream(response.split("\n"))
                .filter(line -> line.equals("Test content"))
                .count();
        assertThat(count).isEqualTo(2);
    }

    private HttpResponse<String> sendRequest(HttpMethod method, String path) throws IOException, InterruptedException {
        try (var client = HttpClient.newHttpClient()) {
            var uri = URI.create("http://localhost:%d%s".formatted(server.getPort(), path));
            var requestBuilder = HttpRequest.newBuilder(uri);
            requestBuilder = switch (method) {
                case GET -> requestBuilder.GET();
                case HEAD -> requestBuilder.HEAD();
                case DELETE -> requestBuilder.DELETE();
                default -> throw new IllegalArgumentException();
            };
            return client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
        }
    }

    private String sendRawRequest(String request) throws IOException {
        try (var socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", server.getPort()));
            socket.getOutputStream().write(request.getBytes(StandardCharsets.US_ASCII));
            return new String(socket.getInputStream().readAllBytes(), StandardCharsets.US_ASCII);
        }
    }

    private static class ThrowingTestHandler implements ResourceRequestHandler {

        @Override
        public Response handle(ValidRequest request, String relativePath) {
            throw new RuntimeException("ERROR");
        }
    }
}
