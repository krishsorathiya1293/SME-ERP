package com.erp.usermanagement.config;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "security")
public class SecurityProperties {
  private List<String> publicAccess = new ArrayList<>();

  private BootstrapAdmin bootstrapAdmin = new BootstrapAdmin();

  /**
   * Login for the very first Admin, read once at startup by {@link FirstAdminBootstrap}. Both
   * values are empty unless the operator sets BOOTSTRAP_ADMIN_EMAIL / BOOTSTRAP_ADMIN_PASSWORD.
   */
  @Getter
  @Setter
  public static class BootstrapAdmin {
    private String email = "";
    private String password = "";
  }
}
