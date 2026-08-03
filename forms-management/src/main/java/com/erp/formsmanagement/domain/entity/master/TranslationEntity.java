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
import jakarta.persistence.UniqueConstraint;
import java.util.Objects;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * A saved transliteration for a party name or finish. The {@code (type, sourceText)} pair is the
 * unique lookup key; {@code hindi}/{@code gujarati} are the editable target scripts shown on the
 * Job Work print.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(
    name = "translation",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_translation_type_source",
            columnNames = {"type", "source_text"}))
@EntityListeners(AuditingEntityListener.class)
public class TranslationEntity extends AuditInfo {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TranslationType type;

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
