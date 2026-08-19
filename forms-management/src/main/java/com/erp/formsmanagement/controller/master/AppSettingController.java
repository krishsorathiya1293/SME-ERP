package com.erp.formsmanagement.controller.master;

import com.erp.formsmanagement.service.master.AppSettingService;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The console-wide settings, as a flat key/value map.
 *
 * <p>Deliberately not modelled in the OpenAPI contract: there is no resource here, just a handful
 * of scalars the Settings screen edits, and giving each one a generated client would be more
 * ceremony than the feature is worth.
 */
@RestController
@RequestMapping("/api/v1/app-settings")
public class AppSettingController {

  private final AppSettingService service;

  public AppSettingController(AppSettingService service) {
    this.service = service;
  }

  @GetMapping
  public ResponseEntity<Map<String, String>> getAll() {
    return ResponseEntity.ok(service.getAll());
  }

  /** Body is {@code {"value": "..."}}; a blank or absent value clears the setting. */
  @PutMapping("/{key}")
  public ResponseEntity<Map<String, String>> put(
      @PathVariable String key, @RequestBody(required = false) Map<String, String> body) {
    String value = body == null ? null : body.get("value");
    // HashMap, not Map.of: clearing a setting stores null and Map.of rejects null values.
    Map<String, String> saved = new HashMap<>();
    saved.put("key", key);
    saved.put("value", service.put(key, value));
    return ResponseEntity.ok(saved);
  }
}
