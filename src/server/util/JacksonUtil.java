package server.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

public class JacksonUtil {
    public static String getStringFromObjectIgnoreField(Object obj, String ignoredField)
            throws JsonProcessingException{
        SimpleBeanPropertyFilter theFilter = SimpleBeanPropertyFilter
            .serializeAllExcept(ignoredField);
        FilterProvider filters = new SimpleFilterProvider()
            .addFilter("userPasswordFilter", theFilter);
        var objectMapper = new ObjectMapper();
        return objectMapper.writer(filters).writeValueAsString(obj);
        
    }

    public static String getStringFromObject(Object obj) 
            throws JsonMappingException, JsonProcessingException {
        var objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(obj);
    }

    public static Object getObjectFromString(String string, Class<?> clas) 
            throws JsonMappingException, JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(string, clas);
    }
}
