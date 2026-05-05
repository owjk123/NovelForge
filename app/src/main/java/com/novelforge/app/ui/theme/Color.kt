package com.novelforge.app.ui.theme

import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

val Primary = Color(0xFF6200EE)
val PrimaryVariant = Color(0xFF3700B3)
val Secondary = Color(0xFF03DAC6)
val Background = Color(0xFFFFFBFE)
val Surface = Color(0xFFFFFBFE)
val OnPrimary = Color.White
val OnSecondary = Color.Black
val OnBackground = Color(0xFF1C1B1F)
val OnSurface = Color(0xFF1C1B1F)

// Novel genre solid colors
val NovelFantasy = Color(0xFF9C27B0)
val NovelSciFi = Color(0xFF2196F3)
val NovelUrban = Color(0xFF4CAF50)
val NovelHarem = Color(0xFFE91E63)
val NovelMystery = Color(0xFF607D8B)
val NovelCustom = Color(0xFFFF9800)

// Genre gradient colors - start colors
val FantasyGradientStart = Color(0xFF9C27B0)  // 玄幻 - 紫色
val FantasyGradientEnd = Color(0xFFE040FB)

val SciFiGradientStart = Color(0xFF1565C0)    // 科幻 - 蓝色
val SciFiGradientEnd = Color(0xFF42A5F5)

val UrbanGradientStart = Color(0xFF2E7D32)    // 都市 - 绿色
val UrbanGradientEnd = Color(0xFF66BB6A)

val HaremGradientStart = Color(0xFFC2185B)    // 后宫 - 粉色
val HaremGradientEnd = Color(0xFFF06292)

val MysteryGradientStart = Color(0xFF37474F)  // 悬疑 - 灰蓝
val MysteryGradientEnd = Color(0xFF78909C)

val CustomGradientStart = Color(0xFFE65100)   // 自定义 - 橙色
val CustomGradientEnd = Color(0xFFFF9800)

/**
 * Get gradient colors for a novel genre
 * @param genre The genre string (FANTASY, SCIFI, URBAN, HAREM, MYSTERY, CUSTOM:xxx)
 * @return Pair of (startColor, endColor)
 */
fun getGenreGradient(genre: String): Pair<Color, Color> {
    return when {
        genre.startsWith("CUSTOM:") -> CustomGradientStart to CustomGradientEnd
        else -> {
            try {
                when (genre.uppercase()) {
                    "FANTASY" -> FantasyGradientStart to FantasyGradientEnd
                    "SCIFI" -> SciFiGradientStart to SciFiGradientEnd
                    "URBAN" -> UrbanGradientStart to UrbanGradientEnd
                    "HAREM" -> HaremGradientStart to HaremGradientEnd
                    "MYSTERY" -> MysteryGradientStart to MysteryGradientEnd
                    else -> CustomGradientStart to CustomGradientEnd
                }
            } catch (e: Exception) {
                CustomGradientStart to CustomGradientEnd
            }
        }
    }
}

/**
 * Get solid color for a novel genre
 */
fun getGenreColor(genre: String): Color {
    return when {
        genre.startsWith("CUSTOM:") -> NovelCustom
        else -> {
            try {
                when (genre.uppercase()) {
                    "FANTASY" -> NovelFantasy
                    "SCIFI" -> NovelSciFi
                    "URBAN" -> NovelUrban
                    "HAREM" -> NovelHarem
                    "MYSTERY" -> NovelMystery
                    else -> NovelCustom
                }
            } catch (e: Exception) {
                NovelCustom
            }
        }
    }
}
