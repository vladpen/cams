package com.vladpen

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

data class StreamDataModel(val name: String, val url: String, val tcp: Boolean)

object StreamData {
    private const val fileName = "streams.json"
    private var streams = mutableListOf<StreamDataModel>()

    fun save(context: Context, position: Int, stream: StreamDataModel) {
        if (position < 0) {
            streams.add(stream)
        } else {
            streams[position] = stream
        }
        streams.sortBy { it.name }
        write(context)
    }

    fun delete(context: Context, position: Int) {
        if (position < 0) {
            return
        }
        streams.removeAt(position)
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

    fun getByPosition(position: Int): StreamDataModel? {
        if (position < 0 || position >= streams.count()) {
            return null
        }
        return streams[position]
    }

    private fun initStreams(json: String) {
        if (json == "") {
            return
        }
        val listType = object : TypeToken<List<StreamDataModel>>() { }.type
        streams = Gson().fromJson<List<StreamDataModel>>(json, listType).toMutableList()
    }
}
