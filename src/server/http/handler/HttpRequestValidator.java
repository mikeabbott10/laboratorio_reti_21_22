package server.http.handler;

import static server.util.Constants.SUPPORTED_HTTP_METHODS;
import static server.util.Constants.SUPPORTED_HTTP_VERSION;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import database.Database;
import exceptions.DatabaseException;
import server.http.request.HttpRequest;
import server.http.response.HttpResponse;
import server.http.response.HttpResponseFactory;
import server.util.expressRouting.ExpressRoute;

public class HttpRequestValidator {
    protected final String userRouteDefinition = "/user/:userID";
    protected final String userWalletRouteDefinition = "/user/:userID/wallet/:btcWallet";
    protected final String postSetRouteDefinition = "/post";
    protected final String postRouteDefinition = "/post/:postID";
    protected final String userPostsRouteDefinition = "/posts/:userID";
    protected final String tagUsersRouteDefinition = "/users/:tagName";

    protected final String userActionRouteDefinition = "/user/:userID/action/:actionID";
    protected final String postActionRouteDefinition = "/post/:postID/action/:actionID";
    protected final String logoutActionRouteDefinition = "/logout";


    protected Map<String, String> GETPathValidation(HttpRequest request) {
        Map<String, String> mappedPath;
        if( (mappedPath = parsePath(request.getPath(), userRouteDefinition)) != null ){
            return mappedPath;
        }else if( (mappedPath = parsePath(request.getPath(), userWalletRouteDefinition)) != null ){
            return mappedPath;
        }else if( (mappedPath = parsePath(request.getPath(), postRouteDefinition)) != null ){
            return mappedPath;
        }else if( (mappedPath = parsePath(request.getPath(), userPostsRouteDefinition)) != null ){
            return mappedPath;
        }else if( (mappedPath = parsePath(request.getPath(), tagUsersRouteDefinition)) != null ){
            return mappedPath;
        }
        return null;
    }

    public Map<String, String> PUTPathValidation(HttpRequest request) {
        Map<String, String> mappedPath;
        if( (mappedPath = parsePath(request.getPath(), userActionRouteDefinition)) != null ){
            return mappedPath;
        }else if( (mappedPath = parsePath(request.getPath(), postActionRouteDefinition)) != null ){
            return mappedPath;
        }else if( (mappedPath = parsePath(request.getPath(), logoutActionRouteDefinition)) != null ){
            return mappedPath;
        }
        return null;
    }

    protected Map<String, String> parsePath(String path, String routeDefinition) {
        ExpressRoute route = new ExpressRoute(routeDefinition);

        if (route.matches(path)) {
            return route.getParametersFromPath(path);
        }
        return null;
    }

    protected HttpRequest parseRequest(String raw) throws IOException {
        try {
            String[] rows = raw.split("\n");
            String[] firstRow = rows[0].split(" ");
            String method = firstRow[0].trim().toUpperCase();
            String path = firstRow[1].trim();
            String version = firstRow[2].trim();
            rows = Arrays.copyOfRange(rows, 1, rows.length);
            Map<String, String> headers = new HashMap<>();
            int i = 0;
            for (String row : rows) {
                ++i;
                String[] arr = row.split(":");
                if( arr.length == 1 ) break; // cr lf
                String key = arr[0].trim();
                arr = Arrays.copyOfRange(arr, 1, arr.length);
                String value = String.join(":", arr).trim();
                //System.out.println("KEY: "+ key);
                //System.out.println("VALUE: "+ value);
                headers.put(key, value);
                //System.out.println("--headers--\n"+headers);
            }
            String[] bodyRows = Arrays.copyOfRange(rows, i, rows.length);
            String body = String.join("\n", bodyRows).trim();

            return new HttpRequest(headers, method, path, version, body);
        } catch (Exception e) {
            throw new IOException("Malformed request", e);
        }
    }

    //#region Request validation
    protected HttpResponse validateRequest(HttpRequest request) throws JsonMappingException, JsonProcessingException {
        // validateSupported request (method, etc.)
        String invalidReason = validateSupported(request);
        if (invalidReason != null) {
            return new HttpResponseFactory().buildBadRequest(invalidReason);
        }
        return null;
    }

    protected String validateSupported(HttpRequest request) {
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

    //#region Login validation
    protected Object validateLogin(Database db, HttpRequest request) throws DatabaseException, JsonMappingException, JsonProcessingException {
        String username = request.getHeaders().get("username");
        String password = request.getHeaders().get("password");
        if(username == null || password == null)
            return new HttpResponseFactory().buildForbiddenResponse("Invalid login");
        return db.getAllowedUser(username, password);
    }
    //#endregion
}
