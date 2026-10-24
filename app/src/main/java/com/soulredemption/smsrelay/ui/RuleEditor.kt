package com.soulredemption.smsrelay.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.soulredemption.smsrelay.data.Contact
import com.soulredemption.smsrelay.data.Matcher
import com.soulredemption.smsrelay.data.Rule
import com.soulredemption.smsrelay.data.Store
import com.soulredemption.smsrelay.sms.Sims
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RuleEditor(ruleId: String?, onDone: () -> Unit) {
    val context = LocalContext.current
    val store = remember { Store.get(context) }
    val existing = remember(ruleId) { store.rules.value.firstOrNull { it.id == ruleId } }
    val base = existing ?: Rule(id = UUID.randomUUID().toString())
    val sims = remember { Sims.active(context) }

    var name by remember { mutableStateOf(base.name) }
    var anySender by remember { mutableStateOf(base.anySender) }
    val senders = remember { mutableStateListOf<Contact>().apply { addAll(base.senders) } }
    val recipients = remember { mutableStateListOf<Contact>().apply { addAll(base.recipients) } }
    var simSlot by remember { mutableStateOf(base.simSlot) }
    var sendSlot by remember { mutableStateOf(base.sendSlot) }
    var includeSender by remember { mutableStateOf(base.includeSender) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existing == null) "New rule" else "Edit rule") },
                navigationIcon = { IconButton(onClick = onDone) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") } },
                actions = {
                    if (existing != null) IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Outlined.Delete, "Delete rule") }
                },
            )
        },
        bottomBar = {
            Column(Modifier.navigationBarsPadding().imePadding().padding(16.dp)) {
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 8.dp))
                }
                Button(
                    onClick = {
                        error = when {
                            !anySender && senders.isEmpty() -> "Add at least one sender, or choose \"Any sender\"."
                            recipients.isEmpty() -> "Add at least one number to forward to."
                            else -> null
                        }
                        if (error == null) {
                            store.saveRule(
                                base.copy(
                                    name = name.trim(), anySender = anySender, senders = senders.toList(),
                                    recipients = recipients.toList(), simSlot = simSlot, sendSlot = sendSlot,
                                    includeSender = includeSender,
                                ),
                            )
                            onDone()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) { Text("Save rule") }
            }
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("Rule name (optional)") }, singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Section("Forward messages from") {
                Segmented(
                    options = listOf("Any sender", "Specific senders"),
                    selected = if (anySender) 0 else 1,
                    onSelect = { anySender = it == 0 },
                )
                if (!anySender) {
                    ContactPickerField(
                        list = senders,
                        hint = "Number or sender ID (e.g. HDFCBK)",
                        keyboard = KeyboardType.Text,
                    )
                }
            }

            Section("Send them to") {
                ContactPickerField(list = recipients, hint = "Phone number", keyboard = KeyboardType.Phone)
            }

            if (sims.size > 1) {
                Section("Only messages received on") {
                    val opts = listOf(Rule.ANY_SIM) + sims.map { it.slot }
                    Segmented(
                        options = opts.map { if (it == Rule.ANY_SIM) "Both SIMs" else "SIM ${it + 1}" },
                        selected = opts.indexOf(simSlot).coerceAtLeast(0),
                        onSelect = { simSlot = opts[it] },
                    )
                    Text(
                        sims.joinToString("   ") { it.label },
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Section("Send the forward using") {
                    val opts = listOf(Rule.SAME_SIM) + sims.map { it.slot }
                    Segmented(
                        options = opts.map { if (it == Rule.SAME_SIM) "Same SIM" else "SIM ${it + 1}" },
                        selected = opts.indexOf(sendSlot).coerceAtLeast(0),
                        onSelect = { sendSlot = opts[it] },
                    )
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Show who it's from", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Adds the original sender at the top of the forwarded text.",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(checked = includeSender, onCheckedChange = { includeSender = it })
                    }
                    val preview = Matcher.formatForward(
                        Rule("p", includeSender = includeSender), "+1 555 010 7781", null,
                        if (sims.size > 1) 0 else -1, sims.size, "See you at 6!",
                    )
                    Text(
                        preview, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (confirmDelete && existing != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this rule?") },
            text = { Text("Messages matching it will no longer be forwarded.") },
            confirmButton = { TextButton(onClick = { store.deleteRule(existing.id); onDone() }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { i, label ->
            SegmentedButton(
                selected = i == selected,
                onClick = { onSelect(i) },
                shape = SegmentedButtonDefaults.itemShape(i, options.size),
            ) { Text(label, maxLines = 1) }
        }
    }
}

/** Chips for the chosen numbers, plus "pick from contacts" and a typed-number field. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ContactPickerField(list: SnapshotStateList<Contact>, hint: String, keyboard: KeyboardType) {
    val context = LocalContext.current
    var typed by remember { mutableStateOf("") }

    fun add(c: Contact) {
        if (list.none { Matcher.sameAddress(it.number, c.number) }) list.add(c)
    }

    fun addTyped() {
        typed.split(',', ';', '\n').map { it.trim() }.filter { it.isNotEmpty() }.forEach { add(Contact(it)) }
        typed = ""
    }

    // ACTION_PICK on phone numbers opens the user's default contacts app and grants read access
    // to just the picked row, so no READ_CONTACTS permission is needed.
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uri = result.data?.data ?: return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME),
                null, null, null,
            )?.use { c ->
                if (c.moveToFirst()) {
                    val number = c.getString(0).orEmpty()
                    val name = c.getString(1)
                    if (number.isNotBlank()) add(Contact(number, name))
                }
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (list.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                list.forEach { c ->
                    InputChip(
                        selected = false,
                        onClick = { list.remove(c) },
                        label = { Text(if (c.name != null) "${c.name} · ${c.number}" else c.number, maxLines = 1) },
                        trailingIcon = { Icon(Icons.Outlined.Close, "Remove", Modifier.size(18.dp)) },
                    )
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = typed, onValueChange = { typed = it },
                placeholder = { Text(hint) }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = keyboard, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { addTyped() }),
                trailingIcon = {
                    if (typed.isNotBlank()) IconButton(onClick = { addTyped() }) { Icon(Icons.Outlined.Add, "Add number") }
                },
                modifier = Modifier.weight(1f),
            )
        }
        FilledTonalButton(onClick = {
            try {
                picker.launch(Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI))
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(context, "No contacts app found. Type the number instead.", Toast.LENGTH_LONG).show()
            }
        }) {
            Icon(Icons.Outlined.Contacts, null, Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            Text("Pick from contacts")
        }
    }
}
