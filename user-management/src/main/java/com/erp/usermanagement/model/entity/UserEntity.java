package com.erp.usermanagement.model.entity;

import com.erp.audit.AuditInfo;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Entity representing a user in the system. Stores user credentials and group assignment for
 * authentication and authorization.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
@EqualsAndHashCode(callSuper = true)
public class UserEntity extends AuditInfo {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String username;

  private String userEmail;
  private String password;

  private String initialPassword;

  /** Links this user to a party (CLIENT users only). Null for ADMIN users. */
  @Column(unique = true)
  private Long partyId;

  @Enumerated(EnumType.STRING)
  private UserGroup userGroup;

  @Column(nullable = false)
  @Builder.Default
  private Boolean enabled = true;
}
