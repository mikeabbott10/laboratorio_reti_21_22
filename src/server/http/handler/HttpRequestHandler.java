package server.http.handler;

import java.io.IOException;
import java.util.Arrays;
import java.util.StringTokenizer;

import server.http.request.*;
import server.http.response.HttpResponse;
import server.http.response.HttpResponseFactory;
import server.nio.CustomRequest;
import server.util.Logger;

import static server.util.Constants.SUPPORTED_HTTP_METHODS;
import static server.util.Constants.SUPPORTED_HTTP_VERSION;

public class HttpRequestHandler {
    private static final Logger LOGGER = new Logger(HttpRequestHandler.class.getName());

    public HttpResponse handleRequest(CustomRequest req) throws IOException{
        HttpRequest request = parseRequest(req.getMessage());
        //LOGGER.info("Parsed incoming HTTP request: " + request);

        // check request validity
        HttpResponse notValidResponse = validateRequest(request);

        if (notValidResponse != null) {
            LOGGER.warn("Invalid incoming HTTP request: " + 
                            request + ", response: " + notValidResponse);
            return notValidResponse;
        }

        // got valid request, do work
        LOGGER.info("Got a valid request: " + request);

        

        return new HttpResponseFactory().buildBadRequest("0123456789"); // TODO
    }

    // Request parsing ------------------------------------------------------------------------------
    private HttpRequest parseRequest(String raw) throws IOException {
        try {
            StringTokenizer tokenizer = new StringTokenizer(raw);
            String method = tokenizer.nextToken().toUpperCase();
            String path = tokenizer.nextToken();
            String version = tokenizer.nextToken();

            return new HttpRequest(method, path, version);
        } catch (Exception e) {
            throw new IOException("Malformed request", e);
        }
    }

    // Request validation ---------------------------------------------------------------------------
    private HttpResponse validateRequest(HttpRequest request) {
        // validateSupported request (method, etc.)
        String invalidReason = validateSupported(request);
        if (invalidReason != null) {
            return new HttpResponseFactory().buildBadRequest(invalidReason);
        }
        return null;
    }

    private String validateSupported(HttpRequest request) {
        String method = request.getMethod();
        if (method == null || !Arrays.asList(SUPPORTED_HTTP_METHODS).contains(method)) {
            return "Unsupported method";
        }
        String version = request.getVersion();
        if (version == null || !version.equals(SUPPORTED_HTTP_VERSION)) {
            return "Unsupported HTTP version";
        }
        return null;
    }
}
