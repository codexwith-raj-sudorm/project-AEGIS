package com.jarvis.aegis.security

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.telecom.TelecomManager

class EssentialAccessResolver(private val context: Context) {
    fun requiredPackages(): Set<String> = buildSet {
        add(context.packageName)
        add("com.android.systemui")
        add("android")
        add("com.android.permissioncontroller")
        add("com.google.android.permissioncontroller")
        defaultDialer()?.let(::add)
        defaultInputMethod()?.let(::add)
        resolve(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))?.let(::add)
        resolve(Intent(Settings.ACTION_SETTINGS))?.let(::add)
    }

    fun defaultDialer(): String? = context.getSystemService(TelecomManager::class.java)?.defaultDialerPackage

    private fun defaultInputMethod(): String? = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.DEFAULT_INPUT_METHOD,
    )?.substringBefore('/')

    private fun resolve(intent: Intent): String? = intent.resolveActivity(context.packageManager)?.packageName
}
