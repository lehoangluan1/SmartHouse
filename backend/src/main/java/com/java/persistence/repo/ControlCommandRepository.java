package com.java.persistence.repo;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.java.domain.CommandStatus;
import com.java.persistence.entity.ControlCommandEntity;

public interface ControlCommandRepository extends JpaRepository<ControlCommandEntity, Long> {

    boolean existsByDeviceIdAndTargetAndValueBooleanAndStatusIn(
            Long deviceId,
            String target,
            Boolean valueBoolean,
            Collection<CommandStatus> statuses
    );

    boolean existsByDeviceIdAndTargetAndValueNumberAndStatusIn(
            Long deviceId,
            String target,
            Double valueNumber,
            Collection<CommandStatus> statuses
    );

    boolean existsByDeviceIdAndTargetAndValueTextAndStatusIn(
            Long deviceId,
            String target,
            String valueText,
            Collection<CommandStatus> statuses
    );

    List<ControlCommandEntity> findByDeviceHomeIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            Long homeId,
            OffsetDateTime from,
            OffsetDateTime to
    );

    default boolean existsPendingSameBoolean(Long deviceId, String target, Boolean valueBoolean) {
        return existsByDeviceIdAndTargetAndValueBooleanAndStatusIn(
                deviceId,
                target,
                valueBoolean,
                List.of(CommandStatus.PENDING, CommandStatus.SENT)
        );
    }

    default boolean existsPendingSameNumber(Long deviceId, String target, Double valueNumber) {
        return existsByDeviceIdAndTargetAndValueNumberAndStatusIn(
                deviceId,
                target,
                valueNumber,
                List.of(CommandStatus.PENDING, CommandStatus.SENT)
        );
    }

    default boolean existsPendingSameText(Long deviceId, String target, String valueText) {
        return existsByDeviceIdAndTargetAndValueTextAndStatusIn(
                deviceId,
                target,
                valueText,
                List.of(CommandStatus.PENDING, CommandStatus.SENT)
        );
    }

    ControlCommandEntity findFirstByDeviceIdAndStatusOrderByCreatedAtAsc(
            Long deviceId,
            CommandStatus status
    );

    default ControlCommandEntity findNextPending(Long deviceId) {
        return findFirstByDeviceIdAndStatusOrderByCreatedAtAsc(deviceId, CommandStatus.PENDING);
    }
}