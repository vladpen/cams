package com.vladpen

import androidx.recyclerview.widget.RecyclerView

class SourceItemTouch : ItemTouch() {
    override fun moveItem(
        from: Int,
        to: Int,
        adapter: RecyclerView.Adapter<RecyclerView.ViewHolder>?
    ) {
        (adapter as SourceAdapter).moveItem(from, to)
    }
}