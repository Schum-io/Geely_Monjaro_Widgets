package com.geely.geely_monjaro_widgets

import com.geely.geely_monjaro_widgets.core.CarProperties
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CarPropertiesTest {

    @Test
    fun trunk_openish_states_close_on_toggle() {
        val openStates = listOf(
            CarProperties.TRUNK_STATE_FULL_OPEN,
            CarProperties.TRUNK_STATE_MOVE_UP,
            CarProperties.TRUNK_STATE_MOVE_UP_BREAK,
            CarProperties.TRUNK_STATE_STOP_DURING_OPEN,
        )
        for (state in openStates) {
            assertTrue("state $state should be openish", CarProperties.isTrunkOpenish(state))
            assertEquals(CarProperties.TRUNK_CLOSE, CarProperties.trunkToggleCommand(state))
        }
    }

    @Test
    fun trunk_closed_states_open_on_toggle() {
        val closedStates = listOf(
            CarProperties.TRUNK_STATE_FULL_CLOSE,
            CarProperties.TRUNK_STATE_MOVE_DOWN,
            CarProperties.TRUNK_STATE_STOP_DURING_CLOSE,
            CarProperties.TRUNK_STATE_HALF_CLOSE,
            CarProperties.TRUNK_STATE_STOP_MIN_POSITION,
        )
        for (state in closedStates) {
            assertFalse("state $state should not be openish", CarProperties.isTrunkOpenish(state))
            assertEquals(CarProperties.TRUNK_OPEN, CarProperties.trunkToggleCommand(state))
        }
    }

    @Test
    fun seat_profile_values_map_by_index() {
        assertEquals(CarProperties.SEAT_PROFILE_1, CarProperties.seatProfileValue(0))
        assertEquals(CarProperties.SEAT_PROFILE_2, CarProperties.seatProfileValue(1))
        assertEquals(CarProperties.SEAT_PROFILE_3, CarProperties.seatProfileValue(2))
    }

    @Test(expected = IllegalArgumentException::class)
    fun seat_profile_value_rejects_unknown_index() {
        CarProperties.seatProfileValue(3)
    }

    @Test
    fun seat_level_cycle_is_off_3_2_1_off() {
        assertEquals(3, CarProperties.nextSeatLevel(0))
        assertEquals(2, CarProperties.nextSeatLevel(3))
        assertEquals(1, CarProperties.nextSeatLevel(2))
        assertEquals(0, CarProperties.nextSeatLevel(1))
    }

    @Test
    fun cycle_with_only_level_3_is_off_3_off() {
        val enabled = setOf(3)
        assertEquals(3, CarProperties.nextSeatLevel(0, enabled))
        assertEquals(0, CarProperties.nextSeatLevel(3, enabled))
    }

    @Test
    fun cycle_with_levels_3_and_1_is_off_3_1_off() {
        val enabled = setOf(3, 1)
        assertEquals(3, CarProperties.nextSeatLevel(0, enabled))
        assertEquals(1, CarProperties.nextSeatLevel(3, enabled))
        assertEquals(0, CarProperties.nextSeatLevel(1, enabled))
    }

    @Test
    fun cycle_with_only_level_2_is_off_2_off() {
        val enabled = setOf(2)
        assertEquals(2, CarProperties.nextSeatLevel(0, enabled))
        assertEquals(0, CarProperties.nextSeatLevel(2, enabled))
    }

    @Test
    fun cycle_with_all_levels_is_off_3_2_1_off() {
        val enabled = setOf(1, 2, 3)
        assertEquals(3, CarProperties.nextSeatLevel(0, enabled))
        assertEquals(2, CarProperties.nextSeatLevel(3, enabled))
        assertEquals(1, CarProperties.nextSeatLevel(2, enabled))
        assertEquals(0, CarProperties.nextSeatLevel(1, enabled))
    }

    @Test
    fun cycle_with_empty_set_always_off() {
        val enabled = emptySet<Int>()
        assertEquals(0, CarProperties.nextSeatLevel(0, enabled))
        assertEquals(0, CarProperties.nextSeatLevel(3, enabled))
        assertEquals(0, CarProperties.nextSeatLevel(1, enabled))
    }

    @Test
    fun cycle_when_current_level_not_enabled_goes_off() {
        // Режим 2 был активен, но 2 отключили (включён только 3) → следующий OFF.
        assertEquals(0, CarProperties.nextSeatLevel(2, setOf(3)))
    }

    @Test
    fun seat_level_encode_matches_reversed_values() {
        // Подогрев
        assertEquals(0x0, CarProperties.encodeSeatLevel(CarProperties.SEAT_HEATING, 0))
        assertEquals(0x10050201, CarProperties.encodeSeatLevel(CarProperties.SEAT_HEATING, 1))
        assertEquals(0x10050202, CarProperties.encodeSeatLevel(CarProperties.SEAT_HEATING, 2))
        assertEquals(0x10050203, CarProperties.encodeSeatLevel(CarProperties.SEAT_HEATING, 3))
        // Вентиляция
        assertEquals(0x10050101, CarProperties.encodeSeatLevel(CarProperties.SEAT_VENTILATION, 1))
        assertEquals(0x10050103, CarProperties.encodeSeatLevel(CarProperties.SEAT_VENTILATION, 3))
    }

    @Test
    fun seat_level_decode_maps_encoded_back_to_level() {
        assertEquals(0, CarProperties.decodeSeatLevel(0x0))
        assertEquals(1, CarProperties.decodeSeatLevel(0x10050201))
        assertEquals(3, CarProperties.decodeSeatLevel(0x10050203))
        assertEquals(2, CarProperties.decodeSeatLevel(0x10050102))
        // auto (0x..0f) и прочее → 0
        assertEquals(0, CarProperties.decodeSeatLevel(0x1005020f))
    }
}
