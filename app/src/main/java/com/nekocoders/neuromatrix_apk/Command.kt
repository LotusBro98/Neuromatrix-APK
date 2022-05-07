package com.nekocoders.neuromatrix_apk

import android.content.Context
import android.content.Context.MODE_PRIVATE
import androidx.core.os.postDelayed
import java.lang.Exception

val PREFERENCE_FILE_KEY = "AppPreference"

val MIN_FREQ_PERIOD_uS = 10000000 // 10 seconds
val MAX_DURATION_MS = 10000 // 10 seconds

class Command(var segment: Int, var tau: Int, var period: Int, var duration: Int, var done: Boolean = false) {
    fun execute(ctx:Context, device: Device, callback: Runnable? = null) {
        if (segment == -1) {
            device.cmd_handler.postDelayed(duration.toLong()) {
                if (callback != null) {
                    device.ui_handler.post(callback)
                }
            }
        } else {
            val segment = device.segments[segment]
            device.set_impulse(ctx, this.segment, period, tau)
            if (duration != -1) {
                device.cmd_handler.postDelayed(duration.toLong()) {
                    device.sendCommand(ctx, device.cmd_clear(this.segment))
                }
            }
            device.cmd_handler.post {
                if (callback != null) {
                    device.ui_handler.post(callback)
                }
            }
        }
    }

    fun save(ctx: Context) {
        val sharedPref = ctx.getSharedPreferences(PREFERENCE_FILE_KEY, MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("channel_cmd_$segment", encode())
            commit()
        }
    }

    fun load(ctx: Context) {
        val sharedPref = ctx.getSharedPreferences(PREFERENCE_FILE_KEY, MODE_PRIVATE)
        val str = sharedPref.getString("channel_cmd_$segment", null)
        try {
            decode(str!!)
        } catch (e: Exception) {
            save(ctx)
        }
    }

    fun encode(): String {
        return String.format("%d %d %d %d", segment, tau, period, duration)
    }

    fun decode(str: String) {
        val arr = str.split(" ")
        segment = arr[0].toInt()
        tau = arr[1].toInt()
        period = arr[2].toInt()
        duration = arr[3].toInt()
    }
}