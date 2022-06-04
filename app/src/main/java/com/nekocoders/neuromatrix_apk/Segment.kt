package com.nekocoders.neuromatrix_apk

import java.util.*

class Segment(var name: String,
              var adr1: Int,
              var mask1: IntArray,
              var adr2: Int,
              var mask2: IntArray,
              var timeSlot: Int)
{
    var curCommand: Command = Command(0, 10, 1, 10)

    fun getDisplayName(): String {
        return "$name $adr1:$mask1 -- $adr2:$mask2"
    }
}