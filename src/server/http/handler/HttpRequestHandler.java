package server.http.handler;

import java.io.IOException;
import java.net.ProtocolException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse.BodyHandlers;
import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;

import database.Database;
import database.social.Post;
import database.social.User;
import exceptions.AlreadyConnectedException;
import exceptions.DatabaseException;
import exceptions.ForbiddenActionException;
import exceptions.ResourceNotFoundException;
import lombok.Data;
import lombok.NoArgsConstructor;
import server.ServerMain;
import server.http.request.HttpRequest;
import server.http.response.HttpResponse;
import server.http.response.HttpResponseFactory;
import server.nio.CustomRequest;
import server.util.JacksonUtil;
import server.util.Logger;

public class HttpRequestHandler {
    private static final Logger LOGGER = new Logger(HttpRequestHandler.class.getName());

    private HttpRequestValidator validator;

    public HttpRequestHandler() {
        this.validator = new HttpRequestValidator();
    }

    public HttpResponse handleRequest(Database db, CustomRequest req) 
            throws DatabaseException, ProtocolException, JsonProcessingException{
        HttpRequest request = new HttpRequestValidator().parseRequest(req.getMessage());
        //LOGGER.info("Parsed incoming HTTP request: " + request);

        //#region check request validity
        HttpResponse notValidResponse = validator.validateRequest(request);
        if (notValidResponse != null) {
            // LOGGER.warn("Invalid incoming HTTP request: " + 
            //                 request + ", response: " + notValidResponse);
            return notValidResponse;
        }
        //#endregion

        //#region check login validity
        Object loginValidationResult = validator.validateLogin(db, request);
        if (loginValidationResult instanceof HttpResponse) {
            // LOGGER.warn("Invalid login: " + 
            //                 request + ", response: " + loginValidationResult);
            return (HttpResponse) loginValidationResult;
        }
        //#endregion

        // update last_session field of the logged user
        User u = (User) loginValidationResult;
        synchronized(u.getLast_session()){
            u.setLast_session(Calendar.getInstance().getTime());
        }

        // local user instance to avoid heavy synchronization working with it
        User user = new User( u, db );

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
            throws JsonProcessingException {
        Map<String, String> mappedPath = validator.PUTPathValidation(request);
        if(mappedPath == null){
            return new HttpResponseFactory().buildBadRequest(
                    request.getMethod() + " " + request.getPath() + " is not allowed");
        }
        
        switch( request.getPath().split("/")[1] ){
            case "user":{
                if(user.getUsername().equals(mappedPath.get("userID")))
                    new HttpResponseFactory().buildBadRequest("Cannot follow yourself.");
                if( mappedPath.get("actionID").equals("follow")){
                    // follow user with id userID
                    try{   
                        db.addFollowerTo(user.getUsername(), mappedPath.get("userID"));
                        String responseMessage = JacksonUtil.getStringFromObject( 
                            new HashMap<String, String>() {{
                                put("message", "Follow added.");
                            }}
                        );
                        // note that users cannot be removed:
                        // here we are sure about mappedPath.get("userID") existence (and user one)
                        ServerMain.serverRMIService.updateFollowersList( new User( db.getUser(mappedPath.get("userID")), db ) );
                        
                        return new HttpResponseFactory().buildSuccess(responseMessage);
                    }catch(ResourceNotFoundException e){
                        return new HttpResponseFactory().buildNotFound(e.getMessage());
                    } catch (ForbiddenActionException e1) {
                        return new HttpResponseFactory().buildForbiddenResponse(e1.getMessage());
                    }catch(NullPointerException e){
                        // not possible here
                        // maybe working on a user which is now deleted from another thread
                        return new HttpResponseFactory().buildErrorResponse("Operation not available.");
                    }
                }else if ( mappedPath.get("actionID").equals("unfollow") ){
                    // unfollow user with id userID
                    try{
                        db.removeFollowerTo(user.getUsername(), mappedPath.get("userID"));
                        String responseMessage = JacksonUtil.getStringFromObject( 
                            new HashMap<String, String>() {{
                                put("message", "Follow removed.");
                            }}
                        );
                        // note that users cannot be removed:
                        // here we are sure about mappedPath.get("userID") existence (and user one)
                        ServerMain.serverRMIService.updateFollowersList( new User( db.getUser(mappedPath.get("userID")), db ) );
                        
                        return new HttpResponseFactory().buildSuccess(responseMessage);
                    }catch(ResourceNotFoundException e){
                        return new HttpResponseFactory().buildNotFound(e.getMessage());
                    } catch (ForbiddenActionException e1) {
                        return new HttpResponseFactory().buildForbiddenResponse(e1.getMessage());
                    }catch(NullPointerException e){
                        // not possible here
                        // maybe working on a user which is now deleted from another thread
                        return new HttpResponseFactory().buildErrorResponse("Operation not available.");
                    }
                }
                return new HttpResponseFactory().buildBadRequest("Protocol error occurred");
            }
            case "post":{
                // rewin, comment, vote or unvote the post with id postID
                int postID;
                try{
                    postID = Integer.parseInt(mappedPath.get("postID"));
                }catch(NumberFormatException e){
                    return new HttpResponseFactory().buildBadRequest("Protocol error occurred");
                }

                switch( mappedPath.get("actionID") ){
                    case "comment":{
                        try{
                            Comment postProperties = 
                                (Comment) JacksonUtil.getObjectFromString(request.getBody(), Comment.class);
                            db.addComment( postID, postProperties.comment, user.getUsername() );
                            // post commented
                            String responseMessage = JacksonUtil.getStringFromObject( 
                                new HashMap<String, String>() {{
                                    put("message", "Comment added.");
                                }}
                            );
                            return new HttpResponseFactory().buildSuccess(responseMessage);
                        }catch(JsonProcessingException e){
                            return new HttpResponseFactory().buildBadRequest("Protocol error occurred");
                        }catch(ResourceNotFoundException ex){
                            return new HttpResponseFactory().buildNotFound(ex.getMessage());
                        }catch(ForbiddenActionException exc){
                            return new HttpResponseFactory().buildForbiddenResponse(exc.getMessage());
                        }catch(NullPointerException e){
                            // maybe working on a post which is now deleted from another thread
                            return new HttpResponseFactory().buildErrorResponse("Operation not available.");
                        }
                    }
                    case "rewin":{
                        try{
                            db.rewinPost(postID, user.getUsername());
                            String responseMessage = JacksonUtil.getStringFromObject( 
                                new HashMap<String, String>() {{
                                    put("message", "Post rewinned.");
                                }}
                            );
                            return new HttpResponseFactory().buildSuccess(responseMessage);
                        }catch(ResourceNotFoundException ex){
                            return new HttpResponseFactory().buildNotFound(ex.getMessage());
                        }catch(ForbiddenActionException exc){
                            return new HttpResponseFactory().buildForbiddenResponse(exc.getMessage());
                        }catch(NullPointerException e){
                            // maybe working on a post which is now deleted from another thread
                            return new HttpResponseFactory().buildErrorResponse("Operation not available.");
                        }
                    }
                    case "vote":{
                        try{
                            db.addVoteTo( postID, user.getUsername() );
                            String responseMessage = JacksonUtil.getStringFromObject( 
                                new HashMap<String, String>() {{
                                    put("message", "Upvote added.");
                                }}
                            );
                            return new HttpResponseFactory().buildSuccess(responseMessage);
                        }catch(ResourceNotFoundException ex){
                            return new HttpResponseFactory().buildNotFound(ex.getMessage());
                        }catch(ForbiddenActionException exc){
                            return new HttpResponseFactory().buildForbiddenResponse(exc.getMessage());
                        }catch(NullPointerException e){
                            // maybe working on a post which is now deleted from another thread
                            return new HttpResponseFactory().buildErrorResponse("Operation not available.");
                        }
                    }
                    case "unvote":{
                        try{
                            db.addDownvoteTo( postID, user.getUsername() );
                            String responseMessage = JacksonUtil.getStringFromObject( 
                                new HashMap<String, String>() {{
                                    put("message", "Downvote added.");
                                }}
                            );
                            return new HttpResponseFactory().buildSuccess(responseMessage);
                        }catch(ResourceNotFoundException ex){
                            return new HttpResponseFactory().buildNotFound(ex.getMessage());
                        }catch(ForbiddenActionException exc){
                            return new HttpResponseFactory().buildForbiddenResponse(exc.getMessage());
                        }catch(NullPointerException e){
                            // maybe working on a post which is now deleted from another thread
                            return new HttpResponseFactory().buildErrorResponse("Operation not available.");
                        }
                    }
                }
            }
            case "logout":{
                // perform logout
                db.removeLoggedUser(user.getUsername());
                server.ServerMain.serverRMIService.safeUnregisterForCallback(user.getUsername());
                String responseMessage = JacksonUtil.getStringFromObject( 
                    new HashMap<String, String>() {{
                        put("message", "Logout performed.");
                    }}
                );
                return new HttpResponseFactory().buildSuccess(responseMessage);
            }
            case "login":{
                try{
                    db.addLoggedUser(user.getUsername());
                }catch(AlreadyConnectedException e){
                    return new HttpResponseFactory().buildForbiddenResponse("User already connected.");
                }

                ServerMain.serverRMIService.updateFollowersList( user );
                

                String responseMessage = JacksonUtil.getStringFromObject( 
                    new HashMap<String, String>() {{
                        put("multicast_group_ip", ServerMain.server_config.MULTICAST_ADDRESS);
                        put("multicast_group_port", Integer.toString(ServerMain.server_config.MULTICAST_PORT) );
                    }}
                );
                return new HttpResponseFactory().buildSuccess(responseMessage);
            }
        }
        return new HttpResponseFactory().buildBadRequest("Protocol error occurred");
    }

    private HttpResponse DELETERequestHandler(Database db, HttpRequest request, User user) 
            throws JsonProcessingException {
        // only one action: remove post
        Map<String, String> mappedPath = 
            validator.parsePath(request.getPath(), validator.postRouteDefinition);
        if(mappedPath == null){
            return new HttpResponseFactory().buildBadRequest(
                    request.getMethod() + " " + request.getPath() + " is not allowed");
        }
        
        int postID;
        try{
            postID = Integer.parseInt(mappedPath.get("postID"));
        }catch(NumberFormatException e){
            return new HttpResponseFactory().buildBadRequest("Protocol error occurred");
        }

        // remove the post
        try{
            db.removePost(user.getUsername(), postID);
            String responseMessage = JacksonUtil.getStringFromObject(
                new HashMap<String, String>() {{
                    put("message", "Post deleted.");
                }}
            );
            return new HttpResponseFactory().buildSuccess(responseMessage);
        }catch(ResourceNotFoundException e){
            return new HttpResponseFactory().buildNotFound("The post does not exist.");
        }catch(NullPointerException e){
            // maybe working on a post which is now deleted from another thread
            return new HttpResponseFactory().buildErrorResponse("Operation not available.");
        }
    }

    private HttpResponse POSTRequestHandler(Database db, HttpRequest request, User user) 
            throws JsonProcessingException, DatabaseException {
        // only one action: create post
        if( !request.getPath().equals(validator.postSetRouteDefinition) ) 
            return new HttpResponseFactory().buildBadRequest("Protocol error occurred");
        try{
            TitleContent postProperties = 
                (TitleContent) JacksonUtil.getObjectFromString(request.getBody(), TitleContent.class);
            int postID;
            try{
                postID = db.createPost( postProperties.title, postProperties.content, user.getUsername());
            }catch(ForbiddenActionException e){
                return new HttpResponseFactory().buildBadRequest("Protocol error occurred");
            }catch(NullPointerException e){
                // maybe working on a user which is now deleted from another thread
                return new HttpResponseFactory().buildErrorResponse("Operation not available.");
            }
            // post created
            String responseMessage = JacksonUtil.getStringFromObject( 
                new HashMap<String, Integer>() {{
                    put("postID", postID);
                }}
            );
            return new HttpResponseFactory().buildSuccess(responseMessage);
        }catch(JsonProcessingException e){
            return new HttpResponseFactory().buildBadRequest("Protocol error occurred");
        }catch(DatabaseException e){
            throw e;
        }
        catch(Exception ex){
            return new HttpResponseFactory().buildBadRequest("Protocol error occurred");
        }
    }

    private HttpResponse GETRequestHandler(Database db, HttpRequest request, User user) 
            throws JsonProcessingException {
        Map<String, String> mappedPath = validator.GETPathValidation(request);
        if(mappedPath == null){
            return new HttpResponseFactory().buildBadRequest(
                    request.getMethod() + " " + request.getPath() + " is not allowed");
        }
        
        switch( request.getPath().split("/")[1] ){
            case "user":{
                // get user from userID or get own wallet
                User userFromUserID;
                try{
                    userFromUserID = new User( db.getUser( mappedPath.get("userID") ), db );
                }catch(NullPointerException e){
                    //e.printStackTrace();
                    return new HttpResponseFactory().buildNotFound("The user does not exist.");
                }
                
                // if wallet
                if(mappedPath.containsKey("btcWallet")){
                    // get own wallet
                    if(user.equals(userFromUserID)){
                        // allowed
                        String responseMessage;
                        if(mappedPath.get("btcWallet").equals("1")){
                            // get bitcoin wallet
                            try {
                                double btc = getBitcoinWallet(user.getWallet().getValue());
                                responseMessage = JacksonUtil.getStringFromObject( 
                                    new HashMap<String, Double>() {{
                                        put("wallet", btc);
                                    }}
                                );
                            } catch (IOException | InterruptedException | NumberFormatException | NullPointerException e) {
                                e.printStackTrace();
                                return new HttpResponseFactory().buildBadRequest("Sorry, we can't get this information at the moment."); 
                            }
                        }else{
                            //get wallet
                            responseMessage = JacksonUtil.getStringFromObject( 
                                new HashMap<String, Object>() {{
                                    put("wallet", user.getWallet().getValue());
                                    put("wallet_history", user.getWallet_history());
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
                    "password", "wallet", "wallet_history", "last_session"
                );
                return new HttpResponseFactory().buildSuccess(responseMessage);
            }
            case "post":{
                // get post from postID
                int postID = Integer.parseInt( mappedPath.get("postID") );
                Post postFromPostID;
                try{
                    postFromPostID = new Post(db.getPost( postID ));
                }catch(NullPointerException e){
                    return new HttpResponseFactory().buildNotFound("The post does not exist.");
                }
                String responseMessage = JacksonUtil.getStringFromObjectIgnoreFields(
                    new HashMap<String, Post>() {{
                        put("post", postFromPostID);
                    }},
                    "postFilter",
                    // ignore these
                    "postAge"
                );
                return new HttpResponseFactory().buildSuccess(responseMessage);
            }
            case "posts":{
                // get posts from userID
                Post[] postsFromUserID = db.getPostsFromUsername( mappedPath.get("userID") );
                if(postsFromUserID==null)
                    return new HttpResponseFactory().buildNotFound("The user does not exist.");
                
                String responseMessage = JacksonUtil.getStringFromObjectIgnoreFields( 
                    new HashMap<String, Post[]>() {{
                        put("posts", postsFromUserID);
                    }},
                    "postFilter",
                    //ignore these
                    "postAge"
                );
                return new HttpResponseFactory().buildSuccess(responseMessage);
            }
            case "users":{
                // get users from tagName
                HashMap<String, HashSet<User>> tagToUsers = new HashMap<String, HashSet<User>>();
                for (String tag : user.getTags()) {
                    tagToUsers.put(tag, db.getUsersFromTagname( tag ));
                }                    
                
                String responseMessage = JacksonUtil.getStringFromObjectIgnoreFields( 
                    new HashMap<String, HashMap<String, HashSet<User>>>() {{
                        put("users", tagToUsers);
                    }},
                    "userFilter",
                    // fields to ignore
                    "password", "wallet", "wallet_history", "last_session"
                );
                return new HttpResponseFactory().buildSuccess(responseMessage);
            }
            case "feed":{
                // get user feed (posts from users the user follows)
                HashMap<String, Post[]> userToPosts = new HashMap<>();
                for (String uName : user.getFollowing()) {
                    Post[] postsFromUserID = db.getPostsFromUsername( uName );
                    if(postsFromUserID!=null){
                        userToPosts.put(uName, postsFromUserID);
                    }
                }
                

                String responseMessage = JacksonUtil.getStringFromObjectIgnoreFields( 
                    new HashMap<String, HashMap<String, Post[]>>() {{
                        put("feed", userToPosts);
                    }},
                    "postFilter",
                    //ignore these
                    "postAge"
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

    @NoArgsConstructor
    private @Data static class Comment{
        protected String comment;
    }
    
}
