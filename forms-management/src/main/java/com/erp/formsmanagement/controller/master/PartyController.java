package com.erp.formsmanagement.controller.master;

import com.erp.api.mastermanagement.PartyMasterManagementApi;
import com.erp.api.mastermanagement.model.NewParty;
import com.erp.api.mastermanagement.model.Party;
import com.erp.controller.GenericCrudDelegateV1;
import com.erp.controller.GetAllDelegateV1;
import com.erp.formsmanagement.service.master.PartyService;
import com.erp.util.GetAllQuery;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PartyController implements PartyMasterManagementApi {

  private final PartyService partyService;

  private GenericCrudDelegateV1<NewParty, Party, Long> crud() {
    return new GenericCrudDelegateV1<>(partyService);
  }

  private GetAllDelegateV1<String, List<Party>> page() {
    return new GetAllDelegateV1<>(partyService);
  }

  @Override
  public ResponseEntity<Party> createParty(NewParty newParty) {
    return crud().createOne(newParty);
  }

  @Override
  public ResponseEntity<Void> deleteParty(Long id) {
    return crud().delete(id);
  }

  @Override
  public ResponseEntity<List<Party>> getAllParties(
      Optional<String> partyType, Optional<String> search) {

    return page().getAll(GetAllQuery.of(partyType, search));
  }

  @Override
  public ResponseEntity<Party> getPartyById(Long id) {
    return crud().getById(id);
  }

  @Override
  public ResponseEntity<Party> updateParty(Long id, NewParty newParty) {
    return crud().update(id, newParty);
  }
}
