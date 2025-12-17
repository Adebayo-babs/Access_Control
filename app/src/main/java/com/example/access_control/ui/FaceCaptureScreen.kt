package com.example.access_control.ui

import android.graphics.Bitmap
import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.access_control.R
import com.example.access_control.viewModel.CardReaderViewModel
import com.neurotec.lang.NCore
import kotlinx.coroutines.delay


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaceCaptureScreen(
    onBack: () -> Unit,
    viewModel: CardReaderViewModel,
    onPlayFaceDetectedSound: () -> Unit
) {

    BackHandler { onBack() }

    val context = LocalContext.current
    val status = viewModel.status
    val dialogState by viewModel.dialogState.collectAsState()


    LaunchedEffect(Unit) {
        NCore.setContext(context)
        viewModel.onFaceDetectedSound = {
            onPlayFaceDetectedSound()
        }
        viewModel.initialize()
    }

    // Handle dialog auto-dismiss and navigation
    LaunchedEffect(dialogState.showDialog) {
        if (dialogState.showDialog && dialogState.message.contains(
                "Face Detected",
                ignoreCase = true
            )
        ) {
            delay(5000) // Show for 5 seconds
            viewModel.hideDialog()
            viewModel.reset()
            onBack() // Navigate back to main menu
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopCapture()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Face Verification") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1C1C1C),
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Camera preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color(0xFF1C1C1C))
                ) {
                    if (viewModel.useNeurotecCamera) {
                        CameraPreviewColoured(
                            modifier = Modifier.fillMaxSize(),
                            viewModel = viewModel
                        )
                    } else {
                        CameraPreviewGrayScale(
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Camera switch button
                        FloatingActionButton(
                            onClick = {
                                Log.d("FaceCaptureScreen", "CAMERA SWITCH BUTTON CLICKED")
                                viewModel.toggleCameraPreview()

//                                Toast.makeText(
//                                    context,
//                                    if (viewModel.useNeurotecCamera) "Switched to Colored" else "Switched to Grayscale",
//                                    Toast.LENGTH_SHORT
//                                ).show()
                            },
                            containerColor = Color(0xFF424242),
                            contentColor = Color.White,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.cameraswitch),
                                contentDescription = "Switch camera",
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Status message
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            status.contains("error", ignoreCase = true) -> Color(0xFFD32F2F)
                            status.contains("success", ignoreCase = true) ||
                                    status.contains("captured", ignoreCase = true) -> Color(
                                0xFF4CAF50
                            )

                            status.contains("matching", ignoreCase = true) ||
                                    status.contains("processing", ignoreCase = true) ||
                                    status.contains("detecting", ignoreCase = true) -> Color(
                                0xFF2196F3
                            )

                            else -> Color(0xFF2C2C2C)
                        }
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (status.contains("matching", ignoreCase = true) ||
                            status.contains("processing", ignoreCase = true) ||
                            status.contains("detecting", ignoreCase = true)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                        }

                        Text(
                            text = status.ifEmpty { "Initializing camera..." },
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
            }

            // Face Detected Dialog
            if (dialogState.showDialog) {
                FaceDetectedDialog(
                    message = dialogState.message,
                    faceBitmap = dialogState.capturedFace
                )
            }
        }
    }
}

@Composable
fun FaceDetectedDialog(
    message: String,
    faceBitmap: Bitmap?
) {
    Dialog(
        onDismissRequest = { /* Don't allow dismissal */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1C1C1C)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Success icon or checkmark
                Icon(
                    painter = painterResource(id = android.R.drawable.ic_menu_camera),
                    contentDescription = "Success",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(48.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = message,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Display captured face
                faceBitmap?.let { bitmap ->
                    Card(
                        modifier = Modifier
                            .size(200.dp),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Captured Face",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Progress indicator
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    color = Color(0xFF4CAF50),
                    strokeWidth = 3.dp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Returning to main menu...",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}