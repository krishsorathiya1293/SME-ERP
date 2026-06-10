package com.erp.formsmanagement.clientportal.controller;

import com.erp.api.clientportalmanagement.AdminClientPortalManagementApi;
import com.erp.api.clientportalmanagement.model.ClientCredentials;
import com.erp.api.clientportalmanagement.model.OrderRequest;
import com.erp.api.clientportalmanagement.model.OrderRequestStatus;
import com.erp.api.clientportalmanagement.model.PaginatedResultClientAccount;
import com.erp.api.clientportalmanagement.model.PaginatedResultOrderRequest;
import com.erp.api.clientportalmanagement.model.UpdateOrderRequestStatusRequest;
import com.erp.formsmanagement.clientportal.service.AdminClientPortalService;
import com.erp.usermanagement.security.roles.Admin;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Admin
public class AdminClientPortalController implements AdminClientPortalManagementApi {

  private final AdminClientPortalService adminClientPortalService;

  @Override
  public ResponseEntity<PaginatedResultClientAccount> getAllClientAccounts(
      Optional<Integer> page,
      Optional<Integer> size,
      Optional<String> search,
      Optional<String> sortByFields,
      Optional<String> direction) {
    return ResponseEntity.ok(
        adminClientPortalService.getAllClientAccounts(page, size, search, sortByFields, direction));
  }

  @Override
  public ResponseEntity<Resource> exportClientCredentials(Optional<Boolean> onlyPending) {
    byte[] workbook = adminClientPortalService.exportClientCredentials(onlyPending.orElse(false));
    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename("client-credentials.xlsx").build().toString())
        .body(new ByteArrayResource(workbook));
  }

  @Override
  public ResponseEntity<ClientCredentials> resetClientCredentials(Long partyId) {
    return ResponseEntity.ok(adminClientPortalService.resetClientCredentials(partyId));
  }

  @Override
  public ResponseEntity<PaginatedResultOrderRequest> getAllOrderRequests(
      Optional<Integer> page,
      Optional<Integer> size,
      Optional<String> sortByFields,
      Optional<String> direction,
      Optional<Long> partyId,
      Optional<OrderRequestStatus> status,
      Optional<String> search) {
    return ResponseEntity.ok(
        adminClientPortalService.getAllOrderRequests(
            page, size, sortByFields, direction, partyId, status, search));
  }

  @Override
  public ResponseEntity<OrderRequest> updateOrderRequestStatus(
      Long id, UpdateOrderRequestStatusRequest updateOrderRequestStatusRequest) {
    return ResponseEntity.ok(
        adminClientPortalService.updateOrderRequestStatus(id, updateOrderRequestStatusRequest));
  }
}
