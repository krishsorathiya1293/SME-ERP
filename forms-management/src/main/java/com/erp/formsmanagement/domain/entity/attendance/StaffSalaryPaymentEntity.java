package com.erp.formsmanagement.domain.entity.attendance;

import com.erp.audit.AuditInfo;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * "Pagar Didho": one settled pay period. The money figures are snapshots taken when it was paid,
 * so history stays put even if the worker's salary or attendance is edited later.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "staff_salary_payment")
@EntityListeners(AuditingEntityListener.class)
public class StaffSalaryPaymentEntity extends AuditInfo {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String empCode;
  private LocalDate periodFrom;
  private LocalDate periodTo;
  private double openingBalance;
  private double totalSalary;
  private double bonus;
  private double loanDeduction;
  private double payable;
  private double paidAmount;
  private String paymentMode;
  private LocalDate paidOn;
  private String note;

  /** What the worker is still owed after this payment (+) or owes back (-). */
  public double balanceAfter() {
    return payable - paidAmount;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof StaffSalaryPaymentEntity e)) return false;
    return id != null && id.equals(e.id);
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
