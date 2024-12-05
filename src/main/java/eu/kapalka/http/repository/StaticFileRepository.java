package eu.kapalka.http.repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Optional;
import java.util.logging.Logger;

import static java.util.logging.Level.FINE;

/**
 * Read-only repository of static files. For security reasons, we serve only files whose absolute normalized paths
 * start from {@link #baseRepositoryPath}. We follow symbolic links.
 */
public class StaticFileRepository {

    private static final Logger logger = Logger.getLogger(StaticFileRepository.class.getName());

    private static final String DEFAULT_INDEX_FILE = "index.html";

    private final Path baseRepositoryPath;

    public StaticFileRepository(Path baseRepositoryPath) {
        this.baseRepositoryPath = baseRepositoryPath.toAbsolutePath().normalize();
    }

    /**
     * Finds the file that corresponds to the given request path. For example, request path "/some/file.txt" maps
     * to path "${baseRepositoryPath}/some/file.txt". Returns {@link Optional#empty()} if the file does not exist,
     * is not a regular file, or is not accessible.
     */
    public Optional<FileInfo> findFile(String resourcePath) {
        var finalResourcePath = resourcePath;
        // Convert to relative path
        if (finalResourcePath.startsWith("/")) {
            finalResourcePath = finalResourcePath.substring(1);
        }
        if (finalResourcePath.isEmpty()) {
            finalResourcePath = DEFAULT_INDEX_FILE;
        }

        var targetPath = baseRepositoryPath.resolve(finalResourcePath).normalize();
        logger.log(FINE, "Request mapped to target file path: {0}", targetPath);
        if (!targetPath.startsWith(baseRepositoryPath)) {
            return Optional.empty();
        }

        try {
            var fileAttrs = Files.readAttributes(targetPath, BasicFileAttributes.class);
            if (fileAttrs.isRegularFile() && Files.isReadable(targetPath)) {
                return Optional.of(new FileInfo(targetPath, fileAttrs.size(), Files.probeContentType(targetPath)));
            }
            return Optional.empty();
        } catch (IOException ex) {
            return Optional.empty();
        }
    }
}
