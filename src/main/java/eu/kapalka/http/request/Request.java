package eu.kapalka.http.request;

public sealed interface Request permits ValidRequest, InvalidRequest {
}
