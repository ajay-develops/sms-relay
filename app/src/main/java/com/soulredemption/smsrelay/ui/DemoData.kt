package com.soulredemption.smsrelay.ui

import com.soulredemption.smsrelay.data.Contact
import com.soulredemption.smsrelay.data.LogEntry
import com.soulredemption.smsrelay.data.Rule
import com.soulredemption.smsrelay.data.Status
import com.soulredemption.smsrelay.data.Store

/** Fictional sample data (555 numbers) used only by debug builds for store screenshots. */
object DemoData {
    fun seed(store: Store) {
        store.rules.value.forEach { store.deleteRule(it.id) }
        store.clearLog()
        store.saveRule(
            Rule(
                id = "demo-bank", name = "Bank alerts to my work phone", anySender = false,
                senders = listOf(Contact("HDFCBK"), Contact("AXISBK")),
                recipients = listOf(Contact("+1 555 010 4477", "Work phone")),
                simSlot = 0,
            ),
        )
        store.saveRule(
            Rule(
                id = "demo-all", name = "", anySender = true,
                recipients = listOf(Contact("+1 555 010 2290", "Alex Rivera")),
                simSlot = 1,
            ),
        )
        store.saveRule(
            Rule(
                id = "demo-school", name = "School messages", anySender = false, enabled = false,
                senders = listOf(Contact("+1 555 010 8812", "Maple School")),
                recipients = listOf(Contact("+1 555 010 3321", "Sam"), Contact("+1 555 010 6650", "Jordan")),
            ),
        )
        val now = System.currentTimeMillis()
        listOf(
            LogEntry("d1", now - 4 * 60_000, "HDFCBK", "+1 555 010 4477", "Work phone", "Fwd from HDFCBK via SIM 1:\nYour OTP is 482913", -1, Status.SENT),
            LogEntry("d2", now - 52 * 60_000, "+1 555 010 7781", "+1 555 010 2290", "Alex Rivera", "Fwd from +1 555 010 7781 via SIM 2:\nRunning 10 min late", -1, Status.SENT),
            LogEntry("d3", now - 3 * 3_600_000, "AXISBK", "+1 555 010 4477", "Work phone", "Fwd from AXISBK via SIM 1:\nCard ending 2201 used", -1, Status.RETRYING, 2, "No mobile service"),
            LogEntry("d4", now - 26 * 3_600_000, "+1 555 010 9034", "+1 555 010 2290", "Alex Rivera", "Fwd from +1 555 010 9034 via SIM 2:\nParcel delivered", -1, Status.SENT),
        ).reversed().forEach { store.addLog(it) }
    }
}
