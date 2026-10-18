package com.erp.formsmanagement.domain.entity.attendance;

import com.erp.audit.AuditInfo;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/** "Edit Attendence - manually": a hand-set day that replaces that day's device punches. */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "attendance_adjustment")
@EntityListeners(AuditingEntityListener.class)
public class AttendanceAdjustmentEntity extends AuditInfo {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String empCode;
  private LocalDate workDate;
  private LocalTime inTime;
  private LocalTime outTime;
  // null = use the department break.
  private Integer breakMinutes;
  private boolean absent;
  private String note;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof AttendanceAdjustmentEntity e)) return false;
    return id != null && id.equals(e.id);
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
