package com.geely.geely_monjaro_widgets.widget.climate

import com.geely.geely_monjaro_widgets.R
import com.geely.geely_monjaro_widgets.core.CarProperties
import com.geely.geely_monjaro_widgets.core.CycleLevelWidgetProvider

/** Общая база виджетов подогрева сиденья (иконки — волны тепла по уровню). */
abstract class SeatHeatWidgetProvider : CycleLevelWidgetProvider() {
    override val layoutRes = R.layout.widget_seat_climate
    override val iconViewId = R.id.climateIcon
    override val propertyId = CarProperties.SEAT_HEATING
    override val bottomLabelRes = R.string.climate_heat

    override fun iconRes(level: Int): Int = when (level) {
        1 -> R.drawable.seat_heat_level1_status
        2 -> R.drawable.seat_heat_level2_status
        3 -> R.drawable.seat_heat_level3_status
        else -> R.drawable.seat_heat_level0_status
    }
}

/** Подогрев сиденья водителя. */
class DriverSeatHeatWidgetProvider : SeatHeatWidgetProvider() {
    override val areaId = CarProperties.AREA_SEAT_DRIVER
    override val actionName = "com.geely.geely_monjaro_widgets.action.HEAT_DRIVER"
    override val topLabelRes = R.string.climate_driver
}

/** Подогрев сиденья пассажира. */
class PassengerSeatHeatWidgetProvider : SeatHeatWidgetProvider() {
    override val areaId = CarProperties.AREA_SEAT_PASSENGER
    override val actionName = "com.geely.geely_monjaro_widgets.action.HEAT_PASSENGER"
    override val topLabelRes = R.string.climate_passenger
}
