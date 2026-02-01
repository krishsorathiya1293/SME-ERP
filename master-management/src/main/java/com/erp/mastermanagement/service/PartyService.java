package com.erp.mastermanagement.service;

import com.erp.api.mastermanagement.model.NewParty;
import com.erp.api.mastermanagement.model.Party;
import com.erp.service.CoreService;
import java.util.List;
import java.util.Optional;

public interface PartyService extends CoreService<NewParty, Party, Long> {
  List<Party> getAll(Optional<String> partyType, Optional<String> search);
}
