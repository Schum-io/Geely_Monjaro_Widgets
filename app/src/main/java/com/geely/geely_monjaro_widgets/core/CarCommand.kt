package com.geely.geely_monjaro_widgets.core

import android.content.BroadcastReceiver
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.geely.geely_monjaro_widgets.service.CarStateService
import com.geely.os.car.IGlyCar

/**
 * Единый путь общения виджетов с машиной.
 *
 * Платформенный `setFunctionValue` возвращает false и НИЧЕГО не делает, если соединение
 * с ECarX ещё не поднято либо функция сейчас `notavailable` — типичная ситуация после
 * долгой стоянки. Поэтому команду повторяем, пока машина её не примет.
 *
 * Авторитет — именно boolean от записи: перечитывать значение сразу после записи
 * бесполезно, CAN-обмен не успевает и вернётся старое. Окончательную сверку делает
 * watcher в [CarStateService].
 */
object CarCommand {

    private const val TAG = "CarCommand"

    private const val MAX_ATTEMPTS = 10

    /** 10 × 500мс ≈ 5с — укладывается в окно goAsync (~10с). */
    private const val RETRY_DELAY_MS = 500L

    /**
     * Повторяет [action] на живом соединении сервиса, пока оно не вернёт не-null;
     * null = нет соединения либо машина команду не приняла. При успехе — [onSuccess],
     * после [MAX_ATTEMPTS] неудач — [onGiveUp]. [pendingResult] завершается ровно один раз.
     */
    fun <T : Any> run(
        context: Context,
        pendingResult: BroadcastReceiver.PendingResult?,
        action: (IGlyCar) -> T?,
        onSuccess: (T) -> Unit,
        onGiveUp: () -> Unit = {},
    ) {
        CarStateService.ensureStarted(context)
        val handler = Handler(Looper.getMainLooper())
        handler.post(object : Runnable {
            private var attempt = 0

            override fun run() {
                attempt++
                val car = CarStateService.liveCar
                val result = if (car == null) null else try {
                    action(car)
                } catch (t: Throwable) {
                    Log.w(TAG, "command failed on attempt $attempt", t)
                    null
                }
                when {
                    result != null -> {
                        onSuccess(result)
                        pendingResult?.finish()
                    }
                    attempt >= MAX_ATTEMPTS -> {
                        onGiveUp()
                        pendingResult?.finish()
                    }
                    else -> handler.postDelayed(this, RETRY_DELAY_MS)
                }
            }
        })
    }

    /**
     * Чтение состояния. Предпочитаем живое соединение сервиса — иначе на каждое
     * обновление от watcher'а создавалось бы и рвалось новое подключение. Разовый
     * [withCar] остаётся запасным путём, пока сервис не поднялся.
     */
    fun read(
        context: Context,
        pendingResult: BroadcastReceiver.PendingResult? = null,
        block: (IGlyCar) -> Unit,
    ) {
        val live = CarStateService.liveCar
        if (live != null) {
            try {
                block(live)
            } catch (t: Throwable) {
                Log.w(TAG, "read on live connection failed", t)
            }
            pendingResult?.finish()
            return
        }
        withCar(context, onDone = { pendingResult?.finish() }) { car -> block(car) }
    }
}
