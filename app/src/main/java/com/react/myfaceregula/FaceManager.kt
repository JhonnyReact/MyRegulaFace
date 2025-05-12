package com.react.myfaceregula

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import com.regula.facesdk.FaceSDK
import com.regula.facesdk.callback.FaceInitializationCompletion
import com.regula.facesdk.configuration.FaceCaptureConfiguration
import com.regula.facesdk.configuration.InitializationConfiguration
import com.regula.facesdk.configuration.MatchFacesConfiguration
import com.regula.facesdk.detection.request.OutputImageCrop
import com.regula.facesdk.detection.request.OutputImageParams
import com.regula.facesdk.enums.ImageType
import com.regula.facesdk.enums.OutputImageCropAspectRatio
import com.regula.facesdk.enums.ProcessingMode
import com.regula.facesdk.exception.InitException
import com.regula.facesdk.model.MatchFacesImage
import com.regula.facesdk.model.results.FaceCaptureResponse
import com.regula.facesdk.model.results.matchfaces.MatchFacesResponse
import com.regula.facesdk.model.results.matchfaces.MatchFacesSimilarityThresholdSplit
import com.regula.facesdk.request.MatchFacesRequest
import java.io.IOException


class FaceManager {

    val TAG = javaClass.simpleName
    init {
        Log.i("--->", ": enjoy")
    }
    var cont = 1;
    var image1: Bitmap? = null
    var image2: Bitmap? = null

    fun initSdKBasic(context: Context) {
        FaceSDK.Instance().initialize(context) { status: Boolean, e: InitException? ->
            if (!status) {
                Toast.makeText(
                    context,
                    "Init finished with error: " + if (e != null) e.message else "",
                    Toast.LENGTH_LONG
                ).show()
                return@initialize
            }
            Log.d("MainActivity", "FaceSDK init completed successfully")
            //startFaceCaptureActivity(context = context)
        }
    }

    fun initSdK2(context: Context) {
        val license: ByteArray? = recoverLicense(context)
        license?.let {
            val initConfig: InitializationConfiguration = InitializationConfiguration.Builder(license).setLicenseUpdate(true).build()
            FaceSDK.Instance().initialize(context, initConfig) { status, e ->
                if (!status) {
                    Log.d("MainActivity", "FaceSDK error: " + e?.message)
                    Toast.makeText(
                        context,
                        "Init finished with error: " + if (e != null) e.message else "",
                        Toast.LENGTH_LONG
                    ).show()
                    return@initialize
                }
                Log.d("MainActivity", "FaceSDK init succeed ")
                Log.d("MainActivity", "FaceSDK init completed successfully")
            }
        } ?: return
    }

    fun initSdK(context: Context){
        val license: ByteArray? = getLicense(context)
        license?.let {
            val configuration = InitializationConfiguration.Builder(license).setLicenseUpdate(false).build()
            FaceSDK.Instance().initialize(context, configuration, { status, exception ->
                    Log.i(TAG, "initSdK: status: $status")
                if (!status) {
                    exception?.let {
                        Log.e(TAG, "initSdK: ", exception)
                        Log.e(TAG, "initSdK: " +
                                "\nCode: ${exception.errorCode}" +
                                "\nCause: ${exception.cause}" +
                                "\nMessage: ${exception.message}" +
                                "\nError: ",exception)
                    }
                }

                    //startFaceCaptureActivity(context = context)
                })
        }

    }

    fun getLicense(context: Context?): ByteArray? {
        if (context == null) return null
        val licInput = context.resources.openRawResource(R.raw.regula)
        val available: Int = try {
            licInput.available()
        } catch (e: IOException) {
            return null
        }
        val license = ByteArray(available)
        try {
            licInput.read(license)
        } catch (e: IOException) {
            return null
        }
        return license
    }

    private fun recoverLicense(context: Context):ByteArray? {
        var license: ByteArray? = null
        try {
            val licInput = context.resources.openRawResource(R.raw.regula)
            val available: Int = licInput.available()
            license = ByteArray(available)
            licInput.read(license)
        } catch (ex: Exception) {
            //onStartedFail(R.string.error_reading_license)
            Log.e(TAG, "recoverLicense: Error reading license", )
        }
        return license;
    }

    fun startFaceCaptureActivity(bitmap: Bitmap? = null, context: Context) {
        val configuration = FaceCaptureConfiguration.Builder().setCameraSwitchEnabled(true).build()

        FaceSDK.Instance().presentFaceCaptureActivity(context, configuration) { faceCaptureResponse: FaceCaptureResponse? ->
            if (faceCaptureResponse?.image != null) {
                if (cont == 1) {
                    cont++
                    image1 = faceCaptureResponse.image!!.bitmap
                } else {
                    cont = 1
                    image2 = faceCaptureResponse.image!!.bitmap
                    matchFaces(image1!!, image2!!, context)
                }
            }
            /*if (faceCaptureResponse?.image != null) {
                imageView!!.setImageBitmap(faceCaptureResponse.image!!.bitmap)
            }*/
        }
    }

