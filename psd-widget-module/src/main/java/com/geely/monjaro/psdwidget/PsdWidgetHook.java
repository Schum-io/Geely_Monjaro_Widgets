package com.geely.monjaro.psdwidget;

import java.util.List;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

/**
 * LSposed-модуль: добавляет виджеты приложения Monjaro Widgets в whitelist
 * виджетов экрана пассажира (PSD) системного лаунчера oneOS_Launcher3.
 *
 * Лаунчер строит список доступных виджетов не из всех установленных провайдеров,
 * а из жёстко зашитого whitelist (res/xml/widget_whitelist.xml, блок &lt;psd&gt;),
 * который отдаёт {@code XMLParseUtils.getWidgetWhiteList(int displayType)}
 * (0 = экран водителя/CSD, !=0 = экран пассажира/PSD). Мы дописываем свои записи
 * в результат для PSD.
 *
 * Сопоставление установленного провайдера с записью whitelist идёт по
 * {@code WidgetEntity.className} == полному имени класса провайдера
 * (см. BaseWidgetVm.filterWidgetWhiteBlackList), поэтому className обязан точно
 * совпадать с ComponentName.getClassName() наших AppWidgetProvider.
 */
public class PsdWidgetHook implements IXposedHookLoadPackage {

    private static final String TAG = "MonjaroPsdWidget";

    private static final String LAUNCHER_PKG = "com.android.launcher3";
    private static final String XML_PARSE_UTILS = "com.geely.utils.XMLParseUtils";
    private static final String WIDGET_ENTITY = "com.geely.model.db.entity.WidgetEntity";

    /** displayType записи для экрана пассажира (enum psd=1 в attrs.xml лаунчера). */
    private static final int DISPLAY_TYPE_PSD = 1;

    /** Пакет классов приложения (namespace) — по нему строятся FQCN провайдеров. */
    private static final String CLASS_PKG = "com.geely.geely_monjaro_widgets";
    /** На экран пассажира выводим только пассажирские виджеты (FQCN классов). */
    private static final String[] OUR_PROVIDERS = {
            CLASS_PKG + ".widget.seat.PassengerSeatMemoryWidgetProvider",
            CLASS_PKG + ".widget.climate.PassengerSeatHeatWidgetProvider",
            CLASS_PKG + ".widget.climate.PassengerSeatVentWidgetProvider",
    };

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) {
        if (!LAUNCHER_PKG.equals(lpparam.packageName)) {
            return;
        }
        final ClassLoader cl = lpparam.classLoader;
        try {
            XposedHelpers.findAndHookMethod(
                    XML_PARSE_UTILS, cl, "getWidgetWhiteList", int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            int displayType = (int) param.args[0];
                            if (displayType == 0) {
                                return; // экран водителя — не трогаем
                            }
                            Object result = param.getResult();
                            if (!(result instanceof List)) {
                                return;
                            }
                            @SuppressWarnings("unchecked")
                            List<Object> list = (List<Object>) result;
                            addOurWidgets(cl, list);
                        }
                    });
            XposedBridge.log(TAG + ": hooked getWidgetWhiteList in " + lpparam.packageName);
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": failed to hook getWidgetWhiteList: " + t);
        }
    }

    private void addOurWidgets(ClassLoader cl, List<Object> list) {
        final Class<?> entityCls;
        try {
            entityCls = XposedHelpers.findClass(WIDGET_ENTITY, cl);
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": WidgetEntity class not found: " + t);
            return;
        }
        for (String provider : OUR_PROVIDERS) {
            if (containsProvider(list, provider)) {
                continue; // результат кешируется лаунчером — избегаем дублей
            }
            try {
                Object entity = entityCls.newInstance();
                XposedHelpers.setObjectField(entity, "packageName", CLASS_PKG);
                XposedHelpers.setObjectField(entity, "className", provider);
                XposedHelpers.setIntField(entity, "displayType", DISPLAY_TYPE_PSD);
                list.add(entity);
                XposedBridge.log(TAG + ": added PSD widget " + provider);
            } catch (Throwable t) {
                XposedBridge.log(TAG + ": failed to add " + provider + ": " + t);
            }
        }
    }

    private boolean containsProvider(List<Object> list, String provider) {
        for (Object entity : list) {
            try {
                Object className = XposedHelpers.getObjectField(entity, "className");
                if (provider.equals(className)) {
                    return true;
                }
            } catch (Throwable ignored) {
                // запись другого формата — пропускаем
            }
        }
        return false;
    }
}
