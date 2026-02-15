package com.erp.exportmanagement.assets;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import lombok.Getter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
@Getter
public class Base64Assets {

  private final String companyLogoPng;
  private final String stampJpg;

  public Base64Assets() {
    this.companyLogoPng = readB64("assets/base64/company-logo.png.b64");
    this.stampJpg = readB64("assets/base64/stamp.jpg.b64");
  }

  private String readB64(String path) {
    try (InputStream is = new ClassPathResource(path).getInputStream()) {
      return new String(is.readAllBytes(), StandardCharsets.UTF_8).replaceAll("\\s+", "").trim();
    } catch (IOException e) {
      throw new IllegalStateException("Missing base64 asset: " + path, e);
    }
  }
}
