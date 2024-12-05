package eu.kapalka.http.handler;

import eu.kapalka.http.repository.StaticFileRepository;
import eu.kapalka.http.request.HttpMethod;
import eu.kapalka.http.request.ValidRequest;
import eu.kapalka.http.response.HttpStatus;
import eu.kapalka.http.response.InputStreamSupplier;
import eu.kapalka.http.response.Response;

import java.nio.file.Files;

public class StaticContentRequestHandler implements ResourceRequestHandler {

    private final StaticFileRepository fileRepository;

    public StaticContentRequestHandler(StaticFileRepository fileRepository) {
        this.fileRepository = fileRepository;
    }

    @Override
    public Response handle(ValidRequest request, String relativePath) {
        var method = request.getMethod();
        if (method != HttpMethod.GET && method != HttpMethod.HEAD) {
            return Response.builder()
                    .status(HttpStatus.METHOD_NOT_ALLOWED)
                    .body("Method %s not allowed".formatted(request.getMethod()))
                    .build();
        }

        var targetFileOpt = fileRepository.findFile(relativePath);
        if (targetFileOpt.isEmpty()) {
            return Response.builder()
                    .status(HttpStatus.NOT_FOUND)
                    .body("Resource with URI %s not found".formatted(request.getURI()))
                    .build();
        }

        var targetFile = targetFileOpt.get();
        InputStreamSupplier body = () -> Files.newInputStream(targetFile.path());
        return Response.builder()
                .body(body, targetFile.size())
                .contentType(targetFile.mimeType())
                .build();
    }
}
