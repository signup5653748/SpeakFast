package com.debk007.speakfast.presentation

import android.os.Bundle
import androidx.activity.compose.setContentView
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.setContent
import com.debk007.speakfast.presentation.ui.theme.SpeakFastTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SpeakFastTheme {
                // The app content will be set by the accessibility service
                // This activity is mainly for settings and as the accessibility service's settings activity
            }
        }
    }
}