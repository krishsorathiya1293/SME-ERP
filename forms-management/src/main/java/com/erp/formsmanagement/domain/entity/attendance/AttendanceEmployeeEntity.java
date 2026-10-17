package com.erp.formsmanagement.domain.entity.attendance;

import com.erp.audit.AuditInfo;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/** Copy of an EasyTime employee (personnel_employee), keyed by the code the device knows. */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "attendance_employee")
@EntityListeners(AuditingEntityListener.class)
public class AttendanceEmployeeEntity extends AuditInfo {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String empCode;
  private String firstName;
  private String lastName;
  private String department;

  public String getFullName() {
    String full = ((firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName));
    return full.isBlank() ? null : full.trim();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof AttendanceEmployeeEntity e)) return false;
    return id != null && id.equals(e.id);
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
