package com.untrunc.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.untrunc.android.ui.screen.HomeScreen
import com.untrunc.android.ui.theme.UntruncTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UntruncTheme {
                HomeScreen()
            }
        }
    }
}
