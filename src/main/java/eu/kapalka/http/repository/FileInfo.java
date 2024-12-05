package eu.kapalka.http.repository;

import java.nio.file.Path;

/**
 * Represents a file stored on the file system and returned by {@link StaticFileRepository#findFile(String)}.
 */
public record FileInfo(Path path, long size, String mimeType) {
}
