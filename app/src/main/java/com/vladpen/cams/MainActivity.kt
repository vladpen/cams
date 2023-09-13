package com.vladpen.cams

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vladpen.Alert
import com.vladpen.Settings
import com.vladpen.SourceAdapter
import com.vladpen.SourceData
import com.vladpen.SourceItemTouch
import com.vladpen.Utils
import com.vladpen.cams.databinding.ActivityMainBinding

class MainActivity: AppCompatActivity() {
    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val sources by lazy { SourceData.getAll() }

    val exportSettings = Settings(this).export()
    val importSettings = Settings(this).import()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        initActivity()
    }

    private fun initActivity() {
        GridLayoutManager(
            this,
            Utils.getColumnCount(resources.displayMetrics),
            RecyclerView.VERTICAL,
            false)
            .apply { binding.recyclerView.layoutManager = this }

        binding.toolbar.btnBack.setImageResource(R.drawable.ic_baseline_menu_24)
        binding.toolbar.btnBack.setOnClickListener {
            MainMenu(this).showPopupMenu(it)
        }
        binding.toolbar.tvLabel.text = getString(R.string.main_title)

        binding.recyclerView.adapter = SourceAdapter(sources)
        SourceItemTouch().helper().attachToRecyclerView(binding.recyclerView)

        if (sources.isEmpty())
            initEmpty()
        else
            Alert.init(this, binding.toolbar.btnAlert)

        this.onBackPressedDispatcher.addCallback(callback)
    }

    private fun initEmpty() {
        binding.emptyBox.emptyContent.visibility = View.VISIBLE
        binding.emptyBox.btnEdit.setOnClickListener {
            val intent = Intent(this, EditActivity::class.java)
            startActivity(intent)
        }
        binding.emptyBox.btnImport.setOnClickListener {
            MainMenu(this).import()
        }
    }

    private val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            finishAffinity()
        }
    }
}