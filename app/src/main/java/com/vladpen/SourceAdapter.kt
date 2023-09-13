package com.vladpen

import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.vladpen.cams.*
import com.vladpen.cams.MainApp.Companion.context
import com.vladpen.cams.databinding.MainItemBinding

class SourceAdapter(private val dataSet: List<SourceDataModel>) :
    RecyclerView.Adapter<SourceAdapter.Holder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = MainItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val row: SourceDataModel = dataSet[position]
        holder.bind(row)
    }

    override fun getItemCount(): Int = dataSet.count()

    fun moveItem(from: Int, to: Int) {
        SourceData.moveItem(from, to)
    }

    inner class Holder(private val binding: MainItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(row: SourceDataModel) {
            if (row.type == "stream")
                initStream(row.id)
            else if (row.type == "group")
                initGroup(row.id)
        }

        private fun initStream(id: Int) {
            with(binding) {
                tvItemName.text = StreamData.getById(id)?.name
                tvItemName.setTextColor(context.getColor(R.color.text))
                tvItemName.setOnClickListener {
                    navigate(
                        Intent(context, VideoActivity::class.java)
                            .putExtra("streamId", id)
                    )
                }
                tvItemTime.text = AlertWork.getLastTime(id)
                btnEdit.setOnClickListener {
                    navigate(
                        Intent(context, EditActivity::class.java)
                            .putExtra("streamId", id)
                    )
                }
            }
        }

        private fun initGroup(id: Int) {
            with(binding) {
                tvItemName.text = GroupData.getById(id)?.name
                tvItemName.setTextColor(context.getColor(R.color.group_link))
                tvItemName.setOnClickListener {
                    navigate(Intent(context, GroupActivity::class.java)
                        .putExtra("groupId", id))
                }
                btnEdit.setOnClickListener {
                    navigate(Intent(context, GroupEditActivity::class.java)
                        .putExtra("groupId", id))
                }
            }
        }

        private fun navigate(intent: Intent) {
            intent.flags = FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        }
    }
}