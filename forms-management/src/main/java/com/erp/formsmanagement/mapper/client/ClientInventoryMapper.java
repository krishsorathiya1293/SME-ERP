package com.erp.formsmanagement.mapper.client;

import com.erp.api.clientmanagement.model.ClientInventory;
import com.erp.api.clientmanagement.model.ClientInventorySize;
import com.erp.api.clientmanagement.model.IdName;
import com.erp.api.clientmanagement.model.NewClientInventory;
import com.erp.formsmanagement.domain.entity.client.ClientInventoryEntity;
import com.erp.formsmanagement.domain.entity.inventory.InventoryEntity;
import com.erp.formsmanagement.domain.entity.inventory.ItemBlueprintDataEntity;
import com.erp.formsmanagement.domain.entity.master.PartyEntity;
import com.erp.mapper.EntityMapper;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ClientInventoryMapper
    extends EntityMapper<ClientInventoryEntity, NewClientInventory, ClientInventory> {

  @Mapping(source = "party.id", target = "client.id")
  @Mapping(source = "party.name", target = "client.name")
  @Mapping(source = "size", target = "size")
  @Mapping(target = "isOverridden", expression = "java(true)")
  @Mapping(target = "createdAt", expression = "java(map(entity.getCreatedAt()))")
  @Mapping(target = "lastUpdatedAt", expression = "java(map(entity.getLastUpdatedAt()))")
  ClientInventory toDomain(ClientInventoryEntity entity);

  @Mapping(target = "party", ignore = true)
  @Mapping(target = "size", ignore = true)
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "lastUpdatedAt", ignore = true)
  ClientInventoryEntity toEntity(NewClientInventory request);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "party", ignore = true)
  @Mapping(target = "size", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "lastUpdatedAt", ignore = true)
  void updateEntity(@MappingTarget ClientInventoryEntity entity, NewClientInventory request);

  /**
   * Maps a base InventoryEntity to a ClientInventory response where no client override exists.
   * isOverridden = false, all pricing comes from base inventory.
   */
  default ClientInventory fromBaseInventory(InventoryEntity inv, PartyEntity client) {
    ClientInventory dto = new ClientInventory();
    dto.setId(inv.getId());
    dto.setIsOverridden(false);

    // client info
    IdName clientDto = new IdName();
    clientDto.setId(client.getId());
    clientDto.setName(client.getName());
    dto.setClient(clientDto);

    // size info
    dto.setSize(toSize(inv.getSize()));

    // pricing from base inventory
    dto.setPcsPerBox(inv.getPcsPerBox());
    dto.setBoxPerCarton(inv.getBoxPerCarton());
    dto.setPcsPerCarton(inv.getPcsPerCarton());
    dto.setCartonWeight(inv.getCartonWeight());
    dto.setSssatinlacq(inv.getSs());
    dto.setAntiq(inv.getAntiq());
    dto.setSidegold(inv.getSidegold());
    dto.setZblack(inv.getZblack());
    dto.setGrblack(inv.getGrblack());
    dto.setMattss(inv.getMattss());
    dto.setMattantiq(inv.getMattantiq());
    dto.setPvdrose(inv.getPvdrose());
    dto.setPvdgold(inv.getPvdgold());
    dto.setPvdblack(inv.getPvdblack());
    dto.setRosegold(inv.getRosegold());
    dto.setClearlacq(inv.getClearlacq());

    dto.setCreatedAt(map(inv.getCreatedAt()));
    dto.setLastUpdatedAt(map(inv.getLastUpdatedAt()));
    return dto;
  }

  default ClientInventorySize toSize(ItemBlueprintDataEntity size) {
    if (size == null) return null;
    ClientInventorySize dto = new ClientInventorySize();
    dto.setId(size.getId());
    dto.setSizeInInch(size.getSizeInInch());
    dto.setSizeInMm(size.getSizeInMm());
    dto.setDozenWeight(size.getDozenWeight());
    if (size.getItem() != null) {
      IdName item = new IdName();
      item.setId(size.getItem().getId());
      item.setName(size.getItem().getItemName());
      dto.setItem(item);
    }
    return dto;
  }

  default OffsetDateTime map(LocalDateTime value) {
    if (value == null) return null;
    return value.atOffset(ZoneOffset.UTC);
  }
}
