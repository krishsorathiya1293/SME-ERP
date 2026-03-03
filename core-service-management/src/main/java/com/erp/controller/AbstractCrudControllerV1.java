package com.erp.controller;

import com.erp.service.CoreServiceV1;
import com.erp.service.GetAllServiceV1;

public abstract class AbstractCrudControllerV1<R, S, F, L> {

  private final GenericCrudDelegateV1<R, S, Long> crud;
  private final GetAllDelegateV1<F, L> page;

  protected AbstractCrudControllerV1(
      CoreServiceV1<R, S, Long> crudService, GetAllServiceV1<F, L> getAllService) {
    this.crud = new GenericCrudDelegateV1<>(crudService);
    this.page = new GetAllDelegateV1<>(getAllService);
  }

  protected final GenericCrudDelegateV1<R, S, Long> crud() {
    return crud;
  }

  protected final GetAllDelegateV1<F, L> page() {
    return page;
  }
}
