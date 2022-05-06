package com.vladpen

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class StreamDataModel(
    val name: String,
    var url: String,
    val tcp: Boolean,
    var sftp: String?,
    var id: Int?)

object StreamData {
    private const val fileName = "streams.json"
    private const val muteFileName = "mute.bin"
    private var streams = mutableListOf<StreamDataModel>()

    fun save(context: Context, streamId: Int, stream: StreamDataModel) {
        if (streamId < 0)
            streams.add(stream)
        else
            streams[streamId] = stream

        streams.sortBy { it.name }
        write(context)
    }

    private fun write(context: Context) {
        val json = Gson().toJson(streams)
        context.openFileOutput(fileName, Context.MODE_PRIVATE).use {
            it.write(json.toByteArray())
        }
    }

    fun delete(context: Context, streamId: Int) {
        if (streamId < 0)
            return
        GroupData.deleteStream(context, streamId)
        streams.removeAt(streamId)
        write(context)
    }

    fun getStreams(context: Context): MutableList<StreamDataModel> {
        if (streams.count() == 0) {
            try {
                context.openFileInput(fileName).use { stream ->
                    val json = stream.bufferedReader().use {
                        it.readText()
                    }
                    initStreams(json)
                }
            } catch (e: Exception) {
                Log.e("Data", "Can't read data file $fileName (${e.localizedMessage})")
            }
        }
        return streams
    }

    fun getById(streamId: Int): StreamDataModel? {
        if (streamId < 0 || streamId >= streams.count())
            return null
        return streams[streamId]
    }

    fun getStreamsMap(context: Context, flip: Boolean = false): MutableMap<Int, Int> {
        val allStreams = getStreams(context)
        val streamsMap = mutableMapOf<Int, Int>()
        for ((i, stream) in allStreams.withIndex()) {
            if (stream.id != null) {
                if (!flip)
                    streamsMap[stream.id!!] = i
                else
                    streamsMap[i] = stream.id!!
            }
        }
        return streamsMap
    }

    fun getStreamsIds(context: Context, indices: MutableSet<Int>): MutableList<Int> {
        streams = getStreams(context)
        val streamsIds = mutableListOf<Int>()
        var toWrite = false
        for (i in indices) {
            if (streams[i].id == null) {
                streams[i].id = generateUniqueId()
                toWrite = true
            }
            streamsIds.add(streams[i].id!!)
        }
        if (toWrite)
            write(context)
        return streamsIds
    }

    fun setMute(context: Context, mute: Int) {
        try {
            context.openFileOutput(muteFileName, Context.MODE_PRIVATE).use {
                it.write(mute)
            }
        } catch (e: Exception) {
            Log.e("Data", "Can't write the file $muteFileName (${e.localizedMessage})")
        }
    }

    fun getMute(context: Context): Int {
        try {
            context.openFileInput(muteFileName).use {
                return it.read()
            }
        } catch (e: Exception) {
            Log.e("Data", "Can't read the file $muteFileName (${e.localizedMessage})")
        }
        return 0
    }

    private fun initStreams(json: String) {
        if (json == "")
            return
        val listType = object : TypeToken<List<StreamDataModel>>() { }.type
        streams = Gson().fromJson<List<StreamDataModel>>(json, listType).toMutableList()
    }

    private fun generateUniqueId(): Int {
        var id = (100000..999999).random()
        var done = false
        while (!done) {
            done = true
            for (s in streams) {
                if (s.id != id)
                    continue
                id = (100000..999999).random()
                done = false
                break
            }
        }
        return id
    }
}
