package com.erp.formsmanagement.domain.entity.master;

import com.erp.audit.AuditInfo;
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
 * One dated stock change of a size: ADD puts petis on top of the stock, SET replaces it. The
 * {@code *After} fields are the stock right after this entry, so the history reads as a ledger.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "stock_entry")
@EntityListeners(AuditingEntityListener.class)
public class StockEntryEntity extends AuditInfo {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "item_id", nullable = false)
  private ItemEntity item;

  private String entryType;
  private LocalDate entryDate;
  private Integer petiCount;
  private Double grossKg;
  private Double emptyPetiWeight;
  private double netKg;
  private Double weightPerPc;
  private double pcs;
  private Integer totalPetiAfter;
  private double totalKgAfter;
  private double totalPcAfter;
  private String note;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof StockEntryEntity e)) return false;
    return id != null && id.equals(e.id);
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
