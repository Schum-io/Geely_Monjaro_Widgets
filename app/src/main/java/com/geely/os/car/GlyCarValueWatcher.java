package com.geely.os.car;

/** Упрощённый слушатель изменения значения свойства машины. */
public interface GlyCarValueWatcher {
    void onPropertyChanged(int propertyId, int areaId);
}
