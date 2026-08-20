package com.erp.formsmanagement.domain.entity.order;

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
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "orders")
@EntityListeners(AuditingEntityListener.class)
public class OrderEntity extends AuditInfo {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "party_id", nullable = false)
  private PartyEntity party;

  private LocalDate orderDate;

  /**
   * The scrap agreed with the party for this order, in rupees.
   *
   * <p>One figure for the whole order rather than per line: it is settled once, when the order is
   * taken, and covers every line on it. Null means "not agreed yet" — an order can be placed
   * before the number is, and a 0 would read as "agreed at nothing".
   */
  private Double scrap;

  @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<OrderItemEntity> orderItems;

  /**
   * The merged order this one was folded into, or null for an ordinary order.
   *
   * <p>Two orders from the same party for the same item, size and finish are one job on the floor.
   * Merging creates a new order carrying the combined lines and points the sources here — the
   * sources keep every row exactly as the party placed it, so the client portal still reports each
   * request against what that request ordered. Listings hide a source; the merged order stands in
   * for it.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "merged_into_id")
  private OrderEntity mergedInto;

  /** The orders folded into this one. Empty unless this *is* a merged order. */
  @OneToMany(mappedBy = "mergedInto", fetch = FetchType.LAZY)
  @OrderBy("orderDate ASC, id ASC")
  private List<OrderEntity> mergedSources = new ArrayList<>();

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof OrderEntity e)) return false;
    return id != null && id.equals(e.id);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }
}
