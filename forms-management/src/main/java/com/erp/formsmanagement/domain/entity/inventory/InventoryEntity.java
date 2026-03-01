package com.erp.formsmanagement.domain.entity.inventory;

import com.erp.audit.AuditInfo;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "inventory")
@EntityListeners(AuditingEntityListener.class)
public class InventoryEntity extends AuditInfo {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private Integer pcsPerBox;
  private Integer boxPerCarton;
  private Integer pcsPerCarton;
  private Double cartonWeight;
  private Double sssatinlacq;
  private Double antiq;
  private Double sidegold;
  private Double zblack;
  private Double grblack;
  private Double mattss;
  private Double mattantiq;
  private Double pvdrose;
  private Double pvdgold;
  private Double pvdblack;
  private Double rosegold;
  private Double clearlacq;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "size_id", nullable = false, unique = true)
  private ItemBlueprintDataEntity size;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof InventoryEntity)) return false;
    return id != null && id.equals(((InventoryEntity) o).id);
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
