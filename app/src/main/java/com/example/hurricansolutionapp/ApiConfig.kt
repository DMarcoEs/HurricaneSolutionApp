package com.example.hurricansolutionapp

/**
 * Configuración centralizada de APIs
 *
 * IMPORTANTE:
 * - Reemplaza los valores con tus credenciales reales
 * - NO subas este archivo a repositorios públicos
 * - Considera usar BuildConfig o variables de entorno en producción
 */
object ApiConfig {

    // ═══════════════════════════════════════════════════════════════════════════════
    // GOHIGHLEVEL API
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * API Key de GoHighLevel
     * Obtener en: Settings → Integrations → API Key
     */
    const val GHL_API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJsb2NhdGlvbl9pZCI6ImZPU3A4TmpRbVBGMHdYdmN0eHdLIiwidmVyc2lvbiI6MSwiaWF0IjoxNzYxNjExODcxNDA3LCJzdWIiOiI2VHJtbnF0bVdjYjk5WjZZaXM1MiJ9.YUP7N5nqbAI4fcPr1UjPbXemRGMqp1dA08iZp8QMsfM"

    /**
     * Location ID (ID de tu agencia/ubicación)
     * Obtener en: Settings → Business Profile → Location ID
     */
    const val GHL_LOCATION_ID = "fOSp8NjQmPF0wXvctxwK"

    /**
     * Base URL de la API de GoHighLevel
     * Versión actual: v1
     */
    const val GHL_BASE_URL = "https://rest.gohighlevel.com/v1"

    /**
     * Pipeline ID del pipeline "Hurricane Solution AI"
     * Obtener listando pipelines o desde la UI de GoHighLevel
     */
    const val GHL_PIPELINE_ID = "1knTxKomTWKupFFre2KL"

    /**
     * Stage IDs del pipeline
     * Estos IDs se obtienen al listar los stages del pipeline
     */
    object GHLStages {
        const val LEADS_NUEVOS = "e58aa9d8-c019-4bb4-94ef-b1a466cfacb8"
        const val MEDIDAS = "96f380d8-856f-43f6-ac1d-8465a00b0814"
        const val SEGUIMIENTO_MEDIDAS = "c2797f95-29ae-493b-8efd-7b05cd9ca764"
        const val ASIGNACION = "79f09eb2-9ccb-4334-b8c4-1e7017f1496f"
        const val CITAS = "55d85125-9788-426d-b259-ceca19190a57"
        const val SEGUIMIENTO_CITAS = "6034e794-2051-4fa9-a945-4f51bfebc915"
        const val PROYECTO_COTIZADO = "05c0a7d7-1c96-4df6-a322-f8024db8a029"
        const val SEGUIMIENTO_PROYECTO = "1ae6f94e-88d4-4e49-920f-f8bcc0591299"
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // GOOGLE DRIVE API
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Client ID de OAuth 2.0
     * Obtener en: Google Cloud Console → APIs & Services → Credentials
     */
    const val DRIVE_CLIENT_ID = "451151009864-p2cqs39c1563rs9m40omu2bj8gepsnj1.apps.googleusercontent.com"

    /**
     * Nombre de la aplicación (aparece en la pantalla de autorización)
     */
    const val DRIVE_APPLICATION_NAME = "Hurricane Solution"

    /**
     * Scopes necesarios para Google Drive
     * drive.file = acceso solo a archivos creados por la app
     */
    const val DRIVE_SCOPES = "https://www.googleapis.com/auth/drive.file"

    /**
     * ID de la carpeta compartida "Hurricane Solution" en Drive de Derek
     * Todos los PDFs se subirán dentro de esta carpeta
     */
    const val DRIVE_SHARED_FOLDER_ID = "1cq6ouhIBjySGAzjg90T0ydpBtVOqxeXV"

    /**
     * Carpeta raíz en Google Drive donde se almacenan los PDFs
     */
    const val DRIVE_ROOT_FOLDER = "Hurricane Solution"

    // ═══════════════════════════════════════════════════════════════════════════════
    // FEATURE FLAGS
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Habilitar/deshabilitar integración con GoHighLevel
     */
    const val GHL_ENABLED = true

    /**
     * Habilitar/deshabilitar integración con Google Drive
     */
    const val DRIVE_ENABLED = true

    /**
     * Sincronización automática de leads al abrir la app
     * false = solo sincroniza cuando el usuario presiona el botón
     */
    const val AUTO_SYNC_LEADS = false

    /**
     * Subida automática a Google Drive
     * false = solo sube cuando el usuario presiona el botón
     */
    const val AUTO_UPLOAD_DRIVE = false

    /**
     * Timeout para requests HTTP (en milisegundos)
     */
    const val REQUEST_TIMEOUT = 30000L // 30 segundos

    /**
     * Número máximo de leads a sincronizar por request
     */
    const val MAX_LEADS_PER_SYNC = 100

    /**
     * Reintentos automáticos en caso de error de red
     */
    const val MAX_RETRIES = 3

    // ═══════════════════════════════════════════════════════════════════════════════
    // LOGGING
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Habilitar logs detallados (solo para desarrollo)
     */
    const val ENABLE_DETAILED_LOGS = true

    /**
     * Tag para logs de GoHighLevel
     */
    const val LOG_TAG_GHL = "GoHighLevel"

    /**
     * Tag para logs de Google Drive
     */
    const val LOG_TAG_DRIVE = "GoogleDrive"
}