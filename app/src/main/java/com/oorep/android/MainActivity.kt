package com.oorep.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oorep.android.ui.navigation.OorepNavHost
import com.oorep.android.ui.theme.OorepTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            enableEdgeToEdge()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        super.onCreate(savedInstanceState)

        setContent {
            OorepTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val app = OorepApplication.instance
                    val isReady by app.databaseHelper.isReady.collectAsState()
                    val progress by app.databaseHelper.initProgress.collectAsState()
                    val statusMessage by app.databaseHelper.statusMessage.collectAsState()

                    LaunchedEffect(Unit) {
                        app.databaseHelper.initializeDatabase()
                    }

                    if (isReady) {
                        OorepNavHost()
                    } else {
                        SplashScreen(progress = progress, message = statusMessage)
                    }
                }
            }
        }
    }
}

@Composable
fun SplashScreen(progress: Float, message: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
        ) {
            Text(text = "🌿", fontSize = 40.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "OOREP",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Open Online Repertory for Homeopathy",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(36.dp))

        if (progress > 0f) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "${(progress * 100).toInt()}%",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            CircularProgressIndicator(modifier = Modifier.size(36.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Loading 146,000+ rubrics for offline use (first launch only)",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center
        )
    }
}
