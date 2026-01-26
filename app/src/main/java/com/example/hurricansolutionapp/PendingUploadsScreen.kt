package com.example.hurricansolutionapp

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

// COLORES PARA LOS ESTADOS

// PENDING - Gris neutro
private val PendingBgLight = Color(0xFFF3F4F6)
private val PendingBgDark = Color(0xFF27272A)
private val PendingTextLight = Color(0xFF374151)
private val PendingTextDark = Color(0xFFD1D5DB)

// DONE/SUBIDO - Verde
private val DoneBgLight = Color(0xFFDCFCE7)
private val DoneBgDark = Color(0xFF14532D).copy(alpha = 0.3f)
private val DoneTextLight = Color(0xFF15803D)
private val DoneTextDark = Color(0xFF4ADE80)

// ERROR/FALLO - Rojo
private val ErrorBgLight = Color(0xFFFEE2E2)
private val ErrorBgDark = Color(0xFF7F1D1D).copy(alpha = 0.3f)
private val ErrorTextLight = Color(0xFFDC2626)
private val ErrorTextDark = Color(0xFFF87171)

// UPLOADING - Azul
private val UploadingBgLight = Color(0xFFDBEAFE)
private val UploadingBgDark = Color(0xFF1E3A8A).copy(alpha = 0.3f)
private val UploadingTextLight = Color(0xFF2563EB)
private val UploadingTextDark = Color(0xFF60A5FA)

// NO_INTERNET - Amarillo
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

    // Cargar items cuando cambia refreshKey
    LaunchedEffect(refreshKey) {
        items = UploadQueueStorage.getAll(context)
    }

    // Actualización automática cada 2 segundos si hay items en proceso
    LaunchedEffect(items) {
        val hayEnProceso = items.any { it.status == "UPLOADING" || it.status == "PENDING" }
        if (hayEnProceso) {
            kotlinx.coroutines.delay(2000)
            refreshKey++
        }
    }

    fun refresh() { refreshKey++ }

    // Colores del tema
    val bg = if (isDarkMode) Color(0xFF000000) else Color(0xFFF3F4F6)
    val surface = if (isDarkMode) Color(0xFF111111) else Color.White
    val textPrimary = if (isDarkMode) Color.White else Color(0xFF111418)
    val textMuted = if (isDarkMode) Color(0xFF9CA3AF) else Color(0xFF6B7280)
    val border = if (isDarkMode) Color(0xFF27272A) else Color(0xFFE5E7EB)

    Scaffold(
        containerColor = bg,
        topBar = {
            StitchTopBar(
                title = "Pendientes Por Subir",
                onBack = onBack,
                isDarkMode = isDarkMode
            )
        }
    ) { innerPadding ->

        if (items.isEmpty()) {
            // Estado vacío
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.CloudUpload,
                        contentDescription = null,
                        tint = textMuted,
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        "No hay PDFs pendientes",
                        color = textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Los archivos pendientes de subir\naparecerán aquí",
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
                items(items, key = { it.id }) { item ->
                    PendingUploadCard(
                        item = item,
                        isDarkMode = isDarkMode,
                        isOnline = isOnline,
                        surface = surface,
                        border = border,
                        textPrimary = textPrimary,
                        textMuted = textMuted,
                        onUpload = {
                            scope.launch {
                                onRetryUpload(item)
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
private fun PendingUploadCard(
    item: PendingUpload,
    isDarkMode: Boolean,
    isOnline: Boolean,
    surface: Color,
    border: Color,
    textPrimary: Color,
    textMuted: Color,
    onUpload: () -> Unit,
    onRemove: () -> Unit
) {
    // Determinar el estado efectivo
    val effectiveStatus = when {
        !isOnline && item.status != "DONE" -> "NO_INTERNET"
        else -> item.status
    }

    // Colores según estado
    val (badgeBg, badgeText, statusLabel) = when (effectiveStatus) {
        "PENDING" -> Triple(
            if (isDarkMode) PendingBgDark else PendingBgLight,
            if (isDarkMode) PendingTextDark else PendingTextLight,
            "PENDING"
        )
        "UPLOADING" -> Triple(
            if (isDarkMode) UploadingBgDark else UploadingBgLight,
            if (isDarkMode) UploadingTextDark else UploadingTextLight,
            "SUBIENDO"
        )
        "DONE" -> Triple(
            if (isDarkMode) DoneBgDark else DoneBgLight,
            if (isDarkMode) DoneTextDark else DoneTextLight,
            "SUBIDO"
        )
        "ERROR" -> Triple(
            if (isDarkMode) ErrorBgDark else ErrorBgLight,
            if (isDarkMode) ErrorTextDark else ErrorTextLight,
            "FALLO"
        )
        "NO_INTERNET" -> Triple(
            if (isDarkMode) NoInternetBgDark else NoInternetBgLight,
            if (isDarkMode) NoInternetTextDark else NoInternetTextLight,
            "SIN INTERNET"
        )
        else -> Triple(
            if (isDarkMode) PendingBgDark else PendingBgLight,
            if (isDarkMode) PendingTextDark else PendingTextLight,
            effectiveStatus
        )
    }

    val isUploading = item.status == "UPLOADING"
    val isDone = item.status == "DONE"
    val canUpload = !isUploading && !isDone && isOnline

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
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Cliente * Folio + Badge
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Título principal: Nombre del cliente
                val tituloCliente = item.clienteNombre?.takeIf { it.isNotBlank() } ?: "Cliente"
                Text(
                    text = tituloCliente,
                    color = textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                // Subtítulo: Folio (extraído del cotizacionId si tiene formato correcto)
                val folio = item.cotizacionId.takeIf { it.contains("-") } ?: "Sin folio"
                Text(
                    text = "Folio: $folio",
                    color = textMuted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Estado:",
                        color = textMuted,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )

                    // Badge de estado
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(badgeBg)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = statusLabel,
                            color = badgeText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                // Mostrar error si existe
                if (!item.lastError.isNullOrBlank() && item.status == "ERROR") {
                    Text(
                        text = "Error: ${item.lastError}",
                        color = if (isDarkMode) ErrorTextDark else ErrorTextLight,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // Botones
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Botón Subir/Reintentar
                Button(
                    onClick = onUpload,
                    enabled = canUpload,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDarkMode) Color.White else Color.Black,
                        contentColor = if (isDarkMode) Color.Black else Color.White,
                        disabledContainerColor = if (isDarkMode) Color(0xFF27272A) else Color(0xFFF3F4F6),
                        disabledContentColor = if (isDarkMode) Color(0xFF52525B) else Color(0xFF9CA3AF)
                    )
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = if (isDarkMode) Color.Black else Color.White
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "SUBIENDO",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            letterSpacing = 1.sp
                        )
                    } else {
                        val buttonText = when {
                            isDone -> "SUBIDO"
                            item.status == "ERROR" -> "REINTENTAR"
                            else -> "SUBIR"
                        }
                        Text(
                            buttonText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            letterSpacing = 1.sp
                        )
                    }
                }

                // Botón Quitar
                OutlinedButton(
                    onClick = onRemove,
                    enabled = !isUploading,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isDarkMode) Color(0xFF3F3F46) else Color(0xFFD1D5DB)
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = textMuted,
                        disabledContentColor = textMuted.copy(alpha = 0.5f)
                    )
                ) {
                    Text(
                        "QUITAR",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}