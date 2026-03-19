package com.java.domain.service;

import com.java.controller.dto.AuditConfigChangeItem;
import java.util.Locale;

public class ConfigKeywordSpecification {

    private final String keyword;
    private final AuditSearchableTextExtractor extractor;

    public ConfigKeywordSpecification(String keyword, AuditSearchableTextExtractor extractor) {
        this.keyword = normalize(keyword);
        this.extractor = extractor;
    }

    public boolean isSatisfiedBy(AuditConfigChangeItem item) {
        if (keyword.isBlank()) {
            return true;
        }

        String haystack = String.join(" ",
                extractor.extract(item.getUsername()),
                extractor.extract(item.getDeviceName()),
                extractor.extract(item.getPrevConfig()),
                extractor.extract(item.getNewConfig()),
                extractor.extract(item.getReason())
        );

        return haystack.contains(keyword);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}