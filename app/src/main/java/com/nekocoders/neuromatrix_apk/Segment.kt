package com.nekocoders.neuromatrix_apk

import java.util.*

class Segment(var name: String,
              var board1: Int,
              var board2: Int,
              var timeSlot: Int,
              var mask1: Array<Int>,
              var mask2: Array<Int>
              )
{
    var curCommand: Command = Command(0, 10, 1, 10)

    fun getMaskStr(mask: Array<Int>): String {
        var str = ""
        for (i in mask) {
            str += "$i;"
        }
        str = str.removeSuffix(";")
        return str
    }

    fun getDisplayName(): String {
        return "$name    ($board1: ${getMaskStr(mask1)} | $board2: ${getMaskStr(mask2)})"
    }
}