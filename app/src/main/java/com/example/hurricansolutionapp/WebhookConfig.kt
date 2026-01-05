package com.example.hurricansolutionapp

/**
 * Configuración de webhooks de Make.com
 *
 * IMPORTANTE: Reemplaza las URLs con las reales de tu cuenta de Make.com
 */
object WebhookConfig {

    /**
     * Webhook para subir PDF a Google Drive y actualizar GoHighLevel
     *
     * Cómo obtener esta URL:
     * 1. Make.com → Crear escenario "Upload PDF to Drive"
     * 2. Agregar módulo "Webhooks → Custom webhook"
     * 3. Crear webhook llamado "PDF Upload Hurricane"
     * 4. Copiar la URL generada
     */
    const val UPLOAD_PDF_TO_DRIVE = "https://hook.us2.make.com/u4cbanx39hai1qspxycduprmilujrwwo"

    /**
     * Webhook para sincronizar leads de GoHighLevel
     * (Opcional - puede ser un Schedule en Make en lugar de webhook)
     */
    const val SYNC_LEADS_FROM_CRM = "https://hook.us1.make.com/REEMPLAZAR_CON_TU_WEBHOOK_ID"

    /**
     * Habilitar/deshabilitar webhooks (útil para testing)
     */
    const val WEBHOOKS_ENABLED = true

    /**
     * Timeout para requests HTTP (en milisegundos)
     */
    const val REQUEST_TIMEOUT = 30000L // 30 segundos
}