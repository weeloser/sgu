package com.example.sgu

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.sgu.ui.SguScreen
import com.example.sgu.ui.theme.SguTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Keep screen on while emergency console is in foreground
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            SguTheme {
                SguScreen()
            }
        }
    }
}
