package com.java.controller.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PageResponse<T> {
    private List<T> items;
    private int page;
    private int size;
    private long totalItems;
    private int totalPages;
    private boolean first;
    private boolean last;

    public static <T> PageResponse<T> of(List<T> items, int page, int size, long totalItems) {
        int safeSize = size <= 0 ? 1 : size;
        int totalPages = (int) Math.ceil((double) totalItems / safeSize);

        return PageResponse.<T>builder()
                .items(items)
                .page(page)
                .size(size)
                .totalItems(totalItems)
                .totalPages(totalPages)
                .first(page <= 0)
                .last(totalPages == 0 || page >= totalPages - 1)
                .build();
    }
}