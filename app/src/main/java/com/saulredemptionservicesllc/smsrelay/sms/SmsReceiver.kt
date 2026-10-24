package com.saulredemptionservicesllc.smsrelay.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SubscriptionManager
import kotlin.concurrent.thread

/** Receives every incoming SMS (even when the app is closed) and forwards it per the user's rules. */
class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val parts = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (parts.isEmpty()) return

        val subId = intent.getIntExtra(
            SubscriptionManager.EXTRA_SUBSCRIPTION_INDEX,
            intent.getIntExtra("subscription", -1),
        )
        // A long SMS arrives as several parts in one broadcast; join them per sender.
        val messages = parts.filterNotNull()
            .groupBy { it.originatingAddress.orEmpty() }
            .map { (sender, ps) -> sender to ps.joinToString("") { it.messageBody.orEmpty() } }
            .filter { it.first.isNotBlank() }

        val pending = goAsync()
        thread(name = "sms-relay-forward") {
            try {
                messages.forEach { (sender, body) -> Forwarder.handleIncoming(context, sender, body, subId) }
            } finally {
                pending.finish()
            }
        }
    }
}
