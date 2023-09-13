package com.vladpen

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.vladpen.cams.MainApp.Companion.context

data class GroupDataModel(val name: String, var streams: MutableList<Int>)

object GroupData {
    private const val fileName = "groups.json"
    private var groups = mutableListOf<GroupDataModel>()
    var currentGroupId = -1

    fun getAll(): MutableList<GroupDataModel> {
        if (groups.isNotEmpty())
            return groups

        return try {
            context.openFileInput(fileName).use { inputStream ->
                val json = inputStream.bufferedReader().use {
                    it.readText()
                }
                fromJson(json)
            }
        } catch (e: Exception) {
            groups
        }
    }

    fun getById(groupId: Int): GroupDataModel? {
        if (groupId < 0 || groupId >= groups.count())
            return null
        return groups[groupId]
    }

    fun add(group: GroupDataModel) {
        groups.add(group)
        save()
        SourceData.add(SourceDataModel("group", groups.count() - 1))
    }

    fun update(groupId: Int, group: GroupDataModel) {
        groups[groupId] = group
        save()
    }

    fun save() {
        context.openFileOutput(fileName, Context.MODE_PRIVATE).use {
            it.write(toJson(groups).toByteArray())
        }
    }

    fun delete(groupId: Int) {
        if (groupId < 0)
            return
        groups.removeAt(groupId)
        save()
        SourceData.delete("group", groupId)
    }

    fun deleteStream(streamId: Int) {
        for ((id, group) in groups.withIndex()) {
            if (group.streams.contains(streamId))
                group.streams.remove(streamId)
            for (i in group.streams.indices) {
                if (group.streams[i] > streamId)
                    group.streams[i] -= 1
            }
            if (group.streams.count() < 2)
                delete(id)
        }
        save()
    }

    fun toJson(data: List<GroupDataModel>): String {
        return Gson().toJson(data)
    }

    fun fromJson(json: String): MutableList<GroupDataModel> {
        if (json == "")
            return groups
        try {
            val listType = object : TypeToken<List<GroupDataModel>>() { }.type
            groups = Gson().fromJson<List<GroupDataModel>>(json, listType).toMutableList()
        } catch (e: Exception) {
            Log.e("GroupData", "Can't parse (${e.localizedMessage})")
        }
        return groups
    }
}