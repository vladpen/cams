package com.vladpen

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.vladpen.Utils.getOption
import com.vladpen.Utils.saveOption
import com.vladpen.cams.MainApp.Companion.context

data class StreamDataModel(
    val name: String,
    var url: String,
    var url2: String?,
    val tcp: Boolean,
    var sftp: String?,
    val alert: Boolean?)

object StreamData {
    private const val STREAM_FILE_NAME = "streams.json"
    private const val MUTE_FILE_NAME = "mute.bin"
    private const val CAM_CHANNEL_FILE_NAME = "channel.bin"
    private const val GROUP_CHANNEL_FILE_NAME = "groupChannel.bin"
    private var streams = mutableListOf<StreamDataModel>()
    var logConnections = false
    var copyStreamId = -1

    fun getAll(): MutableList<StreamDataModel> {
        if (streams.isNotEmpty())
            return streams

        return try {
            context.openFileInput(STREAM_FILE_NAME).use { inputStream ->
                val json = inputStream.bufferedReader().use {
                    it.readText()
                }
                fromJson(json)
            }
        } catch (e: Exception) {
            streams
        }
    }

    fun getById(streamId: Int): StreamDataModel? {
        if (streams.isEmpty()) {
            this.getAll()
        }
        if (streamId < 0 || streamId >= streams.count())
            return null
        return streams[streamId]
    }

    fun add(stream: StreamDataModel) {
        streams.add(stream)
        save()
        SourceData.add(SourceDataModel("stream", streams.count() - 1))
    }

    fun update(streamId: Int, stream: StreamDataModel) {
        streams[streamId] = stream
        save()
    }

    fun save() {
        context.openFileOutput(STREAM_FILE_NAME, Context.MODE_PRIVATE).use {
            it.write(toJson(streams).toByteArray())
        }
    }

    fun delete(streamId: Int) {
        if (streamId < 0)
            return
        streams.removeAt(streamId)
        save()
        GroupData.deleteStream(streamId)
        SourceData.delete("stream", streamId)
    }

    fun toJson(data: List<StreamDataModel>): String {
        return Gson().toJson(data)
    }

    fun fromJson(json: String): MutableList<StreamDataModel> {
        if (json == "")
            return streams
        try {
            val listType = object : TypeToken<List<StreamDataModel>>() { }.type
            streams = Gson().fromJson<List<StreamDataModel>>(json, listType).toMutableList()
        } catch (e: Exception) {
            Log.e("StreamData", "Can't parse json (${e.localizedMessage})")
        }
        return streams
    }

    fun setMute(mute: Int) {
        saveOption(MUTE_FILE_NAME, mute)
    }

    fun getMute(): Int {
        return getOption(MUTE_FILE_NAME)
    }

    fun setChannel(channel: Int) {
        saveOption(CAM_CHANNEL_FILE_NAME, channel)
    }

    fun getChannel(): Int {
        return getOption(CAM_CHANNEL_FILE_NAME)
    }

    fun setGroupChannel(channel: Int) {
        saveOption(GROUP_CHANNEL_FILE_NAME, channel)
    }

    fun getGroupChannel(): Int {
        return getOption(GROUP_CHANNEL_FILE_NAME, 1)
    }

    fun getUrl(stream: StreamDataModel): String {
        val url = if (stream.url2 != null && getChannel() == 1)
            stream.url2!!
        else
            stream.url
        return Utils.getFullUrl(url, 554, "rtsp")
    }
}