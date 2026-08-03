package com.erp.formsmanagement.mapper.master;

import com.erp.api.mastermanagement.model.Translation;
import com.erp.formsmanagement.domain.entity.master.TranslationEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TranslationMapper {
  Translation toDomain(TranslationEntity entity);
}
