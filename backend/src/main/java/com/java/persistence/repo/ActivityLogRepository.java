package com.java.persistence.repo;

import com.java.persistence.entity.ActivityLogEntity;
import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ActivityLogRepository extends JpaRepository<ActivityLogEntity, Long> {

    String CONFIG_LIKE_SQL = """
            (
              lower(concat_ws(' ',
                a.action,
                a.method,
                a.detail::text,
                a.old_value::text,
                a.new_value::text
              )) like '%config%'
              or lower(concat_ws(' ',
                a.action,
                a.method,
                a.detail::text,
                a.old_value::text,
                a.new_value::text
              )) like '%setting%'
              or lower(concat_ws(' ',
                a.action,
                a.method,
                a.detail::text,
                a.old_value::text,
                a.new_value::text
              )) like '%profile%'
              or lower(concat_ws(' ',
                a.action,
                a.method,
                a.detail::text,
                a.old_value::text,
                a.new_value::text
              )) like '%schedule%'
              or lower(concat_ws(' ',
                a.action,
                a.method,
                a.detail::text,
                a.old_value::text,
                a.new_value::text
              )) like '%threshold%'
            )
            """;

    String SEARCH_SQL = """
            (
              :keyword is null
              or lower(concat_ws(' ',
                a.action,
                a.method,
                d.name,
                u.username,
                a.detail::text,
                a.old_value::text,
                a.new_value::text
              )) like concat('%', lower(:keyword), '%')
            )
            """;

    String EVENT_CATEGORY_SQL = """
            (
              :category = 'all'
              or (:category = 'alerts' and (
                upper(a.action) in ('ALERT_ACTIVE', 'ALERT_ACK', 'ALERT_RESOLVED')
                or (upper(a.method) = 'OBSERVER' and upper(a.action) like 'ALERT_%')
              ))
              or (:category = 'device' and (
                upper(a.action) in ('MANUAL_CONTROL', 'AUTO_CONTROL', 'INGEST_TELEMETRY')
                or upper(a.method) in ('APP', 'DEVICE')
                or (a.device_id is not null and upper(a.action) not in (
                  'ALERT_ACTIVE',
                  'ALERT_ACK',
                  'ALERT_RESOLVED',
                  'INIT_DEVICE',
                  'UPSERT_CONFIG',
                  'UPDATE_CONFIG',
                  'ACTIVATE_CONFIG',
                  'DELETE_CONFIG',
                  'MANUAL_HOLD_STARTED',
                  'MANUAL_HOLD_RESTORED',
                  'MANUAL_HOLD_RESTORE',
                  'MANUAL_HOLD_CLEARED'
                ))
              ))
              or (:category = 'system' and (
                upper(a.action) in (
                  'INIT_DEVICE',
                  'UPSERT_CONFIG',
                  'UPDATE_CONFIG',
                  'ACTIVATE_CONFIG',
                  'DELETE_CONFIG',
                  'MANUAL_HOLD_STARTED',
                  'MANUAL_HOLD_RESTORED',
                  'MANUAL_HOLD_RESTORE',
                  'MANUAL_HOLD_CLEARED'
                )
                or upper(a.method) = 'SYSTEM'
                or a.device_id is null
              ))
            )
            """;

    List<ActivityLogEntity> findByHome_IdAndCreatedAtBetweenOrderByCreatedAtDesc(
            Long homeId,
            OffsetDateTime from,
            OffsetDateTime to
    );

    List<ActivityLogEntity> findByDevice_IdOrderByCreatedAtDesc(Long deviceId);

    @Query(
            value = """
                select a.*
                from public.activity_logs a
                left join public.devices d on d.id = a.device_id
                left join public.users u on u.id = a.user_id
                where a.home_id = :homeId
                  and a.created_at between :from and :to
                  and """ + CONFIG_LIKE_SQL + """
                  and """ + SEARCH_SQL + """
                order by a.created_at desc, a.id desc
                """,
            countQuery = """
                select count(*)
                from public.activity_logs a
                left join public.devices d on d.id = a.device_id
                left join public.users u on u.id = a.user_id
                where a.home_id = :homeId
                  and a.created_at between :from and :to
                  and """ + CONFIG_LIKE_SQL + """
                  and """ + SEARCH_SQL,
            nativeQuery = true
    )
    Page<ActivityLogEntity> findAuditConfigChanges(
            @Param("homeId") Long homeId,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    @Query(
            value = """
                select a.*
                from public.activity_logs a
                left join public.devices d on d.id = a.device_id
                left join public.users u on u.id = a.user_id
                where a.home_id = :homeId
                  and a.created_at between :from and :to
                  and not """ + CONFIG_LIKE_SQL + """
                  and upper(a.action) <> 'INGEST_TELEMETRY_NO_STATE_CHANGE'
                  and """ + EVENT_CATEGORY_SQL + """
                  and """ + SEARCH_SQL + """
                order by a.created_at desc, a.id desc
                """,
            countQuery = """
                select count(*)
                from public.activity_logs a
                left join public.devices d on d.id = a.device_id
                left join public.users u on u.id = a.user_id
                where a.home_id = :homeId
                  and a.created_at between :from and :to
                  and not """ + CONFIG_LIKE_SQL + """
                  and upper(a.action) <> 'INGEST_TELEMETRY_NO_STATE_CHANGE'
                  and """ + EVENT_CATEGORY_SQL + """
                  and """ + SEARCH_SQL,
            nativeQuery = true
    )
    Page<ActivityLogEntity> findAuditEvents(
            @Param("homeId") Long homeId,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to,
            @Param("category") String category,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    @Query(
            value = """
                select count(*)
                from public.activity_logs a
                left join public.devices d on d.id = a.device_id
                left join public.users u on u.id = a.user_id
                where a.home_id = :homeId
                  and a.created_at between :from and :to
                  and not """ + CONFIG_LIKE_SQL + """
                  and upper(a.action) <> 'INGEST_TELEMETRY_NO_STATE_CHANGE'
                  and """ + EVENT_CATEGORY_SQL + """
                  and """ + SEARCH_SQL,
            nativeQuery = true
    )
    long countAuditEvents(
            @Param("homeId") Long homeId,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to,
            @Param("category") String category,
            @Param("keyword") String keyword
    );

    @Query("""
        select a
        from ActivityLogEntity a
        where a.device.id = :deviceId
          and a.action in :actions
        order by a.createdAt desc, a.id desc
    """)
    List<ActivityLogEntity> findLatestHoldLogs(
            @Param("deviceId") Long deviceId,
            @Param("actions") List<String> actions
    );

    @Query("""
        select a
        from ActivityLogEntity a
        where a.action = :action
          and a.device is not null
        order by a.createdAt desc, a.id desc
    """)
    List<ActivityLogEntity> findByActionOrderByCreatedAtDesc(@Param("action") String action);

    @Query("""
        select a
        from ActivityLogEntity a
        where (:homeId is null or a.home.id = :homeId)
          and (:deviceId is null or a.device.id = :deviceId)
          and (:actorId is null or a.user.id = :actorId)
          and (:action is null or lower(a.action) = lower(:action))
          and (:entityType is null
               or (:entityType = 'DEVICE' and a.device is not null)
               or (:entityType = 'SYSTEM' and a.device is null))
          and (:from is null or a.createdAt >= :from)
          and (:to is null or a.createdAt <= :to)
          and (:cursorCreatedAt is null
               or a.createdAt < :cursorCreatedAt
               or (a.createdAt = :cursorCreatedAt and a.id < :cursorId))
        order by a.createdAt desc, a.id desc
    """)
    List<ActivityLogEntity> findCursorPage(
            @Param("homeId") Long homeId,
            @Param("deviceId") Long deviceId,
            @Param("actorId") Long actorId,
            @Param("action") String action,
            @Param("entityType") String entityType,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to,
            @Param("cursorCreatedAt") OffsetDateTime cursorCreatedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );
}
