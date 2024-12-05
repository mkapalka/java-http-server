package eu.kapalka.http.request;

import eu.kapalka.http.TestBase;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

public class LineReaderTest extends TestBase {

    @Test
    void basicTest() throws IOException {
        var input = """
                abc 123\r
                efg xxx\r
                \r
                xyz""";
        var reader = new LineReader(asInputStream(input));

        // 1st line
        assertThat(reader.nextLine()).isTrue();
        assertThat(reader.isLineEmpty()).isFalse();
        assertThat(reader.readToken()).isEqualTo("abc");
        assertThat(reader.readToken()).isEqualTo("123");

        // 2nd line
        reader.nextLine();
        assertThat(reader.readToken()).isEqualTo("efg");
        // Ignore the token "xxx"

        // 3rd line
        reader.nextLine();
        assertThat(reader.isLineEmpty()).isTrue();

        // 4th line
        reader.nextLine();
        assertThat(reader.readToken()).isEqualTo("xyz");

        assertThat(reader.nextLine()).isFalse();
    }

    @Test
    void invalidCharacters() throws IOException {
        var input = "éà";
        var reader = new LineReader(asInputStream(input));
        reader.nextLine();
        assertThat(reader.readToken()).isEqualTo("??");
    }
}
