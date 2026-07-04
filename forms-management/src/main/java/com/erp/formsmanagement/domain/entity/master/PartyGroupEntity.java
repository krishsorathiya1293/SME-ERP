package com.erp.formsmanagement.domain.entity.master;

import com.erp.audit.AuditInfo;
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
 * A customer group that owns multiple parties/companies. One client login is provisioned per group;
 * the client selects which member party to act as when shopping.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "party_group")
@EntityListeners(AuditingEntityListener.class)
public class PartyGroupEntity extends AuditInfo {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String name;
  private String email;
  private String contactNo;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PartyGroupEntity e)) return false;
    return id != null && id.equals(e.id);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }
}
