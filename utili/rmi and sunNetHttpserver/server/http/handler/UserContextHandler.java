package server.http.handler;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.Map;

import com.sun.net.httpserver.*;

import server.util.Logger;
import server.util.expressRouting.ExpressRoute;

public class UserContextHandler {
    private static final Logger LOGGER = new Logger(UserContextHandler.class.getName());
    private static final String routeDefinition = "/user/:userID"; // userID is the username

    public static void userHandler(HttpExchange exchange){
        // usa database così: ServerMain.db;
        //LOGGER.info("richiesto profilo utente");
        URI requestURI = exchange.getRequestURI();
        Map<String, String> mappedRequestPath = parsePath(requestURI.getPath());
        LOGGER.info("Parsed path: " + mappedRequestPath);
        printRequestInfo(exchange);

        // response
        String response = "This is the response at " + requestURI;

        // send response
        try(OutputStream os = exchange.getResponseBody()){
            exchange.sendResponseHeaders(200, response.getBytes().length);
            os.write(response.getBytes());
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    private static Map<String, String> parsePath(String path) {
        ExpressRoute route = new ExpressRoute(routeDefinition);
        
        if (route.matches(path)) {
            return route.getParametersFromPath(path);
        }
        return null;
    }


    private static void printRequestInfo(HttpExchange exchange) {
        System.out.println("-- headers --");
        Headers requestHeaders = exchange.getRequestHeaders();
        requestHeaders.entrySet().forEach(System.out::println);
        
        System.out.println("-- principle --");
        HttpPrincipal principal = exchange.getPrincipal();
        System.out.println(principal);

        System.out.println("-- HTTP method --");
        String requestMethod = exchange.getRequestMethod();
        System.out.println(requestMethod);

        System.out.println("-- query --");
        URI requestURI = exchange.getRequestURI();
        String query = requestURI.getQuery();
        System.out.println(query);
    }
}
