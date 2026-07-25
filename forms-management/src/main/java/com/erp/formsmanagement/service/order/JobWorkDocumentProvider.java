package com.erp.formsmanagement.service.order;

import com.erp.api.exportmanagement.model.JobWorkFormType;
import com.erp.config.transliteration.TransliterationService;
import com.erp.exception.EntityNotFoundException;
import com.erp.formsmanagement.domain.entity.order.JobWorkEntity;
import com.erp.formsmanagement.domain.repository.order.JobWorkRepository;
import com.erp.service.AbstractDocumentProvider;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JobWorkDocumentProvider extends AbstractDocumentProvider<JobWorkFormType> {

  private static final String DEFAULT_PAPER_SIZE = "A6";

  private final JobWorkRepository jobWorkRepository;
  private final TransliterationService transliterationService;

  @Override
  protected Class<JobWorkFormType> variantType() {
    return JobWorkFormType.class;
  }

  @Override
  public String formType() {
    return "job-work";
  }

  @Override
  public DocumentData resolve(Long id, String variant) {
    String[] parts = variant != null ? variant.split("_", 2) : new String[] {""};
    JobWorkFormType type = toVariant(parts[0]);
    String paperSize = parts.length > 1 && !parts[1].isBlank() ? parts[1].toUpperCase() : DEFAULT_PAPER_SIZE;

    JobWorkEntity entity =
        jobWorkRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("JobWork with ID " + id + " not found"));

    String partyName = entity.getParty().getName();

    Map<String, Object> variables = new HashMap<>();
    variables.put("job", entity);
    variables.put("partyPlain", partyName);
    variables.put("partyTri", transliterateTri(partyName));
    variables.put("finishTri", transliterateTri(entity.getFinish()));
    return new DocumentData(resolveTemplateName(type, paperSize), variables);
  }

  private String transliterateTri(String text) {
    if (text == null || text.isBlank()) {
      return "";
    }
    return text
        + " / " + transliterationService.convertToHindi(text)
        + " / " + transliterationService.convertToGujarati(text);
  }

  private String resolveTemplateName(JobWorkFormType type, String paperSize) {
    String size = "A8".equals(paperSize) ? "a8" : "a6";
    return switch (type) {
      case AAVAK -> "jobwork-avak-" + size;
      case JAVAK -> "jobwork-javak-" + size;
    };
  }
}
