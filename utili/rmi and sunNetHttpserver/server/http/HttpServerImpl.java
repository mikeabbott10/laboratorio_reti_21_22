package server.http;

import com.sun.net.httpserver.*;

import server.util.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;

public class HttpServerImpl {
    private static final Logger LOGGER = new Logger(HttpServerImpl.class.getName());
    HttpServer server;

    public HttpServerImpl(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(null); // single thread!
    }

    public void setNewContext(String context_path, HttpHandler handler){
        HttpContext context = server.createContext(context_path);
        context.setHandler(handler);
    }

    public void start(){
        server.start();
    }
}