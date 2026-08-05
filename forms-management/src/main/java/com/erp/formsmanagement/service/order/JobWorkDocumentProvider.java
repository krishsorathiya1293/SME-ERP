package com.erp.formsmanagement.service.order;

import com.erp.api.exportmanagement.model.JobWorkFormType;
import com.erp.exception.EntityNotFoundException;
import com.erp.formsmanagement.domain.entity.order.JobWorkEntity;
import com.erp.formsmanagement.domain.repository.order.JobWorkRepository;
import com.erp.formsmanagement.service.master.TranslationService;
import com.erp.formsmanagement.service.master.TranslationService.LocalizedText;
import com.erp.formsmanagement.util.JobWorkNumber;
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
  private final TranslationService translationService;

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
    variables.put("jwLabel", JobWorkNumber.label(partyName, entity.getJobWorkNo()));
    variables.put("partyPlain", partyName);
    variables.put(
        "partyTri",
        triLine(partyName, translationService.resolvePartyForPrint(entity.getParty().getId())));
    variables.put(
        "finishTri",
        triLine(entity.getFinish(), translationService.resolveFinishForPrint(entity.getFinish())));
    return new DocumentData(resolveTemplateName(type, paperSize), variables);
  }

  /**
   * Builds the {@code English / हिंदी / ગુજરાતી} string shown on the print from the saved
   * (user-editable) dictionary entry. Missing scripts render blank — nothing is fetched live.
   */
  private String triLine(String text, LocalizedText localized) {
    if (text == null || text.isBlank()) {
      return "";
    }
    return text + " / " + localized.hindi() + " / " + localized.gujarati();
  }

  private String resolveTemplateName(JobWorkFormType type, String paperSize) {
    String size = "A8".equals(paperSize) ? "a8" : "a6";
    return switch (type) {
      case AAVAK -> "jobwork-avak-" + size;
      case JAVAK -> "jobwork-javak-" + size;
    };
  }
}
