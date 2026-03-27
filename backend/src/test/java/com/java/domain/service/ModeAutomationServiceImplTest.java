// package com.java.domain.service;

// import static org.junit.jupiter.api.Assertions.assertNotNull;
// import static org.mockito.Mockito.mock;

// import java.util.List;

// import org.junit.jupiter.api.Test;

// import com.java.domain.provider.DefaultDeviceTargetResolver;
// import com.java.domain.provider.DeviceSubtypeResolver;
// import com.java.domain.provider.DeviceTargetResolver;
// import com.java.persistence.repo.ConfigRepository;
// import com.java.persistence.repo.DeviceRepository;

// class ModeAutomationServiceImplTest {

//     private final DeviceRepository deviceRepository = mock(DeviceRepository.class);
//     private final ConfigRepository configRepository = mock(ConfigRepository.class);
//     private final HomeModeResolver homeModeResolver = mock(HomeModeResolver.class);
//     private final SensorSnapshotService sensorSnapshotService = mock(SensorSnapshotService.class);
//     private final AutoControlService autoControlService = mock(AutoControlService.class);
//     private final ManualHoldQueryService manualHoldQueryService = mock(ManualHoldQueryService.class);
//     private final DeviceRuntimeStateService deviceRuntimeStateService = mock(DeviceRuntimeStateService.class);
//     private final AutomationCooldownService automationCooldownService = mock(AutomationCooldownService.class);
//     private final DeviceSubtypeResolver deviceSubtypeResolver = mock(DeviceSubtypeResolver.class);

//     private final DeviceTargetResolver deviceTargetResolver = new DefaultDeviceTargetResolver();
//     private final DeviceTargetPolicy deviceTargetPolicy = new DeviceTargetPolicy(
//             deviceTargetResolver,
//             deviceSubtypeResolver,
//             List.of(),
//             List.of()
//     );

//     private final FanAutomationPolicy fanAutomationPolicy = new FanAutomationPolicy();
//     private final LightAutomationPolicy lightAutomationPolicy = new LightAutomationPolicy();

//     @Test
//     void constructor_createsServiceSuccessfully() {
//         ModeAutomationServiceImpl service = new ModeAutomationServiceImpl(
//                 deviceRepository,
//                 configRepository,
//                 homeModeResolver,
//                 sensorSnapshotService,
//                 fanAutomationPolicy,
//                 lightAutomationPolicy,
//                 autoControlService,
//                 manualHoldQueryService,
//                 deviceRuntimeStateService,
//                 automationCooldownService,
//                 deviceTargetPolicy
//         );

//         assertNotNull(service);
//     }
// }