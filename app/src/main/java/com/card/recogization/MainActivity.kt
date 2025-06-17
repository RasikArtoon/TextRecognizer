package com.card.recogization

import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.graphics.Point
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.createBitmap
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import com.card.recogization.databinding.ActivityMainBinding
import com.google.android.material.button.MaterialButton
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.text.FirebaseVisionCloudTextRecognizerOptions
import com.google.firebase.ml.vision.text.RecognizedLanguage
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import java.util.regex.Pattern

class MainActivity : AppCompatActivity() {
    private val getImageFromGallery =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
            uri?.let { displayImage(it) }
        }
    lateinit var biding: ActivityMainBinding

    //    private var ocrEngine: OcrEngine? = null
    private var sb: java.lang.StringBuilder? = null
    private lateinit var imageView: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        biding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(biding.root)

//        try {
//            Platform.setLibPath(sharedLibsPath)
//            Platform.loadLibrary(LTLibrary.LEADTOOLS)
//            Platform.loadLibrary(LTLibrary.CODECS)
//            Platform.loadLibrary(LTLibrary.OCR)
//        } catch (ex: java.lang.Exception) {
//            Log.d("TAG", "Failed to load LEADTOOLS Native libraries")
//        }
//
//        sb = java.lang.StringBuilder()
//
//        var demoBaseDir = filesDir.toString()
//        if (!demoBaseDir.endsWith("/")) demoBaseDir += "/"
//
//        val ocrLanguageFileDir = "$demoBaseDir/OCRRuntime/"
//        val substitutionFontsDir = "$demoBaseDir/SubstitutionFonts/"
//
//        if (!Utils.copyOcrRuntimeFiles(this, ocrLanguageFileDir)) {
//            Log.d("TAG", "Failed to copy OCR Language Files")
//            finish()
//        }
//        if (!Utils.copyAssetsFiles(this, "substitution_fonts", substitutionFontsDir)) {
//            Log.d("TAG", "Failed to copy Substitution Fonts")
//            finish()
//        }
//        try {
//            //Set Substitution Fonts path and startup the OCR Engine
//            RasterDefaults.setResourceDirectory(LEADResourceDirectory.FONTS, substitutionFontsDir)
//            ocrEngine = OcrEngineManager.createEngine(OcrEngineType.LEAD)
//            ocrEngine.startup(null, null, null, ocrLanguageFileDir)
//        } catch (e: java.lang.Exception) {
//            Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
//        }
//
//        if (!ocrEngine.isStarted()) {
//            Toast.makeText(this, "OCR Engine was not started successfully", Toast.LENGTH_LONG)
//                .show()
//        }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        imageView = findViewById(R.id.selectedImageView)

        val btnPickImage: MaterialButton = findViewById(R.id.btnPickImage)
        if (OpenCVLoader.initDebug()) {
            Log.i("OpenCV", "OpenCV loaded successfully")
        } else {
            Log.e("OpenCV", "OpenCV initialization failed!")
        }


        checkAndRequestImagePermission(this, biding.btnPickImage, getImageFromGallery)

    }


    // Function to display the selected image in the ImageView
    private fun displayImage(uri: Uri) {
        try {
            val bitmap: Bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // Use ImageDecoder on API 28 (Android 9) and above
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, uri))
            } else {
                // Fallback for older devices, using BitmapFactory
                MediaStore.Images.Media.getBitmap(contentResolver, uri)
            }
            imageView.setImageBitmap(bitmap)
            processImageWithOpenCVAndMLKit(bitmap)
            val grayScledBitMap = bitmap.toGrayscale()
