package com.erp.formsmanagement.domain.entity.order;

import com.erp.audit.AuditInfo;
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

/**
 * One order line's share of a merged job work.
 *
 * <p>When two orders for the same party/item/size/finish go to the plater as a single 300 Kg
 * batch, the chitthi is one job work — but the 300 Kg still belongs to the two lines that ordered
 * it, and the client portal reports per line. This row is that split.
 *
 * <p>Unmerged job works have no rows here at all; they are described entirely by
 * {@code job_works.order_item_id}, exactly as before.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "job_work_order_items")
@EntityListeners(AuditingEntityListener.class)
public class JobWorkOrderItemEntity extends AuditInfo {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "job_work_id", nullable = false)
  private JobWorkEntity jobWork;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "order_item_id", nullable = false)
  private OrderItemEntity orderItem;

  private Double qtyKg;
  private Double qtyPc;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof JobWorkOrderItemEntity e)) return false;
    return id != null && id.equals(e.id);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }
}
