package com.vladpen.cams

import android.content.Intent
import android.view.View
import android.widget.PopupMenu
import com.vladpen.Settings
import com.vladpen.StreamData

class MainMenu(val context: MainActivity) {

    fun showPopupMenu(view: View) {
        val popup = PopupMenu(context, view)
        popup.menuInflater.inflate(R.menu.main_menu, popup.menu)

        if (StreamData.getAll().count() < 2)
            popup.menu.findItem(R.id.iGroupAdd).isVisible = false

        popup.setOnMenuItemClickListener { item ->
            when (item!!.itemId) {
                R.id.iStreamAdd -> editScreen()
                R.id.iGroupAdd -> editGroupScreen()
                R.id.iExport -> export()
                R.id.iImport -> import()
                R.id.iAbout -> aboutScreen()
            }
            true
        }
        popup.show()
    }

    private fun editScreen() {
        val intent = Intent(context, EditActivity::class.java)
        context.startActivity(intent)
    }

    private fun editGroupScreen() {
        val intent = Intent(context, GroupEditActivity::class.java)
        context.startActivity(intent)
    }

    private fun export() {
        Settings(context).exportDialog(context.exportSettings)
    }

    fun import() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        context.importSettings.launch(intent)
    }

    private fun aboutScreen() {
        val intent = Intent(context, AboutActivity::class.java)
        context.startActivity(intent)
    }
}