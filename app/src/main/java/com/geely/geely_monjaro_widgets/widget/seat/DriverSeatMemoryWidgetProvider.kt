package com.geely.geely_monjaro_widgets.widget.seat

import android.content.Context
import android.util.TypedValue
import android.widget.RemoteViews
import com.geely.geely_monjaro_widgets.R
import com.geely.geely_monjaro_widgets.core.CarProperties

/** Виджет памяти положений сиденья водителя — 2 профиля. */
class DriverSeatMemoryWidgetProvider : SeatMemoryWidgetProvider() {
    override val areaId = CarProperties.AREA_SEAT_DRIVER
    override val titleRes = R.string.seat_memory_driver
    override val actionPrefix = "com.geely.geely_monjaro_widgets.action.SEAT_DRIVER"
    override val activeIconRes = R.drawable.seat_set_detail_position_indicator_active
    override val inactiveIconRes = R.drawable.seat_set_detail_position_indicator_unactive

    override fun buildViews(context: Context, appWidgetId: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_seat_memory_driver)
        views.setTextViewText(R.id.seatTitle, context.getString(titleRes))
        views.setTextViewTextSize(R.id.seatTitle, TypedValue.COMPLEX_UNIT_SP, 22f)
        val selected = selectedIndex(context, appWidgetId)
        bindButton(views, context, appWidgetId, 0, R.id.seatButton1, R.id.seatIcon1, selected)
        bindButton(views, context, appWidgetId, 1, R.id.seatButton2, R.id.seatIcon2, selected)
        return views
    }
}
