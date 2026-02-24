package com.erp.controller;

import com.erp.service.CoreServiceV1;
import com.erp.wrappers.CreateMany;
import com.erp.wrappers.CreateNone;
import com.erp.wrappers.CreateOne;
import com.erp.wrappers.CreateResult;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@RequiredArgsConstructor
public class GenericCrudDelegateV1<R, S, I> {

  private final CoreServiceV1<R, S, I> service;

  public ResponseEntity<S> getById(I id) {
    return ResponseEntity.ok(service.getById(id));
  }

  public ResponseEntity<S> createOne(R request) {
    CreateResult<S> result = service.save(request);

    if (result instanceof CreateOne<S>(S item)) {
      return ResponseEntity.status(HttpStatus.CREATED).body(item);
    }

    throw new IllegalStateException(
        "Expected CreateOne but got: " + result.getClass().getSimpleName());
  }

  public ResponseEntity<List<S>> createMany(R request) {
    CreateResult<S> result = service.save(request);

    if (result instanceof CreateMany<S>(List<S> items)) {
      return ResponseEntity.status(HttpStatus.CREATED).body(items);
    }

    if (result instanceof CreateNone<S>) {
      return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    throw new IllegalStateException(
        "Expected CreateMany/CreateNone but got: " + result.getClass().getSimpleName());
  }

  public ResponseEntity<S> update(I id, R request) {
    return ResponseEntity.ok(service.update(id, request));
  }

  public ResponseEntity<Void> delete(I id) {
    service.deleteById(id);
    return ResponseEntity.noContent().build();
  }
}
