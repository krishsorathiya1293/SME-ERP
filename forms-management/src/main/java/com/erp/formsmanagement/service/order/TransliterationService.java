package com.erp.formsmanagement.service.order;

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
    List<?> wordData = (List<?>) innerList.get(0);
    List<?> suggestions = (List<?>) wordData.get(1);

    return suggestions.get(0).toString();
  }
}
