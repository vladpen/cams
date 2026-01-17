package com.vladpen.cams

import android.content.Intent
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vladpen.Alert
import com.vladpen.Effects.edgeToEdge
import com.vladpen.GroupData
import com.vladpen.Settings
import com.vladpen.SourceAdapter
import com.vladpen.SourceData
import com.vladpen.SourceItemTouch
import com.vladpen.StreamData
import com.vladpen.Utils
import com.vladpen.cams.MainApp.Companion.context
import com.vladpen.cams.databinding.ActivityMainBinding

class MainActivity: AppCompatActivity() {
    companion object {
        private var isCreated: Boolean = false
    }

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val sources by lazy { SourceData.getAll() }

    val exportSettings = Settings(this).export()
    val importSettings = Settings(this).import()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (redirect())
            return
        setContentView(binding.root)
        edgeToEdge(binding.root)
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

        binding.recyclerView.adapter = SourceAdapter(sources) { it: Intent -> startActivity(it) }
        SourceItemTouch().helper().attachToRecyclerView(binding.recyclerView)

        if (sources.isEmpty())
            initEmpty()
        else
            Alert.init(this, binding.toolbar.btnAlert)

        // Add test PTZ button
        binding.fabTest.setOnClickListener {
            android.util.Log.d("ONVIF", "Test PTZ button clicked!")
            android.widget.Toast.makeText(this, "PTZ Test Button Works!", android.widget.Toast.LENGTH_SHORT).show()
        }

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
        binding.emptyBox.tvManualLink.text = HtmlCompat.fromHtml(
            getString(R.string.manual_link),
            HtmlCompat.FROM_HTML_MODE_COMPACT)
        binding.emptyBox.tvManualLink.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun redirect(): Boolean {
        if (isCreated)
            return false
        isCreated = true
        val startup = SourceData.getStartup() ?: return false
        if (startup.id < 0)
            return false

        val newIntent = Intent(context, StreamsActivity::class.java).putExtra("id", startup.id)
        when (startup.type) {
            "stream" -> {
                val streams = StreamData.getAll()
                if (streams.isEmpty() || startup.id > streams.count() - 1)
                    return false
                newIntent.putExtra("type", "stream")
            }
            "group" -> {
                val groups = GroupData.getAll()
                if (groups.isEmpty() || startup.id > groups.count() - 1)
                    return false
                newIntent.putExtra("type", "group")
            }
            else -> return false
        }
        startActivity(newIntent)
        return true
    }

    private val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            finishAffinity()
        }
    }
}