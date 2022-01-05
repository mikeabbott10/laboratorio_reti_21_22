package server.http.handler;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.StringTokenizer;

import database.Database;
import server.http.request.*;
import server.http.response.HttpResponse;
import server.http.response.HttpResponseFactory;
import server.nio.CustomRequest;
import server.util.Logger;
import server.util.expressRouting.ExpressRoute;

import static server.util.Constants.SUPPORTED_HTTP_METHODS;
import static server.util.Constants.SUPPORTED_HTTP_VERSION;

public class HttpRequestHandler {
    private static final Logger LOGGER = new Logger(HttpRequestHandler.class.getName());

    String routeDefinition = "/login/:username/:password/action/:actionId";

    public HttpResponse handleRequest(Database db, CustomRequest req) throws IOException{
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
        LOGGER.info("Got a valid request: " + request + "\n");

        LOGGER.info("Parsed path: "+parsePath(request.getPath()));

        return new HttpResponseFactory().buildBadRequest("0123456789"); // TODO
    }

    //#region Path parsing
    private Map<String, String> parsePath(String path) {
        ExpressRoute route = new ExpressRoute(routeDefinition);
        
        if (route.matches(path)) {
            return route.getParametersFromPath(path);
        }
        return null;
    }

    //#region Request parsing
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
    //#endregion

    //#region Request validation
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
    //#endregion
}
