package com.erp.util;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PageMapper {

    public static <E, D, R> R toResult(
            Page<E> page,
            Function<E, D> mapper,
            Supplier<R> factory,
            BiConsumer<R, List<D>> dataSetter) {
        List<D> content = page.getContent().stream().map(mapper).toList();
        R result = factory.get();
        dataSetter.accept(result, content);
        setPageMetadata(result, page, content.isEmpty());
        return result;
    }

    private static <R> void setPageMetadata(R result, Page<?> page, boolean empty) {
        try {
            Class<?> cls = result.getClass();
            cls.getMethod("setEmpty", Boolean.class).invoke(result, empty);
            cls.getMethod("setFirst", Boolean.class).invoke(result, page.isFirst());
            cls.getMethod("setLast", Boolean.class).invoke(result, page.isLast());
            cls.getMethod("setTotalPages", Integer.class).invoke(result, page.getTotalPages());
            cls.getMethod("setSize", Integer.class).invoke(result, page.getSize());
            cls.getMethod("setTotalElements", Integer.class).invoke(result, (int) page.getTotalElements());
        } catch (Exception e) {
            throw new IllegalStateException("Paginated result DTO missing standard setters", e);
        }
    }
}
