package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.ui.AppTab
import com.example.ui.MainScreen
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val initialTabString = intent.getStringExtra("EXTRA_NAV_TAB")
        val initialTab = when (initialTabString?.lowercase()) {
            "blocker" -> AppTab.BLOCKER
            "sleep" -> AppTab.SLEEP
            "stats" -> AppTab.STATS
            else -> AppTab.HABITS
        }

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackground
                ) {
                    MainScreen(initialTab = initialTab)
                }
            }
        }
    }
}

