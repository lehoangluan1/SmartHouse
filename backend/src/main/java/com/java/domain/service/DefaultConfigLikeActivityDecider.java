package com.java.domain.service;

import com.java.domain.provider.ConfigLikeActivityDecider;
import com.java.persistence.entity.ActivityLogEntity;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DefaultConfigLikeActivityDecider implements ConfigLikeActivityDecider {

    private final AuditValueFormatter auditValueFormatter;

    @Override
    public boolean isConfigLike(ActivityLogEntity entity) {
        String text = (
                auditValueFormatter.safe(entity.getAction()) + " " +
                auditValueFormatter.safe(entity.getMethod()) + " " +
                auditValueFormatter.safe(auditValueFormatter.displayRaw(entity.getDetail())) + " " +
                auditValueFormatter.safe(auditValueFormatter.displayRaw(entity.getOldValue())) + " " +
                auditValueFormatter.safe(auditValueFormatter.displayRaw(entity.getNewValue()))
        ).toLowerCase(Locale.ROOT);

        return text.contains("config")
                || text.contains("setting")
                || text.contains("profile")
                || text.contains("schedule")
                || text.contains("threshold");
    }
}