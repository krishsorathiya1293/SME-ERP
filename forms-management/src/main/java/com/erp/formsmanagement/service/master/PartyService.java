package com.erp.formsmanagement.service.master;

import com.erp.api.mastermanagement.model.NewParty;
import com.erp.api.mastermanagement.model.Party;
import com.erp.service.CoreServiceV1;
import com.erp.service.GetAllServiceV1;
import java.util.List;

public interface PartyService
    extends CoreServiceV1<NewParty, Party, Long>, GetAllServiceV1<String, List<Party>> {}
