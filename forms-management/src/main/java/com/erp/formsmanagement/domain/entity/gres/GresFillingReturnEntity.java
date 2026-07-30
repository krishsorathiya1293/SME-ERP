package com.erp.formsmanagement.domain.entity.gres;

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
@Table(name = "gres_filling_returns")
@EntityListeners(AuditingEntityListener.class)
public class GresFillingReturnEntity extends AuditInfo {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "gres_filling_id", nullable = false)
  private GresFillingEntity gresFilling;

  // Gross weight the vendor sent back (Excel "Kgs :- 153.000 Kg"). Manual entry.
  private Double grossKg;
  // Tare per Peti in Kg (Excel "1 Peti Weight :- 1 Kg"). Manual entry.
  private Double petiWeightKg;
  // Net returned Kg — server-computed from grossKg - returnElementCount * petiWeightKg.
  private Double returnKg;
  // Diff between return net and the job's own net (positive = returned more).
  private Double ghati;
  private Double returnElementCount;

  @Enumerated(EnumType.STRING)
  private GresElementType elementType;

  private LocalDate returnDate;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof GresFillingReturnEntity e)) return false;
    return id != null && id.equals(e.id);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }
}
