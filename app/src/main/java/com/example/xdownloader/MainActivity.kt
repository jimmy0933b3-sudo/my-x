package com.example.xdownloader

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import com.example.xdownloader.ui.screens.MainAppScreen
import com.example.xdownloader.ui.theme.XDownloaderTheme

class MainActivity : ComponentActivity() {
    private var incomingPostUrl by mutableStateOf<String?>(null)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { !it }) {
            Toast.makeText(this, getString(R.string.permission_denied), Toast.LENGTH_SHORT).show()
        }
    }

    private fun extractSharedPostUrl(intent: Intent?): String? {
        if (intent?.action != Intent.ACTION_SEND || intent.type != "text/plain") return null
        val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return null
        val pattern = Regex("""https?://(?:www\\.|mobile\\.)?(?:x\\.com|twitter\\.com)/[^\\s/]+/status/\\d+(?:[^\\s]*)?""")
        return pattern.find(text)?.value?.trimEnd('.', ')', ',', '。')
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkAndRequestPermissions()
        incomingPostUrl = extractSharedPostUrl(intent)
        setContent {
            XDownloaderTheme {
                MainAppScreen(
                    sharedPostUrl = incomingPostUrl,
                    onSharedPostConsumed = { incomingPostUrl = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractSharedPostUrl(intent)?.let { incomingPostUrl = it }
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        if (permissions.isNotEmpty()) {
            requestPermissionLauncher.launch(permissions.toTypedArray())
        }
    }
}
