package com.vladpen

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class GroupDataModel(val name: String, var streams: MutableList<Int>)

object GroupData {
    var currentGroupId = -1
    private const val fileName = "groups.json"
    private var groups = mutableListOf<GroupDataModel>()

    fun save(context: Context, groupId: Int, group: GroupDataModel) {
        if (groupId < 0)
            groups.add(group)
        else
            groups[groupId] = group

        write(context)
    }

    private fun write(context: Context) {
        val json = Gson().toJson(groups)
        context.openFileOutput(fileName, Context.MODE_PRIVATE).use {
            it.write(json.toByteArray())
        }
    }

    fun getGroups(context: Context): MutableList<GroupDataModel> {
        if (groups.count() == 0) {
            try {
                context.openFileInput(fileName).use { group ->
                    val json = group.bufferedReader().use {
                        it.readText()
                    }
                    initGroups(json)
                }
            } catch (e: Exception) {
                Log.e("Data", "Can't read data file $fileName (${e.localizedMessage})")
            }
        }
        return groups
    }

    fun getById(groupId: Int): GroupDataModel? {
        if (groupId < 0 || groupId >= groups.count())
            return null
        return groups[groupId]
    }

    fun delete(context: Context, groupId: Int) {
        if (groupId < 0)
            return
        groups.removeAt(groupId)
        write(context)
    }

    fun deleteStream(context: Context, streamId: Int) {
        if (streamId < 0)
            return
        groups = getGroups(context)
        val streamsMap = StreamData.getStreamsMap(context, true)

        var toWrite = false
        for ((i, group) in groups.withIndex()) {
            if (!group.streams.contains(streamsMap[streamId]))
                continue
            toWrite = true
            group.streams.remove(streamsMap[streamId])
            if (group.streams.count() < 2)
                delete(context, i)
        }
        if (toWrite)
            write(context)
    }

    fun moveItem(context: Context, from: Int, to: Int) {
        val item = groups.removeAt(from)
        groups.add(to, item)
        write(context)
    }

    private fun initGroups(json: String) {
        if (json == "")
            return
        val listType = object : TypeToken<List<GroupDataModel>>() { }.type
        groups = Gson().fromJson<List<GroupDataModel>>(json, listType).toMutableList()
    }
}
