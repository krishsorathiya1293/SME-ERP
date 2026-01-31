package com.erp.service;

public interface CoreService<R, S, I> {
  S getById(I id);

  S save(R request);

  S update(I id, R request);

  void deleteById(I id);
}
