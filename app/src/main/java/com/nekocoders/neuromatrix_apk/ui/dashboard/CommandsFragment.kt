package com.nekocoders.neuromatrix_apk.ui.dashboard

import android.R
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.nekocoders.neuromatrix_apk.databinding.FragmentCommandsBinding
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt
import android.provider.AlarmClock.EXTRA_MESSAGE

import android.widget.EditText

import android.content.Intent
import android.provider.AlarmClock
import androidx.core.view.size
import androidx.fragment.app.FragmentTransaction
import com.nekocoders.neuromatrix_apk.*

val REQUEST_SEG_CONFIG: Int = 11

class CommandsFragment : Fragment() {

    private lateinit var commandsViewModel: CommandsViewModel
    private var _binding: FragmentCommandsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        commandsViewModel =
                ViewModelProvider(this).get(CommandsViewModel::class.java)

        _binding = FragmentCommandsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setupSeekBars()
        setupRadioButtons()

        binding.buttonApplyToAll.setOnClickListener {
            val device = (activity as MainActivity).device
            val channel: Int = binding.commandsRadio.checkedRadioButtonId - 1
            val cmd = device.segments[channel].curCommand
            for (seg in device.segments) {
                seg.curCommand.period = cmd.period
                seg.curCommand.duration = cmd.duration
                seg.curCommand.tau = cmd.tau
                seg.curCommand.save(requireContext())
            }
        }

        binding.buttonExecute.setOnClickListener {
            val device = (activity as MainActivity).device
            val channel: Int = binding.commandsRadio.checkedRadioButtonId - 1
            val cmd = device.segments[channel].curCommand
            cmd.execute(requireContext(), device)
        }

        binding.buttonAdd.setOnClickListener {
            val device = (activity as MainActivity).device
            device.addSegment()
            addSegmentButton(device.segments.last(), device.segments.size - 1)
            showConfigureSegment(device.segments.last())
        }

        binding.buttonRemove.setOnClickListener {
            val device = (activity as MainActivity).device
            if (device.segments.size <= 1)
                return@setOnClickListener
            device.removeSegment()
            binding.commandsRadio.removeViewAt(binding.commandsRadio.size - 1);
        }

        return root
    }

    fun showConfigureSegment(segment: Segment) {
        val intent = Intent(this.context, SegmentConfigActivity::class.java)
        val device = (activity as MainActivity).device
        intent.putExtra("segment", device.segments.indexOf(segment))
        startActivityForResult(intent, REQUEST_SEG_CONFIG)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val main = activity as MainActivity

        main.reloadDevice()
        val ft: FragmentTransaction = requireFragmentManager().beginTransaction()
        ft.detach(this)
        ft.attach(this)
        ft.commit()
    }

    fun addSegmentButton(segment: Segment, i: Int) {
        val button = RadioButton(this.context)
        button.setPadding(24, 24, 24, 24)
        button.text = segment.getDisplayName()
        button.id = i + 1
        button.setOnLongClickListener {
            showConfigureSegment(segment)
            true
        }
        binding.commandsRadio.addView(button)
    }

    @SuppressLint("ResourceType")
    fun setupRadioButtons() {
        for (i in (activity as MainActivity).device.segments.indices) {
            val segment = (activity as MainActivity).device.segments[i]
            addSegmentButton(segment, i)
        }

        binding.commandsRadio.setOnCheckedChangeListener(object : RadioGroup.OnCheckedChangeListener {
            override fun onCheckedChanged(group: RadioGroup?, checkedId: Int) {
                val device = (activity as MainActivity).device
                val channel: Int = binding.commandsRadio.checkedRadioButtonId - 1
                val tau = device.segments[channel].curCommand.tau
                val period = device.segments[channel].curCommand.period
                val duration = device.segments[channel].curCommand.duration
                val maxTau = device.getMaxTau(channel)
                val T = device.T
                val Tmax = MIN_FREQ_PERIOD_uS / T.toDouble()
                val t_max = MAX_DURATION_MS * 1000 / T.toDouble()

                val x_tau = tau.toDouble() / maxTau.toDouble()
                val x_period = 1 - (ln(period.toDouble()) / ln(Tmax))
                val x_duration = ln(duration.toDouble() * 1000 / T) / ln(t_max)

                binding.seekBarTau.progress = 0
                binding.seekBarTau.progress = binding.seekBarTau.max
                binding.seekBarTau.progress = (x_tau * binding.seekBarTau.max).roundToInt()

                binding.seekBarF.progress = 0
                binding.seekBarF.progress = binding.seekBarF.max
                binding.seekBarF.progress = (x_period * binding.seekBarF.max).roundToInt()

                binding.seekBarT.progress = 0
                binding.seekBarT.progress = binding.seekBarT.max
                if (duration != -1) {
                    binding.seekBarT.progress = (x_duration * binding.seekBarT.max).roundToInt()
                }
            }
        })

        binding.commandsRadio.check(1)
    }

    fun setupSeekBars() {
        binding.seekBarTau.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                val x: Double = progress.toDouble() / seekBar.max.toDouble()
                val device = (activity as MainActivity).device
                val channel: Int = binding.commandsRadio.checkedRadioButtonId - 1
                val maxTau = device.getMaxTau(channel)
                val tau = (x * maxTau).toInt()
                device.segments[channel].curCommand.tau = tau
                device.segments[channel].curCommand.save(requireContext())
                binding.tauView.text = tau.toString() + " мкс"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        binding.seekBarF.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                val x: Double = 1 - progress.toDouble() / seekBar.max.toDouble()
                val channel: Int = binding.commandsRadio.checkedRadioButtonId - 1
                val device = (activity as MainActivity).device
                val T = device.T
                val Tmax = MIN_FREQ_PERIOD_uS / T.toDouble()
                val period = Tmax.pow(x).toInt()
                device.segments[channel].curCommand.period = period
                device.segments[channel].curCommand.save(requireContext())
                val freq = 1000000f / (period * T)
                binding.fView.text = String.format("%.1f", freq) + " Гц"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        binding.seekBarT.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                val x: Double = progress.toDouble() / seekBar.max.toDouble()
                val channel: Int = binding.commandsRadio.checkedRadioButtonId - 1
                val device = (activity as MainActivity).device
                val T = device.T
                val t_max = MAX_DURATION_MS * 1000 / T.toDouble()
                if (progress == seekBar.max) {
                    val duration = -1
                    device.segments[channel].curCommand.duration = duration
                    device.segments[channel].curCommand.save(requireContext())
                    binding.tView.text = "∞"
                } else {
                    val duration = (T * t_max.pow(x) / 1000).toInt()
                    device.segments[channel].curCommand.duration = duration
                    device.segments[channel].curCommand.save(requireContext())
                    binding.tView.text = duration.toString() + " мс"
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}