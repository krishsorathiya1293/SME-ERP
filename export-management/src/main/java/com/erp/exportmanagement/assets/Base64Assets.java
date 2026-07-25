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

  private final String companyLogo;

  /** Same logo cropped to its content box — for small formats (stickers) where padding wastes space. */
  private final String companyLogoMark;

  private final String stamp;

  public Base64Assets() {
    this.companyLogo = "data:image/png;base64," + readB64("assets/base64/company-logo.png.b64");
    this.companyLogoMark =
        "data:image/png;base64," + readB64("assets/base64/company-logo-mark.png.b64");
    this.stamp = "data:image/jpeg;base64," + readB64("assets/base64/stamp.jpg.b64");
  }

  private String readB64(String path) {
    try (InputStream is = new ClassPathResource(path).getInputStream()) {
      return new String(is.readAllBytes(), StandardCharsets.UTF_8).replaceAll("\\s+", "").trim();
    } catch (IOException e) {
      throw new IllegalStateException("Missing base64 asset: " + path, e);
    }
  }
}
