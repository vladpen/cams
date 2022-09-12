package com.vladpen.cams

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
        initActivity()
    }

    private fun initActivity() {
        if (resources.displayMetrics.heightPixels > resources.displayMetrics.widthPixels)
            binding.recyclerView.layoutManager = LinearLayoutManager(this)
        else
            GridLayoutManager(this,2, RecyclerView.VERTICAL,false)
                .apply { binding.recyclerView.layoutManager = this }

        binding.toolbar.btnBack.setImageResource(R.drawable.ic_baseline_menu_24)
        binding.toolbar.btnBack.setOnClickListener {
            MainMenu(this).showPopupMenu(it)
        }
        binding.recyclerView.adapter = SourceAdapter(sources)
        SourceItemTouch().helper().attachToRecyclerView(binding.recyclerView)

        binding.toolbar.tvToolbarLabel.text = getString(R.string.main_title)

        if (sources.isEmpty())
            initEmpty()

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