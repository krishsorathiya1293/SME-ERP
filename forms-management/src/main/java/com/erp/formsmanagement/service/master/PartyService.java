package com.erp.formsmanagement.service.master;

import com.erp.api.mastermanagement.model.NewParty;
import com.erp.api.mastermanagement.model.Party;
import com.erp.service.CoreServiceV1;
import java.util.List;
import java.util.Optional;

public interface PartyService extends CoreServiceV1<NewParty, Party, Long> {
  List<Party> getAll(Optional<String> partyType, Optional<String> search);
}
