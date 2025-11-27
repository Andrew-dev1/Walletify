package hu.ait.walletify

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

// needs to be defined in Manifest file but why is this needed again?
@HiltAndroidApp
class MainApplication: Application() {

}