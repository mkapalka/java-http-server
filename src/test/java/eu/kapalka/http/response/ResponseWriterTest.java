package eu.kapalka.http.response;

import eu.kapalka.http.TestBase;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static eu.kapalka.http.response.HttpStatus.BAD_REQUEST;
import static org.assertj.core.api.Assertions.assertThat;

class ResponseWriterTest extends TestBase {

    @Test
    void fullResponse() throws IOException {
        var response = Response.builder()
                .body("ABC")
                .build();
        var output = outputStream();
        var writer = new ResponseWriter(output);

        withDateTime("2024-01-02T10:30:50Z", () -> writer.writeHeaders(response, true));

        assertThat(toString(output)).isEqualTo("""
                HTTP/1.1 200 OK\r
                Server: KapalkaHTTPServer/1.0\r
                Date: Tue, 2 Jan 2024 10:30:50 GMT\r
                Connection: keep-alive\r
                Content-Type: text/plain; charset=utf-8\r
                Content-Length: 3\r
                \r
                """);

        output.reset();
        writer.writeBody(response);
        assertThat(toString(output)).isEqualTo("ABC");
    }

    @Test
    void streamedBody() throws IOException {
        var body = "<html><body>ABC</body></html>";
        var response = Response.builder()
                .body(() -> asInputStream(body), body.length())
                .contentType("text/html")
                .build();
        var output = outputStream();
        var writer = new ResponseWriter(output);
        writer.writeHeaders(response, false);
        writer.writeBody(response);
        assertThat(toString(output))
                .contains("Content-Type: text/html\r\n")
                .contains("Content-Length: %d\r\n".formatted(body.length()))
                .endsWith("\r\n%s".formatted(body));
    }

    @Test
    void errorStatus() throws IOException {
        var response = Response.builder()
                .status(BAD_REQUEST)
                .build();
        var output = outputStream();
        var writer = new ResponseWriter(output);
        writer.writeHeaders(response, false);
        assertThat(toString(output))
                .startsWith("HTTP/1.1 400 Bad Request\r\n");
    }

    private static ByteArrayOutputStream outputStream() {
        return new ByteArrayOutputStream();
    }

    private static String toString(ByteArrayOutputStream output) {
        return output.toString(StandardCharsets.UTF_8);
    }

    private static void withDateTime(String dateTime, RunnableWithIO code) throws IOException {
        var savedClock = ResponseWriter.clock;
        try {
            ResponseWriter.clock = Clock.fixed(Instant.parse(dateTime), ZoneId.of("UTC"));
            code.run();
        } finally {
            ResponseWriter.clock = savedClock;
        }
    }

    private interface RunnableWithIO {
        void run() throws IOException;
    }
}
