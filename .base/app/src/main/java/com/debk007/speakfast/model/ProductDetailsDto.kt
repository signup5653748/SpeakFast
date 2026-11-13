package com.debk007.speakfast.model

import kotlinx.serialization.Serializable

@Serializable
data class ProductDetailsDto(
    val id: Int = 0,
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val price: Double = 0.0,
    val discountPercentage: Double = 0.0,
    val rating: Double = 0.0,
    val stock: Int = 0,
    val tags: List<String> = emptyList(),
    val brand: String = "",
    val sku: String = "",
    val weight: Int = 0,
    val dimensions: Dimensions = Dimensions(),
    val warrantyInformation: String = "",
    val shippingInformation: String = "",
    val availabilityStatus: String = "",
    val reviews: List<Review> = emptyList(),
    val returnPolicy: ReturnPolicy = ReturnPolicy(),
    val minimumOrderQuantity: Int = 0,
    val images: List<String> = emptyList(),
    val thumbnail: String = ""
)

@Serializable
data class Dimensions(
    val width: Double = 0.0,
    val height: Double = 0.0,
    val depth: Double = 0.0
)

@Serializable
data class Review(
    val rating: Int = 0,
    val comment: String = "",
    val date: String = "",
    val reviewerName: String = "",
    val reviewerEmail: String = ""
)

@Serializable
data class ReturnPolicy(
    val returnsAccepted: Boolean = false,
    val returnPeriod: String = "",
    val restockingFee: Double = 0.0
)