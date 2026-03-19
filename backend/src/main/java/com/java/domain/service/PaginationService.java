package com.java.domain.service;

import com.java.controller.dto.PageResponse;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PaginationService {

    public <T> PageResponse<T> paginate(List<T> items, int page, int size) {
        int fromIndex = page * size;
        if (fromIndex >= items.size()) {
            return PageResponse.of(List.of(), page, size, items.size());
        }

        int toIndex = Math.min(fromIndex + size, items.size());
        return PageResponse.of(items.subList(fromIndex, toIndex), page, size, items.size());
    }
}