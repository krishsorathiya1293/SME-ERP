package com.erp.formsmanagement.domain.entity.pricing;

import com.erp.audit.AuditInfo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Objects;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * A dynamic finish-price formula: {@code finish = S.S. * multiplier + offsetValue}.
 *
 * <p>Scope is defined by {@code clientId} and {@code itemId}, either of which may be null. NULL
 * means "applies to all". Most specific scope wins when resolving:
 * client+item &rarr; client &rarr; item &rarr; global (both null).
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "pricing_rule")
@EntityListeners(AuditingEntityListener.class)
public class PricingRuleEntity extends AuditInfo {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "client_id")
  private Long clientId;

  @Column(name = "item_id")
  private Long itemId;

  @Column(name = "finish_key", nullable = false)
  private String finishKey;

  @Column(nullable = false)
  private Double multiplier;

  @Column(name = "offset_value", nullable = false)
  private Double offsetValue;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PricingRuleEntity e)) return false;
    return id != null && id.equals(e.id);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }
}
