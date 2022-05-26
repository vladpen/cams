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
    private val sources by lazy { SourceData.getAll() }

    val exportSettings = Settings(this).export()
    val importSettings = Settings(this).import()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        if (sources.isEmpty())
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
        binding.recyclerView.adapter = SourceAdapter(sources)
        ItemTouch.helper().attachToRecyclerView(binding.recyclerView)

        binding.toolbar.tvToolbarLabel.text = getString(R.string.main_title)
        this.onBackPressedDispatcher.addCallback(callback)
    }

    private fun editScreen() {
        val intent = Intent(this, EditActivity::class.java)
        startActivity(intent)
    }

    private val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            finishAffinity()
        }
    }
}