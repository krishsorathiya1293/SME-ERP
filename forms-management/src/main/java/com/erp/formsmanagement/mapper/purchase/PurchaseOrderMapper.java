package com.erp.formsmanagement.mapper.purchase;

import com.erp.api.purchasemanagement.model.NewPurchaseOrder;
import com.erp.api.purchasemanagement.model.NewPurchaseOrderItem;
import com.erp.api.purchasemanagement.model.PurchaseOrder;
import com.erp.api.purchasemanagement.model.PurchaseOrderItem;
import com.erp.api.purchasemanagement.model.PurchaseOrderParty;
import com.erp.api.purchasemanagement.model.PurchaseOrderSize;
import com.erp.formsmanagement.domain.entity.inventory.ItemBlueprintDataEntity;
import com.erp.formsmanagement.domain.entity.master.PartyEntity;
import com.erp.formsmanagement.domain.entity.purchase.PurchaseOrderEntity;
import com.erp.formsmanagement.domain.entity.purchase.PurchaseOrderItemEntity;
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
public interface PurchaseOrderMapper
    extends EntityMapper<PurchaseOrderEntity, NewPurchaseOrder, PurchaseOrder> {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "party", ignore = true)
  @Mapping(target = "purchaseNo", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "lastUpdatedAt", ignore = true)
  @Mapping(target = "items", source = "items")
  PurchaseOrderEntity toEntity(NewPurchaseOrder request);

  @Mapping(target = "party", ignore = true)
  @Mapping(target = "purchaseNo", ignore = true)
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "lastUpdatedAt", ignore = true)
  @Mapping(target = "items", source = "items")
  void updateEntity(@MappingTarget PurchaseOrderEntity entity, NewPurchaseOrder request);

  @Mapping(target = "party", source = "party")
  @Mapping(target = "items", source = "items")
  @Mapping(target = "createdAt", source = "createdAt", qualifiedByName = "toOffsetDateTime")
  @Mapping(target = "lastUpdatedAt", source = "lastUpdatedAt", qualifiedByName = "toOffsetDateTime")
  PurchaseOrder toDomain(PurchaseOrderEntity entity);

  List<PurchaseOrder> toDomainList(List<PurchaseOrderEntity> entities);

  // ---- Item mappings ----

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "purchaseOrder", ignore = true)
  @Mapping(target = "size", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "lastUpdatedAt", ignore = true)
  PurchaseOrderItemEntity toItemEntity(NewPurchaseOrderItem item);

  @Mapping(target = "size", source = "size")
  PurchaseOrderItem toItemDomain(PurchaseOrderItemEntity entity);

  // ---- Sub-object mappings ----

  default PurchaseOrderParty toPartyDomain(PartyEntity party) {
    if (party == null) return null;
    return new PurchaseOrderParty().id(party.getId()).name(party.getName());
  }

  default PurchaseOrderSize toSizeDomain(ItemBlueprintDataEntity size) {
    if (size == null) return null;
    return new PurchaseOrderSize()
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
  default void setItemBackRef(
      @MappingTarget PurchaseOrderEntity entity, NewPurchaseOrder source) {
    if (entity.getItems() != null) {
      for (PurchaseOrderItemEntity item : entity.getItems()) {
        item.setPurchaseOrder(entity);
      }
    }
  }
}
