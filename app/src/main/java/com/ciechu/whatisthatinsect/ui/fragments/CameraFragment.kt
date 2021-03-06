package com.ciechu.whatisthatinsect.ui.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.ciechu.whatisthatinsect.R
import com.ciechu.whatisthatinsect.data.Insect
import com.ciechu.whatisthatinsect.viewmodels.ImageDetectorViewModel
import com.ciechu.whatisthatinsect.viewmodels.InsectViewModel
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.custom.CustomImageLabelerOptions
import kotlinx.android.synthetic.main.fragment_camera.*
import org.koin.android.ext.android.inject
import org.koin.android.viewmodel.ext.android.viewModel
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraFragment : Fragment(), ImageAnalysis.Analyzer {

    private val imageDetectorViewModel: ImageDetectorViewModel by inject()
    private val insectViewModel: InsectViewModel by viewModel()

    private val TAG = "CameraLabeling"

    private val REQUEST_CODE_PERMISSIONS = 666
    private val permissions =
        arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )

    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var labeler: ImageLabeler
    private lateinit var resolver: ContentResolver

    private var imageCapture: ImageCapture? = null
    private var preview: Preview? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var camera: Camera? = null
    private var mediaPlayer: MediaPlayer? = null
    private var optionMenu: Menu? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_camera, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        cameraExecutor = Executors.newSingleThreadExecutor()
        resolver = activity?.contentResolver!!

        (requireActivity() as AppCompatActivity).supportActionBar?.title = "Camera analyzer"

        if (allPermissionsGranted()) {
            startAnalysis()
        } else {
            requestPermissions(
                permissions,
                REQUEST_CODE_PERMISSIONS
            )
        }

        take_a_photo_bt.setOnClickListener {
            imageCapture()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_camera, menu)
        optionMenu = menu
        super.onCreateOptionsMenu(menu, inflater)
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
                    requireContext(),
                    "You must find the insect first",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        if (item.itemId == R.id.listen_name_bt) {
            val uri =
                Uri.parse("https://translate.google.com.vn/translate_tts?ie=UTF-8&q=$insectName&tl=en&client=tw-ob")
            mediaPlayer = MediaPlayer.create(requireContext(), uri)
            mediaPlayer?.start()
        }
        if (item.itemId == R.id.flashlight_bt) {
            // TODO Write flashlightFun
        }
        return super.onOptionsItemSelected(item)
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
                Toast.makeText(
                    requireContext(),
                    "Permission not granted by the user.",
                    Toast.LENGTH_LONG
                ).show()
                requireActivity().finish()
                super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            }
        }
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

    private fun saveInsectToDatabase(name: String, image: String, date: String) {
        val insect = Insect(name, image, date)
        insectViewModel.insert(insect)
    }

    private fun imageCapture() {

        if (what_is_that_insect_tv.text != "None" && what_is_that_insect_tv.text != "What is that insect") {

            // Set desired name and type of captured image
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "${what_is_that_insect_tv.text}")
                put(
                    MediaStore.MediaColumns.DATE_MODIFIED.format("dd/MM/yyyy"),
                    (Calendar.getInstance().timeInMillis / 1000L)
                )
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
            }

            // Create the output file option to store the captured image in MediaStore
            val outputFileOptions = ImageCapture.OutputFileOptions
                .Builder(resolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                .build()

            // Initiate image capture
            imageCapture?.takePicture(
                outputFileOptions,
                cameraExecutor,
                object : ImageCapture.OnImageSavedCallback {
                    @SuppressLint("SimpleDateFormat")
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        // Image was successfully saved to `outputFileResults.savedUri`
                        val uri = outputFileResults.savedUri
                        val today = Calendar.getInstance()
                        val dateFormat = SimpleDateFormat("dd/MM/yyyy").format(today.time)

                        saveInsectToDatabase(
                            what_is_that_insect_tv?.text.toString(),
                            uri.toString(),
                            dateFormat
                        )
                    }

                    override fun onError(exception: ImageCaptureException) {
                        val errorType = exception.imageCaptureError
                        Toast.makeText(requireContext(), "$errorType", Toast.LENGTH_SHORT).show()
                    }
                })
            Toast.makeText(requireContext(), "photo saved", Toast.LENGTH_SHORT).show()
           } else {
               Toast.makeText(
                   requireContext(),
                   "You must find the insect first",
                   Toast.LENGTH_SHORT
               ).show()
           }
    }

    private fun startAnalysis() {

        val localModel = LocalModel.Builder()
            .setAssetFilePath("lite-model_aiy_vision_classifier_insects_V1_3.tflite")
            .build()

        val customImageLabelerOptions =
            CustomImageLabelerOptions.Builder(localModel)
                .setMaxResultCount(3)
                .setConfidenceThreshold(0.75f)
                .build()

        startCamera()

        imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        labeler = ImageLabeling.getClient(customImageLabelerOptions)

        analyzingImage()
        imageDetectorObjectLabelObserver()
    }

    private fun imageDetectorObjectLabelObserver() {
        imageDetectorViewModel.objectLabel.observe(viewLifecycleOwner, Observer { text ->
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
            requireContext(),
            it
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
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
                ContextCompat.getMainExecutor(requireContext()),
                viewFinder.createSurfaceProvider()
            )
        }, ContextCompat.getMainExecutor(requireContext()))
    }
}