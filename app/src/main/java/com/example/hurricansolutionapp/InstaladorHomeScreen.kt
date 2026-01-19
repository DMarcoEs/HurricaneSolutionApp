package com.example.hurricansolutionapp

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Home Screen del Instalador - Diseño Stitch
 * Homogéneo con el resto de la app (negro/blanco, sin verde)
 */
@Composable
fun InstaladorHomeScreen(
    instaladorName: String,
    pendingDriveCount: Int = 0,
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    onVerMedidas: () -> Unit,
    onPendientesDrive: () -> Unit,
    logoutEnabled: Boolean = true,
    onCerrarSesion: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Colores Stitch (igual que HomeScreen del especialista)
    val bg = if (isDarkMode) Color(0xFF000000) else Color(0xFFF3F4F6)
    val surface = if (isDarkMode) Color(0xFF111111) else Color.White
    val textPrimary = if (isDarkMode) Color.White else Color(0xFF111418)
    val textMuted = if (isDarkMode) Color(0xFF9CA3AF) else Color(0xFF6B7280)
    val border = if (isDarkMode) Color(0xFF27272A) else Color(0xFFE5E7EB)

    var pendingCount by remember { mutableIntStateOf(pendingDriveCount) }

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                pendingCount = InstaladorRepository.countAllPending()
            } catch (_: Exception) {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Spacer(Modifier.height(12.dp))

        // ═══════════════════════════════════════════════════════════════
        // TOP BAR - Logo + Badge INSTALADOR + Botón tema
        // ═══════════════════════════════════════════════════════════════
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo_header_new),
                contentDescription = "Hurricane Solution",
                modifier = Modifier.height(36.dp)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Badge INSTALADOR (negro/blanco)
                Surface(
                    color = if (isDarkMode) Color.White else Color.Black,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        "INSTALADOR",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        color = if (isDarkMode) Color.Black else Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                // Botón tema
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = surface,
                    border = BorderStroke(1.dp, border)
                ) {
                    IconButton(onClick = onToggleDarkMode) {
                        Icon(
                            painter = painterResource(id = if (isDarkMode) R.drawable.ic_sun else R.drawable.ic_moon),
                            contentDescription = "Cambiar tema",
                            tint = textPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Fecha dinámica
        Text(
            text = getInstaladorSpanishDate(),
            color = textMuted,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )

        // Bienvenida
        val firstName = instaladorName.split(" ").firstOrNull() ?: "Instalador"
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = textPrimary)) {
                    append("Bienvenido, ")
                }
                withStyle(SpanStyle(color = textMuted)) {
                    append(firstName)
                }
            },
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 34.sp
        )

        Spacer(Modifier.height(24.dp))

        // ═══════════════════════════════════════════════════════════════
        // MENÚ
        // ═══════════════════════════════════════════════════════════════
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(16.dp)
                    .background(
                        if (isDarkMode) Color.White else Color.Black,
                        RoundedCornerShape(2.dp)
                    )
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "MENÚ",
                color = textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
        }

        Spacer(Modifier.height(16.dp))

        // Card principal - Medidas Asignadas
        val cardBg = if (isDarkMode) Color.White else Color.Black
        val cardText = if (isDarkMode) Color.Black else Color.White

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
            color = cardBg,
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 4.dp,
            onClick = onVerMedidas
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        color = cardText.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = Icons.Default.Straighten,
                                contentDescription = null,
                                tint = cardText,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Text(
                        "MEDIDAS ASIGNADAS",
                        color = cardText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = cardText,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Google Drive
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = surface,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, border.copy(alpha = 0.5f)),
            onClick = onPendientesDrive
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(44.dp),
                        color = if (isDarkMode) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = null,
                                tint = textPrimary.copy(alpha = 0.8f),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            "Pendientes Google Drive",
                            color = textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "PDFs sin subir a Drive",
                            color = textMuted,
                            fontSize = 12.sp
                        )
                    }
                }

                if (pendingCount > 0) {
                    Surface(color = Color(0xFFC11007), shape = CircleShape) {
                        Text(
                            text = if (pendingCount > 99) "99+" else pendingCount.toString(),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = textMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // ═══════════════════════════════════════════════════════════════
        // CERRAR SESIÓN
        // ═══════════════════════════════════════════════════════════════
        val redColor = Color(0xFFC11007)

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = if (isDarkMode) redColor.copy(alpha = 0.1f) else redColor.copy(alpha = 0.05f),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, redColor.copy(alpha = 0.2f)),
            onClick = { if (logoutEnabled) onCerrarSesion() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    color = redColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            painter = painterResource(R.drawable.ic_logout_lucide),
                            contentDescription = null,
                            tint = redColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Column {
                    Text(
                        "Cerrar Sesión",
                        color = redColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Salir de la cuenta",
                        color = redColor.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

private fun getInstaladorSpanishDate(): String {
    val locale = Locale("es", "MX")
    val calendar = Calendar.getInstance()
    val dayOfWeek = SimpleDateFormat("EEEE", locale).format(calendar.time)
        .replaceFirstChar { it.uppercase() }
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    val month = SimpleDateFormat("MMMM", locale).format(calendar.time)
    return "$dayOfWeek, $day de $month"
}