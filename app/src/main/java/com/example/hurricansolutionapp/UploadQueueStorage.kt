package com.example.hurricansolutionapp

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object UploadQueueStorage {
    private const val PREFS = "upload_queue_prefs"
    private const val KEY = "pending_uploads"
    private val gson = Gson()


    fun getAll(context: Context): MutableList<PendingUpload> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY, null) ?: return mutableListOf()
        val type = object : TypeToken<MutableList<PendingUpload>>() {}.type
        return gson.fromJson(json, type) ?: mutableListOf()
    }

    fun saveAll(context: Context, items: List<PendingUpload>) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY, gson.toJson(items)).apply()
    }

    fun enqueue(context: Context, item: PendingUpload) {
        val list = getAll(context)
        list.add(item)
        saveAll(context, list)
    }

    fun remove(context: Context, id: String) {
        val list = getAll(context)
        val newList = list.filterNot { it.id == id }
        saveAll(context, newList)
    }

    fun markDone(context: Context, id: String) {
        val list = getAll(context)
        val idx = list.indexOfFirst { it.id == id }
        if (idx >= 0) {
            list[idx] = list[idx].copy(status = "DONE", lastError = null)
            saveAll(context, list)
        }
    }

    fun markError(context: Context, id: String, error: String) {
        val list = getAll(context)
        val idx = list.indexOfFirst { it.id == id }
        if (idx >= 0) {
            list[idx] = list[idx].copy(status = "ERROR", lastError = error)
            saveAll(context, list)
        }
    }

    fun clearDone(context: Context) {
        val list = getAll(context)
        val newList = list.filterNot { it.status == "DONE" }
        saveAll(context, newList)
    }

    fun markUploading(context: Context, id: String) {
        val all = getAll(context)
        val updated = all.map {
            if (it.id == id) it.copy(status = "UPLOADING", lastError = null)
            else it
        }
        saveAll(context, updated)
    }



}
