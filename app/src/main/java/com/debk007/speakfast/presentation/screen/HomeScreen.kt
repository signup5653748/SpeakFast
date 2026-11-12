package com.debk007.speakfast.presentation.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.debk007.speakfast.model.ProductDetailsDto
import com.debk007.speakfast.util.ApiState

@Composable
fun HomeScreen(productDetails: ApiState<ProductDetailsDto>) {
    when (productDetails) {
        is ApiState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Loading...",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        is ApiState.Error -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = productDetails.errorMsg,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        is ApiState.Success -> {
            val data = productDetails.data

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 24.dp)
            ) {
                Text(
                    text = "Product Details",
                    style = MaterialTheme.typography.headlineLarge
                )

                Text(
                    text = "Brand:\n${data.brand}",
                    modifier = Modifier.padding(top = 16.dp)
                )

                Text(
                    text = "Description:\n${data.description}",
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}