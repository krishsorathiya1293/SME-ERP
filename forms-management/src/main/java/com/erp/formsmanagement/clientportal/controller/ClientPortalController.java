package com.erp.formsmanagement.clientportal.controller;

import com.erp.api.clientportalmanagement.ClientClientPortalManagementApi;
import com.erp.api.clientportalmanagement.model.CatalogItem;
import com.erp.api.clientportalmanagement.model.ChangePasswordRequest;
import com.erp.api.clientportalmanagement.model.ClientProfile;
import com.erp.api.clientportalmanagement.model.NewOrderRequest;
import com.erp.api.clientportalmanagement.model.OrderRequest;
import com.erp.api.clientportalmanagement.model.PaginatedResultClientOrder;
import com.erp.api.clientportalmanagement.model.PaginatedResultOrderRequest;
import com.erp.api.clientportalmanagement.model.UpdateClientProfileRequest;
import com.erp.formsmanagement.clientportal.service.ClientPortalService;
import com.erp.usermanagement.security.roles.Client;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Client
public class ClientPortalController implements ClientClientPortalManagementApi {

  private final ClientPortalService clientPortalService;

  @Override
  public ResponseEntity<ClientProfile> getMyProfile() {
    return ResponseEntity.ok(clientPortalService.getMyProfile());
  }

  @Override
  public ResponseEntity<ClientProfile> updateMyProfile(
      UpdateClientProfileRequest updateClientProfileRequest) {
    return ResponseEntity.ok(clientPortalService.updateMyProfile(updateClientProfileRequest));
  }

  @Override
  public ResponseEntity<Void> changeMyPassword(ChangePasswordRequest changePasswordRequest) {
    clientPortalService.changeMyPassword(changePasswordRequest);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<PaginatedResultClientOrder> getMyOrders(
      Optional<Integer> page,
      Optional<Integer> size,
      Optional<String> sortByFields,
      Optional<String> direction) {
    return ResponseEntity.ok(clientPortalService.getMyOrders(page, size, sortByFields, direction));
  }

  @Override
  public ResponseEntity<List<CatalogItem>> getMyCatalog() {
    return ResponseEntity.ok(clientPortalService.getMyCatalog());
  }

  @Override
  public ResponseEntity<OrderRequest> submitOrderRequest(NewOrderRequest newOrderRequest) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(clientPortalService.submitOrderRequest(newOrderRequest));
  }

  @Override
  public ResponseEntity<PaginatedResultOrderRequest> getMyOrderRequests(
      Optional<Integer> page,
      Optional<Integer> size,
      Optional<String> sortByFields,
      Optional<String> direction) {
    return ResponseEntity.ok(
        clientPortalService.getMyOrderRequests(page, size, sortByFields, direction));
  }
}
