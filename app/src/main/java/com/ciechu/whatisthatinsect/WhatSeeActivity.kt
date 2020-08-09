package com.ciechu.whatisthatinsect

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.custom.CustomImageLabelerOptions
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
//przyy pierwszym odpalenie do otrzymaniu permission i tak wywala apke


// 1. trzeba uzyskać pozwolenie na kamere. Jest to high Risk wiec musi dac allow
// 2. Odpalamy kamera. Najlepiej uzyć do tego CameraX
// 3. Ustawić obiekt nasłuchujący. Bez robienia zdjecią aby analizowało ostatnią klatkę
// 4. Ustawić model do oznaczania zdjęć TensorFlow
// 5. Wyświetlić dane (ViewModel)
// 6. zamknać zdjęcie. Bez tego zrobi sie za duzo elementów nasłuchujących

// w activity_main preView sluzy do pokazywania widoku z kamerki
// nasz button opala nam analizowanie i do zatrzymuje jednosczesnie

class WhatSeeActivity : AppCompatActivity(), ImageAnalysis.Analyzer {

    private val TAG = "CameraLabeling" //wykorzystywane przy Log w failureListener

//  This is an arbitrary number we are using to keep track of the permission
// request. Where an app has multiple context for requesting permission,
// this can help differentiate the different contexts.
    private val REQUEST_CODE_PERMISSIONS = 666
    private val permissions = arrayOf(Manifest.permission.CAMERA) //tablica zawierająca poczbene permission

    private lateinit var imageDetectorViewModel: ImageDetectorViewModel //wez to w koina
    private lateinit var cameraProvider: ProcessCameraProvider //Dostawca kamery procesowej
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var labeler: ImageLabeler // coś z lifecycle observer

    private var imageCapture: ImageCapture? = null
    private var preview: Preview? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var camera: Camera? = null

    @SuppressLint("UnsafeExperimentalUsageError")   //sprobuj wywalic
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        captureImage()

        imageDetectorViewModel = ViewModelProvider(this)[ImageDetectorViewModel::class.java]
        imageDetectorViewModel.objectLabel.observe(this, Observer { text ->
            text?.let { what_is_that_insect_tv.text = it }   //nasz obserwer z livedata
        })

        val localModel = LocalModel.Builder()
            .setAssetFilePath("lite-model_aiy_vision_classifier_insects_V1_3.tflite") // dajesz całą nazwę pliku z assets
            .build()

        val customImageLabelerOptions = CustomImageLabelerOptions.Builder(localModel) //Niestandardowe opcje etykiet obrazów
            .setMaxResultCount(5) //dowiedziec sie co to za liczby
            .setConfidenceThreshold(0.55f)
            .build()

        cameraExecutor = Executors.newSingleThreadExecutor()

        if (allPermissionsGranted()){
            startCamera()
            imageAnalysis = ImageAnalysis.Builder() // obiekt analizujący ostatnie klatki
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST) //strategia zatrzymywania ostatniej klatki
                .build()
            labeler = ImageLabeling.getClient(customImageLabelerOptions)// klient który tworzy opisy
        } else {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE_PERMISSIONS)
        }

        /*camera_capture_bt.setOnClickListener { // pobawic sie tym buttonem
            if (!imageDetectorViewModel.isAnalysing){
                imageAnalysis?.let {
                    imageDetectorViewModel.isAnalysing = true   // jezeli detektor Nie analizuje to przestaw na analizowanie

                    it.setAnalyzer(cameraExecutor, this)
                }
            } else {
                imageAnalysis?.clearAnalyzer()
                imageDetectorViewModel.isAnalysing = false // jezeli analizuje to go wyczysc i przestan
            }
        }*/
    }

    private fun captureImage(){
        if (!imageDetectorViewModel.isAnalysing){
            imageAnalysis?.let {
                imageDetectorViewModel.isAnalysing = true   // jezeli detektor Nie analizuje to przestaw na analizowanie

                it.setAnalyzer(cameraExecutor, this)
            }
        } else {
            imageAnalysis?.clearAnalyzer()
            imageDetectorViewModel.isAnalysing = false // jezeli analizuje to go wyczysc i przestan
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS){
            if (allPermissionsGranted()){
                startCamera()
            } else {
                Toast.makeText(this, "Permission not granted by the user.", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    //funkcja sprawdzająca czy są dane wszystkie permisiions
    private fun allPermissionsGranted() = permissions.all{
        ContextCompat.checkSelfPermission(applicationContext, it) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("UnsafeExperimentalUsageError") // sprobowac wywalic
    private fun startCamera(){ // odpalamy kamere
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(Runnable {
            cameraProvider = cameraProviderFuture.get()
            preview = Preview.Builder().build()
            imageCapture = ImageCapture.Builder().build()

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()

            camera = cameraProvider.bindToLifecycle(this,
            cameraSelector,
            preview,
            imageCapture,
            imageAnalysis)
            preview?.setSurfaceProvider(ContextCompat.getMainExecutor(this), viewFinder.createSurfaceProvider())
        }, ContextCompat.getMainExecutor(this))
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        imageProxy.image?.let { image ->
            val inputImage = InputImage.fromMediaImage(image, imageProxy.imageInfo.rotationDegrees) //tutaj coś z rotacją a o tym bylo na medium i na oficiajelnej

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
                    imageProxy.close() }
                .addOnCompleteListener {
                    image.close()
                    imageProxy.close()}
                .addOnCanceledListener {
                    image.close()
                    imageProxy.close() }
        }
    }
}