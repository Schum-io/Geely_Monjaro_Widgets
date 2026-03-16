package com.geely.geely_monjaro_widgets.widget.seat

import android.content.Context
import android.util.TypedValue
import android.widget.RemoteViews
import com.geely.geely_monjaro_widgets.R
import com.geely.geely_monjaro_widgets.core.CarProperties

/** Виджет памяти положений сиденья пассажира — 3 профиля. */
class PassengerSeatMemoryWidgetProvider : SeatMemoryWidgetProvider() {
    override val areaId = CarProperties.AREA_SEAT_PASSENGER
    override val titleRes = R.string.seat_memory_passenger
    override val actionPrefix = "com.geely.geely_monjaro_widgets.action.SEAT_PASSENGER"
    override val activeIconRes = R.drawable.seat_set_position_left_on
    override val inactiveIconRes = R.drawable.seat_set_position_left_off

    override fun buildViews(context: Context, appWidgetId: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_seat_memory_passenger)
        views.setTextViewText(R.id.seatTitle, context.getString(titleRes))
        views.setTextViewTextSize(R.id.seatTitle, TypedValue.COMPLEX_UNIT_SP, 22f)
        val selected = selectedIndex(context, appWidgetId)
        bindButton(views, context, appWidgetId, 0, R.id.seatButton1, R.id.seatIcon1, selected)
        bindButton(views, context, appWidgetId, 1, R.id.seatButton2, R.id.seatIcon2, selected)
        bindButton(views, context, appWidgetId, 2, R.id.seatButton3, R.id.seatIcon3, selected)
        return views
    }
}
