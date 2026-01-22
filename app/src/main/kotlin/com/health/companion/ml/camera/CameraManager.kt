package com.health.companion.ml.camera

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.LifecycleOwner
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class CameraManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var imageCapture: ImageCapture? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    
    private val _isCameraReady = MutableStateFlow(false)
    val isCameraReady: StateFlow<Boolean> = _isCameraReady.asStateFlow()
    
    private val _isFlashEnabled = MutableStateFlow(false)
    val isFlashEnabled: StateFlow<Boolean> = _isFlashEnabled.asStateFlow()
    
    private val _cameraFacing = MutableStateFlow(CameraSelector.LENS_FACING_BACK)
    val cameraFacing: StateFlow<Int> = _cameraFacing.asStateFlow()

    suspend fun initializeCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView
    ) = suspendCancellableCoroutine { continuation ->
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                
                // Preview
                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                
                // ImageCapture
                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                    .setFlashMode(
                        if (_isFlashEnabled.value) ImageCapture.FLASH_MODE_ON
                        else ImageCapture.FLASH_MODE_OFF
                    )
                    .build()
                
                // Camera selector
                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(_cameraFacing.value)
                    .build()
                
                // Unbind any existing use cases
                cameraProvider?.unbindAll()
                
                // Bind use cases to camera
                cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )
                
                _isCameraReady.value = true
                Timber.d("Camera initialized successfully")
                continuation.resume(Unit)
                
            } catch (e: Exception) {
                Timber.e(e, "Camera initialization failed")
                _isCameraReady.value = false
                continuation.resumeWithException(e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    suspend fun takePicture(): Uri = suspendCancellableCoroutine { continuation ->
        val imageCapture = imageCapture ?: run {
            continuation.resumeWithException(IllegalStateException("Camera not initialized"))
            return@suspendCancellableCoroutine
        }
        
        val photoFile = createImageFile()
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        
        imageCapture.takePicture(
            outputOptions,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = output.savedUri ?: Uri.fromFile(photoFile)
                    Timber.d("Photo saved: $savedUri")
                    continuation.resume(savedUri)
                }
                
                override fun onError(exception: ImageCaptureException) {
                    Timber.e(exception, "Photo capture failed")
                    continuation.resumeWithException(exception)
                }
            }
        )
    }

    fun takePictureWithCallback(
        onPhotoCapture: (Uri) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val imageCapture = imageCapture ?: run {
            onError(IllegalStateException("Camera not initialized"))
            return
        }
        
        val photoFile = createImageFile()
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        
        imageCapture.takePicture(
            outputOptions,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = output.savedUri ?: Uri.fromFile(photoFile)
                    Timber.d("Photo saved: $savedUri")
                    onPhotoCapture(savedUri)
                }
                
                override fun onError(exception: ImageCaptureException) {
                    Timber.e(exception, "Photo capture failed")
                    onError(exception)
                }
            }
        )
    }

    fun toggleFlash() {
        _isFlashEnabled.value = !_isFlashEnabled.value
        imageCapture?.flashMode = if (_isFlashEnabled.value) {
            ImageCapture.FLASH_MODE_ON
        } else {
            ImageCapture.FLASH_MODE_OFF
        }
        Timber.d("Flash toggled: ${_isFlashEnabled.value}")
    }

    fun switchCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        _cameraFacing.value = if (_cameraFacing.value == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        
        // Reinitialize camera with new facing
        kotlinx.coroutines.runBlocking {
            initializeCamera(lifecycleOwner, previewView)
        }
        Timber.d("Camera switched to: ${if (_cameraFacing.value == CameraSelector.LENS_FACING_BACK) "Back" else "Front"}")
    }

    private fun createImageFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        
        return File.createTempFile(
            "HEALTH_DOC_${timestamp}_",
            ".jpg",
            storageDir
        ).also {
            Timber.d("Created image file: ${it.absolutePath}")
        }
    }

    fun getPhotoUri(file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
    }

    fun release() {
        try {
            cameraProvider?.unbindAll()
            cameraExecutor.shutdown()
            _isCameraReady.value = false
            Timber.d("Camera released")
        } catch (e: Exception) {
            Timber.e(e, "Failed to release camera")
        }
    }

    fun shutdown() {
        release()
    }
}

// Camera state for Compose
data class CameraState(
    val isReady: Boolean = false,
    val isFlashEnabled: Boolean = false,
    val isFrontCamera: Boolean = false,
    val capturedImageUri: Uri? = null,
    val error: String? = null
)
