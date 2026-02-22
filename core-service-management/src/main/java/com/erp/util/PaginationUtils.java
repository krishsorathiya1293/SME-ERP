package com.erp.util;

import java.util.Optional;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PaginationUtils {

  public static PageRequest getPageRequest(
      Optional<Integer> page,
      Optional<Integer> size,
      Optional<String> sortDirection,
      Optional<String> sortBy) {
    return PageRequest.of(
        page.orElse(0),
        size.orElse(10),
        Sort.by(
            Sort.Direction.valueOf(sortDirection.orElse(String.valueOf(Sort.Direction.DESC))),
            sortBy.orElse("createdAt")));
  }
}
