package com.vladpen

import android.content.Context
import android.content.Intent

object Navigator {
    fun go(context: Context, intent: Intent) {
        context.startActivity(intent)
    }
}