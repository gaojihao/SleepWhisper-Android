package com.lizhi1026.sleepwhisper.features.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lizhi1026.sleepwhisper.app.AppStateContainer
import com.lizhi1026.sleepwhisper.core.visualkit.LocalSWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWColor
import com.lizhi1026.sleepwhisper.core.visualkit.SWFont
import com.lizhi1026.sleepwhisper.core.visualkit.SWSpacing
import com.lizhi1026.sleepwhisper.core.visualkit.components.BreathingBackground
import com.lizhi1026.sleepwhisper.core.visualkit.components.SoftButton
import kotlinx.coroutines.delay

@Composable
fun WelcomeRitualScreen(app: AppStateContainer, vm: WelcomeRitualViewModel = hiltViewModel()) {
    val scheme = LocalSWScheme.current
    val baby by app.baby.observeAsState(null)

    LaunchedEffect(Unit) {
        delay(2600)
        app.dismissWelcome()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        BreathingBackground()
        Column(
            modifier = Modifier.fillMaxSize().padding(SWSpacing.xl),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BasicText(
                text = "Welcome",
                style = SWFont.titleXL().copy(color = SWColor.textPrimary(scheme))
            )
            BasicText(
                text = baby?.name ?: "",
                style = SWFont.displayLG().copy(color = SWColor.primary(scheme))
            )
            BasicText(
                text = "Sleep well, little one.",
                style = SWFont.bodyLG().copy(color = SWColor.textSecondary(scheme)),
                modifier = Modifier.padding(top = SWSpacing.lg)
            )
            SoftButton(
                text = "Continue",
                onClick = { app.dismissWelcome() },
                modifier = Modifier.padding(top = SWSpacing.xxl)
            )
        }
    }
}
