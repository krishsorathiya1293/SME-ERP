package com.erp.formsmanagement.clientportal.domain.entity;

import com.erp.audit.AuditInfo;
import com.erp.formsmanagement.domain.entity.inventory.ItemBlueprintDataEntity;
import com.erp.formsmanagement.domain.entity.inventory.ItemBlueprintEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "client_order_request_items")
@EntityListeners(AuditingEntityListener.class)
public class ClientOrderRequestItemEntity extends AuditInfo {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "request_id", nullable = false)
  private ClientOrderRequestEntity request;

  // Reference to the catalog item/size at the time of ordering. Nullable so the
  // request remains valid even if the item/size is later removed from the catalog.
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "item_id")
  private ItemBlueprintEntity item;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "size_id")
  private ItemBlueprintDataEntity itemSize;

  // Snapshot fields, captured at submission time so the request stays accurate
  // even if the catalog item/size is later renamed or removed.
  private String itemName;
  private String category;
  private String sizeInInch;
  private String sizeInMm;
  private String plating;

  private Double qtyPc;
  private Double qtyKg;
  private Double pendingPc;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ClientOrderRequestItemEntity)) return false;
    return id != null && id.equals(((ClientOrderRequestItemEntity) o).id);
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
