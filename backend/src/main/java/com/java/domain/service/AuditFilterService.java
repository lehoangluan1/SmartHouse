package com.java.domain.service;

import com.java.controller.dto.AuditConfigChangeItem;
import com.java.controller.dto.AuditEventItem;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuditFilterService {

    private final AuditSearchableTextExtractor extractor;

    public List<AuditConfigChangeItem> filterConfigChanges(
            List<AuditConfigChangeItem> items,
            String keyword
    ) {
        ConfigKeywordSpecification specification =
                new ConfigKeywordSpecification(keyword, extractor);

        return items.stream()
                .filter(specification::isSatisfiedBy)
                .toList();
    }

    public List<AuditEventItem> filterEvents(
            List<AuditEventItem> items,
            String keyword,
            String category
    ) {
        EventCategorySpecification categorySpecification =
                new EventCategorySpecification(category);

        EventKeywordSpecification keywordSpecification =
                new EventKeywordSpecification(keyword, extractor);

        return items.stream()
                .filter(categorySpecification::isSatisfiedBy)
                .filter(keywordSpecification::isSatisfiedBy)
                .toList();
    }
}