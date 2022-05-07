package com.vladpen.cams

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.vladpen.GroupData
import com.vladpen.StreamData
import com.vladpen.StreamsAdapter
import com.vladpen.cams.databinding.ActivityMainBinding

class MainActivity: AppCompatActivity() {
    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val streams by lazy { StreamData.getStreams(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        if (streams.count() == 0)
            editScreen()
        else
            initActivity()
    }

    private fun initActivity() {
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = StreamsAdapter(streams)

        binding.toolbar.tvToolbarLabel.text = getString(R.string.main_title)
        if (streams.count() >= 2) {
            binding.toolbar.tvToolbarLink.text = getString(R.string.groups)
            binding.toolbar.tvToolbarLink.setOnClickListener {
                groupsScreen()
            }
        }
        binding.fab.btnAdd.setOnClickListener {
            editScreen()
        }
        binding.toolbar.btnBack.visibility = View.GONE
        /*
        binding.toolbar.btnBack.setImageResource(R.drawable.ic_baseline_menu_24)
        binding.toolbar.btnBack.setOnClickListener {
            MainMenu(this).showPopupMenu(it)
        }
        */
        this.onBackPressedDispatcher.addCallback(callback)
    }

    private fun editScreen() {
        val intent = Intent(this, EditActivity::class.java)
        startActivity(intent)
    }

    private fun groupsScreen() {
        val intent = if (GroupData.getGroups(this).count() == 0)
            Intent(this, EditGroupActivity::class.java)
        else
            Intent(this, GroupsActivity::class.java)
        startActivity(intent)
    }

    private val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            finishAffinity()
        }
    }
}
