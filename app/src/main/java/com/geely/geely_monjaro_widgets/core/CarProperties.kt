package com.geely.geely_monjaro_widgets.core

/**
 * Идентификаторы свойств, зон и значений API машины Geely Monjaro.
 *
 */
object CarProperties {

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
    const val TRUNK_PAUSE = 0x21020101

    // Значения TRUNK_STATE
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
     * (в этом случае команда должна закрывать).
     */
    fun isTrunkOpenish(state: Int): Boolean = when (state) {
        TRUNK_STATE_FULL_OPEN,
        TRUNK_STATE_MOVE_UP,
        TRUNK_STATE_MOVE_UP_BREAK,
        TRUNK_STATE_STOP_DURING_OPEN -> true
        else -> false
    }

    /** Команда для toggle-нажатия исходя из текущего состояния багажника. */
    fun trunkToggleCommand(state: Int): Int =
        if (isTrunkOpenish(state)) TRUNK_CLOSE else TRUNK_OPEN

    // ───── Память сидений ─────
    /** Восстановление (вызов) сохранённого профиля положения сиденья. */
    const val SEAT_POSITION_RESTORE = 0x2d500600
    const val AREA_SEAT_DRIVER = 0x1
    const val AREA_SEAT_PASSENGER = 0x4

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

    // ───── Топливо ─────
    /** Уровень топлива в баке в процентах (сенсор, float 0..100). */
    const val SENSOR_FUEL_PERCENTAGE = 0x404500
    /** Уровень топлива в баке в литрах (сенсор, float). */
    const val SENSOR_FUEL_LEVEL = 0x100600

    const val SEAT_LEVEL_MAX = 3

    /** Декодирует прочитанное значение свойства в уровень 0..3 (auto/прочее → 0). */
    fun decodeSeatLevel(value: Int): Int {
        if (value == 0) return 0
        val level = value and 0xF
        return if (level in 1..SEAT_LEVEL_MAX) level else 0
    }

    /** Кодирует уровень 0..3 в значение свойства для записи (база — id свойства). */
    fun encodeSeatLevel(propertyBase: Int, level: Int): Int =
        if (level <= 0) 0 else propertyBase or (level and 0xF)

    /** Следующий уровень по циклу штатной кнопки: OFF → 3 → 2 → 1 → OFF. */
    fun nextSeatLevel(level: Int): Int = when (level) {
        0 -> 3
        3 -> 2
        2 -> 1
        else -> 0
    }
}
