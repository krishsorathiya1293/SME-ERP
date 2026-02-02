package com.erp.service;

public interface CoreServiceV2<P, R, S, I> {
  S getById(P entityId, I id);

  S save(P entityId, R request);

  S update(P entityId, I id, R request);

  void deleteById(P entityId, I id);
}
