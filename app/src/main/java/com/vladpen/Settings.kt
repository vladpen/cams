package com.vladpen

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import com.vladpen.cams.MainActivity
import com.vladpen.cams.R

class Settings(val context: MainActivity)  {

    fun export(): ActivityResultLauncher<Intent> {
        return context.registerForActivityResult(ActivityResultContracts.StartActivityForResult())
        { result ->
            try {
                if (result.resultCode != Activity.RESULT_OK)
                    return@registerForActivityResult

                result.data?.data?.let { uri ->
                    context.contentResolver?.openOutputStream(uri, "w").use { outputStream ->
                        StreamData.getStreams(context)
                        GroupData.getGroups(context)
                        outputStream?.bufferedWriter().use {
                            it?.write("${StreamData.toJson()}\n${GroupData.toJson()}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("Settings", "export (${e.localizedMessage})")
            }
        }
    }

    fun import(): ActivityResultLauncher<Intent> {
        return context.registerForActivityResult(ActivityResultContracts.StartActivityForResult())
        { result ->
            try {
                if (result.resultCode != Activity.RESULT_OK)
                    return@registerForActivityResult

                result.data?.data?.let { uri ->
                    context.contentResolver?.openInputStream(uri).let { inputStream ->
                        val fileContent = inputStream?.bufferedReader().use {
                            it?.readText()?.trim()?.lines()
                        }
                        if (fileContent?.count() == 2)
                            renewSettings(fileContent)
                    }
                }
            } catch (e: Exception) {
                Log.e("Settings", "import (${e.localizedMessage})")
            }
        }
    }

    private fun renewSettings(content: List<String>) {
        AlertDialog.Builder(context)
            .setMessage(R.string.renew_settings)
            .setPositiveButton(R.string.ok) { _, _ ->
                try {
                    StreamData.initStreams(content[0])
                    StreamData.write(context)
                    GroupData.initGroups(content[1])
                    GroupData.write(context)
                } catch (e: Error) {
                    Log.e("Settings", "import: parse error (${e.localizedMessage})")
                }
                context.finish()
                context.startActivity(context.intent)
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .create().show()
    }
}