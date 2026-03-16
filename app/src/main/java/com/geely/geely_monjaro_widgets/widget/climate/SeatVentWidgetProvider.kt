package com.geely.geely_monjaro_widgets.widget.climate

import com.geely.geely_monjaro_widgets.R
import com.geely.geely_monjaro_widgets.core.CarProperties
import com.geely.geely_monjaro_widgets.core.CycleLevelWidgetProvider

/** Общая база виджетов вентиляции сиденья (иконки — поток воздуха по уровню). */
abstract class SeatVentWidgetProvider : CycleLevelWidgetProvider() {
    override val layoutRes = R.layout.widget_seat_climate
    override val iconViewId = R.id.climateIcon
    override val propertyId = CarProperties.SEAT_VENTILATION
    override val bottomLabelRes = R.string.climate_vent

    override fun iconRes(level: Int): Int = when (level) {
        1 -> R.drawable.seat_wind_level1_status
        2 -> R.drawable.seat_wind_level2_status
        3 -> R.drawable.seat_wind_level3_status
        else -> R.drawable.seat_wind_level0_status
    }
}

/** Вентиляция сиденья водителя. */
class DriverSeatVentWidgetProvider : SeatVentWidgetProvider() {
    override val areaId = CarProperties.AREA_SEAT_DRIVER
    override val actionName = "com.geely.geely_monjaro_widgets.action.VENT_DRIVER"
    override val topLabelRes = R.string.climate_driver
}

/** Вентиляция сиденья пассажира. */
class PassengerSeatVentWidgetProvider : SeatVentWidgetProvider() {
    override val areaId = CarProperties.AREA_SEAT_PASSENGER
    override val actionName = "com.geely.geely_monjaro_widgets.action.VENT_PASSENGER"
    override val topLabelRes = R.string.climate_passenger
}
