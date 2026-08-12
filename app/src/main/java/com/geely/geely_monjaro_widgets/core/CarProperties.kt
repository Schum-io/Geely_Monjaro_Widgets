package com.geely.geely_monjaro_widgets.core

/**
 * Идентификаторы свойств, зон и значений API машины Geely Monjaro.
 */
object CarProperties {

    /**
     * Что платформа возвращает ВМЕСТО реального значения, когда соединения с ECarX нет
     * или функция сейчас `notavailable`. Это «состояние неизвестно», а не «выключено».
     */
    const val FUNCTION_VALUE_UNAVAILABLE = 0xFF

    fun isUnavailable(value: Int): Boolean = value == FUNCTION_VALUE_UNAVAILABLE

    /**
     * Свойства, за изменениями которых следим, чтобы отражать правки из штатного меню.
     * Общий список для фонового сервиса и экрана-хаба.
     */
    val WATCHED_PROPERTIES: IntArray
        get() = intArrayOf(
            WIPER_SERVICE_POSITION,
            TRUNK_STATE,
            SEAT_HEATING,
            SEAT_VENTILATION,
            STEERING_WHEEL_HEATING,
            SEAT_POSITION_RESTORE,
            AIR_CIRCULATION,
            DEFROST_REAR,
        )

    /** Сенсоры, за которыми следим (топливо). */
    val WATCHED_SENSORS: IntArray
        get() = intArrayOf(SENSOR_FUEL_PERCENTAGE, SENSOR_FUEL_LEVEL)

    // ───── Дворники ─────
    /** GlyCarPropertyIds.SETTING_FUNC_WINDSCREEN_SERVICE_POSITION */
    const val WIPER_SERVICE_POSITION = 0x200c0100
    const val AREA_FRONT_WIPER = 1

    // ───── Багажник ─────
    /** Командный property: запись = действие. */
    const val TRUNK_COMMAND = 0x21020100
    /** Текущее состояние багажника (только чтение). */
    const val TRUNK_STATE = 0x2c020600
    const val AREA_TRUNK = 0x20000000

    const val TRUNK_OPEN = 1
    const val TRUNK_CLOSE = 0
    /** В штатном API называется DOOR_PAUSE — остановить створку на полпути. */
    const val TRUNK_PAUSE = 0x21020101

    // Значения TRUNK_STATE (сверено с GlyCarPropertyValue штатной прошивки)
    const val TRUNK_STATE_UNKNOWN = 0x2c020601
    const val TRUNK_STATE_FULL_CLOSE = 0x2c020602
    const val TRUNK_STATE_MOVE_UP = 0x2c020603
    const val TRUNK_STATE_MOVE_UP_BREAK = 0x2c020604
    const val TRUNK_STATE_STOP_DURING_OPEN = 0x2c020605
    const val TRUNK_STATE_FULL_OPEN = 0x2c020606
    const val TRUNK_STATE_MOVE_DOWN = 0x2c020607
    const val TRUNK_STATE_MOVE_DOWN_BREAK = 0x2c020608
    const val TRUNK_STATE_STOP_DURING_CLOSE = 0x2c020609
    const val TRUNK_STATE_HALF_CLOSE = 0x2c020610
    const val TRUNK_STATE_STOP_MIN_POSITION = 0x2c020611

    /**
     * Считается ли багажник «открытым или открывающимся» — для toggle-виджета
     * (в этом случае команда должна закрывать). Неизвестное состояние (UNKNOWN от самой
     * машины либо сентинел 0xFF) трактуем как «закрыт»: нажатие пошлёт «открыть».
     */
    fun isTrunkOpenish(state: Int): Boolean = when (state) {
        TRUNK_STATE_FULL_OPEN,
        TRUNK_STATE_MOVE_UP,
        TRUNK_STATE_MOVE_UP_BREAK,
        TRUNK_STATE_STOP_DURING_OPEN -> true

        else -> false
    }

    // ───── Память сидений ─────
    /**
     * Выбор слота + вызов профиля (SETTING_FUNC_SEAT_POSITION_SAVE_AS_RESTORE).
     * Свойство читаемое: возвращает текущий выбранный слот — этим и подсвечиваем кнопку.
     *
     * ⚠️ Рядом есть опасное свойство `SEAT_POSITION_SAVE = 0x2d400100`: запись в него
     * значения 1 ПЕРЕЗАПИСЫВАЕТ выбранный слот текущим положением сиденья (штатная
     * кнопка «Сохранить»). Мы в 0x2d400100 не пишем никогда, иначе затрём профиль.
     */
    const val SEAT_POSITION_RESTORE = 0x2d500600
    const val AREA_SEAT_DRIVER = 0x1
    const val AREA_SEAT_PASSENGER = 0x4

    /**
     * Значения слотов. Штатное приложение (getSeatSavePosition) нумерует кнопки именно
     * так, пропуская SAVE_AS_1 (0x2d500501) — сверено с декомпилом.
     */
    const val SEAT_PROFILE_1 = 0x2d500502
    const val SEAT_PROFILE_2 = 0x2d500503
    const val SEAT_PROFILE_3 = 0x2d500504

    /** Значение профиля по индексу кнопки (0..2). */
    fun seatProfileValue(index: Int): Int = when (index) {
        0 -> SEAT_PROFILE_1
        1 -> SEAT_PROFILE_2
        2 -> SEAT_PROFILE_3
        else -> throw IllegalArgumentException("Unknown seat profile index: $index")
    }

    /**
     * Индекс активного профиля (0..2) по прочитанному значению SEAT_POSITION_RESTORE.
     * Маппинг как в штатном приложении (getSeatSavePositionIndex): профиль 1 и любое
     * неизвестное значение → 0.
     */
    fun seatProfileIndex(value: Int): Int = when (value) {
        SEAT_PROFILE_2 -> 1
        SEAT_PROFILE_3 -> 2
        else -> 0
    }

