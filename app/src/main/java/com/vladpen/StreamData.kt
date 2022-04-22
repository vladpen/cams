package com.vladpen

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class StreamDataModel(
    val name: String,
    var url: String,
    val tcp: Boolean,
    var sftp: String?)

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

    fun delete(context: Context, streamId: Int) {
        if (streamId < 0)
            return
        streams.removeAt(streamId)
        write(context)
    }

    private fun write(context: Context) {
        val json = Gson().toJson(streams)
        context.openFileOutput(fileName, Context.MODE_PRIVATE).use {
            it.write(json.toByteArray())
        }
    }

    fun getStreams(context: Context): MutableList<StreamDataModel> {
        if (streams.size == 0) {
            try {
                context.openFileInput(fileName).use { stream ->
                    val json = stream.bufferedReader().use {
                        it.readText()
                    }
                    initStreams(json)
                }
            } catch (e: Exception) {
                Log.e("Data", e.localizedMessage ?: "Can't read data file $fileName")
            }
        }
        return streams
    }

    fun getById(streamId: Int): StreamDataModel? {
        if (streamId < 0 || streamId >= streams.count())
            return null
        return streams[streamId]
    }

    fun setMute(context: Context, mute: Int) {
        try {
            context.openFileOutput(muteFileName, Context.MODE_PRIVATE).use {
                it.write(mute)
            }
        } catch (e: Exception) {
            Log.e("Data", e.localizedMessage ?: "Can't write the file $muteFileName")
        }
    }

    fun getMute(context: Context): Int {
        try {
            context.openFileInput(muteFileName).use {
                return it.read()
            }
        } catch (e: Exception) {
            Log.e("Data", e.localizedMessage ?: "Can't read the file $muteFileName")
        }
        return 0
    }

    private fun initStreams(json: String) {
        if (json == "")
            return
        val listType = object : TypeToken<List<StreamDataModel>>() { }.type
        streams = Gson().fromJson<List<StreamDataModel>>(json, listType).toMutableList()
    }
}
