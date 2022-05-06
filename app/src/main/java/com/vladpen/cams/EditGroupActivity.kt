package com.vladpen.cams

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.widget.PopupMenu
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.vladpen.*
import com.vladpen.cams.databinding.ActivityEditGroupBinding

private const val STREAMS_MIN = 2
private const val STREAMS_MAX = 4

class EditGroupActivity : AppCompatActivity() {
    private val binding by lazy { ActivityEditGroupBinding.inflate(layoutInflater) }
    private var groupId: Int = -1
    private lateinit var streams: List<StreamDataModel>
    private var selectedStreams = mutableMapOf<Int, Int>()
    private val streamsMap by lazy { StreamData.getStreamsMap(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        initActivity()
    }

    private fun initActivity() {
        groupId = intent.getIntExtra("groupId", -1)
        streams = StreamData.getStreams(this)

        val group = GroupData.getById(groupId)
        if (group == null) {
            groupId = -1
            binding.toolbar.tvToolbarLabel.text = getString(R.string.group_add)
            binding.tvDeleteLink.visibility = View.GONE
        } else {
            binding.toolbar.tvToolbarLabel.text = group.name

            binding.etEditName.setText(group.name)

            setStreams(group.streams)

            binding.tvDeleteLink.setOnClickListener {
                delete()
            }
        }
        if (group == null || group.streams.count() < STREAMS_MAX)
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

    private fun setStreams(ids: List<Int>?) {
        if (ids == null || streamsMap.count() == 0) // this is impossible
            return
        for (id in ids) {
            if (!streamsMap.containsKey(id))
                continue
            val i = streamsMap[id]!!.toInt()
            selectedStreams[i] = i
            addStreamToView(i)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun addStreamToView(streamId: Int) {
        val tv = TextView(this)
        tv.text = streams[streamId].name
        tv.textSize = 18f
        tv.setPadding(0, 10, 0, 10)
        tv.setTextColor(getColor(R.color.white))
        binding.llStreams.addView(tv)

        tv.setOnTouchListener(object : OnSwipeListener(this) {
            override fun onSwipe() {
                binding.llStreams.removeView(tv)
                selectedStreams.remove(streamId)
            }
        })
    }

    private fun showPopupMenu(view: View) {
        val popup = PopupMenu(this, view)

        for ((i, stream) in streams.withIndex()) {
            if (!selectedStreams.containsKey(i))
                popup.menu.add(Menu.NONE, i, i, stream.name)
        }
        popup.setOnMenuItemClickListener { item ->
            val i = item.itemId
            selectedStreams[i] = i

            popup.menu.findItem(i).isVisible = false // removeItem(i)
            addStreamToView(i)

            if (!popup.menu.hasVisibleItems() || selectedStreams.count() > 3)
                binding.tvAddStream.visibility = View.GONE

            true
        }
        popup.show()
    }

    private fun save() {
        val ids = StreamData.getStreamsIds(this, selectedStreams.keys)

        if (!validate(ids))
            return

        GroupData.save(this, groupId, GroupDataModel(
            binding.etEditName.text.toString().trim(),
            ids
        ))
        back()
    }

    private fun validate(ids: List<Int>): Boolean {
        val name = binding.etEditName.text.toString().trim()
        val groups = GroupData.getGroups(this)
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

            if (groups[i].streams.count() == ids.count() && groups[i].streams.containsAll(ids)) {
                binding.tvStreamsError.text = getString(R.string.err_group_exists)
                ok = false
            }
        }
        if (!ok)
            return ok
        if (selectedStreams.count() < STREAMS_MIN || selectedStreams.count() > STREAMS_MAX) {
            binding.tvStreamsError.text = getString(
                R.string.err_group_streams_count,
                STREAMS_MIN,
                STREAMS_MAX
            )
            ok = false
        } else {
            binding.tvStreamsError.text = ""
        }
        return ok
    }

    private fun delete() {
        AlertDialog.Builder(this)
            .setMessage(R.string.group_delete)
            .setPositiveButton(R.string.delete) { _, _ ->
                GroupData.delete(this, groupId)
                back()
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .create().show()
    }

    private fun back() {
        val intent = Intent(this, GroupsActivity::class.java)
        Navigator.go(this, intent)
    }
}