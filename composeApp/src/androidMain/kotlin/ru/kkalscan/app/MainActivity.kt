package ru.kkalscan.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import ru.kkalscan.app.analytics.KkalAnalytics
import ru.kkalscan.data.storage.AndroidDeviceIdContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidDeviceIdContext.init(applicationContext)
        KkalAnalytics.init(applicationContext, application, BuildConfig.APPMETRICA_API_KEY)
        enableEdgeToEdge()
        setContent { App() }
    }
}
