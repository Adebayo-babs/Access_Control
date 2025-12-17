package com.example.access_control

import android.app.Application
import android.util.Log
import com.example.neurotecsdklibrary.NeurotecLicenseHelper
import com.neurotec.lang.NCore
import com.neurotec.licensing.NLicenseManager
import com.neurotec.plugins.NDataFileManager

class MatchApplication : Application() {

    companion object {
        private const val TAG = "FaceMatchApplication"
        @Volatile
        var areFaceLicensesActivated = false
            private set

        @Volatile
        var areFingerLicensesActivated = false
            private set
    }

    override fun onCreate() {
        super.onCreate()

        try {
            NLicenseManager.setTrialMode(true)
            Log.d("NeurotecLicense", "Trial mode enabled")
        } catch (e: Exception) {
            Log.e("NeurotecLicense", "Failed to set trial mode", e)
        }

        try {
            NCore.setContext(this@MatchApplication)
            NDataFileManager.getInstance().addFromDirectory("data", false)
        }catch (e: Exception){
            Log.e(TAG, "Failed to set NCore context", e)
        }


        // Activate face licenses
        try {
            areFaceLicensesActivated = NeurotecLicenseHelper.obtainFaceLicenses(this@MatchApplication)
            if (areFaceLicensesActivated) {
                Log.d(TAG, " Licenses activated successfully")
            } else {
                Log.e(TAG, " License activation FAILED")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error doing background Neurotec initialization", e)
        }

        // Activate finger licenses
//        try {
//            areFingerLicensesActivated = NeurotecLicenseHelper.obtainFingerLicenses(this@MatchApplication)
//            if (areFingerLicensesActivated) {
//                Log.d(TAG, " Fingerprint licenses activated successfully")
//            } else {
//                Log.d(TAG, "✓ Fingerprint licenses activated failed")
//            }
//        } catch (e: Exception) {
//            Log.d(TAG, "Error activating Fingerprint licenses")
//        }
    }

    override fun onTerminate() {
        super.onTerminate()

        NeurotecLicenseHelper.release()
    }


}