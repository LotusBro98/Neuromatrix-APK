package com.nekocoders.neuromatrix_apk

import android.content.Context
import java.lang.Exception
import java.util.*

class Segment(var name: String,
              var adr1: Int,
              var mask1: IntArray,
              var adr2: Int,
              var mask2: IntArray,
              var timeSlot: Int)
{
    var curCommand: Command = Command(0, 10, 1, 10)

    fun getMaskStr(mask: IntArray): String {
        var str = ""
        for (i in mask) {
            str += "$i;"
        }
        str = str.removeSuffix(";")
        return str
    }

    fun decodeMask(maskStr: String): IntArray {
        val bits = maskStr.split(";")
        var mask = mutableListOf<Int>()
        for (bit in bits) {
            mask.add(bit.toInt())
        }
        return mask.toIntArray()
    }

    fun getDisplayName(): String {
        return "$name    ($adr1: ${getMaskStr(mask1)} | $adr2: ${getMaskStr(mask2)})"
    }

    fun encode(): String {
        var mask1str = getMaskStr(mask1)
        var mask2str = getMaskStr(mask2)
        return "$name\n$adr1\n$mask1str\n$adr2\n$mask2str\n$timeSlot"
    }

    fun decode(str: String) {
        val arr = str.split("\n")
        name = arr[0]
        adr1 = arr[1].toInt()
        mask1 = decodeMask(arr[2])
        adr2 = arr[3].toInt()
        mask2 = decodeMask(arr[4])
        timeSlot = arr[5].toInt()
    }

    fun save(ctx: Context, id: Int) {
        val sharedPref = ctx.getSharedPreferences(PREFERENCE_FILE_KEY, Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("segment_$id", encode())
            commit()
        }
    }

    fun load(ctx: Context, id: Int) {
        val sharedPref = ctx.getSharedPreferences(PREFERENCE_FILE_KEY, Context.MODE_PRIVATE)
        val str = sharedPref.getString("segment_$id", null)
        try {
            decode(str!!)
        } catch (e: Exception) {
            save(ctx, id)
        }
    }
}