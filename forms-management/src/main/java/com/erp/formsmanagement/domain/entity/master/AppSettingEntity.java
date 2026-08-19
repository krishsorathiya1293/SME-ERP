package com.erp.formsmanagement.domain.entity.master;

import com.erp.audit.AuditInfo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Objects;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * One console-wide setting, stored as a string against a stable key.
 *
 * <p>Key/value rather than a column per setting because these are single scalars a human edits on
 * the Settings screen — nothing joins on them or filters by them, and a new setting should not cost
 * a migration. Callers own the parsing: {@code jobwork.fixed.bajaar} is read as a number, another
 * key might be a flag.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "app_settings")
@EntityListeners(AuditingEntityListener.class)
public class AppSettingEntity extends AuditInfo {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "setting_key", nullable = false, unique = true, length = 64)
  private String settingKey;

  /** Null means "never set" — distinct from "set to empty", which the UI shows as blank too. */
  @Column(name = "setting_value", length = 512)
  private String settingValue;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof AppSettingEntity e)) return false;
    return id != null && id.equals(e.id);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }
}
