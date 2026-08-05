package com.erp.formsmanagement.service.master;

import com.erp.config.transliteration.TransliterationService;
import com.erp.event.FormChangedEvent;
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
import org.springframework.context.ApplicationEventPublisher;
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
  private final ApplicationEventPublisher eventPublisher;

  public TranslationService(
      TranslationRepository repository,
      PartyRepository partyRepository,
      TransliterationService transliterationService,
      ApplicationEventPublisher eventPublisher) {
    this.repository = repository;
    this.partyRepository = partyRepository;
    this.transliterationService = transliterationService;
    this.eventPublisher = eventPublisher;
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
    // Opening the editor is the user's "manage translations" action — always drop cached job-work
    // prints so a reprint afterwards re-renders from the current dictionary, even when no row here
    // actually changed (an earlier print may hold a stale render from before the value settled).
    evictJobWorkPrintCache();
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
   * Ensures a dictionary row exists for {@code (type, sourceText)} and returns it with both Hindi
   * and Gujarati populated. A missing row is created; an existing row keeps whatever the user
   * typed but has any <em>blank</em> Hindi/Gujarati field back-filled via Google transliteration —
   * so the editor and print never show an empty translation. Returns {@code null} for blank input.
   */
  public TranslationEntity ensure(TranslationType type, String sourceText) {
    if (sourceText == null || sourceText.isBlank()) {
      return null;
    }
    String key = sourceText.trim();
    TranslationEntity entity =
        repository
            .findByTypeAndSourceText(type, key)
            .orElseGet(
                () -> {
                  TranslationEntity fresh = new TranslationEntity();
                  fresh.setType(type);
                  fresh.setSourceText(key);
                  return fresh;
                });

    String hindi = filledOrTransliterated(entity.getHindi(), key, true);
    String gujarati = filledOrTransliterated(entity.getGujarati(), key, false);

    boolean changed =
        entity.getId() == null
            || !hindi.equals(nullToEmpty(entity.getHindi()))
            || !gujarati.equals(nullToEmpty(entity.getGujarati()));
    if (changed) {
      entity.setHindi(hindi);
      entity.setGujarati(gujarati);
      TranslationEntity saved = repository.save(entity);
      // A back-fill changes what the print renders, so drop stale cached PNG/PDF too — this fires
      // when the editor is merely opened (listFull auto-fills blanks), not only on explicit Save.
      evictJobWorkPrintCache();
      return saved;
    }
    return entity;
  }

  /** Drops every cached Job Work PNG/PDF so the next print re-renders with current translations. */
  private void evictJobWorkPrintCache() {
    eventPublisher.publishEvent(new FormChangedEvent("job-work", null, "TRANSLATION"));
  }

  /**
   * Keeps a non-blank existing value as-is; otherwise transliterates {@code key} (Hindi or
   * Gujarati), never returning blank — falling back to the source text if Google yields nothing.
   */
  private String filledOrTransliterated(String current, String key, boolean hindi) {
    if (current != null && !current.isBlank()) {
      return current;
    }
    String converted =
        hindi
            ? transliterationService.convertToHindi(key)
            : transliterationService.convertToGujarati(key);
    return (converted != null && !converted.isBlank()) ? converted : key;
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
    TranslationEntity saved = repository.save(entity);
    // The Job Work print renders party/finish from this dictionary; drop any cached PNG/PDF so the
    // edited Hindi/Gujarati shows immediately instead of a stale (Google-seeded) render.
    evictJobWorkPrintCache();
    return saved;
  }

  /**
   * Hindi + Gujarati for the print. Always resolves through the saved dictionary: an existing row is
   * returned exactly as saved (the user's edits win); the very first time a term is seen it is
   * seeded from Google <em>and persisted</em>, so every subsequent print reads that saved value
   * rather than calling Google again (which can return a slightly different result each time).
   */
  @Transactional
  public LocalizedText resolveForPrint(TranslationType type, String sourceText) {
    if (sourceText == null || sourceText.isBlank()) {
      return new LocalizedText("", "");
    }
    TranslationEntity e = ensure(type, sourceText);
    if (e == null) {
      return new LocalizedText("", "");
    }
    log.info(
        "TRANSLATION-DEBUG print type={} key=[{}] -> hindi=[{}] gujarati=[{}]",
        type, sourceText.trim(), e.getHindi(), e.getGujarati());
    return new LocalizedText(nullToEmpty(e.getHindi()).strip(), nullToEmpty(e.getGujarati()).strip());
  }

  private static String nullToEmpty(String s) {
    return s == null ? "" : s;
  }

  /** Hindi + Gujarati rendition of a source string. */
  public record LocalizedText(String hindi, String gujarati) {}
}
