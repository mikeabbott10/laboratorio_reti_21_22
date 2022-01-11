package server.http.handler;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import database.Database;
import exceptions.DatabaseException;
import lombok.Data;
import lombok.NoArgsConstructor;
import server.http.request.HttpRequest;
import server.http.response.HttpResponse;
import server.http.response.HttpResponseFactory;
import server.nio.CustomRequest;
import server.util.JacksonUtil;
import server.util.Logger;
import social.Post;
import social.User;

public class HttpRequestHandler {
    private static final Logger LOGGER = new Logger(HttpRequestHandler.class.getName());

    private HttpRequestValidator validator;

    public HttpRequestHandler() {
        this.validator = new HttpRequestValidator();
    }

    public HttpResponse handleRequest(Database db, CustomRequest req) throws IOException, DatabaseException{
        HttpRequest request = new HttpRequestValidator().parseRequest(req.getMessage());
        //LOGGER.info("Parsed incoming HTTP request: " + request);

        //#region check request validity
        HttpResponse notValidResponse = validator.validateRequest(request);
        if (notValidResponse != null) {
            LOGGER.warn("Invalid incoming HTTP request: " + 
                            request + ", response: " + notValidResponse);
            return notValidResponse;
        }
        //#endregion

        //#region check login validity
        Object loginValidationResult = validator.validateLogin(db, request);
        if (loginValidationResult instanceof HttpResponse) {
            LOGGER.warn("Invalid login: " + 
                            request + ", response: " + loginValidationResult);
            return (HttpResponse) loginValidationResult;
        }
        //#endregion
        User user = (User) loginValidationResult;

        switch(request.getMethod()){
            case "GET":{
                return GETRequestHandler(db, request, user);
            }
            case "POST":{
                return POSTRequestHandler(db, request, user);
            }
            case "PUT":{
                return PUTRequestHandler(db, request, user);
            }
            case "DELETE":{
                return DELETERequestHandler(db, request, user);
            }
            default:
                assert true == false; // never here
        }

        return new HttpResponseFactory().buildBadRequest("Protocol error occurred");
    }

    //#region PUT, DELETE, POST, GET handling
    private HttpResponse PUTRequestHandler(Database db, HttpRequest request, User user) 
            throws JsonMappingException, JsonProcessingException {
        Map<String, String> mappedPath = validator.PUTPathValidation(request);
        if(mappedPath == null){
            return new HttpResponseFactory().buildBadRequest(
                    request.getMethod() + " " + request.getPath() + " is not allowed");
        }
        
        switch( request.getPath().split("/")[1] ){
            case "user":{
                // follow or unfollow the user with id userID
                
            }
            case "post":{
                // rewin, comment, vote or unvote the post with id postID
            }
            case "logout":{
                // perform logout
            }
        }
        return new HttpResponseFactory().buildBadRequest("Protocol error occurred");
    }

    private HttpResponse DELETERequestHandler(Database db, HttpRequest request, User user) 
            throws JsonMappingException, JsonProcessingException {
        // only one action: remove post
        Map<String, String> mappedPath = 
            validator.parsePath(request.getPath(), validator.postRouteDefinition);
        if(mappedPath == null){
            return new HttpResponseFactory().buildBadRequest(
                    request.getMethod() + " " + request.getPath() + " is not allowed");
        }
        
        // remove the post
        if( db.removePost(user, Integer.parseInt(mappedPath.get("postID"), 10)) ){
            String responseMessage = JacksonUtil.getStringFromObject(
                new HashMap<String, String>() {{
                    put("message", "Post deleted.");
                }}
            );
            return new HttpResponseFactory().buildSuccess(responseMessage);
        }
        return new HttpResponseFactory().buildNotFound("The post does not exist.");
    }

    private HttpResponse POSTRequestHandler(Database db, HttpRequest request, User user) 
            throws JsonMappingException, JsonProcessingException, DatabaseException {
        // only one action: create post
        if( !request.getPath().equals(validator.postSetRouteDefinition) ) 
            return new HttpResponseFactory().buildBadRequest("Protocol error occurred");
        TitleContent postProperties = (TitleContent) JacksonUtil.getObjectFromString(request.getBody(), TitleContent.class);
        int postID = db.createPost( postProperties.title, postProperties.content, user.getUsername());
        // post created
        String responseMessage = JacksonUtil.getStringFromObject( 
            new HashMap<String, Integer>() {{
                put("postID", postID);
            }}
        );
        return new HttpResponseFactory().buildSuccess(responseMessage);
    }

