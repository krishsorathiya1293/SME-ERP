package com.erp.formsmanagement.clientportal.service;

import com.erp.api.clientportalmanagement.model.ClientCredentials;
import com.erp.api.clientportalmanagement.model.NewPartyGroup;
import com.erp.api.clientportalmanagement.model.PartyGroup;
import com.erp.api.clientportalmanagement.model.SetGroupPartiesRequest;
import java.util.List;

/** Admin-facing management of party groups and their single shared client login. */
public interface PartyGroupService {

  List<PartyGroup> getAll();

  PartyGroup create(NewPartyGroup request);

  PartyGroup setParties(Long groupId, SetGroupPartiesRequest request);

  /** Assign a single party to a group, or remove it from its group when groupId is null. */
  void assignPartyToGroup(Long partyId, Long groupId);

  ClientCredentials resetCredentials(Long groupId);
}
