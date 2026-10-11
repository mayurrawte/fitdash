package app.fitdash

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.fitdash.ui.theme.FitdashTheme
import app.fitdash.ui.theme.Palette

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FitdashTheme {
                Box(Modifier.fillMaxSize().background(Palette.Background), contentAlignment = Alignment.Center) {
                    Text("fitdash", color = Palette.Text)
                }
            }
        }
    }
}
