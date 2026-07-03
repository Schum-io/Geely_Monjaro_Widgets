package com.geely.os.car;

/** Упрощённый слушатель изменения значения сенсора машины (например уровень топлива). */
public interface GlyCarSensorWatcher {
    void onSensorChanged(int sensorType, float value);
}
