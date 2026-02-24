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
import com.erp.mapper.EntityMapper;
import com.erp.service.AbstractCrudServiceV1;
import com.erp.util.GetAllQuery;
import com.erp.util.PageMapper;
import com.erp.util.PaginationUtils;
import com.erp.wrappers.CreateMany;
import com.erp.wrappers.CreateResult;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class InvoiceServiceImpl extends AbstractCrudServiceV1<InvoiceEntity, NewInvoice, Invoice>
    implements InvoiceService {

  private final InvoiceRepository invoiceRepository;
  private final InvoiceMapper invoiceMapper;
  private final ApplicationEventPublisher eventPublisher;

  @Override
  protected JpaRepository<InvoiceEntity, Long> repository() {
    return invoiceRepository;
  }

  @Override
  protected EntityMapper<InvoiceEntity, NewInvoice, Invoice> mapper() {
    return invoiceMapper;
  }

  @Override
  public CreateResult<Invoice> save(NewInvoice invoice) {
    List<InvoiceEntity> invoices = Arrays.stream(InvoiceType.values())
        .map(
            type -> {
              InvoiceEntity entity = invoiceMapper.toEntity(invoice);
              entity.setInvoiceType(type);
              return entity;
            })
        .toList();
    List<InvoiceEntity> saved = invoiceRepository.saveAll(invoices);
    var result = new CreateMany<>(invoiceMapper.toDomainList(saved));
    saved.forEach(
        e -> eventPublisher.publishEvent(new FormChangedEvent("invoice", e.getId(), "SAVE")));
    return result;
  }

  @Override
  public Invoice getInvoiceByType(Long id, String invoiceType) {
    return invoiceMapper.toDomain(
        invoiceRepository
            .findByIdAndInvoiceType(id, InvoiceType.valueOf(String.valueOf(invoiceType)))
            .orElseThrow(() -> new com.erp.exception.EntityNotFoundException("Invoice not found with id " + id)));
  }

  @Override
  public PaginatedResultInvoice getAll(GetAllQuery<String> query) {
    Specification<InvoiceEntity> spec = Specification.where(invoiceRepository.filterBySearch(query.search()));
    Page<InvoiceEntity> results = invoiceRepository.findAll(
        spec,
        PaginationUtils.getPageRequest(
            query.page(), query.size(), query.direction(), query.sortBy()));
    return PageMapper.toResult(
        results, invoiceMapper::toInfo, PaginatedResultInvoice::new, PaginatedResultInvoice::setData);
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
