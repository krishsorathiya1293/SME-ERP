package com.erp.exportmanagement.config;

import com.erp.event.FormChangedEvent;
import com.erp.exportmanagement.service.ExportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PdfCacheEvictionListener {

  private final ExportService exportService;

  @EventListener
  public void onFormChanged(FormChangedEvent event) {
    log.info(
        "Form changed: type={} id={} action={}",
        event.formType(),
        event.entityId(),
        event.action());
    exportService.evictCache(event.formType(), event.entityId());
  }
}
