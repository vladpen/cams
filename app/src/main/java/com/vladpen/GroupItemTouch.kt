package com.vladpen

import androidx.recyclerview.widget.RecyclerView

class GroupItemTouch : ItemTouch() {
    override fun moveItem(
        from: Int,
        to: Int,
        adapter: RecyclerView.Adapter<RecyclerView.ViewHolder>?
    ) {
        (adapter as GroupAdapter).moveItem(from, to)
    }
}