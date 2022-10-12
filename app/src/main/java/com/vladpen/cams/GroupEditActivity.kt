package com.vladpen.cams

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.widget.PopupMenu
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vladpen.*
import com.vladpen.cams.databinding.ActivityEditGroupBinding

private const val STREAMS_MIN = 2

class GroupEditActivity : AppCompatActivity() {
    private val binding by lazy { ActivityEditGroupBinding.inflate(layoutInflater) }
    private var groupId: Int = -1
    private var selectedStreams = mutableListOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        initActivity()
    }

    private fun initActivity() {
        groupId = intent.getIntExtra("groupId", -1)

        val group = GroupData.getById(groupId)
        if (group == null) {
            groupId = -1
            binding.toolbar.tvToolbarLabel.text = getString(R.string.group_add)
            binding.tvDeleteLink.visibility = View.GONE
        } else {
            binding.toolbar.tvToolbarLabel.text = group.name

            binding.etEditName.setText(group.name)

            selectedStreams = group.streams.map { it } as MutableList<Int>

            binding.tvDeleteLink.setOnClickListener {
                delete()
            }
        }

        GridLayoutManager(this,
            Utils.getColumnCount(resources.displayMetrics),
            RecyclerView.VERTICAL,
            false).apply { binding.recyclerView.layoutManager = this }

        binding.recyclerView.adapter = GroupAdapter(selectedStreams, this)
        GroupItemTouch().helper().attachToRecyclerView(binding.recyclerView)

        if (StreamData.getAll().count() <= 4)
            binding.tvWarning.visibility = View.GONE
        if (selectedStreams.count() >= StreamData.getAll().count())
            binding.tvAddStream.visibility = View.GONE
        binding.tvAddStream.setOnClickListener {
            showPopupMenu(it)
        }
        binding.btnSave.setOnClickListener {
            save()
        }
        binding.toolbar.btnBack.setOnClickListener {
            back()
        }
        this.onBackPressedDispatcher.addCallback(callback)
    }

    private val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            back()
        }
    }

    private fun showPopupMenu(view: View) {
        val popup = PopupMenu(this, view)
        for ((i, source) in SourceData.getAll().withIndex()) {
            if (source.type == "stream" && !selectedStreams.contains(source.id))
                popup.menu.add(Menu.NONE, source.id, i, StreamData.getById(source.id)?.name)
        }
        popup.setOnMenuItemClickListener { item ->
            val streamId = item.itemId
            selectedStreams.add(streamId)

            popup.menu.findItem(streamId).isVisible = false

            binding.recyclerView.adapter?.notifyItemInserted(selectedStreams.count() - 1)
            binding.recyclerView.smoothScrollToPosition(selectedStreams.count() - 1)

            if (!popup.menu.hasVisibleItems())
                binding.tvAddStream.visibility = View.GONE

            true
        }
        popup.show()
    }

    private fun save() {
        if (!validate())
            return

        val newGroup = GroupDataModel(
            binding.etEditName.text.toString().trim(),
            selectedStreams
        )
        if (groupId < 0) {
            GroupData.add(newGroup)
        } else {
            GroupData.update(groupId, newGroup)
        }
        back()
    }

    private fun validate(): Boolean {
        val name = binding.etEditName.text.toString().trim()
        val groups = GroupData.getAll()
        var ok = true

        if (name.isEmpty() || name.length > 255) {
            binding.etEditName.error = getString(R.string.err_invalid)
            ok = false
        }
        for (i in groups.indices) {
            if (i == groupId)
                break

            if (groups[i].name == name) {
                binding.etEditName.error = getString(R.string.err_group_exists)
                ok = false
            }
            if (groups[i].streams.count() == selectedStreams.count() &&
                groups[i].streams.containsAll(selectedStreams)) {
                binding.tvStreamsError.text = getString(R.string.err_group_exists)
                ok = false
            }
        }
        if (!ok)
            return false
        if (selectedStreams.count() < STREAMS_MIN) {
            binding.tvStreamsError.text = getString(R.string.err_group_streams_count, STREAMS_MIN)
            ok = false
        } else {
            binding.tvStreamsError.text = ""
        }
        return ok
    }

    fun moveItem(from: Int, to: Int) {
        val item = selectedStreams.removeAt(from)
        selectedStreams.add(to, item)
    }

    fun removeAt(position: Int) {
        selectedStreams.removeAt(position)
        binding.tvAddStream.visibility = View.VISIBLE
    }

    private fun delete() {
        AlertDialog.Builder(this)
            .setMessage(R.string.group_delete)
            .setPositiveButton(R.string.delete) { _, _ ->
                GroupData.delete(groupId)
                back()
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .create().show()
    }

    private fun back() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }
}