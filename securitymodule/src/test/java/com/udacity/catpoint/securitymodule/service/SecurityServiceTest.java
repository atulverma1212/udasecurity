package com.udacity.catpoint.securitymodule.service;

import com.udacity.catpoint.imagemodule.service.ImageService;
import com.udacity.catpoint.securitymodule.TestUtils;
import com.udacity.catpoint.securitymodule.data.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
//@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SecurityServiceTest {

    private SecurityService securityService;
    private Sensor sensor;

    @Mock
    private ImageService imageService;

    @Mock
    private SecurityRepository securityRepository;

    @BeforeEach
    void setUp() {
        securityService = new SecurityService(securityRepository, imageService);
        sensor = new Sensor(UUID.randomUUID().toString(), SensorType.DOOR);
    }

    // Test 1: If alarm is armed and a sensor becomes activated, put the system into pending alarm status.
    @ParameterizedTest(name = "[{index}] Alarm: {0}")
    @DisplayName("When Alarm: armed, Sensor: activated, then set Status: pending")
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME","ARMED_AWAY"})
    void armedAlarm_activatedSensor_putStatusToPending(ArmingStatus armingStatus) {
        when(securityService.getArmingStatus()).thenReturn(armingStatus);
        when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    // Test 2: If alarm is armed and a sensor becomes activated and the system is already pending alarm, set the alarm status to alarm.
    @ParameterizedTest(name = "[{index}] Alarm: {0}")
    @DisplayName("When Alarm: armed, Sensor: activated, System: pendingAlarm, then set Status: Alarm")
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME","ARMED_AWAY"})
    void armedAlarm_activatedSensor_statusPending_putStatusToAlarm(ArmingStatus armingStatus) {
        when(securityService.getArmingStatus()).thenReturn(armingStatus);
        when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    // Test 3: If pending alarm and all sensors are inactive, return to no alarm state.
    @Test
    @DisplayName("When Alarm: pending, Sensor: inActive, then set system Status: NO_ALARM")
    void pendingAlarm_inactiveSensor_setStatusNoAlarm() {
        sensor.setActive(true);
        when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor, false);
        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    // Test 4: If alarm is active, change in sensor state should not affect the alarm state.
    @Test
    @DisplayName("When Alarm: active, then change in sensor state doNot affect AlarmState")
    void activeAlarm_sensorStateDontAffectAlarmState() {
        when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        securityService.changeSensorActivationStatus(sensor, false);
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));

        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    // Test 5: If a sensor is activated while already active and the system is in pending state, change it to alarm state.
    @Test
    @DisplayName("When Sensor: activate, previousSensorState: Active, system: PendingAlarm, then put system to Alarm State")
    void activatedSensor_prevStateActive_systemPendingAlarm_putSystemToAlarm() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        sensor.setActive(true);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    // Test 6: If a sensor is deactivated while already inactive, make no changes to the alarm state.
    @ParameterizedTest(name = "[{index}] AlarmState: {0}")
    @EnumSource(value = AlarmStatus.class, names = {"ALARM", "NO_ALARM", "PENDING_ALARM"})
    @DisplayName("When Sensor: deactivated while already Inactive, then AlarmState is not affected")
    void sensorDeactivatedWhileAlreadyInactive_alarmStateDontChange(AlarmStatus alarmStatus) {
        Set<Sensor> sensors = TestUtils.getSensors(4, false);
        lenient().when(securityRepository.getSensors()).thenReturn(sensors);
        lenient().when(securityRepository.getAlarmStatus()).thenReturn(alarmStatus);
        sensor.setActive(false);
        securityService.changeSensorActivationStatus(sensor, false);
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    // Test 7: If the image service identifies an image containing a cat while the system is armed-home, put the system into alarm status.
    @Test
    @DisplayName("When Cat detected while System: Armed_Home, then put system to Alarm State")
    void catDetected_armedHome_putSystemtoAlarm() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(imageService.imageContainsCat(any(BufferedImage.class), anyFloat())).thenReturn(true);
        securityService.processImage(mock(BufferedImage.class));
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    // Test 8: If the image service identifies an image that does not contain a cat, change the status to no alarm as long as the sensors are not active.
    @Test
    @DisplayName("When cat not detected, then set status to NoAlarm")
    void catNotDetected_putStatusToNoAlarm() {
        Set<Sensor> sensors = TestUtils.getSensors(3, false);
        when(securityRepository.getSensors()).thenReturn(sensors);
        when(imageService.imageContainsCat(any(BufferedImage.class), anyFloat())).thenReturn(false);

        securityService.processImage(mock(BufferedImage.class));
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    // Test 9: If the system is disarmed, set the status to no alarm.
    @Test
    @DisplayName("When system: disarmed, then set status: NoAlarm")
    void disarmedSystem_setStatusNoAlarm() {
        securityService.setArmingStatus(ArmingStatus.DISARMED);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    // Test 10: If the system is armed, reset all sensors to inactive.
    @ParameterizedTest(name = "[{index}] ArmingStatus: {0}")
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME","ARMED_AWAY"})
    @DisplayName("When System: Armed, then reset All Sensors")
    void armedSystem_resetSensors(ArmingStatus armingStatus) {
        Set<Sensor> sensors = TestUtils.getSensors(4, true);
        when(securityRepository.getSensors()).thenReturn(sensors);
        securityService.setArmingStatus(armingStatus);
        securityService.getSensors().forEach(sensor -> assertFalse(sensor.getActive()));
    }

    // Test 11: If the system is armed-home while the camera shows a cat, set the alarm status to alarm.
    @Test
    @DisplayName("When system: armed_home, cat detected, then set alarm status: ALARM")
    void systemArmedHome_CatDetected_setStatusToAlarm() {
        when(imageService.imageContainsCat(any(BufferedImage.class), anyFloat())).thenReturn(true);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        securityService.processImage(mock(BufferedImage.class));

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    /*  ---------------  Additional Test Cases  --------------------- */

    @ParameterizedTest
    @DisplayName("When System: disarmed, sensor: activated: then Arming State is Unchanged")
    @EnumSource(value = AlarmStatus.class, names = {"NO_ALARM", "PENDING_ALARM"})
    void disarmedSystem_sensorActivated_ArmingStateUnchanged(AlarmStatus status) {
        when(securityRepository.getAlarmStatus()).thenReturn(status);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository, never()).setArmingStatus(ArmingStatus.DISARMED);
    }

    @Test
    @DisplayName("When Sensor: deactivated, prevState: active, then System moves from PendingAlarm to NoAlarm")
    void deactivatedSensor_prevActive_pendingAlarm_putSystemToNoAlarm() {
        sensor.setActive(true);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor,  false);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    // Test 10: If the system is armed, reset all sensors to inactive.
    @Test
    @DisplayName("When System: Armed; then reset All Sensors")
    void armedSystem_catDetected_resetSensors() {
        Set<Sensor> sensors = TestUtils.getSensors(4, true);
        when(securityRepository.getSensors()).thenReturn(sensors);
        when(imageService.imageContainsCat(any(BufferedImage.class), anyFloat())).thenReturn(true);
        securityService.processImage(mock(BufferedImage.class));
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        securityService.getSensors().forEach(sensor -> assertFalse(sensor.getActive()));
    }

    @Test
    @DisplayName("When System: Armed; then reset All Sensors")
    void disarmedSystem_sensorActivated_doNothing() {
        sensor.setActive(true);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        securityService.changeSensorActivationStatus(sensor, false);
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }


}