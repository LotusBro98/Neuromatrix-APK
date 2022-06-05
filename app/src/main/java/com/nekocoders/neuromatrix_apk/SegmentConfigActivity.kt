package com.nekocoders.neuromatrix_apk

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.view.children
import com.google.android.material.chip.ChipGroup

class SegmentConfigActivity : AppCompatActivity() {
    lateinit var segment: Segment

    lateinit var name_edit: EditText
    lateinit var addr1_edit: EditText
    lateinit var addr2_edit: EditText
    lateinit var mask1_chips: ChipGroup
    lateinit var mask2_chips: ChipGroup
    lateinit var button_save: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_segment_config)

        val args = intent.extras
        val segment_id = args!!.getInt("segment")
        val device = Device(this)
        segment = device.segments[segment_id]

        name_edit = findViewById(R.id.name_edit)
        addr1_edit = findViewById(R.id.addr1_edit)
        addr2_edit = findViewById(R.id.addr2_edit)
        mask1_chips = findViewById(R.id.chipGroup1)
        mask2_chips = findViewById(R.id.chipGroup2)
        button_save = findViewById(R.id.button_save)

        name_edit.text.clear()
        addr1_edit.text.clear()

        name_edit.text.insert(0, segment.name)
        addr1_edit.text.insert(0, segment.adr1.toString())
        addr2_edit.text.insert(0, segment.adr2.toString())

        mask1_chips.clearCheck()
        for (i in segment.mask1) {
            mask1_chips.check(mask1_chips.children.elementAt(i).id)
        }

        mask2_chips.clearCheck()
        for (i in segment.mask2) {
            mask2_chips.check(mask2_chips.children.elementAt(i).id)
        }

        button_save.setOnClickListener {
            val name = name_edit.text.toString()
            val addr1 = addr1_edit.text.toString().toInt()
            val addr2 = addr2_edit.text.toString().toInt()
            var mask1 = mutableListOf<Int>()
            var mask2 = mutableListOf<Int>()

            for (id in mask1_chips.checkedChipIds) {
                val index = mask1_chips.children.indexOf(findViewById(id))
                mask1.add(index)
            }

            for (id in mask2_chips.checkedChipIds) {
                val index = mask2_chips.children.indexOf(findViewById(id))
                mask2.add(index)
            }

            segment.name = name
            segment.adr1 = addr1
            segment.adr2 = addr2
            segment.mask1 = mask1.toIntArray()
            segment.mask2 = mask2.toIntArray()
            segment.save(this, segment_id)

            finish()
        }
    }
}