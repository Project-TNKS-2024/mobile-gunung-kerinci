package com.dicoding.gunungkerinci.Homepage.Sop


sealed class SopItem {
    object  Header : SopItem()
    data class Title(val text: String) :SopItem()
    data class Subtitle(val text: String) :SopItem()
    data class Text(val text: String) :SopItem()
}