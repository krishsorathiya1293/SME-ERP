package com.erp.formsmanagement.domain.entity.gres;

import com.erp.formsmanagement.domain.entity.inventory.ItemBlueprintDataEntity;
import jakarta.persistence.Entity;
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

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "gres_filling_items")
public class GresFillingItemEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "gres_filling_id", nullable = false)
  private GresFillingEntity gresFilling;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "size_id")
  private ItemBlueprintDataEntity size;

  private Double unitKg;
  private String unitType;
  private Double elementCount;

  @Enumerated(EnumType.STRING)
  private GresElementType elementType;

  private Double netWeight;
  private Double ratePerKg;
  private Double totalAmount;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof GresFillingItemEntity e)) return false;
    return id != null && id.equals(e.id);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }
}
