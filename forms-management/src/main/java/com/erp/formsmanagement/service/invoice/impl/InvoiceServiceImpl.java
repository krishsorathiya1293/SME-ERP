package com.erp.formsmanagement.service.invoice.impl;

import com.erp.api.invoicemanagement.model.Invoice;
import com.erp.api.invoicemanagement.model.NewInvoice;
import com.erp.api.invoicemanagement.model.PaginatedResultInvoice;
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
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

  /** Creates one invoice row per InvoiceType (EXPORT, COMMERCIAL, PACKAGING_LIST). */
  @Override
  public CreateResult<Invoice> save(NewInvoice invoice) {
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
    var result = new CreateMany<>(((InvoiceMapper) mapper()).toDomainList(saved));
    saved.forEach(
        e -> eventPublisher.publishEvent(new FormChangedEvent("invoice", e.getId(), "SAVE")));
    return result;
  }

  @Override
  public Invoice getInvoiceByType(Long id, String invoiceType) {
    return mapper()
        .toDomain(
            invoiceRepository
                .findByIdAndInvoiceType(id, InvoiceType.valueOf(String.valueOf(invoiceType)))
                .orElseThrow(
                    () ->
                        new com.erp.exception.EntityNotFoundException(
                            "Invoice not found with id " + id)));
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

  @Override
  public Invoice update(Long id, NewInvoice request) {
    Invoice result = super.update(id, request);
    eventPublisher.publishEvent(new FormChangedEvent("invoice", id, "UPDATE"));
    return result;
  }

  @Override
  public void deleteById(Long id) {
    super.deleteById(id);
    eventPublisher.publishEvent(new FormChangedEvent("invoice", id, "DELETE"));
  }
}
