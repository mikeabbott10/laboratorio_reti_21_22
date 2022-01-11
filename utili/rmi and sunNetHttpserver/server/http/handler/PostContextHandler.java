package server.http.handler;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;

import com.sun.net.httpserver.*;

import server.util.Logger;

public class PostContextHandler {
    private static final Logger LOGGER = new Logger(PostContextHandler.class.getName());
    private static final String singlePostRouteDefinition = "/post/:postID";
    private static final String userPostsRouteDefinition = "/posts/:userID"; // userID is the username

    public static void postHandler(HttpExchange exchange){
        // usa database così: ServerMain.db;
        LOGGER.info("richiesto post");
    }

    public static void userPostsHandler(HttpExchange exchange){
        // usa database così: ServerMain.db;
        LOGGER.info("richiesti post di un utente");
    }

    /*private static void handleRequest(HttpExchange exchange) throws IOException {
        URI requestURI = exchange.getRequestURI();
        printRequestInfo(exchange);
        String response = "This is the response at " + requestURI;
        exchange.sendResponseHeaders(200, response.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
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
    }*/
}