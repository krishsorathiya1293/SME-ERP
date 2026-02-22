package com.erp.wrappers;

public sealed interface CreateResult<S> permits CreateOne, CreateMany, CreateNone {}
