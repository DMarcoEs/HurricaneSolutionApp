package com.example.hurricansolutionapp

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

// ═══════════════════════════════════════════════════════════════════════════════
// COLORES PARA LOS ESTADOS
// ═══════════════════════════════════════════════════════════════════════════════

private val PendingBgLight = Color(0xFFF3F4F6)
private val PendingBgDark = Color(0xFF27272A)
private val PendingTextLight = Color(0xFF374151)
private val PendingTextDark = Color(0xFFD1D5DB)

private val DoneBgLight = Color(0xFFDCFCE7)
private val DoneBgDark = Color(0xFF14532D).copy(alpha = 0.3f)
private val DoneTextLight = Color(0xFF15803D)
private val DoneTextDark = Color(0xFF4ADE80)

private val ErrorBgLight = Color(0xFFFEE2E2)
private val ErrorBgDark = Color(0xFF7F1D1D).copy(alpha = 0.3f)
private val ErrorTextLight = Color(0xFFDC2626)
private val ErrorTextDark = Color(0xFFF87171)

private val UploadingBgLight = Color(0xFFDBEAFE)
private val UploadingBgDark = Color(0xFF1E3A8A).copy(alpha = 0.3f)
private val UploadingTextLight = Color(0xFF2563EB)
private val UploadingTextDark = Color(0xFF60A5FA)

