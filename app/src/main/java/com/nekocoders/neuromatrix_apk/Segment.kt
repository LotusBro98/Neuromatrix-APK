package com.nekocoders.neuromatrix_apk

import java.util.*

class Segment(var name: String,
              var timeSlot: Int)
{
    var curCommand: Command = Command(0, 10, 1, 10)

    fun getDisplayName(): String {
        return name
    }
}