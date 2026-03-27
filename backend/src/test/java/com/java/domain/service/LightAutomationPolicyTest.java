// package com.java.domain.service;

// import static org.assertj.core.api.Assertions.assertThat;

// import java.util.List;
// import java.util.Map;

// import org.junit.jupiter.api.Test;

// import com.java.domain.SystemMode;
// import com.java.domain.service.dto.AutomationDecision;
// import com.java.persistence.entity.ConfigEntity;
// import com.java.persistence.entity.DeviceRuntimeStateEntity;

// class LightAutomationPolicyTest {

//     private final LightAutomationPolicy policy = new LightAutomationPolicy();

//     @Test
//     void autoModeUsesNormalizedPowerOnDecision() {
//         ConfigEntity config = new ConfigEntity();
//         config.setLlow(30);

//         List<AutomationDecision> decisions = policy.decide(Map.of(), config, 10.0, SystemMode.auto);

//         assertThat(decisions).containsExactly(new AutomationDecision("POWER", "true", "AUTO_LIGHT_LOW"));
//     }

//     @Test
//     void autoModeUsesNormalizedPowerOffDecision() {
//         ConfigEntity config = new ConfigEntity();
//         config.setLhigh(70);

//         List<AutomationDecision> decisions = policy.decide(
//                 Map.of("POWER", powerState(true)),
//                 config,
//                 90.0,
//                 SystemMode.auto
//         );

//         assertThat(decisions).containsExactly(new AutomationDecision("POWER", "false", "AUTO_LIGHT_HIGH"));
//     }

//     @Test
//     void sleepAndAwayForcePowerOffUsingCapabilityKey() {
//         ConfigEntity config = new ConfigEntity();

//         assertThat(policy.decide(Map.of("POWER", powerState(true)), config, null, SystemMode.sleep))
//                 .containsExactly(new AutomationDecision("POWER", "false", "LIGHT_MODE_FORCE_OFF"));

//         assertThat(policy.decide(Map.of("POWER", powerState(true)), config, null, SystemMode.away))
//                 .containsExactly(new AutomationDecision("POWER", "false", "LIGHT_MODE_FORCE_OFF"));
//     }

//     private static DeviceRuntimeStateEntity powerState(boolean value) {
//         DeviceRuntimeStateEntity entity = new DeviceRuntimeStateEntity();
//         entity.setCapabilityCode("POWER");
//         entity.setValueBoolean(value);
//         return entity;
//     }
// }
