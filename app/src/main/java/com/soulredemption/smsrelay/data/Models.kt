package com.soulredemption.smsrelay.data

import org.json.JSONArray
import org.json.JSONObject

data class Contact(val number: String, val name: String? = null) {
    val label: String get() = if (name.isNullOrBlank()) number else name

    fun toJson(): JSONObject = JSONObject().put("number", number).apply { if (name != null) put("name", name) }

    companion object {
        fun fromJson(o: JSONObject) = Contact(o.getString("number"), o.optString("name").ifBlank { null })
    }
}

data class Rule(
    val id: String,
    val name: String = "",
    val enabled: Boolean = true,
    val anySender: Boolean = true,
    val senders: List<Contact> = emptyList(),
    val recipients: List<Contact> = emptyList(),
    /** SIM slot (0 = SIM 1, 1 = SIM 2) the message must arrive on, or [ANY_SIM]. */
    val simSlot: Int = ANY_SIM,
    /** SIM slot used to send the forward, or [SAME_SIM] to reply from the receiving SIM. */
    val sendSlot: Int = SAME_SIM,
    val includeSender: Boolean = true,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id).put("name", name).put("enabled", enabled).put("anySender", anySender)
        .put("senders", JSONArray(senders.map { it.toJson() }))
        .put("recipients", JSONArray(recipients.map { it.toJson() }))
        .put("simSlot", simSlot).put("sendSlot", sendSlot).put("includeSender", includeSender)

    companion object {
        const val ANY_SIM = -1
        const val SAME_SIM = -1

        fun fromJson(o: JSONObject) = Rule(
            id = o.getString("id"),
            name = o.optString("name"),
            enabled = o.optBoolean("enabled", true),
            anySender = o.optBoolean("anySender", true),
            senders = o.optJSONArray("senders").toContacts(),
            recipients = o.optJSONArray("recipients").toContacts(),
            simSlot = o.optInt("simSlot", ANY_SIM),
            sendSlot = o.optInt("sendSlot", SAME_SIM),
            includeSender = o.optBoolean("includeSender", true),
        )

        private fun JSONArray?.toContacts(): List<Contact> =
            if (this == null) emptyList() else (0 until length()).map { Contact.fromJson(getJSONObject(it)) }
    }
}

enum class Status { SENDING, SENT, RETRYING, FAILED }

data class LogEntry(
    val id: String,
    val time: Long,
    val from: String,
    val to: String,
    val toName: String?,
    val body: String,
    val sendSubId: Int,
    val status: Status,
    val attempts: Int = 1,
    val error: String? = null,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id).put("time", time).put("from", from).put("to", to).put("toName", toName ?: "")
        .put("body", body).put("sendSubId", sendSubId).put("status", status.name)
        .put("attempts", attempts).put("error", error ?: "")

    companion object {
        fun fromJson(o: JSONObject) = LogEntry(
            id = o.getString("id"),
            time = o.getLong("time"),
            from = o.getString("from"),
            to = o.getString("to"),
            toName = o.optString("toName").ifBlank { null },
            body = o.optString("body"),
            sendSubId = o.optInt("sendSubId", -1),
            status = runCatching { Status.valueOf(o.getString("status")) }.getOrDefault(Status.FAILED),
            attempts = o.optInt("attempts", 1),
            error = o.optString("error").ifBlank { null },
        )
    }
}
