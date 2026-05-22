package app.coreme.messenger

import android.app.Application
import app.coreme.messenger.core.notifications.NotificationHelper
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class CoremeApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        NotificationHelper.createChannels(this)
    }
}
