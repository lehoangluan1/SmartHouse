// package com.java.domain.service;

// import static org.assertj.core.api.Assertions.assertThat;
// import static org.mockito.Mockito.mock;
// import static org.mockito.Mockito.when;

// import java.time.OffsetDateTime;
// import java.util.Map;
// import java.util.Optional;

// import org.junit.jupiter.api.Test;

// import com.java.domain.provider.DefaultDeviceTargetResolver;
// import com.java.domain.provider.DeviceSubtypeResolver;
// import com.java.domain.provider.DeviceTargetResolver;
// import com.java.persistence.entity.ConfigEntity;
// import com.java.persistence.entity.DeviceRuntimeStateEntity;
// import com.java.persistence.repo.ConfigRepository;

// class AutomationCooldownServiceTest {

//     private final ConfigRepository configRepository = mock(ConfigRepository.class);
//     private final DeviceTargetResolver targetResolver = new DefaultDeviceTargetResolver();
//     private final DeviceSubtypeResolver subtypeResolver = mock(DeviceSubtypeResolver.class);
//     private final DeviceTargetPolicy deviceTargetPolicy = new DeviceTargetPolicy(
//             targetResolver,
//             subtypeResolver,
//             java.util.List.of(),
//             java.util.List.of()
//     );
//     private final AutomationCooldownService service =
//             new AutomationCooldownService(configRepository, deviceTargetPolicy);

//     @Test
//     void lightAliasResolvesToPowerCooldownState() {
//         ConfigEntity config = new ConfigEntity();
//         config.setKMinutes(5);

//         DeviceRuntimeStateEntity powerState = new DeviceRuntimeStateEntity();
//         powerState.setCapabilityCode("POWER");
//         powerState.setUpdatedAt(OffsetDateTime.now().minusMinutes(1));

//         when(configRepository.findFirstByHomeIdOrderByUpdatedAtDesc(1L))
//                 .thenReturn(Optional.of(config));

//         assertThat(service.isCoolingDown(1L, Map.of("POWER", powerState), "light")).isTrue();
//     }

//     @Test
//     void expiredStateIsNotCoolingDown() {
//         ConfigEntity config = new ConfigEntity();
//         config.setKMinutes(1);

//         DeviceRuntimeStateEntity powerState = new DeviceRuntimeStateEntity();
//         powerState.setCapabilityCode("POWER");
//         powerState.setUpdatedAt(OffsetDateTime.now().minusMinutes(5));

//         when(configRepository.findFirstByHomeIdOrderByUpdatedAtDesc(1L))
//                 .thenReturn(Optional.of(config));

//         assertThat(service.isCoolingDown(1L, Map.of("POWER", powerState), "POWER")).isFalse();
//     }
// }
