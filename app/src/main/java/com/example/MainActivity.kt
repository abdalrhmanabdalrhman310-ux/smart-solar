package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.smartsolar.app.SmartSolarApp
import com.smartsolar.app.ui.navigation.SmartSolarRoot
import com.smartsolar.app.ui.theme.SmartSolarTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val appContainer = (application as SmartSolarApp).container
        setContent {
            SmartSolarTheme {
                SmartSolarRoot(container = appContainer)
            }
        }
    }
}
