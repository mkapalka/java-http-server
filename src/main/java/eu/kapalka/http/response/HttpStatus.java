package eu.kapalka.http.response;

public enum HttpStatus {
    OK(200, "OK"),
    NO_CONTENT(204, "No Content"),
    BAD_REQUEST(400, "Bad Request"),
    NOT_FOUND(404, "Not Found"),
    METHOD_NOT_ALLOWED(405, "Method Not Allowed"),
    URI_TOO_LONG(414, "URI Too Long"),
    REQUEST_HEADER_TOO_LARGE(431, "Request Header Fields Too Large"),
    INTERNAL_SERVER_ERROR(500, "Internal Server Error");

    private final int code;
    private final String message;

    private HttpStatus(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
