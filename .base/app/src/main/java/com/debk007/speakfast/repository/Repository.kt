package com.debk007.speakfast.repository

import com.debk007.speakfast.model.ProductDetailsDto
import com.debk007.speakfast.util.ApiState

interface Repository {
    suspend fun getProductDetails(): ApiState<ProductDetailsDto>
}