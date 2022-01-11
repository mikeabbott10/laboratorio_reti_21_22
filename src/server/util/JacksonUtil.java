package server.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

public class JacksonUtil {
    /**
     * Get a string representation of obj ignoring some field
     * @param obj
     * @param filterName the name of the filter
     * @param ignoredFields the fields of obj to ignore
     * @return the string representation of obj
     * @throws JsonProcessingException
     */
    public static String getStringFromObjectIgnoreFields(Object obj, String filterName, String... ignoredFields)
            throws JsonProcessingException{
        SimpleBeanPropertyFilter theFilter = SimpleBeanPropertyFilter
            .serializeAllExcept(ignoredFields);
        FilterProvider filters = new SimpleFilterProvider()
            .addFilter(filterName, theFilter);
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
