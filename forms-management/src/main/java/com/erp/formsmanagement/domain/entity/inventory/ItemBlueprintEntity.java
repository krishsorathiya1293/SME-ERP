package com.erp.formsmanagement.domain.entity.inventory;

import com.erp.audit.AuditInfo;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "item_inventory")
@EntityListeners(AuditingEntityListener.class)
public class ItemBlueprintEntity extends AuditInfo {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String itemName;

  @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<ItemBlueprintDataEntity> sizes;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ItemBlueprintEntity)) return false;
    return id != null && id.equals(((ItemBlueprintEntity) o).id);
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
