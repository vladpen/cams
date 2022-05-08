package com.vladpen.cams

import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.PopupMenu

class MainMenu(val context: Context) {

    fun showPopupMenu(view: View, showItem: String) {
        val popup = PopupMenu(context, view)
        popup.menuInflater.inflate(R.menu.main_menu, popup.menu)

        when (showItem) {
            "streamAdd" -> {
                popup.menu.findItem(R.id.iStreamAdd).isVisible = true
            }
            "groupAdd" -> {
                popup.menu.findItem(R.id.iGroupAdd).isVisible = true
            }
        }

        popup.setOnMenuItemClickListener { item ->
            when (item!!.itemId) {
                R.id.iStreamAdd -> {
                    editScreen()
                }
                R.id.iGroupAdd -> {
                    editGroupScreen()
                }
                R.id.iAbout -> {
                     aboutScreen()
                }
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
        val intent = Intent(context, EditGroupActivity::class.java)
        context.startActivity(intent)
    }

    private fun aboutScreen() {
        // val intent = Intent(context, AboutActivity::class.java)
        // context.startActivity(intent)
    }
}