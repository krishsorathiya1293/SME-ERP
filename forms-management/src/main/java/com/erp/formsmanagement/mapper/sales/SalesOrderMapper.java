package com.erp.formsmanagement.mapper.sales;

import com.erp.api.salesmanagement.model.NewSalesOrder;
import com.erp.api.salesmanagement.model.NewSalesOrderItem;
import com.erp.api.salesmanagement.model.SalesOrder;
import com.erp.api.salesmanagement.model.SalesOrderItem;
import com.erp.api.salesmanagement.model.SalesOrderParty;
import com.erp.api.salesmanagement.model.SalesOrderSize;
import com.erp.formsmanagement.domain.entity.inventory.ItemBlueprintDataEntity;
import com.erp.formsmanagement.domain.entity.master.PartyEntity;
import com.erp.formsmanagement.domain.entity.sales.SalesOrderEntity;
import com.erp.formsmanagement.domain.entity.sales.SalesOrderItemEntity;
import com.erp.mapper.EntityMapper;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface SalesOrderMapper
    extends EntityMapper<SalesOrderEntity, NewSalesOrder, SalesOrder> {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "party", ignore = true)
  @Mapping(target = "salesNo", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "lastUpdatedAt", ignore = true)
  @Mapping(target = "items", source = "items")
  SalesOrderEntity toEntity(NewSalesOrder request);

  @Mapping(target = "party", ignore = true)
  @Mapping(target = "salesNo", ignore = true)
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "lastUpdatedAt", ignore = true)
  @Mapping(target = "items", source = "items")
  void updateEntity(@MappingTarget SalesOrderEntity entity, NewSalesOrder request);

  @Mapping(target = "party", source = "party")
  @Mapping(target = "items", source = "items")
  @Mapping(target = "createdAt", source = "createdAt", qualifiedByName = "toOffsetDateTime")
  @Mapping(target = "lastUpdatedAt", source = "lastUpdatedAt", qualifiedByName = "toOffsetDateTime")
  SalesOrder toDomain(SalesOrderEntity entity);

  List<SalesOrder> toDomainList(List<SalesOrderEntity> entities);

  // ---- Item mappings ----

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "salesOrder", ignore = true)
  @Mapping(target = "size", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "lastUpdatedAt", ignore = true)
  SalesOrderItemEntity toItemEntity(NewSalesOrderItem item);

  @Mapping(target = "size", source = "size")
  SalesOrderItem toItemDomain(SalesOrderItemEntity entity);

  // ---- Sub-object mappings ----

  default SalesOrderParty toPartyDomain(PartyEntity party) {
    if (party == null) return null;
    return new SalesOrderParty().id(party.getId()).name(party.getName());
  }

  default SalesOrderSize toSizeDomain(ItemBlueprintDataEntity size) {
    if (size == null) return null;
    return new SalesOrderSize()
        .id(size.getId())
        .sizeInInch(size.getSizeInInch())
        .sizeInMm(size.getSizeInMm());
  }

  @Named("toOffsetDateTime")
  default OffsetDateTime toOffsetDateTime(LocalDateTime dateTime) {
    return dateTime == null ? null : dateTime.atOffset(ZoneOffset.UTC);
  }

  /** Set back-references on items after mapping (party is set by service). */
  @AfterMapping
  default void setItemBackRef(@MappingTarget SalesOrderEntity entity, NewSalesOrder source) {
    if (entity.getItems() != null) {
      for (SalesOrderItemEntity item : entity.getItems()) {
        item.setSalesOrder(entity);
      }
    }
  }
}
