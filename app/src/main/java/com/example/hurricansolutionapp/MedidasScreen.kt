package com.example.hurricansolutionapp

import androidx.compose.runtime.Composable

@Composable
fun MedidasScreen(
    draft: CotizacionDraft,
    onDraftChange: (CotizacionDraft) -> Unit,
    onBack: () -> Unit,
    onContinuarResumen: (Cotizacion) -> Unit,
    isDarkMode: Boolean // ✅ Agregar este parámetro
) {
    // ✅ Cambiar a CotizacionesFormScreen (plural)
    CotizacionesFormScreen(
        draft = draft,
        onDraftChange = onDraftChange,
        onContinuarResumen = onContinuarResumen,
        onBack = onBack,
        isDarkMode = isDarkMode // ✅ Pasar el estado del tema
    )
}
