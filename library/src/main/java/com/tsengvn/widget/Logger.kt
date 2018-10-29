package com.tsengvn.widget

import android.util.Log

internal object Logger {
    private val TAG = "column_layout_manager"
    private val isDebug = true

    fun d(message: String) {
        Log.d(TAG, message)
    }

    fun v(message: String) {
        Log.v(TAG, message)
    }

    fun e(message: String, error: Throwable? = null) {
        Log.e(TAG, message, error)
    }

}
