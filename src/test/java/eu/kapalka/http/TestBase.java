package eu.kapalka.http;

import eu.kapalka.http.repository.StaticFileRepositoryTest;
import org.junit.jupiter.api.BeforeAll;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.logging.LogManager;

public class TestBase {

    @BeforeAll
    static void loggingSetup() throws IOException {
        LogManager.getLogManager().readConfiguration(StaticFileRepositoryTest.class.getResourceAsStream("/test-logging.properties"));
    }

    protected static InputStream asInputStream(String input) {
        return new ByteArrayInputStream(input.getBytes(StandardCharsets.US_ASCII));
    }

    protected static URI uri(String path) {
        try {
            return new URI(path);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
