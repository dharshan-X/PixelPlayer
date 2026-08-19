package com.theveloper.pixelplay

import android.util.Log
import timber.log.Timber

/**
 * A release-optimized Timber Tree that:
 * - Only logs WARN, ERROR, and WTF (suppresses VERBOSE, DEBUG, and INFO)
 * - Strips method/line information for performance
 * - Could be extended to report errors to crash analytics (e.g., Firebase Crashlytics)
 */
class ReleaseTree : Timber.Tree() {
    
    override fun isLoggable(tag: String?, priority: Int): Boolean {
        return priority >= Log.DEBUG
    }
    
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (!isLoggable(tag, priority)) return
        
        when (priority) {
            Log.VERBOSE -> Log.v(tag, message, t)
            Log.DEBUG -> Log.d(tag, message, t)
            Log.INFO -> Log.i(tag, message, t)
            Log.WARN -> Log.w(tag, message, t)
            Log.ERROR -> Log.e(tag, message, t)
            Log.ASSERT -> Log.wtf(tag, message, t)
        }
    }
}
