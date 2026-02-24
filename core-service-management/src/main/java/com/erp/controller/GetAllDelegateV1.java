package com.erp.controller;

import com.erp.service.GetAllServiceV1;
import com.erp.util.GetAllQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;

@RequiredArgsConstructor
public class GetAllDelegateV1<F, S> {

  private final GetAllServiceV1<F, S> service;

  public ResponseEntity<S> getAll(GetAllQuery<F> query) {
    return ResponseEntity.ok(service.getAll(query));
  }
}
