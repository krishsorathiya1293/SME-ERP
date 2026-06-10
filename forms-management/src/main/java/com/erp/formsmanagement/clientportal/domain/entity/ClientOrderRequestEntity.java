package com.erp.formsmanagement.clientportal.domain.entity;

import com.erp.audit.AuditInfo;
import com.erp.formsmanagement.domain.entity.master.PartyEntity;
import com.erp.formsmanagement.domain.entity.order.OrderEntity;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "client_order_requests")
@EntityListeners(AuditingEntityListener.class)
public class ClientOrderRequestEntity extends AuditInfo {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "party_id", nullable = false)
  private PartyEntity party;

  @Enumerated(EnumType.STRING)
  private ClientOrderRequestStatus status;

  private LocalDate orderDate;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "order_id")
  private OrderEntity order;

  @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<ClientOrderRequestItemEntity> items;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ClientOrderRequestEntity)) return false;
    return id != null && id.equals(((ClientOrderRequestEntity) o).id);
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
