package com.erp.formsmanagement.domain.entity.attendance;

import com.erp.audit.AuditInfo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/** Payroll-wide key/value settings: WEEK_START (pay week), WEEKLY_OFF. */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "staff_setting")
@EntityListeners(AuditingEntityListener.class)
public class StaffSettingEntity extends AuditInfo {

  public static final String WEEK_START = "WEEK_START";
  public static final String WEEKLY_OFF = "WEEKLY_OFF";

  @Id
  @Column(name = "setting_key")
  private String key;

  @Column(name = "setting_value")
  private String value;
}
