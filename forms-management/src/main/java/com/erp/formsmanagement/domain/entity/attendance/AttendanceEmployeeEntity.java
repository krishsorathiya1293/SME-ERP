package com.erp.formsmanagement.domain.entity.attendance;

import com.erp.audit.AuditInfo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * A worker. Two owners on one row:
 *
 * <ul>
 *   <li>the face device (EasyTime personnel_employee), via the sync: empCode, firstName,
 *       lastName, department — the sync only ever writes these;
 *   <li>the ERP (the client's "Add Emp." sheet): name, gender, phone, Aadhar, joining date,
 *       department + salary, photos, opening balances, active/deleted — never touched by the sync.
 * </ul>
 */
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

  // ---- device-owned (sync) ----
  private String empCode;
  private String firstName;
  private String lastName;
  private String department;

  // ---- ERP-owned (profile) ----
  private String name;
  private String gender;
  private String phone;
  private String aadharNo;
  private LocalDate birthDate;
  private String reference;
  private LocalDate joiningDate;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "staff_department_id")
  private StaffDepartmentEntity staffDepartment;

  private Double perHourSalary;
  private String payMode;

  private double openingBalance;
  private double openingLoan;
  private LocalDate balanceFrom;

  private boolean active = true;
  private boolean deleted;

  @Column(columnDefinition = "text")
  private String photo;

  @Column(columnDefinition = "text")
  private String photoThumb;

  @Column(columnDefinition = "text")
  private String aadharPhoto;

  /** The name the device knows (EasyTime first + last). */
  public String getDeviceName() {
    String full = ((firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName));
    return full.isBlank() ? null : full.trim();
  }

  /** The ERP name when set, otherwise the device name. */
  public String getFullName() {
    return name != null && !name.isBlank() ? name.trim() : getDeviceName();
  }

  /** The ERP department when set, otherwise whatever the device calls it. */
  public String getDepartmentName() {
    return staffDepartment != null ? staffDepartment.getName() : department;
  }

  /** Per-hour salary is filled in, so the day can be paid. */
  public boolean isSalarySet() {
    return perHourSalary != null && perHourSalary > 0;
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
