package com.erp.formsmanagement.domain.entity.sales;

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
import jakarta.persistence.ManyToOne;
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
@Table(name = "sales_order_items")
@EntityListeners(AuditingEntityListener.class)
public class SalesOrderItemEntity extends AuditInfo {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "sales_order_id", nullable = false)
  private SalesOrderEntity salesOrder;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "size_id")
  private ItemBlueprintDataEntity size;

  private Double unitKg;

  @Enumerated(EnumType.STRING)
  private SalesUnitType unitType;

  private Double elementCount;

  @Enumerated(EnumType.STRING)
  private SalesElementType elementType;

  private Double scrap;
  private Double labour;
  private Double price;
  private Double totalPrice;
  private Double javakKgPc;
  private Double javakRs;
  private Double javakTotalRs;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof SalesOrderItemEntity e)) return false;
    return id != null && id.equals(e.id);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }
}
