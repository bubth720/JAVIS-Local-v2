package com.stark.jarvislocal

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager

internal data class MvpDeviceState(val lowMemory: Boolean, val availableBytes: Long, val lowRam: Boolean,
    val batteryPercent: Int, val charging: Boolean, val temperatureTenths: Int, val thermalStatus: Int?)
internal object MvpSafety {
    fun check(bytes: Long, state: MvpDeviceState) {
        require(!state.lowMemory && !state.lowRam && state.availableBytes >= maxOf(1024L * 1024 * 1024, bytes * 3 + 256L * 1024 * 1024)) {
            "Mémoire disponible insuffisante pour le MVP" }
        require(state.batteryPercent in 0..100 && (state.charging || state.batteryPercent > 20)) { "Batterie faible ou inconnue" }
        require(state.temperatureTenths in 1..399) { "Température batterie élevée ou inconnue" }
        require(state.thermalStatus == null || state.thermalStatus in 0..1) { "Limitation thermique Android : opération refusée" }
    }
    fun read(context: Context): MvpDeviceState {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memory = ActivityManager.MemoryInfo(); am.getMemoryInfo(memory)
        val battery = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = battery?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = battery?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val status = battery?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val thermal = if (Build.VERSION.SDK_INT >= 29)
            (context.getSystemService(Context.POWER_SERVICE) as PowerManager).currentThermalStatus else null
        return MvpDeviceState(memory.lowMemory, memory.availMem, am.isLowRamDevice,
            if (level >= 0 && scale > 0) ((level.toLong() * 100 / scale).toInt()) else -1,
            status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL,
            battery?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1, thermal)
    }
}
