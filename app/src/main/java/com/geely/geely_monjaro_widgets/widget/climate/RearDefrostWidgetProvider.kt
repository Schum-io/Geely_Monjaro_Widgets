package com.geely.geely_monjaro_widgets.widget.climate

import com.geely.geely_monjaro_widgets.R
import com.geely.geely_monjaro_widgets.core.CarProperties
import com.geely.geely_monjaro_widgets.core.ToggleCarWidgetProvider
import com.geely.os.car.IGlyCar

/**
 * Виджет обогрева заднего стекла (toggle 0/1). Свойство без зоны.
 * Иконка одна на оба состояния, состояние показывает цвет круга.
 */
class RearDefrostWidgetProvider : ToggleCarWidgetProvider() {

    override val layoutRes = R.layout.widget_rear_defrost
    override val iconViewId = R.id.rearDefrostIcon
    override val actionName = ACTION_TOGGLE_REAR_DEFROST

    override fun isActive(car: IGlyCar): Boolean =
        // «Включено» только если функция реально активна (на заглушённой машине
        // свойство принимает запись, но функция не active — тогда не показываем on).
        car.isFunctionActive(CarProperties.DEFROST_REAR) &&
            car.getIntProperty(CarProperties.DEFROST_REAR) == 1

    override fun toggle(car: IGlyCar, currentlyActive: Boolean) {
        car.setIntProperty(CarProperties.DEFROST_REAR, if (currentlyActive) 0 else 1)
    }

    override fun iconRes(active: Boolean): Int = R.drawable.ic_rear_defrost

    companion object {
        private const val ACTION_TOGGLE_REAR_DEFROST =
            "com.geely.geely_monjaro_widgets.action.TOGGLE_REAR_DEFROST"
    }
}
