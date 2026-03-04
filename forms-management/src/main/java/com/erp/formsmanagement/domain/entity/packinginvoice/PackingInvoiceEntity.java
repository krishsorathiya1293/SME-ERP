package com.erp.formsmanagement.domain.entity.packinginvoice;

import com.erp.audit.AuditInfo;
import com.erp.formsmanagement.domain.entity.master.PartyEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "packing_invoice")
@EntityListeners(AuditingEntityListener.class)
public class PackingInvoiceEntity extends AuditInfo {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private LocalDate invoiceDate;

  /** Auto-generated: sequential per party+date (01, 02, 03…) */
  private String invoiceNo;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "party_id", nullable = false)
  private PartyEntity party;

  /** Auto-generated: cumulative sequential per party across all dates (01, 02, 03…) */
  private String cartoonNo;

  @OneToMany(mappedBy = "packingInvoice", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<PackingInvoiceItemEntity> items;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PackingInvoiceEntity e)) return false;
    return id != null && id.equals(e.id);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }

  @Transient
  public List<PackingInvoiceItemEntity> getSummaryItems() {
    Map<String, PackingInvoiceItemEntity> map = new LinkedHashMap<>();
    if (items != null) {
      for (PackingInvoiceItemEntity item : items) {
        if (item.getSize() == null) continue;
        String key = item.getSize().getId() + "-" + (item.getFinish() != null ? item.getFinish() : "");
        if (map.containsKey(key)) {
          PackingInvoiceItemEntity existing = map.get(key);
          existing.setTotalPc((existing.getTotalPc() != null ? existing.getTotalPc() : 0.0)
              + (item.getTotalPc() != null ? item.getTotalPc() : 0.0));
          existing.setTotalRs((existing.getTotalRs() != null ? existing.getTotalRs() : 0.0)
              + (item.getTotalRs() != null ? item.getTotalRs() : 0.0));
        } else {
          PackingInvoiceItemEntity copy = new PackingInvoiceItemEntity();
          copy.setSize(item.getSize());
          copy.setFinish(item.getFinish());
          copy.setTotalPc(item.getTotalPc() != null ? item.getTotalPc() : 0.0);
          copy.setTotalRs(item.getTotalRs() != null ? item.getTotalRs() : 0.0);
          copy.setScrap(item.getScrap());
          copy.setRsKg(item.getRsKg());
          copy.setRatePc(item.getRatePc());
          map.put(key, copy);
        }
      }
    }
    return new ArrayList<>(map.values());
  }
}
