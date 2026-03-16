package com.geely.geely_monjaro_widgets.widget.wipers

import com.geely.geely_monjaro_widgets.R
import com.geely.geely_monjaro_widgets.core.CarProperties
import com.geely.geely_monjaro_widgets.core.ToggleCarWidgetProvider
import com.geely.os.car.IGlyCar

/**
 * Виджет перевода стеклоочистителей в сервисное положение (toggle 0 ↔ 1).
 */
class WiperServiceWidgetProvider : ToggleCarWidgetProvider() {

    override val layoutRes = R.layout.widget_wiper_service
    override val iconViewId = R.id.wiperServiceIcon
    override val actionName = ACTION_TOGGLE_WIPER_SERVICE

    override fun isActive(car: IGlyCar): Boolean =
        car.getIntProperty(CarProperties.WIPER_SERVICE_POSITION, CarProperties.AREA_FRONT_WIPER) == 1

    override fun toggle(car: IGlyCar, currentlyActive: Boolean) {
        val next = if (currentlyActive) 0 else 1
        car.setIntProperty(CarProperties.WIPER_SERVICE_POSITION, CarProperties.AREA_FRONT_WIPER, next)
    }

    override fun iconRes(active: Boolean): Int =
        if (active) R.drawable.ic_wiper_on else R.drawable.ic_wiper_off

    companion object {
        private const val ACTION_TOGGLE_WIPER_SERVICE =
            "com.geely.geely_monjaro_widgets.action.TOGGLE_WIPER_SERVICE"
    }
}
