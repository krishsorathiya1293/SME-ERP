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

/** Upad: an advance / loan given to a worker, recovered later as "Loan Jama" from salary. */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "staff_upad")
@EntityListeners(AuditingEntityListener.class)
public class StaffUpadEntity extends AuditInfo {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String empCode;
  private LocalDate entryDate;
  private double amount;
  private String paymentMode;
  private String note;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof StaffUpadEntity e)) return false;
    return id != null && id.equals(e.id);
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
