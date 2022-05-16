package com.vladpen.cams

import android.content.Intent
import android.view.View
import android.widget.PopupMenu

class MainMenu(val context: MainActivity) {

    fun showPopupMenu(view: View) {
        val popup = PopupMenu(context, view)
        popup.menuInflater.inflate(R.menu.main_menu, popup.menu)

        if (context.getMode() == "streams")
            popup.menu.findItem(R.id.iStreamAdd).isVisible = true
        else if (context.getMode() == "groups")
            popup.menu.findItem(R.id.iGroupAdd).isVisible = true

        popup.setOnMenuItemClickListener { item ->
            when (item!!.itemId) {
                R.id.iStreamAdd -> editScreen()
                R.id.iGroupAdd -> editGroupScreen()
                R.id.iExport -> exportScreen()
                R.id.iImport -> importScreen()
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

    private fun exportScreen() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_TITLE, "cams.cfg")
        }
        context.exportSettings.launch(intent)
    }

    private fun importScreen() {
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