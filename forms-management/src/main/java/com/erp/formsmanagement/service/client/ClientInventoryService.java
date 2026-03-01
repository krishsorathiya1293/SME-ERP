package com.erp.formsmanagement.service.client;

import com.erp.api.clientmanagement.model.ClientInventory;
import com.erp.api.clientmanagement.model.NewClientInventory;
import com.erp.api.clientmanagement.model.PaginatedResultClientInventory;
import com.erp.formsmanagement.domain.entity.client.filter.ClientInventoryFilter;
import com.erp.service.CoreServiceV2;
import com.erp.service.GetAllServiceV2;

public interface ClientInventoryService
    extends CoreServiceV2<Long, NewClientInventory, ClientInventory, Long>,
        GetAllServiceV2<Long, ClientInventoryFilter, PaginatedResultClientInventory> {
}
