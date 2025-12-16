package com.yourname.furnituresales.ui.util

import java.util.Locale

const val USD_TO_RUB = 90.0

fun formatPriceRub(usdAmount: Double): String {
    val rub = usdAmount * USD_TO_RUB
    return String.format(Locale("ru", "RU"), "%,.0f ₽", rub)
}
