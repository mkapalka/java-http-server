package eu.kapalka.http.handler;

import eu.kapalka.http.TestBase;
import eu.kapalka.http.request.HttpMethod;
import eu.kapalka.http.request.InvalidRequest;
import eu.kapalka.http.request.ValidRequest;
import eu.kapalka.http.response.HttpStatus;
import eu.kapalka.http.response.Response;
import eu.kapalka.http.response.ResponseWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class TopLevelRequestHandlerTest extends TestBase {

    private TopLevelRequestHandler topLevelHandler;
    private ResourceRequestHandler staticHandler;
    private ResourceRequestHandler otherHandler;
    private ResponseWriter responseWriter;
    private ResponseWriterStub responseWriterStub;

    @BeforeEach
    void setup() {
        topLevelHandler = new TopLevelRequestHandler();
        staticHandler = mock(ResourceRequestHandler.class);
        topLevelHandler.registerRequestHandler("/static", staticHandler);
        otherHandler = mock(ResourceRequestHandler.class);
        topLevelHandler.registerRequestHandler("/other", otherHandler);
        responseWriter = mock(ResponseWriter.class);
        responseWriterStub = new ResponseWriterStub();
    }

    @Test
    void validRequest() throws IOException {
        var request = ValidRequest.builder()
                .method(HttpMethod.GET)
                .uri(uri("/static/file.txt"))
                .httpMinorVersion((byte) 1)
                .build();

        var response = someResponse();
        when(staticHandler.handle(request, "/file.txt")).thenReturn(response);
        var keepAlive = topLevelHandler.handleRequest(request, responseWriter);
        assertThat(keepAlive).isTrue(); // Because HTTP version 1.1 in the request
        verify(responseWriter).writeHeaders(response, true);
        verify(responseWriter).writeBody(response);
    }

    @Test
    void validRequestOther() throws IOException {
        var request = ValidRequest.builder()
                .method(HttpMethod.HEAD)
                .uri(uri("/other/file.txt"))
                .httpMinorVersion((byte) 0)
                .build();

        var response = someResponse();
        when(otherHandler.handle(request, "/file.txt")).thenReturn(response);
        var keepAlive = topLevelHandler.handleRequest(request, responseWriter);
        assertThat(keepAlive).isFalse(); // Because HTTP version 1.0 in the request
        verify(responseWriter).writeHeaders(response, false);
        verifyNoMoreInteractions(responseWriter);
    }

    @Test
    void noHandlerForPrefix() throws IOException {
        var request = ValidRequest.builder()
                .method(HttpMethod.GET)
                .uri(uri("/unknown/file.txt"))
                .build();

        topLevelHandler.handleRequest(request, responseWriterStub);
        verifyNoInteractions(staticHandler, otherHandler);
        assertThat(responseWriterStub.writtenResponse.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(responseWriterStub.bodyWritten).isTrue();
    }

    @Test
    void invalidRequest() throws IOException {
        var request = new InvalidRequest(HttpStatus.BAD_REQUEST, "Missing method");

        topLevelHandler.handleRequest(request, responseWriterStub);
        verifyNoInteractions(staticHandler, otherHandler);
        assertThat(responseWriterStub.writtenResponse.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(responseWriterStub.bodyWritten).isTrue();
    }

    private static Response someResponse() {
        return Response.builder().body("Some response").build();
    }

    private static class ResponseWriterStub extends ResponseWriter {

        Response writtenResponse;
        boolean bodyWritten;

        ResponseWriterStub() {
            super(null);
        }


        @Override
        public void writeHeaders(Response response, boolean keepAlive) {
            writtenResponse = response;
        }

        @Override
        public void writeBody(Response response) {
            bodyWritten = true;
        }
    }
}
