package com.vladpen.cams

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
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

    private lateinit var gestureDetector: VideoGestureDetector
    private var gestureInProgress = 0
    private var aspectRatio = 1f
    private var fragments = arrayListOf<VideoFragment>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        initActivity()
        if (savedInstanceState == null)
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
        gestureDetector = VideoGestureDetector(binding.clGroupBox)
        gestureDetector.reset(aspectRatio)

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
                val frame = findViewById<FrameLayout>(cells[i])
                supportFragmentManager.beginTransaction().add(frame.id, fragment).commit()
                frame.setOnClickListener {
                    videoScreen(i)
                }
                fragments.add(fragment)
            }
        } catch (e: Exception) {
            Log.e("Group", "Data is corrupted (${e.localizedMessage})")
            startActivity(Intent(this, GroupEditActivity::class.java)
                .putExtra("groupId", groupId))
        }
    }

    private val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            back()
        }
    }

    private fun back() {
        GroupData.currentGroupId = -1
        startActivity(Intent(this, MainActivity::class.java))
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

            aspectRatio = ASPECT_RATIO / group.streams.count()
            if (screenAspectRatio > aspectRatio) {
                frameHeight = screenHeight / group.streams.count()
                hideBars = true
            } else {
                frameHeight = (screenWidth / ASPECT_RATIO).toInt()
            }
        } else {
            binding.llRow1.orientation = LinearLayout.HORIZONTAL
            binding.llRow2.orientation = LinearLayout.HORIZONTAL

            aspectRatio = if (group.streams.count() == 2)
                ASPECT_RATIO * 2
            else
                ASPECT_RATIO

            if (screenAspectRatio > aspectRatio) {
                frameHeight = screenHeight / 2
                hideBars = true
            } else {
                frameHeight = (screenWidth / (ASPECT_RATIO * 2)).toInt()
            }
        }
        for (i in group.streams.indices) {
            val frame = findViewById<FrameLayout>(cells[i])
            frame.layoutParams.height = frameHeight
            frame.layoutParams.width = (frameHeight * ASPECT_RATIO).toInt()
        }
        initBars()
    }

    override fun dispatchTouchEvent(e: MotionEvent?): Boolean {
        if (e == null)
            return super.dispatchTouchEvent(e)

        val res = gestureDetector.onTouchEvent(e) || e.pointerCount > 1
        if (res)
            gestureInProgress = e.pointerCount + 1

        if (e.action == MotionEvent.ACTION_UP) {
            if (gestureInProgress == 0)
                initBars()
            else
                gestureInProgress -= 1

            for ((i, fragment) in fragments.withIndex()) {
                if (findViewById<FrameLayout>(cells[i]).getGlobalVisibleRect(Rect()))
                    fragment.play()
                else
                    fragment.stop()
            }
        }
        return res || super.dispatchTouchEvent(e)
    }

    private fun videoScreen(i: Int) {
        if (gestureInProgress > 0)
            return

        val frame = findViewById<FrameLayout>(cells[i])
        Effects.dimmer(frame)

        val id = group.streams[i]
        startActivity(
            Intent(this, VideoActivity::class.java)
                .putExtra("streamId", id)
        )
    }

    private fun initBars() {
        Effects.cancel()
        binding.toolbar.root.visibility = View.VISIBLE
        if (hideBars) {
            Effects.delayedFadeOut(arrayOf(binding.toolbar.root))
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        resizeGrid()
        gestureDetector.reset(aspectRatio)
    }
}