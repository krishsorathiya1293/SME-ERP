package com.erp.formsmanagement.clientportal.service;

import com.erp.api.clientportalmanagement.model.ClientCredentials;
import com.erp.api.clientportalmanagement.model.OrderRequest;
import com.erp.api.clientportalmanagement.model.OrderRequestStatus;
import com.erp.api.clientportalmanagement.model.PaginatedResultClientAccount;
import com.erp.api.clientportalmanagement.model.PaginatedResultOrderRequest;
import com.erp.api.clientportalmanagement.model.UpdateOrderRequestStatusRequest;
import java.util.Optional;

public interface AdminClientPortalService {

  PaginatedResultClientAccount getAllClientAccounts(
      Optional<Integer> page,
      Optional<Integer> size,
      Optional<String> search,
      Optional<String> sortBy,
      Optional<String> sortDirection);

  byte[] exportClientCredentials(boolean onlyPending);

  ClientCredentials resetClientCredentials(Long partyId);

  PaginatedResultOrderRequest getAllOrderRequests(
      Optional<Integer> page,
      Optional<Integer> size,
      Optional<String> sortBy,
      Optional<String> sortDirection,
      Optional<Long> partyId,
      Optional<OrderRequestStatus> status,
      Optional<String> search);

  OrderRequest updateOrderRequestStatus(Long id, UpdateOrderRequestStatusRequest request);
}
