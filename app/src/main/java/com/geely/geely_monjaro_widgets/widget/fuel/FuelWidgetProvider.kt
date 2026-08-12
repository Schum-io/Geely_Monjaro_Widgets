package com.geely.geely_monjaro_widgets.widget.fuel

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.geely.geely_monjaro_widgets.R
import com.geely.geely_monjaro_widgets.core.CarCommand
import com.geely.geely_monjaro_widgets.core.CarProperties
import com.geely.geely_monjaro_widgets.service.CarStateService
import com.geely.os.car.IGlyCar
import kotlin.math.roundToInt

/**
 * Read-only виджет: уровень топлива в баке в процентах (сенсор 0x404500).
 * Обновляется при onUpdate и по нажатию (обновить). Топливо — сенсор, а не
 * function-value, поэтому автоподписка сервиса на него не распространяется.
 */
class FuelWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        CarStateService.ensureStarted(context)
        for (appWidgetId in appWidgetIds) {
            appWidgetManager.updateAppWidget(appWidgetId, buildViews(context, appWidgetId))
        }
        refresh(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action != ACTION_REFRESH) return
        CarCommand.read(context, goAsync()) { car -> updatePercent(context, car) }
    }

    private fun refresh(context: Context) {
        CarCommand.read(context) { car -> updatePercent(context, car) }
    }

    private fun updatePercent(context: Context, car: IGlyCar) {
        val pct = car.getSensorValue(CarProperties.SENSOR_FUEL_PERCENTAGE)
        val litersSensor = car.getSensorValue(CarProperties.SENSOR_FUEL_LEVEL)
        val liters = CarProperties.fuelLiters(litersSensor, pct, car.getFuelTankCapacityLiters())
        // «40% / 20 л»; без литров, если их нет; прочерк, если нет и процентов.
        val percentText = if (pct > 0f) "${pct.roundToInt()}%" else "—"
        val text = if (pct > 0f && liters > 0f) "$percentText / ${liters.roundToInt()} л" else percentText
        val manager = AppWidgetManager.getInstance(context)
        for (id in manager.getAppWidgetIds(ComponentName(context, javaClass))) {
            val views = buildViews(context, id)
            views.setTextViewText(R.id.fuelPercent, text)
            manager.updateAppWidget(id, views)
        }
    }

    private fun buildViews(context: Context, appWidgetId: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_fuel)
        views.setOnClickPendingIntent(
            R.id.widgetRoot,
            CarStateService.actionPendingIntent(
                context, appWidgetId, javaClass, ACTION_REFRESH, "$ACTION_REFRESH/$appWidgetId"
            )
        )
        return views
    }

    companion object {
        private const val ACTION_REFRESH =
            "com.geely.geely_monjaro_widgets.action.FUEL_REFRESH"
    }
}
