package com.erp.formsmanagement.domain.entity.order;

import com.erp.audit.AuditInfo;
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
import jakarta.persistence.Table;
import java.util.Objects;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * One dispatch or chitthi that moved onto a merged line, and the line it came from.
 *
 * <p>Merging a line that is already at the plater or part-dispatched moves those records onto the
 * merged line, so from then on it is simply one line. This row is the memory of the move: un-merge
 * reads it to put every record back, and the client portal reads it to keep crediting work done
 * before the merge to the order it was done for.
 *
 * <p>{@code recordId} is not a foreign key because it points at one of three tables, chosen by
 * {@link #kind}. See V66 for what the quantities mean for each kind.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "order_item_merge_moves")
@EntityListeners(AuditingEntityListener.class)
public class OrderItemMergeMoveEntity extends AuditInfo {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "merged_item_id", nullable = false)
  private OrderItemEntity mergedItem;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "source_item_id", nullable = false)
  private OrderItemEntity sourceItem;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private MergeMoveKind kind;

  @Column(nullable = false)
  private Long recordId;

  private Double qtyKg;
  private Double qtyPc;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof OrderItemMergeMoveEntity e)) return false;
    return id != null && id.equals(e.id);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }
}
