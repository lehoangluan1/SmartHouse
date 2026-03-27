// package com.java.domain.service;

// import static org.assertj.core.api.Assertions.assertThat;
// import org.junit.jupiter.api.Test;
// import static org.mockito.ArgumentMatchers.any;
// import static org.mockito.ArgumentMatchers.anyLong;
// import static org.mockito.ArgumentMatchers.eq;
// import static org.mockito.Mockito.mock;
// import static org.mockito.Mockito.never;
// import static org.mockito.Mockito.verify;
// import static org.mockito.Mockito.when;

// import com.java.domain.CommandStatus;
// import com.java.persistence.entity.ControlCommandEntity;
// import com.java.persistence.entity.DeviceEntity;
// import com.java.persistence.entity.HomeEntity;
// import com.java.persistence.repo.ControlCommandRepository;

// class AutoControlServiceTest {

//     private final ControlCommandRepository controlCommandRepository = mock(ControlCommandRepository.class);
//     private final DeviceTargetPolicy deviceTargetPolicy = mock(DeviceTargetPolicy.class);
//     private final DeviceRuntimeStateService deviceRuntimeStateService = mock(DeviceRuntimeStateService.class);
//     private final ControlCommandFactory controlCommandFactory = mock(ControlCommandFactory.class);
//     private final ControlCommandSender controlCommandSender = mock(ControlCommandSender.class);
//     private final ActivityLogService activityLogService = mock(ActivityLogService.class);
//     private final ActivityLogPayloadBuilder activityLogPayloadBuilder = mock(ActivityLogPayloadBuilder.class);

//     private final AutoControlService service = new AutoControlService(
//             controlCommandRepository,
//             deviceTargetPolicy,
//             deviceRuntimeStateService,
//             controlCommandFactory,
//             controlCommandSender,
//             activityLogService,
//             activityLogPayloadBuilder
//     );

//     @Test
//     void execute_updatesRuntimeState_evenWhenCommandSendFails_currentBug() {
//         DeviceEntity device = device(10L, 100L);

//         ControlCommandEntity createdCommand = new ControlCommandEntity();
//         createdCommand.setDevice(device);
//         createdCommand.setTarget("POWER");

//         ControlCommandEntity savedCommand = new ControlCommandEntity();
//         savedCommand.setId(999L);
//         savedCommand.setDevice(device);
//         savedCommand.setTarget("POWER");
//         savedCommand.setStatus(CommandStatus.PENDING);

//         ControlCommandEntity failedCommand = new ControlCommandEntity();
//         failedCommand.setId(999L);
//         failedCommand.setDevice(device);
//         failedCommand.setTarget("POWER");
//         failedCommand.setStatus(CommandStatus.FAILED);

//         DeviceRuntimeStateService.StateWriteResult writeResult =
//                 new DeviceRuntimeStateService.StateWriteResult(
//                         null,
//                         false,
//                         true,
//                         true,
//                         true
//                 );

//         when(deviceTargetPolicy.normalizeTarget("POWER")).thenReturn("POWER");
//         when(deviceTargetPolicy.normalizeValue("POWER", "true")).thenReturn(true);
//         when(deviceRuntimeStateService.hasChanged(device.getId(), "POWER", true)).thenReturn(true);

//         when(controlCommandFactory.createSystem(device, "POWER", true)).thenReturn(createdCommand);
//         when(controlCommandRepository.save(createdCommand)).thenReturn(savedCommand);
//         when(controlCommandSender.sendNow(savedCommand)).thenReturn(failedCommand);

//         when(deviceRuntimeStateService.upsertValueAndRecordHistory(
//                 eq(device.getId()),
//                 eq("POWER"),
//                 eq(true),
//                 eq("AUTO_CONTROL"),
//                 eq(savedCommand.getId()),
//                 eq(null)
//         )).thenReturn(writeResult);

//         when(activityLogPayloadBuilder.controlPayload("POWER", false, true))
//                 .thenReturn(java.util.Map.of("target", "POWER"));

//         boolean result = service.execute(device, "POWER", "true", "AUTO_TEMP_HIGH");

//         assertThat(result).isTrue();

//         verify(controlCommandSender).sendNow(savedCommand);

//         // Đây là hành vi bug hiện tại: gửi FAIL nhưng vẫn update runtime state
//         verify(deviceRuntimeStateService).upsertValueAndRecordHistory(
//                 eq(device.getId()),
//                 eq("POWER"),
//                 eq(true),
//                 eq("AUTO_CONTROL"),
//                 eq(savedCommand.getId()),
//                 eq(null)
//         );

//         verify(activityLogService).log(
//                 eq(100L),
//                 eq(device.getId()),
//                 eq(null),
//                 eq("AUTO_CONTROL"),
//                 eq("AUTO_TEMP_HIGH"),
//                 eq(null),
//                 eq(null),
//                 any()
//         );
//     }

//     @Test
//     void execute_returnsFalse_andDoesNothing_whenStateHasNotChanged() {
//         DeviceEntity device = device(11L, 101L);

//         when(deviceTargetPolicy.normalizeTarget("POWER")).thenReturn("POWER");
//         when(deviceTargetPolicy.normalizeValue("POWER", "true")).thenReturn(true);
//         when(deviceRuntimeStateService.hasChanged(device.getId(), "POWER", true)).thenReturn(false);

//         boolean result = service.execute(device, "POWER", "true", "AUTO_TEMP_HIGH");

//         assertThat(result).isFalse();

//         verify(controlCommandFactory, never()).createSystem(any(), any(), any());
//         verify(controlCommandSender, never()).sendNow(any());
//         verify(deviceRuntimeStateService, never()).upsertValueAndRecordHistory(
//                 anyLong(), any(), any(), any(), any(), any()
//         );
//     }

//     private static DeviceEntity device(Long deviceId, Long homeId) {
//         HomeEntity home = new HomeEntity();
//         home.setId(homeId);

//         DeviceEntity device = new DeviceEntity();
//         device.setId(deviceId);
//         device.setHome(home);
//         device.setDeviceKey("demo-device");

//         return device;
//     }
// }