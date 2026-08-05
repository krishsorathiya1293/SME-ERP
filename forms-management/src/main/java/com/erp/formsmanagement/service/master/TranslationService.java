package com.erp.formsmanagement.service.master;

import com.erp.event.FormChangedEvent;
import com.erp.formsmanagement.clientportal.ClientPortalConstants;
import com.erp.formsmanagement.domain.entity.master.PartyEntity;
import com.erp.formsmanagement.domain.entity.master.TranslationEntity;
import com.erp.formsmanagement.domain.entity.master.TranslationType;
import com.erp.formsmanagement.domain.repository.master.PartyRepository;
import com.erp.formsmanagement.domain.repository.master.TranslationRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Global dictionary of party / finish translations shown on the Job Work and Gres prints.
 *
 * <p>PARTY rows key on the party's id, so renaming a party (or a stray space / casing difference)
 * can never orphan the saved Hindi/Gujarati and leave the print blank. FINISH rows key on their
 * text (a fixed {@link ClientPortalConstants#FINISH_OPTIONS} list). Values are user-entered and
 * edited in the Translations editor — nothing is fetched from Google (or anywhere else); a term
 * with no saved value simply prints blank until someone fills it in.
 */
@Slf4j
@Service
@Transactional
public class TranslationService {

  private final TranslationRepository repository;
  private final PartyRepository partyRepository;
  private final ApplicationEventPublisher eventPublisher;

  public TranslationService(
      TranslationRepository repository,
      PartyRepository partyRepository,
      ApplicationEventPublisher eventPublisher) {
    this.repository = repository;
    this.partyRepository = partyRepository;
    this.eventPublisher = eventPublisher;
  }

  /**
   * Full editor list for a type: every canonical source (all finishes, or all party-master
   * entries), each backed by a saved row — created blank if missing so the list always has an
   * editable Hindi/Gujarati pair. No value is ever auto-filled from an external service.
   */
  public List<TranslationEntity> listFull(TranslationType type) {
    List<TranslationEntity> result = new ArrayList<>();
    if (type == TranslationType.FINISH) {
      for (String finish : ClientPortalConstants.FINISH_OPTIONS) {
        result.add(ensureFinish(finish));
      }
    } else {
      for (PartyEntity party : partiesForEditor()) {
        result.add(ensureParty(party));
      }
    }
    // Opening the editor is the user's "manage translations" action — always drop cached job-work
    // prints so a reprint afterwards re-renders from the current dictionary, even when no row here
    // actually changed (an earlier print may hold a stale render from before the value settled).
    evictPrintCache();
    return result;
  }

  /** Party-master entries, de-duplicated by id and ordered by name for the editor. */
  private List<PartyEntity> partiesForEditor() {
    return partyRepository.findAll().stream()
        .filter(p -> p.getName() != null && !p.getName().isBlank())
        .sorted(Comparator.comparing(p -> p.getName().trim(), String.CASE_INSENSITIVE_ORDER))
        .toList();
  }

  /**
   * Ensures a dictionary row exists for the given party, keyed by its id, and returns it. A row
   * created before party-id keying (matched only by name) is adopted onto the id here so its saved
   * value carries over. The display {@code sourceText} is refreshed to the party's current name.
   */
  public TranslationEntity ensureParty(PartyEntity party) {
    if (party == null || party.getId() == null) {
      return null;
    }
    String name = party.getName() == null ? "" : party.getName().trim();
    TranslationEntity entity =
        repository
            .findByPartyId(party.getId())
            // Adopt a legacy name-keyed row (no party_id yet) so its value isn't lost.
            .or(() -> repository.findByTypeAndSourceText(TranslationType.PARTY, name))
            .orElseGet(
                () -> {
                  TranslationEntity fresh = new TranslationEntity();
                  fresh.setType(TranslationType.PARTY);
                  return fresh;
                });

    boolean changed =
        entity.getId() == null
            || !party.getId().equals(entity.getPartyId())
            || !name.equals(nullToEmpty(entity.getSourceText()));
    entity.setPartyId(party.getId());
    entity.setSourceText(name);
    return changed ? repository.save(entity) : entity;
  }

  /** Ensures a dictionary row exists for a finish (keyed by its text) and returns it. */
  public TranslationEntity ensureFinish(String finish) {
    if (finish == null || finish.isBlank()) {
      return null;
    }
    String key = finish.trim();
    return repository
        .findByTypeAndSourceText(TranslationType.FINISH, key)
        .orElseGet(
            () -> {
              TranslationEntity fresh = new TranslationEntity();
              fresh.setType(TranslationType.FINISH);
              fresh.setSourceText(key);
              return repository.save(fresh);
            });
  }

  /** Drops every cached Job Work PNG/PDF so the next print re-renders with current translations. */
  private void evictPrintCache() {
    eventPublisher.publishEvent(new FormChangedEvent("job-work", null, "TRANSLATION"));
  }

  /**
   * Creates or overwrites the saved translation for a PARTY (by {@code partyId}) or FINISH (by
   * {@code sourceText}) from user input.
   */
  public TranslationEntity upsert(
      TranslationType type, Long partyId, String sourceText, String hindi, String gujarati) {
    TranslationEntity entity;
    if (type == TranslationType.PARTY) {
      if (partyId == null) {
        throw new IllegalArgumentException("partyId is required for a PARTY translation");
      }
      entity =
          repository
              .findByPartyId(partyId)
              .orElseGet(
                  () -> {
                    TranslationEntity fresh = new TranslationEntity();
                    fresh.setType(TranslationType.PARTY);
                    fresh.setPartyId(partyId);
                    return fresh;
                  });
      // Keep the display label in step with the party name the user just edited against.
      if (sourceText != null && !sourceText.isBlank()) {
        entity.setSourceText(sourceText.trim());
      }
    } else {
      if (sourceText == null || sourceText.isBlank()) {
        throw new IllegalArgumentException("sourceText is required for a FINISH translation");
      }
      String key = sourceText.trim();
      entity =
          repository
              .findByTypeAndSourceText(TranslationType.FINISH, key)
              .orElseGet(
                  () -> {
                    TranslationEntity fresh = new TranslationEntity();
                    fresh.setType(TranslationType.FINISH);
                    fresh.setSourceText(key);
                    return fresh;
                  });
    }
    entity.setHindi(hindi);
    entity.setGujarati(gujarati);
    TranslationEntity saved = repository.save(entity);
    // The print renders from this dictionary; drop any cached PNG/PDF so the edit shows immediately.
    evictPrintCache();
    return saved;
  }

  /**
   * Hindi + Gujarati for a party's print line, read only from the saved dictionary (by party id).
   * An unseen party prints empty — nothing is fetched live.
   */
  @Transactional(readOnly = true)
  public LocalizedText resolvePartyForPrint(Long partyId) {
    if (partyId == null) {
      return new LocalizedText("", "");
    }
    return repository.findByPartyId(partyId).map(TranslationService::toLocalized).orElse(LocalizedText.EMPTY);
  }

  /** Hindi + Gujarati for a finish's print line, read only from the saved dictionary (by text). */
  @Transactional(readOnly = true)
  public LocalizedText resolveFinishForPrint(String finish) {
    if (finish == null || finish.isBlank()) {
      return new LocalizedText("", "");
    }
    return repository
        .findByTypeAndSourceText(TranslationType.FINISH, finish.trim())
        .map(TranslationService::toLocalized)
        .orElse(LocalizedText.EMPTY);
  }

  private static LocalizedText toLocalized(TranslationEntity e) {
    return new LocalizedText(nullToEmpty(e.getHindi()).strip(), nullToEmpty(e.getGujarati()).strip());
  }

  private static String nullToEmpty(String s) {
    return s == null ? "" : s;
  }

  /** Hindi + Gujarati rendition of a source string. */
  public record LocalizedText(String hindi, String gujarati) {
    static final LocalizedText EMPTY = new LocalizedText("", "");
  }
}
