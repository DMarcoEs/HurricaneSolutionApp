package com.example.hurricansolutionapp

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import kotlinx.coroutines.launch



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingUploadsScreen(
    onBack: () -> Unit,
    onRetryUpload: suspend (PendingUpload) -> Unit,
    onRemove: (String) -> Unit = {}
) {
    BackHandler { onBack() }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var refreshKey by remember { mutableIntStateOf(0) }
    var items by remember { mutableStateOf(emptyList<PendingUpload>()) }

    LaunchedEffect(refreshKey) {
        items = UploadQueueStorage.getAll(context)
    }

    fun refresh() { refreshKey++ }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pendientes de subir") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Volver") } }
            )
        }
    ) { inner ->
        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(inner), contentAlignment = Alignment.Center) {
                Text("No hay PDFs pendientes.")
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(inner).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items) { itx ->
                val isUploading = itx.status == "UPLOADING"

                Card {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("Cotización ID: ${itx.cotizacionId}", fontWeight = FontWeight.Bold)
                        Text("Estado: ${itx.status}")

                        if (!itx.lastError.isNullOrBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text("Error: ${itx.lastError}")
                        }

                        Spacer(Modifier.height(10.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        onRetryUpload(itx)
                                        refresh()
                                    }
                                },
                                enabled = !isUploading && itx.status != "DONE"
                            ) {
                                if (isUploading) {
                                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Subiendo…")
                                } else {
                                    val actionText = when (itx.status) {
                                        "PENDING" -> "Subir"
                                        "ERROR" -> "Reintentar"
                                        "DONE" -> "Subido"
                                        else -> "Subir"
                                    }
                                    Text(actionText)
                                }
                            }

                            OutlinedButton(
                                onClick = { onRemove(itx.id); refresh() },
                                enabled = !isUploading
                            ) { Text("Quitar") }
                        }
                    }
                }
            }
        }
    }
}
