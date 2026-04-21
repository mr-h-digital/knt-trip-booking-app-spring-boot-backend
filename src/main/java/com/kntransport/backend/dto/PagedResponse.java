package com.kntransport.backend.dto;

import org.springframework.data.domain.Page;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public record PagedResponse<T>(
        List<T> content,
        long totalElements,
        int totalPages,
        int number,
        boolean last
) {
    public static <E, D> PagedResponse<D> from(Page<E> page, Function<E, D> mapper) {
        return new PagedResponse<>(
                page.getContent().stream().map(mapper).collect(Collectors.toList()),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.isLast()
        );
    }
}
