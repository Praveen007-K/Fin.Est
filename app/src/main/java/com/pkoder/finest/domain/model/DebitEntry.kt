package com.pkoder.finest.domain.model  // use your appropriate package here

data class DebitEntry(
    val category: String,
    val paymentMethod: String,
    val bank: String,
    val amount: Double,
    val description: String? = null
)
