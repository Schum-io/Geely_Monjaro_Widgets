package com.geely.geely_monjaro_widgets.widget.trunk

import com.geely.geely_monjaro_widgets.R
import com.geely.geely_monjaro_widgets.core.CarProperties
import com.geely.geely_monjaro_widgets.core.ToggleCarWidgetProvider
import com.geely.os.car.IGlyCar

/**
 * Виджет открытия/закрытия багажника. Toggle по текущему состоянию: если багажник
 * открыт или открывается — закрывает, иначе открывает.
 *
 * Чтение состояния — TRUNK_STATE, команда — отдельный командный property TRUNK_COMMAND.
 */
class TrunkWidgetProvider : ToggleCarWidgetProvider() {

    override val layoutRes = R.layout.widget_trunk
    override val iconViewId = R.id.trunkIcon
    override val actionName = ACTION_TOGGLE_TRUNK

    override fun isActive(car: IGlyCar): Boolean {
        val state = car.getIntProperty(CarProperties.TRUNK_STATE, CarProperties.AREA_TRUNK)
        return CarProperties.isTrunkOpenish(state)
    }

    override fun toggle(car: IGlyCar, currentlyActive: Boolean) {
        val command = if (currentlyActive) CarProperties.TRUNK_CLOSE else CarProperties.TRUNK_OPEN
        car.setIntProperty(CarProperties.TRUNK_COMMAND, CarProperties.AREA_TRUNK, command)
    }

    override fun iconRes(active: Boolean): Int =
        if (active) R.drawable.ic_trunk_on else R.drawable.ic_trunk_off

    companion object {
        private const val ACTION_TOGGLE_TRUNK =
            "com.geely.geely_monjaro_widgets.action.TOGGLE_TRUNK"
    }
}
