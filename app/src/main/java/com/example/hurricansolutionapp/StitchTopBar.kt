package com.example.hurricansolutionapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * TopBar estándar de la app para mantener cohesión visual.
 * Usa CenterAlignedTopAppBar con chevron_left.
 *
 * @param title Título de la pantalla
 * @param onBack Acción al presionar el botón de volver
 * @param isDarkMode Si está en modo oscuro
 * @param actions Composable opcional para acciones en el lado derecho
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StitchTopBar(
    title: String,
    onBack: () -> Unit,
    isDarkMode: Boolean,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val surface = if (isDarkMode) Color(0xFF000000) else Color.White
    val textPrimary = if (isDarkMode) Color.White else Color(0xFF111418)

    Column(
        modifier = Modifier
            .background(surface)
            .statusBarsPadding()
    ) {
        CenterAlignedTopAppBar(
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = surface
            ),
            title = {
                Text(
                    title.uppercase(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = textPrimary,
                    letterSpacing = 0.5.sp
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_chevron_left),
                        contentDescription = "Volver",
                        tint = textPrimary
                    )
                }
            },
            actions = actions
        )
    }
}

/**
 * TopBar con línea separadora inferior
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StitchTopBarWithDivider(
    title: String,
    onBack: () -> Unit,
    isDarkMode: Boolean,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val surface = if (isDarkMode) Color(0xFF000000) else Color.White
    val border = if (isDarkMode) Color(0xFF27272A) else Color(0xFFE5E7EB)
    val textPrimary = if (isDarkMode) Color.White else Color(0xFF111418)

    Column(modifier = Modifier.background(surface).statusBarsPadding()) {
        CenterAlignedTopAppBar(
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = surface
            ),
            title = {
                Text(
                    title.uppercase(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = textPrimary,
                    letterSpacing = 0.5.sp
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_chevron_left),
                        contentDescription = "Volver",
                        tint = textPrimary
                    )
                }
            },
            actions = actions
        )
        HorizontalDivider(color = border, thickness = 1.dp)
    }
}