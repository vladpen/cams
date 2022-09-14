package com.vladpen

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.vladpen.cams.databinding.LogItemBinding

class LogAdapter(private val dataSet: List<String>) : RecyclerView.Adapter<LogAdapter.Holder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = LogItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val row: String = dataSet[position]
        holder.bind(row)
    }

    override fun getItemCount(): Int = dataSet.count()

    inner class Holder(private val binding: LogItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(row: String) {
            binding.tvItemLine.text = row
        }
    }
}