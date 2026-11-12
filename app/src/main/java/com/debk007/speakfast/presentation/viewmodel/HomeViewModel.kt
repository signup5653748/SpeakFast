package com.debk007.speakfast.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.debk007.speakfast.data.SettingsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val settingsManager: SettingsManager
) : ViewModel() {

    private val _productDetailsState: MutableStateFlow<ApiState<ProductDetailsDto>> =
        MutableStateFlow(ApiState.Loading)
    val productDetailsState: StateFlow<ApiState<ProductDetailsDto>> = _productDetailsState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val settings = settingsManager.getSettings()
            _productDetailsState.value = ApiState.Success(
                ProductDetailsDto(
                    id = 1,
                    title = "SpeakFast Settings",
                    description = "Language: ${settings.languageCode}, Speech Rate: ${settings.speechRate}",
                    price = 0.0,
                    discountPercentage = 0.0,
                    rating = 5.0,
                    stock = 100,
                    tags = listOf("accessibility", "speech"),
                    brand = "SpeakFast",
                    sku = "SF-001",
                    weight = 0.5,
                    dimensions = Dimensions(10.0, 5.0, 2.0),
                    warrantyInformation = "1 year",
                    shippingInformation = "Free shipping",
                    availabilityStatus = "In Stock",
                    reviews = listOf(),
                    returnPolicy = "30 days",
                    minimumOrderQuantity = 1,
                    images = listOf("https://example.com/image1.jpg")
                )
            )
        }
    }
}