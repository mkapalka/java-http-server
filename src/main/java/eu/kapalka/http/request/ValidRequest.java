package eu.kapalka.http.request;

import java.net.URI;
import java.util.Objects;

/**
 * Parsed information from a valid HTTP request.
 */
public final class ValidRequest implements Request {

    private final HttpMethod method;
    private final URI uri;
    private final byte httpMinorVersion;
    private final Boolean keepAliveHeader;

    public static Builder builder() {
        return new Builder();
    }

    public HttpMethod getMethod() {
        return method;
    }

    public URI getURI() {
        return uri;
    }

    public byte getHttpMinorVersion() {
        return httpMinorVersion;
    }

    public Boolean getKeepAliveHeader() {
        return keepAliveHeader;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ValidRequest that = (ValidRequest) o;
        return httpMinorVersion == that.httpMinorVersion && method == that.method && Objects.equals(uri, that.uri)
                && Objects.equals(keepAliveHeader, that.keepAliveHeader);
    }

    @Override
    public int hashCode() {
        return Objects.hash(method, uri, httpMinorVersion, keepAliveHeader);
    }

    private ValidRequest(HttpMethod method, URI uri, byte httpMinorVersion, Boolean keepAliveHeader) {
        this.method = method;
        this.uri = uri;
        this.httpMinorVersion = httpMinorVersion;
        this.keepAliveHeader = keepAliveHeader;
    }

    public static class Builder {
        private HttpMethod method;
        private URI uri;
        private byte httpMinorVersion;
        private Boolean keepAliveHeader;

        private Builder() {
        }

        public Builder method(HttpMethod method) {
            this.method = method;
            return this;
        }

        public Builder uri(URI uri) {
            this.uri = uri;
            return this;
        }

        public Builder httpMinorVersion(byte httpMinorVersion) {
            this.httpMinorVersion = httpMinorVersion;
            return this;
        }

        public Builder keepAlive(boolean keepAliveHeader) {
            this.keepAliveHeader = keepAliveHeader;
            return this;
        }

        public ValidRequest build() {
            return new ValidRequest(this.method, this.uri, this.httpMinorVersion, this.keepAliveHeader);
        }
    }
}
