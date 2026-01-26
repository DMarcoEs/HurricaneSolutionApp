package com.example.hurricansolutionapp

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingDriveUploadsScreen(
    isDarkMode: Boolean = false,
    isOnline: Boolean = true,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var refreshKey by remember { mutableIntStateOf(0) }
    var items by remember { mutableStateOf(emptyList<DrivePendingUpload>()) }
    var isLoading by remember { mutableStateOf(false) }

    // Cargar items cuando cambia refreshKey
    LaunchedEffect(refreshKey) {
        isLoading = true
        items = DriveUploadManager.getPendingDriveUploads(context)
        isLoading = false
    }

    fun refresh() { refreshKey++ }

    // Colores del tema
    val bg = if (isDarkMode) Color(0xFF000000) else Color(0xFFF3F4F6)
    val surface = if (isDarkMode) Color(0xFF111111) else Color.White
    val textPrimary = if (isDarkMode) Color.White else Color(0xFF111418)
    val textMuted = if (isDarkMode) Color(0xFF9CA3AF) else Color(0xFF6B7280)
    val border = if (isDarkMode) Color(0xFF27272A) else Color(0xFFE5E7EB)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "PENDIENTES GOOGLE DRIVE",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualizar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = surface,
                    titleContentColor = textPrimary,
                    navigationIconContentColor = textPrimary,
                    actionIconContentColor = textPrimary
                )
            )
        },
        containerColor = bg
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                items.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_google_drive),
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = textMuted.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No hay archivos pendientes",
                            color = textMuted,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Todos los PDFs están en Google Drive",
                            color = textMuted.copy(alpha = 0.7f),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(items) { item ->
                            DrivePendingCard(
                                item = item,
                                isDarkMode = isDarkMode,
                                isOnline = isOnline,
                                surface = surface,
                                border = border,
                                textPrimary = textPrimary,
                                textMuted = textMuted,
                                onUpload = {
                                    scope.launch {
                                        val success = DriveUploadManager.retryDriveUpload(context, item)
                                        refresh()
                                    }
                                },
                                onRemove = {
                                    scope.launch {
                                        DriveUploadManager.removePendingDriveUpload(item.id)
                                        refresh()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DrivePendingCard(
    item: DrivePendingUpload,
    isDarkMode: Boolean,
    isOnline: Boolean,
    surface: Color,
    border: Color,
    textPrimary: Color,
    textMuted: Color,
    onUpload: () -> Unit,
    onRemove: () -> Unit
) {
    val canUpload = isOnline

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = surface,
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, border.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Nombre del archivo
            Text(
                text = item.pdfFilename,
                color = textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            // Folio
            Text(
                text = "Folio: ${item.folio}",
                color = textMuted,
                fontSize = 12.sp
            )

            // Usuario
            Text(
                text = "Usuario: ${item.userName}",
                color = textMuted,
                fontSize = 12.sp
            )

            // Ruta destino
            Text(
                text = " ${item.targetFolderPath}",
                color = textMuted.copy(alpha = 0.7f),
                fontSize = 11.sp
            )

            // Error si existe
            item.lastError?.let { error ->
                Surface(
                    color = Color(0xFFFEE2E2).copy(alpha = if (isDarkMode) 0.3f else 1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = " $error",
                        color = if (isDarkMode) Color(0xFFF87171) else Color(0xFFDC2626),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            // Reintentos
            if (item.retryCount > 0) {
                Text(
                    text = "Reintentos: ${item.retryCount}",
                    color = textMuted.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            }

            // Botones
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Botón Subir a Drive
                Button(
                    onClick = onUpload,
                    modifier = Modifier.weight(1f),
                    enabled = canUpload,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4285F4)
                    )
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_google_drive),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Subir a Drive", fontSize = 12.sp, color = Color.White)
                }

                // Botón Eliminar
                OutlinedButton(
                    onClick = onRemove,
                    modifier = Modifier.width(48.dp)
                ) {
                    Text("×", fontSize = 20.sp)
                }
            }
        }
    }
}