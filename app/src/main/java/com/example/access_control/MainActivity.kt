package com.example.access_control

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.ToneGenerator
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.access_control.ui.FaceCaptureScreen
import com.example.access_control.ui.FingerCaptureScreen
import com.example.access_control.ui.MainMenuScreen
import com.example.access_control.ui.SplashScreen
import com.example.access_control.ui.theme.Access_ControlTheme
import com.example.access_control.viewModel.CardReaderViewModel

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private var nfcAdapter: NfcAdapter? = null
    private var pendingIntent: PendingIntent? = null
    private var intentFiltersArray: Array<IntentFilter>? = null
    private var techListsArray: Array<Array<String>>? = null

    private val viewModel: CardReaderViewModel by viewModels()

    // Navigation States
    private enum class Screen {
        SPLASH,
        MAIN_MENU,
        FACE_CAPTURE,
        FINGERPRINT_CAPTURE
    }

    private var toneGenerator: ToneGenerator? = null

    // Navigation state
    private var currentScreen by mutableStateOf(Screen.SPLASH)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 1000)

        Log.d(TAG, "Checking licenses...")
        Log.d(TAG, "Face Licenses activated: ${MatchApplication.areFaceLicensesActivated}")
        Log.d(TAG, "Finger Licenses activated: ${MatchApplication.areFingerLicensesActivated}")


        setupNFC()

        enableEdgeToEdge()
        setContent {
            Access_ControlTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when(currentScreen) {
                        Screen.SPLASH -> {
                            SplashScreen (
                                onNavigateToMain = {
                                    currentScreen = Screen.MAIN_MENU
                                }
                            )
                        }

                        Screen.MAIN_MENU -> {
                            MainMenuScreen(
                                viewModel = viewModel,
                                onNavigateToFaceCapture = {
                                    currentScreen = Screen.FACE_CAPTURE
                                }
                            )
                        }

                        Screen.FACE_CAPTURE -> {
                            FaceCaptureScreen(
                                    viewModel = viewModel,
                                    onBack = {
                                        currentScreen = Screen.MAIN_MENU
                                    },
                                onPlayFaceDetectedSound = {
                                    val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 1000)
                                    toneGen.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 1000)
                                },
                                onSwitchToFingerprint = {
                                    currentScreen = Screen.FINGERPRINT_CAPTURE
                                }
                                )
                            }

                        Screen.FINGERPRINT_CAPTURE -> {
                            FingerCaptureScreen(
                                onBack = {
                                    currentScreen = Screen.MAIN_MENU
                                },
                                onSwitchToFace = {
                                    currentScreen = Screen.FACE_CAPTURE
                                }
                            )
                        }
                    }
                }
            }
        }
        // Handle NFC intent
        handleIntent(intent)
    }

    private fun setupNFC() {
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        // PendingIntent object so the Android system can populate it with the details of the tag when it is scanned
        pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_MUTABLE
        )

        // Setup intent filters for NFC
        val ndef = IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED)
        val tech = IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED)
        val tag = IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)
        intentFiltersArray = arrayOf(ndef, tech, tag)

        // Setup technology filters
        techListsArray = arrayOf(
            arrayOf(IsoDep::class.java.name)
        )
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, intentFiltersArray, techListsArray)
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val action = intent.action
        if (NfcAdapter.ACTION_TAG_DISCOVERED == action ||
            NfcAdapter.ACTION_TECH_DISCOVERED == action ||
            NfcAdapter.ACTION_NDEF_DISCOVERED == action
        ) {
            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
            tag?.let { processNFCTag(it) }
        }
    }

    private fun processNFCTag(tag: Tag) {
        Log.d(TAG, "Card tapped!")

        playSuccessSound()

        // Navigate to face capture screen
        currentScreen = Screen.FACE_CAPTURE

        // Show dialog immediately
//        viewModel.showCardTappedDialog()

        // Hide dialog after 2 seconds
//        lifecycleScope.launch {
//            delay(2000)
//            viewModel.hideDialog()
//        }

    }

    private fun playSuccessSound() {
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Release the tone generator when activity is destroyed
        toneGenerator?.release()
        toneGenerator = null
    }

}

