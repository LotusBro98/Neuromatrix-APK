package com.nekocoders.neuromatrix_apk

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import androidx.core.os.postDelayed

private val PREFERENCE_FILE_KEY = "AppPreference"

val MIN_FREQ_PERIOD_uS = 10000000 // 10 seconds
val MAX_DURATION_MS = 10000 // 10 seconds

class Command(var channel: Int, var tau: Int, var period: Int, var duration: Int ) {
    fun execute(ctx:Context, device: Device) {
        val segment = device.segments[channel]
        device.set_impulse(ctx, segment.board1, segment.board2, channel, period, tau)
        device.cmd_handler.postDelayed(duration.toLong()) {
//            device.set_impulse(ctx, segment.board1, segment.board2, channel, 1, 0)
            device.postCommand(ctx, 0, byteArrayOf(0), log=false)
            device.postCommand(ctx, segment.board1, device.cmd_pulldown(channel))
            device.postCommand(ctx, segment.board2, device.cmd_pulldown(channel))
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
        if (str != null) {
            decode(str)
        } else {
            save(ctx)
        }
    }

    fun encode(): String {
        return String.format("%d %d %d", tau, period, duration)
    }

    fun decode(str: String) {
        val arr = str.split(" ")
        tau = arr[0].toInt()
        period = arr[1].toInt()
        duration = arr[2].toInt()
    }
}