package com.erp.formsmanagement.clientportal.service;

import com.erp.api.clientportalmanagement.model.CatalogItem;
import com.erp.api.clientportalmanagement.model.ChangePasswordRequest;
import com.erp.api.clientportalmanagement.model.ClientProfile;
import com.erp.api.clientportalmanagement.model.Company;
import com.erp.api.clientportalmanagement.model.NewOrderRequest;
import com.erp.api.clientportalmanagement.model.OrderRequest;
import com.erp.api.clientportalmanagement.model.PaginatedResultClientOrder;
import com.erp.api.clientportalmanagement.model.PaginatedResultOrderRequest;
import com.erp.api.clientportalmanagement.model.UpdateClientProfileRequest;
import java.util.List;
import java.util.Optional;

public interface ClientPortalService {

  ClientProfile getMyProfile();

  List<Company> getMyCompanies();

  ClientProfile updateMyProfile(UpdateClientProfileRequest request);

  void changeMyPassword(ChangePasswordRequest request);

  PaginatedResultClientOrder getMyOrders(
      Optional<Integer> page,
      Optional<Integer> size,
      Optional<String> sortBy,
      Optional<String> sortDirection);

  List<CatalogItem> getMyCatalog();

  OrderRequest submitOrderRequest(NewOrderRequest request);

  PaginatedResultOrderRequest getMyOrderRequests(
      Optional<Integer> page,
      Optional<Integer> size,
      Optional<String> sortBy,
      Optional<String> sortDirection);
}