    private HttpResponse GETRequestHandler(Database db, HttpRequest request, User user) 
            throws JsonMappingException, JsonProcessingException {
        Map<String, String> mappedPath = validator.GETPathValidation(request);
        if(mappedPath == null){
            return new HttpResponseFactory().buildBadRequest(
                    request.getMethod() + " " + request.getPath() + " is not allowed");
        }
        
        switch( request.getPath().split("/")[1] ){
            case "user":{
                // get user from userID or get own wallet
                User userFromUserID = db.getUser( mappedPath.get("userID") );
                if(userFromUserID==null)
                    return new HttpResponseFactory().buildNotFound("The user does not exist.");
                
                if(mappedPath.containsKey("btcWallet")){
                    // get own wallet
                    if(user.equals(userFromUserID)){
                        // allowed
                        String responseMessage;
                        if(mappedPath.get("btcWallet").equals("1")){
                            // get bitcoin wallet
                            try {
                                double btc = getBitcoinWallet(user.getWallet());
                                responseMessage = JacksonUtil.getStringFromObject( 
                                    new HashMap<String, Double>() {{
                                        put("wallet", btc);
                                    }}
                                );
                            } catch (IOException | InterruptedException | NumberFormatException | NullPointerException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                                return new HttpResponseFactory().buildBadRequest("Sorry, we can't get this information at the moment."); 
                            }
                        }else{
                            //get wallet
                            responseMessage = JacksonUtil.getStringFromObject( 
                                new HashMap<String, Object>() {{
                                    put("wallet", user.getWallet());
                                    put("wallet_history", user.getWalletHistory());
                                }}
                            );
                        }
                        return new HttpResponseFactory().buildSuccess(responseMessage);
                    }else{
                        // not allowed
                        return new HttpResponseFactory().buildForbiddenResponse("Operation forbidden."); 
                    }
                }

                // get user from userID
                String responseMessage = JacksonUtil.getStringFromObjectIgnoreFields( 
                    new HashMap<String, User>() {{
                        put("user", userFromUserID);
                    }},
                    "userFilter",
                    // fields to ignore
                    "password", "wallet", "walletHistory"
                    
                );
                return new HttpResponseFactory().buildSuccess(responseMessage);
            }
            case "post":{
                // get post from postID
                int postID = Integer.parseInt( mappedPath.get("postID") );
                Post postFromPostID = db.getPost( postID );
                if(postFromPostID==null){
                    return new HttpResponseFactory().buildNotFound("The post does not exist.");
                }
                String responseMessage = JacksonUtil.getStringFromObject(
                    new HashMap<String, Post>() {{
                        put("post", postFromPostID);
                    }}
                );
                return new HttpResponseFactory().buildSuccess(responseMessage);
            }
            case "posts":{
                // get posts from userID
                Post[] postsFromUserID = db.getPostsFromUsername( mappedPath.get("userID") );
                if(postsFromUserID==null)
                    return new HttpResponseFactory().buildNotFound("The user does not exist.");
                
                String responseMessage = JacksonUtil.getStringFromObject( 
                    new HashMap<String, Post[]>() {{
                        put("posts", postsFromUserID);
                    }}
                );
                return new HttpResponseFactory().buildSuccess(responseMessage);
            }
            case "users":{
                // get users from tagName
                HashMap<String, User> usersFromTagname = 
                    db.getUsersFromTagname( mappedPath.get("tagName") );
                
                String responseMessage = JacksonUtil.getStringFromObjectIgnoreFields( 
                    new HashMap<String, HashMap<String, User>>() {{
                        put("users", usersFromTagname);
                    }},
                    "userFilter",
                    // fields to ignore
                    "password", "wallet", "walletHistory"
                );
                return new HttpResponseFactory().buildSuccess(responseMessage);
            }
        }
        return new HttpResponseFactory().buildBadRequest("Protocol error occurred");
    }
    //#endregion

    private double getBitcoinWallet(double userWallet) throws IOException, InterruptedException {
        if (userWallet == 0.0) return 0.0;
        String res = performGETRequestTo("https://www.random.org/decimal-fractions/?num=1&dec=10&col=1&format=plain&rnd=new");
        //LOGGER.info("WALLET -------> "+ res);
        return userWallet * Double.parseDouble(res.toString());
    }

    public static String performGETRequestTo(String uri) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .GET()
                .build();
    
        java.net.http.HttpResponse<String> response =
              client.send(request, BodyHandlers.ofString());
    
        return response.body();
    }

    @NoArgsConstructor
    private @Data static class TitleContent{
        protected String title;
        protected String content;
    }
    
}
