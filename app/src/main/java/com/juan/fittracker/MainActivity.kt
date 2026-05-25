package com.juan.fittracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.juan.fittracker.data.OnboardingState
import com.juan.fittracker.data.UserPrefs
import com.juan.fittracker.data.UserProfile
import com.juan.fittracker.ui.SplashScreen
import com.juan.fittracker.ui.main.MainNav
import com.juan.fittracker.ui.onboarding.OnboardingScreen
import com.juan.fittracker.ui.theme.AppThemeMode
import com.juan.fittracker.ui.theme.FitTrackerTheme
import com.juan.fittracker.ui.tour.TourScreen
import kotlinx.coroutines.launch

private sealed class Screen {
    data object Splash : Screen()
    data object Onboarding : Screen()
    data class Tour(val profile: UserProfile) : Screen()
    data class Main(val profile: UserProfile) : Screen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppRoot()
        }
    }
}

@Composable
private fun AppRoot() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val themeName by produceState<String?>(initialValue = null) {
        UserPrefs.observeThemeMode(context).collect { value = it }
    }
    val themeMode = remember(themeName) {
        runCatching { AppThemeMode.valueOf(themeName ?: "") }
            .getOrDefault(AppThemeMode.Dark)
    }

    FitTrackerTheme(themeMode = themeMode) {
        AppContent()
    }
}

@Composable
private fun AppContent() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val onboardingState by produceState<OnboardingState>(initialValue = OnboardingState.Loading) {
        UserPrefs.observeState(context).collect { value = it }
    }
    val tourSeen by produceState<Boolean?>(initialValue = null) {
        UserPrefs.observeTourSeen(context).collect { value = it }
    }
    var screen by remember { mutableStateOf<Screen>(Screen.Splash) }

    Crossfade(
        targetState = screen,
        animationSpec = tween(durationMillis = 600),
        label = "root",
    ) { current ->
        when (current) {
            Screen.Splash -> {
                SplashScreen(
                    isReady = onboardingState !is OnboardingState.Loading && tourSeen != null,
                    onFinished = {
                        screen = when (val s = onboardingState) {
                            is OnboardingState.Onboarded ->
                                if (tourSeen == true) Screen.Main(s.profile)
                                else Screen.Tour(s.profile)
                            else -> Screen.Onboarding
                        }
                    },
                )
            }
            Screen.Onboarding -> {
                OnboardingScreen(onFinished = { profile ->
                    scope.launch {
                        UserPrefs.save(context, profile)
                        screen = Screen.Tour(profile)
                    }
                })
            }
            is Screen.Tour -> TourScreen(
                profile = current.profile,
                onFinish = {
                    scope.launch {
                        UserPrefs.setTourSeen(context, true)
                        screen = Screen.Main(current.profile)
                    }
                },
            )
            is Screen.Main -> MainNav(
                profile = current.profile,
                onResetProfile = {
                    scope.launch {
                        UserPrefs.resetEverything(context)
                        screen = Screen.Onboarding
                    }
                },
            )
        }
    }
}
