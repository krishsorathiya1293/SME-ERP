package com.erp.formsmanagement.domain.repository.invoice;

import com.erp.formsmanagement.domain.entity.invoice.InvoiceEntity;
import com.erp.formsmanagement.domain.entity.invoice.InvoiceType;
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
          cb.like(cb.lower(root.get("invoiceNo")), like),
          cb.like(cb.lower(root.get("exporterCompanyName")), like));
    };
  }

  Optional<InvoiceEntity> findByIdAndInvoiceType(Long id, InvoiceType invoiceType);
}
