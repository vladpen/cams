package com.vladpen

import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.vladpen.cams.VideoActivity
import com.vladpen.cams.EditActivity
import com.vladpen.cams.databinding.MainItemBinding

class StreamsAdapter(private val dataSet: List<StreamDataModel>) :
    RecyclerView.Adapter<StreamsAdapter.StreamHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StreamHolder {
        val binding = MainItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return StreamHolder(parent.context, binding)
    }

    override fun onBindViewHolder(holder: StreamHolder, idx: Int) {
        val row: StreamDataModel = dataSet[idx]
        holder.bind(idx, row)
    }

    override fun getItemCount(): Int = dataSet.count()

    fun moveItem(context: Context, from: Int, to: Int) {
        StreamData.moveItem(context, from, to)
    }

    inner class StreamHolder(private val context: Context, private val binding: MainItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(idx: Int, row: StreamDataModel) {
            with(binding) {
                tvItemName.text = row.name
                tvItemName.setOnClickListener {
                    val intent = Intent(context, VideoActivity::class.java)
                    navigate(intent, idx)
                }
                btnEdit.setOnClickListener {
                    val intent = Intent(context, EditActivity::class.java)
                    navigate(intent, idx)
                }
            }
        }

        private fun navigate(intent: Intent,  idx: Int) {
            intent.setFlags(FLAG_ACTIVITY_NEW_TASK).putExtra("streamId", idx)
            context.startActivity(intent)
        }
    }
}