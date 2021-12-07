package com.nekocoders.neuromatrix_apk.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.nekocoders.neuromatrix_apk.R
import com.nekocoders.neuromatrix_apk.databinding.FragmentCommandsBinding

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

//        val textView: TextView = binding.textDashboard
//        commandsViewModel.text.observe(viewLifecycleOwner, Observer {
//            textView.text = it
//        })
        val commands = this.resources.getStringArray(R.array.segments)
        for (com in commands) {
            val button = RadioButton(this.context)
            button.text = com
            button.setPadding(24, 24, 24, 24)
            binding.commandsRadio.addView(button)
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}