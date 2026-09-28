package com.blackboxbench.reproduction

import androidx.compose.ui.graphics.Color

/** Palette measured from the observed target. */
object Palette {
    val Background = Color(0xFF0E1018)
    val BoardBackdrop = Color(0xFF11131C)

    val CoverTop = Color(0xFF343B4F)
    val CoverBottom = Color(0xFF262C3C)
    val CoverEdge = Color(0xFF3E4660)

    val Revealed = Color(0xFF171B26)
    val Fogged = Color(0xFF4B3A72)

    val DisplayPanel = Color(0xFF180C0C)
    val DisplayEdge = Color(0xFF43181A)
    val DisplayDigit = Color(0xFFFF3B30)

    val FaceAmber = Color(0xFFF0C24B)
    val FaceInk = Color(0xFF2A1F08)

    val TabIdle = Color(0xFF1B2030)
    val TabSelected = Color(0xFF2AA79B)
    val TabTextIdle = Color(0xFF8C93A6)
    val TabTextSelected = Color(0xFF0B1D1A)

    val BannerLost = Color(0xFF3E1214)
    val BannerLostText = Color(0xFFFF5B4A)
    val BannerWon = Color(0xFF12341C)
    val BannerWonText = Color(0xFF5FD07A)

    val FlagPole = Color(0xFF0B0D14)
    val FlagCloth = Color(0xFFF26B1D)

    val MineBody = Color(0xFF101218)
    val MineSpike = Color(0xFF3A1A1A)
    val ExplodedTile = Color(0xFF8E1F16)

    val LicenseText = Color(0xFF6E7385)

    val Num1 = Color(0xFF4C8DF6)
    val Num2 = Color(0xFF4CAF50)
    val Num3 = Color(0xFFE24A3B)
    val Num4 = Color(0xFF7E6BE0)
    val Num5 = Color(0xFFE08A2B)
    val Num6 = Color(0xFF26A69A)
    val Num7 = Color(0xFFE0E4EC)
    val Num8 = Color(0xFF9AA3B2)

    fun numberColor(value: Int): Color = when (value) {
        1 -> Num1
        2 -> Num2
        3 -> Num3
        4 -> Num4
        5 -> Num5
        6 -> Num6
        7 -> Num7
        else -> Num8
    }
}
