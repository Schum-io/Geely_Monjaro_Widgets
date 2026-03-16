package com.geely.geely_monjaro_widgets.widget.climate

import com.geely.geely_monjaro_widgets.R
import com.geely.geely_monjaro_widgets.core.CarProperties
import com.geely.geely_monjaro_widgets.core.CycleLevelWidgetProvider

/** Виджет подогрева руля — 3 уровня, цикл OFF→3→2→1→OFF. Свойство без зоны. */
class SteeringWheelHeatWidgetProvider : CycleLevelWidgetProvider() {
    override val layoutRes = R.layout.widget_seat_climate
    override val iconViewId = R.id.climateIcon
    override val propertyId = CarProperties.STEERING_WHEEL_HEATING
    override val areaId: Int? = null
    override val actionName = "com.geely.geely_monjaro_widgets.action.HEAT_STEERING"
    override val topLabelRes = R.string.climate_steering
    override val bottomLabelRes = R.string.climate_heat

    override fun iconRes(level: Int): Int = when (level) {
        1 -> R.drawable.steering_wheel_status_level1
        2 -> R.drawable.steering_wheel_status_level2
        3 -> R.drawable.steering_wheel_status_level3
        else -> R.drawable.steering_wheel_status_level0
    }
}
