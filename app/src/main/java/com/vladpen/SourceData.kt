package com.vladpen

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.vladpen.cams.MainApp.Companion.context

data class SourceDataModel(val type: String, var id: Int)

object SourceData {
    private const val fileName = "sources.json"
    private var sources = mutableListOf<SourceDataModel>()

    fun getAll(): MutableList<SourceDataModel> {
        if (sources.isNotEmpty())
            return sources

        try {
            context.openFileInput(fileName).use { inputStream ->
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
        context.openFileOutput(fileName, Context.MODE_PRIVATE).use {
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
}