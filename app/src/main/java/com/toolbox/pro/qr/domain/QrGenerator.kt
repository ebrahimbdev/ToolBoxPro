package com.toolbox.pro.qr.domain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
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
        logoSizeFraction: Float = 0.2f,
        primaryColor: Long = 0xFF000000,
        secondaryColor: Long = 0xFF0000FF,
        useGradient: Boolean = false
    ): Bitmap {
        if (content.isBlank()) {
            return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        }

        val hints = HashMap<EncodeHintType, Any>().apply {
            put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H)
            put(EncodeHintType.MARGIN, 1)
            put(EncodeHintType.CHARACTER_SET, "UTF-8")
        }

        val qrCodeWriter = QRCodeWriter()
        val bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, size, size, hints)
        val matrixWidth = bitMatrix.width
        val matrixHeight = bitMatrix.height

        val bitmap = Bitmap.createBitmap(matrixWidth, matrixHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        if (useGradient) {
            paint.shader = LinearGradient(
                0f, 0f, matrixWidth.toFloat(), matrixHeight.toFloat(),
                primaryColor.toInt(), secondaryColor.toInt(),
                Shader.TileMode.CLAMP
            )
        } else {
            paint.color = primaryColor.toInt()
        }

        for (x in 0 until matrixWidth) {
            for (y in 0 until matrixHeight) {
                if (bitMatrix.get(x, y)) {
                    canvas.drawRect(
                        x.toFloat(),
                        y.toFloat(),
                        (x + 1).toFloat(),
                        (y + 1).toFloat(),
                        paint
                    )
                }
            }
        }

        return if (logoBitmap != null) {
            addLogoToQr(bitmap, logoBitmap, logoSizeFraction)
        } else {
            bitmap
        }
    }

    private fun addLogoToQr(
        qrBitmap: Bitmap,
        logoBitmap: Bitmap,
        sizeFraction: Float
    ): Bitmap {
        val combined = Bitmap.createBitmap(
            qrBitmap.width,
            qrBitmap.height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(combined)
        canvas.drawBitmap(qrBitmap, 0f, 0f, null)

        val logoSize = (qrBitmap.width * sizeFraction).toInt()
        val logo = Bitmap.createScaledBitmap(logoBitmap, logoSize, logoSize, true)
        val x = (qrBitmap.width - logoSize) / 2f
        val y = (qrBitmap.height - logoSize) / 2f

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = android.graphics.Color.WHITE
        canvas.drawRoundRect(
            android.graphics.RectF(x - 8, y - 8, x + logoSize + 8, y + logoSize + 8),
            16f, 16f, paint
        )
        canvas.drawBitmap(logo, x, y, null)

        return combined
    }
}
