package com.erp.invoicemanagement.mapper;

import com.erp.api.invoicemanagement.model.Invoice;
import com.erp.api.invoicemanagement.model.InvoiceInfo;
import com.erp.api.invoicemanagement.model.NewInvoice;
import com.erp.invoicemanagement.domain.InvoiceEntity;
import com.erp.invoicemanagement.domain.InvoiceItemEntity;
import java.util.List;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface InvoiceMapper {
  @Mapping(target = "rodtep", source = "rodTep")
  Invoice toDomain(InvoiceEntity entity);

  @Mapping(source = "rodtep", target = "rodTep")
  InvoiceEntity toEntity(NewInvoice newInvoice);

  @Mapping(source = "rodtep", target = "rodTep")
  void updateEntity(@MappingTarget InvoiceEntity entity, NewInvoice newInvoice);

  @AfterMapping
  default void linkChildren(NewInvoice source, @MappingTarget InvoiceEntity target) {

    if (target.getItems() != null) {
      for (InvoiceItemEntity item : target.getItems()) {
        item.setInvoice(target);
      }
    }
  }

  @AfterMapping
  default void linkChildrenOnUpdate(NewInvoice source, @MappingTarget InvoiceEntity target) {}

  List<Invoice> toDomainList(List<InvoiceEntity> invoiceEntities);

  InvoiceInfo toInfo(InvoiceEntity invoiceEntity);
}
