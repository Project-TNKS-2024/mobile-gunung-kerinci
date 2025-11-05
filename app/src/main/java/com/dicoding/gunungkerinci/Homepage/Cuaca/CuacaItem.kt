package com.dicoding.gunungkerinci.Homepage.Cuaca

sealed class CuacaItem {
    data class Header(val temperature: String, val condition: String, val iconRes: Int) : CuacaItem()
    data class CardCuaca(val title: String, val forecast: String) : CuacaItem()
    data class CardPendaki(val jumlah: String) : CuacaItem()
}
