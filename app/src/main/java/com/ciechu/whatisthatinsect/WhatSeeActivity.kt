package com.ciechu.whatisthatinsect

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.custom.CustomImageLabelerOptions
import kotlinx.android.synthetic.main.activity_main.*
import org.koin.android.ext.android.inject
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class WhatSeeActivity : AppCompatActivity(), ImageAnalysis.Analyzer {

    private val imageDetectorViewModel: ImageDetectorViewModel by inject()

    // Tag for the [Log]
    private val TAG = "CameraLabeling"

    /*This is an arbitrary number we are using to keep track of the permission
    request. Where an app has multiple context for requesting permission,
    this can help differentiate the different contexts.*/
    private val REQUEST_CODE_PERMISSIONS = 666
    private val permissions =
        arrayOf(Manifest.permission.CAMERA)

    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var labeler: ImageLabeler

    private var imageCapture: ImageCapture? = null
    private var preview: Preview? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var camera: Camera? = null
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val localModel = LocalModel.Builder()
            .setAssetFilePath("lite-model_aiy_vision_classifier_insects_V1_3.tflite")
            .build()

        val customImageLabelerOptions =
            CustomImageLabelerOptions.Builder(localModel)
                .setMaxResultCount(4)
                .setConfidenceThreshold(0.70f)
                .build()

        cameraExecutor = Executors.newSingleThreadExecutor()

        startAnalysis(customImageLabelerOptions)
        captureImage()
        imageDetectorObjectLabelObserver()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
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
        if (item.itemId == R.id.listen_name_bt){
            val uri = Uri.parse("https://translate.google.com.vn/translate_tts?ie=UTF-8&q=$insectName&tl=en&client=tw-ob")
            mediaPlayer = MediaPlayer.create(this, uri)
            mediaPlayer?.start()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun startAnalysis(customImageLabelerOptions: CustomImageLabelerOptions) {
        if (allPermissionsGranted()) {
            startCamera()
            imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            labeler = ImageLabeling.getClient(customImageLabelerOptions)
        } else {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE_PERMISSIONS)
        }
    }

    private fun imageDetectorObjectLabelObserver() {
        imageDetectorViewModel.objectLabel.observe(this, Observer { text ->
            text?.let { what_is_that_insect_tv.text = it }
        })
    }

    private fun captureImage() {
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                /* Toast.makeText(this, "Permission not granted by the user.", Toast.LENGTH_LONG).show()
                 finish()*/
                super.onRequestPermissionsResult(requestCode, permissions, grantResults)

            }
        }
    }

    private fun allPermissionsGranted() = permissions.all {
        ContextCompat.checkSelfPermission(
            applicationContext,
            it
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(Runnable {
            cameraProvider = cameraProviderFuture.get()
            preview = Preview.Builder().build()
            imageCapture = ImageCapture.Builder().build()

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