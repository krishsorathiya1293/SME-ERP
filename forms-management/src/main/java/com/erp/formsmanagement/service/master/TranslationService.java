package com.erp.formsmanagement.service.master;

import com.erp.config.transliteration.TransliterationService;
import com.erp.formsmanagement.clientportal.ClientPortalConstants;
import com.erp.formsmanagement.domain.entity.master.PartyEntity;
import com.erp.formsmanagement.domain.entity.master.TranslationEntity;
import com.erp.formsmanagement.domain.entity.master.TranslationType;
import com.erp.formsmanagement.domain.repository.master.PartyRepository;
import com.erp.formsmanagement.domain.repository.master.TranslationRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Global dictionary of party-name / finish transliterations. The editor shows the full canonical
 * set — every finish ({@link ClientPortalConstants#FINISH_OPTIONS}) and every party in the party
 * master — each transliterated on first sight (Google) and thereafter editable. The Job Work print
 * renders from these saved values ({@link #resolveForPrint}) instead of calling Google live.
 */
@Slf4j
@Service
@Transactional
public class TranslationService {

  private final TranslationRepository repository;
  private final PartyRepository partyRepository;
  private final TransliterationService transliterationService;

  public TranslationService(
      TranslationRepository repository,
      PartyRepository partyRepository,
      TransliterationService transliterationService) {
    this.repository = repository;
    this.partyRepository = partyRepository;
    this.transliterationService = transliterationService;
  }

  /**
   * Full editor list for a type: every canonical source (all finishes, or all party-master names),
   * each backed by a saved row — transliterating + saving any that don't have one yet, so the list
   * always shows a Hindi/Gujarati default the user can edit.
   */
  public List<TranslationEntity> listFull(TranslationType type) {
    List<TranslationEntity> result = new ArrayList<>();
    for (String source : sourcesFor(type)) {
      result.add(ensure(type, source));
    }
    return result;
  }

  /** Canonical source strings for a type, de-duplicated and ordered. */
  private List<String> sourcesFor(TranslationType type) {
    if (type == TranslationType.FINISH) {
      return ClientPortalConstants.FINISH_OPTIONS;
    }
    return partyRepository.findAll().stream()
        .map(PartyEntity::getName)
        .filter(n -> n != null && !n.isBlank())
        .map(String::trim)
        .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new))
        .stream()
        .sorted(Comparator.naturalOrder())
        .toList();
  }

  /**
   * Ensures a dictionary row exists for {@code (type, sourceText)} and returns it. On first sight
   * the source is transliterated to Hindi + Gujarati and saved; an existing row is returned
   * untouched (the user's edits win). Returns {@code null} for blank input.
   */
  public TranslationEntity ensure(TranslationType type, String sourceText) {
    if (sourceText == null || sourceText.isBlank()) {
      return null;
    }
    String key = sourceText.trim();
    return repository
        .findByTypeAndSourceText(type, key)
        .orElseGet(
            () -> {
              TranslationEntity entity = new TranslationEntity();
              entity.setType(type);
              entity.setSourceText(key);
              entity.setHindi(transliterationService.convertToHindi(key));
              entity.setGujarati(transliterationService.convertToGujarati(key));
              return repository.save(entity);
            });
  }

  /** Creates or overwrites the saved translation for {@code (type, sourceText)} from user input. */
  public TranslationEntity upsert(
      TranslationType type, String sourceText, String hindi, String gujarati) {
    if (sourceText == null || sourceText.isBlank()) {
      throw new IllegalArgumentException("sourceText is required");
    }
    String key = sourceText.trim();
    TranslationEntity entity =
        repository.findByTypeAndSourceText(type, key).orElseGet(TranslationEntity::new);
    entity.setType(type);
    entity.setSourceText(key);
    entity.setHindi(hindi);
    entity.setGujarati(gujarati);
    return repository.save(entity);
  }

  /**
   * Hindi + Gujarati for the print. Prefers the saved (user-edited) row; if none exists yet, falls
   * back to a live transliteration without persisting (keeps the print path side-effect free).
   */
  @Transactional(readOnly = true)
  public LocalizedText resolveForPrint(TranslationType type, String sourceText) {
    if (sourceText == null || sourceText.isBlank()) {
      return new LocalizedText("", "");
    }
    String key = sourceText.trim();
    return repository
        .findByTypeAndSourceText(type, key)
        .map(e -> new LocalizedText(nullToEmpty(e.getHindi()), nullToEmpty(e.getGujarati())))
        .orElseGet(
            () ->
                new LocalizedText(
                    transliterationService.convertToHindi(key),
                    transliterationService.convertToGujarati(key)));
  }

  private static String nullToEmpty(String s) {
    return s == null ? "" : s;
  }

  /** Hindi + Gujarati rendition of a source string. */
  public record LocalizedText(String hindi, String gujarati) {}
}
