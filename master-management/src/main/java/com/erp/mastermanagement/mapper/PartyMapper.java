package com.erp.mastermanagement.mapper;

import com.erp.api.mastermanagement.model.NewParty;
import com.erp.api.mastermanagement.model.Party;
import com.erp.mastermanagement.domain.PartyEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface PartyMapper {
    Party toDomain(PartyEntity partyEntity);

    PartyEntity toEntity(NewParty newParty);

    void updateEntity(@MappingTarget PartyEntity partyEntity, NewParty newParty);
}
