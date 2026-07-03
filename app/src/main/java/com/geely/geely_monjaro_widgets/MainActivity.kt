package com.geely.geely_monjaro_widgets

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.geely.geely_monjaro_widgets.core.CarProperties
import com.geely.geely_monjaro_widgets.core.ModeConfig
import com.geely.geely_monjaro_widgets.ui.theme.Geely_Monjaro_WidgetsTheme
import com.geely.os.car.ConnectionListener
import com.geely.os.car.GlyCar
import com.geely.os.car.IGlyCar

/**
 * Экран-хаб управления функциями машины. Дублирует возможности виджетов.
 * Соединение с машиной держится, пока экран открыт (connect в onResume,
 * disconnect в onPause) — как в исходном проекте Geely_Monjaro_Wipers.
 */
class MainActivity : ComponentActivity() {

    private var car: IGlyCar? = null

    private val connected = mutableStateOf(false)
    private val wiperServiceOn = mutableStateOf(false)
    private val trunkOpen = mutableStateOf(false)
    private val heatDriverLevel = mutableStateOf(0)
    private val heatPassengerLevel = mutableStateOf(0)
    private val ventDriverLevel = mutableStateOf(0)
    private val ventPassengerLevel = mutableStateOf(0)
    private val steeringHeatLevel = mutableStateOf(0)
    // Включённые режимы цикла (1..3) — раздельно на каждое место. По умолчанию все три;
    // реальные значения читаются из ModeConfig в onCreate.
    private val heatDriverModes = mutableStateOf(setOf(1, 2, 3))
    private val heatPassengerModes = mutableStateOf(setOf(1, 2, 3))
    private val ventDriverModes = mutableStateOf(setOf(1, 2, 3))
    private val ventPassengerModes = mutableStateOf(setOf(1, 2, 3))
    private val steeringHeatModes = mutableStateOf(setOf(1, 2, 3))
    private val recircOn = mutableStateOf(false)
    private val rearDefrostOn = mutableStateOf(false)
    private val fuelPercent = mutableStateOf(-1)
    private val fuelLiters = mutableStateOf(-1)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.geely.geely_monjaro_widgets.service.CarStateService.ensureStarted(this)
        loadModeConfig()
        enableEdgeToEdge()
        setContent {
            Geely_Monjaro_WidgetsTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ControlScreen(
                        modifier = Modifier.padding(innerPadding),
                        connected = connected.value,
                        wiperServiceOn = wiperServiceOn.value,
                        trunkOpen = trunkOpen.value,
                        onWiperToggle = ::onWiperToggle,
                        onTrunkOpen = { onTrunkSet(open = true) },
                        onTrunkClose = { onTrunkSet(open = false) },
                        onDriverProfile = { applySeatProfile(CarProperties.AREA_SEAT_DRIVER, it) },
                        onPassengerProfile = { applySeatProfile(CarProperties.AREA_SEAT_PASSENGER, it) },
                        heatDriverLevel = heatDriverLevel.value,
                        heatPassengerLevel = heatPassengerLevel.value,
                        ventDriverLevel = ventDriverLevel.value,
                        ventPassengerLevel = ventPassengerLevel.value,
                        onHeatDriver = { cycleClimate(CarProperties.SEAT_HEATING, CarProperties.AREA_SEAT_DRIVER, heatDriverLevel) },
                        onHeatPassenger = { cycleClimate(CarProperties.SEAT_HEATING, CarProperties.AREA_SEAT_PASSENGER, heatPassengerLevel) },
                        onVentDriver = { cycleClimate(CarProperties.SEAT_VENTILATION, CarProperties.AREA_SEAT_DRIVER, ventDriverLevel) },
                        onVentPassenger = { cycleClimate(CarProperties.SEAT_VENTILATION, CarProperties.AREA_SEAT_PASSENGER, ventPassengerLevel) },
                        heatDriverModes = heatDriverModes.value,
                        heatPassengerModes = heatPassengerModes.value,
                        ventDriverModes = ventDriverModes.value,
                        ventPassengerModes = ventPassengerModes.value,
                        onHeatDriverModes = { updateModes(CarProperties.SEAT_HEATING, CarProperties.AREA_SEAT_DRIVER, heatDriverModes, it) },
                        onHeatPassengerModes = { updateModes(CarProperties.SEAT_HEATING, CarProperties.AREA_SEAT_PASSENGER, heatPassengerModes, it) },
                        onVentDriverModes = { updateModes(CarProperties.SEAT_VENTILATION, CarProperties.AREA_SEAT_DRIVER, ventDriverModes, it) },
                        onVentPassengerModes = { updateModes(CarProperties.SEAT_VENTILATION, CarProperties.AREA_SEAT_PASSENGER, ventPassengerModes, it) },
                        steeringHeatLevel = steeringHeatLevel.value,
                        onHeatSteering = ::cycleSteeringHeat,
                        steeringHeatModes = steeringHeatModes.value,
                        onSteeringHeatModes = { updateModes(CarProperties.STEERING_WHEEL_HEATING, null, steeringHeatModes, it) },
                        recircOn = recircOn.value,
                        onRecircToggle = ::onRecircToggle,
                        rearDefrostOn = rearDefrostOn.value,
                        onRearDefrostToggle = ::onRearDefrostToggle,
                        fuelPercent = fuelPercent.value,
                        fuelLiters = fuelLiters.value,
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        car = GlyCar.create(this, object : ConnectionListener {
            override fun onConnected() = runOnUiThread {
                connected.value = true
                readState()
            }

            override fun onDisConnected() = runOnUiThread {
                connected.value = false
            }
        })
    }

    override fun onPause() {
        super.onPause()
        car?.disconnect()
        car = null
        connected.value = false
    }

    private fun readState() {
        val c = car ?: return
        wiperServiceOn.value =
            c.getIntProperty(CarProperties.WIPER_SERVICE_POSITION, CarProperties.AREA_FRONT_WIPER) == 1
        val state = c.getIntProperty(CarProperties.TRUNK_STATE, CarProperties.AREA_TRUNK)
        trunkOpen.value = CarProperties.isTrunkOpenish(state)
        heatDriverLevel.value =
            CarProperties.decodeSeatLevel(c.getIntProperty(CarProperties.SEAT_HEATING, CarProperties.AREA_SEAT_DRIVER))
        heatPassengerLevel.value =
            CarProperties.decodeSeatLevel(c.getIntProperty(CarProperties.SEAT_HEATING, CarProperties.AREA_SEAT_PASSENGER))
        ventDriverLevel.value =
            CarProperties.decodeSeatLevel(c.getIntProperty(CarProperties.SEAT_VENTILATION, CarProperties.AREA_SEAT_DRIVER))
        ventPassengerLevel.value =
            CarProperties.decodeSeatLevel(c.getIntProperty(CarProperties.SEAT_VENTILATION, CarProperties.AREA_SEAT_PASSENGER))
        steeringHeatLevel.value =
            CarProperties.decodeSeatLevel(c.getIntProperty(CarProperties.STEERING_WHEEL_HEATING))
        recircOn.value =
            c.getIntProperty(CarProperties.AIR_CIRCULATION) == CarProperties.CIRCULATION_INNER
        rearDefrostOn.value = c.getIntProperty(CarProperties.DEFROST_REAR) == 1
        val fuel = c.getSensorValue(CarProperties.SENSOR_FUEL_PERCENTAGE)
        fuelPercent.value = if (fuel > 0f) Math.round(fuel) else -1
        val liters = CarProperties.fuelLiters(
            c.getSensorValue(CarProperties.SENSOR_FUEL_LEVEL), fuel, c.getFuelTankCapacityLiters()
        )
        fuelLiters.value = if (liters > 0f) Math.round(liters) else -1
    }

    private fun onRecircToggle() {
        val c = car ?: return
        val active = c.getIntProperty(CarProperties.AIR_CIRCULATION) == CarProperties.CIRCULATION_INNER
        c.setIntProperty(
            CarProperties.AIR_CIRCULATION,
            if (active) CarProperties.CIRCULATION_OUTSIDE else CarProperties.CIRCULATION_INNER
        )
        recircOn.value =
            c.getIntProperty(CarProperties.AIR_CIRCULATION) == CarProperties.CIRCULATION_INNER
    }

    private fun onRearDefrostToggle() {
        val c = car ?: return
        val active = c.getIntProperty(CarProperties.DEFROST_REAR) == 1
        c.setIntProperty(CarProperties.DEFROST_REAR, if (active) 0 else 1)
        rearDefrostOn.value = c.getIntProperty(CarProperties.DEFROST_REAR) == 1
    }

    private fun cycleSteeringHeat() {
        val c = car ?: return
        val current = CarProperties.decodeSeatLevel(c.getIntProperty(CarProperties.STEERING_WHEEL_HEATING))
        val enabled = ModeConfig.enabledLevels(this, CarProperties.STEERING_WHEEL_HEATING, null)
        val next = CarProperties.nextSeatLevel(current, enabled)
        c.setIntProperty(CarProperties.STEERING_WHEEL_HEATING, CarProperties.encodeSeatLevel(CarProperties.STEERING_WHEEL_HEATING, next))
        // Реальное состояние: при отклонённой команде (двигатель заглушён) уровень не изменится.
        steeringHeatLevel.value = CarProperties.decodeSeatLevel(c.getIntProperty(CarProperties.STEERING_WHEEL_HEATING))
    }

    private fun cycleClimate(
        propertyId: Int,
        areaId: Int,
        level: androidx.compose.runtime.MutableState<Int>
    ) {
        val c = car ?: return
        val current = CarProperties.decodeSeatLevel(c.getIntProperty(propertyId, areaId))
        val enabled = ModeConfig.enabledLevels(this, propertyId, areaId)
        val next = CarProperties.nextSeatLevel(current, enabled)
        c.setIntProperty(propertyId, areaId, CarProperties.encodeSeatLevel(propertyId, next))
        // Реальное состояние: при отклонённой команде (двигатель заглушён) уровень не изменится.
        level.value = CarProperties.decodeSeatLevel(c.getIntProperty(propertyId, areaId))
    }

    /** Читает сохранённые наборы режимов в состояние экрана. */
    private fun loadModeConfig() {
        heatDriverModes.value =
            ModeConfig.enabledLevels(this, CarProperties.SEAT_HEATING, CarProperties.AREA_SEAT_DRIVER)
        heatPassengerModes.value =
            ModeConfig.enabledLevels(this, CarProperties.SEAT_HEATING, CarProperties.AREA_SEAT_PASSENGER)
        ventDriverModes.value =
            ModeConfig.enabledLevels(this, CarProperties.SEAT_VENTILATION, CarProperties.AREA_SEAT_DRIVER)
        ventPassengerModes.value =
            ModeConfig.enabledLevels(this, CarProperties.SEAT_VENTILATION, CarProperties.AREA_SEAT_PASSENGER)
        steeringHeatModes.value =
            ModeConfig.enabledLevels(this, CarProperties.STEERING_WHEEL_HEATING, null)
    }

    /** Сохраняет новый набор режимов и обновляет состояние экрана. */
    private fun updateModes(
        propertyId: Int,
        areaId: Int?,
        state: androidx.compose.runtime.MutableState<Set<Int>>,
        levels: Set<Int>,
    ) {
        // Защита: минимум один режим должен остаться включённым.
        if (levels.isEmpty()) return
        ModeConfig.setEnabledLevels(this, propertyId, areaId, levels)
        state.value = levels
    }

    private fun onWiperToggle() {
        val c = car ?: return
        val next = if (wiperServiceOn.value) 0 else 1
        c.setIntProperty(CarProperties.WIPER_SERVICE_POSITION, CarProperties.AREA_FRONT_WIPER, next)
        wiperServiceOn.value = next == 1
    }

    private fun onTrunkSet(open: Boolean) {
        val c = car ?: return
        val command = if (open) CarProperties.TRUNK_OPEN else CarProperties.TRUNK_CLOSE
        c.setIntProperty(CarProperties.TRUNK_COMMAND, CarProperties.AREA_TRUNK, command)
        trunkOpen.value = open
    }

    private fun applySeatProfile(areaId: Int, profileIndex: Int) {
        val c = car ?: return
        c.setIntProperty(
            CarProperties.SEAT_POSITION_RESTORE,
            areaId,
            CarProperties.seatProfileValue(profileIndex)
        )
    }
}

@Composable
private fun ControlScreen(
    modifier: Modifier = Modifier,
    connected: Boolean,
    wiperServiceOn: Boolean,
    trunkOpen: Boolean,
    onWiperToggle: () -> Unit,
    onTrunkOpen: () -> Unit,
    onTrunkClose: () -> Unit,
    onDriverProfile: (Int) -> Unit,
    onPassengerProfile: (Int) -> Unit,
    heatDriverLevel: Int,
    heatPassengerLevel: Int,
    ventDriverLevel: Int,
    ventPassengerLevel: Int,
    onHeatDriver: () -> Unit,
    onHeatPassenger: () -> Unit,
    onVentDriver: () -> Unit,
    onVentPassenger: () -> Unit,
    heatDriverModes: Set<Int>,
    heatPassengerModes: Set<Int>,
    ventDriverModes: Set<Int>,
    ventPassengerModes: Set<Int>,
    onHeatDriverModes: (Set<Int>) -> Unit,
    onHeatPassengerModes: (Set<Int>) -> Unit,
    onVentDriverModes: (Set<Int>) -> Unit,
    onVentPassengerModes: (Set<Int>) -> Unit,
    steeringHeatLevel: Int,
    onHeatSteering: () -> Unit,
    steeringHeatModes: Set<Int>,
    onSteeringHeatModes: (Set<Int>) -> Unit,
    recircOn: Boolean,
    onRecircToggle: () -> Unit,
    rearDefrostOn: Boolean,
    onRearDefrostToggle: () -> Unit,
    fuelPercent: Int,
    fuelLiters: Int,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringRes(R.string.app_name),
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = if (connected) "Подключено к машине" else "Нет соединения с машиной",
            style = MaterialTheme.typography.bodySmall,
        )

