package com.erp.formsmanagement.service.client;

import com.erp.api.clientmanagement.model.ClientInventoryImportResult;
import org.springframework.web.multipart.MultipartFile;

public interface ClientInventoryImportService {
  ClientInventoryImportResult importClientInventory(Long clientId, MultipartFile file);
}
