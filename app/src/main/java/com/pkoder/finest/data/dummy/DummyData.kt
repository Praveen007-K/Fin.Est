package com.pkoder.finest.data.dummy

import com.pkoder.finest.domain.model.CreditEntry
import com.pkoder.finest.domain.model.DebitEntry

object DummyData {
    val dummyDebits = listOf(
        DebitEntry("Food", "Cash", "Chase", 200.0),
        DebitEntry("Housing", "Card", "Bank of America", 500.0),
        DebitEntry("Food", "Cash", "Chase", 100.0),
        DebitEntry("Transport", "Card", "Wells Fargo", 150.0),
    )


    val dummyCredits = listOf(
        CreditEntry("Salary", 3000.0),
        CreditEntry("Freelancing", 1500.0),
        CreditEntry("Salary", 1000.0)
    )
}

