package com.erp.formsmanagement.service.inventory;

import com.erp.api.itemmanagement.model.ImportResult;
import org.springframework.web.multipart.MultipartFile;

public interface ItemBlueprintImportService {

  /**
   * Wipes all existing items, sizes, inventory and stock data, then re-imports items, sizes and
   * packing details from a Stock Master Excel file. Finish / plating price fields are left empty.
   */
  ImportResult importStockMaster(MultipartFile file);
}
