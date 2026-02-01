package com.erp.mastermanagement.controller;

import com.erp.api.mastermanagement.PartyMasterManagementApi;
import com.erp.api.mastermanagement.model.NewParty;
import com.erp.api.mastermanagement.model.Party;
import com.erp.mastermanagement.service.PartyService;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PartyController implements PartyMasterManagementApi {

  private final PartyService partyService;

  @Override
  public ResponseEntity<Party> createParty(NewParty newParty) {
    return ResponseEntity.ok(partyService.save(newParty));
  }

  @Override
  public ResponseEntity<Void> deleteParty(Long id) {
    partyService.deleteById(id);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<List<Party>> getAllParties(
      Optional<String> partyType, Optional<String> search) {
    return ResponseEntity.ok(partyService.getAll(partyType, search));
  }

  @Override
  public ResponseEntity<Party> getPartyById(Long id) {
    return ResponseEntity.ok(partyService.getById(id));
  }

  @Override
  public ResponseEntity<Party> updateParty(Long id, NewParty newParty) {
    return ResponseEntity.ok(partyService.update(id, newParty));
  }
}
