package com.geely.os.car;

import android.content.Context;
import android.util.Log;

import com.ecarx.xui.adaptapi.binder.IConnectable;
import com.ecarx.xui.adaptapi.car.Car;
import com.ecarx.xui.adaptapi.car.ICar;
import com.ecarx.xui.adaptapi.car.base.ICarFunction;
import com.ecarx.xui.adaptapi.car.sensor.ISensor;

public final class GlyCar {

    private static final String TAG = "GlyCar";
    private static final int ZONE_ALL = 0x80000000; // == Integer.MIN_VALUE == -0x80000000

    private GlyCar() {}

    public static IGlyCar create(Context context) {
        return create(context, null);
    }

    public static IGlyCar create(Context context, ConnectionListener listener) {
        ICar car = Car.create(context);
        return new Bridge(car, listener);
    }

    private static final class Bridge implements IGlyCar {

        private final ICar mCar;
        private volatile ICarFunction mCarFunction;
        private volatile ICarFunction.IFunctionValueWatcher mWatcher;

        Bridge(ICar car, ConnectionListener listener) {
            mCar = car;
            if (car instanceof IConnectable) {
                IConnectable connectable = (IConnectable) car;
                connectable.registerConnectWatcher(new IConnectable.IConnectWatcher() {
                    @Override
                    public void onConnected() {
                        mCarFunction = mCar.getICarFunction();
                        Log.d(TAG, "onConnected, carFunction=" + mCarFunction);
                        if (listener != null) listener.onConnected();
                    }

                    @Override
                    public void onDisConnected() {
                        mCarFunction = null;
                        Log.w(TAG, "onDisConnected");
                        if (listener != null) listener.onDisConnected();
                    }
                });
            } else {
                mCarFunction = car.getICarFunction();
                Log.d(TAG, "sync connect, carFunction=" + mCarFunction);
                if (listener != null) listener.onConnected();
            }
        }

        @Override
        public int getIntProperty(int propertyId, int areaId) {
            ICarFunction cf = mCarFunction;
            if (cf == null) {
                Log.w(TAG, "getIntProperty called but carFunction is null");
                return 0;
            }
            return cf.getFunctionValue(propertyId, areaId);
        }

        @Override
        public boolean setIntProperty(int propertyId, int areaId, int value) {
            ICarFunction cf = mCarFunction;
            if (cf == null) {
                Log.w(TAG, "setIntProperty called but carFunction is null");
                return false;
            }
            return cf.setFunctionValue(propertyId, areaId, value);
        }

        @Override
        public int getIntProperty(int propertyId) {
            ICarFunction cf = mCarFunction;
            if (cf == null) {
                Log.w(TAG, "getIntProperty(no-area) called but carFunction is null");
                return 0;
            }
            return cf.getFunctionValue(propertyId);
        }

        @Override
        public boolean setIntProperty(int propertyId, int value) {
            ICarFunction cf = mCarFunction;
            if (cf == null) {
                Log.w(TAG, "setIntProperty(no-area) called but carFunction is null");
                return false;
            }
            return cf.setFunctionValue(propertyId, value);
        }

        @Override
        public float getSensorValue(int sensorType) {
            try {
                ISensor sensor = mCar.getSensorManager();
                if (sensor == null) {
                    Log.w(TAG, "getSensorValue: sensor manager is null");
                    return 0f;
                }
                return sensor.getSensorLatestValue(sensorType);
            } catch (Throwable t) {
                Log.w(TAG, "getSensorValue failed: " + t);
                return 0f;
            }
        }

        @Override
        public boolean registerValueWatcher(int[] propertyIds, final GlyCarValueWatcher watcher) {
            ICarFunction cf = mCarFunction;
            if (cf == null || watcher == null) {
                Log.w(TAG, "registerValueWatcher: carFunction null or watcher null");
                return false;
            }
            ICarFunction.IFunctionValueWatcher w = new ICarFunction.IFunctionValueWatcher() {
                @Override
                public void onFunctionValueChanged(int propertyId, int areaId, int value) {
                    watcher.onPropertyChanged(propertyId, areaId);
                }

                @Override
                public void onCustomizeFunctionValueChanged(int propertyId, int areaId, float value) {
                    watcher.onPropertyChanged(propertyId, areaId);
                }

                @Override
                public void onFunctionChanged(int propertyId) {
                    watcher.onPropertyChanged(propertyId, 0);
                }

                @Override
                public void onSupportedFunctionStatusChanged(int propertyId, int areaId,
                        com.ecarx.xui.adaptapi.FunctionStatus status) {
                }

                @Override
                public void onSupportedFunctionValueChanged(int propertyId, int[] supportedValues) {
                }
            };
            mWatcher = w;
            return cf.registerFunctionValueWatcher(propertyIds, w);
        }

        @Override
        public void unregisterValueWatcher() {
            ICarFunction cf = mCarFunction;
            ICarFunction.IFunctionValueWatcher w = mWatcher;
            if (cf != null && w != null) {
                cf.unregisterFunctionValueWatcher(w);
            }
            mWatcher = null;
        }

        @Override
        public void disconnect() {
            if (mCar instanceof IConnectable) {
                ((IConnectable) mCar).disconnect();
            }
        }
    }
}
