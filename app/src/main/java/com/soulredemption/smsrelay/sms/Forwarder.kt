package com.soulredemption.smsrelay.sms

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.soulredemption.smsrelay.data.LogEntry
import com.soulredemption.smsrelay.data.Matcher
import com.soulredemption.smsrelay.data.Rule
import com.soulredemption.smsrelay.data.Status
import com.soulredemption.smsrelay.data.Store
import java.util.UUID

object Forwarder {
    private const val TAG = "SmsRelay"
    const val MAX_ATTEMPTS = 3

    fun handleIncoming(context: Context, sender: String, body: String, receivedSubId: Int) {
        val store = Store.get(context)
        // Echo guard: if this exact text is something we forwarded recently, it's our own forward
        // arriving back (e.g. forwarding to your other SIM) — forwarding it again would loop forever.
        if (Matcher.isEcho(body, store.log.value.map { it.time to it.body }, System.currentTimeMillis())) {
            Log.i(TAG, "skipping echo of an earlier forward")
            return
        }
        val slot = Sims.slotForSubId(context, receivedSubId)
        val simCount = Sims.active(context).size
        // One forward per destination even if several rules match it.
        val sentTo = mutableSetOf<String>()

        for (rule in store.rules.value) {
            val recipients = Matcher.recipientsFor(rule, sender, slot)
            if (recipients.isEmpty()) continue
            val senderName = rule.senders.firstOrNull { Matcher.sameAddress(it.number, sender) }?.name
            val text = Matcher.formatForward(rule, sender, senderName, slot, simCount, body)
            val sendSubId = when (rule.sendSlot) {
                Rule.SAME_SIM -> receivedSubId
                else -> Sims.subIdForSlot(context, rule.sendSlot) ?: receivedSubId
            }
            for (to in recipients) {
                val key = Matcher.digits(to.number).takeLast(10).ifEmpty { to.number }
                if (!sentTo.add(key)) continue
                val entry = LogEntry(
                    id = UUID.randomUUID().toString(),
                    time = System.currentTimeMillis(),
                    from = sender,
                    to = to.number,
                    toName = to.name,
                    body = text,
                    sendSubId = sendSubId,
                    status = Status.SENDING,
                )
                store.addLog(entry)
                send(context, entry)
            }
        }
    }

    /** Sends (or re-sends) the forward described by [entry]; delivery results arrive in [SentStatusReceiver]. */
    fun send(context: Context, entry: LogEntry) {
        try {
            val sms = Sims.smsManager(context, entry.sendSubId)
            val parts = sms.divideMessage(entry.body)
            val intents = ArrayList<PendingIntent>(parts.size)
            parts.indices.forEach { i ->
                val intent = Intent(context, SentStatusReceiver::class.java)
                    .setAction(SentStatusReceiver.ACTION_SENT)
                    .putExtra(SentStatusReceiver.EXTRA_LOG_ID, entry.id)
                    .putExtra(SentStatusReceiver.EXTRA_ATTEMPT, entry.attempts)
                    .setData(android.net.Uri.parse("smsrelay://sent/${entry.id}/${entry.attempts}/$i"))
                intents.add(PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE))
            }
            if (parts.size > 1) {
                sms.sendMultipartTextMessage(entry.to, null, parts, intents, null)
            } else {
                sms.sendTextMessage(entry.to, null, entry.body, intents.first(), null)
            }
        } catch (e: Exception) {
            Log.w(TAG, "send failed", e)
            onFailure(context, entry.id, e.message ?: e.javaClass.simpleName)
        }
    }

    fun onSuccess(context: Context, logId: String) {
        Store.get(context).updateLog(logId) { if (it.status == Status.SENDING) it.copy(status = Status.SENT, error = null) else it }
    }

    fun onFailure(context: Context, logId: String, reason: String) {
        val store = Store.get(context)
        val current = store.findLog(logId) ?: return
        // Several parts of one message can fail; react once per attempt.
        if (current.status != Status.SENDING) return
        if (current.attempts < MAX_ATTEMPTS) {
            store.updateLog(logId) { it.copy(status = Status.RETRYING, error = reason) }
            ForwardWorker.scheduleRetry(context, logId, current.attempts)
        } else {
            val failed = store.updateLog(logId) { it.copy(status = Status.FAILED, error = reason) }
            if (failed != null) Notifier.forwardFailed(context, failed)
        }
    }
}
