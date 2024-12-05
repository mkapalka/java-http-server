package eu.kapalka.http.handler;

import eu.kapalka.http.TestBase;
import eu.kapalka.http.repository.StaticFileRepository;
import eu.kapalka.http.request.HttpMethod;
import eu.kapalka.http.request.ValidRequest;
import eu.kapalka.http.response.HttpStatus;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

public class StaticContentRequestHandlerTest extends TestBase {

    private final Path repositoryBasePath = Path.of("src/test/resources/content");
    private final StaticContentRequestHandler handler =
            new StaticContentRequestHandler(new StaticFileRepository(repositoryBasePath));

    @Test
    void testGetFile() {
        var request = ValidRequest.builder()
                .method(HttpMethod.GET)
                .uri(uri("/static/file.txt"))
                .build();
        var response = handler.handle(request, "/file.txt");
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK);
        assertThat(response.getContentType()).isEqualTo("text/plain");
        assertThat(response.getBodyLength()).isEqualTo(13);
    }

    @Test
    void notFound() {
        var request = ValidRequest.builder()
                .method(HttpMethod.GET)
                .uri(uri("/static/non-existent.txt"))
                .build();
        var response = handler.handle(request, "/non-existent.txt");
        assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void methodNotAllowed() {
        var request = ValidRequest.builder()
                .method(HttpMethod.PUT)
                .uri(uri("/static/file.txt"))
                .build();
        var response = handler.handle(request, "/file.txt");
        assertThat(response.getStatus()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
    }
}
