package com.example.camera.fragments

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.example.camera.MainActivity
import com.example.camera.databinding.FragmentCameraBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizerOptions
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors


class CameraFragment : Fragment() {

    private lateinit var binding: FragmentCameraBinding
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var imageCapture: ImageCapture
    private lateinit var imageAnalyzer: ImageAnalysis
    private lateinit var preview: Preview
    private lateinit var outputFileDirectory: File
    private lateinit var camera: Camera

    private val viewFinder by lazy { binding.viewFinder }
    private val captureButton by lazy { binding.captureButton }

    private val executor = Executors.newSingleThreadExecutor()
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    private var lensFacing = CameraSelector.LENS_FACING_BACK

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "onCreateView")
        // Initialize binging
        binding = FragmentCameraBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    /**
     * Check if user has all permission when fragment resume
     */
    override fun onResume() {
        super.onResume()
        // User may change permissions before resume
        if (!PermissionsFragment.hasPermissions(requireContext())) {
            //Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
            findNavController().navigate(CameraFragmentDirections.actionCameraFragmentToPermissionsFragment())
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        outputFileDirectory = MainActivity.getOutputFileDirectory(requireContext())

        viewFinder.post {

            // Set up click listener
            updateCameraUI()

            // Set up camera
            setUpCamera()
        }

    }

    /**
     * Update UI and set clickListener for each button
     */
    private fun updateCameraUI() {

        // Capture image and store it in the output file directory
        captureButton.setOnClickListener {
            // Get a stable reference of the modifiable image capture use case
            imageCapture.let { imageCapture ->

                // Create the output file to hold image
                val photoFile = createFile(outputFileDirectory, FILE_NAME, PHOTO_EXTENSION)

                // Set up image capture metadata
                val metadata = ImageCapture.Metadata().apply {
                    // Mirror image when using the front camera
                    isReversedHorizontal = lensFacing == CameraSelector.LENS_FACING_FRONT
                }
                // Create output options object which contain files + metadata
                val outputFileOptions = ImageCapture.OutputFileOptions
                    .Builder(photoFile)
                    .setMetadata(metadata)
                    .build()

                imageCapture.takePicture(
                    outputFileOptions,
                    executor,
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                            val savedUri = outputFileResults.savedUri ?: Uri.fromFile(photoFile)
                            Log.d(TAG, "Photo capture succeed: $savedUri (file path: $photoFile)")
                            val image = InputImage.fromFilePath(requireContext(), savedUri)
                            val result = recognizer.process(image)
                                .addOnSuccessListener { visionText ->
                                    Log.d(TAG, "The recognized text is ${visionText.text}")
                                    findNavController().navigate(
                                        CameraFragmentDirections.actionCameraFragmentToRecognitionFragment(
                                            visionText.text
                                        )
                                    )
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(
                                        context,
                                        "Failed to recognize text!\nThe error is $e",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }

                        }

                        override fun onError(exception: ImageCaptureException) {
                            Log.d(TAG, "Image capture failed: ${exception.message}", exception)
                        }
                    })
            }
        }

    }

    /**
     * Create cameraProvider, then bind camera useCases
     */
    private fun setUpCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({

            cameraProvider = cameraProviderFuture.get()

            lensFacing = when {
                cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) -> CameraSelector.LENS_FACING_BACK
                cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) -> CameraSelector.LENS_FACING_FRONT
                else -> throw IllegalStateException("No camera was found.")
            }

            bindCameraUseCases()

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    /**
     * Create cameraSelector and useCases(preview, imageCapture, imageAnalysis)
     * unbind previous useCases, rebind with new one.
     */
    @SuppressLint("UnsafeOptInUsageError")
    private fun bindCameraUseCases() {

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        preview = Preview.Builder().build()
        preview.setSurfaceProvider(viewFinder.surfaceProvider)

        imageCapture = ImageCapture.Builder().build()

        imageAnalyzer = ImageAnalysis.Builder().build()
            // The analyzer can then be assigned to the instance
            .also {
                it.setAnalyzer(executor, { imageProxy ->
                    // For realtime application
                })
            }

        cameraProvider.unbindAll()
        try {
            camera = cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                preview,
                imageCapture,
                imageAnalyzer
            )
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Bind camera use case failed.", Toast.LENGTH_LONG)
                .show()
        }

    }

    /**
     * Check if has back camera
     */
    private fun hasBackCamera() = cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)

    /**
     * Check if has front camera
     */
    private fun hasFrontCamera() = cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)


    companion object {
        private const val TAG = "CameraFragment"

        // String in the single quote do not map to real number
        private const val FILE_NAME = "'IMG'_yyyyMMdd_HHmmss'.jpg'"
        private const val PHOTO_EXTENSION = ".jpg"

        private val WHITELIST_EXTENSION = arrayOf("JPG")

        private fun createFile(baseFolder: File, format: String, extension: String) = File(
            baseFolder,
            SimpleDateFormat(FILE_NAME, Locale.CHINA).format(System.currentTimeMillis())
        )
    }
}