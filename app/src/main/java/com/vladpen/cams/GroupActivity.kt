package com.vladpen.cams

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.vladpen.*
import com.vladpen.cams.databinding.ActivityGroupBinding

private const val ASPECT_RATIO = 16f / 9f
private const val STREAMS_MAX = 4

class GroupActivity : AppCompatActivity() {
    private val binding by lazy { ActivityGroupBinding.inflate(layoutInflater) }

    private var groupId: Int = -1
    private lateinit var group: GroupDataModel
    private val cells = listOf(R.id.llCell1, R.id.llCell2, R.id.llCell3, R.id.llCell4)
    private var hideBars = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        initActivity()
        if(savedInstanceState == null)
            initFragments()
    }

    private fun initActivity() {
        groupId = intent.getIntExtra("groupId", -1)

        this.onBackPressedDispatcher.addCallback(callback)
        binding.toolbar.btnBack.setOnClickListener {
            back()
        }
        group = GroupData.getById(groupId) ?: return

        binding.toolbar.tvToolbarLabel.text = group.name

        resizeGrid()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        GroupData.currentGroupId = groupId  // save for back navigation
    }

    private fun initFragments() {
        try { // prevents exception if group file is corrupted
            for ((i, id) in group.streams.withIndex()) {
                if (i > STREAMS_MAX - 1)
                    break
                if (id > StreamData.getAll().count())
                    throw Exception("invalid group ID")

                val fragment = VideoFragment.newInstance(id)
                val viewId = cells[i]
                supportFragmentManager.beginTransaction().add(viewId, fragment).commit()
            }
        } catch (e: Exception) {
            Log.e("Group", "Data is corrupted (${e.localizedMessage})")

            val intent = Intent(this, GroupEditActivity::class.java)
                .putExtra("groupId", groupId)
            startActivity(intent)
        }
    }

    private val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            back()
        }
    }

    private fun back() {
        GroupData.currentGroupId = -1
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }

    private fun resizeGrid() {
        var statusBarHeight = 0
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0)
            statusBarHeight = resources.getDimensionPixelSize(resourceId)

        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels - statusBarHeight
        val screenAspectRatio = screenWidth.toFloat() / screenHeight.toFloat()

        val frameHeight: Int
        if (screenHeight > screenWidth) {
            binding.llRow1.orientation = LinearLayout.VERTICAL
            binding.llRow2.orientation = LinearLayout.VERTICAL

            val groupAspectRatio = ASPECT_RATIO / group.streams.count()
            if (screenAspectRatio > groupAspectRatio) {
                frameHeight = screenHeight / group.streams.count()
                hideBars = true
            } else {
                frameHeight = (screenWidth / ASPECT_RATIO).toInt()
            }
        } else {
            binding.llRow1.orientation = LinearLayout.HORIZONTAL
            binding.llRow2.orientation = LinearLayout.HORIZONTAL

            val groupAspectRatio = if (group.streams.count() == 2)
                ASPECT_RATIO * 2
            else
                ASPECT_RATIO

            if (screenAspectRatio > groupAspectRatio) {
                frameHeight = screenHeight / 2
                hideBars = true
            } else {
                frameHeight = (screenWidth / (ASPECT_RATIO * 2)).toInt()
            }
        }
        for (i in group.streams.indices) {
            val view = findViewById<LinearLayout>(cells[i])
            view.layoutParams.height = frameHeight
            view.layoutParams.width = (frameHeight * ASPECT_RATIO).toInt()
        }
        initBars()
    }

    private fun initBars() {
        Effects.cancel()
        binding.toolbar.root.visibility = View.VISIBLE
        if (hideBars) {
            Effects.delayedFadeOut(arrayOf(binding.toolbar.root))
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        initBars()
        return super.onTouchEvent(event)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        resizeGrid()
    }
}