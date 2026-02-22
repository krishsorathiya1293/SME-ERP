package com.erp.wrappers;

import java.util.List;

public record CreateMany<S>(List<S> items) implements CreateResult<S> {}
