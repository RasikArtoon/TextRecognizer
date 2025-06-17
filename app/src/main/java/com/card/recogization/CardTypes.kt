package com.card.recogization

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import androidx.core.graphics.createBitmap

enum class CardType {
    SOCIALSECURITY,
    DRIVERSLICENSCE,
    CAID,
    MILITARYID,
    BIRTHDCERTIFICATENUM,
    OTHER,
}
fun Bitmap.toGrayscale(): Bitmap {
    // Ensure the bitmap is mutable and use ARGB_8888 to avoid hardware-backed issues
    val mutableBitmap = this.copy(Bitmap.Config.ARGB_8888, true)  // Ensure mutable bitmap

    val width = mutableBitmap.width
    val height = mutableBitmap.height

    // Create a new bitmap with the same dimensions (mutable)
    val bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

    // Create a canvas and paint to apply the grayscale filter
    val canvas = Canvas(bmpGrayscale)
    val paint = Paint()
    val colorMatrix = ColorMatrix()

    // Set the color matrix for grayscale (0 saturation)
    colorMatrix.setSaturation(0f) // 0 means grayscale
    val filter = ColorMatrixColorFilter(colorMatrix)
    paint.colorFilter = filter

    // Draw the original bitmap onto the new mutable grayscale bitmap
    canvas.drawBitmap(mutableBitmap, 0f, 0f, paint)

    // Return the new grayscale bitmap
    return bmpGrayscale
}

