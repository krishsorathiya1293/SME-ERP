package com.erp.config.transliteration;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TransliterationService {

  private final TransliterationClient client;

  public TransliterationService(TransliterationClient client) {
    this.client = client;
  }

  public String convertToHindi(String text) {
    Object response = client.transliterate(text, "hi-t-i0-und", 1);
    List<?> outerList = (List<?>) response;
    List<?> innerList = (List<?>) outerList.get(1);
    List<?> wordData = (List<?>) innerList.getFirst();
    List<?> suggestions = (List<?>) wordData.get(1);
    return suggestions.getFirst().toString();
  }
}
