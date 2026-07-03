package com.geely.os.car;

public interface IGlyCar {
    int getIntProperty(int propertyId, int areaId);
    boolean setIntProperty(int propertyId, int areaId, int value);

    /** Варианты без зоны — для свойств с одной зоной (например подогрев руля). */
    int getIntProperty(int propertyId);
    boolean setIntProperty(int propertyId, int value);

    /** Чтение значения сенсора (например уровень топлива в %). */
    float getSensorValue(int sensorType);

    /**
     * Ёмкость топливного бака в литрах из данных машины (ICarInfo). 0, если машина
     * не сообщает значение — тогда используется запасная константа.
     */
    float getFuelTankCapacityLiters();

    /** true, если функция сейчас реально доступна (FunctionStatus.active). */
    boolean isFunctionActive(int propertyId);

    /** Подписка на изменения значений указанных свойств. */
    boolean registerValueWatcher(int[] propertyIds, GlyCarValueWatcher watcher);
    /** Снять ранее зарегистрированную подписку. */
    void unregisterValueWatcher();

    /** Подписка на изменения значений указанных сенсоров (например уровень топлива). */
    boolean registerSensorWatcher(int[] sensorTypes, GlyCarSensorWatcher watcher);
    /** Снять ранее зарегистрированную подписку на сенсоры. */
    void unregisterSensorWatcher();

    void disconnect();
}
