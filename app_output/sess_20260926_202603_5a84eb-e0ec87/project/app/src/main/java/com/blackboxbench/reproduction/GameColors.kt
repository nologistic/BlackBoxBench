package com.blackboxbench.reproduction

import androidx.compose.ui.graphics.Color

/** Visual palette of the reproduced game. */
object GameColors {
    val background = Color(0xFF0B1521)

    val tabIdle = Color(0xFF1B2B3B)
    val tabSelected = Color(0xFF4ECDC4)
    val tabTitleIdle = Color(0xFFC9D3DD)
    val tabTitleSelected = Color(0xFF16202B)
    val tabSubIdle = Color(0xFF8494A4)
    val tabSubSelected = Color(0xFF1E5550)

    val cellHidden = Color(0xFF26323F)
    val cellHiddenEdge = Color(0xFF33414F)
    val cellRevealed = Color(0xFF1B2B3B)
    val cellMine = Color(0xFF3A0A0A)
    val cellExploded = Color(0xFFE23A0B)
    val cellFog = Color(0xFF2B2540)

    val numberColors = listOf(
        Color(0xFF4E9DF5), // 1
        Color(0xFF3FC97A), // 2
        Color(0xFFF0524F), // 3
        Color(0xFFB07CF0), // 4
        Color(0xFFF0A64E), // 5
        Color(0xFF4ED0C8), // 6
        Color(0xFFE0E6EC), // 7
        Color(0xFF9AA7B4)  // 8
    )

    val faceButton = Color(0xFF1B2B3B)

    val segmentPanel = Color(0xFF180A10)
    val segmentOn = Color(0xFFFF2A2A)
    val segmentOff = Color(0xFF2C1014)

    val winBanner = Color(0xFF0E3A1F)
    val winText = Color(0xFF7FE0A0)
    val loseBanner = Color(0xFF3C0908)
    val loseText = Color(0xFFFF6B5A)

    val licenseText = Color(0xFF7E8C9B)
    val dialogBackground = Color(0xFF1B2B3B)
    val dialogText = Color(0xFFE6EDF3)
    val accent = Color(0xFF4ECDC4)
    val switchTrackOff = Color(0xFF2A3442)
    val switchThumbOff = Color(0xFF97A6B5)
    val sliderInactive = Color(0xFF16202B)
}
