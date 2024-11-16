package com.vladpen.cams

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.RelativeLayout
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.Insets
import com.vladpen.*
import com.vladpen.Effects.edgeToEdge
import com.vladpen.cams.databinding.ActivityGroupBinding
import kotlin.math.*

private const val ASPECT_RATIO = 16f / 9f

class GroupActivity : AppCompatActivity() {
    private val binding by lazy { ActivityGroupBinding.inflate(layoutInflater) }
    private val handler by lazy { Handler(Looper.getMainLooper()) }

    private lateinit var group: GroupDataModel
    private lateinit var layoutListener: ViewTreeObserver.OnGlobalLayoutListener
    private lateinit var gestureDetector: VideoGestureDetector
    private var gestureInProgress = 0
    private var groupId: Int = -1
    private var fragments = arrayListOf<VideoFragment>()
    private var frames = arrayListOf<FrameLayout>()
    private var hideBars = false
    private val fragmentLoading = mutableMapOf<Int, Boolean>()
    private val watchdogInterval: Long = 10000 // milliseconds
    private var insets: Insets? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        edgeToEdge(binding.root) { innerPadding ->
            insets = innerPadding
        }
        initActivity()
        if (savedInstanceState == null)
            initFragments()

        layoutListener = ViewTreeObserver.OnGlobalLayoutListener {
            resizeGrid() // also reset gestureDetector
            binding.root.viewTreeObserver.removeOnGlobalLayoutListener(layoutListener)
        }
        binding.root.viewTreeObserver.addOnGlobalLayoutListener(layoutListener)
    }

    private fun initActivity() {
        groupId = intent.getIntExtra("groupId", -1)

        this.onBackPressedDispatcher.addCallback(callback)
        binding.toolbar.btnBack.setOnClickListener {
            back()
        }
        group = GroupData.getById(groupId) ?: return

        binding.toolbar.tvLabel.text = group.name

        gestureDetector = VideoGestureDetector(binding.clScreenBox, binding.rlGroupBox)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        GroupData.currentGroupId = groupId  // save for back navigation

        Alert.init(this, binding.toolbar.btnAlert)
    }

    private fun initFragments() {
        try { // prevents exception if group file is corrupted
            var isChannelsAvailable = false
            for ((i, id) in group.streams.withIndex()) {
                if (id > StreamData.getAll().count())
                    throw Exception("invalid group ID")

                val frame = FrameLayout(this)
                frame.id = FrameLayout.generateViewId()
                binding.rlGroupBox.addView(frame)
                frames.add(frame)

                val fragment = VideoFragment.newInstance(id)
                supportFragmentManager.beginTransaction().add(frame.id, fragment).commit()
                frame.setOnClickListener {
                    videoScreen(i)
                }
                frame.setOnLongClickListener {
                    initBars()
                    true
                }
                fragments.add(fragment)

                val stream = StreamData.getById(id)
                isChannelsAvailable = isChannelsAvailable || stream?.url2 != null

                fragmentLoading[id] = true
            }
            if (isChannelsAvailable) {
                initChannel()
            }
        } catch (e: Exception) {
            Log.e("Group", "Data is corrupted (${e.localizedMessage})")
            startActivity(Intent(this, GroupEditActivity::class.java)
                .putExtra("groupId", groupId))
        }
    }

    private fun initChannel() {
        var channel = StreamData.getGroupChannel()
        binding.btnChannel.setImageResource(Utils.getChannelButton(channel))
        binding.btnChannel.visibility = View.VISIBLE
        binding.btnChannel.setOnClickListener {
            channel = if (channel != 1) 1 else 0
            StreamData.setGroupChannel(channel)
            binding.btnChannel.setImageResource(Utils.getChannelButton(channel))
            for (fragment in fragments) {
                fragment.stop()
                fragment.start()
                fragmentLoading[fragment.streamId] = true
            }
            binding.progressBar.pbLoading.visibility = View.VISIBLE
        }
    }

    fun hideLoading(streamId: Int) {
        fragmentLoading.remove(streamId)
        if (fragmentLoading.isEmpty())
            binding.progressBar.pbLoading.visibility = View.GONE
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

    override fun onStart() {
        super.onStart()
        handler.postDelayed(runnable, watchdogInterval)
    }

    override fun onStop() {
        super.onStop()
        handler.removeCallbacks(runnable)
    }

    private val runnable = object : Runnable {
        override fun run() {
            for (fragment in fragments) {
                if (fragment.watchdog())
                    continue
                binding.progressBar.pbLoading.visibility = View.VISIBLE
            }
            handler.postDelayed(this, watchdogInterval)
        }
    }

    private fun resizeGrid() {
        val rootWidth = binding.root.width - (insets?.left ?: 0) - (insets?.right ?: 0)
        val rootHeight = binding.root.height - (insets?.top ?: 0) - (insets?.bottom ?: 0)

        val cellQty = if (rootHeight > rootWidth)
            max(4, frames.count() - 5 / frames.count()) / 4.0 // 1 column for up to 5 cells
        else
            frames.count().toDouble()
        val columnCount = ceil(sqrt(cellQty)).toInt()
        val rowCount = ceil(frames.count() / columnCount.toDouble()).toInt()

        val frameHeight: Int
        val rootAspectRatio = rootWidth.toFloat() / rootHeight.toFloat()
        val videoAspectRatio = ASPECT_RATIO * columnCount / rowCount
        if (rootAspectRatio > videoAspectRatio) { // vertical margins
            frameHeight = rootHeight / rowCount
            hideBars = true
        } else { // horizontal margins
            frameHeight = ((rootWidth / columnCount) / ASPECT_RATIO).toInt()
            hideBars = false
        }
        val frameWidth = (frameHeight * ASPECT_RATIO).toInt()

        for ((i, frame) in frames.withIndex()) {
            frame.layoutParams.height = frameHeight
            frame.layoutParams.width = frameWidth
            if (i == 0)
                continue

            val params = frame.layoutParams as RelativeLayout.LayoutParams
            params.removeRule(RelativeLayout.BELOW)
            params.removeRule(RelativeLayout.RIGHT_OF)
            params.marginStart = 0

            val lastCount = frames.count() - i
            if (i % columnCount != 0) // except first cell in each row
                params.addRule(RelativeLayout.RIGHT_OF, frames[i - 1].id)
            else if (lastCount <= columnCount) // first cell in the last row, center horizontally
                params.marginStart = (frameWidth * (columnCount - lastCount) / 2)
            if (i >= columnCount) // except first row
                params.addRule(RelativeLayout.BELOW, frames[i - columnCount].id)
        }
        gestureDetector.reset()
        initBars()
    }

    override fun dispatchTouchEvent(e: MotionEvent?): Boolean {
        if (e == null)
            return super.dispatchTouchEvent(null)

        val res = gestureDetector.onTouchEvent(e) || e.pointerCount > 1
        if (res)
            gestureInProgress = e.pointerCount + 1

        if (e.action == MotionEvent.ACTION_UP) {
            if (gestureInProgress == 0)
                initBars()
            else
                gestureInProgress -= 1

            for ((i, fragment) in fragments.withIndex()) {
                if (frames[i].getGlobalVisibleRect(Rect()))
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

        Effects.dimmer(frames[i])

        val id = group.streams[i]
        startActivity(
            Intent(this, VideoActivity::class.java)
                .putExtra("streamId", id)
        )
    }

    private fun initBars() {
        Effects.cancel()
        binding.toolbar.root.visibility = View.VISIBLE
        binding.llVideoBar.visibility = View.VISIBLE
        if (hideBars) {
            Effects.delayedFadeOut(arrayOf(binding.toolbar.root, binding.llVideoBar))
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        binding.root.viewTreeObserver.addOnGlobalLayoutListener(layoutListener)
    }
}