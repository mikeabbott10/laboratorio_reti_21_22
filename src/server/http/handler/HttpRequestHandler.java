package server.http.handler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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
                Map<String, String> mappedPath = validator.GETPathValidation(request);
                if(mappedPath == null){
                    return new HttpResponseFactory().buildBadRequest(
                            request.getMethod() + " " + request.getPath() + " is not allowed");
                }
                
                switch( request.getPath().split("/")[1] ){
                    case "user":{
                        // get user from userID
                        User userFromUserID = db.getUser( mappedPath.get("userID") );
                        if(userFromUserID==null)
                            return new HttpResponseFactory().buildNotFound("The user does not exist.");
                        //String userFromUserIDString = JacksonUtil.getStringFromObject(userFromUserID);
                        String responseMessage = JacksonUtil.getStringFromObjectIgnoreField( 
                            new HashMap<String, User>() {{
                                put("user", userFromUserID);
                            }}, 
                            "password"
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
                        HashMap<String, User> usersFromTagname = db.getUsersFromTagname( mappedPath.get("tagName") );
                        
                        String responseMessage = JacksonUtil.getStringFromObjectIgnoreField( 
                            new HashMap<String, HashMap<String, User>>() {{
                                put("users", usersFromTagname);
                            }},
                            "password"
                        );
                        return new HttpResponseFactory().buildSuccess(responseMessage);
                    }
                }
                
                break;
            }
            case "POST":{
                // only one action: create post
                if( !request.getPath().equals(validator.postSetRouteDefinition) )
                    break;
                TitleContent postProperties = (TitleContent) JacksonUtil.getObjectFromString(request.getBody(), TitleContent.class);
                int postID = db.createPost( postProperties.title, postProperties.content, user.getUsername());
                // post created
                String responseMessage = JacksonUtil.getStringFromObject( new HashMap<String, Integer>() {{
                    put("postID", postID);
                }});
                return new HttpResponseFactory().buildSuccess(responseMessage);
            }
            case "PUT":{
                break;
            }
            case "DELETE":{
                // only one action: remove post
                Map<String, String> mappedPath = validator.parsePath(request.getPath(), validator.postRouteDefinition);
                if(mappedPath == null){
                    return new HttpResponseFactory().buildBadRequest(
                            request.getMethod() + " " + request.getPath() + " is not allowed");
                }
                
                // remove the post
                if( db.removePost(user, Integer.parseInt(mappedPath.get("postID"), 10)) ){
                    String responseMessage = JacksonUtil.getStringFromObject(new HashMap<String, String>() {{
                        put("message", "Post deleted.");
                    }});
                    return new HttpResponseFactory().buildSuccess(responseMessage);
                }
                return new HttpResponseFactory().buildNotFound("The post does not exist.");
            }
            default:
                assert true == false; // never here
        }

        return new HttpResponseFactory().buildBadRequest("Protocol error occurred");
    }

    @NoArgsConstructor
    private @Data static class TitleContent{
        protected String title;
        protected String content;
    }
    
}
