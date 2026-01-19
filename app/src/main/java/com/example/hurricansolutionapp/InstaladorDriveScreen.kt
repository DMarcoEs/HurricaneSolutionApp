package com.example.hurricansolutionapp

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstaladorDriveScreen(isDarkMode: Boolean, isOnline: Boolean = true, onBack: () -> Unit) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(true) }
    var pendingList by remember { mutableStateOf<List<InstaladorPendingUpload>>(emptyList()) }
    var isAuthenticated by remember { mutableStateOf(false) }
    var uploadingId by remember { mutableStateOf<String?>(null) }
    var refreshKey by remember { mutableIntStateOf(0) }

    val bg = if (isDarkMode) Color(0xFF000000) else Color(0xFFF3F4F6)
    val cardBg = if (isDarkMode) Color(0xFF111111) else Color.White
    val textPrimary = if (isDarkMode) Color.White else Color(0xFF111418)
    val textMuted = if (isDarkMode) Color(0xFF9CA3AF) else Color(0xFF6B7280)
    val border = if (isDarkMode) Color(0xFF27272A) else Color(0xFFE5E7EB)
    val userName = remember { SessionManager.getNombre(context) }
    val userRole = remember { SessionManager.getRole(context) }

    LaunchedEffect(refreshKey) {
        isAuthenticated = DriveAuthManager.isAuthenticated(context)
        isLoading = true
        try {
            val result = InstaladorRepository.getAllPending(); if (result.isSuccess) pendingList =
                result.getOrNull() ?: emptyList()
        } catch (_: Exception) {
        }
        isLoading = false
    }

    fun refresh() {
        refreshKey++
    }

    fun uploadFile(p: InstaladorPendingUpload) {
        scope.launch {
            uploadingId = p.id
            try {
                val file = File(p.filePath)
                if (!file.exists()) {
                    Toast.makeText(context, "Archivo no encontrado", Toast.LENGTH_SHORT)
                        .show(); InstaladorRepository.markPendingError(
                        p.id,
                        "Archivo no encontrado"
                    ); uploadingId = null; refresh(); return@launch
                }
                InstaladorRepository.updatePendingStatus(p.id, InstaladorUploadStatus.UPLOADING)
                val result = GoogleDriveRepository.uploadPdfToStructuredFolder(
                    context,
                    file,
                    userName,
                    userRole
                )
                if (result.isSuccess && result.getOrNull()?.success == true) {
                    val ur = result.getOrNull()!!
                    InstaladorRepository.markPendingDone(p.id, ur.fileId ?: "", ur.folderPath)
                    Toast.makeText(context, "[OK] Subido", Toast.LENGTH_SHORT).show()
                } else {
                    InstaladorRepository.markPendingError(
                        p.id,
                        result.getOrNull()?.error ?: result.exceptionOrNull()?.message ?: "Error"
                    )
                    Toast.makeText(context, "Error al subir", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                InstaladorRepository.markPendingError(p.id, e.message ?: "Error")
            } finally {
                uploadingId = null; refresh()
            }
        }
    }

    Scaffold(
        containerColor = bg,
        topBar = {
            StitchTopBarWithDivider(
                title = "Google Drive",
                onBack = onBack,
                isDarkMode = isDarkMode,
                actions = {
                    IconButton(onClick = { refresh() }) {
                        Icon(
                            Icons.Default.Refresh,
                            null,
                            tint = textPrimary
                        )
                    }
                })
        }) { innerPadding ->
        Box(Modifier
            .fillMaxSize()
            .padding(innerPadding)) {
            when {
                !isAuthenticated -> Column(
                    Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_google_drive),
                        null,
                        tint = textMuted.copy(0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "No conectado",
                        color = textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Inicia sesión para subir archivos",
                        color = textMuted,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }

                isLoading -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator(color = textPrimary, strokeWidth = 2.dp) }

                pendingList.isEmpty() -> Column(
                    Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Outlined.CloudDone,
                        null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Todo al día",
                        color = textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text("No hay PDFs pendientes", color = textMuted, fontSize = 14.sp)
                }

                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            "${pendingList.count { it.status == InstaladorUploadStatus.PENDING }} pendiente(s)",
                            color = textMuted,
                            fontSize = 12.sp
                        )
                    }
                    items(pendingList, key = { it.id }) { p ->
                        val sc = when (p.status) {
                            InstaladorUploadStatus.PENDING -> Color(0xFFF59E0B); InstaladorUploadStatus.UPLOADING -> Color(
                                0xFF3B82F6
                            ); InstaladorUploadStatus.DONE -> Color(0xFF10B981); else -> Color(
                                0xFFEF4444
                            )
                        }
                        Surface(
                            Modifier.fillMaxWidth(),
                            color = cardBg,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, border.copy(0.5f))
                        ) {
                            Column(
                                Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Icon(
                                            Icons.Default.PictureAsPdf,
                                            null,
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Column {
                                            Text(
                                                p.fileName,
                                                color = textPrimary,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            ); Text(p.folio, color = textMuted, fontSize = 11.sp)
                                        }
                                    }
                                    Surface(
                                        color = sc.copy(0.15f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            p.status,
                                            Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            color = sc,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Text(
                                    p.clienteNombre,
                                    color = textMuted,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val err = p.getErrorMessageSeguro()
                                if (p.status == InstaladorUploadStatus.ERROR && err.isNotBlank()) Text(
                                    "âš ï¸ $err",
                                    color = Color(0xFFEF4444),
                                    fontSize = 11.sp
                                )
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    when (p.status) {
                                        InstaladorUploadStatus.PENDING -> if (uploadingId == p.id) CircularProgressIndicator(
                                            Modifier.size(24.dp),
                                            textPrimary,
                                            2.dp
                                        ) else Button(
                                            { uploadFile(p) },
                                            enabled = isOnline && isAuthenticated,
                                            colors = ButtonDefaults.buttonColors(Color(0xFF4285F4)),
                                            shape = RoundedCornerShape(6.dp)
                                        ) { Text("Subir", color = Color.White, fontSize = 12.sp) }

                                        InstaladorUploadStatus.ERROR -> Row {
                                            OutlinedButton({
                                                scope.launch {
                                                    InstaladorRepository.deletePending(
                                                        p.id
                                                    ); refresh()
                                                }
                                            }, shape = RoundedCornerShape(6.dp)) {
                                                Text(
                                                    "Eliminar",
                                                    color = Color(0xFFEF4444),
                                                    fontSize = 12.sp
                                                )
                                            }; Spacer(Modifier.width(8.dp)); Button(
                                            { uploadFile(p) },
                                            colors = ButtonDefaults.buttonColors(Color(0xFF4285F4)),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                "Reintentar",
                                                color = Color.White,
                                                fontSize = 12.sp
                                            )
                                        }
                                        }

                                        InstaladorUploadStatus.DONE -> Icon(
                                            Icons.Default.CheckCircle,
                                            null,
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(24.dp)
                                        )

                                        else -> {}
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}