package com.debk007.speakfast.presentation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.debk007.speakfast.presentation.ui.SettingsScreen
import com.debk007.speakfast.presentation.viewmodel.SettingsViewModel

@Composable
fun App() {
    val viewModel = hiltViewModel<SettingsViewModel>()
    SettingsScreen(viewModel = viewModel)
}