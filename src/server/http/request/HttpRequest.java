package server.http.request;

import java.util.Map;

import lombok.Data;

public @Data final class HttpRequest {
    private final Map<String,String> headers;
    private final String method;
    private final String path;
    private final String version;
    private final String body;

    public HttpRequest(Map<String,String> headers, String method, String path, String version, String body) {
        this.headers = headers;
        this.method = method;
        this.path = path;
        this.version = version;
        this.body = body;
    }

    @Override
    public String toString() {
        return "HttpRequest{" +
            "headers='" + headers + '\'' +
            "method='" + method + '\'' +
            ", path='" + path + '\'' +
            ", version='" + version + '\'' +
            ", body='" + body + '\'' +
            '}';
    }
}
