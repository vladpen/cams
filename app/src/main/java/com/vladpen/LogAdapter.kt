package com.vladpen

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.vladpen.cams.databinding.LogItemBinding

class LogAdapter(private val dataSet: List<String>) :
    RecyclerView.Adapter<LogAdapter.LineHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LineHolder {
        val binding = LogItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LineHolder(binding)
    }

    override fun onBindViewHolder(holder: LineHolder, idx: Int) {
        val row: String = dataSet[idx]
        holder.bind(row)
    }

    override fun getItemCount(): Int = dataSet.count()

    inner class LineHolder(private val binding: LogItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(row: String) {
            binding.tvItemLine.text = row
        }
    }
}