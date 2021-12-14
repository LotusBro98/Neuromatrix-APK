package com.nekocoders.neuromatrix_apk.ui.dashboard

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
import com.nekocoders.neuromatrix_apk.MAX_DURATION_MS
import com.nekocoders.neuromatrix_apk.MIN_FREQ_PERIOD_uS
import com.nekocoders.neuromatrix_apk.MainActivity
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt

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

        return root
    }

    @SuppressLint("ResourceType")
    fun setupRadioButtons() {
        for (i in (activity as MainActivity).device.segments.indices) {
            val segment = (activity as MainActivity).device.segments[i]
            val button = RadioButton(this.context)
            button.setPadding(24, 24, 24, 24)
            button.text = segment.name
            button.id = i + 1
            binding.commandsRadio.addView(button)
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
                binding.seekBarT.progress = (x_duration * binding.seekBarT.max).roundToInt()

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
                val duration = (T * t_max.pow(x) / 1000).toInt()
                device.segments[channel].curCommand.duration = duration
                device.segments[channel].curCommand.save(requireContext())
                binding.tView.text = duration.toString() + " мс"
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