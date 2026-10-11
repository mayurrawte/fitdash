package app.fitdash.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.fitdash.ui.components.Orb
import app.fitdash.ui.theme.Palette
import app.fitdash.ui.theme.Type

@Composable
fun LoadingScreen() {
    Box(Modifier.fillMaxSize().background(Palette.Background), contentAlignment = Alignment.Center) { Orb(null) {} }
}

@Composable
fun MessageScreen(title: String, body: String, action: String? = null, onAction: () -> Unit = {}) {
    Column(
        Modifier.fillMaxSize().background(Palette.Background).padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Orb(null) {}
        Spacer(Modifier.height(32.dp))
        Text(title, style = Type.value, color = Palette.Text, textAlign = TextAlign.Center)
        Text(body, style = Type.body, color = Palette.Muted, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 10.dp))
        if (action != null) Button(onClick = onAction, modifier = Modifier.padding(top = 28.dp)) { Text(action) }
    }
}
