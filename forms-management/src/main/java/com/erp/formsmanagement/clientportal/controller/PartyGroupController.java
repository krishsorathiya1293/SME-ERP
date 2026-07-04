package com.erp.formsmanagement.clientportal.controller;

import com.erp.api.clientportalmanagement.PartyGroupClientPortalManagementApi;
import com.erp.api.clientportalmanagement.model.AssignPartyGroupRequest;
import com.erp.api.clientportalmanagement.model.ClientCredentials;
import com.erp.api.clientportalmanagement.model.NewPartyGroup;
import com.erp.api.clientportalmanagement.model.PartyGroup;
import com.erp.api.clientportalmanagement.model.SetGroupPartiesRequest;
import com.erp.formsmanagement.clientportal.service.PartyGroupService;
import com.erp.usermanagement.security.roles.Admin;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Admin
public class PartyGroupController implements PartyGroupClientPortalManagementApi {

  private final PartyGroupService partyGroupService;

  @Override
  public ResponseEntity<List<PartyGroup>> getAllPartyGroups() {
    return ResponseEntity.ok(partyGroupService.getAll());
  }

  @Override
  public ResponseEntity<PartyGroup> createPartyGroup(NewPartyGroup newPartyGroup) {
    return ResponseEntity.status(HttpStatus.CREATED).body(partyGroupService.create(newPartyGroup));
  }

  @Override
  public ResponseEntity<PartyGroup> setPartyGroupParties(
      Long groupId, SetGroupPartiesRequest setGroupPartiesRequest) {
    return ResponseEntity.ok(partyGroupService.setParties(groupId, setGroupPartiesRequest));
  }

  @Override
  public ResponseEntity<Void> assignPartyGroup(
      Long partyId, AssignPartyGroupRequest assignPartyGroupRequest) {
    partyGroupService.assignPartyToGroup(partyId, assignPartyGroupRequest.getGroupId());
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<ClientCredentials> resetPartyGroupCredentials(Long groupId) {
    return ResponseEntity.ok(partyGroupService.resetCredentials(groupId));
  }
}
