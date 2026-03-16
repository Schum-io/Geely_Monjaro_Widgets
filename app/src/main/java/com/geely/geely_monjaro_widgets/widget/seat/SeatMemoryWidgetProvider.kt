package com.geely.geely_monjaro_widgets.widget.seat

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.geely.geely_monjaro_widgets.core.CarProperties
import com.geely.geely_monjaro_widgets.core.withCar
import com.geely.geely_monjaro_widgets.service.CarStateService
import com.geely.os.car.IGlyCar

/**
 * Общая логика виджетов памяти сидений: работа с машиной и хранение выбранной
 * позиции. Раскладку кнопок каждый подкласс строит явно в [buildViews] (водитель —
 * 2 кнопки, пассажир — 3), без динамики.
 */
abstract class SeatMemoryWidgetProvider : AppWidgetProvider() {

    /** Зона сиденья: водитель (0x1) или пассажир (0x4). */
    protected abstract val areaId: Int

    /** Подпись виджета (имя сиденья). */
    protected abstract val titleRes: Int

    /** Уникальный префикс action данного виджета. */
    protected abstract val actionPrefix: String

    /** Иконка сиденья в выбранном (активном) состоянии. */
    protected abstract val activeIconRes: Int

    /** Иконка сиденья в невыбранном состоянии. */
    protected abstract val inactiveIconRes: Int

    /** Подкласс строит свою раскладку кнопок. */
    protected abstract fun buildViews(context: Context, appWidgetId: Int): RemoteViews

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
        refreshActiveProfile(context)
    }

    /** Читает активный профиль из машины и обновляет подсветку всех экземпляров. */
    private fun refreshActiveProfile(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, javaClass))
        if (ids.isEmpty()) return
        withCar(context) { car ->
            val index = CarProperties.seatProfileIndex(
                car.getIntProperty(CarProperties.SEAT_POSITION_RESTORE, areaId)
            )
            for (id in ids) {
                setSelectedIndex(context, id, index)
                manager.updateAppWidget(id, buildViews(context, id))
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return
        if (!action.startsWith(actionPrefix)) return
        val profileIndex = intent.getIntExtra(EXTRA_PROFILE_INDEX, -1)
        if (profileIndex < 0) return

        val appWidgetId =
            intent.getIntExtra(EXTRA_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            // Мгновенно подсвечиваем выбранную позицию.
            setSelectedIndex(context, appWidgetId, profileIndex)
            AppWidgetManager.getInstance(context)
                .updateAppWidget(appWidgetId, buildViews(context, appWidgetId))
        }

        val pendingResult = goAsync()
        withCar(context, onDone = { pendingResult.finish() }) { car ->
            car.setIntProperty(
                CarProperties.SEAT_POSITION_RESTORE,
                areaId,
                CarProperties.seatProfileValue(profileIndex)
            )
        }
    }

    /** Привязывает одну кнопку профиля: иконка по состоянию + обработчик нажатия. */
    protected fun bindButton(
        views: RemoteViews,
        context: Context,
        appWidgetId: Int,
        index: Int,
        buttonId: Int,
        iconId: Int,
        selectedIndex: Int
    ) {
        views.setImageViewResource(
            iconId,
            if (selectedIndex == index) activeIconRes else inactiveIconRes
        )
        views.setOnClickPendingIntent(buttonId, profilePendingIntent(context, appWidgetId, index))
    }

    protected fun selectedIndex(context: Context, appWidgetId: Int): Int =
        prefs(context).getInt(selectionKey(appWidgetId), -1)

    private fun setSelectedIndex(context: Context, appWidgetId: Int, index: Int) {
        prefs(context).edit().putInt(selectionKey(appWidgetId), index).apply()
    }

    private fun profilePendingIntent(
        context: Context,
        appWidgetId: Int,
        profileIndex: Int
    ): PendingIntent {
        val intent = Intent(context, javaClass).apply {
            action = "$actionPrefix.$profileIndex"
            putExtra(EXTRA_PROFILE_INDEX, profileIndex)
            putExtra(EXTRA_WIDGET_ID, appWidgetId)
        }
        return PendingIntent.getBroadcast(
            context,
            appWidgetId * 10 + profileIndex,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun selectionKey(appWidgetId: Int) = "$actionPrefix.$appWidgetId.selected"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "seat_memory_widget"
        private const val EXTRA_PROFILE_INDEX =
            "com.geely.geely_monjaro_widgets.extra.PROFILE_INDEX"
        private const val EXTRA_WIDGET_ID =
            "com.geely.geely_monjaro_widgets.extra.WIDGET_ID"
    }
}
