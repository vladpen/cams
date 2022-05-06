package com.vladpen

import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.vladpen.cams.FilesActivity
import com.vladpen.cams.R
import com.vladpen.cams.VideoActivity
import com.vladpen.cams.databinding.FileItemBinding
import java.text.DecimalFormat

class FilesAdapter(
    private val dataSet: List<FileDataModel>,
    private val remotePath: String,
    private val streamId: Int,
    private val sftpUrl: String?) :

    RecyclerView.Adapter<FilesAdapter.FileHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileHolder {
        val binding = FileItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FileHolder(parent.context, binding)
    }

    override fun onBindViewHolder(holder: FileHolder, idx: Int) {
        val row: FileDataModel = dataSet[idx]
        holder.bind(row)
    }

    override fun getItemCount(): Int = dataSet.count()

    inner class FileHolder(private val context: Context, private val binding: FileItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(row: FileDataModel) {
            with(binding) {
                tvFileName.text = row.name
                if (!row.isDir) {
                    var s = DecimalFormat("#.00")
                        .format(row.size.toDouble() / 1000000)
                    s += " " + context.getString(R.string.MB)
                    tvFileSize.text = s

                    ivLabel.setBackgroundResource(R.drawable.ic_outline_videocam_24)
                } else {
                    ivLabel.setBackgroundResource(R.drawable.ic_outline_folder_24)
                }
                clFileRow.setOnClickListener {
                    navigate(context, row)
                }
            }
        }
    }

    private fun navigate(context: Context, file: FileDataModel) {
        if (!file.isDir) {
            FileData(context, sftpUrl).remoteToCache(remotePath, file.name)

            val intent = Intent(context, VideoActivity::class.java)
                .setFlags(FLAG_ACTIVITY_NEW_TASK)
                .putExtra("remotePath", remotePath + file.name)
                .putExtra("streamId", streamId)
            Navigator.go(context, intent)
        } else {
            val intent = Intent(context, FilesActivity::class.java)
                .setFlags(FLAG_ACTIVITY_NEW_TASK)
                .putExtra("remotePath", remotePath + file.name + "/")
                .putExtra("streamId", streamId)
            Navigator.go(context, intent)
        }
    }
}
