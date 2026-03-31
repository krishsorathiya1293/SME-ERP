package com.erp.exportmanagement.service;

import com.erp.formsmanagement.domain.entity.gres.GresFillingEntity;
import com.erp.formsmanagement.domain.entity.order.JobWorkEntity;
import com.erp.formsmanagement.domain.entity.purchase.PurchaseOrderEntity;
import com.erp.formsmanagement.domain.entity.sales.SalesOrderEntity;
import com.erp.formsmanagement.domain.entity.master.PartyEntity;
import com.erp.formsmanagement.domain.repository.gres.GresFillingRepository;
import com.erp.formsmanagement.domain.repository.order.JobWorkRepository;
import com.erp.formsmanagement.domain.repository.purchase.PurchaseOrderRepository;
import com.erp.formsmanagement.domain.repository.sales.SalesOrderRepository;
import com.erp.formsmanagement.domain.repository.master.PartyRepository;
import com.erp.exportmanagement.DocumentRenderService;
import com.erp.service.DocumentFormat;
import com.erp.exception.EntityNotFoundException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportExportService {

  private final PartyRepository partyRepository;
  private final GresFillingRepository gresFillingRepository;
  private final JobWorkRepository jobWorkRepository;
  private final PurchaseOrderRepository purchaseOrderRepository;
  private final SalesOrderRepository salesOrderRepository;
  private final DocumentRenderService documentRenderService;

  @Transactional(readOnly = true)
  public byte[] generateGresFillingReportPdf(Long partyId, LocalDate startDate, LocalDate endDate) {
    PartyEntity party = getParty(partyId);
    List<GresFillingEntity> records = gresFillingRepository.findByPartyIdAndChitthiDateBetweenOrderByChitthiDateAsc(partyId, startDate, endDate);

    double totalNetWeight = 0;
    double totalGhati = 0;
    double totalAmount = 0;

    for (GresFillingEntity g : records) {
      if (g.getItems() != null) {
        for (var item : g.getItems()) {
          totalNetWeight += (item.getNetWeight() != null ? item.getNetWeight() : 0);
          totalAmount += (item.getTotalAmount() != null ? item.getTotalAmount() : 0);
        }
      }
      if (g.getReturns() != null) {
        for (var ret : g.getReturns()) {
          totalGhati += (ret.getGhati() != null ? ret.getGhati() : 0);
        }
      }
    }

    Map<String, Object> variables = new HashMap<>();
    variables.put("partyName", party.getName());
    variables.put("startDate", startDate);
    variables.put("endDate", endDate);
    variables.put("records", records);
    variables.put("totalNetWeight", totalNetWeight);
    variables.put("totalGhati", totalGhati);
    variables.put("totalAmount", totalAmount);

    return documentRenderService.renderDocument("gres-report", variables, DocumentFormat.PDF).getByteArray();
  }

  @Transactional(readOnly = true)
  public byte[] generateJobWorkReportPdf(Long partyId, LocalDate startDate, LocalDate endDate) {
    PartyEntity party = getParty(partyId);
    List<JobWorkEntity> records = jobWorkRepository.findByPartyIdAndJobDateBetweenOrderByJobDateAsc(partyId, startDate, endDate);

    double totalReturnKg = 0;
    double totalGhati = 0;

    for (JobWorkEntity jw : records) {
      if (jw.getJobWorkReturns() != null) {
        for (var ret : jw.getJobWorkReturns()) {
          totalReturnKg += (ret.getReturnKg() != null ? ret.getReturnKg() : 0);
          totalGhati += (ret.getGhati() != null ? ret.getGhati() : 0);
        }
      }
    }

    Map<String, Object> variables = new HashMap<>();
    variables.put("partyName", party.getName());
    variables.put("startDate", startDate);
    variables.put("endDate", endDate);
    variables.put("records", records);
    variables.put("totalReturnKg", totalReturnKg);
    variables.put("totalGhati", totalGhati);

    return documentRenderService.renderDocument("jobwork-report", variables, DocumentFormat.PDF).getByteArray();
  }

  @Transactional(readOnly = true)
  public byte[] generatePurchaseReportPdf(Long partyId, LocalDate startDate, LocalDate endDate) {
    PartyEntity party = getParty(partyId);
    List<PurchaseOrderEntity> records = purchaseOrderRepository.findByPartyIdAndOrderDateBetweenOrderByOrderDateAsc(partyId, startDate, endDate);

    double totalPurchasedAmount = 0;
    double totalJavakAmount = 0;

    for (PurchaseOrderEntity po : records) {
      if (po.getItems() != null) {
        for (var item : po.getItems()) {
          totalPurchasedAmount += (item.getTotalPrice() != null ? item.getTotalPrice() : 0);
          totalJavakAmount += (item.getJavakTotalRs() != null ? item.getJavakTotalRs() : 0);
        }
      }
    }

    Map<String, Object> variables = new HashMap<>();
    variables.put("partyName", party.getName());
    variables.put("startDate", startDate);
    variables.put("endDate", endDate);
    variables.put("records", records);
    variables.put("totalPurchasedAmount", totalPurchasedAmount);
    variables.put("totalJavakAmount", totalJavakAmount);

    return documentRenderService.renderDocument("purchase-report", variables, DocumentFormat.PDF).getByteArray();
  }

  @Transactional(readOnly = true)
  public byte[] generateSalesReportPdf(Long partyId, LocalDate startDate, LocalDate endDate) {
    PartyEntity party = getParty(partyId);
    List<SalesOrderEntity> records = salesOrderRepository.findByPartyIdAndOrderDateBetweenOrderByOrderDateAsc(partyId, startDate, endDate);

    double totalSalesAmount = 0;
    double totalJavakAmount = 0;

    for (SalesOrderEntity so : records) {
      if (so.getItems() != null) {
        for (var item : so.getItems()) {
          totalSalesAmount += (item.getTotalPrice() != null ? item.getTotalPrice() : 0);
          totalJavakAmount += (item.getJavakTotalRs() != null ? item.getJavakTotalRs() : 0);
        }
      }
    }

    Map<String, Object> variables = new HashMap<>();
    variables.put("partyName", party.getName());
    variables.put("startDate", startDate);
    variables.put("endDate", endDate);
    variables.put("records", records);
    variables.put("totalSalesAmount", totalSalesAmount);
    variables.put("totalJavakAmount", totalJavakAmount);

    return documentRenderService.renderDocument("sales-report", variables, DocumentFormat.PDF).getByteArray();
  }

  private PartyEntity getParty(Long partyId) {
    return partyRepository.findById(partyId)
        .orElseThrow(() -> new EntityNotFoundException("Party not found with id: " + partyId));
  }
}
