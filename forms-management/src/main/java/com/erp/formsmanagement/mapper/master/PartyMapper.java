package com.erp.formsmanagement.mapper.master;

import com.erp.api.mastermanagement.model.NewParty;
import com.erp.api.mastermanagement.model.Party;
import com.erp.formsmanagement.domain.entity.master.PartyEntity;
import com.erp.mapper.EntityMapper;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface PartyMapper extends EntityMapper<PartyEntity, NewParty, Party> {
  Party toDomain(PartyEntity partyEntity);

  PartyEntity toEntity(NewParty newParty);

  void updateEntity(@MappingTarget PartyEntity partyEntity, NewParty newParty);
}
