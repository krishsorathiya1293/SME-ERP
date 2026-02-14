package com.erp.encryptionmanagement.crypto;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

public class JsonUtil {

  private static final TypeReference<Map<String, Object>> MAP_REF = new TypeReference<>() {};
  private final ObjectMapper mapper;

  public JsonUtil(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  public String write(Object obj) {
    try {
      return mapper.writeValueAsString(obj);
    } catch (Exception e) {
      throw new IllegalStateException("JSON serialize failed", e);
    }
  }

  public <T> T read(String json, Class<T> type) {
    try {
      return mapper.readValue(json, type);
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid JSON", e);
    }
  }

  public Map<String, Object> readMap(String json) {
    try {
      return mapper.readValue(json, MAP_REF);
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid JSON", e);
    }
  }

  public ObjectMapper mapper() {
    return mapper;
  }
}
