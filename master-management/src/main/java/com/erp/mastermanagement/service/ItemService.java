package com.erp.mastermanagement.service;

import com.erp.api.mastermanagement.model.Item;
import com.erp.api.mastermanagement.model.NewItem;
import com.erp.api.mastermanagement.model.PaginatedResultItem;
import com.erp.service.CoreServiceV1;
import java.util.Optional;

public interface ItemService extends CoreServiceV1<NewItem, Item, Long> {
  PaginatedResultItem getAll(
      Optional<String> filterByStatus,
      Optional<String> search,
      Optional<Integer> page,
      Optional<Integer> size,
      Optional<String> sortByFields,
      Optional<String> direction);
}
