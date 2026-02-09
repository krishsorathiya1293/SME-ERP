package com.erp.invoicemanagement.service.impl;

import com.erp.api.invoicemanagement.model.Invoice;
import com.erp.api.invoicemanagement.model.InvoiceInfo;
import com.erp.api.invoicemanagement.model.NewInvoice;
import com.erp.api.invoicemanagement.model.PaginatedResultInvoice;
import com.erp.exception.EntityNotFoundException;
import com.erp.invoicemanagement.domain.InvoiceEntity;
import com.erp.invoicemanagement.domain.InvoiceType;
import com.erp.invoicemanagement.mapper.InvoiceMapper;
import com.erp.invoicemanagement.repository.InvoiceRepository;
import com.erp.invoicemanagement.service.InvoiceService;
import com.erp.util.PaginationUtils;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class InvoiceServiceImpl implements InvoiceService {

  private final InvoiceRepository invoiceRepository;
  private final InvoiceMapper invoiceMapper;

  @Override
  public List<Invoice> saveInvoice(NewInvoice invoice) {
    List<InvoiceEntity> invoices =
        Arrays.stream(InvoiceType.values())
            .map(
                type -> {
                  InvoiceEntity entity = invoiceMapper.toEntity(invoice);
                  entity.setInvoiceType(type);
                  return entity;
                })
            .toList();
    return invoiceMapper.toDomainList(invoiceRepository.saveAll(invoices));
  }

  @Override
  public Invoice getInvoiceByType(
      Long id, com.erp.api.invoicemanagement.model.InvoiceType invoiceType) {
    return invoiceMapper.toDomain(
        invoiceRepository
            .findByIdAndInvoiceType(id, InvoiceType.valueOf(String.valueOf(invoiceType)))
            .orElseThrow(() -> new EntityNotFoundException("Invoice not found with id " + id)));
  }

  @Override
  public PaginatedResultInvoice getAll(
      Optional<String> filterByType,
      Optional<String> search,
      Optional<Integer> page,
      Optional<Integer> size,
      Optional<String> sortByFields,
      Optional<String> direction) {
    Specification<InvoiceEntity> spec =
        Specification.where(invoiceRepository.filterBySearch(search));

    Page<InvoiceEntity> results =
        invoiceRepository.findAll(
            spec, PaginationUtils.getPageRequest(page, size, direction, sortByFields));

    List<InvoiceInfo> content = results.getContent().stream().map(invoiceMapper::toInfo).toList();

    return new PaginatedResultInvoice()
        .data(content)
        .empty(results.isEmpty())
        .first(results.isFirst())
        .last(results.isLast())
        .totalPages(results.getTotalPages())
        .size(results.getSize())
        .totalElements((int) results.getTotalElements());
  }

  @Override
  @Transactional(readOnly = true)
  public Invoice getById(Long id) {
    return invoiceMapper.toDomain(
        invoiceRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Invoice not found with id " + id)));
  }

  @Override
  public Invoice save(NewInvoice request) {
    return null;
  }

  @Override
  public Invoice update(Long id, NewInvoice request) {
    InvoiceEntity entity =
        invoiceRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Invoice not found with id " + id));
    invoiceMapper.updateEntity(entity, request);
    return invoiceMapper.toDomain(invoiceRepository.save(entity));
  }

  @Override
  public void deleteById(Long id) {
    invoiceRepository.deleteById(id);
  }
}
