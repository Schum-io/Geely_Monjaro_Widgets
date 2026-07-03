package com.ecarx.xui.adaptapi.car.base;

public interface ICarInfo {
    /** Ёмкость топливного бака (float). */
    int FLT_INFO_FUEL_CAPACITY = 0x200100;

    float getCarInfoFloat(int infoType);
}
