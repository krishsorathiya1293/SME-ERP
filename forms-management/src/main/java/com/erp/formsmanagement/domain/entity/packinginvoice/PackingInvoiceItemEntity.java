package com.erp.formsmanagement.domain.entity.packinginvoice;

import com.erp.audit.AuditInfo;
import com.erp.formsmanagement.domain.entity.inventory.ItemBlueprintDataEntity;
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
@Table(name = "packing_invoice_items")
@EntityListeners(AuditingEntityListener.class)
public class PackingInvoiceItemEntity extends AuditInfo {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "packing_invoice_id", nullable = false)
  private PackingInvoiceEntity packingInvoice;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "size_id", nullable = false)
  private ItemBlueprintDataEntity size;

  private String finish;
  private Double box;
  private Double pc;
  private Double totalPc;
  private Double scrap;
  private Double laboure;
  private Double rsKg;
  private Double boxWeight;
  private Double boxWeightAcDocWeight;
  private Double billCalDocWeight;
  private Double ratePc;
  private Double totalRs;
  private Double totalKg;
  private Double asPerDocWeight;
  private Double loss;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PackingInvoiceItemEntity e)) return false;
    return id != null && id.equals(e.id);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }
}
