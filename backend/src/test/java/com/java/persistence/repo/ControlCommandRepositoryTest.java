package com.java.persistence.repo;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;

import com.java.persistence.entity.ControlCommandEntity;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

class ControlCommandRepositoryTest {

    @Test
    void nextDeliverableQueryLimitsToOneOldestClaimableCommand() throws Exception {
        Method method = ControlCommandRepository.class.getMethod(
                "findNextDeliverableForUpdate",
                Long.class,
                java.time.OffsetDateTime.class
        );

        Query query = method.getAnnotation(Query.class);

        assertThat(query).isNotNull();
        assertThat(query.nativeQuery()).isTrue();
        assertThat(query.value()).contains("ORDER BY created_at ASC");
        assertThat(query.value()).contains("LIMIT 1");
        assertThat(query.value()).contains("FOR UPDATE SKIP LOCKED");
    }

    @Test
    void repositoryExposesDerivedFindFirstQueryForCompatibility() throws Exception {
        Method method = ControlCommandRepository.class.getMethod(
                "findFirstByDeviceIdAndStatusInOrderByCreatedAtAsc",
                Long.class,
                java.util.Collection.class
        );

        assertThat(method.getReturnType()).isEqualTo(java.util.Optional.class);
        assertThat(method.getGenericReturnType().getTypeName()).contains(ControlCommandEntity.class.getSimpleName());
    }
}
