package com.example.hurricansolutionapp

import androidx.compose.runtime.Composable

@Composable
fun MedidasScreen(
    draft: CotizacionDraft,
    onDraftChange: (CotizacionDraft) -> Unit,
    onBack: () -> Unit,
    onContinuarResumen: (Cotizacion) -> Unit,
    isDarkMode: Boolean,
    currentStep: Int = 2,  // ✅ Paso actual por defecto
    totalSteps: Int = 3    // ✅ Total de pasos por defecto
) {
    CotizacionesFormScreen(
        draft = draft,
        onDraftChange = onDraftChange,
        onContinuarResumen = onContinuarResumen,
        onBack = onBack,
        isDarkMode = isDarkMode,
        currentStep = currentStep,
        totalSteps = totalSteps
    )
}