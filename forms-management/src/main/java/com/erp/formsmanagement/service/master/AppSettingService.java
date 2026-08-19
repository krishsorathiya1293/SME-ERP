package com.erp.formsmanagement.service.master;

import com.erp.formsmanagement.domain.entity.master.AppSettingEntity;
import com.erp.formsmanagement.domain.repository.master.AppSettingRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads and writes the console-wide settings.
 *
 * <p>Everything is stored as text; typed accessors live here so each caller doesn't reimplement
 * "blank means unset" and "a value the user broke must not take the screen down with it".
 */
@Service
@Transactional
public class AppSettingService {

  /** The single house rate a FIXED-bajaar job work is priced against. */
  public static final String JOBWORK_FIXED_BAJAAR = "jobwork.fixed.bajaar";

  private final AppSettingRepository repository;

  public AppSettingService(AppSettingRepository repository) {
    this.repository = repository;
  }

  @Transactional(readOnly = true)
  public Map<String, String> getAll() {
    Map<String, String> settings = new LinkedHashMap<>();
    repository.findAll().forEach(s -> settings.put(s.getSettingKey(), s.getSettingValue()));
    // Always advertise the keys the console knows about, so a fresh database still renders the
    // Settings screen with an empty field rather than nothing at all.
    settings.putIfAbsent(JOBWORK_FIXED_BAJAAR, null);
    return settings;
  }

  @Transactional(readOnly = true)
  public String get(String key) {
    return repository.findBySettingKey(key).map(AppSettingEntity::getSettingValue).orElse(null);
  }

  /**
   * The stored value as a number, or null when unset or unparseable. A setting typed by hand is
   * allowed to be wrong; callers treat that the same as "not configured" rather than failing.
   */
  @Transactional(readOnly = true)
  public Double getDouble(String key) {
    String raw = get(key);
    if (raw == null || raw.isBlank()) return null;
    try {
      return Double.valueOf(raw.trim());
    } catch (NumberFormatException e) {
      return null;
    }
  }

  /** Upsert. A blank value clears the setting back to "unset". */
  public String put(String key, String value) {
    AppSettingEntity entity =
        repository
            .findBySettingKey(key)
            .orElseGet(
                () -> {
                  AppSettingEntity fresh = new AppSettingEntity();
                  fresh.setSettingKey(key);
                  return fresh;
                });
    entity.setSettingValue(value == null || value.isBlank() ? null : value.trim());
    return repository.save(entity).getSettingValue();
  }
}
