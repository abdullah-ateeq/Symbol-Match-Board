package com.example.symbolmatchboard.data

import android.content.SharedPreferences
import androidx.core.content.edit

/** Minimal key-value abstraction so repositories can be tested without Android. */
interface KeyValueStore {
    fun getBoolean(key: String, default: Boolean): Boolean
    fun getInt(key: String, default: Int): Int
    fun getString(key: String, default: String): String
    fun edit(block: Editor.() -> Unit)
    fun clear(keys: Collection<String>)

    interface Editor {
        fun putBoolean(key: String, value: Boolean)
        fun putInt(key: String, value: Int)
        fun putString(key: String, value: String)
    }
}

class SharedPreferencesStore(private val prefs: SharedPreferences) : KeyValueStore {
    override fun getBoolean(key: String, default: Boolean) = prefs.getBoolean(key, default)
    override fun getInt(key: String, default: Int) = prefs.getInt(key, default)
    override fun getString(key: String, default: String) = prefs.getString(key, default) ?: default

    override fun edit(block: KeyValueStore.Editor.() -> Unit) {
        prefs.edit {
            val prefsEditor = this
            val editor = object : KeyValueStore.Editor {
                override fun putBoolean(key: String, value: Boolean) { prefsEditor.putBoolean(key, value) }
                override fun putInt(key: String, value: Int) { prefsEditor.putInt(key, value) }
                override fun putString(key: String, value: String) { prefsEditor.putString(key, value) }
            }
            editor.block()
        }
    }

    override fun clear(keys: Collection<String>) {
        prefs.edit { keys.forEach { remove(it) } }
    }
}

class InMemoryStore : KeyValueStore {
    private val values = mutableMapOf<String, Any>()
    override fun getBoolean(key: String, default: Boolean) = values[key] as? Boolean ?: default
    override fun getInt(key: String, default: Int) = values[key] as? Int ?: default
    override fun getString(key: String, default: String) = values[key] as? String ?: default

    override fun edit(block: KeyValueStore.Editor.() -> Unit) {
        object : KeyValueStore.Editor {
            override fun putBoolean(key: String, value: Boolean) { values[key] = value }
            override fun putInt(key: String, value: Int) { values[key] = value }
            override fun putString(key: String, value: String) { values[key] = value }
        }.block()
    }

    override fun clear(keys: Collection<String>) {
        keys.forEach { values.remove(it) }
    }
}
