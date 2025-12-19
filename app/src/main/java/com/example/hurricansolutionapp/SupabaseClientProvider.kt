package com.example.hurricansolutionapp

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.engine.okhttp.OkHttp
import io.github.jan.supabase.storage.Storage

object SupabaseClientProvider {

    private const val SUPABASE_URL = "https://vlorculyexquudkiwxoq.supabase.co"
    private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InZsb3JjdWx5ZXhxdXVka2l3eG9xIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjYwNzQwODcsImV4cCI6MjA4MTY1MDA4N30.nm16hTVvaYsWUvK71--T9P16lySZ1eqhEIokwWjCklM"

    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = SUPABASE_URL,
            supabaseKey = SUPABASE_ANON_KEY
        ) {
            httpEngine = OkHttp.create()

            // 👇 OJO: esta install es la de SUPABASE, no la de KTOR
            install(Auth)
            install(Postgrest)
            install(Storage)
        }
    }
}
