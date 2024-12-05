package com.vladpen

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.vladpen.cams.FilesActivity
import com.vladpen.cams.MainApp.Companion.context
import com.vladpen.cams.R
import com.vladpen.cams.VideoActivity
import com.vladpen.cams.databinding.FileItemBinding
import java.text.DecimalFormat

class FileAdapter(
    private val dataSet: List<FileDataModel>,
    private val remotePath: String,
    private val streamId: Int,
    private val sftpUrl: String?,
    private val navigate: ((intent: Intent) -> Unit)
) : RecyclerView.Adapter<FileAdapter.Holder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = FileItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val row: FileDataModel = dataSet[position]
        holder.bind(row)
    }

    override fun getItemCount(): Int = dataSet.count()

    inner class Holder(private val binding: FileItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(row: FileDataModel) {
            with(binding) {
                tvFileName.text = row.name
                if (!row.isDir) {
                    var s = DecimalFormat("0.00")
                        .format(row.size.toDouble() / 1000000)
                    s += " " + context.getString(R.string.MB)
                    tvFileSize.text = s

                    ivLabel.setBackgroundResource(R.drawable.ic_outline_videocam_24)
                } else {
                    ivLabel.setBackgroundResource(R.drawable.ic_outline_folder_24)
                }
                clFileRow.setOnClickListener {
                    onClick(row)
                }
            }
        }
    }

    private fun onClick(file: FileDataModel) {
        if (!file.isDir) {
            FileData(sftpUrl).remoteToCache(remotePath, file.name)

            val intent = Intent(context, VideoActivity::class.java)
                .putExtra("remotePath", remotePath + file.name)
                .putExtra("streamId", streamId)
            navigate(intent)
        } else {
            val intent = Intent(context, FilesActivity::class.java)
                .putExtra("remotePath", remotePath + file.name + "/")
                .putExtra("streamId", streamId)
            navigate(intent)
        }
    }
}