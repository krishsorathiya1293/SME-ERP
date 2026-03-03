package com.erp.formsmanagement.service.order;

import com.erp.api.exportmanagement.model.JobWorkFormType;
import com.erp.exception.EntityNotFoundException;
import com.erp.formsmanagement.domain.entity.order.JobWorkEntity;
import com.erp.formsmanagement.domain.repository.order.JobWorkRepository;
import com.erp.service.AbstractDocumentProvider;
import com.erp.service.DocumentDataProvider.DocumentData;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JobWorkDocumentProvider extends AbstractDocumentProvider<JobWorkFormType> {

  private final JobWorkRepository jobWorkRepository;

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

    Map<String, Object> variables = new HashMap<>();
    variables.put(
        "jobWorkNo", String.format("%03d", entity.getOrderItem().getJobWorkNo().intValue()));
    if (entity.getJobDate() != null) {
      variables.put(
          "jobDate", entity.getJobDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
    } else {
      variables.put("jobDate", "");
    }
    variables.put("partyName", entity.getParty() != null ? entity.getParty().getName() : "");
    variables.put("finish", entity.getFinish() != null ? entity.getFinish() : "");
    variables.put(
        "stickerQty",
        entity.getOrderItem().getStickerQty() != null
            ? entity.getOrderItem().getStickerQty().toString()
            : "");
    variables.put("sizeLabel", entity.getSize() != null ? entity.getSize().getSizeInInch() : "");
    variables.put("qtyKg", entity.getQtyKg() != null ? entity.getQtyKg() + "Kg" : "");
    variables.put("qtyPc", entity.getQtyPc() != null ? entity.getQtyPc() + " Pc." : "");
    variables.put(
        "elementType", entity.getElementType() != null ? entity.getElementType().name() : "");
    Double eCount = entity.getElementCount();
    if (eCount != null) {
      if (eCount == eCount.intValue()) {
        variables.put("elementCount", String.valueOf(eCount.intValue()));
      } else {
        variables.put("elementCount", eCount.toString());
      }
    } else {
      variables.put("elementCount", "");
    }

    if (type == JobWorkFormType.JAVAK && entity.getJobWorkReturn() != null) {
      variables.put(
          "returnKg",
          entity.getJobWorkReturn().getReturnKg() != null
              ? entity.getJobWorkReturn().getReturnKg() + "Kg"
              : "");
      variables.put(
          "returnElementType",
          entity.getJobWorkReturn().getElementType() != null
              ? entity.getJobWorkReturn().getElementType().name()
              : "");
      Double retCount = entity.getJobWorkReturn().getReturnElementCount();
      if (retCount != null) {
        if (retCount == retCount.intValue()) {
          variables.put("returnElementCount", String.valueOf(retCount.intValue()));
        } else {
          variables.put("returnElementCount", retCount.toString());
        }
      } else {
        variables.put("returnElementCount", "");
      }
    } else {
      variables.put("returnKg", "");
      variables.put("returnElementType", "");
      variables.put("returnElementCount", "");
    }

    String templateName = type == JobWorkFormType.AAVAK ? "jobwork-avak" : "jobwork-javak";
    return new DocumentData(templateName, variables);
  }
}
