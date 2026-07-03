package com.geely.geely_monjaro_widgets.core

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.widget.RemoteViews
import com.geely.geely_monjaro_widgets.R
import com.geely.geely_monjaro_widgets.service.CarStateService
import com.geely.os.car.IGlyCar

/**
 * Базовый класс для виджетов-переключателей (одна иконка-кнопка, toggle одного
 * состояния машины). Инкапсулирует весь шаблон: RemoteViews, PendingIntent,
 * goAsync(), короткоживущее соединение через [withCar] и обновление иконки.
 *
 * Подкласс задаёт лишь привязку к ресурсам и логику конкретного свойства.
 */
abstract class ToggleCarWidgetProvider : AppWidgetProvider() {

    /** Layout виджета. */
    protected abstract val layoutRes: Int

    /** id иконки-кнопки внутри layout. */
    protected abstract val iconViewId: Int

    /** Уникальное имя action для PendingIntent данного виджета. */
    protected abstract val actionName: String

    /** Активно ли состояние сейчас (читает свойство машины). */
    protected abstract fun isActive(car: IGlyCar): Boolean

    /** Выполнить переключение, зная текущее состояние [currentlyActive]. */
    protected abstract fun toggle(car: IGlyCar, currentlyActive: Boolean)

    /** Ресурс иконки для активного / неактивного состояния. */
    protected abstract fun iconRes(active: Boolean): Int

    /**
     * true — сразу рисуем предполагаемое (переключённое) состояние, не перечитывая
     * (для команд с отложенным эффектом, напр. багажник: створка едет, TRUNK_STATE
     * меняется не сразу). false — после записи перечитываем реальное состояние
     * (машина могла отклонить команду, напр. двигатель заглушён).
     */
    protected open val optimistic: Boolean = false

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
        refreshState(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action != actionName) return

        // Быстрый путь: живое соединение сервиса — без переподключения.
        val live = CarStateService.liveCar
        if (live != null) {
            try {
                applyToggle(context, live)
            } catch (_: Throwable) {
            }
            return
        }
        // Запасной путь: разовое подключение.
        val pendingResult = goAsync()
        withCar(context, onDone = { pendingResult.finish() }) { car ->
            applyToggle(context, car)
        }
    }

    private fun applyToggle(context: Context, car: IGlyCar) {
        val active = isActive(car)
        toggle(car, active)
        // По умолчанию показываем РЕАЛЬНОЕ состояние (если машина отклонила команду
        // — напр. заглушённый двигатель — иконка не «загорится»). Для отложенных
        // команд (багажник) — оптимистично.
        val shown = if (optimistic) !active else isActive(car)
        updateIcon(context, shown)
    }

    /** Читает актуальное состояние и обновляет внешний вид виджета. */
    private fun refreshState(context: Context) {
        withCar(context) { car -> updateIcon(context, isActive(car)) }
    }

    private fun updateIcon(context: Context, active: Boolean) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val thisWidget = ComponentName(context, javaClass)
        val bitmap = drawableToBitmap(context.getDrawable(iconRes(active))!!)
        val circleRes = if (active) R.drawable.climate_circle_on else R.drawable.climate_circle_off
        for (id in appWidgetManager.getAppWidgetIds(thisWidget)) {
            val views = buildViews(context, id)
            views.setImageViewBitmap(iconViewId, bitmap)
            views.setImageViewResource(R.id.stateCircle, circleRes)
            appWidgetManager.updateAppWidget(id, views)
        }
    }

    private fun buildViews(context: Context, appWidgetId: Int): RemoteViews {
        val views = RemoteViews(context.packageName, layoutRes)
        views.setOnClickPendingIntent(iconViewId, togglePendingIntent(context, appWidgetId))
        return views
    }

    private fun togglePendingIntent(context: Context, requestCode: Int): PendingIntent =
        CarStateService.actionPendingIntent(
            context,
            requestCode,
            javaClass,
            actionName,
            uniqueTag = "$actionName/$requestCode",
        )

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        val size = 192
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, size, size)
        drawable.draw(canvas)
        return bitmap
    }
}
