package com.toolbox.pro.qr.domain

enum class DotStyle { SQUARE, ROUNDED }

enum class LogoPosition { NONE, CENTER, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

enum class BorderWidth(val dp: Int) {
    NONE(0),
    THIN(6),
    MEDIUM(12),
    THICK(20)
}

data class QrStyle(
    val primaryColor: Long = 0xFF000000,
    val secondaryColor: Long = 0xFF6C63FF,
    val useGradient: Boolean = false,
    val dotStyle: DotStyle = DotStyle.SQUARE,
    val border: BorderWidth = BorderWidth.NONE,
    val borderColor: Long = 0xFF6C63FF,
    val cornerRadiusDp: Float = 0f,
    val backgroundColor: Long = 0xFFFFFFFF,
    val logoPosition: LogoPosition = LogoPosition.CENTER,
    val logoSizeFraction: Float = 0.20f
)