private val NoInternetBgLight = Color(0xFFFEF9C3)
private val NoInternetBgDark = Color(0xFF713F12).copy(alpha = 0.3f)
private val NoInternetTextLight = Color(0xFFA16207)
private val NoInternetTextDark = Color(0xFFFACC15)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingUploadsScreen(
    isDarkMode: Boolean = false,
    isOnline: Boolean = true,
    onBack: () -> Unit,
    onRetryUpload: suspend (PendingUpload) -> Unit,
    onRemove: (String) -> Unit = {}
) {
    BackHandler { onBack() }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var refreshKey by remember { mutableIntStateOf(0) }
    var items by remember { mutableStateOf(emptyList<PendingUpload>()) }
    var isGoogleAuthenticated by remember { mutableStateOf(DriveAuthManager.isAuthenticated(context)) }

    // Cargar items
    LaunchedEffect(refreshKey) {
        items = UploadQueueStorage.getAll(context)
        isGoogleAuthenticated = DriveAuthManager.isAuthenticated(context)
    }

    // Auto-refresh si hay items en proceso
    LaunchedEffect(items) {
        val hayEnProceso = items.any { it.status == "UPLOADING" || it.status == "PENDING" }
        if (hayEnProceso) {
            kotlinx.coroutines.delay(2000)
            refreshKey++
        }
    }

    fun refresh() { refreshKey++ }

    val bg = if (isDarkMode) Color(0xFF000000) else Color(0xFFF3F4F6)
    val surface = if (isDarkMode) Color(0xFF111111) else Color.White
    val textPrimary = if (isDarkMode) Color.White else Color(0xFF111418)
    val textMuted = if (isDarkMode) Color(0xFF9CA3AF) else Color(0xFF6B7280)
    val border = if (isDarkMode) Color(0xFF27272A) else Color(0xFFE5E7EB)

    Scaffold(
        containerColor = bg,
        topBar = {
            StitchTopBar(
                title = "Proyectos Por Registrar",
                onBack = onBack,
                isDarkMode = isDarkMode
            )
        }
    ) { innerPadding ->

        if (items.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.CloudDone,
                        contentDescription = null,
                        tint = Color(0xFF34A853),
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        "Todo sincronizado",
                        color = textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "No hay cotizaciones pendientes\nde subir a la nube",
                        color = textMuted,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                // Banner de Google Drive
                item {
                    GoogleDriveStatusBanner(
                        isAuthenticated = isGoogleAuthenticated,
                        isDarkMode = isDarkMode
                    )
                }

                items(items, key = { it.id }) { item ->
                    UnifiedPendingCard(
                        item = item,
                        isDarkMode = isDarkMode,
                        isOnline = isOnline,
                        isGoogleAuthenticated = isGoogleAuthenticated,
                        surface = surface,
                        border = border,
                        textPrimary = textPrimary,
                        textMuted = textMuted,
                        onUploadSupabase = {
                            scope.launch {
                                onRetryUpload(item)
                                refresh()
                            }
                        },
                        onUploadDrive = {
                            scope.launch {
                                val pdfFile = java.io.File(item.filePath)
                                if (pdfFile.exists()) {
                                    val userName = SessionManager.getNombre(context)
                                    val userRole = SessionManager.getRole(context)
                                    DriveUploadManager.uploadPdfToDriveAuto(
                                        context = context,
                                        pdfFile = pdfFile,
                                        userName = userName,
                                        userRole = userRole,
                                        folio = item.cotizacionId
                                    )
                                }
                                refresh()
                            }
                        },
                        onRemove = {
                            onRemove(item.id)
                            refresh()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun GoogleDriveStatusBanner(isAuthenticated: Boolean, isDarkMode: Boolean) {
    val bgColor = if (isAuthenticated) {
        if (isDarkMode) Color(0xFF14532D).copy(alpha = 0.3f) else Color(0xFFDCFCE7)
    } else {
        if (isDarkMode) Color(0xFF7F1D1D).copy(alpha = 0.3f) else Color(0xFFFEF3C7)
    }
    val textColor = if (isAuthenticated) {
        if (isDarkMode) Color(0xFF4ADE80) else Color(0xFF15803D)
    } else {
        if (isDarkMode) Color(0xFFFACC15) else Color(0xFFB45309)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = bgColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_google_drive),
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isAuthenticated) "Google Drive conectado" else "Google Drive no conectado",
                    color = textColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (!isAuthenticated) {
                    Text(
                        text = "Inicia sesión desde el botón en la pantalla principal",
                        color = textColor.copy(alpha = 0.8f),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun UnifiedPendingCard(
    item: PendingUpload,
    isDarkMode: Boolean,
    isOnline: Boolean,
    isGoogleAuthenticated: Boolean,
    surface: Color,
    border: Color,
    textPrimary: Color,
    textMuted: Color,
    onUploadSupabase: () -> Unit,
    onUploadDrive: () -> Unit,
    onRemove: () -> Unit
) {
    // Determinar estados
    val supabaseStatus = when {
        !isOnline && item.status != "DONE" -> "NO_INTERNET"
        else -> item.status
    }
    val driveStatus = when {
        !isGoogleAuthenticated -> "NOT_AUTH"
        item.driveStatus == "DONE" -> "DONE"
        item.driveStatus == "ERROR" -> "ERROR"
        item.driveStatus == "UPLOADING" -> "UPLOADING"
        else -> "PENDING"
    }

    val isSupabaseUploading = item.status == "UPLOADING"
    val isDriveUploading = driveStatus == "UPLOADING"
    val isSupabaseDone = item.status == "DONE"
    val isDriveDone = driveStatus == "DONE"

    val canUploadSupabase = !isSupabaseUploading && !isSupabaseDone && isOnline
    val canUploadDrive = !isDriveUploading && !isDriveDone && isOnline && isGoogleAuthenticated

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = surface,
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, border.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = item.clienteNombre ?: "Cliente",
                    color = textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Folio: ${item.cotizacionId.takeIf { it.contains("-") } ?: "Sin folio"}",
                    color = textMuted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Estados
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusRow("Supabase", supabaseStatus, item.lastError, isDarkMode, textMuted)
                StatusRow("Google Drive", driveStatus, item.driveError, isDarkMode, textMuted)
            }

            // Botones
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Botón Supabase
                    Button(
                        onClick = onUploadSupabase,
                        enabled = canUploadSupabase,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDarkMode) Color.White else Color.Black,
                            contentColor = if (isDarkMode) Color.Black else Color.White,
                            disabledContainerColor = if (isDarkMode) Color(0xFF27272A) else Color(0xFFF3F4F6),
                            disabledContentColor = if (isDarkMode) Color(0xFF52525B) else Color(0xFF9CA3AF)
                        )
                    ) {
                        if (isSupabaseUploading) {
                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text(
                                when {
                                    isSupabaseDone -> "✓ SUBIDO"
                                    item.status == "ERROR" -> "REINTENTAR"
                                    else -> "SUBIR"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Botón Drive
                    Button(
                        onClick = onUploadDrive,
                        enabled = canUploadDrive,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4285F4),
                            contentColor = Color.White,
                            disabledContainerColor = if (isDarkMode) Color(0xFF27272A) else Color(0xFFF3F4F6),
                            disabledContentColor = if (isDarkMode) Color(0xFF52525B) else Color(0xFF9CA3AF)
                        )
                    ) {
                        if (isDriveUploading) {
                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                        } else {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_google_drive),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                when {
                                    isDriveDone -> "✓"
                                    !isGoogleAuthenticated -> "NO AUTH"
                                    driveStatus == "ERROR" -> "REINTENTAR"
                                    else -> "DRIVE"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Botón Quitar
                OutlinedButton(
                    onClick = onRemove,
                    enabled = !isSupabaseUploading && !isDriveUploading,
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) Color(0xFF3F3F46) else Color(0xFFD1D5DB))
                ) {
                    Text("QUITAR DE LA LISTA", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = textMuted)
                }
            }
        }
    }
}

@Composable
private fun StatusRow(label: String, status: String, error: String?, isDarkMode: Boolean, textMuted: Color) {
    val (badgeBg, badgeText, statusLabel) = when (status) {
        "PENDING" -> Triple(if (isDarkMode) PendingBgDark else PendingBgLight, if (isDarkMode) PendingTextDark else PendingTextLight, "PENDIENTE")
        "UPLOADING" -> Triple(if (isDarkMode) UploadingBgDark else UploadingBgLight, if (isDarkMode) UploadingTextDark else UploadingTextLight, "SUBIENDO")
        "DONE" -> Triple(if (isDarkMode) DoneBgDark else DoneBgLight, if (isDarkMode) DoneTextDark else DoneTextLight, "SUBIDO")
        "ERROR" -> Triple(if (isDarkMode) ErrorBgDark else ErrorBgLight, if (isDarkMode) ErrorTextDark else ErrorTextLight, "ERROR")
        "NOT_AUTH" -> Triple(if (isDarkMode) NoInternetBgDark else NoInternetBgLight, if (isDarkMode) NoInternetTextDark else NoInternetTextLight, "SIN SESIÓN")
        "NO_INTERNET" -> Triple(if (isDarkMode) NoInternetBgDark else NoInternetBgLight, if (isDarkMode) NoInternetTextDark else NoInternetTextLight, "SIN INTERNET")
        else -> Triple(if (isDarkMode) PendingBgDark else PendingBgLight, if (isDarkMode) PendingTextDark else PendingTextLight, status)
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "$label:", color = textMuted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Box(
                modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(badgeBg).padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(text = statusLabel, color = badgeText, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            }
        }
        if (!error.isNullOrBlank() && status == "ERROR") {
            Text(text = "Error: $error", color = if (isDarkMode) ErrorTextDark else ErrorTextLight, fontSize = 11.sp, modifier = Modifier.padding(start = 8.dp, top = 4.dp))
        }
    }
}