package com.soulredemption.smsrelay.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.soulredemption.smsrelay.BuildConfig
import com.soulredemption.smsrelay.data.Store
import com.soulredemption.smsrelay.sms.Sims

sealed interface Screen {
    data object Home : Screen
    data class Edit(val ruleId: String?) : Screen
}

class MainActivity : ComponentActivity() {
    private var granted by mutableStateOf(false)
    private var screen by mutableStateOf<Screen>(Screen.Home)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (BuildConfig.DEBUG) applyDebugExtras()
        granted = hasAllPermissions(this)

        setContent {
            SmsRelayTheme {
                if (!granted) {
                    SetupScreen(onGranted = { granted = hasAllPermissions(this) })
                } else when (val s = screen) {
                    Screen.Home -> HomeScreen(onEdit = { screen = Screen.Edit(it) })
                    is Screen.Edit -> {
                        BackHandler { screen = Screen.Home }
                        RuleEditor(ruleId = s.ruleId, onDone = { screen = Screen.Home })
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        granted = hasAllPermissions(this)
    }

    /** Debug builds only: seed sample data and fake a second SIM for store screenshots. */
    private fun applyDebugExtras() {
        val i = intent ?: return
        if (i.getBooleanExtra("dual_sim", false)) Sims.fakeDualSim = true
        if (i.getBooleanExtra("brand_colors", false)) useDynamicColor = false
        if (i.getBooleanExtra("demo", false)) {
            DemoData.seed(Store.get(this))
            getSharedPreferences("ui", MODE_PRIVATE).edit().putBoolean("battery_tip_dismissed", true).apply()
        }
        if (i.getBooleanExtra("skip_setup", false)) skipSetup = true
        i.getStringExtra("edit")?.let { id -> screen = Screen.Edit(id.ifBlank { null }) }
    }

    companion object {
        private var skipSetup = false

        fun requiredPermissions(): Array<String> = buildList {
            add(Manifest.permission.RECEIVE_SMS)
            add(Manifest.permission.SEND_SMS)
            add(Manifest.permission.READ_PHONE_STATE)
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
        }.toTypedArray()

        /** Notifications are optional; the SMS and phone-state permissions are required. */
        fun hasAllPermissions(context: Context): Boolean = skipSetup || listOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_PHONE_STATE,
        ).all { context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }
    }
}
