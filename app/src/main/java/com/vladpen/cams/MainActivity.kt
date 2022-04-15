package com.vladpen.cams

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.vladpen.StreamData
import com.vladpen.StreamsAdapter
import com.vladpen.cams.databinding.ActivityMainBinding

class MainActivity: AppCompatActivity() {
    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val streams by lazy { StreamData.getStreams(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        initActivity()
    }

    private fun initActivity() {
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = StreamsAdapter(streams)

        binding.toolbar.btnBack.visibility = View.GONE
        binding.toolbar.tvToolbarLabel.text = getString(R.string.app_name)
        binding.toolbar.tvToolbarLink.text = getString(R.string.add)
        binding.toolbar.tvToolbarLink.setOnClickListener {
            editScreen()
        }
        this.onBackPressedDispatcher.addCallback(callback)
    }

    private fun editScreen() {
        val editIntent = Intent(this, EditActivity::class.java)
        startActivity(editIntent)
    }

    private val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            finishAffinity()
        }
    }
}
