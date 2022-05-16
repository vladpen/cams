package com.vladpen

import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.vladpen.cams.GroupEditActivity
import com.vladpen.cams.GroupActivity
import com.vladpen.cams.databinding.MainItemBinding

class GroupsAdapter(private val dataSet: List<GroupDataModel>) :
    RecyclerView.Adapter<GroupsAdapter.GroupHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupHolder {
        val binding = MainItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return GroupHolder(parent.context, binding)
    }

    override fun onBindViewHolder(holder: GroupHolder, idx: Int) {
        val row: GroupDataModel = dataSet[idx]
        holder.bind(idx, row)
    }

    override fun getItemCount(): Int = dataSet.count()

    fun moveItem(context: Context, from: Int, to: Int) {
        GroupData.moveItem(context, from, to)
    }

    inner class GroupHolder(private val context: Context, private val binding: MainItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(idx: Int, row: GroupDataModel) {
            with(binding) {
                tvItemName.text = row.name
                tvItemName.setOnClickListener {
                    val intent = Intent(context, GroupActivity::class.java)
                    navigate(intent, idx)
                }
                btnEdit.setOnClickListener {
                    val intent = Intent(context, GroupEditActivity::class.java)
                    navigate(intent, idx)
                }
            }
        }

        private fun navigate(intent: Intent,  idx: Int) {
            intent.setFlags(FLAG_ACTIVITY_NEW_TASK).putExtra("groupId", idx)
            context.startActivity(intent)
        }
    }
}