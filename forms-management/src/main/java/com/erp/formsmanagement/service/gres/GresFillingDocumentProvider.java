package com.erp.formsmanagement.service.gres;

import com.erp.api.exportmanagement.model.GresFillingFormType;
import com.erp.exception.EntityNotFoundException;
import com.erp.formsmanagement.domain.entity.gres.GresFillingEntity;
import com.erp.formsmanagement.domain.entity.gres.GresFillingReturnEntity;
import com.erp.formsmanagement.domain.repository.gres.GresFillingRepository;
import com.erp.formsmanagement.service.master.TranslationService;
import com.erp.formsmanagement.service.master.TranslationService.LocalizedText;
import com.erp.service.AbstractDocumentProvider;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GresFillingDocumentProvider extends AbstractDocumentProvider<GresFillingFormType> {

  private final GresFillingRepository gresFillingRepository;
  private final TranslationService translationService;

  @Override
  protected Class<GresFillingFormType> variantType() {
    return GresFillingFormType.class;
  }

  @Override
  public String formType() {
    return "gres-filling";
  }

  @Override
  public DocumentData resolve(Long id, String variant) {
    GresFillingFormType type = toVariant(variant);
    GresFillingEntity entity =
        gresFillingRepository
            .findById(id)
            .orElseThrow(
                () -> new EntityNotFoundException("GresFilling with ID " + id + " not found"));

    LocalDate latestReturnDate =
        entity.getReturns().stream()
            .map(GresFillingReturnEntity::getReturnDate)
            .filter(Objects::nonNull)
            .max(LocalDate::compareTo)
            .orElse(null);

    // Party Hindi comes from the saved, user-editable dictionary (keyed by party id), same source
    // as the Job Work print — no live transliteration. Blank until someone fills it in.
    LocalizedText partyLocalized =
        translationService.resolvePartyForPrint(entity.getParty().getId());
    Map<String, Object> variables = new HashMap<>();
    variables.put("gres", entity);
    variables.put("party", entity.getParty().getName() + " / " + partyLocalized.hindi());
    variables.put("latestReturnDate", latestReturnDate);
    return new DocumentData(resolveTemplateName(type), variables);
  }

  private String resolveTemplateName(GresFillingFormType type) {
    return switch (type) {
      case JAVAK -> "gres-filling";
    };
  }
}
