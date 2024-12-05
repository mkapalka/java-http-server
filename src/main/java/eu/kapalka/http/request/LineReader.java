package eu.kapalka.http.request;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;

/**
 * Reader and tokenizer of ASCII text with CRLF-terminated lines. Unlike {@link BufferedReader#readLine()},
 * it enforces a maximum line length, which is important for untrusted input data. Non-ASCII characters
 * are converted to "?".
 */
public class LineReader {

    private static final int MAX_LINE_LENGHT = 8000;

    private final InputStream inputStream;
    private StringBuilder buffer;
    private int currentPos;
    private boolean lineTruncated;

    public LineReader(InputStream inputStream) {
        this.inputStream = new BufferedInputStream(inputStream);
    }

    /**
     * Read next line to an internal buffer.
     *
     * @return <code>false</code> on EOF, <code>true</code> otherwise
     * @throws IOException when reading from the input stream fails
     */
    public boolean nextLine() throws IOException {
        buffer = new StringBuilder();
        currentPos = 0;
        lineTruncated = false;
        while (buffer.length() < MAX_LINE_LENGHT) {
            var b = inputStream.read();
            if (b == -1) {
                return false;
            }
            // ASCII encoding (required for HTTP header), so byte --> char translation is trivial
            char ch = (b <= 127) ? (char) b : '?';
            if (ch == '\n') {
                return true;
            }
            if (ch != '\r') {
                buffer.append(ch);
            }
        }
        lineTruncated = true;
        return true;
    }

    /**
     * Skips the given number of bytes (without any conversion / processing).
     *
     * @param numBytes number of bytes to skip
     * @throws IOException when
     */
    public void skipBytes(long numBytes) throws IOException {
        // EOFException thrown below means that connection with client was interrupted, so we propagate it
        // the same way as any other I/O error.
        inputStream.skipNBytes(numBytes);
    }

    /**
     * Returns <code>true</code> if the last line read by {@link #nextLine()} is empty.
     */
    public boolean isLineEmpty() {
        return buffer.isEmpty();
    }

    /**
     * Returns <code>true</code> if the last line read by {@link #nextLine()} was longer than the maximum allowed
     * length and was therefore truncated. The line was not read entirely, and so the next invocation of
     * {@link #nextLine()} will continue reading the same line: it is better to stop parsing and return an error
     * immediately.
     */
    public boolean isLineTruncated() {
        return lineTruncated;
    }

    /**
     * Reads the next token from the last line read by {@link #nextLine()} until a space character or EOL.
     * Returns <code>null</code> if there are no more tokens to be read.
     */
    public String readToken() {
        if (currentPos >= buffer.length()) {
            return null;
        }

        var prevPos = currentPos;
        while (currentPos < buffer.length() && buffer.charAt(currentPos) != ' ') {
            currentPos++;
        }
        var token = buffer.substring(prevPos, currentPos);
        currentPos++; // skip over the separator
        return token;
    }
}
