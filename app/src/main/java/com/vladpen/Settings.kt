package com.vladpen

import android.app.Activity
import android.content.Intent
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import com.vladpen.cams.MainActivity
import com.vladpen.cams.R
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

private const val MIN_PASSWORD_LEN = 6

class Settings(val context: MainActivity)  {
    private val filePrefix = "cams"
    private val fileExt = "cfg"
    private val input by lazy { getEditText() }
    private val inputConfirm by lazy { getEditText() }

    companion object {
        private var password: String = ""
    }

    fun exportDialog(launcher: ActivityResultLauncher<Intent>) {
        val dialog = getDialog(getExportDialogContent())
        dialog.apply {
            setOnShowListener {
                getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    val userPassword = input.text.toString().trim()
                    if (userPassword.length < MIN_PASSWORD_LEN) {
                        input.error = context.getString(R.string.err_invalid)
                    } else if (inputConfirm.text.toString().trim() != userPassword) {
                        inputConfirm.error = context.getString(R.string.err_invalid)
                    } else {
                        password = userPassword

                        val sdf = SimpleDateFormat("yyMMdd", Locale.getDefault())
                        val currentDate = sdf.format(Date())

                        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                            addCategory(Intent.CATEGORY_OPENABLE)
                            type = "application/octet-stream"
                            putExtra(Intent.EXTRA_TITLE, "$filePrefix$currentDate.$fileExt")
                        }
                        launcher.launch(intent)
                        dismiss()
                    }
                }
            }
        }.show()
    }

    fun export(): ActivityResultLauncher<Intent> {
        return context.registerForActivityResult(ActivityResultContracts.StartActivityForResult())
        { result ->
            try {
                if (result.resultCode == Activity.RESULT_OK)
                    result.data?.data?.let { uri ->
                        context.contentResolver?.openOutputStream(uri).use { outputStream ->
                            outputStream as FileOutputStream
                            outputStream.channel.truncate(0)
                            outputStream.bufferedWriter().use {
                                it.write(encodeSettings())
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
                if (result.resultCode == Activity.RESULT_OK)
                    result.data?.data?.let { uri ->
                        context.contentResolver?.openInputStream(uri).let { inputStream ->
                            val fileContent = inputStream?.bufferedReader().use {
                                it?.readText()?.trim()?.lines()
                            }
                            if (fileContent != null)
                                decodeSettings(fileContent)
                            else
                                Log.e("Settings", "Invalid file content")
                        }
                    }
            } catch (e: Exception) {
                Log.e("Settings", "import (${e.localizedMessage})")
            }
        }
    }

    private fun decodeSettings(content: List<String>) {
        val dialog = getDialog(getImportDialogContent())
        dialog.apply {
            setOnShowListener {
                getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    val password = input.text.toString().trim()
                    if (password.length < MIN_PASSWORD_LEN) {
                        input.error = context.getString(R.string.err_invalid)
                    } else {
                        try {
                            restoreSettings(content, password)
                            dismiss()
                        } catch (e: Exception) {
                            input.error = context.getString(R.string.err_invalid)
                            Log.e("Settings", "Can't decode (${e.localizedMessage})")
                        }
                    }
                }
            }
        }.show()
    }

    private fun restoreSettings(content: List<String>, password: String) {
        val streams = StreamData.fromJson(content[0])
        for (stream in streams) {
            stream.url = restoreUrl(stream.url, password)!!
            stream.sftp = restoreUrl(stream.sftp, password)
        }
        StreamData.save()
        if (content.count() > 1) {
            GroupData.fromJson(content[1])
            GroupData.save()
        }
        if (content.count() > 2) {
            SourceData.fromJson(content[2])
            SourceData.save()
        } else {
            SourceData.createSources()
        }
        SourceData.validate()
        context.finish()
        val intent = Intent(context, MainActivity::class.java)
        context.startActivity(intent)
    }

    private fun restoreUrl(url: String?, password: String): String? {
        if (url == null)
            return url

        val parsedUrl = Utils.parseUrl(url)
        if (parsedUrl == null || parsedUrl.password == "")
            return url

        val decodedPassword = Utils.decodeString(parsedUrl.password, password)
        if (decodedPassword == parsedUrl.password)
            throw Exception("invalid password")
        val encodedPassword = Utils.encodeString(decodedPassword)
        return Utils.replacePassword(url, encodedPassword)
    }

    private fun encodeSettings(): String {
        val reEncodedStreams = StreamData.getAll().map { it.copy() }
        for (stream in reEncodedStreams) {
            stream.url = storeUrl(stream.url, password)!!
            stream.sftp = storeUrl(stream.sftp, password)
        }
        val streams = StreamData.toJson(reEncodedStreams)
        val groups = GroupData.toJson(GroupData.getAll())
        val sources = SourceData.toJson(SourceData.getAll())
        return "$streams\n$groups\n$sources"
    }

    private fun storeUrl(url: String?, password: String): String? {
        if (url == null)
            return url

        val parsedUrl = Utils.parseUrl(url)
        if (parsedUrl == null || parsedUrl.password == "")
            return url

        val decodedPassword = Utils.decodeString(parsedUrl.password)
        val encodedPassword = Utils.encodeString(decodedPassword, password)
        return Utils.replacePassword(url, encodedPassword)
    }

    private fun getEditText(): EditText {
        val editText = EditText(context)
        editText.transformationMethod = PasswordTransformationMethod.getInstance()
        editText.gravity = Gravity.CENTER
        return editText
    }

    private fun getDialog(layout: LinearLayout): AlertDialog {
        return AlertDialog.Builder(context)
            .setView(layout)
            .setPositiveButton(R.string.btn_continue, null)
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                (layout.parent as ViewGroup).removeView(layout)
                dialog.dismiss()
            }
            .create()
    }

    private fun getLayout(): LinearLayout {
        val layout = LinearLayout(context)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(64, 0, 64, 0)
        return layout
    }

    private fun getTextView(title: String): TextView {
        val tv = TextView(context)
        tv.text = title
        tv.setPadding(0, 64, 0, 0)
        return tv
    }

    private fun getExportDialogContent(): LinearLayout {
        val layout = getLayout()
        layout.addView(getTextView(context.getString(R.string.export_password, MIN_PASSWORD_LEN)))
        layout.addView(input)

        layout.addView(getTextView(context.getString(R.string.export_password_confirm)))
        layout.addView(inputConfirm)
        return layout
    }

    private fun getImportDialogContent(): LinearLayout {
        val layout = getLayout()
        layout.addView(getTextView(context.getString(R.string.import_password)))
        layout.addView(input)
        return layout
    }
}