package com.erp.wrappers;

public record CreateOne<S>(S item) implements CreateResult<S> {}
