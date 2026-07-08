package ru.kkalscan.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity

/**
 * Required by Health Connect on Android 14+ when the user taps "Privacy policy"
 * in the permission rationale flow.
 */
class PermissionsRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("https://kkalscan.ru/privacy")),
        )
        finish()
    }
}
