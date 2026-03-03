package com.erp.formsmanagement.domain.entity.master;

import com.erp.audit.AuditInfo;
import com.erp.formsmanagement.domain.entity.inventory.ItemBlueprintDataEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.Objects;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "item")
@EntityListeners(AuditingEntityListener.class)
public class ItemEntity extends AuditInfo {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "size_id")
  private ItemBlueprintDataEntity size;

  private Double itemKg;
  private Double weightPerPc;
  private Double totalPc;
  private Double lowStockWarning;

  @Enumerated(EnumType.STRING)
  private StockStatus stockStatus;

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof ItemEntity e))
      return false;
    return id != null && id.equals(e.id);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }
}
