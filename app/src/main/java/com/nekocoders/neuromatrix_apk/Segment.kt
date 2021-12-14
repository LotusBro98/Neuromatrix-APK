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
}