    // ───── Подогрев / вентиляция сидений ─────
    /**
     * База подогрева/вентиляции. Значение уровня кодируется как `base | level`
     * для уровней 1..3, выключено = 0. Чтение возвращает закодированное значение,
     * уровень = `value and 0xF` (0 = выкл, 1..3, 0xf = auto).
     */
    const val SEAT_HEATING = 0x10050200
    const val SEAT_VENTILATION = 0x10050100
    /** Подогрев руля — одна зона (без areaId), кодировка уровней как у сидений. */
    const val STEERING_WHEEL_HEATING = 0x10090100
    // areaId сидений — те же, что у памяти: AREA_SEAT_DRIVER (0x1) / AREA_SEAT_PASSENGER (0x4)

    // ───── Рециркуляция воздуха ─────
    /** Рециркуляция — одна зона (без areaId). */
    const val AIR_CIRCULATION = 0x10030100
    const val CIRCULATION_INNER = 0x10030101   // рециркуляция (закрытый контур)
    const val CIRCULATION_OUTSIDE = 0x10030102 // забор наружного воздуха

    // ───── Обогрев заднего стекла ─────
    /** Обогрев заднего стекла — toggle 0/1, без areaId. */
    const val DEFROST_REAR = 0x10040300

    // ───── Готовность функций ─────
    /**
     * Достоверно ли известно, что функция сейчас НЕ работает физически.
     *
     * Признак «работает» — строгий `FunctionStatus == active`: по нему же штатное меню
     * включает свои кнопки, поэтому на заведённой машине статус заведомо `active`, а на
     * заглушённой — нет. Гейт только на понижение: `null` (статус не прочитался) НЕ
     * блокирует, иначе виджет гаснет там, где на деле всё исправно.
     *
     * Пробовали вместо этого гейт по зажиганию (0x200100) и режиму машины (0x201400),
     * как в штатном HVAC-приложении, — на этой машине читаются недостоверно, отказались.
     */
    fun isKnownInactive(status: String?): Boolean =
        status != null && !status.equals("active", ignoreCase = true)

    // ───── Топливо ─────
    /** Уровень топлива в баке в процентах (сенсор, float 0..100). */
    const val SENSOR_FUEL_PERCENTAGE = 0x404500
    /** Уровень топлива в баке в литрах (сенсор, float). На части машин = 0. */
    const val SENSOR_FUEL_LEVEL = 0x100600
    /** Физический объём бака (л) — показывается при 100%. Запасная константа, если ICarInfo не отдаёт инфо. */
    const val FUEL_TANK_CAPACITY_L = 62f
    /**
     * Калибровка: гейдж 0→100% соответствует ~55 л реального движения топлива
     */
    const val FUEL_GAUGE_SPAN_L = 55f

    /**
     * Литры топлива в баке. Если сенсор литров даёт вменяемое значение — берём его.
     * Иначе линейная модель с резервом: `reserve + проценты/100 × span`, где
     * span = [FUEL_GAUGE_SPAN_L] (калибр наклона), reserve = ёмкость − span. Так при 100%
     * выходит физический объём бака, а приросты между процентами совпадают с реальными
     * доливами (наклон ~0.55 л/%). Ёмкость [tankCapacityL] — из машины (ICarInfo);
     * если 0/недоступна — запасная [FUEL_TANK_CAPACITY_L].
     */
    fun fuelLiters(litersSensor: Float, percent: Float, tankCapacityL: Float = FUEL_TANK_CAPACITY_L): Float {
        if (litersSensor > 1f) return litersSensor
        if (percent <= 0f) return 0f
        val capacity = if (tankCapacityL > 0f) tankCapacityL else FUEL_TANK_CAPACITY_L
        val reserve = (capacity - FUEL_GAUGE_SPAN_L).coerceAtLeast(0f)
        return reserve + percent / 100f * FUEL_GAUGE_SPAN_L
    }

    const val SEAT_LEVEL_MAX = 3

    /**
     * Служебное значение AUTO имеет вид `база|0xF`, например 0x1005020F.
     * Оно не является обычным уровнем 1..3 и при декодировании отображается как OFF.
     */
    fun isAutoLevel(value: Int): Boolean = value != 0 && (value and 0xF) == 0xF

    /** Декодирует прочитанное значение свойства в уровень 0..3 (auto/прочее → 0). */
    fun decodeSeatLevel(value: Int): Int {
        if (value == 0) return 0
        val level = value and 0xF
        return if (level in 1..SEAT_LEVEL_MAX) level else 0
    }

    /** Кодирует уровень 0..3 в значение свойства для записи (база — id свойства). */
    fun encodeSeatLevel(propertyBase: Int, level: Int): Int =
        if (level <= 0) 0 else propertyBase or (level and 0xF)

    /**
     * Следующий уровень по циклу кнопки. По включённым режимам [enabled] (подмножество
     * {1,2,3}) обход идёт по убыванию, как у штатной кнопки Geely: OFF → max → … → min → OFF.
     * По умолчанию включены все три уровня (штатное поведение OFF→3→2→1→OFF).
     *
     * Если текущий уровень не входит в [enabled] (напр. режим отключили, пока он активен)
     * или набор пуст — следующим будет OFF.
     */
    fun nextSeatLevel(level: Int, enabled: Set<Int> = setOf(1, 2, 3)): Int {
        val order = enabled.filter { it in 1..SEAT_LEVEL_MAX }.sortedDescending()
        if (order.isEmpty()) return 0
        if (level == 0) return order.first()
        val idx = order.indexOf(level)
        if (idx == -1) return 0
        return order.getOrElse(idx + 1) { 0 }
    }
}
