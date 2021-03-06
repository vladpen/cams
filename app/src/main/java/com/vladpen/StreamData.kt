package com.vladpen

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.vladpen.cams.MainApp.Companion.context

data class StreamDataModel(val name: String, var url: String, val tcp: Boolean, var sftp: String?)

object StreamData {
    private const val fileName = "streams.json"
    private const val muteFileName = "mute.bin"
    private var streams = mutableListOf<StreamDataModel>()
    var logConnections = false

    fun getAll(): MutableList<StreamDataModel> {
        if (streams.isEmpty()) {
            try {
                context.openFileInput(fileName).use { inputStream ->
                    val json = inputStream.bufferedReader().use {
                        it.readText()
                    }
                    fromJson(json)
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
        context.openFileOutput(fileName, Context.MODE_PRIVATE).use {
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
            Log.e("StreamData", "Can't parse (${e.localizedMessage})")
        }
        return streams
    }

    fun setMute(mute: Int) {
        try {
            context.openFileOutput(muteFileName, Context.MODE_PRIVATE).use {
                it.write(mute)
            }
        } catch (e: Exception) {
            Log.e("Data", "Can't write the file $muteFileName (${e.localizedMessage})")
        }
    }

    fun getMute(): Int {
        try {
            context.openFileInput(muteFileName).use {
                return it.read()
            }
        } catch (e: Exception) {
            Log.e("Data", "Can't read the file $muteFileName (${e.localizedMessage})")
        }
        return 0
    }
}