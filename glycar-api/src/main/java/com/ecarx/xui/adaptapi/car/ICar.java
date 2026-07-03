package com.ecarx.xui.adaptapi.car;

import com.ecarx.xui.adaptapi.car.base.ICarFunction;
import com.ecarx.xui.adaptapi.car.base.ICarInfo;
import com.ecarx.xui.adaptapi.car.sensor.ISensor;

public interface ICar {
    ICarFunction getICarFunction();
    ISensor getSensorManager();
    ICarInfo getCarInfoManager();
}
