package com.geely.os.car;

public interface IGlyCar {
    int getIntProperty(int propertyId, int areaId);
    boolean setIntProperty(int propertyId, int areaId, int value);

    /** Варианты без зоны — для свойств с одной зоной (например подогрев руля). */
    int getIntProperty(int propertyId);
    boolean setIntProperty(int propertyId, int value);

    /** Чтение значения сенсора (например уровень топлива в %). */
    float getSensorValue(int sensorType);

    /** Подписка на изменения значений указанных свойств. */
    boolean registerValueWatcher(int[] propertyIds, GlyCarValueWatcher watcher);
    /** Снять ранее зарегистрированную подписку. */
    void unregisterValueWatcher();

    void disconnect();
}
