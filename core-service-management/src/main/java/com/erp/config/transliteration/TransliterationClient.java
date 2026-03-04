package com.erp.config.transliteration;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "transliterationClient", url = "https://inputtools.google.com")
public interface TransliterationClient {

  @GetMapping("/request")
  Object transliterate(
      @RequestParam("text") String text,
      @RequestParam("itc") String itc,
      @RequestParam("num") int num);
}
