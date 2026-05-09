package com.java.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import com.java.domain.CommandStatus;
import com.java.eventing.DomainEventBus;
import com.java.mapper.ControlCommandMapper;
import com.java.persistence.entity.ControlCommandEntity;
import com.java.persistence.entity.DeviceEntity;
import com.java.persistence.entity.HomeEntity;
import com.java.persistence.repo.ControlCommandRepository;
import com.java.persistence.repo.DeviceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ControlCommandServiceTest {

    @Mock
    private DeviceRepository deviceRepo;
    @Mock
    private ControlCommandRepository cmdRepo;
    @Mock
    private DomainEventBus bus;
    @Mock
    private DeviceTargetPolicy deviceTargetPolicy;

    private CapabilityValueSupport valueSupport;
    private ControlCommandService service;

    @BeforeEach
    void setUp() {
        valueSupport = new CapabilityValueSupport();
        service = new ControlCommandService(
                deviceRepo,
                cmdRepo,
                bus,
                deviceTargetPolicy,
                new ControlCommandMapper(valueSupport),
                new ControlCommandFactory(valueSupport),
                valueSupport,
                2000,
                15000
        );
    }

    @Test
    void requestCommandCreatesPendingCommand() {
        DeviceEntity device = device("ohstem-fan-ctrl-01");
        when(deviceRepo.findById(2L)).thenReturn(Optional.of(device));
        when(deviceTargetPolicy.normalizeTarget("power")).thenReturn("POWER");
        when(deviceTargetPolicy.normalizeValue("POWER", "on")).thenReturn(true);
        when(cmdRepo.existsPendingSameBoolean(2L, "POWER", true)).thenReturn(false);
        when(cmdRepo.save(any(ControlCommandEntity.class))).thenAnswer(invocation -> {
            ControlCommandEntity command = invocation.getArgument(0);
            command.setId(10L);
            return command;
        });

        service.requestCommand(2L, "power", "on", "manual");

        ArgumentCaptor<ControlCommandEntity> captor = ArgumentCaptor.forClass(ControlCommandEntity.class);
        verify(cmdRepo).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(CommandStatus.PENDING);
        assertThat(captor.getValue().getSentAt()).isNull();
    }

    @Test
    void getNextCommandClaimsPendingAsSent() {
        DeviceEntity device = device("ohstem-fan-ctrl-01");
        ControlCommandEntity command = command(device, CommandStatus.PENDING);
        when(deviceRepo.findByDeviceKey("ohstem-fan-ctrl-01")).thenReturn(Optional.of(device));
        when(cmdRepo.findNextDeliverableForUpdate(eq(2L), any(OffsetDateTime.class)))
                .thenReturn(Optional.of(command));
        when(cmdRepo.save(command)).thenReturn(command);

        var response = service.getNextCommandImmediate("ohstem-fan-ctrl-01");

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(99L);
        assertThat(command.getStatus()).isEqualTo(CommandStatus.SENT);
        assertThat(command.getSentAt()).isNotNull();
    }

    @Test
    void getNextCommandDoesNotReturnFreshSentCommand() {
        DeviceEntity device = device("ohstem-fan-ctrl-01");
        when(deviceRepo.findByDeviceKey("ohstem-fan-ctrl-01")).thenReturn(Optional.of(device));
        when(cmdRepo.findNextDeliverableForUpdate(eq(2L), any(OffsetDateTime.class)))
                .thenReturn(Optional.empty());

        var response = service.getNextCommandImmediate("ohstem-fan-ctrl-01");

        assertThat(response).isNull();
        verify(cmdRepo, never()).save(any(ControlCommandEntity.class));
    }

    @Test
    void getNextCommandRedeliversExpiredSentCommand() {
        DeviceEntity device = device("ohstem-fan-ctrl-01");
        ControlCommandEntity command = command(device, CommandStatus.SENT);
        command.setSentAt(OffsetDateTime.now().minusSeconds(30));
        OffsetDateTime previousSentAt = command.getSentAt();
        when(deviceRepo.findByDeviceKey("ohstem-fan-ctrl-01")).thenReturn(Optional.of(device));
        when(cmdRepo.findNextDeliverableForUpdate(eq(2L), any(OffsetDateTime.class)))
                .thenReturn(Optional.of(command));
        when(cmdRepo.save(command)).thenReturn(command);

        var response = service.getNextCommandImmediate("ohstem-fan-ctrl-01");

        assertThat(response).isNotNull();
        assertThat(command.getStatus()).isEqualTo(CommandStatus.SENT);
        assertThat(command.getSentAt()).isAfter(previousSentAt);
    }

    @Test
    void ackMovesPendingOrSentToAcked() {
        DeviceEntity device = device("ohstem-fan-ctrl-01");
        ControlCommandEntity pending = command(device, CommandStatus.PENDING);
        when(deviceRepo.findByDeviceKey("ohstem-fan-ctrl-01")).thenReturn(Optional.of(device));
        when(cmdRepo.findById(99L)).thenReturn(Optional.of(pending));

        service.ackCommand("ohstem-fan-ctrl-01", 99L);

        assertThat(pending.getStatus()).isEqualTo(CommandStatus.ACKED);
        assertThat(pending.getSentAt()).isNotNull();
        assertThat(pending.getAckAt()).isNotNull();
        verify(cmdRepo).save(pending);
    }

    @Test
    void batchNextReturnsOneEntryPerRequestedKey() {
        DeviceEntity fan = device("ohstem-fan-ctrl-01");
        DeviceEntity light = device("ohstem-light-ctrl-01");
        light.setId(3L);
        ControlCommandEntity command = command(fan, CommandStatus.PENDING);
        when(deviceRepo.findByDeviceKeyIn(List.of("ohstem-fan-ctrl-01", "ohstem-light-ctrl-01")))
                .thenReturn(List.of(fan, light));
        when(cmdRepo.findNextDeliverableForUpdate(eq(2L), any(OffsetDateTime.class)))
                .thenReturn(Optional.of(command));
        when(cmdRepo.findNextDeliverableForUpdate(eq(3L), any(OffsetDateTime.class)))
                .thenReturn(Optional.empty());
        when(cmdRepo.save(command)).thenReturn(command);

        var responses = service.getNextCommandsImmediate(List.of("ohstem-fan-ctrl-01", "ohstem-light-ctrl-01"));

        assertThat(responses).containsOnlyKeys("ohstem-fan-ctrl-01", "ohstem-light-ctrl-01");
        assertThat(responses.get("ohstem-fan-ctrl-01")).isNotNull();
        assertThat(responses.get("ohstem-light-ctrl-01")).isNull();
    }

    private DeviceEntity device(String key) {
        HomeEntity home = new HomeEntity();
        home.setId(1L);

        DeviceEntity device = new DeviceEntity();
        device.setId(2L);
        device.setDeviceKey(key);
        device.setHome(home);
        return device;
    }

    private ControlCommandEntity command(DeviceEntity device, CommandStatus status) {
        ControlCommandEntity command = new ControlCommandEntity();
        command.setId(99L);
        command.setDevice(device);
        command.setTarget("POWER");
        command.setValueBoolean(true);
        command.setStatus(status);
        return command;
    }
}
