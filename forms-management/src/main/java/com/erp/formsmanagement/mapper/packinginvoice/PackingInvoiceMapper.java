package com.erp.formsmanagement.mapper.packinginvoice;

import com.erp.api.packinginvoicemanagement.model.NewPackingInvoice;
import com.erp.api.packinginvoicemanagement.model.NewPackingInvoiceItem;
import com.erp.api.packinginvoicemanagement.model.PackingInvoice;
import com.erp.api.packinginvoicemanagement.model.PackingInvoiceItem;
import com.erp.api.packinginvoicemanagement.model.PackingInvoiceParty;
import com.erp.api.packinginvoicemanagement.model.PackingInvoiceSize;
import com.erp.formsmanagement.domain.entity.inventory.ItemBlueprintDataEntity;
import com.erp.formsmanagement.domain.entity.master.PartyEntity;
import com.erp.formsmanagement.domain.entity.packinginvoice.PackingInvoiceEntity;
import com.erp.formsmanagement.domain.entity.packinginvoice.PackingInvoiceItemEntity;
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
public interface PackingInvoiceMapper
    extends EntityMapper<PackingInvoiceEntity, NewPackingInvoice, PackingInvoice> {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "party", ignore = true)
  @Mapping(target = "invoiceNo", ignore = true)
  @Mapping(target = "cartoonNo", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "lastUpdatedAt", ignore = true)
  @Mapping(target = "items", source = "items")
  PackingInvoiceEntity toEntity(NewPackingInvoice request);

  @Mapping(target = "party", ignore = true)
  @Mapping(target = "invoiceNo", ignore = true)
  @Mapping(target = "cartoonNo", ignore = true)
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "lastUpdatedAt", ignore = true)
  @Mapping(target = "items", source = "items")
  void updateEntity(@MappingTarget PackingInvoiceEntity entity, NewPackingInvoice request);

  @Mapping(target = "party", source = "party")
  @Mapping(target = "items", source = "items")
  @Mapping(target = "createdAt", source = "createdAt", qualifiedByName = "toOffsetDateTime")
  @Mapping(
      target = "lastUpdatedAt",
      source = "lastUpdatedAt",
      qualifiedByName = "toOffsetDateTime")
  PackingInvoice toDomain(PackingInvoiceEntity entity);

  List<PackingInvoice> toDomainList(List<PackingInvoiceEntity> entities);

  // ---- Item mappings ----

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "packingInvoice", ignore = true)
  @Mapping(target = "size", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "lastUpdatedAt", ignore = true)
  PackingInvoiceItemEntity toItemEntity(NewPackingInvoiceItem item);

  @Mapping(target = "size", source = "size")
  PackingInvoiceItem toItemDomain(PackingInvoiceItemEntity entity);

  // ---- Sub-object mappings ----

  default PackingInvoiceParty toPartyDomain(PartyEntity party) {
    if (party == null) return null;
    return new PackingInvoiceParty().id(party.getId()).name(party.getName());
  }

  default PackingInvoiceSize toSizeDomain(ItemBlueprintDataEntity size) {
    if (size == null) return null;
    return new PackingInvoiceSize()
        .id(size.getId())
        .sizeInInch(size.getSizeInInch())
        .sizeInMm(size.getSizeInMm())
        .dozenWeight(size.getDozenWeight());
  }

  @Named("toOffsetDateTime")
  default OffsetDateTime toOffsetDateTime(LocalDateTime dateTime) {
    return dateTime == null ? null : dateTime.atOffset(ZoneOffset.UTC);
  }

  /** Set back-references on items after mapping (party is set by service). */
  @AfterMapping
  default void setItemBackRef(
      @MappingTarget PackingInvoiceEntity entity, NewPackingInvoice source) {
    if (entity.getItems() != null) {
      for (PackingInvoiceItemEntity item : entity.getItems()) {
        item.setPackingInvoice(entity);
      }
    }
  }
}
