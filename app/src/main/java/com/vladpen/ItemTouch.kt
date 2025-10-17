package com.vladpen

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

open class ItemTouch {
    open fun moveItem(
        from: Int,
        to: Int,
        adapter: RecyclerView.Adapter<RecyclerView.ViewHolder>?) {}

    fun helper(): ItemTouchHelper {
        val itemTouchCallback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP
                    or ItemTouchHelper.DOWN
                    or ItemTouchHelper.START
                    or ItemTouchHelper.END,
            0) {

            override fun onMove(recyclerView: RecyclerView,
                                viewHolder: RecyclerView.ViewHolder,
                                target: RecyclerView.ViewHolder): Boolean {
                val adapter = recyclerView.adapter
                val from = viewHolder.bindingAdapterPosition
                val to = target.bindingAdapterPosition

                moveItem(from, to, adapter)
                adapter?.notifyItemMoved(from, to)

                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?,
                                           actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                    viewHolder?.itemView?.alpha = 0.5f
                }
            }

            override fun clearView(recyclerView: RecyclerView,
                                   viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                viewHolder.itemView.alpha = 1.0f
            }
        }
        return ItemTouchHelper(itemTouchCallback)
    }
}