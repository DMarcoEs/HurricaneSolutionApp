package com.example.hurricansolutionapp

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
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

    val bg = if (isDarkMode) Color(0xFF000000) else Color(0xFFF6F7F8)
    val surface = if (isDarkMode) Color(0xFF0A0A0A) else Color.White
    val textPrimary = if (isDarkMode) Color.White else Color(0xFF111418)
    val textMuted = if (isDarkMode) Color(0xFF9CA3AF) else Color(0xFF6B7280)
    val cardBg = if (isDarkMode) Color(0xFF111111) else Color.White
    val border = if (isDarkMode) Color(0xFF27272A) else Color(0xFFE5E7EB)
    val installerAccent = Color(0xFF10B981)

    var pendingCount by remember { mutableIntStateOf(pendingDriveCount) }

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                pendingCount = InstaladorRepository.countAllPending()
            } catch (_: Exception) {
            }
        }
    }

    Scaffold(
        containerColor = bg,
        topBar = {
            Column(modifier = Modifier
                .background(surface)
                .statusBarsPadding()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo_header_new),
                        contentDescription = "Logo",
                        modifier = Modifier.height(28.dp)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = installerAccent.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                "INSTALADOR",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = installerAccent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                        IconButton(onClick = onToggleDarkMode) {
                            Icon(
                                painter = painterResource(id = if (isDarkMode) R.drawable.ic_sun else R.drawable.ic_moon),
                                contentDescription = "Tema",
                                tint = textMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(onClick = onCerrarSesion, enabled = logoutEnabled) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = "Salir",
                                tint = if (logoutEnabled) textMuted else textMuted.copy(alpha = 0.3f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                HorizontalDivider(color = border, thickness = 0.5.dp)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Hola, ${instaladorName.split(" ").firstOrNull() ?: "Instalador"}",
                    color = textPrimary,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
                Text("Panel de Instalador", color = textMuted, fontSize = 14.sp)
            }

            Text(
                "ACCIONES",
                color = textMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onVerMedidas),
                color = cardBg, shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, border.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            color = if (isDarkMode) Color.White.copy(alpha = 0.1f) else Color.Black.copy(
                                alpha = 0.05f
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Straighten,
                                    contentDescription = null,
                                    tint = textPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                "Medidas Asignadas",
                                color = textPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text("Ver y rectificar medidas", color = textMuted, fontSize = 12.sp)
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = textMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onPendientesDrive),
                color = cardBg, shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, border.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            color = if (isDarkMode) Color.White.copy(alpha = 0.1f) else Color.Black.copy(
                                alpha = 0.05f
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(
                                        id = R.drawable.ic_google_drive
                                    ),
                                    contentDescription = null,
                                    tint = textPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                "Google Drive",
                                color = textPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text("Subir PDFs de instalación", color = textMuted, fontSize = 12.sp)
                        }
                    }
                    if (pendingCount > 0) {
                        Surface(color = Color(0xFFEF4444), shape = CircleShape) {
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

            Spacer(modifier = Modifier.weight(1f))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = cardBg,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, border.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Sesión activa", color = textMuted, fontSize = 11.sp)
                        Text(
                            instaladorName,
                            color = textPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(installerAccent)
                    )
                }
            }
        }
    }
}