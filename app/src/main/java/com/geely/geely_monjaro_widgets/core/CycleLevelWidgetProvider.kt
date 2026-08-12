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
import android.util.TypedValue
import android.widget.RemoteViews
import com.geely.geely_monjaro_widgets.R
import com.geely.geely_monjaro_widgets.service.CarStateService
import com.geely.os.car.IGlyCar

/**
 * Базовый класс для виджетов с одной кнопкой, циклически переключающей уровень
 * свойства машины (OFF → 3 → 2 → 1 → OFF; набор уровней настраивается в приложении).
 * Иконка отражает текущий уровень.
 *
 * Используется для подогрева и вентиляции сидений и подогрева руля.
 */
abstract class CycleLevelWidgetProvider : AppWidgetProvider() {

    protected abstract val layoutRes: Int
    protected abstract val iconViewId: Int
    protected abstract val actionName: String

    /** Id свойства машины (он же база кодирования уровня). */
    protected abstract val propertyId: Int

    /** Зона: водитель (0x1) / пассажир (0x4), либо null для свойств без зоны (руль). */
    protected abstract val areaId: Int?

    /** Подпись сверху (роль: водитель/пассажир). */
    protected abstract val topLabelRes: Int

    /** Подпись снизу (функция: подогрев/вентиляция). */
    protected abstract val bottomLabelRes: Int

    /** Ресурс иконки для уровня 0..3. */
    protected abstract fun iconRes(level: Int): Int

    /** Круг-индикатор активного состояния (по умолчанию оранжевый). */
    protected open val activeCircleRes: Int = R.drawable.climate_circle_on

    /**
     * AUTO не является отдельным режимом кнопки руля. Если API вернул такое служебное
     * значение, следующее нажатие сбрасывает его в OFF; для сидений начинается обычный цикл.
     */
    protected open val autoExitsToOff: Boolean = false

    /**
     * Работает ли функция физически прямо сейчас. На заглушённой машине свойство
     * принимает запись и меняет значение, но ничего не греется — поэтому «включено»
     * рисуем только когда функция реально активна.
     */
    protected open fun isAvailable(car: IGlyCar): Boolean =
        !CarProperties.isKnownInactive(readSupportStatus(car))

    private fun readSupportStatus(car: IGlyCar): String? {
        val area = areaId
        return if (area != null) car.supportStatus(propertyId, area) else car.supportStatus(propertyId)
    }

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
        refreshLevel(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action != actionName) return

        CarCommand.run(
            context = context,
            pendingResult = goAsync(),
            action = { car ->
                val raw = readEncoded(car)
                val current = CarProperties.decodeSeatLevel(raw)
                val enabled = ModeConfig.enabledLevels(context, propertyId, areaId)
                val next = if (autoExitsToOff && CarProperties.isAutoLevel(raw)) {
                    0
                } else {
                    CarProperties.nextSeatLevel(current, enabled)
                }
                // Доступность проверяем после записи, как и в toggle-виджетах.
                if (writeEncoded(car, CarProperties.encodeSeatLevel(propertyId, next))) {
                    if (isAvailable(car)) next else 0
                } else {
                    null
                }
            },
            onSuccess = { level -> updateIcon(context, level) },
        )
    }

    private fun refreshLevel(context: Context) {
        CarCommand.read(context) { car ->
            val level = if (isAvailable(car)) CarProperties.decodeSeatLevel(readEncoded(car)) else 0
            updateIcon(context, level)
        }
    }

    private fun readEncoded(car: IGlyCar): Int {
        val area = areaId
        return if (area != null) car.getIntProperty(propertyId, area) else car.getIntProperty(propertyId)
    }

    /** Возвращает ответ машины: false = команду не приняли, её нужно повторить. */
    private fun writeEncoded(car: IGlyCar, encoded: Int): Boolean {
        val area = areaId
        return if (area != null) {
            car.setIntProperty(propertyId, area, encoded)
        } else {
            car.setIntProperty(propertyId, encoded)
        }
    }

    private fun updateIcon(context: Context, level: Int) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val thisWidget = ComponentName(context, javaClass)
        val bitmap = drawableToBitmap(context.getDrawable(iconRes(level))!!)
        val circleRes = if (level > 0) activeCircleRes else R.drawable.climate_circle_off
        for (id in appWidgetManager.getAppWidgetIds(thisWidget)) {
            val views = buildViews(context, id)
            views.setImageViewBitmap(iconViewId, bitmap)
            views.setImageViewResource(R.id.climateCircle, circleRes)
            appWidgetManager.updateAppWidget(id, views)
        }
    }

    private fun buildViews(context: Context, appWidgetId: Int): RemoteViews {
        val views = RemoteViews(context.packageName, layoutRes)
        views.setTextViewText(R.id.climateTopText, context.getString(topLabelRes))
        views.setTextViewText(R.id.climateBottomText, context.getString(bottomLabelRes))
        // Размер задаём программно — надёжнее XML на виджетах (некоторые лаунчеры
        // не применяют textSize из layout до перезапуска).
        views.setTextViewTextSize(R.id.climateTopText, TypedValue.COMPLEX_UNIT_SP, 22f)
        views.setTextViewTextSize(R.id.climateBottomText, TypedValue.COMPLEX_UNIT_SP, 18f)
        views.setOnClickPendingIntent(iconViewId, cyclePendingIntent(context, appWidgetId))
        return views
    }

    private fun cyclePendingIntent(context: Context, requestCode: Int): PendingIntent =
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
