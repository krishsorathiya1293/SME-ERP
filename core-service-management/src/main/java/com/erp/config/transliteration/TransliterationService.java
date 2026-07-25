package com.erp.config.transliteration;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TransliterationService {

  private final TransliterationClient client;

  public TransliterationService(TransliterationClient client) {
    this.client = client;
  }

  public String convertToHindi(String text) {
    return transliterate(text, "hi-t-i0-und");
  }

  public String convertToGujarati(String text) {
    return transliterate(text, "gu-t-i0-und");
  }

  private String transliterate(String text, String itc) {
    try {
      Object response = client.transliterate(text, itc, 1);
      List<?> outerList = (List<?>) response;
      List<?> innerList = (List<?>) outerList.get(1);
      List<?> wordData = (List<?>) innerList.getFirst();
      List<?> suggestions = (List<?>) wordData.get(1);
      return suggestions.getFirst().toString();
    } catch (Exception e) {
      log.warn("Transliteration ({}) failed for '{}', falling back to original text", itc, text, e);
      return text;
    }
  }
}
