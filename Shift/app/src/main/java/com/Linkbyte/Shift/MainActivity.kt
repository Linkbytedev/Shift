package com.Linkbyte.Shift

import com.Linkbyte.Shift.BuildConfig


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.Linkbyte.Shift.domain.repository.AuthRepository
import com.Linkbyte.Shift.presentation.navigation.AppNavigation
import com.Linkbyte.Shift.presentation.navigation.Screen
import com.Linkbyte.Shift.ui.theme.ShiftTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

import android.view.WindowManager

import androidx.fragment.app.FragmentActivity

@AndroidEntryPoint class MainActivity : FragmentActivity() {
    
    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var signalingClient: com.Linkbyte.Shift.webrtc.SignalingClient
    
    @Inject
    lateinit var updateRepository: com.Linkbyte.Shift.domain.repository.UpdateRepository

    @Inject
    lateinit var securityPreferences: com.Linkbyte.Shift.data.preferences.SecurityPreferences

    @Inject
    lateinit var themePreferences: com.Linkbyte.Shift.data.preferences.ThemePreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        
        setContent {
            val themeMode by themePreferences.themeMode.collectAsState(initial = com.Linkbyte.Shift.data.preferences.ThemeMode.SYSTEM)
            val isScreenSecurityEnabled by securityPreferences.isScreenSecurityEnabled.collectAsState(initial = true)

            // Dynamic Screen Security
            androidx.compose.runtime.LaunchedEffect(isScreenSecurityEnabled) {
                if (isScreenSecurityEnabled) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }
            val darkTheme = when (themeMode) {
                com.Linkbyte.Shift.data.preferences.ThemeMode.LIGHT -> false
                com.Linkbyte.Shift.data.preferences.ThemeMode.DARK -> true
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            ShiftTheme(darkTheme = darkTheme) {
                val navController = rememberNavController()
                val currentUser by authRepository.getCurrentUser().collectAsState(initial = null)
                
                // Update Check State
                val updateInfo = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<com.Linkbyte.Shift.data.model.AppUpdateInfo?>(null) }
                val showDialog = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

                val context = androidx.compose.ui.platform.LocalContext.current
                
                // Request Notification Permission (Android 13+)
                androidx.compose.runtime.LaunchedEffect(Unit) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        if (androidx.core.content.ContextCompat.checkSelfPermission(
                                context,
                                android.Manifest.permission.POST_NOTIFICATIONS
                            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                        ) {
                            androidx.core.app.ActivityCompat.requestPermissions(
                                this@MainActivity,
                                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                                101
                            )
                        }
                    }
                }

                // Start/Stop Notification Service based on auth state
                androidx.compose.runtime.LaunchedEffect(currentUser) {
                    if (currentUser != null) {
                        val intent = android.content.Intent(context, com.Linkbyte.Shift.service.NotificationService::class.java)
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            context.startForegroundService(intent)
                        } else {
                            context.startService(intent)
                        }
                    } else {
                        // Stop service on sign out
                        val intent = android.content.Intent(context, com.Linkbyte.Shift.service.NotificationService::class.java)
                        context.stopService(intent)
                    }
                }

                androidx.compose.runtime.LaunchedEffect(Unit) {
                    try {
                        val result = updateRepository.checkForUpdate()
                        result.onSuccess { info ->
                            info?.let {
                                updateInfo.value = it
                                showDialog.value = true
                            }
                        }.onFailure { e ->
                            android.widget.Toast.makeText(
                                context,
                                "Update Check Error: ${e.message}",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        android.widget.Toast.makeText(
                            context,
                            "Unexpected Error: ${e.message}",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                }

                if (showDialog.value && updateInfo.value != null) {
                    com.Linkbyte.Shift.presentation.common.UpdateDialog(
                        updateInfo = updateInfo.value!!,
                        onDismiss = { showDialog.value = false }
                    )
                }
                
                val startDestination = if (currentUser != null) {
                    Screen.ChatList.route
                } else {
                    Screen.Login.route
                }
                
                AppNavigation(
                    navController = navController,
                    startDestination = startDestination,
                    authRepository = authRepository,
                    signalingClient = signalingClient
                )
            }
        }
    }
}
