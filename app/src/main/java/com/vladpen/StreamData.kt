package com.vladpen

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

data class StreamDataModel(val name: String, val url: String, val tcp: Boolean, var sftp: String?)

object StreamData {
    private const val fileName = "streams.json"
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
                val filesDir = context.filesDir

                if (File(filesDir, fileName).exists()) {
                    val json: String = File(filesDir, fileName).readText()
                    initStreams(json)
                } else {
                    Log.i("DATA", "Data file $fileName does not exist")
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

    private fun initStreams(json: String) {
        if (json == "")
            return
        val listType = object : TypeToken<List<StreamDataModel>>() { }.type
        streams = Gson().fromJson<List<StreamDataModel>>(json, listType).toMutableList()
    }
}
