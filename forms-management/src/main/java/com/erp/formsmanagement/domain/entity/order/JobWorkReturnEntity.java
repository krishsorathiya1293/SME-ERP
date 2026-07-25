package com.erp.formsmanagement.domain.entity.order;

import com.erp.audit.AuditInfo;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.Objects;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "job_work_returns")
@EntityListeners(AuditingEntityListener.class)
public class JobWorkReturnEntity extends AuditInfo {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "job_work_id", nullable = false)
  private JobWorkEntity jobWork;

  /** Tare weight of a single Peti/Drum (kg) for this return. */
  private Double petiWeightKg;

  /** Total weighed gross kg for this return (weighed once, as received back). */
  private Double grossKg;

  /** Auto: grossKg - returnElementCount * petiWeightKg. */
  private Double returnKg;
  private Double ghati;
  private Double returnElementCount;
  private LocalDate jobReturnDate;

  @Enumerated(EnumType.STRING)
  private ElementType elementType;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof JobWorkReturnEntity e)) return false;
    return id != null && id.equals(e.id);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }
}
