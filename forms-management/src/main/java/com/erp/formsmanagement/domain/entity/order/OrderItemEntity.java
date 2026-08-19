package com.erp.formsmanagement.domain.entity.order;

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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.Formula;
import java.util.Objects;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "order_items")
@EntityListeners(AuditingEntityListener.class)
public class OrderItemEntity extends AuditInfo {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "order_id", nullable = false)
  private OrderEntity order;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "size_id", nullable = false, unique = true)
  private ItemBlueprintDataEntity itemSize;

  private String plating;
  private Double qtyPc;
  private Double qtyKg;
  private Double pcPerBox;
  private Double boxPerCartoon;
  private Double pcPerCartoon;
  private Double stickerQty;

  private Double pendingPc;

  @Formula("(SELECT COALESCE(SUM(d.dispatch_pcs), 0) FROM sme_erp.order_dispatch d WHERE d.order_item_id = id)")
  private Double totalDispatchedPc;

  @Formula("(SELECT MAX(d.dispatch_date) FROM sme_erp.order_dispatch d WHERE d.order_item_id = id)")
  private LocalDate lastDispatchDate;

  /**
   * Every job work this line has been sent out on. A line can go to the plater in batches, so this
   * is a list — {@code jobWorks} summed gives the Kg sent, and what is left of the order quantity
   * is still waiting to be sent.
   */
  @OneToMany(mappedBy = "orderItem", fetch = FetchType.LAZY)
  @OrderBy("createdAt ASC")
  private List<JobWorkEntity> jobWorks = new ArrayList<>();

  /**
   * This line's share of every *merged* chitthi it appears on.
   *
   * <p>{@link #jobWorks} only sees job works whose primary order item is this one, so a line
   * merged into someone else's chitthi would otherwise look untouched. These rows carry both the
   * link and how much of that chitthi's weight belongs here.
   */
  @OneToMany(mappedBy = "orderItem", fetch = FetchType.LAZY)
  private List<JobWorkOrderItemEntity> jobWorkAllocations = new ArrayList<>();

  private Boolean jobActionDone;

  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  @Enumerated(EnumType.STRING)
  private JobPlatingType platingType;

  private Double jobWorkNo;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof OrderItemEntity e)) return false;
    return id != null && id.equals(e.id);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }
}
