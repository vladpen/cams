package com.vladpen

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.vladpen.cams.*
import com.vladpen.cams.databinding.GroupItemBinding

class GroupAdapter(var dataSet: List<Int>, private val context: GroupEditActivity) :
    RecyclerView.Adapter<GroupAdapter.Holder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = GroupItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int = dataSet.count()

    fun moveItem(from: Int, to: Int) {
        context.moveItem(from, to)
    }

    inner class Holder(private val binding: GroupItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(position: Int) {
            binding.tvItemName.text = StreamData.getById(dataSet[position])?.name
            binding.tvItemName.setTextColor(context.getColor(R.color.text))
            binding.btnRemove.setOnClickListener {
                context.removeAt(bindingAdapterPosition)
                notifyItemRemoved(bindingAdapterPosition)
            }
        }
    }
}