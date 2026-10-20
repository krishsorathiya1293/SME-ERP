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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
  // Every in/out as "HH:mm,HH:mm,...", in working-day order; null on edits made before V65.
  private String punchTimes;
  // null = use the department break.
  private Integer breakMinutes;
  private boolean absent;
  private String note;

  /** The day's in/outs in order: the full list when kept, else just in and out. */
  public List<LocalTime> times() {
    List<LocalTime> out = new ArrayList<>();
    if (absent) return out;
    if (punchTimes != null && !punchTimes.isBlank()) {
      Arrays.stream(punchTimes.split(",")).map(String::trim).filter(t -> !t.isEmpty())
          .map(LocalTime::parse).forEach(out::add);
      return out;
    }
    if (inTime != null) out.add(inTime);
    if (outTime != null) out.add(outTime);
    return out;
  }

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
