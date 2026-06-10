package com.erp.exportmanagement.clientportal.service.impl;

import static com.erp.constant.Constant.INVOICE_FILENAME_FORMAT;

import com.erp.api.clientportalmanagement.model.ClientInvoice;
import com.erp.api.clientportalmanagement.model.ClientInvoiceType;
import com.erp.api.clientportalmanagement.model.PaginatedResultClientInvoice;
import com.erp.exception.EntityNotFoundException;
import com.erp.exportmanagement.clientportal.service.ClientInvoicePdfResult;
import com.erp.exportmanagement.clientportal.service.ClientInvoiceService;
import com.erp.exportmanagement.service.ExportService;
import com.erp.formsmanagement.clientportal.service.CurrentClientProvider;
import com.erp.formsmanagement.domain.entity.invoice.InvoiceEntity;
import com.erp.formsmanagement.domain.entity.master.PartyEntity;
import com.erp.formsmanagement.domain.repository.invoice.InvoiceRepository;
import com.erp.formsmanagement.domain.repository.master.PartyRepository;
import com.erp.service.DocumentFormat;
import com.erp.usermanagement.model.entity.UserEntity;
import com.erp.util.PageMapper;
import com.erp.util.PaginationUtils;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClientInvoiceServiceImpl implements ClientInvoiceService {

  private final CurrentClientProvider currentClientProvider;
  private final PartyRepository partyRepository;
  private final InvoiceRepository invoiceRepository;
  private final ExportService exportService;

  @Override
  @Transactional(readOnly = true)
  public PaginatedResultClientInvoice getMyInvoices(
      Optional<Integer> page,
      Optional<Integer> size,
      Optional<String> sortBy,
      Optional<String> sortDirection) {
    PartyEntity party = getParty(currentClientProvider.getCurrentUser());

    List<InvoiceEntity> matching =
        invoiceRepository.findAll().stream()
            .filter(invoice -> belongsToParty(invoice, party))
            .sorted(
                Comparator.comparing(
                        InvoiceEntity::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(InvoiceEntity::getId, Comparator.nullsLast(Comparator.reverseOrder())))
            .toList();

    Pageable pageable = PaginationUtils.getPageRequest(page, size, sortDirection, sortBy);
    int start = (int) pageable.getOffset();
    int end = Math.min(start + pageable.getPageSize(), matching.size());
    List<InvoiceEntity> content = start >= matching.size() ? List.of() : matching.subList(start, end);

    Page<InvoiceEntity> result = new PageImpl<>(content, pageable, matching.size());

    return PageMapper.toResult(
        result,
        this::toClientInvoice,
        PaginatedResultClientInvoice::new,
        PaginatedResultClientInvoice::setData);
  }

  @Override
  @Transactional(readOnly = true)
  public ClientInvoicePdfResult getMyInvoicePdf(Long id) {
    PartyEntity party = getParty(currentClientProvider.getCurrentUser());

    InvoiceEntity invoice =
        invoiceRepository
            .findById(id)
            .filter(inv -> belongsToParty(inv, party))
            .orElseThrow(() -> new EntityNotFoundException("Invoice not found with id: " + id));

    byte[] pdfBytes =
        exportService.getCachedDocument(
            "invoice", id, String.valueOf(invoice.getInvoiceType()), DocumentFormat.PDF);

    String filename =
        String.format(
            INVOICE_FILENAME_FORMAT,
            invoice.getInvoiceNo(),
            exportService.shortName(invoice.getExporterCompanyName()),
            invoice.getInvoiceType().name())
            + ".pdf";

    return new ClientInvoicePdfResult(pdfBytes, filename);
  }

  private boolean belongsToParty(InvoiceEntity invoice, PartyEntity party) {
    String partyName = party.getName() == null ? "" : party.getName().trim();
    String billToName = invoice.getBillToName() == null ? "" : invoice.getBillToName().trim();
    return !partyName.isEmpty() && partyName.equalsIgnoreCase(billToName);
  }

  private ClientInvoice toClientInvoice(InvoiceEntity invoice) {
    return new ClientInvoice()
        .id(invoice.getId())
        .invoiceNo(invoice.getInvoiceNo())
        .invoiceDate(invoice.getInvoiceDate())
        .invoiceType(
            invoice.getInvoiceType() == null
                ? null
                : ClientInvoiceType.valueOf(invoice.getInvoiceType().name()))
        .exporterCompanyName(invoice.getExporterCompanyName())
        .createdAt(
            invoice.getCreatedAt() == null ? null : invoice.getCreatedAt().atOffset(ZoneOffset.UTC));
  }

  private PartyEntity getParty(UserEntity user) {
    return partyRepository
        .findById(user.getPartyId())
        .orElseThrow(
            () -> new EntityNotFoundException("Party not found with id: " + user.getPartyId()));
  }
}
