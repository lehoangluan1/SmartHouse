package com.java.domain.service;

import com.java.controller.dto.AuditEventItem;
import java.util.Locale;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class EventKeywordSpecification {

    private final String keyword;
    private final AuditSearchableTextExtractor extractor;

    public boolean isSatisfiedBy(AuditEventItem item) {
        String normalizedKeyword = normalize(keyword);
        if (normalizedKeyword.isBlank()) {
            return true;
        }

        String haystack = String.join(" ",
                extractor.extract(item.getType()),
                extractor.extract(item.getCategory()),
                extractor.extract(item.getDeviceName()),
                extractor.extract(item.getUsername()),
                extractor.extract(item.getFromState()),
                extractor.extract(item.getToState()),
                extractor.extract(item.getDetails()),
                extractor.extract(item.getStatus()),
                extractor.extract(item.getMethod())
        );

        return haystack.contains(normalizedKeyword);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}