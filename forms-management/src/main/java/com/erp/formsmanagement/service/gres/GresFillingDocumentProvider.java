package com.erp.formsmanagement.service.gres;

import com.erp.api.exportmanagement.model.GresFillingFormType;
import com.erp.config.transliteration.TransliterationService;
import com.erp.exception.EntityNotFoundException;
import com.erp.formsmanagement.domain.entity.gres.GresFillingEntity;
import com.erp.formsmanagement.domain.entity.gres.GresFillingReturnEntity;
import com.erp.formsmanagement.domain.repository.gres.GresFillingRepository;
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
  private final TransliterationService transliterationService;

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

    Map<String, Object> variables = new HashMap<>();
    variables.put("gres", entity);
    variables.put(
        "party",
        entity.getParty().getName()
            + " / "
            + transliterationService.convertToHindi(entity.getParty().getName()));
    variables.put("latestReturnDate", latestReturnDate);
    return new DocumentData(resolveTemplateName(type), variables);
  }

  private String resolveTemplateName(GresFillingFormType type) {
    return switch (type) {
      case JAVAK -> "gres-filling";
    };
  }
}
