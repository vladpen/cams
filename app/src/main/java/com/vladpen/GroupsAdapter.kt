package com.vladpen

import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.vladpen.cams.EditGroupActivity
import com.vladpen.cams.GroupActivity
import com.vladpen.cams.databinding.GroupItemBinding

class GroupsAdapter(private val dataSet: List<GroupDataModel>) :
    RecyclerView.Adapter<GroupsAdapter.GroupHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupHolder {
        val binding = GroupItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return GroupHolder(parent.context, binding)
    }

    override fun onBindViewHolder(holder: GroupHolder, idx: Int) {
        val row: GroupDataModel = dataSet[idx]
        holder.bind(idx, row)
    }

    override fun getItemCount(): Int = dataSet.count()

    inner class GroupHolder(private val context: Context, private val binding: GroupItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(idx: Int, row: GroupDataModel) {
            with(binding) {
                tvGroupName.text = row.name
                tvGroupName.setOnClickListener {
                    val intent = Intent(context, GroupActivity::class.java)
                    navigate(intent, idx)
                }
                btnEdit.setOnClickListener {
                    val intent = Intent(context, EditGroupActivity::class.java)
                    navigate(intent, idx)
                }
            }
        }

        private fun navigate(intent: Intent,  idx: Int) {
            intent.setFlags(FLAG_ACTIVITY_NEW_TASK).putExtra("groupId", idx)
            Navigator.go(context, intent)
        }
    }
}
