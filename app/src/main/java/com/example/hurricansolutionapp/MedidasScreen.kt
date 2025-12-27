package com.example.hurricansolutionapp

import androidx.compose.runtime.Composable

@Composable
fun MedidasScreen(
    draft: CotizacionDraft,
    onDraftChange: (CotizacionDraft) -> Unit,
    onBack: () -> Unit,
    onContinuarResumen: (Cotizacion) -> Unit
) {
    CotizacionFormScreen(
        draft = draft,
        onDraftChange = onDraftChange,
        onContinuarResumen = onContinuarResumen,
        onBack = onBack
    )
}
