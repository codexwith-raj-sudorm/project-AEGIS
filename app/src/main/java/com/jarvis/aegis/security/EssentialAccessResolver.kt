package com.jarvis.aegis.security

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
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
        roleHolder(RoleManager.ROLE_DIALER)?.let(::add)
        resolve(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))?.let(::add)
        resolve(Intent(Settings.ACTION_SETTINGS))?.let(::add)
    }

    fun defaultDialer(): String? = context.getSystemService(TelecomManager::class.java)?.defaultDialerPackage

    private fun defaultInputMethod(): String? = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.DEFAULT_INPUT_METHOD,
    )?.substringBefore('/')

    private fun roleHolder(role: String): String? {
        if (Build.VERSION.SDK_INT < 29) return null
        val manager = context.getSystemService(RoleManager::class.java)
        return if (manager.isRoleAvailable(role)) manager.getRoleHolders(role).firstOrNull() else null
    }

    private fun resolve(intent: Intent): String? = intent.resolveActivity(context.packageManager)?.packageName
}
