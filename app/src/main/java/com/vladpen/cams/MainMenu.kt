package com.vladpen.cams

import android.content.Context
import android.view.View
import android.widget.PopupMenu

class MainMenu(val context: Context) {
    fun showPopupMenu(view: View) {
        val popup = PopupMenu(context, view)
        popup.menuInflater.inflate(R.menu.main_menu, popup.menu)

        popup.setOnMenuItemClickListener { item ->
            when (item!!.itemId) {
                R.id.iAbout -> {
                     aboutScreen()
                }
            }
            true
        }
        popup.show()
    }
    private fun aboutScreen() {
        // val intent = Intent(context, AboutActivity::class.java)
        // context.startActivity(intent)
    }
}