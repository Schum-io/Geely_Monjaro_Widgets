package com.geely.geely_monjaro_widgets.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Поднимает сервис синхронизации после загрузки системы. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            CarStateService.ensureStarted(context)
        }
    }
}
