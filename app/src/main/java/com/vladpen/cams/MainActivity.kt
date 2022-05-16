package com.vladpen.cams

import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.vladpen.*
import com.vladpen.cams.databinding.ActivityMainBinding

class MainActivity: AppCompatActivity() {
    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val streams by lazy { StreamData.getStreams(this) }
    private val groups by lazy { GroupData.getGroups(this) }
    val exportSettings = Settings(this).export()
    val importSettings = Settings(this).import()

    companion object {
        private var contentMode: String = "streams"
    }

    fun getMode(): String {
        return contentMode
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        if (streams.isEmpty())
            editScreen()
        else
            initActivity()
    }

    private fun initActivity() {
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.toolbar.btnBack.setImageResource(R.drawable.ic_baseline_menu_24)
        binding.toolbar.btnBack.setOnClickListener {
            MainMenu(this).showPopupMenu(it)
        }
        if (contentMode == "streams") {
            binding.recyclerView.adapter = StreamsAdapter(streams)
            ItemTouch.helper(this).attachToRecyclerView(binding.recyclerView)

            binding.toolbar.tvToolbarLabel.text = getString(R.string.main_title)
            if (streams.count() >= 2) {
                binding.toolbar.tvToolbarLink.text = getString(R.string.groups)
                binding.toolbar.tvToolbarLink.setOnClickListener {
                    groupsScreen()
                }
            }
        } else if (contentMode == "groups") {
            binding.recyclerView.adapter = GroupsAdapter(groups)
            ItemTouch.helper(this).attachToRecyclerView(binding.recyclerView)

            binding.toolbar.tvToolbarLabel.text = getString(R.string.groups)
            binding.toolbar.tvToolbarLink.text = getString(R.string.main_title)
            binding.toolbar.tvToolbarLink.setTextColor(getColor(R.color.live_link))
            binding.toolbar.tvToolbarLink.setOnClickListener {
                back()
            }
        }
        this.onBackPressedDispatcher.addCallback(callback)
    }

    private fun editScreen() {
        val intent = Intent(this, EditActivity::class.java)
        startActivity(intent)
    }

    private fun groupsScreen() {
        contentMode = "groups"
        if (GroupData.getGroups(this).isEmpty()) {
            val intent = Intent(this, GroupEditActivity::class.java)
            startActivity(intent)
        } else {
            finish()
            startActivity(intent)
        }
    }

    private fun back() {
        contentMode = "streams"
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }

    private val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            finishAffinity()
        }
    }
}