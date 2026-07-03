package com.geely.geely_monjaro_widgets.widget.climate

import com.geely.geely_monjaro_widgets.R
import com.geely.geely_monjaro_widgets.core.CarProperties
import com.geely.geely_monjaro_widgets.core.ToggleCarWidgetProvider
import com.geely.os.car.IGlyCar

/**
 * Виджет рециркуляции воздуха (toggle). Вкл = внутренний контур (INNER),
 * выкл = забор наружного воздуха (OUTSIDE). Свойство без зоны.
 */
class RecirculationWidgetProvider : ToggleCarWidgetProvider() {

    override val layoutRes = R.layout.widget_recirc
    override val iconViewId = R.id.recircIcon
    override val actionName = ACTION_TOGGLE_RECIRC

    override fun isActive(car: IGlyCar): Boolean =
        car.getIntProperty(CarProperties.AIR_CIRCULATION) == CarProperties.CIRCULATION_INNER

    override fun toggle(car: IGlyCar, currentlyActive: Boolean) {
        val next = if (currentlyActive) CarProperties.CIRCULATION_OUTSIDE else CarProperties.CIRCULATION_INNER
        car.setIntProperty(CarProperties.AIR_CIRCULATION, next)
    }

    override fun iconRes(active: Boolean): Int =
        if (active) R.drawable.ic_ac_cycle_inner else R.drawable.ic_ac_cycle_outer

    companion object {
        private const val ACTION_TOGGLE_RECIRC =
            "com.geely.geely_monjaro_widgets.action.TOGGLE_RECIRC"
    }
}
