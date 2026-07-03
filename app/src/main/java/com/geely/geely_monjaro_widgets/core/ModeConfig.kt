package com.geely.geely_monjaro_widgets.core

import android.content.Context

/**
 * Хранилище настройки «какие режимы (1..3) участвуют в цикле» для функций с уровнями:
 * подогрев/вентиляция сидений (отдельно водитель/пассажир) и подогрев руля.
 *
 * Ключ — функция ([propertyId]) + зона ([areaId], у руля зоны нет). Так водитель и
 * пассажир одной функции настраиваются независимо. Значение — набор включённых уровней
 * (по умолчанию все три). Настройку читают и виджеты, и экран приложения — через общий
 * [android.content.SharedPreferences] пакета.
 */
object ModeConfig {

    private const val PREFS_NAME = "mode_config"
    private val DEFAULT_LEVELS = setOf(1, 2, 3)

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun key(propertyId: Int, areaId: Int?): String =
        if (areaId != null) "$propertyId.$areaId" else "$propertyId"

    /**
     * Включённые уровни для функции+зоны. Если настройка не задавалась — все три
     * ([DEFAULT_LEVELS], штатное поведение). Пустой сохранённый набор возвращается как есть
     * (означает «нет доступных режимов» — цикл всегда OFF).
     */
    fun enabledLevels(context: Context, propertyId: Int, areaId: Int?): Set<Int> {
        val stored = prefs(context).getStringSet(key(propertyId, areaId), null)
            ?: return DEFAULT_LEVELS
        return stored.mapNotNull { it.toIntOrNull() }.filter { it in 1..CarProperties.SEAT_LEVEL_MAX }.toSet()
    }

    /** Сохраняет включённые уровни для функции+зоны. */
    fun setEnabledLevels(context: Context, propertyId: Int, areaId: Int?, levels: Set<Int>) {
        val stored = levels.filter { it in 1..CarProperties.SEAT_LEVEL_MAX }.map { it.toString() }.toSet()
        prefs(context).edit().putStringSet(key(propertyId, areaId), stored).apply()
    }
}
