package com.erp.controller;

import com.erp.service.CoreServiceV2;
import com.erp.wrappers.CreateMany;
import com.erp.wrappers.CreateNone;
import com.erp.wrappers.CreateOne;
import com.erp.wrappers.CreateResult;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@RequiredArgsConstructor
public class GenericCrudDelegateV2<P, R, S, I> {

  private final CoreServiceV2<P, R, S, I> service;

  public ResponseEntity<S> getById(P parentId, I id) {
    return ResponseEntity.ok(service.getById(parentId, id));
  }

  public ResponseEntity<S> createOne(P parentId, R request) {
    CreateResult<S> result = service.save(parentId, request);

    if (result instanceof CreateOne<S>(S item)) {
      return ResponseEntity.status(HttpStatus.CREATED).body(item);
    }

    throw new IllegalStateException(
        "Expected CreateOne but got: " + result.getClass().getSimpleName());
  }

  public ResponseEntity<List<S>> createMany(P parentId, R request) {
    CreateResult<S> result = service.save(parentId, request);

    if (result instanceof CreateMany<S>(List<S> items)) {
      return ResponseEntity.status(HttpStatus.CREATED).body(items);
    }

    if (result instanceof CreateNone<S>) {
      return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    throw new IllegalStateException(
        "Expected CreateMany/CreateNone but got: " + result.getClass().getSimpleName());
  }

  public ResponseEntity<S> update(P parentId, I id, R request) {
    return ResponseEntity.ok(service.update(parentId, id, request));
  }

  public ResponseEntity<Void> delete(P parentId, I id) {
    service.deleteById(parentId, id);
    return ResponseEntity.noContent().build();
  }
}
