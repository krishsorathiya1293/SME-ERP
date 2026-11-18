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
 * A bonus or upad jama entered on any day from Pagar Didho, without settling the pay week.
 *
 * <ul>
 *   <li>BONUS: {@code amount} is added to what the company owes; {@code paidNow} of it was handed
 *       over on the day, the rest goes out with the salary.
 *   <li>JAMA: the worker gave {@code amount} of upad back in cash. Pending upad goes down; the
 *       salary balance is not touched.
 * </ul>
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "staff_bonus_jama")
@EntityListeners(AuditingEntityListener.class)
public class StaffBonusJamaEntity extends AuditInfo {

  public static final String BONUS = "BONUS";
  public static final String JAMA = "JAMA";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String empCode;
  private String entryType;
  private LocalDate entryDate;
  private double amount;
  private double paidNow;
  private String paymentMode;
  private String note;

  public boolean isBonus() {
    return BONUS.equals(entryType);
  }

  public boolean isJama() {
    return JAMA.equals(entryType);
  }

  /** What this entry adds to the company-owes balance: a bonus less what was handed over. */
  public double balanceEffect() {
    return isBonus() ? amount - paidNow : 0;
  }

  /** What this entry takes off the pending upad. */
  public double loanEffect() {
    return isJama() ? amount : 0;
  }

  public boolean on(LocalDate from, LocalDate to) {
    return !entryDate.isBefore(from) && !entryDate.isAfter(to);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof StaffBonusJamaEntity e)) return false;
    return id != null && id.equals(e.id);
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
