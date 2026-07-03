package com.ecarx.xui.adaptapi.car.base;

import com.ecarx.xui.adaptapi.FunctionStatus;

public interface ICarFunction {
    int getFunctionValue(int propertyId);
    int getFunctionValue(int propertyId, int areaId);
    boolean setFunctionValue(int propertyId, int value);
    boolean setFunctionValue(int propertyId, int areaId, int value);

    FunctionStatus isFunctionSupported(int propertyId);

    boolean registerFunctionValueWatcher(int[] propertyIds, IFunctionValueWatcher watcher);
    boolean unregisterFunctionValueWatcher(IFunctionValueWatcher watcher);

    interface IFunctionValueWatcher {
        void onFunctionValueChanged(int propertyId, int areaId, int value);
        void onCustomizeFunctionValueChanged(int propertyId, int areaId, float value);
        void onFunctionChanged(int propertyId);
        void onSupportedFunctionStatusChanged(int propertyId, int areaId, FunctionStatus status);
        void onSupportedFunctionValueChanged(int propertyId, int[] supportedValues);
    }
}