        // Дворники
        FunctionCard(title = stringRes(R.string.wiper_service_label)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = if (wiperServiceOn) stringRes(R.string.wiper_service_on) else stringRes(R.string.wiper_service_off))
                Switch(checked = wiperServiceOn, onCheckedChange = { onWiperToggle() }, enabled = connected)
            }
        }

        // Багажник
        FunctionCard(title = stringRes(R.string.trunk_label)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(onClick = onTrunkOpen, enabled = connected) { Text("Открыть") }
                OutlinedButton(onClick = onTrunkClose, enabled = connected) { Text("Закрыть") }
            }
        }

        // Память сиденья водителя (2 профиля)
        FunctionCard(title = stringRes(R.string.seat_memory_driver)) {
            ProfileRow(enabled = connected, count = 2, onProfile = onDriverProfile)
        }

        // Память сиденья пассажира (3 профиля)
        FunctionCard(title = stringRes(R.string.seat_memory_passenger)) {
            ProfileRow(enabled = connected, count = 3, onProfile = onPassengerProfile)
        }

        // Подогрев / вентиляция (цикл по включённым режимам, см. чипы в карточке)
        FunctionCard(title = stringRes(R.string.heat_driver_label)) {
            ClimateRow(enabled = connected, level = heatDriverLevel, onCycle = onHeatDriver,
                modes = heatDriverModes, onModesChange = onHeatDriverModes)
        }
        FunctionCard(title = stringRes(R.string.heat_passenger_label)) {
            ClimateRow(enabled = connected, level = heatPassengerLevel, onCycle = onHeatPassenger,
                modes = heatPassengerModes, onModesChange = onHeatPassengerModes)
        }
        FunctionCard(title = stringRes(R.string.vent_driver_label)) {
            ClimateRow(enabled = connected, level = ventDriverLevel, onCycle = onVentDriver,
                modes = ventDriverModes, onModesChange = onVentDriverModes)
        }
        FunctionCard(title = stringRes(R.string.vent_passenger_label)) {
            ClimateRow(enabled = connected, level = ventPassengerLevel, onCycle = onVentPassenger,
                modes = ventPassengerModes, onModesChange = onVentPassengerModes)
        }
        FunctionCard(title = stringRes(R.string.heat_steering_label)) {
            ClimateRow(enabled = connected, level = steeringHeatLevel, onCycle = onHeatSteering,
                modes = steeringHeatModes, onModesChange = onSteeringHeatModes)
        }

        // Рециркуляция
        FunctionCard(title = stringRes(R.string.recirc_label)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = if (recircOn) "Включена" else "Выключена")
                Switch(checked = recircOn, onCheckedChange = { onRecircToggle() }, enabled = connected)
            }
        }

        // Обогрев заднего стекла
        FunctionCard(title = stringRes(R.string.rear_defrost_label)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = if (rearDefrostOn) "Включён" else "Выключен")
                Switch(checked = rearDefrostOn, onCheckedChange = { onRearDefrostToggle() }, enabled = connected)
            }
        }

        // Топливо (только показ)
        FunctionCard(title = stringRes(R.string.fuel_label)) {
            val pctText = if (fuelPercent >= 0) "$fuelPercent%" else "—"
            val text = if (fuelLiters >= 0) "$pctText · $fuelLiters л" else pctText
            Text(text = text)
        }

        AboutCard()
    }
}

