package com.ciechu.whatisthatinsect

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CaptureRequest
import android.media.MediaPlayer
import android.net.CaptivePortal
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.SystemClock
import android.provider.ContactsContract
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.Surface
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.CameraView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.Observer
import com.google.android.gms.vision.barcode.Barcode
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.custom.CustomImageLabelerOptions
import kotlinx.android.synthetic.main.activity_main.*
import org.koin.android.ext.android.inject
import java.io.File
import java.sql.Date
import java.time.Year
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class WhatSeeActivity : AppCompatActivity(), ImageAnalysis.Analyzer {

    private val imageDetectorViewModel: ImageDetectorViewModel by inject()

    private val TAG = "CameraLabeling"

    /*This is an arbitrary number we are using to keep track of the permission
    request. Where an app has multiple context for requesting permission,
    this can help differentiate the different contexts.*/
    private val REQUEST_CODE_PERMISSIONS = 666
    private val permissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)

    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var labeler: ImageLabeler

    private var imageCapture: ImageCapture? = null
    private var preview: Preview? = null // we to zamiast imageView
    private var imageAnalysis: ImageAnalysis? = null
    private var camera: Camera? = null
    private var mediaPlayer: MediaPlayer? = null
    private var optionMenu: Menu? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        cameraExecutor = Executors.newSingleThreadExecutor()

        if (allPermissionsGranted()) {
            startAnalysis()
        } else {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE_PERMISSIONS)
        }

        take_a_photo_bt.setOnClickListener {
            imageCapture()
            Toast.makeText(applicationContext,"photo saved",Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        optionMenu = menu
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val insectName = what_is_that_insect_tv.text
        if (item.itemId == R.id.info_wiki_bt) {
            insectName.replace(Regex(" ")) {
                when (it.value) {
                    " " -> "_"
                    else -> it.value
                }
            }
            if (what_is_that_insect_tv.text != "None" && what_is_that_insect_tv.text != "What is that insect") {
                val openURL = Intent(Intent.ACTION_VIEW)
                openURL.data = Uri.parse("https://en.wikipedia.org/wiki/$insectName")
                startActivity(openURL)
            } else {
                Toast.makeText(
                    applicationContext,
                    "You must find the insect first",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        if (item.itemId == R.id.listen_name_bt) {
            val uri =
                Uri.parse("https://translate.google.com.vn/translate_tts?ie=UTF-8&q=$insectName&tl=en&client=tw-ob")
            mediaPlayer = MediaPlayer.create(this, uri)
            mediaPlayer?.start()
        }
        if (item.itemId == R.id.flashlight_bt) {
            // TODO Write flashlightFun
        }
        return super.onOptionsItemSelected(item)
    }

    private fun imageCapture(){

        // Set desired name and type of captured image
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "${what_is_that_insect_tv.text}")
            put(MediaStore.MediaColumns.DATE_MODIFIED, System.currentTimeMillis() / 1000)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
        }

// Create the output file option to store the captured image in MediaStore
        val outputFileOptions = ImageCapture.OutputFileOptions
            .Builder(contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            .build()

// Initiate image capture
        imageCapture?.takePicture(outputFileOptions, cameraExecutor, object: ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                // Image was successfully saved to `outputFileResults.savedUri`
            }
            override fun onError(exception: ImageCaptureException) {
                val errorType = exception.imageCaptureError
                Toast.makeText(applicationContext,"$errorType",Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun startAnalysis() {

        val localModel = LocalModel.Builder()
            .setAssetFilePath("lite-model_aiy_vision_classifier_insects_V1_3.tflite")
            .build()

        val customImageLabelerOptions =
            CustomImageLabelerOptions.Builder(localModel)
                .setMaxResultCount(4)
                .setConfidenceThreshold(0.60f)
                .build()

        startCamera()

        imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        labeler = ImageLabeling.getClient(customImageLabelerOptions)

       // analyzingImage()
        imageDetectorObjectLabelObserver()
    }

    private fun imageDetectorObjectLabelObserver() {
        imageDetectorViewModel.objectLabel.observe(this, Observer { text ->
            text?.let { what_is_that_insect_tv.text = it }
        })
    }

    private fun analyzingImage() {
        if (!imageDetectorViewModel.isAnalysing) {
            imageAnalysis?.let {
                imageDetectorViewModel.isAnalysing =
                    true
                it.setAnalyzer(cameraExecutor, this)
            }
        } else {
            imageAnalysis?.clearAnalyzer()
            imageDetectorViewModel.isAnalysing = false
        }
    }

    private fun allPermissionsGranted() = permissions.all {
        ContextCompat.checkSelfPermission(
            applicationContext,
            it
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startAnalysis()
            } else {
                Toast.makeText(this, "Permission not granted by the user.", Toast.LENGTH_LONG)
                    .show()
                finish()
                super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(Runnable {
            cameraProvider = cameraProviderFuture.get()
            preview = Preview.Builder().build()
            imageCapture = ImageCapture.Builder()
                .setTargetRotation(Surface.ROTATION_0)
                .setFlashMode(ImageCapture.FLASH_MODE_AUTO)
                //.setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build()

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()

            camera = cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                preview,
                imageCapture,
                imageAnalysis
            )

            preview?.setSurfaceProvider(
                ContextCompat.getMainExecutor(this),
                viewFinder.createSurfaceProvider()
            )
        }, ContextCompat.getMainExecutor(this))
    }



    @SuppressLint("UnsafeExperimentalUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        imageProxy.image?.let { image ->
            val inputImage = InputImage.fromMediaImage(
                image,
                imageProxy.imageInfo.rotationDegrees
            )

            labeler.process(inputImage)
                .addOnSuccessListener { list ->
                    if (!list.isNullOrEmpty())
                        list.maxBy { it.confidence }?.let { imageLabel ->
                            imageDetectorViewModel.objectLabel.postValue(imageLabel.text)
                        }
                }
                .addOnFailureListener {
                    Log.d(TAG, it.message.toString())
                    image.close()
                    imageProxy.close()
                }
                .addOnCompleteListener {
                    image.close()
                    imageProxy.close()
                }
                .addOnCanceledListener {
                    image.close()
                    imageProxy.close()
                }
        }
    }
}