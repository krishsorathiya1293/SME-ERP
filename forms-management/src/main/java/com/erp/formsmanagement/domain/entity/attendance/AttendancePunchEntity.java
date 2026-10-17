package com.erp.formsmanagement.domain.entity.attendance;

import com.erp.audit.AuditInfo;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * One face/finger punch from the factory device, as EasyTime stored it (iclock_transaction).
 * Written only by the sync endpoint's ON CONFLICT insert; never edited in the ERP.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "attendance_punch")
@EntityListeners(AuditingEntityListener.class)
public class AttendancePunchEntity extends AuditInfo {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String empCode;
  // Factory local time — the device has no zone and neither does EasyTime.
  private LocalDateTime punchTime;
  private String punchState;
  private Integer verifyType;
  private String terminalSn;
  private String terminalAlias;
  private Long sourceId;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof AttendancePunchEntity e)) return false;
    return id != null && id.equals(e.id);
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
