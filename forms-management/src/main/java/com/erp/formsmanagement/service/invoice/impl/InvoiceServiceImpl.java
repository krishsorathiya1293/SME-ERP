package com.erp.formsmanagement.service.invoice.impl;

import com.erp.api.invoicemanagement.model.Invoice;
import com.erp.api.invoicemanagement.model.NewInvoice;
import com.erp.api.invoicemanagement.model.PaginatedResultInvoice;
import com.erp.api.invoicemanagement.model.PastImporter;
import com.erp.event.FormChangedEvent;
import com.erp.formsmanagement.domain.entity.invoice.InvoiceEntity;
import com.erp.formsmanagement.domain.entity.invoice.InvoiceType;
import com.erp.formsmanagement.domain.repository.invoice.InvoiceRepository;
import com.erp.formsmanagement.mapper.invoice.InvoiceMapper;
import com.erp.formsmanagement.service.invoice.InvoiceService;
import com.erp.service.AbstractSpecificationServiceV1;
import com.erp.util.GetAllQuery;
import com.erp.util.PageMapper;
import com.erp.util.PaginationUtils;
import com.erp.wrappers.CreateMany;
import com.erp.wrappers.CreateResult;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class InvoiceServiceImpl
    extends AbstractSpecificationServiceV1<InvoiceEntity, NewInvoice, Invoice>
    implements InvoiceService {

  private final InvoiceRepository invoiceRepository;
  private final ApplicationEventPublisher eventPublisher;

  public InvoiceServiceImpl(
      InvoiceRepository invoiceRepository,
      InvoiceMapper invoiceMapper,
      ApplicationEventPublisher eventPublisher) {
    super(invoiceRepository, invoiceMapper);
    this.invoiceRepository = invoiceRepository;
    this.eventPublisher = eventPublisher;
  }

  /**
   * Creates one invoice row per InvoiceType (EXPORT, COMMERCIAL, PACKAGING_LIST), tied together by
   * a shared groupId (the export row's id) so edits and downloads reach all three.
   */
  @Override
  public CreateResult<Invoice> save(NewInvoice invoice) {
    requireUniqueInvoiceNo(invoice.getInvoiceNo(), null);
    List<InvoiceEntity> invoices =
        Arrays.stream(InvoiceType.values())
            .map(
                type -> {
                  InvoiceEntity entity = mapper().toEntity(invoice);
                  entity.setInvoiceType(type);
                  return entity;
                })
            .toList();
    List<InvoiceEntity> saved = invoiceRepository.saveAll(invoices);
    Long groupId = saved.getFirst().getId();
    saved.forEach(e -> e.setGroupId(groupId));
    saved = invoiceRepository.saveAll(saved);
    var result = new CreateMany<>(((InvoiceMapper) mapper()).toDomainList(saved));
    saved.forEach(
        e -> eventPublisher.publishEvent(new FormChangedEvent("invoice", e.getId(), "SAVE")));
    return result;
  }

  /**
   * The row of the requested type in the same save as {@code id}. Any of the three ids works, so a
   * caller holding only the export row's id still gets the commercial invoice and packing list.
   */
  @Override
  public Invoice getInvoiceByType(Long id, String invoiceType) {
    InvoiceType type = InvoiceType.valueOf(String.valueOf(invoiceType));
    InvoiceEntity row =
        invoiceRepository
            .findById(id)
            .orElseThrow(
                () ->
                    new com.erp.exception.EntityNotFoundException(
                        "Invoice not found with id " + id));
    if (row.getInvoiceType() != type && row.getGroupId() != null) {
      row = invoiceRepository.findFirstByGroupIdAndInvoiceType(row.getGroupId(), type).orElse(row);
    }
    return mapper().toDomain(row);
  }

  @Override
  public PaginatedResultInvoice getAll(GetAllQuery<String> query) {
    Specification<InvoiceEntity> spec =
        Specification.where(invoiceRepository.filterBySearch(query.search()));
    Page<InvoiceEntity> results =
        invoiceRepository.findAll(
            spec,
            PaginationUtils.getPageRequest(
                query.page(), query.size(), query.direction(), query.sortBy()));
    return PageMapper.toResult(
        results,
        ((InvoiceMapper) mapper())::toInfo,
        PaginatedResultInvoice::new,
        PaginatedResultInvoice::setData);
  }

  /** Edits apply to every row of the save, so the three documents never drift apart. */
  @Override
  public Invoice update(Long id, NewInvoice request) {
    InvoiceEntity existing =
        invoiceRepository
            .findById(id)
            .orElseThrow(
                () ->
                    new com.erp.exception.EntityNotFoundException(
                        "Invoice not found with id " + id));
    requireUniqueInvoiceNo(
        request.getInvoiceNo(), existing.getGroupId() != null ? existing.getGroupId() : id);
    Invoice result = super.update(id, request);
    InvoiceEntity row = invoiceRepository.findById(id).orElseThrow();
    if (row.getGroupId() != null) {
      for (InvoiceEntity sibling : invoiceRepository.findByGroupId(row.getGroupId())) {
        if (sibling.getId().equals(id)) continue;
        mapper().updateEntity(sibling, request);
        invoiceRepository.save(sibling);
        eventPublisher.publishEvent(new FormChangedEvent("invoice", sibling.getId(), "UPDATE"));
      }
    }
    eventPublisher.publishEvent(new FormChangedEvent("invoice", id, "UPDATE"));
    return result;
  }

  /**
   * The Bill To of every past invoice (one row per save), deduplicated on name + address so picking
   * "ABC Limited" again fills in the details last used for it.
   */
  @Override
  @Transactional(readOnly = true)
  public List<PastImporter> getPastImporters() {
    Map<String, PastImporter> byParty = new LinkedHashMap<>();
    invoiceRepository.findAll().stream()
        .filter(e -> e.getGroupId() == null || e.getGroupId().equals(e.getId()))
        .filter(e -> e.getBillToName() != null && !e.getBillToName().isBlank())
        .sorted(Comparator.comparing(InvoiceEntity::getId).reversed())
        .forEach(
            e -> {
              String address = e.getBillToAddress() == null ? "" : e.getBillToAddress().trim();
              String key = (e.getBillToName().trim() + "|" + address).toLowerCase(Locale.ROOT);
              byParty.computeIfAbsent(
                  key,
                  k ->
                      new PastImporter()
                          .country(e.getBillToCountry())
                          .name(e.getBillToName().trim())
                          .contactNo(e.getBillToContactNo())
                          .address(e.getBillToAddress())
                          .lastInvoiceNo(e.getInvoiceNo())
                          .lastInvoiceDate(e.getInvoiceDate()));
            });
    return List.copyOf(byParty.values());
  }

  /**
   * An invoice number names one save (its three documents). Reusing it would print two different
   * invoices under the same number, so it is refused; {@code ownGroupId} is the save being edited.
   */
  private void requireUniqueInvoiceNo(String invoiceNo, Long ownGroupId) {
    if (invoiceNo == null || invoiceNo.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invoice No. is required");
    }
    boolean taken =
        invoiceRepository.findByInvoiceNoIgnoreCase(invoiceNo.trim()).stream()
            .anyMatch(
                e ->
                    ownGroupId == null
                        || !(ownGroupId.equals(e.getGroupId()) || ownGroupId.equals(e.getId())));
    if (taken) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "Invoice No. " + invoiceNo.trim() + " already exists. Use a different number.");
    }
  }

  @Override
  public void deleteById(Long id) {
    super.deleteById(id);
    eventPublisher.publishEvent(new FormChangedEvent("invoice", id, "DELETE"));
  }
}
