package com.erp.usermanagement.config;

import com.erp.usermanagement.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Creates the first Admin from startup configuration. Registering a user needs an Admin token, so a
 * database with no Admin has nobody who could create one; whoever can set the server's environment
 * supplies that first login here instead of through an open endpoint.
 *
 * <p>Set BOOTSTRAP_ADMIN_EMAIL and BOOTSTRAP_ADMIN_PASSWORD, start the application once, then remove
 * them. They are ignored as soon as any Admin exists, so they can never add a second Admin or reset
 * an existing password.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FirstAdminBootstrap implements ApplicationRunner {

  private final SecurityProperties securityProperties;
  private final UserService userService;

  @Override
  public void run(ApplicationArguments args) {
    SecurityProperties.BootstrapAdmin bootstrap = securityProperties.getBootstrapAdmin();
    String email = bootstrap.getEmail() == null ? "" : bootstrap.getEmail().trim();
    String password = bootstrap.getPassword() == null ? "" : bootstrap.getPassword();

    if (email.isEmpty() || password.isBlank()) {
      return;
    }

    // A bad value here (say, an email already used by a client login) must not stop the ERP from
    // starting: report it and carry on without the bootstrap Admin.
    try {
      userService
          .createFirstAdmin(email, password)
          .ifPresentOrElse(
              admin ->
                  log.warn(
                      "Created the first Admin login '{}' from BOOTSTRAP_ADMIN_EMAIL. Remove"
                          + " BOOTSTRAP_ADMIN_EMAIL and BOOTSTRAP_ADMIN_PASSWORD from the"
                          + " environment now.",
                      admin.getUsername()),
              () ->
                  log.warn(
                      "BOOTSTRAP_ADMIN_EMAIL / BOOTSTRAP_ADMIN_PASSWORD are set but an Admin"
                          + " already exists, so they were ignored. Remove them from the"
                          + " environment."));
    } catch (RuntimeException e) {
      log.error("Could not create the first Admin from BOOTSTRAP_ADMIN_EMAIL: {}", e.getMessage());
    }
  }
}
