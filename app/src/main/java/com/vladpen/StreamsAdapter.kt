package com.vladpen

import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.vladpen.cams.VideoActivity
import com.vladpen.cams.EditActivity
import com.vladpen.cams.databinding.StreamItemBinding

class StreamsAdapter(private val dataSet: List<StreamDataModel>) :
    RecyclerView.Adapter<StreamsAdapter.StreamHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StreamHolder {
        val binding = StreamItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return StreamHolder(parent.context, binding)
    }

    override fun onBindViewHolder(holder: StreamHolder, position: Int) {
        val row: StreamDataModel = dataSet[position]
        holder.bind(position, row)
    }

    override fun getItemCount(): Int = dataSet.size

    inner class StreamHolder(private val context: Context, private val binding: StreamItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(position: Int, row: StreamDataModel) {
            with(binding) {
                tvStreamName.text = row.name
                tvStreamName.setOnClickListener {
                    val intent = Intent(context, VideoActivity::class.java)
                    navigate(context, intent, position)
                }
                btnEdit.setOnClickListener {
                    val intent = Intent(context, EditActivity::class.java)
                    navigate(context, intent, position)
                }
            }
        }
    }

    private fun navigate(context: Context, intent: Intent,  position: Int) {
        intent.setFlags(FLAG_ACTIVITY_NEW_TASK).putExtra("position", position)
        context.startActivity(intent)
    }
}
