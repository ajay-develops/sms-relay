package com.soulredemption.smsrelay.ui

import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import android.text.format.DateUtils
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.BatteryAlert
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.soulredemption.smsrelay.data.LogEntry
import com.soulredemption.smsrelay.data.Rule
import com.soulredemption.smsrelay.data.Status
import com.soulredemption.smsrelay.data.Store
import com.soulredemption.smsrelay.sms.Sims

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onEdit: (String?) -> Unit) {
    val context = LocalContext.current
    val store = remember { Store.get(context) }
    val rules by store.rules.collectAsState()
    val log by store.log.collectAsState()
    val simCount = remember { Sims.active(context).size }
    val power = remember { context.getSystemService(PowerManager::class.java) }
    val prefs = remember { context.getSharedPreferences("ui", android.content.Context.MODE_PRIVATE) }
    var batteryOk by remember {
        mutableStateOf(power.isIgnoringBatteryOptimizations(context.packageName) || prefs.getBoolean("battery_tip_dismissed", false))
    }
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = { LargeTopAppBar(title = { Text("SMS Relay") }, scrollBehavior = scroll) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onEdit(null) },
                icon = { Icon(Icons.Outlined.Add, null) },
                text = { Text("Add rule") },
            )
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp, top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!batteryOk && rules.isNotEmpty()) item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                    Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Outlined.BatteryAlert, null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                        Column(Modifier.weight(1f)) {
                            Text("Keep forwarding reliable", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                            Text(
                                "Some phones pause apps to save battery. Set SMS Relay to \"Unrestricted\" battery use.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                            )
                            val btn = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onTertiaryContainer)
                            Row {
                                TextButton(colors = btn, onClick = {
                                    runCatching { context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
                                }) { Text("Open settings") }
                                TextButton(colors = btn, onClick = {
                                    prefs.edit().putBoolean("battery_tip_dismissed", true).apply(); batteryOk = true
                                }) { Text("Dismiss") }
                            }
                        }
                    }
                }
            }

            item { SectionTitle("Rules") }
            if (rules.isEmpty()) item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("No rules yet", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Add a rule to choose which texts get forwarded, and where they go.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            items(rules, key = { it.id }) { rule ->
                RuleCard(rule, simCount, onClick = { onEdit(rule.id) }, onToggle = { store.saveRule(rule.copy(enabled = it)) })
            }

            if (log.isNotEmpty()) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SectionTitle("Recent activity", Modifier.weight(1f))
                        TextButton(onClick = { store.clearLog() }) { Text("Clear") }
                    }
                }
                items(log.take(30), key = { "log-" + it.id }) { LogRow(it) }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text, modifier.padding(top = 8.dp, start = 4.dp),
        style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary,
    )
}

fun describeSenders(rule: Rule): String =
    if (rule.anySender) "Any sender" else rule.senders.joinToString(", ") { it.label }

fun describeSim(slot: Int): String = if (slot == Rule.ANY_SIM) "Both SIMs" else "SIM ${slot + 1}"

@Composable
private fun RuleCard(rule: Rule, simCount: Int, onClick: () -> Unit, onToggle: (Boolean) -> Unit) {
    val title = rule.name.ifBlank { "${describeSenders(rule)} → ${rule.recipients.joinToString(", ") { it.label }}" }
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(Modifier.padding(start = 16.dp, end = 12.dp, top = 14.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Detail("From", describeSenders(rule))
                Detail("To", rule.recipients.joinToString(", ") { it.label })
                if (simCount > 1 || rule.simSlot != Rule.ANY_SIM) {
                    val via = if (rule.sendSlot == Rule.SAME_SIM) "" else " · sends via SIM ${rule.sendSlot + 1}"
                    Detail("On", describeSim(rule.simSlot) + via)
                }
            }
            Spacer(Modifier.width(8.dp))
            Switch(checked = rule.enabled, onCheckedChange = onToggle)
        }
    }
}

@Composable
private fun Detail(label: String, value: String) {
    Row {
        Text(
            label, Modifier.width(44.dp), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun LogRow(e: LogEntry) {
    val (icon, tint, status) = when (e.status) {
        Status.SENT -> Triple(Icons.Outlined.CheckCircle, MaterialTheme.colorScheme.primary, "Forwarded")
        Status.SENDING -> Triple(Icons.Outlined.Schedule, MaterialTheme.colorScheme.onSurfaceVariant, "Sending…")
        Status.RETRYING -> Triple(Icons.Outlined.Schedule, MaterialTheme.colorScheme.tertiary, "Retrying · ${e.error ?: ""}")
        Status.FAILED -> Triple(Icons.Outlined.ErrorOutline, MaterialTheme.colorScheme.error, "Failed · ${e.error ?: ""}")
    }
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, null, Modifier.padding(top = 2.dp).size(20.dp), tint = tint)
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(e.from, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, Modifier.padding(horizontal = 6.dp).size(14.dp))
                Text(e.toName ?: e.to, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
            }
            Text(
                "$status · ${if (System.currentTimeMillis() - e.time < 60_000) "Just now" else DateUtils.getRelativeTimeSpanString(e.time)}",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
