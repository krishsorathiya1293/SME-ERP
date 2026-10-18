package com.erp.formsmanagement.domain.entity.attendance;

import com.erp.audit.AuditInfo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * A department and its shift (sheet "Add Emp." of the client's Excel): morning in/out, then back
 * in/out after the break. Standard working time = both halves; break = out1 -> in2. Times are read
 * within the factory's 7 AM - 7 AM working day ({@link WorkDay}), so night shifts work too.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "staff_department")
@EntityListeners(AuditingEntityListener.class)
public class StaffDepartmentEntity extends AuditInfo {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String name;

  @Column(name = "in_time_1")
  private LocalTime inTime1;

  @Column(name = "out_time_1")
  private LocalTime outTime1;

  @Column(name = "in_time_2")
  private LocalTime inTime2;

  @Column(name = "out_time_2")
  private LocalTime outTime2;

  public boolean hasBreak() {
    return inTime2 != null && outTime2 != null;
  }

  /** Lunch break between the two halves, in minutes (0 for a single-half shift). */
  public int breakMinutes() {
    return hasBreak() ? minutes(outTime1, inTime2) : 0;
  }

  /** Working minutes per day, break excluded. */
  public int standardMinutes() {
    int first = minutes(inTime1, outTime1);
    return hasBreak() ? first + minutes(inTime2, outTime2) : first;
  }

  /** When the day ends: out of the second half, or of the only half. */
  public LocalTime dayEnd() {
    return hasBreak() ? outTime2 : outTime1;
  }

  // Within the 7-to-7 working day, so a night shift (20:00 -> 04:00) measures correctly.
  private static int minutes(LocalTime a, LocalTime b) {
    return WorkDay.between(a, b);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof StaffDepartmentEntity e)) return false;
    return id != null && id.equals(e.id);
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
