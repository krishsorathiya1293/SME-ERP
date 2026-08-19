package com.erp.formsmanagement.domain.repository.master;

import com.erp.formsmanagement.domain.entity.master.AppSettingEntity;
import com.erp.repository.CoreRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public interface AppSettingRepository extends CoreRepository<AppSettingEntity, Long> {
  Optional<AppSettingEntity> findBySettingKey(String settingKey);
}
