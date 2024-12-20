package com.micro.learningplatform.shared.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@Converter
public class JsonbConverter implements AttributeConverter<Map<String, Object>, String> {

   // ObjectMapper je thread-safe, bolje ga je imati kao static
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger log = LogManager.getLogger(JsonbConverter.class);


    @Override
    public String convertToDatabaseColumn(Map<String, Object> attribute) {
        try {
            String jsonValue = attribute == null ? "{}" : objectMapper.writeValueAsString(attribute);
            log.debug("Converting to database column. Input: {}, Output: {}", attribute, jsonValue);
            return jsonValue;
        } catch (JsonProcessingException e) {
            log.error("Error converting Map to JSON", e);
            throw new IllegalArgumentException("Could not convert Map to JSON string.", e);
        }
    }

    @Override
    public Map<String, Object> convertToEntityAttribute(String dbData) {
        try {
            if (dbData == null || dbData.trim().isEmpty()) {
                return new HashMap<>();
            }

            Map<String, Object> result = objectMapper.readValue(dbData, Map.class);
            log.debug("Converting from database column. Input: {}, Output: {}", dbData, result);
            return result;
        } catch (JsonProcessingException e) {
            log.error("Error converting JSON to Map: {}", dbData, e);
            throw new IllegalArgumentException("Could not convert JSON string to Map.", e);
        }
    }

}
