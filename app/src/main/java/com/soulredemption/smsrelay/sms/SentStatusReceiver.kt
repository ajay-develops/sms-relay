package com.soulredemption.smsrelay.sms

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import com.soulredemption.smsrelay.data.Store

class SentStatusReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_SENT) return
        val logId = intent.getStringExtra(EXTRA_LOG_ID) ?: return
        val attempt = intent.getIntExtra(EXTRA_ATTEMPT, 1)
        // Ignore late callbacks from an earlier attempt.
        if (Store.get(context).findLog(logId)?.attempts != attempt) return
        if (resultCode == Activity.RESULT_OK) {
            Forwarder.onSuccess(context, logId)
        } else {
            Forwarder.onFailure(context, logId, reason(resultCode))
        }
    }

    private fun reason(code: Int): String = when (code) {
        SmsManager.RESULT_ERROR_NO_SERVICE -> "No mobile service"
        SmsManager.RESULT_ERROR_RADIO_OFF -> "Airplane mode or radio off"
        SmsManager.RESULT_ERROR_NULL_PDU -> "Message could not be encoded"
        SmsManager.RESULT_ERROR_LIMIT_EXCEEDED -> "Android SMS sending limit reached"
        SmsManager.RESULT_ERROR_SHORT_CODE_NOT_ALLOWED,
        SmsManager.RESULT_ERROR_SHORT_CODE_NEVER_ALLOWED -> "Short codes are blocked"
        else -> "Carrier rejected the message (code $code)"
    }

    companion object {
        const val ACTION_SENT = "com.soulredemption.smsrelay.SENT"
        const val EXTRA_LOG_ID = "log_id"
        const val EXTRA_ATTEMPT = "attempt"
    }
}
