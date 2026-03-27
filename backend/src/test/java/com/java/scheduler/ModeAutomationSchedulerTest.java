// package com.java.scheduler;

// import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
// import static org.mockito.Mockito.mock;
// import static org.mockito.Mockito.times;
// import static org.mockito.Mockito.verify;
// import static org.mockito.Mockito.when;

// import java.util.List;

// import org.junit.jupiter.api.Test;

// import com.java.domain.DeviceClass;
// import com.java.domain.provider.DefaultDeviceTargetResolver;
// import com.java.domain.provider.DeviceSubtypeResolver;
// import com.java.domain.provider.DeviceTargetResolver;
// import com.java.domain.service.AutoControlService;
// import com.java.domain.service.AutomationCooldownService;
// import com.java.domain.service.DeviceRuntimeStateService;
// import com.java.domain.service.DeviceTargetPolicy;
// import com.java.domain.service.FanAutomationPolicy;
// import com.java.domain.service.HomeModeResolver;
// import com.java.domain.service.LightAutomationPolicy;
// import com.java.domain.service.ManualHoldQueryService;
// import com.java.domain.service.ModeAutomationService;
// import com.java.domain.service.ModeAutomationServiceImpl;
// import com.java.domain.service.SensorSnapshotService;
// import com.java.persistence.entity.DeviceEntity;
// import com.java.persistence.entity.HomeEntity;
// import com.java.persistence.repo.ConfigRepository;
// import com.java.persistence.repo.DeviceRepository;

// class ModeAutomationSchedulerTest {

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

//     private final ModeAutomationService modeAutomationService = new ModeAutomationServiceImpl(
//             deviceRepository,
//             configRepository,
//             homeModeResolver,
//             sensorSnapshotService,
//             fanAutomationPolicy,
//             lightAutomationPolicy,
//             autoControlService,
//             manualHoldQueryService,
//             deviceRuntimeStateService,
//             automationCooldownService,
//             deviceTargetPolicy
//     );

//     private final ModeAutomationScheduler scheduler =
//             new ModeAutomationScheduler(deviceRepository, modeAutomationService);

//     @Test
//     void reevaluateHomes_withDevices_doesNotThrow() {
//         DeviceEntity fan = device(10L, 1L, "FAN");

//         when(deviceRepository.findAll()).thenReturn(List.of(fan));
//         when(deviceRepository.findByHomeId(1L)).thenReturn(List.of(fan));

//         assertDoesNotThrow(() -> scheduler.reevaluateHomes());

//         verify(deviceRepository, times(1)).findAll();
//     }

//     @Test
//     void reevaluateHomes_withNoDevices_doesNotThrow() {
//         when(deviceRepository.findAll()).thenReturn(List.of());

//         assertDoesNotThrow(() -> scheduler.reevaluateHomes());

//         verify(deviceRepository, times(1)).findAll();
//     }

//     private static DeviceEntity device(Long deviceId, Long homeId, String subtype) {
//         HomeEntity home = new HomeEntity();
//         home.setId(homeId);

//         DeviceEntity device = new DeviceEntity();
//         device.setId(deviceId);
//         device.setHome(home);
//         device.setSubtype(subtype);
//         device.setDeviceClass(DeviceClass.ACTUATOR);
//         device.setDeviceKey("device-" + deviceId);
//         return device;
//     }
// }