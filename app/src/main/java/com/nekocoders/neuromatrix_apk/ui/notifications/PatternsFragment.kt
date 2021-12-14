package com.nekocoders.neuromatrix_apk.ui.notifications

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nekocoders.neuromatrix_apk.Command
import com.nekocoders.neuromatrix_apk.R
import com.nekocoders.neuromatrix_apk.databinding.FragmentPatternsBinding

class PatternsRecyclerAdapter(var commands : List<Command>) : RecyclerView.Adapter<PatternsRecyclerAdapter.PatternViewHolder>() {
    class PatternViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var checkbox: CheckBox;
        var text_name: TextView;
        var text_tau: TextView;
        var text_f: TextView;
        var text_t: TextView;

        init {
            checkbox = itemView.findViewById(R.id.checkBox)
            text_name = itemView.findViewById(R.id.text_name)
            text_tau = itemView.findViewById(R.id.text_tau)
            text_f = itemView.findViewById(R.id.text_f)
            text_t = itemView.findViewById(R.id.text_t)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PatternViewHolder {
        val itemView =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.recycler_pattern_item, parent, false)

        return PatternViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: PatternViewHolder, position: Int) {
        val cmd = commands[position]
        holder.checkbox.isChecked = false;
        holder.text_name.text = holder.itemView.resources.getStringArray(R.array.segments)[cmd.channel];
        holder.text_tau.text = cmd.tau.toString();
        holder.text_f.text = (1000 / (cmd.period)).toString();
        holder.text_t.text = cmd.duration.toString();
    }

    override fun getItemCount() = commands.size
}

class PatternsFragment : Fragment() {

    private lateinit var patternsViewModel: PatternsViewModel
    private var _binding: FragmentPatternsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        patternsViewModel =
                ViewModelProvider(this).get(PatternsViewModel::class.java)

        _binding = FragmentPatternsBinding.inflate(inflater, container, false)
        val root: View = binding.root

//        val textView: TextView = binding.textNotifications
//        patternsViewModel.text.observe(viewLifecycleOwner, Observer {
//            textView.text = it
//        })
        binding.recyclerView.layoutManager = LinearLayoutManager(this.context)
        val commands: MutableList<Command> = mutableListOf();
        commands.add(Command(0, 1, 1, 1))
        commands.add(Command(1, 2, 1, 1))
        commands.add(Command(2, 3, 1, 1))
        commands.add(Command(3, 4, 1, 1))
        binding.recyclerView.adapter = PatternsRecyclerAdapter(commands)
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}