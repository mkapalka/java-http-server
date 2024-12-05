package eu.kapalka.http.response;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

/**
 * Represents HTTP response. For simplicity, only fields that are used in this repository are represented in this class.
 */
public class Response {

    private static final String TEXT_BODY_CONTENT_TYPE = "text/plain; charset=utf-8";

    private final HttpStatus status;
    private final InputStreamSupplier body;
    private final long bodyLength;
    private final String contentType;

    public static Builder builder() {
        return new Builder();
    }

    public HttpStatus getStatus() {
        return status;
    }

    public InputStreamSupplier getBody() {
        return body;
    }

    public long getBodyLength() {
        return bodyLength;
    }

    public String getContentType() {
        return contentType;
    }

    private Response(HttpStatus status, InputStreamSupplier body, long bodyLength, String contentType) {
        this.status = status;
        this.body = body;
        this.bodyLength = bodyLength;
        this.contentType = contentType;
    }

    public static class Builder {
        private HttpStatus status = HttpStatus.OK;
        private InputStreamSupplier body;
        private long bodyLength;
        private String contentType;

        private Builder() {
        }

        public Builder status(HttpStatus status) {
            this.status = status;
            return this;
        }

        public Builder body(String body) {
            var bodyBytes = body.getBytes(StandardCharsets.UTF_8);
            this.body = () -> new ByteArrayInputStream(bodyBytes);
            this.bodyLength = bodyBytes.length;
            this.contentType = TEXT_BODY_CONTENT_TYPE;
            return this;
        }

        public Builder body(InputStreamSupplier inputStream, long bodyLength) {
            this.body = inputStream;
            this.bodyLength = bodyLength;
            return this;
        }

        public Builder contentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        public Response build() {
            return new Response(status, body, bodyLength, contentType);
        }
    }
}
