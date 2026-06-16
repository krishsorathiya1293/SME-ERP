package com.erp.formsmanagement.domain.entity.client;

import com.erp.audit.AuditInfo;
import com.erp.formsmanagement.domain.entity.inventory.ItemBlueprintDataEntity;
import com.erp.formsmanagement.domain.entity.master.PartyEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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
@Table(name = "client_inventory")
@EntityListeners(AuditingEntityListener.class)
public class ClientInventoryEntity extends AuditInfo {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "client_id", nullable = false)
  private PartyEntity party;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "size_id", nullable = false)
  private ItemBlueprintDataEntity size;

  private Integer pcsPerBox;
  private Integer boxPerCarton;
  private Integer pcsPerCarton;
  private Double cartonWeight;

  private Double sssatinlacq;
  private Double antiq;
  private Double sidegold;
  private Double sartinlacq;
  private Double zblack;
  private Double grblack;
  private Double mattss;
  private Double mattantiq;
  private Double pvdrose;
  private Double pvdgold;
  private Double pvdblack;
  private Double rosegold;
  private Double clearlacq;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ClientInventoryEntity e)) return false;
    return id != null && id.equals(e.id);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }
}
