package com.erp.formsmanagement.service.order;

import com.erp.api.exportmanagement.model.JobWorkFormType;
import com.erp.config.transliteration.TransliterationService;
import com.erp.exception.EntityNotFoundException;
import com.erp.formsmanagement.domain.entity.order.JobWorkEntity;
import com.erp.formsmanagement.domain.entity.order.JobWorkReturnEntity;
import com.erp.formsmanagement.domain.repository.order.JobWorkRepository;
import com.erp.service.AbstractDocumentProvider;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JobWorkDocumentProvider extends AbstractDocumentProvider<JobWorkFormType> {

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
    JobWorkFormType type = toVariant(variant);
    JobWorkEntity entity =
        jobWorkRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("JobWork with ID " + id + " not found"));
    LocalDate latestReturnDate =
        entity.getJobWorkReturns().stream()
            .map(JobWorkReturnEntity::getJobReturnDate)
            .filter(Objects::nonNull)
            .max(LocalDate::compareTo)
            .orElse(null);

    Map<String, Object> variables = new HashMap<>();
    variables.put("job", entity);
    variables.put(
        "party",
        entity.getParty().getName()
            + " / "
            + transliterationService.convertToHindi(entity.getParty().getName()));
    variables.put("latestReturnDate", latestReturnDate);
    return new DocumentData(resolveTemplateName(type), variables);
  }

  private String resolveTemplateName(JobWorkFormType type) {
    return switch (type) {
      case AAVAK -> "jobwork-avak";
      case JAVAK -> "jobwork-javak";
    };
  }
}
