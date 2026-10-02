package com.mobile.core.data.platform

import android.app.Activity
import android.app.Application
import android.os.Bundle
import java.lang.ref.WeakReference

/**
 * Tracks the foreground Activity for APIs that need one to show system UI
 * (e.g. Credential Manager) but are called from data-layer singletons.
 * Holds only a weak reference, so it never leaks an Activity.
 *
 * Created eagerly by Koin so it is registered before the first Activity starts.
 */
class CurrentActivityProvider(application: Application) {

    private var current: WeakReference<Activity>? = null

    val currentActivity: Activity?
        get() = current?.get()?.takeUnless { it.isFinishing || it.isDestroyed }

    init {
        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = track(activity)
            override fun onActivityStarted(activity: Activity) = track(activity)
            override fun onActivityResumed(activity: Activity) = track(activity)
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivityStopped(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

            override fun onActivityDestroyed(activity: Activity) {
                // Only clear if nothing newer has taken over (e.g. after a rotation)
                if (current?.get() === activity) {
                    current = null
                }
            }
        })
    }

    private fun track(activity: Activity) {
        current = WeakReference(activity)
    }
}
