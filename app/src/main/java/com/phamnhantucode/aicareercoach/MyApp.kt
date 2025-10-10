package com.phamnhantucode.aicareercoach

import android.app.Application
import com.clerk.api.Clerk

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Clerk.initialize(this, BuildConfig.CLERK_PUBLISHABLE_KEY)
    }
}
