package com.erp.formsmanagement.domain.entity.master;

import com.erp.audit.AuditInfo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * A saved translation for a party or finish, shown on the Job Work / Gres print.
 *
 * <p>PARTY rows key on {@link #partyId} (so a rename never orphans the saved value); FINISH rows
 * have a {@code null} partyId and key on {@link #sourceText} (a fixed option list). {@code
 * sourceText} is kept for both as the display label. Uniqueness is enforced by partial indexes in
 * the migration (one row per partyId; one FINISH row per source_text), not a table constraint.
 * {@code hindi}/{@code gujarati} are the editable target scripts.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "translation")
@EntityListeners(AuditingEntityListener.class)
public class TranslationEntity extends AuditInfo {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TranslationType type;

  /** Party this row translates; {@code null} for FINISH rows. Lookup key for PARTY rows. */
  @Column(name = "party_id")
  private Long partyId;

  @Column(name = "source_text", nullable = false)
  private String sourceText;

  private String hindi;

  private String gujarati;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof TranslationEntity e)) return false;
    return id != null && id.equals(e.id);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }
}
