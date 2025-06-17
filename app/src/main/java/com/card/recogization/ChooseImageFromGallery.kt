package com.card.recogization

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.text.Text
import com.googlecode.tesseract.android.TessBaseAPI
import java.io.File
import java.io.FileOutputStream


// Check for Android 13 permission before opening the image picker
//if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//    if (ContextCompat.checkSelfPermission(
//            this,
//            android.Manifest.permission.READ_MEDIA_IMAGES
//        ) == PackageManager.PERMISSION_GRANTED
//    ) {
//        btnPickImage.setOnClickListener {
//
//            getImageFromGallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
//        }
//    } else {
//        ActivityCompat.requestPermissions(
//            this,
//            arrayOf(android.Manifest.permission.READ_MEDIA_IMAGES),
//            100
//        )
//    }
//} else {
//    // For versions below Android 13, just use the default permission
//    btnPickImage.setOnClickListener {
//        getImageFromGallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
//    }
//}


fun checkAndRequestImagePermission(
    activity: Activity,
    btnPickImage: View,
    getImageFromGallery: ActivityResultLauncher<PickVisualMediaRequest>,
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (ContextCompat.checkSelfPermission(
                activity,
                android.Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            btnPickImage.setOnClickListener {
                getImageFromGallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
        } else {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(android.Manifest.permission.READ_MEDIA_IMAGES),
                100
            )
        }
    } else {
        btnPickImage.setOnClickListener {
            getImageFromGallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }
}


 fun extractCardDetails(result: Text, onReceivedNumber :(String) -> Unit) : CardType {
    val cardText = result.text
     onReceivedNumber(extractDatafromTextBlocks(result))

    val sanitizedCardText = cardText.replace(Regex("[^a-zA-Z0-9\\s]"), " ")
    val socialSecurityKeywordRegex = Regex("\\bSocial\\sSecurity\\b", RegexOption.IGNORE_CASE)


//        val driverLicenseKeywordRegex = Regex("\\b(driver[-\\s]?[l1i][-\\s]?[c1]ense|[bB][i1]cense|license)\\b", RegexOption.IGNORE_CASE)
    val driverLicenseKeywordRegex = Regex("\\b(d[ri1v][-\\s]?[l1i][-\\s]?[c1]ense|[bB][i1]cense|licenge|license|driving[-\\s]?[l1i][-\\s]?[c1]enge|driving[-\\s]?license)\\b", RegexOption.IGNORE_CASE)


//        val militaryPhraseRegex = Regex("\\bArmed\\sForce?\\s(of\\s(the\\s)?(United\\sStates|US|U\\.S\\.))\\b", RegexOption.IGNORE_CASE)
    val militaryPhraseRegex = Regex("\\b(Arno?d?[-\\s]?Forces?|A?r?med[-\\s]?Forces?|A?mod?[-\\s]?Forces?|A?med[-\\s]?For[e3]s?|Armed[-\\s]?Force?)\\b", RegexOption.IGNORE_CASE)

    val caidRegex = Regex("\\b[A-Z]{2}\\d{6}\\b") // Hypothetical: Two letters + 6 digits (e.g., CA123456)


    val birthCertificateRegex = Regex("\\b\\d{6,10}\\b") // Variable digits (customize based on issuing authority)

    val otherDocRegex = Regex(".+") // Catch-all for unstructured or unknown formats




    when {
        socialSecurityKeywordRegex.containsMatchIn(sanitizedCardText) -> {
            Log.e("TAG", "CardType: --->  SOCIALSECURITY")
            return CardType.SOCIALSECURITY
        }

        driverLicenseKeywordRegex.containsMatchIn(sanitizedCardText) -> {
            Log.e("TAG", "CardType: --->  DRIVERSLICENSCE")
            return CardType.DRIVERSLICENSCE
        }

        militaryPhraseRegex.containsMatchIn(sanitizedCardText) -> {
            Log.e("TAG", "CardType: --->  MILITARYID")
            return CardType.MILITARYID
        }

        birthCertificateRegex.containsMatchIn("BIRTHDCERTIFICATENUM") -> {
            Log.e("TAG", "CardType: --->  BIRTHDCERTIFICATENUM")
            return CardType.BIRTHDCERTIFICATENUM
        }

        else -> {
            Log.e("TAG", "CardType: --->  OTHER")
            return CardType.OTHER
        }
    }




}
fun Context.showAlertDialog(
    title: String,
    message: String,
    isCancelable: Boolean,
    positiveBtnText: String? = "",
    positiveBtnOnClickListener: (DialogInterface, Int) -> Unit = { _, _ -> },
    negativeBtnText: String = "",
    negativeBtnOnClickListener: (DialogInterface, Int) -> Unit = { _, _ -> },
    neutralBtnText: String = "",
    neutralBtnOnClickListener: (DialogInterface, Int) -> Unit = { _, _ -> },
) {
    val builder = AlertDialog.Builder(this, R.style.AlertDialogTheme)
    builder.setTitle(title)
        .setMessage(message)
        .setCancelable(isCancelable)
        .setPositiveButton(positiveBtnText) { dialog, which ->
            positiveBtnOnClickListener(dialog, which)
        }
        .setNegativeButton(negativeBtnText) { dialog, which ->
            negativeBtnOnClickListener(dialog, which)
        }
        .setNeutralButton(neutralBtnText) { dialog, which ->
            neutralBtnOnClickListener(dialog, which)
        }
    val alert = builder.create()
    alert.window!!.setBackgroundDrawableResource(R.drawable.round_dialog)
    alert.show()
}


 @SuppressLint("SuspiciousIndentation")
 fun extractDatafromTextBlocks(result: Text) :String {
    val resultText = result.text
    var cardName = resultText
    var cardExpiry = resultText
     var mLNumber = ""
     val realTextResult = StringBuilder()
    for (block in result.textBlocks) {
        val blockText = block.text
        val blockCornerPoints = block.cornerPoints
        val blockFrame = block.boundingBox
        val dlPattern = Regex("\\b(ID:\\s?\\d{3}\\s\\d{3}\\s\\d{3}|DLN\\s?[A-Z]?\\d{8,9}|DL\\s?[A-Z]\\d{7,8})\\b", RegexOption.IGNORE_CASE)

        val dlMatch = dlPattern.find(blockText)
        if (dlMatch != null) {
            mLNumber  = dlMatch.value // Return the license number if found
            Log.e("TAG", "Found DL License Number: --->  ${mLNumber}")
        }
        Log.e("TAG", "blockText: --->  $blockText")
        for (line in block.lines) {
            val lineText = line.text
            val lineCornerPoints = line.cornerPoints
            val lineFrame = line.boundingBox
            Log.e("TAG", "lineText: --->  $lineText")
            for (element in line.elements) {
                realTextResult.append(element.text + " ")
                val elementText = element.text
                val elementCornerPoints = element.cornerPoints
                val elementFrame = element.boundingBox
                Log.e("TAG", "elementText: --->  $elementText")
                if (elementText.contains("debit", true) || elementText.contains(
                        "credit",
                        true
                    ) || elementText.contains(
                        "visa",
                        true
                    ) || elementText.contains("mastercard", true) || elementText.contains(
                        "pan",
                        true
                    )
                ) {
                  cardName= elementText
                    Log.e("TAG", "cardName elementText: --->  $elementText")
                } else if (elementText.contains(
                        "/",
                        true
                    )
                ) {
                  cardExpiry = elementText
                    Log.e("TAG", "cardExpiry elementText: --->  $elementText")
                }

            }
        }
    }
    Log.e("TAG", "extractCardDetails: --->  $resultText")
     return mLNumber
}



//
//fun extractTextUsingTesseract(bitmap: Bitmap): String {
//    val processedBitmap = convertBitmapToARGB8888(bitmap)
//    try {
//        val tessBaseAPI = TessBaseAPI()
//        val dataPath = filesDir.absolutePath + "/tesseract/"
//        val trainedDataFile = File(dataPath + "tessdata/", "eng.traineddata")
//        Log.e("Tesseract", trainedDataFile.absolutePath)
//        if (!trainedDataFile.exists()) {
//            Log.e("Tesseract", "Trained data not found at $dataPath")
//            return "Error: Missing trained model."
//        }
//        tessBaseAPI.init(dataPath, "eng") // Path to trained data
//        tessBaseAPI.setImage(processedBitmap)
//        val extractedText = tessBaseAPI.utF8Text
//        tessBaseAPI.recycle()
//        Log.e("Tesseract", "extractTextUsingTesseract: -------> $extractedText")
//        return extractedText
//    } catch (e: Exception) {
//        Log.e("Tesseract", "extractTextUsingTesseract: -> ${e.message}")
//    }
//
//    return ""
//}
//
//fun copyTesseractData(context: Context) {
//    val assetManager = context.assets
//    val inputStream = assetManager.open("eng.traineddata")
//    val tessDir = File(context.filesDir, "tesseract/tessdata")
//
//    if (!tessDir.exists()) tessDir.mkdirs()
//    Log.e("FilePath", "Tesseract Directory: ${context.filesDir.absolutePath}/tesseract")
//
//    val outputFile = File(tessDir, "eng.traineddata")
//    val outputStream = FileOutputStream(outputFile)
//
//    inputStream.copyTo(outputStream)
//    inputStream.close()
//    outputStream.close()
//}