// package com.java.domain.service;

// import static org.assertj.core.api.Assertions.assertThat;
// import static org.mockito.Mockito.mock;
// import static org.mockito.Mockito.when;

// import java.util.List;
// import java.util.Optional;

// import org.junit.jupiter.api.Test;

// import com.java.domain.DeviceClass;
// import com.java.domain.SystemMode;
// import com.java.persistence.entity.DeviceEntity;
// import com.java.persistence.entity.DeviceRuntimeStateEntity;
// import com.java.persistence.repo.DeviceRepository;
// import com.java.persistence.repo.DeviceRuntimeStateRepository;

// class HomeModeResolverTest {

//     private final DeviceRepository deviceRepository = mock(DeviceRepository.class);
//     private final DeviceRuntimeStateRepository runtimeStateRepository = mock(DeviceRuntimeStateRepository.class);
//     private final HomeModeResolver resolver = new HomeModeResolver(deviceRepository, runtimeStateRepository);

//     @Test
//     void invalidControllerModeFallsBackToProvidedMode() {
//         Long homeId = 1L;
//         DeviceEntity controller = controller(10L);
//         DeviceRuntimeStateEntity invalidMode = new DeviceRuntimeStateEntity();
//         invalidMode.setCapabilityCode("MODE");
//         invalidMode.setValueText("broken");

//         when(deviceRepository.findFirstByHomeIdAndDeviceClass(homeId, DeviceClass.CONTROLLER))
//                 .thenReturn(Optional.of(controller));
//         when(runtimeStateRepository.findByIdDeviceId(controller.getId()))
//                 .thenReturn(List.of(invalidMode));

//         assertThat(resolver.resolveHomeMode(homeId, SystemMode.manual)).isEqualTo(SystemMode.manual);
//     }

//     @Test
//     void missingControllerModeDoesNotDefaultToAutoWhenNoFallbackExists() {
//         Long homeId = 2L;
//         DeviceEntity controller = controller(20L);

//         when(deviceRepository.findFirstByHomeIdAndDeviceClass(homeId, DeviceClass.CONTROLLER))
//                 .thenReturn(Optional.of(controller));
//         when(runtimeStateRepository.findByIdDeviceId(controller.getId()))
//                 .thenReturn(List.of());

//         assertThat(resolver.resolveHomeMode(homeId, null)).isNull();
//     }

//     private static DeviceEntity controller(Long id) {
//         DeviceEntity entity = new DeviceEntity();
//         entity.setId(id);
//         entity.setDeviceClass(DeviceClass.CONTROLLER);
//         return entity;
//     }
// }
