package com.nekocoders.neuromatrix_apk.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Spinner
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nekocoders.neuromatrix_apk.R
import com.nekocoders.neuromatrix_apk.databinding.FragmentInitBinding
import androidx.recyclerview.widget.DividerItemDecoration
import com.nekocoders.neuromatrix_apk.MainActivity


class TimeSlotsRecyclerAdapter : RecyclerView.Adapter<TimeSlotsRecyclerAdapter.TimeSlotViewHolder>() {
    class TimeSlotViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var combobox: Spinner;

        init {
            combobox = itemView.findViewById(R.id.spinner_func)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimeSlotViewHolder {
        val itemView =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.recycler_time_slot, parent, false)

        return TimeSlotViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: TimeSlotViewHolder, position: Int) {
//        val adapter = ArrayAdapter.createFromResource(holder.itemView.context, R.array.animals, android.R.layout.simple_spinner_item);
//        holder.combobox.adapter = adapter;
    }

    override fun getItemCount() = 5
}

class BoardsRecyclerAdapter :
    RecyclerView.Adapter<BoardsRecyclerAdapter.BoardViewHolder>() {

    class BoardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var slots: RecyclerView;

        init {
            slots = itemView.findViewById(R.id.slots_recycler)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BoardViewHolder {
        val itemView =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.recycler_board_config, parent, false)

        return BoardViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: BoardViewHolder, position: Int) {
        holder.slots.layoutManager = LinearLayoutManager(holder.itemView.context, LinearLayoutManager.HORIZONTAL, false);
        holder.slots.adapter = TimeSlotsRecyclerAdapter();
        holder.slots.addItemDecoration(
            DividerItemDecoration(
                holder.slots.getContext(),
                DividerItemDecoration.HORIZONTAL
            )
        )
    }

    override fun getItemCount() = 3
}

class InitFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true;
    }

    private lateinit var initViewModel: InitViewModel
    private var _binding: FragmentInitBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        initViewModel =
                ViewModelProvider(this).get(InitViewModel::class.java)

        _binding = FragmentInitBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val main = activity as MainActivity
        main.device.cmd_log_view = binding.commandLog

        binding.button.setOnClickListener {
            if (main.device.socket == null || !main.device.socket!!.isConnected) {
                main.device.connect(main)
            } else {
                main.device.initialize(main)
            }
        }

        binding.commandLog.text = (activity as MainActivity).device.cmd_log

        return root
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}