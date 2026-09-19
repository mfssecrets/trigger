package com.triggerapp

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.triggerapp.navigation.TriggerApp
import com.triggerapp.splash.SplashViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {

    private val splashViewModel: SplashViewModel by viewModel()

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    /**
     * Installs splash retention, enables edge-to-edge, and sets [TriggerApp] as content.
     *
     * @param savedInstanceState Activity restored state, if any.
     * @author udit
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        // The app UI is forced dark (TriggerTheme darkTheme = true, black app bars), so status
        // bar icons must always be light. The default enableEdgeToEdge() style is "auto", which
        // follows the SYSTEM light/dark mode: on devices set to light mode it produced dark
        // icons over the black dashboard header (invisible clock/battery). SystemBarStyle.dark
        // pins light icons regardless of system theme — same pattern as TriggerProfileCropActivity.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        requestNotificationPermissionIfNeeded()
        splashScreen.setKeepOnScreenCondition { splashViewModel.keepSplashScreen.value }
        setContent {
            TriggerApp()
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val permission = Manifest.permission.POST_NOTIFICATIONS
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) return
        notificationPermissionLauncher.launch(permission)
    }
}
