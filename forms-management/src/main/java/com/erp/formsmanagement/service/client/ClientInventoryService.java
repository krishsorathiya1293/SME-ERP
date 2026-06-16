package com.erp.formsmanagement.service.client;

import com.erp.api.clientmanagement.model.ClientInventory;
import com.erp.api.clientmanagement.model.NewClientInventory;
import java.util.List;
import java.util.Optional;

public interface ClientInventoryService {

  List<ClientInventory> getAll(Long clientId, Optional<Long> sizeId, Optional<String> search);

  ClientInventory getById(Long clientId, Long id);

  ClientInventory create(Long clientId, NewClientInventory request);

  ClientInventory update(Long clientId, Long id, NewClientInventory request);

  void deleteById(Long clientId, Long id);
}
