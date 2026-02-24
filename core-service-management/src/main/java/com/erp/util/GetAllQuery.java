package com.erp.util;

import java.util.Optional;

public record GetAllQuery<F>(
    Optional<F> filter,
    Optional<String> search,
    Optional<Integer> page,
    Optional<Integer> size,
    Optional<String> sortBy,
    Optional<String> direction) {

  public static <F> GetAllQuery<F> of(Optional<String> search) {
    return new GetAllQuery<>(
        Optional.empty(),
        search,
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty());
  }

  public static <F> GetAllQuery<F> of(Optional<F> filter, Optional<String> search) {
    return new GetAllQuery<>(
        filter, search, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
  }

  public static <F> GetAllQuery<F> of(
      Optional<F> filter,
      Optional<String> search,
      Optional<Integer> page,
      Optional<Integer> size,
      Optional<String> sortBy,
      Optional<String> direction) {
    return new GetAllQuery<>(filter, search, page, size, sortBy, direction);
  }
}
