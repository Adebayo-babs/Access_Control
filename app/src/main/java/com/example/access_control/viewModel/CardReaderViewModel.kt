package com.example.access_control.viewModel

import android.app.Application
import android.graphics.Bitmap
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.example.neurotecsdklibrary.NeurotecLicenseHelper
import com.neurotec.biometrics.NBiometricCaptureOption
import com.neurotec.biometrics.NBiometricOperation
import com.neurotec.biometrics.NBiometricStatus
import com.neurotec.biometrics.NFace
import com.neurotec.biometrics.NSubject
import com.neurotec.biometrics.client.NBiometricClient
import com.neurotec.devices.NCamera
import com.neurotec.devices.NDeviceType
import com.neurotec.images.NImage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.EnumSet
import java.util.concurrent.Executors

data class FaceDetectionFeedback(
    val lightingStatus: LightingStatus = LightingStatus.UNKNOWN,
    val distanceStatus: DistanceStatus = DistanceStatus.UNKNOWN,
    val positionStatus: PositionStatus = PositionStatus.UNKNOWN,
    val qualityStatus: QualityStatus = QualityStatus.UNKNOWN,
    val overallMessage: String = "Position your face in view"
)

enum class LightingStatus {
    GOOD, UNKNOWN
}

enum class DistanceStatus {
    GOOD, UNKNOWN
}

enum class PositionStatus {
    CENTERED, UNKNOWN
}

enum class QualityStatus {
    EXCELLENT, UNKNOWN
}

class CardReaderViewModel(application: Application) : AndroidViewModel(application) {
    data class DialogState(
        val showDialog: Boolean = false,
        val message: String = "",
        val capturedFace: Bitmap? = null
    )

    // Private mutable state - only this viewModel can change it
    private val _dialogState = MutableStateFlow(DialogState())
    // Public read-only state - UI can observe but not modify
    val dialogState: StateFlow<DialogState> = _dialogState.asStateFlow()

    private val main = android.os.Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()

    var status by mutableStateOf("")
        private set

    var biometricClient: NBiometricClient? = null
        private set

    var isCapturing by mutableStateOf(false)
        private set

    var currentSubject: NSubject? = null
        private set

    var detectionFeedback by mutableStateOf(FaceDetectionFeedback())
        private set

    var useNeurotecCamera by mutableStateOf(true)
        private set

    private var isInitialized = false
    private var captureInProgress = false

    fun initialize() {
        if (isInitialized) {
            startAutomaticCapture()
            return
        }

        executor.execute {
            try {
                NeurotecLicenseHelper.obtainFaceLicenses(getApplication())
                main.post {
                    status = "Licenses OK - Initializing"
                    initClient()
                }
            } catch (e: Exception) {
                main.post { status = "License Error: ${e.message}" }
            }
        }
    }

    private fun initClient() {
        try {
            biometricClient = NBiometricClient().apply {
                setFacesDetectProperties(true)
                isUseDeviceManager = true
                deviceManager.deviceTypes = EnumSet.of(NDeviceType.CAMERA)

                facesQualityThreshold = 50
                facesConfidenceThreshold = 1

                isUseDeviceManager = true
                deviceManager.deviceTypes = EnumSet.of(NDeviceType.CAMERA)
                // Disable features that needs the missing models
                setProperty("Faces.DetectAllFeaturePoints", "false")
                setProperty("Faces.RecognizeExpression", "false")

                initialize()
            }

            val cameras = biometricClient?.deviceManager?.devices
            if (cameras?.isNotEmpty() == true) {
                val camera = cameras[0] as NCamera
                biometricClient?.faceCaptureDevice = camera

                isInitialized = true

                main.postDelayed({
                    status = "Ready. Position your face..."
                    startAutomaticCapture()
                }, 300)
            } else {
                status = "No camera found"
            }
        } catch (e: Exception) {
            Log.e("CardReaderViewModel", "Camera initialization error", e)
            main.post { status = "Camera initialization error: ${e.message}" }
        }
    }

