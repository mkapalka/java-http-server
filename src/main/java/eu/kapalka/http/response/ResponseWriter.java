package eu.kapalka.http.response;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;

import static java.util.logging.Level.FINER;

/**
 * Class responsible for formatting an HTTP response (see {@link Response}) and writing it to the output stream
 * of the underlying network connection.
 */
public class ResponseWriter {

    static Clock clock = Clock.systemUTC(); // Overwritten for testing

    private static final Logger logger = Logger.getLogger(ResponseWriter.class.getName());

    private final OutputStream output;

    public ResponseWriter(OutputStream output) {
        this.output = output;
    }

    /**
     * Write HTTP headers and body. Equivalent to calling {@link #writeHeaders(Response, boolean)} and
     * {@link #writeBody(Response)} (convenience method).
     *
     * @param response HTTP response
     * @param keepAlive maps to the <code>Connection</code> header value: <code>keep-alive</code> of <code>close</code>
     * @throws IOException
     */
    public void writeFull(Response response, boolean keepAlive) throws IOException {
        writeHeaders(response, keepAlive);
        writeBody(response);
    }

    /**
     * Write HTTP headers.
     *
     * @param response HTTP response
     * @param keepAlive maps to the <code>Connection</code> header value: <code>keep-alive</code> of <code>close</code>
     * @throws IOException
     */
    public void writeHeaders(Response response, boolean keepAlive) throws IOException {
        var status = response.getStatus();
        writeLine(output, "HTTP/1.1 %d %s".formatted(status.getCode(), status.getMessage()));
        writeHeaderLine(output, "Server", "KapalkaHTTPServer/1.0");
        writeHeaderLine(output, "Date",
                ZonedDateTime.now(clock).format(DateTimeFormatter.RFC_1123_DATE_TIME));

        var connectionHeaderValue = (keepAlive) ? "keep-alive" : "close";
        writeHeaderLine(output, "Connection", connectionHeaderValue);

        if (response.getContentType() != null) {
            writeHeaderLine(output, "Content-Type", response.getContentType());
        }

        writeHeaderLine(output, "Content-Length", Long.toString(response.getBodyLength()));
        writeLine(output, "");
    }

    /**
     * Write HTTP body (copy from the input stream in {@link Response#getBody()}).
     *
     * @param response HTTP response
     * @throws IOException
     */
    public void writeBody(Response response) throws IOException {
        if (response.getBody() == null) {
            return;
        }

        var buffer = new byte[4096];
        try (var bodyStream = response.getBody().get()) {
            while (true) {
                int size = bodyStream.read(buffer);
                if (size == -1) {
                    break; // EOF
                }
                output.write(buffer, 0, size);
            }
        }
    }

    private void writeHeaderLine(OutputStream output, String headerName, String headerValue) throws IOException {
        writeLine(output, "%s: %s".formatted(headerName, headerValue));
    }

    private void writeLine(OutputStream output, String line) throws IOException {
        logger.log(FINER, "Header OUT> {0}", line);
        output.write(line.getBytes(StandardCharsets.US_ASCII));
        output.write('\r');
        output.write('\n');
    }
}
