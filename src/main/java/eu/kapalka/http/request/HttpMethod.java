package eu.kapalka.http.request;

import java.util.Map;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toUnmodifiableMap;

public enum HttpMethod {
    GET,
    HEAD,
    POST,
    PUT,
    DELETE,
    PATCH,
    OPTIONS,
    TRACE;

    private static final Map<String, HttpMethod> methods = stream(HttpMethod.values())
            .collect(toUnmodifiableMap(Enum::name, method -> method));

    /**
     * Look up HTTP method by name. Returns null if method unknown.
     */
    public static HttpMethod of(String methodName) {
        return methods.get(methodName);
    }
}
