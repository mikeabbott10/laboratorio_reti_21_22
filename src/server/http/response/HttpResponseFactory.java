package server.http.response;

import java.util.HashMap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import server.util.JacksonUtil;
import server.util.Constants.HttpStatus;

/**
 * A simple factory for {@link HttpResponse}.
 */
public final class HttpResponseFactory {

    public HttpResponse buildBadRequest(String msg) throws JsonMappingException, JsonProcessingException {
        String responseMessage = JacksonUtil.getStringFromObject( new HashMap<String, String>() {{
            put("message", msg);
        }});
        return buildImmediateResponse(HttpStatus.BAD_REQUEST, responseMessage);
    }

    public HttpResponse buildErrorResponse(String msg) throws JsonMappingException, JsonProcessingException {
        String responseMessage = JacksonUtil.getStringFromObject( new HashMap<String, String>() {{
            put("message", msg);
        }});
        return buildImmediateResponse(HttpStatus.REQUEST_TIMEOUT, responseMessage);
    }

    public HttpResponse buildNotFound(String msg) throws JsonMappingException, JsonProcessingException {
        String responseMessage = JacksonUtil.getStringFromObject( new HashMap<String, String>() {{
            put("message", msg);
        }});
        return buildImmediateResponse(HttpStatus.NOT_FOUND, responseMessage);
    }

    public HttpResponse buildForbiddenResponse(String msg) throws JsonMappingException, JsonProcessingException {
        String responseMessage = JacksonUtil.getStringFromObject( new HashMap<String, String>() {{
            put("message", msg);
        }});
        return buildImmediateResponse(HttpStatus.FORBIDDEN, responseMessage);
    }

    public HttpResponse buildSuccess(String msg){
        return buildImmediateResponse(HttpStatus.SUCCESS, msg);
    }

    private HttpResponse buildImmediateResponse(HttpStatus status, String msg) {
        HttpResponse response = new HttpResponse();
        response.setCode(status.code);
        response.setReason(status.reason);
        byte[] content = msg.getBytes();
        response.addContentChunk(content);
        response.setContentLength(content.length);
        response.markAsComplete();
        response.addDefaultHeaders();
        return response;
    }

}