package com.erp.controller;

import com.erp.service.GetAllServiceV2;
import com.erp.util.GetAllQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;

@RequiredArgsConstructor
public class GetAllDelegateV2<P, F, S> {

  private final GetAllServiceV2<P, F, S> service;

  public ResponseEntity<S> getAll(P parentId, GetAllQuery<F> query) {
    return ResponseEntity.ok(service.getAll(parentId, query));
  }
}
