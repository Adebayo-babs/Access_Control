package com.example.neurotecsdklibrary

import android.content.Context
import android.util.Log
import com.neurotec.licensing.NLicense
import java.io.File

object NeurotecLicenseHelper {

    private const val TAG = "NeurotecLicense"

    // Face licenses
    const val LICENSE_FACE_DETECTION = "Biometrics.FaceDetection"
    const val LICENSE_FACE_EXTRACTION = "Biometrics.FaceExtraction"
    const val LICENSE_FACE_MATCHING = "Biometrics.FaceMatching"

    // Fingerprint licenses
    const val LICENSE_FINGER_DETECTION = "Biometrics.FingerDetection"
    const val LICENSE_FINGER_EXTRACTION = "Biometrics.FingerExtraction"
    const val LICENSE_FINGER_MATCHING = "Biometrics.FingerMatching"

    private val faceLicenseComponents = listOf(
        LICENSE_FACE_DETECTION,
        LICENSE_FACE_EXTRACTION,
        LICENSE_FACE_MATCHING
    )

    private val fingerLicenseComponents = listOf(
        LICENSE_FINGER_DETECTION,
        LICENSE_FINGER_EXTRACTION,
        LICENSE_FINGER_MATCHING
    )


    // Obtain face licenses
    fun obtainFaceLicenses(context: Context): Boolean {
        return try {
            Log.d(TAG, "Starting License Activation")

//            NLicenseManager.setTrialMode(true)
            Log.d(TAG, "Trial mode enabled")

            var faceExtraction = NLicense.isComponentActivated(LICENSE_FACE_EXTRACTION)
            var faceMatching = NLicense.isComponentActivated(LICENSE_FACE_MATCHING)

            if (!faceExtraction || !faceMatching) {

                val licenseDir = File(context.filesDir, "Licenses")

                licenseDir.listFiles()?.forEach { file ->
                    try {
                        val content = file.readText()
                        NLicense.add(content)
                        Log.d(TAG, " Loaded license file: ${file.name}")
                    } catch (e: Exception) {
                        Log.e(TAG, " Failed to load ${file.name}", e)
                    }
                }

                faceLicenseComponents.forEach { component ->
                    try {
                        val obtained = NLicense.obtainComponents("/local", "5000", component)
                        Log.d(TAG, "$component activation = $obtained")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error activating $component", e)
                    }
                }
            }

            NLicense.isComponentActivated(LICENSE_FACE_EXTRACTION) &&
                    NLicense.isComponentActivated(LICENSE_FACE_MATCHING)

        } catch (e: Exception) {
            Log.e(TAG, " License activation failed", e)
            e.printStackTrace()
            false
        }
    }


    // Obtain finger licenses
    fun obtainFingerLicenses(context: Context): Boolean {
        return try {
            Log.d(TAG, "Starting Fingerprint License Activation")

            var fingerExtraction = NLicense.isComponentActivated(LICENSE_FINGER_EXTRACTION)
            var fingerMatching = NLicense.isComponentActivated(LICENSE_FINGER_MATCHING)

            if (!fingerExtraction || !fingerMatching) {
                loadLicenseFiles(context)

                fingerLicenseComponents.forEach { component ->
                    try {
                        val obtained = NLicense.obtainComponents("/local", "5000", component)
                        Log.d(TAG, "$component activation = $obtained")
                    } catch (e: Exception) {
                        Log.d(TAG, "Error activating $component", e)
                    }
                }
            }
            val result = NLicense.isComponentActivated(LICENSE_FINGER_EXTRACTION) &&
                    NLicense.isComponentActivated(LICENSE_FINGER_MATCHING)

            Log.d(TAG, "Fingerprint licenses activation result: $result")
            result
        } catch (e: Exception) {
            Log.d(TAG, "Fingerprint licenses activation failed", e)
            false
        }
    }

    private fun loadLicenseFiles(context: Context) {
        val licenseDir = File(context.filesDir, "Licenses")

        if (!licenseDir.exists() || !licenseDir.isDirectory) {
            Log.w(TAG, "License directory not found: ${licenseDir.absolutePath}")
            return
        }

        licenseDir.listFiles()?.forEach { file ->
            try {
                val content = file.readText()
                NLicense.add(content)
                Log.d(TAG, " Loaded license file: ${file.name}")
            } catch (e: Exception) {
                Log.e(TAG, " Failed to load ${file.name}", e)
            }
        }
    }

    fun release() {
        try {
            val allComponents = (faceLicenseComponents + fingerLicenseComponents).joinToString (",")
            NLicense.releaseComponents(allComponents)
            Log.d(TAG, "All licenses released")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to release licenses", e)
        }
    }
}