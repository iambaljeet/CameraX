package com.app.camerax

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import com.app.camerax.databinding.ActivityMainBinding
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"

    private lateinit var binding: ActivityMainBinding

    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null

    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initUi()
        initCamera()
    }

    private fun initUi() {
        with(binding) {
            ivCaptureImage.setOnClickListener {
                captureImage()
            }

            ivCaptureVideo.setOnClickListener {
                captureVideo()
            }
        }
    }

    private fun initCamera() {
        cameraExecutor = Executors.newSingleThreadExecutor()

        if (Utility.cameraPermissionsGranted(this)) {
            startCamera()
        } else {
            requestCameraPermissions.launch(Utility.CAMERA_PERMISSIONS)
        }
    }

    private val requestCameraPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { uris ->
            if (Utility.cameraPermissionsGranted(this)) {
                startCamera()
            }
        }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()


            imageCapture = ImageCapture.Builder().build()

            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                .build()

            videoCapture = VideoCapture.withOutput(recorder)

            preview.setSurfaceProvider(binding.previewView.surfaceProvider)

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            runCatching {
                cameraProvider.unbindAll()

                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture, videoCapture
                )
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun captureImage() {
        val imageCapture = imageCapture ?: return

        val image = File(
            Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES
            ), "${System.currentTimeMillis()}.jpg"
        )
        image.createNewFile()

        val imageCaptureOptions = ImageCapture.OutputFileOptions.Builder(image)
            .build()

        val imageCaptureCallback = object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                Log.d(TAG, "onImageSaved: output: ${outputFileResults.savedUri}")
            }

            override fun onError(exception: ImageCaptureException) {
                Log.d(TAG, "onError: exception: $exception")
            }
        }

        imageCapture.takePicture(
            imageCaptureOptions,
            ContextCompat.getMainExecutor(this),
            imageCaptureCallback
        )
    }

    private fun captureVideo() {
        val videoCapture = videoCapture ?: return

        recording?.let {
            recording?.stop()
            recording = null

            binding.ivCaptureVideo.setImageResource(R.drawable.outline_play_circle_outline_24)

            return
        }

        val video = File(
            Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MOVIES
            ), "${System.currentTimeMillis()}.mp4"
        )
        video.createNewFile()

        val videoCaptureOptions = FileOutputOptions.Builder(video)
            .build()

        recording = videoCapture.output.prepareRecording(
            this, videoCaptureOptions
        ).start(
            ContextCompat.getMainExecutor(this)
        ) { recordEvent ->
            when (recordEvent) {
                is VideoRecordEvent.Start -> {
                    binding.ivCaptureVideo.setImageResource(R.drawable.outline_stop_circle_24)
                }

                is VideoRecordEvent.Pause -> {
                }

                is VideoRecordEvent.Resume -> {
                }

                is VideoRecordEvent.Finalize -> {
                }

                is VideoRecordEvent.Status -> {
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        cameraExecutor.shutdown()
    }
}