package com.erp.service;

public interface CoreServiceV1<R, S, I> {
  S getById(I id);

  S save(R request);

  S update(I id, R request);

  void deleteById(I id);
}
