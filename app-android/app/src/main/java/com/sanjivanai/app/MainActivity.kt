package com.sanjivanai.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.DisposableEffect
import com.sanjivanai.app.core.DemoData
import com.sanjivanai.app.data.MedStore
import com.sanjivanai.app.ui.App
import com.sanjivanai.app.ui.AppState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MedStore.init(applicationContext, DemoData.BASE_MEDICATIONS)
        val state = AppState(applicationContext)
        setContent {
            DisposableEffect(Unit) {
                state.startClock()
                onDispose { state.stopClock() }
            }
            App(state)
        }
    }
}
