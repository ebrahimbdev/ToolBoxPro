package com.toolbox.pro.qr.domain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QrGenerator @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun generateQrBitmap(
        content: String,
        size: Int = 512,
        logoBitmap: Bitmap? = null,
        style: QrStyle = QrStyle()
    ): Bitmap {
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        if (content.isBlank()) return output

        val qr = renderModules(content, size, logoBitmap, style)
        return composeFrame(qr, size, style)
    }

    private fun renderModules(
        content: String,
        size: Int,
        logoBitmap: Bitmap?,
        style: QrStyle
    ): Bitmap {
        // A logo in the middle covers modules -> use level H. Corner logos and
        // logo-less codes keep level M so longer payloads (vCards) still fit.
        val correction = if (logoBitmap != null && style.logoPosition == LogoPosition.CENTER) {
            ErrorCorrectionLevel.H
        } else {
            ErrorCorrectionLevel.M
        }

        val hints = HashMap<EncodeHintType, Any>().apply {
            put(EncodeHintType.ERROR_CORRECTION, correction)
            put(EncodeHintType.MARGIN, 1)
            put(EncodeHintType.CHARACTER_SET, "UTF-8")
        }

        val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)

        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(style.backgroundColor.toInt())

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        if (style.useGradient) {
            paint.shader = LinearGradient(
                0f, 0f, size.toFloat(), size.toFloat(),
                style.primaryColor.toInt(), style.secondaryColor.toInt(),
                Shader.TileMode.CLAMP
            )
        } else {
            paint.color = style.primaryColor.toInt()
        }

        val columns = bitMatrix.width
        val cell = size.toFloat() / columns
        val radius = when (style.dotStyle) {
            DotStyle.SQUARE -> 0f
            DotStyle.ROUNDED -> cell * 0.35f
        }

        for (x in 0 until columns) {
            for (y in 0 until bitMatrix.height) {
                if (!bitMatrix.get(x, y)) continue
                val left = x * cell
                val top = y * cell
                if (radius <= 0f) {
                    canvas.drawRect(left, top, left + cell, top + cell, paint)
                } else {
                    canvas.drawRoundRect(
                        RectF(left, top, left + cell, top + cell),
                        radius, radius, paint
                    )
                }
            }
        }

        val positioned = logoBitmap?.let { logo ->
            drawLogo(bitmap, logo, size, cell, style)
        } ?: bitmap

        if (positioned !== bitmap) bitmap.recycle()

        // The quiet zone lives on the module bitmap; the frame is painted around it.
        return positioned
    }

    private fun drawLogo(
        qrBitmap: Bitmap,
        logoSource: Bitmap,
        size: Int,
        cell: Float,
        style: QrStyle
    ): Bitmap {
        if (style.logoPosition == LogoPosition.NONE) return qrBitmap

        val combined = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(combined)
        canvas.drawBitmap(qrBitmap, 0f, 0f, null)

        val box = (size * style.logoSizeFraction).toInt().coerceAtLeast(1)
        val inset = (cell * 1.2f)

        val x: Float
        val y: Float
        when (style.logoPosition) {
            LogoPosition.CENTER -> {
                x = (size - box) / 2f
                y = (size - box) / 2f
            }
            LogoPosition.TOP_LEFT -> {
                x = inset
                y = inset
            }
            LogoPosition.TOP_RIGHT -> {
                x = size - box - inset
                y = inset
            }
            LogoPosition.BOTTOM_LEFT -> {
                x = inset
                y = size - box - inset
            }
            LogoPosition.BOTTOM_RIGHT -> {
                x = size - box - inset
                y = size - box - inset
            }
            LogoPosition.NONE -> return qrBitmap
        }

        val pad = box * 0.08f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = android.graphics.Color.WHITE
        canvas.drawRoundRect(
            RectF(x - pad, y - pad, x + box + pad, y + box + pad),
            box * 0.18f, box * 0.18f, paint
        )

        // Fit the logo inside the box without distorting its aspect ratio.
        val scale = minOf(box.toFloat() / logoSource.width, box.toFloat() / logoSource.height)
        val targetW = logoSource.width * scale
        val targetH = logoSource.height * scale
        val dx = x + (box - targetW) / 2f
        val dy = y + (box - targetH) / 2f
        val scaled = Bitmap.createScaledBitmap(logoSource, targetW.toInt().coerceAtLeast(1), targetH.toInt().coerceAtLeast(1), true)
        canvas.drawBitmap(scaled, dx, dy, null)
        if (scaled != logoSource) scaled.recycle()

        return combined
    }

    private fun composeFrame(qr: Bitmap, size: Int, style: QrStyle): Bitmap {
        val density = context.resources.displayMetrics.density
        val borderPx = style.border.dp * density
        val cornerPx = style.cornerRadiusDp * density

        if (borderPx <= 0f) {
            // No frame: keep the rendered QR as-is (its own quiet zone is intact).
            return qr
        }

        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = style.backgroundColor.toInt() }
        canvas.drawRoundRect(RectF(0f, 0f, size.toFloat(), size.toFloat()), cornerPx, cornerPx, bg)

        val frame = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = style.borderColor.toInt()
            this.style = Paint.Style.STROKE
            strokeWidth = borderPx
        }
        val half = borderPx / 2f
        canvas.drawRoundRect(
            RectF(half, half, size - half, size - half),
            cornerPx, cornerPx, frame
        )

        // Clip inside the frame so rounded corners cut background, never modules.
        val inner = RectF(borderPx, borderPx, size - borderPx, size - borderPx)
        val save = canvas.save()
        canvas.clipRect(inner)
        val qrSrc = RectF(0f, 0f, qr.width.toFloat(), qr.height.toFloat())
        canvas.drawBitmap(qr, null, inner, null)
        canvas.restoreToCount(save)

        qr.recycle()
        return output
    }
}
