package com.erp.formsmanagement.domain.entity.order;

import com.erp.audit.AuditInfo;
import com.erp.formsmanagement.domain.entity.inventory.ItemBlueprintDataEntity;
import jakarta.persistence.Column;
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

  /**
   * The merged line now carrying this line's quantity, or null when this line is not merged away.
   *
   * <p>This line's own {@code qtyPc} / {@code qtyKg} are deliberately left alone — they are what
   * the party ordered, and the share of the merged line that belongs here is worked out from them.
   * Nothing is summed in place, so un-merging is a matter of dropping the merged order.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "merged_into_item_id")
  private OrderItemEntity mergedIntoItem;

  /** The lines this one sums. More than one means the quantities were genuinely added together. */
  @OneToMany(mappedBy = "mergedIntoItem", fetch = FetchType.LAZY)
  @OrderBy("id ASC")
  private List<OrderItemEntity> mergedSourceItems = new ArrayList<>();

  private Boolean jobActionDone;

  /**
   * The manual "done with it" tick, held on the LINE rather than the order.
   *
   * <p>One order routinely carries five items that finish at five different times, so a tick that
   * could only say "all of it" or "none of it" was useless: ticking the two that were settled took
   * the other three off the sheet with them. The parent order keeps its own flag, meaning every
   * line of it, so nothing that already reads the order-level tick breaks.
   *
   * <p>Deliberately separate from the derived stage, for the same reason the order's is: a
   * supervisor tidying their screen must never be able to rewrite what the works actually did,
   * which is what the client portal reads. This hides a row and says nothing else.
   */
  @Column(nullable = false)
  private boolean completed = false;

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
