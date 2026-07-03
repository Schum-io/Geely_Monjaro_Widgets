package com.geely.geely_monjaro_widgets.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import com.geely.geely_monjaro_widgets.R
import com.geely.geely_monjaro_widgets.core.CarProperties
import com.geely.geely_monjaro_widgets.widget.climate.DriverSeatHeatWidgetProvider
import com.geely.geely_monjaro_widgets.widget.climate.DriverSeatVentWidgetProvider
import com.geely.geely_monjaro_widgets.widget.climate.PassengerSeatHeatWidgetProvider
import com.geely.geely_monjaro_widgets.widget.climate.PassengerSeatVentWidgetProvider
import com.geely.geely_monjaro_widgets.widget.climate.SteeringWheelHeatWidgetProvider
import com.geely.geely_monjaro_widgets.widget.seat.DriverSeatMemoryWidgetProvider
import com.geely.geely_monjaro_widgets.widget.seat.PassengerSeatMemoryWidgetProvider
import com.geely.geely_monjaro_widgets.widget.trunk.TrunkWidgetProvider
import com.geely.geely_monjaro_widgets.widget.wipers.WiperServiceWidgetProvider
import com.geely.os.car.ConnectionListener
import com.geely.os.car.GlyCar
import com.geely.os.car.GlyCarValueWatcher
import com.geely.os.car.IGlyCar

/**
 * Фоновый сервис: держит соединение с машиной и подписывается на изменения
 * свойств. При изменении (в т.ч. через штатное меню) обновляет соответствующие
 * виджеты, которые перечитывают своё актуальное состояние.
 *
 * Виджет-провайдер — это broadcast-receiver и не живёт постоянно, поэтому live-
 * синхронизация требует отдельного долгоживущего компонента.
 */
class CarStateService : Service() {

    private var car: IGlyCar? = null

    /** Свойства, за которыми следим, и провайдеры, которые от них зависят. */
    private val watchedProperties = intArrayOf(
        CarProperties.WIPER_SERVICE_POSITION,
        CarProperties.TRUNK_STATE,
        CarProperties.SEAT_HEATING,
        CarProperties.SEAT_VENTILATION,
        CarProperties.STEERING_WHEEL_HEATING,
        CarProperties.SEAT_POSITION_RESTORE,
    )

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundNotification()
        connect()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (car == null) connect()
        if (intent?.action == ACTION_RELAY) relayToProvider(intent)
        return START_STICKY
    }

    /**
     * Доставляет действие клика провайдеру виджета уже в «тёплом» процессе.
     * Запуск через getForegroundService не задерживается системой (в отличие от
     * broadcast в закэшированный процесс), поэтому клики срабатывают сразу.
     */
    private fun relayToProvider(intent: Intent) {
        val target = intent.getStringExtra(EXTRA_TARGET) ?: return
        val relayAction = intent.getStringExtra(EXTRA_RELAY_ACTION) ?: return
        val forward = Intent(relayAction).apply {
            component = ComponentName(this@CarStateService, target)
            intent.extras?.let { putExtras(it) }
        }
        sendBroadcast(forward)
    }

    override fun onDestroy() {
        super.onDestroy()
        liveCar = null
        car?.unregisterValueWatcher()
        car?.disconnect()
        car = null
    }

    private fun connect() {
        car = GlyCar.create(applicationContext, object : ConnectionListener {
            override fun onConnected() {
                val ok = car?.registerValueWatcher(watchedProperties, watcher) ?: false
                liveCar = car
                Log.d(TAG, "watcher registered=$ok")
            }

            override fun onDisConnected() {
                liveCar = null
                Log.w(TAG, "car disconnected")
            }
        })
    }

    private val watcher = GlyCarValueWatcher { propertyId, _ ->
        for (provider in providersFor(propertyId)) {
            updateProvider(provider)
        }
    }

    private fun providersFor(propertyId: Int): List<Class<*>> = when (propertyId) {
        CarProperties.WIPER_SERVICE_POSITION -> listOf(WiperServiceWidgetProvider::class.java)
        CarProperties.TRUNK_STATE -> listOf(TrunkWidgetProvider::class.java)
        CarProperties.SEAT_HEATING -> listOf(
            DriverSeatHeatWidgetProvider::class.java,
            PassengerSeatHeatWidgetProvider::class.java,
        )
        CarProperties.SEAT_VENTILATION -> listOf(
            DriverSeatVentWidgetProvider::class.java,
            PassengerSeatVentWidgetProvider::class.java,
        )
        CarProperties.STEERING_WHEEL_HEATING -> listOf(SteeringWheelHeatWidgetProvider::class.java)
        CarProperties.SEAT_POSITION_RESTORE -> listOf(
            DriverSeatMemoryWidgetProvider::class.java,
            PassengerSeatMemoryWidgetProvider::class.java,
        )
        else -> emptyList()
    }

    /** Триггерит onUpdate провайдера — он перечитает состояние и перерисуется. */
    private fun updateProvider(provider: Class<*>) {
        val manager = AppWidgetManager.getInstance(this)
        val ids = manager.getAppWidgetIds(ComponentName(this, provider))
        if (ids.isEmpty()) return
        val intent = Intent(this, provider).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        }
        sendBroadcast(intent)
    }

    private fun startForegroundNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Состояние машины",
            NotificationManager.IMPORTANCE_MIN,
        )
        manager.createNotificationChannel(channel)
        val notification: Notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Monjaro Widgets")
            .setContentText("Синхронизация состояния виджетов")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    companion object {
        @Volatile
        var liveCar: IGlyCar? = null
            private set

        private const val TAG = "CarStateService"
        private const val CHANNEL_ID = "car_state_sync"
        private const val NOTIFICATION_ID = 1

        private const val ACTION_RELAY = "com.geely.geely_monjaro_widgets.action.RELAY"
        private const val EXTRA_TARGET = "com.geely.geely_monjaro_widgets.extra.TARGET"
        private const val EXTRA_RELAY_ACTION = "com.geely.geely_monjaro_widgets.extra.RELAY_ACTION"

        /** Запускает сервис, если ещё не запущен (idempotent). */
        fun ensureStarted(context: Context) {
            try {
                val intent = Intent(context, CarStateService::class.java)
                context.startForegroundService(intent)
            } catch (t: Throwable) {
                Log.w(TAG, "ensureStarted failed: $t")
            }
        }

        /**
         * PendingIntent клика виджета: запускает foreground-сервис (быстрый путь,
         * греет процесс), который ретранслирует [action] провайдеру [target].
         * [uniqueTag] делает PendingIntent уникальным (filterEquals игнорирует extras).
         */
        fun actionPendingIntent(
            context: Context,
            requestCode: Int,
            target: Class<*>,
            action: String,
            uniqueTag: String,
            extras: Bundle? = null,
        ): PendingIntent {
            val intent = Intent(context, CarStateService::class.java).apply {
                this.action = ACTION_RELAY
                data = Uri.parse("monjarowidget://$uniqueTag")
                putExtra(EXTRA_TARGET, target.name)
                putExtra(EXTRA_RELAY_ACTION, action)
                if (extras != null) putExtras(extras)
            }
            return PendingIntent.getForegroundService(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
    }
}
