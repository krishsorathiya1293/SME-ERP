package com.erp.formsmanagement.controller.master;

import com.erp.api.mastermanagement.TranslationMasterManagementApi;
import com.erp.api.mastermanagement.model.NewTranslation;
import com.erp.api.mastermanagement.model.Translation;
import com.erp.formsmanagement.domain.entity.master.TranslationType;
import com.erp.formsmanagement.mapper.master.TranslationMapper;
import com.erp.formsmanagement.service.master.TranslationService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TranslationController implements TranslationMasterManagementApi {

  private final TranslationService service;
  private final TranslationMapper mapper;

  public TranslationController(TranslationService service, TranslationMapper mapper) {
    this.service = service;
    this.mapper = mapper;
  }

  @Override
  public ResponseEntity<List<Translation>> getTranslations(
      com.erp.api.mastermanagement.model.TranslationType type) {
    List<Translation> result =
        service.listFull(TranslationType.valueOf(type.name())).stream()
            .map(mapper::toDomain)
            .toList();
    return ResponseEntity.ok(result);
  }

  @Override
  public ResponseEntity<Translation> upsertTranslation(NewTranslation newTranslation) {
    var saved =
        service.upsert(
            TranslationType.valueOf(newTranslation.getType().name()),
            newTranslation.getPartyId(),
            newTranslation.getSourceText(),
            newTranslation.getHindi(),
            newTranslation.getGujarati());
    return ResponseEntity.ok(mapper.toDomain(saved));
  }
}
