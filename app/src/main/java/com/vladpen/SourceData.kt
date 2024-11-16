package com.vladpen

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.vladpen.Utils.getOption
import com.vladpen.Utils.saveOption
import com.vladpen.cams.MainApp.Companion.context

data class SourceDataModel(val type: String, var id: Int)

object SourceData {
    private const val SOURCES_FILE_NAME = "sources.json"
    private const val STARTUP_FILE_NAME = "startup.json"
    private const val STRETCH_FILE_NAME = "stretch.bin"
    private var sources = mutableListOf<SourceDataModel>()

    fun getAll(): MutableList<SourceDataModel> {
        if (sources.isNotEmpty())
            return sources

        try {
            context.openFileInput(SOURCES_FILE_NAME).use { inputStream ->
                val json = inputStream.bufferedReader().use {
                    it.readText()
                }
                fromJson(json)
                validate()
            }
        } catch (e: Exception) {
            createSources()
        }
        return sources
    }

    fun getById(sourceId: Int): SourceDataModel? {
        if (sourceId < 0 || sourceId >= sources.count())
            return null
        return sources[sourceId]
    }

    fun add(source: SourceDataModel) {
        sources.add(source)
        save()
    }

    fun save() {
        context.openFileOutput(SOURCES_FILE_NAME, Context.MODE_PRIVATE).use {
            it.write(toJson(sources).toByteArray())
        }
    }

    fun delete(type: String, id: Int) {
        sources.removeAll { it.type == type && it.id == id }
        for (source in sources) {
            if (source.type == type && source.id > id)
                source.id -= 1
        }
        save()

        val startup = getStartup()
        if (startup == null || startup.type != type)
            return
        if (startup.id == id)
            saveStartup(null)
        else if (startup.id > id)
            saveStartup(SourceDataModel(type, startup.id - 1))
    }

    fun moveItem(from: Int, to: Int) {
        val item = sources.removeAt(from)
        sources.add(to, item)
        save()
    }

    fun toJson(data: List<SourceDataModel>): String {
        return Gson().toJson(data)
    }

    fun fromJson(json: String): List<SourceDataModel> {
        if (json == "")
            return sources
        try {
            val listType = object : TypeToken<List<SourceDataModel>>() { }.type
            sources = Gson().fromJson<List<SourceDataModel>>(json, listType).toMutableList()
        } catch (e: Exception) {
            Log.e("SourceData", "Can't parse (${e.localizedMessage})")
        }
        return sources
    }

    fun createSources() {
        sources = (StreamData.getAll().indices.map { SourceDataModel("stream", it) } +
                GroupData.getAll().indices.map { SourceDataModel("group", it) })
            .toMutableList()
        save()
    }

    fun validate() {
        val streamSources = sources.filter { it.type == "stream" }
        val groupSources = sources.filter { it.type == "group" }
        val streamCount = StreamData.getAll().count()
        val groupCount = GroupData.getAll().count()
        val maxStreamId = streamSources.maxByOrNull { it.id }?.id
        val maxGroupId = groupSources.maxByOrNull { it.id }?.id

        if (sources.count() != streamCount + groupCount ||
            streamSources.count() != streamCount ||
            groupSources.count() != groupCount ||
            (maxStreamId != null && maxStreamId > streamCount - 1) ||
            (maxGroupId != null && maxGroupId > groupCount - 1)
        )
            createSources()
    }

    fun setStartup(isSet: Boolean, type: String, id: Int) {
        if (isSet) {
            saveStartup(SourceDataModel(type, id))
        } else {
            val startup = getStartup()
            if (startup != null && startup.type == type && startup.id == id) {
                saveStartup(null)
            }
        }
    }

    fun saveStartup(source: SourceDataModel?) {
        context.openFileOutput(STARTUP_FILE_NAME, Context.MODE_PRIVATE).use {
            it.write(Gson().toJson(source).toByteArray())
        }
    }

    fun getStartup(): SourceDataModel? {
        try {
            context.openFileInput(STARTUP_FILE_NAME).use { inputStream ->
                val json = inputStream.bufferedReader().use {
                    it.readText()
                }
                return Gson().fromJson(json, SourceDataModel::class.java)
            }
        } catch (e: Exception) {
            Log.e("SourceData", "Can't read startup (${e.localizedMessage})")
        }
        return null
    }

    fun setStretch(isSet: Boolean) {
        saveOption(STRETCH_FILE_NAME, if (isSet) 1 else 0 )
    }

    fun getStretch(): Boolean {
        return getOption(STRETCH_FILE_NAME) == 1
    }
}