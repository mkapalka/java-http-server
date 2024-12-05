package eu.kapalka.http.repository;

import eu.kapalka.http.TestBase;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

public class StaticFileRepositoryTest extends TestBase {

    private final Path repositoryBasePath = Path.of("src/test/resources/content");
    private final StaticFileRepository repository = new StaticFileRepository(repositoryBasePath);

    @Test
    void findFile() {
        var fileInfo = repository.findFile("/folder/page.html");
        assertThat(fileInfo.get())
                .isEqualTo(new FileInfo(absoluteRepoPath("folder/page.html"), 80, "text/html"));
    }

    @Test
    void fileNotFound() {
        var fileInfo = repository.findFile("/does-not-exist.txt");
        assertThat(fileInfo).isEmpty();
    }

    @Test
    void notRegularFile() {
        var fileInfo = repository.findFile("/folder");
        assertThat(fileInfo).isEmpty();
    }

    @Test
    void indexFile() {
        var fileInfo = repository.findFile("/");
        assertThat(fileInfo.get())
                .isEqualTo(new FileInfo(absoluteRepoPath("index.html"), 77, "text/html"));
    }

    @Test
    void relativeResourcePath() {
        var fileInfo = repository.findFile("file.txt");
        assertThat(fileInfo.get())
                .isEqualTo(new FileInfo(absoluteRepoPath("file.txt"), 13, "text/plain"));
    }

    @Test
    void followSymlinks() {
        var fileInfo = repository.findFile("/symlink.txt");
        assertThat(fileInfo.get())
                .isEqualTo(new FileInfo(absoluteRepoPath("symlink.txt"), 15, "text/plain"));
    }

    @Test
    void preventEscape() {
        var fileInfo = repository.findFile("/../secret.txt");
        assertThat(fileInfo).isEmpty();
    }

    private Path absoluteRepoPath(String relativePath) {
        return repositoryBasePath.resolve(relativePath).toAbsolutePath();
    }
}
