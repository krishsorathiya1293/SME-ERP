package com.erp.formsmanagement.domain.entity.inventory;

import com.erp.audit.AuditInfo;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "size_inventory")
@EntityListeners(AuditingEntityListener.class)
public class ItemBlueprintDataEntity extends AuditInfo {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String sizeInInch;
  private String sizeInMm;
  private Double dozenWeight;
  private Double pcsWeight;

  /**
   * The row's highlight in the stock master, marked up the way a spreadsheet is: one of a small
   * fixed set of colour names, and the reason for it.
   *
   * <p>A name rather than a hex value, so the palette stays the console's and a row keeps reading
   * correctly if the theme moves. Null in both means an unmarked row. They are set and cleared
   * together — a coloured row nobody can explain is worse than no colour at all.
   */
  private String highlightColor;

  private String highlightNote;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "item_name_inventory_id", nullable = false)
  private ItemBlueprintEntity item;

  @OneToOne(mappedBy = "size", cascade = CascadeType.ALL, orphanRemoval = true)
  private InventoryEntity inventory;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ItemBlueprintDataEntity)) return false;
    return id != null && id.equals(((ItemBlueprintDataEntity) o).id);
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
