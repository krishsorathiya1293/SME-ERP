package com.erp.controller;

import com.erp.service.CoreServiceV2;
import com.erp.service.GetAllServiceV2;

public abstract class AbstractCrudControllerV2<P, R, S, F, L> {

  private final GenericCrudDelegateV2<P, R, S, Long> crud;
  private final GetAllDelegateV2<P, F, L> page;

  protected AbstractCrudControllerV2(
      CoreServiceV2<P, R, S, Long> crudService, GetAllServiceV2<P, F, L> getAllService) {
    this.crud = new GenericCrudDelegateV2<>(crudService);
    this.page = new GetAllDelegateV2<>(getAllService);
  }

  protected final GenericCrudDelegateV2<P, R, S, Long> crud() {
    return crud;
  }

  protected final GetAllDelegateV2<P, F, L> page() {
    return page;
  }
}