    fun startAutomaticCapture() {
        if (captureInProgress || !isInitialized) {
            Log.d("CardReaderViewModel", "Capture already in progress or not initialized")
            return
        }

        executor.execute {
            try {
                captureInProgress = true
                main.post {
                    isCapturing = true
                    status = "Detecting face..."
                    detectionFeedback = FaceDetectionFeedback(
                        overallMessage = "Looking for face..."
                    )
                }

                Log.d("CardReaderViewModel", "Starting automatic capture...")

                val subject = NSubject()
                val face = NFace().apply {
                    captureOptions = EnumSet.of(NBiometricCaptureOption.STREAM)
                }

                subject.faces.add(face)
                main.post { currentSubject = subject }

                val task = biometricClient?.createTask(
                    EnumSet.of(NBiometricOperation.CAPTURE, NBiometricOperation.CREATE_TEMPLATE),
                    subject
                )

                task?.let { biometricClient?.performTask(it) }

                val taskStatus = task?.status

                Log.d("CardReaderViewModel", "Capture task status: $taskStatus")

                when (taskStatus) {
                    NBiometricStatus.OK -> {
                        Log.d("CardReaderViewModel", "Face captured successfully!")

                        // Extract face image
                        val faceImage = subject.faces.firstOrNull()?.image
                        val bitmap = faceImage?.let { convertNImageToBitmap(it) }

                        main.post {
                            status = "Face captured successfully!"
                            detectionFeedback = FaceDetectionFeedback(
                                lightingStatus = LightingStatus.GOOD,
                                distanceStatus = DistanceStatus.GOOD,
                                positionStatus = PositionStatus.CENTERED,
                                qualityStatus = QualityStatus.EXCELLENT,
                                overallMessage = "Perfect! Face captured!"
                            )

                            // Show dialog with captured face
                            showFaceDetectedDialog(bitmap)
                        }

                        captureInProgress = false
                    }

                    NBiometricStatus.TIMEOUT,
                    NBiometricStatus.BAD_OBJECT -> {
                        Log.w("CardReaderViewModel", "No face detected, retrying...")
                        main.post {
                            status = "No face detected. Please position your face..."
                            detectionFeedback = FaceDetectionFeedback(
                                overallMessage = "No face detected. Please try again..."
                            )
                        }

                        captureInProgress = false
                        main.postDelayed({ startAutomaticCapture() }, 500)
                    }

                    else -> {
                        Log.w("CardReaderViewModel", "Capture failed: $taskStatus")
                        main.post {
                            status = "Capture failed. Retrying..."
                            detectionFeedback = FaceDetectionFeedback(
                                overallMessage = "Detection failed. Retrying..."
                            )
                        }

                        captureInProgress = false
                        main.postDelayed({ startAutomaticCapture() }, 800)
                    }
                }

                task?.dispose()

            } catch (e: Exception) {
                Log.e("CardReaderViewModel", "Error during capture", e)
                captureInProgress = false
                main.post {
                    isCapturing = false
                    status = "Capture error. Retrying..."
                    detectionFeedback = FaceDetectionFeedback(
                        overallMessage = "Error occurred. Retrying..."
                    )
                    main.postDelayed({ startAutomaticCapture() }, 1000)
                }
            }
        }
    }

    private fun convertNImageToBitmap(nImage: NImage): Bitmap? {
        return try {
            val bitmap = nImage.toBitmap()
            Log.d("CardReaderViewModel", "Converted to bitmap: ${bitmap.width}x${bitmap.height}")
            bitmap
        } catch (e: Exception) {
            Log.e("CardReaderViewModel", "Error converting NImage to Bitmap", e)
            null
        }
    }

    fun stopCapture() {
        captureInProgress = false
        isCapturing = false
        currentSubject = null
    }

    fun toggleCameraPreview() {
        useNeurotecCamera = !useNeurotecCamera
        Log.d("CardReaderViewModel", "Camera preview toggled to: ${if (useNeurotecCamera) "Neurotec (Colored)" else "CameraX (Grayscale)"}")
    }

    private fun showFaceDetectedDialog(faceBitmap: Bitmap?) {
        _dialogState.value = DialogState(
            showDialog = true,
            message = "Face Detected Successfully!",
            capturedFace = faceBitmap
        )
    }

    fun showCardTappedDialog() {
        _dialogState.value = DialogState(
            showDialog = true,
            message = "Card tapped"
        )
    }

    fun hideDialog() {
        _dialogState.value = DialogState(
            showDialog = false,
            message = "",
            capturedFace = null
        )
    }

    fun reset() {
        stopCapture()
        hideDialog()
        status = ""
        detectionFeedback = FaceDetectionFeedback()
        isInitialized = false
    }

    override fun onCleared() {
        super.onCleared()
        stopCapture()
        executor.shutdown()
        biometricClient?.dispose()
    }
}