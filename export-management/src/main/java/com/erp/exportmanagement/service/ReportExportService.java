package com.erp.exportmanagement.service;

import com.erp.formsmanagement.domain.entity.gres.GresFillingEntity;
import com.erp.formsmanagement.domain.entity.order.JobWorkEntity;
import com.erp.formsmanagement.domain.entity.order.JobWorkType;
import com.erp.formsmanagement.domain.entity.purchase.PurchaseOrderEntity;
import com.erp.formsmanagement.domain.entity.sales.SalesOrderEntity;
import com.erp.formsmanagement.domain.entity.master.PartyEntity;
import com.erp.formsmanagement.domain.repository.gres.GresFillingRepository;
import com.erp.formsmanagement.domain.repository.order.JobWorkRepository;
import com.erp.formsmanagement.util.JobWorkNumber;
import com.erp.formsmanagement.domain.repository.purchase.PurchaseOrderRepository;
import com.erp.formsmanagement.domain.repository.sales.SalesOrderRepository;
import com.erp.formsmanagement.domain.repository.master.PartyRepository;
import com.erp.exportmanagement.DocumentRenderService;
import com.erp.service.DocumentFormat;
import com.erp.exception.EntityNotFoundException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Comparator;
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

  /**
   * Statements list chitthis in Ch. No. order, oldest first -- F-1, F-2, F-3 down the page, the way
   * the party reads their own book and the way a statement totals up. Not newest-first like the job
   * work page: that put a statement upside down (F-32 above F-31 ... above F-13), and a range
   * running into the next month put that month's F-1..F-4 at the very top.
   *
   * <p>Not date order either: several chitthis go out on one day, and the date alone leaves them
   * in whatever order the database returns. The number restarts every month -- the month the
   * chitthi was created (see JobWorkServiceImpl.assignJobWorkNo), which a backdated job date need
   * not match -- so it sorts by that month first, then the number. Rows with no number fall back
   * to their date, after the numbered ones of their month.
   */
  private static final Comparator<JobWorkEntity> JOB_WORK_CH_NO_ORDER =
      Comparator.comparing((JobWorkEntity jw) -> monthOf(jw.getCreatedAt(), jw.getJobDate()))
          .thenComparing(JobWorkEntity::getJobWorkNo, Comparator.nullsLast(Comparator.naturalOrder()))
          .thenComparing(JobWorkEntity::getJobDate, Comparator.nullsLast(Comparator.naturalOrder()))
          .thenComparing(JobWorkEntity::getId, Comparator.nullsLast(Comparator.naturalOrder()));

  /** The same rule for gres, whose serial carries its own "YYYY-MM". */
  private static final Comparator<GresFillingEntity> GRES_CH_NO_ORDER =
      Comparator.comparing(
              (GresFillingEntity g) ->
                  g.getChNoYearMonth() != null
                      ? g.getChNoYearMonth()
                      : monthOf(g.getCreatedAt(), g.getChitthiDate()))
          .thenComparing(GresFillingEntity::getChNoSerial, Comparator.nullsLast(Comparator.naturalOrder()))
          .thenComparing(GresFillingEntity::getChitthiDate, Comparator.nullsLast(Comparator.naturalOrder()))
          .thenComparing(GresFillingEntity::getId, Comparator.nullsLast(Comparator.naturalOrder()));

  /** "YYYY-MM" of the creation time, or of the chitthi date when a row has none. */
  private static String monthOf(LocalDateTime createdAt, LocalDate fallback) {
    if (createdAt != null) {
      return YearMonth.from(createdAt).toString();
    }
    return fallback != null ? YearMonth.from(fallback).toString() : "9999-12";
  }

  @Transactional(readOnly = true)
  public byte[] generateGresFillingReportPdf(Long partyId, LocalDate startDate, LocalDate endDate) {
    PartyEntity party = getParty(partyId);
    List<GresFillingEntity> records =
        gresFillingRepository
            .findByPartyIdAndChitthiDateBetweenOrderByChitthiDateAsc(partyId, startDate, endDate)
            .stream()
            .sorted(GRES_CH_NO_ORDER)
            .toList();

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
    List<JobWorkEntity> records =
        jobWorkRepository
            .findByPartyIdAndJobDateBetweenOrderByJobDateAsc(partyId, startDate, endDate)
            .stream()
            .sorted(JOB_WORK_CH_NO_ORDER)
            .toList();

    List<JobWorkEntity> outsideRecords =
        records.stream().filter(jw -> jw.getJobWorkType() == JobWorkType.OUTSIDE).toList();
    List<JobWorkEntity> insideRecords =
        records.stream().filter(jw -> jw.getJobWorkType() == JobWorkType.INHOUSE).toList();
    // MANUAL (order-less) job works have no workflow section of their own; show them alongside the
    // generic "Job Work" statement so they aren't silently dropped from the party statement.
    List<JobWorkEntity> plainJobWorkRecords =
        records.stream()
            .filter(
                jw ->
                    jw.getJobWorkType() == JobWorkType.JOB_WORK
                        || jw.getJobWorkType() == JobWorkType.MANUAL)
            .toList();

    // Ch. No. on the statement is the same party-wise job number the app's cards and the printed
    // chitthi show (ED-4, NP-22, …). Rows created before the label column existed have only the
    // serial, so rebuild the label from the party name rather than trusting the stored copy.
    Map<Long, String> chNoById = new HashMap<>();
    for (JobWorkEntity jw : records) {
      String label = JobWorkNumber.label(party.getName(), jw.getJobWorkNo());
      if (label == null) {
        label = jw.getJobWorkLabel() != null ? jw.getJobWorkLabel() : jw.getChitthiNo();
      }
      if (label != null) {
        chNoById.put(jw.getId(), label);
      }
    }

    Map<String, Object> variables = new HashMap<>();
    variables.put("partyName", party.getName());
    variables.put("startDate", startDate);
    variables.put("endDate", endDate);
    variables.put("outsideRecords", outsideRecords);
    variables.put("insideRecords", insideRecords);
    variables.put("plainJobWorkRecords", plainJobWorkRecords);
    variables.put("chNoById", chNoById);

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