//            processImage(bitmap)
//            processImage(bitmap.scale(1500, 1500))
//            processWithFirebaseVision(bitmap)
        } catch (e: Exception) {
            Log.e("TAG", "displayImage: -> ${e.message}")
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun processImageWithOpenCVAndMLKit(originalBitmap: Bitmap) {
        biding.progressBar.isVisible=true
        biding.progressCircular.isVisible=true

        val mutableBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)

        // 1. Convert Bitmap to OpenCV Mat
        val originalMat = Mat()
        Utils.bitmapToMat(mutableBitmap, originalMat)


        // 2. OpenCV Preprocessing
        val grayMat = Mat()
        Imgproc.cvtColor(originalMat, grayMat, Imgproc.COLOR_BGR2GRAY)

        // Enhance Contrast
        val contrastMat = Mat()
        Core.normalize(grayMat, contrastMat, 0.0, 255.0, Core.NORM_MINMAX)

        // Optional: Gaussian Blur to reduce noise before thresholding
        val blurredMat = Mat()
//        Imgproc.GaussianBlur(grayMat, blurredMat, Size(5.0, 5.0), 0.0) // Kernel size 5x5, sigma 0
        Imgproc.bilateralFilter(contrastMat, blurredMat, 9, 75.0, 75.0) // Kernel size 5x5, sigma 0

        val thresholdMat = Mat()
        // Adaptive thresholding can be very effective for varying lighting
//        Imgproc.adaptiveThreshold(
//            blurredMat, // Or use grayMat directly if blur is not desired
//            thresholdMat,
//            255.0, // Max value to assign
//            Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, // Adaptive method
//            Imgproc.THRESH_BINARY, // Threshold type (INV for black text on white bg for some OCRs)
//            // Or THRESH_BINARY if your text is light on dark after this
//            11, // Block size (must be odd)
//            2.0  // C (constant subtracted from the mean or weighted mean)
//        )
        Imgproc.threshold(grayMat, thresholdMat, 0.0, 255.0, Imgproc.THRESH_OTSU)

        // Experiment with block size and C value. Common block sizes: 11, 15, 21. Common C: 2, 5, 10.

        // 3. Convert Processed Mat back to Bitmap
        val processedBitmap = createBitmap(thresholdMat.width(), thresholdMat.height())
        Utils.matToBitmap(thresholdMat, processedBitmap)


        // 4. Feed processed Bitmap to ML Kit
        val inputImage = InputImage.fromBitmap(processedBitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        recognizer.process(inputImage)
            .addOnSuccessListener { visionText ->

                val resultText = visionText.text
                if (resultText.isNotBlank()) {
                    Log.d("TAG", "Text recognized: $resultText")
                    val cardType = extractCardDetails(visionText){
                        biding.cardIdTextViewName.text = it
                    } // Call your parsing logic
                    showAlertDialog(
                        "Text Recognized",
                        resultText,
                        false,
                        "Copy", { _, _ ->
                            run {
                                biding.progressBar.isVisible=false
                                biding.progressCircular.isVisible=false
                                copyText(resultText)
                            }
                        },
                        "Cancel", { _, _ ->
                            biding.progressBar.isVisible=false
                            biding.progressCircular.isVisible=false
                        },
                        "Close Dialog", { dialog, _ ->
                            run {
                                biding.progressBar.isVisible=false
                                biding.progressCircular.isVisible=false
                                dialog.cancel()
                            }
                        }
                    )
                            biding.CardTypeTextViewName.text = cardType.toString()

                } else {
                    Log.d("TAG", "No text found after OpenCV processing.")
                }

            }
            .addOnFailureListener { e ->
                Log.e("TAG", "ML Kit Text recognition failed")
            }

        // Release Mats to free memory
        originalMat.release()
        grayMat.release()
        blurredMat.release()
        thresholdMat.release()
        // Do not recycle originalBitmap or processedBitmap here if they are being displayed
        // or if originalBitmap is needed elsewhere.
    }
    private fun copyText(text: String) {
        if (!TextUtils.isEmpty(text)) {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Recognized Text", text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Copied to Clipboard!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun processWithFirebaseVision(bitmap: Bitmap) {
        val image: FirebaseVisionImage = FirebaseVisionImage.fromBitmap(bitmap)
        val options: FirebaseVisionCloudTextRecognizerOptions =
            FirebaseVisionCloudTextRecognizerOptions.Builder()
                .setLanguageHints(listOf("en", "hi"))
                .build()
        val textRecognizer = FirebaseVision.getInstance()
            .getCloudTextRecognizer(options)

        textRecognizer.processImage(image)
            .addOnSuccessListener { result ->
                val resultText: String = result.text
                Log.e("TAG", "Firebae processImage Result : -> ${resultText}")
                for (block in result.textBlocks) {
                    val blockText: String = block.text
                    val blockConfidence: Float = block.confidence!!
                    val blockLanguages: List<RecognizedLanguage> =
                        block.recognizedLanguages
                    val blockCornerPoints: Array<Point> = block.cornerPoints!!
                    val blockFrame: Rect = block.boundingBox!!
                    for (line in block.lines) {
                        val lineText: String = line.getText()
                        val lineConfidence: Float = line.getConfidence()!!
                        val lineLanguages: List<RecognizedLanguage> =
                            line.getRecognizedLanguages()
                        val lineCornerPoints: Array<Point> = line.getCornerPoints()!!
                        val lineFrame: Rect = line.getBoundingBox()!!
                        for (element in line.getElements()) {
                            val elementText: String = element.getText()
                            val elementConfidence: Float = element.getConfidence()!!
                            val elementLanguages: List<RecognizedLanguage> =
                                element.getRecognizedLanguages()
                            val elementCornerPoints: Array<Point> = element.getCornerPoints()!!
                            val elementFrame: Rect = element.getBoundingBox()!!
                        }
                    }
                }
                // ...
            }
            .addOnFailureListener {
                Log.e("TAG", "Fireabse ML Exceptions: -> ${it.message}")
            }
    }

    fun preprocessImage(bitmap: Bitmap): Bitmap {
        // Convert to grayscale
        val grayscaleBitmap = convertToGrayscale(bitmap)

        // Apply thresholding (binary conversion)
        val thresholdBitmap = applyThreshold(grayscaleBitmap)

        // Optionally apply image sharpening (you can use a filter here)
        val sharpenedBitmap = sharpenImage(thresholdBitmap)

        // Resize the image to an optimal resolution
        val resizedBitmap = resizeImage(sharpenedBitmap, 800, 600)

        return resizedBitmap
    }

    fun convertToGrayscale(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val grayscaleBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        for (x in 0 until width) {
            for (y in 0 until height) {
                val pixel = bitmap.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                val gray = (r + g + b) / 3
                grayscaleBitmap.setPixel(x, y, Color.rgb(gray, gray, gray))
            }
        }
        return grayscaleBitmap
    }


    fun applyThreshold(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val thresholdBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val thresholdValue = 128  // Adjust based on your image brightness

        for (x in 0 until width) {
            for (y in 0 until height) {
                val pixel = bitmap.getPixel(x, y)
                val gray = (Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3
                val newColor = if (gray < thresholdValue) Color.BLACK else Color.WHITE
                thresholdBitmap.setPixel(x, y, newColor)
            }
        }
        return thresholdBitmap
    }

    fun sharpenImage(bitmap: Bitmap): Bitmap {
        val matrix = ColorMatrix()
        matrix.setSaturation(2.0f) // Adjust sharpening factor
        val paint = Paint()
        paint.colorFilter = ColorMatrixColorFilter(matrix)

        val canvas = Canvas(bitmap)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        return bitmap
    }

    fun resizeImage(bitmap: Bitmap, width: Int, height: Int): Bitmap {
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }


    // Method to process image and extract card details
    private fun processImage(bitmap: Bitmap) {
        // Convert Bitmap to InputImage
        val image = InputImage.fromBitmap(bitmap, 0)

        // Initialize text recognizer
        val TextRecognitionOptions = TextRecognizerOptions.Builder().setExecutor {

        }.build()

        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        // Process the image
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val cardType = extractCardDetails(visionText){

                }


            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Text Recognition Failed: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            }
    }

    var cardName: String? = null
    var cardExpiry: String? = null

    // --- Your Parsing Logic (from previous example, adapt as needed) ---
    private fun parseCardDetails(rawText: String) {
        Log.i("TAG", "--- Raw Extracted Text (Post-OpenCV) ---")
        Log.i("TAG", rawText)
        Log.i("TAG", "----------------------------------------")

        val cardNumberRegex = Regex("\\b(\\d{4}\\s?){3}\\d{4}\\b")
        val expiryDateRegex = Regex("\\b(0[1-9]|1[0-2])/([2-9][0-9])\\b")
        val panRegex = Regex("\\b[A-Z]{5}[0-9]{4}[A-Z]{1}\\b")

        val cardNumber = cardNumberRegex.find(rawText)?.value
        val expiryDate = expiryDateRegex.find(rawText)?.value
        val panNumber = panRegex.find(rawText)?.value

        val details = StringBuilder()
        cardNumber?.let { details.append("Card Number: $it\n") }
        expiryDate?.let { details.append("Expiry Date: $it\n") }
        panNumber?.let { details.append("PAN Number: $it\n") }

        if (details.isNotBlank()) {
            Log.i("TAG", "--- Parsed Details ---")
            Log.i("TAG", details.toString())
            // You would update specific TextViews for each detail here
        } else {
            Log.i("TAG", "No specific card details found with basic regex.")
        }
        // WARNING: Handle sensitive data appropriately.
    }

    private fun cardPatterns() {
        // Credit/Debit Card regex (for example)
        val creditCardRegex = Regex("^(4[0-9]{12}(?:[0-9]{3})?)$") // Visa Card pattern
        val debitCardRegex = Regex("^(5[1-5][0-9]{14})$")  // MasterCard pattern

        // Example regex patterns for credit card details, CVV, expiry date
        val cardNumberPattern = Pattern.compile("\\b(?:\\d[ -]*?){13,16}\\b")
        val cvvPattern = Pattern.compile("\\b\\d{3,4}\\b")  // For CVV
        val expiryPattern = Pattern.compile("(0[1-9]|1[0-2])/(\\d{2})")

        val cardNumberMatcher = cardNumberPattern.matcher("cardText")
        val cvvMatcher = cvvPattern.matcher("cardText")
        val expiryMatcher = expiryPattern.matcher("cardText")

        // Check for matches and extract card details
        if (cardNumberMatcher.find()) {
            val cardNumber = cardNumberMatcher.group().replace(" ", "").replace("-", "")
            println("Card Number: $cardNumber")
        }

        if (cvvMatcher.find()) {
            val cvv = cvvMatcher.group()
            println("CVV: $cvv")
        }

        if (expiryMatcher.find()) {
            val expiry = expiryMatcher.group()
            println("Expiry Date: $expiry")
        }
        // Example Regex patterns (these are very basic and need refinement)
        val cardNumberRegex =
            Regex("\\b(\\d{4}\\s?){3}\\d{4}\\b") // 16 digits, possibly with spaces
        val expiryDateRegex = Regex("\\b(0[1-9]|1[0-2])/(\\d{2})\\b") // MM/YY
        val panRegex = Regex("\\b[A-Z]{5}[0-9]{4}[A-Z]{1}\\b") // Indian PAN Card format
        val cvv3DigitRegex = Regex("\\b\\d{3}\\b") // For 3-digit CVVs (typically on card back)
        val amexCIDRegex = Regex("\\b\\d{4}\\b")   // For 4-digit Amex CIDs (can be on card front)
        // Name extraction is harder, often requires looking for lines with all caps or specific keywords.

        val cardNumber = cardNumberRegex.find("cardText")?.value
        val expiryDate = expiryDateRegex.find("cardText")?.value
        val cVVNumber = cvv3DigitRegex.find("cardText")?.value
        val panNumber = panRegex.find("cardText")?.value
        println("expiryDate---> $expiryDate")
        val details = StringBuilder()
        cardNumber?.let { details.append("Card Number: $it\n") }
        expiryDate?.let { details.append("Expiry Date: $it\n") }
        cVVNumber?.let { details.append("CVV Number: $it\n") }
        panNumber?.let { details.append("PAN Number: $it\n") }
        // ... and so on for other fields
    }


}