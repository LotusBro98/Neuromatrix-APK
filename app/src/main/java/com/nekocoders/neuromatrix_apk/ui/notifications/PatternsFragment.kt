package com.nekocoders.neuromatrix_apk.ui.notifications

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nekocoders.neuromatrix_apk.*
import com.nekocoders.neuromatrix_apk.databinding.FragmentPatternsBinding
import com.nekocoders.neuromatrix_apk.ui.home.TimeSlotsRecyclerAdapter
import java.lang.Exception
import kotlin.math.roundToInt


class PatternsFragment : Fragment() {

    private lateinit var patternsViewModel: PatternsViewModel
    private var _binding: FragmentPatternsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    lateinit var commands: MutableList<Command>
    lateinit var adapter: PatternsRecyclerAdapter

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        patternsViewModel =
                ViewModelProvider(this).get(PatternsViewModel::class.java)

        _binding = FragmentPatternsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.recyclerView.layoutManager = LinearLayoutManager(this.context)
        commands = mutableListOf()

        load(requireContext())

        adapter = PatternsRecyclerAdapter(commands)
        binding.recyclerView.adapter = adapter

        binding.buttonAdd.setOnClickListener {
            commands.add(Command(-1, 0, 0, 0))
            adapter.notifyItemInserted(commands.size - 1)
            save(requireContext())
        }

        binding.buttonRemove.setOnClickListener {
            if (commands.size > 0) {
                commands.removeAt(commands.size - 1)
                adapter.notifyItemRemoved(commands.size)
                save(requireContext())
            }
        }

        binding.buttonPlay.setOnClickListener {
            val device = (activity as MainActivity).device
            for (i in commands.indices) {
                val cmd = commands[i]
                cmd.done = false
                adapter.notifyItemChanged(i)
            }

            var callback: Runnable? = null
            for (i in commands.indices) {
                val i1 = commands.size - 1 - i
                val cmd = commands[i1]
                val callb = callback
                callback = Runnable {
                    cmd.execute(requireContext(), device) {
                        cmd.done = true
                        adapter.notifyItemChanged(i1)
                        callb?.run()
                    }
                }
            }
            callback?.run()
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun save(ctx: Context) {
        var data = ""

        for (cmd in commands) {
            data += cmd.encode() + "\n"
        }

        val sharedPref = ctx.getSharedPreferences(PREFERENCE_FILE_KEY, Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("pattern_" + binding.spinner.selectedItemPosition, data)
            commit()
        }
    }

    fun load(ctx: Context) {
        val sharedPref = ctx.getSharedPreferences(PREFERENCE_FILE_KEY, Context.MODE_PRIVATE)
        val data = sharedPref.getString("pattern_" + binding.spinner.selectedItemPosition, null)
        if (data == null) {
            save(ctx)
            return
        }

        commands.clear()
        for (line in data.split("\n")) {
            try {
                val cmd = Command(-1, 0, 1, 0)
                cmd.decode(line)
                commands.add(cmd)
            } catch (e: Exception) {

            }
        }
    }

    inner class PatternsRecyclerAdapter(var commands : List<Command>) : RecyclerView.Adapter<PatternsRecyclerAdapter.PatternViewHolder>() {
        inner class PatternViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            var checkbox: CheckBox
            var spinner_name: Spinner
            var edit_tau: EditText
            var edit_f: EditText
            var edit_t: EditText
            lateinit var cmd: Command

            init {
                val device = (activity as MainActivity).device

                checkbox = itemView.findViewById(R.id.checkBox)
                spinner_name = itemView.findViewById(R.id.spinner_name)
                edit_tau = itemView.findViewById(R.id.edit_tau)
                edit_f = itemView.findViewById(R.id.edit_f)
                edit_t = itemView.findViewById(R.id.edit_t)

                edit_tau.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(
                        s: CharSequence?,
                        start: Int,
                        count: Int,
                        after: Int
                    ) {}

                    override fun onTextChanged(
                        s: CharSequence?,
                        start: Int,
                        before: Int,
                        count: Int
                    ) {}

                    override fun afterTextChanged(s: Editable?) {
                        val text = edit_tau.text.toString()
                        if (text == "")
                            return
                        cmd.tau = text.toInt()
                        save(requireContext())
                    }
                })

                edit_f.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(
                        s: CharSequence?,
                        start: Int,
                        count: Int,
                        after: Int
                    ) {}

                    override fun onTextChanged(
                        s: CharSequence?,
                        start: Int,
                        before: Int,
                        count: Int
                    ) {}

                    override fun afterTextChanged(s: Editable?) {
                        val text = edit_f.text.toString().replace(',', '.')
                        if (text == "")
                            return
                        val freq = text.toDouble()
                        val period = 1000000f / (freq * device.T)
                        cmd.period = period.roundToInt()
                        save(requireContext())
                    }
                })

                edit_t.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(
                        s: CharSequence?,
                        start: Int,
                        count: Int,
                        after: Int
                    ) {}

                    override fun onTextChanged(
                        s: CharSequence?,
                        start: Int,
                        before: Int,
                        count: Int
                    ) {}

                    override fun afterTextChanged(s: Editable?) {
                        val text = edit_t.text.toString()
                        if (text == "")
                            return
                        cmd.duration = text.toInt()
                        save(requireContext())
                    }
                })
            }

            fun update() {
                val device = (activity as MainActivity).device
                if (cmd.channel == -1) {
                    edit_tau.setText("")
                    edit_f.setText("")
                    edit_t.setText(cmd.duration.toString())
                } else {
                    val freq = 1000000f / (cmd.period * device.T)
                    edit_tau.setText(cmd.tau.toString())
                    edit_f.setText(String.format("%.1f", freq))
                    edit_t.setText(cmd.duration.toString())
                }
                checkbox.isChecked = cmd.done
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PatternViewHolder {
            val itemView =
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.recycler_pattern_item, parent, false)

            return PatternViewHolder(itemView)
        }

        override fun onBindViewHolder(holder: PatternViewHolder, position: Int) {
            val device = (activity as MainActivity).device
            val cmd = commands[position]
            holder.checkbox.isChecked = false;
            holder.spinner_name.adapter = ArrayAdapter(requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                android.R.id.text1,
                Array(device.segments.size + 1) { if (it == 0) {"delay"} else {device.segments[it - 1].name}}
            )
            holder.cmd = cmd
            holder.spinner_name.setSelection(cmd.channel + 1, false)
            holder.update()

            holder.spinner_name.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    pos: Int,
                    id: Long
                ) {
                    if (cmd.channel == pos - 1) {
                        return
                    }
                    cmd.channel = pos - 1
                    cmd.load(requireContext())
                    holder.update()
                    save(requireContext())
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                }
            }
        }

        override fun getItemCount() = commands.size
    }
}