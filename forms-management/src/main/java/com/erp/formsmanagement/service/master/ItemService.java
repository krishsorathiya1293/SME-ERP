package com.erp.formsmanagement.service.master;

import com.erp.api.mastermanagement.model.Item;
import com.erp.api.mastermanagement.model.NewItem;
import com.erp.api.mastermanagement.model.PaginatedResultItem;
import com.erp.service.CoreServiceV1;
import com.erp.service.GetAllServiceV1;

public interface ItemService
    extends CoreServiceV1<NewItem, Item, Long>, GetAllServiceV1<String, PaginatedResultItem> {}