@Composable
private fun AboutCard() {
    val context = LocalContext.current
    FunctionCard(title = stringRes(R.string.about_title)) {
        Text(text = stringRes(R.string.about_author))
        Text(
            text = "github.com/Schum-io",
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Schum-io"))
                )
            },
        )
    }
}

@Composable
private fun ClimateRow(
    enabled: Boolean,
    level: Int,
    onCycle: () -> Unit,
    modes: Set<Int>,
    onModesChange: (Set<Int>) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = if (level == 0) "Выключено" else "Уровень $level")
            Button(onClick = onCycle, enabled = enabled) { Text("Переключить") }
        }
        ModeChips(modes = modes, onModesChange = onModesChange)
    }
}

/**
 * Чипы выбора режимов (1/2/3), участвующих в цикле. Нельзя снять последний включённый —
 * хотя бы один режим всегда доступен.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModeChips(modes: Set<Int>, onModesChange: (Set<Int>) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = "Режимы:", style = MaterialTheme.typography.bodySmall)
        for (levelValue in 1..CarProperties.SEAT_LEVEL_MAX) {
            val selected = levelValue in modes
            FilterChip(
                selected = selected,
                onClick = {
                    val next = if (selected) modes - levelValue else modes + levelValue
                    if (next.isNotEmpty()) onModesChange(next)
                },
                label = { Text("$levelValue") },
            )
        }
    }
}

@Composable
private fun ProfileRow(enabled: Boolean, count: Int, onProfile: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (i in 0 until count) {
            Button(onClick = { onProfile(i) }, enabled = enabled) { Text("${i + 1}") }
        }
    }
}

@Composable
private fun FunctionCard(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun stringRes(id: Int): String = androidx.compose.ui.res.stringResource(id)
