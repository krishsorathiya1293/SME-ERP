package com.erp.formsmanagement.domain.entity.order;

import com.erp.audit.AuditInfo;
import com.erp.formsmanagement.domain.entity.inventory.ItemBlueprintDataEntity;
import com.erp.formsmanagement.domain.entity.master.PartyEntity;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.Objects;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "job_works")
@EntityListeners(AuditingEntityListener.class)
public class JobWorkEntity extends AuditInfo {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "order_item_id", nullable = false, unique = true)
  private OrderItemEntity orderItem;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "party_id", nullable = false)
  private PartyEntity party;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "size_id", nullable = false)
  private ItemBlueprintDataEntity size;

  private LocalDate jobDate;
  private Double qtyPc;
  private Double qtyKg;
  private String finish;
  private Double elementCount;

  @Enumerated(EnumType.STRING)
  private ElementType elementType;

  @Enumerated(EnumType.STRING)
  private JobWorkStatus status;

  @Enumerated(EnumType.STRING)
  private JobWorkType jobWorkType;

  @OneToOne(
      mappedBy = "jobWork",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.LAZY)
  private JobWorkReturnEntity jobWorkReturn;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof JobWorkEntity e)) return false;
    return id != null && id.equals(e.id);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }
}
