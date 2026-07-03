package com.ecarx.xui.adaptapi.car.sensor;

public interface ISensor {
    float getSensorLatestValue(int sensorType);

    /** Подписка на изменения значения сенсора. Сигнатуры совпадают с рантаймом ECARX. */
    boolean registerListener(ISensorListener listener, int sensorType);
    void unregisterListener(ISensorListener listener);

    interface ISensorListener {
        void onSensorValueChanged(int sensorType, float value);
        void onSensorEventChanged(int sensorType, int event);
        void onSensorSupportChanged(int sensorType, com.ecarx.xui.adaptapi.FunctionStatus status);
    }
}
