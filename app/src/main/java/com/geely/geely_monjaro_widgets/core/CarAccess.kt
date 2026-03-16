package com.geely.geely_monjaro_widgets.core

import android.content.Context
import com.geely.os.car.ConnectionListener
import com.geely.os.car.GlyCar
import com.geely.os.car.IGlyCar
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Создаёт подключение к GlyCar, выполняет [block] после установки соединения,
 * отключается и вызывает [onDone]. Гарантирует, что [onDone] вызывается ровно один раз.
 *
 */
fun withCar(context: Context, onDone: () -> Unit = {}, block: (IGlyCar) -> Unit) {
    val finished = AtomicBoolean(false)
    val finish = { if (finished.compareAndSet(false, true)) onDone() }
    var car: IGlyCar? = null
    car = GlyCar.create(context.applicationContext, object : ConnectionListener {
        override fun onConnected() {
            try {
                block(car!!)
            } catch (_: Exception) {
            } finally {
                car?.disconnect()
                finish()
            }
        }

        override fun onDisConnected() {
            finish()
        }
    })
}
