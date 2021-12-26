package com.nekocoders.neuromatrix_apk

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.util.Log
import androidx.core.os.postDelayed
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.lang.Exception

val PREFERENCE_FILE_KEY = "AppPreference"

val MIN_FREQ_PERIOD_uS = 10000000 // 10 seconds
val MAX_DURATION_MS = 10000 // 10 seconds

class Command(var channel: Int, var tau: Int, var period: Int, var duration: Int, var done: Boolean = false) {
    fun execute(ctx:Context, device: Device, callback: Runnable? = null) {
        if (channel == -1) {
            device.cmd_handler.postDelayed(duration.toLong()) {
                if (callback != null) {
                    device.ui_handler.post(callback)
                }
            }
        } else {
            val segment = device.segments[channel]
            device.set_impulse(ctx, segment.board1, segment.board2, channel, period, tau)
            if (duration != -1) {
                device.cmd_handler.postDelayed(duration.toLong()) {
//                    device.set_impulse(ctx, segment.board1, segment.board2, channel, 1, 0)
//                    device.sendCommand(ctx, 0, byteArrayOf(0), log = false)
                    device.sendCommand(ctx, segment.board1, device.cmd_pulldown(channel))
                    device.sendCommand(ctx, segment.board2, device.cmd_pulldown(channel))
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
            putString("channel_cmd_$channel", encode())
            commit()
        }
    }

    fun load(ctx: Context) {
        val sharedPref = ctx.getSharedPreferences(PREFERENCE_FILE_KEY, MODE_PRIVATE)
        val str = sharedPref.getString("channel_cmd_$channel", null)
        try {
            decode(str!!)
        } catch (e: Exception) {
            save(ctx)
        }
    }

    fun encode(): String {
        return String.format("%d %d %d %d", channel, tau, period, duration)
    }

    fun decode(str: String) {
        val arr = str.split(" ")
        channel = arr[0].toInt()
        tau = arr[1].toInt()
        period = arr[2].toInt()
        duration = arr[3].toInt()
    }
}