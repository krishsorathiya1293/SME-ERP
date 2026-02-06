package com.erp.invoicemanagement.repository;

import com.erp.invoicemanagement.domain.InvoiceEntity;
import com.erp.invoicemanagement.domain.InvoiceType;
import com.erp.repository.CoreRepository;
import java.util.Optional;
import org.springframework.data.jpa.domain.Specification;

public interface InvoiceRepository extends CoreRepository<InvoiceEntity, Long> {
  default Specification<InvoiceEntity> filterBySearch(Optional<String> search) {
    return (root, query, cb) -> {
      if (search.isEmpty() || search.get().isBlank()) {
        return cb.conjunction();
      }
      String like = "%" + search.get().toLowerCase() + "%";
      return cb.or(
          cb.like(cb.lower(root.get("invoiceNumber")), like),
          cb.like(cb.lower(root.get("customerName")), like));
    };
  }

  Optional<InvoiceEntity> findByIdAndInvoiceType(Long id, InvoiceType invoiceType);
}