    fun matchFacesOffline(first: Bitmap = createSolidColorBitmap(), second: Bitmap = createSolidColorBitmap(), context: Context) {
        val firstImage = MatchFacesImage(first, ImageType.PRINTED, false)
        val secondImage = MatchFacesImage(second, ImageType.PRINTED, false)
        val matchFacesRequest = MatchFacesRequest(arrayListOf(firstImage, secondImage))

        val crop = OutputImageCrop(
            OutputImageCropAspectRatio.OUTPUT_IMAGE_CROP_ASPECT_RATIO_3X4
        )
        val outputImageParams = OutputImageParams(crop, Color.WHITE)
        matchFacesRequest.outputImageParams = outputImageParams

        val configuration =
            MatchFacesConfiguration.Builder().setProcessingMode(ProcessingMode.OFFLINE).build()


        FaceSDK.Instance().matchFaces(matchFacesRequest, configuration) { matchFacesResponse: MatchFacesResponse ->
            val split = MatchFacesSimilarityThresholdSplit(matchFacesResponse.results, 0.75)
            val similarity = if (split.matchedFaces.size > 0) {
                split.matchedFaces[0].similarity
            } else if (split.unmatchedFaces.size > 0){
                split.unmatchedFaces[0].similarity
            } else {
                null
            }

            val text = similarity?.let {
                "Similarity: " + String.format("%.2f", it * 100) + "%"
            } ?: matchFacesResponse.exception?.let {
                "Similarity: " + it.message
            } ?: "Similarity: "

            Log.i("--->", "similarity: $text ")
            var faceBitmaps: ArrayList<Bitmap> = arrayListOf()


            for(matchFaces in matchFacesResponse.detections) {
                for (face in matchFaces.faces)
                    face.crop?.let {
                        faceBitmaps.add(it) }
            }

            /*val l = faceBitmaps.size
            if (l > 0) {
                buttonSee.text = "Detections ($l)"
                buttonSee.visibility = View.VISIBLE
            } else {
                buttonSee.visibility = View.GONE
            }

            buttonMatch.isEnabled = true
            buttonClear.isEnabled = true*/
        }
    }

    fun matchFaces(first: Bitmap = createSolidColorBitmap(), second: Bitmap = createSolidColorBitmap(), context: Context) {
        val firstImage = MatchFacesImage(first, ImageType.PRINTED, false)
        val secondImage = MatchFacesImage(second, ImageType.PRINTED, false)
        val matchFacesRequest = MatchFacesRequest(arrayListOf(firstImage, secondImage))

        val crop = OutputImageCrop(
            OutputImageCropAspectRatio.OUTPUT_IMAGE_CROP_ASPECT_RATIO_3X4
        )
        val outputImageParams = OutputImageParams(crop, Color.WHITE)
        matchFacesRequest.outputImageParams = outputImageParams

        FaceSDK.Instance().matchFaces(context, matchFacesRequest) { matchFacesResponse: MatchFacesResponse ->
            val split = MatchFacesSimilarityThresholdSplit(matchFacesResponse.results, 0.75)
            val similarity = if (split.matchedFaces.size > 0) {
                split.matchedFaces[0].similarity
            } else if (split.unmatchedFaces.size > 0){
                split.unmatchedFaces[0].similarity
            } else {
                null
            }

            val text = similarity?.let {
                "Similarity: " + String.format("%.2f", it * 100) + "%"
            } ?: matchFacesResponse.exception?.let {
                "Similarity: " + it.message
            } ?: "Similarity: "

            Log.i("--->", "similarity: $text ")
            var faceBitmaps: ArrayList<Bitmap> = arrayListOf()


            for(matchFaces in matchFacesResponse.detections) {
                for (face in matchFaces.faces)
                    face.crop?.let {
                        faceBitmaps.add(it) }
            }

            /*val l = faceBitmaps.size
            if (l > 0) {
                buttonSee.text = "Detections ($l)"
                buttonSee.visibility = View.VISIBLE
            } else {
                buttonSee.visibility = View.GONE
            }

            buttonMatch.isEnabled = true
            buttonClear.isEnabled = true*/
        }
    }

    fun createSolidColorBitmap(): Bitmap {
        val width = 200
        val height = 200

        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            val canvas = Canvas(this)
            val paint = Paint().apply {
                color = Color.RED
                style = Paint.Style.FILL
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        }
    }

    fun deInitialice(){
        FaceSDK.Instance().deinitialize()
    }
}