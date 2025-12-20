package com.phamnhantucode.aicareercoach.data.neon

import java.math.BigDecimal

data class CreditPack(
    val id: String,
    val name: String,
    val credits: Int,
    val price: BigDecimal,
    val platform: String,
    val googlePlaySku: String?,
    val bonusCredits: Int
)
