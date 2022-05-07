package com.vladpen.cams

import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.vladpen.GroupData
import com.vladpen.GroupsAdapter
import com.vladpen.cams.databinding.ActivityGroupsBinding

class GroupsActivity: AppCompatActivity() {
    private val binding by lazy { ActivityGroupsBinding.inflate(layoutInflater) }
    private val groups by lazy { GroupData.getGroups(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        initActivity()
    }

    private fun initActivity() {
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = GroupsAdapter(groups)

        binding.toolbar.tvToolbarLabel.text = getString(R.string.groups)
        binding.fab.btnAdd.setOnClickListener {
            editScreen()
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

    private fun back() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }

    private fun editScreen() {
        val intent = Intent(this, EditGroupActivity::class.java)
        startActivity(intent)
    }
}
