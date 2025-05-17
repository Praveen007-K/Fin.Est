package com.example.finest.data.dummy

import com.example.finest.domain.model.CreditEntry
import com.example.finest.domain.model.DebitEntry

object DummyData {
    val dummyDebits = listOf(
        DebitEntry("Food", 200.0),
        DebitEntry("Housing", 500.0),
        DebitEntry("Food", 100.0),
        DebitEntry("Transport", 150.0),
    )

    val dummyCredits = listOf(
        CreditEntry("Salary", 3000.0),
        CreditEntry("Freelancing", 1500.0),
        CreditEntry("Salary", 1000.0)
    )
}

