package com.soulredemption.smsrelay.data

/**
 * Pure matching logic (no Android dependencies) so it can be unit tested.
 */
object Matcher {

    /** Digits only, keeping no formatting. */
    fun digits(s: String): String = s.filter { it.isDigit() }

    private fun isAlphanumericId(s: String): Boolean = s.any { it.isLetter() }

    /**
     * True when two addresses refer to the same sender.
     * - Phone numbers are compared on their trailing digits, so "+91 98765 43210",
     *   "098765 43210" and "9876543210" all match.
     * - Alphanumeric sender IDs ("VM-HDFCBK") match case-insensitively, and a saved
     *   "HDFCBK" also matches carrier-prefixed variants like "AD-HDFCBK".
     */
    fun sameAddress(saved: String, incoming: String): Boolean {
        val a = saved.trim()
        val b = incoming.trim()
        if (a.isEmpty() || b.isEmpty()) return false
        if (isAlphanumericId(a) || isAlphanumericId(b)) {
            val na = a.uppercase().filter { it.isLetterOrDigit() }
            val nb = b.uppercase().filter { it.isLetterOrDigit() }
            if (na.isEmpty() || nb.isEmpty()) return false
            if (na == nb) return true
            // Carrier prefix forms: "AD-HDFCBK", "VM-HDFCBK-S"
            val tokens = b.uppercase().split('-', ' ', '_').filter { it.isNotEmpty() }
            return tokens.any { it == na }
        }
        val da = digits(a)
        val db = digits(b)
        if (da.isEmpty() || db.isEmpty()) return false
        if (da.length < 7 || db.length < 7) return da == db
        val n = minOf(10, da.length, db.length)
        return da.takeLast(n) == db.takeLast(n)
    }

    /** Which of the rule's recipients should get a message from [sender] arriving on [slot]. */
    fun recipientsFor(rule: Rule, sender: String, slot: Int): List<Contact> {
        if (!rule.enabled) return emptyList()
        if (rule.simSlot != Rule.ANY_SIM && rule.simSlot != slot) return emptyList()
        if (!rule.anySender && rule.senders.none { sameAddress(it.number, sender) }) return emptyList()
        // Never bounce a message back to the number it came from (prevents forwarding loops).
        return rule.recipients.filterNot { sameAddress(it.number, sender) }
    }

    /** True when [body] matches a forward we sent within [windowMs] (it has looped back to us). */
    fun isEcho(body: String, recent: List<Pair<Long, String>>, now: Long, windowMs: Long = 30 * 60_000L): Boolean =
        recent.any { (time, sent) -> now - time in 0..windowMs && sent.trim() == body.trim() }

    fun formatForward(rule: Rule, sender: String, senderName: String?, slot: Int, simCount: Int, body: String): String {
        if (!rule.includeSender) return body
        val who = if (senderName.isNullOrBlank()) sender else "$senderName ($sender)"
        val sim = if (simCount > 1 && slot >= 0) " via SIM ${slot + 1}" else ""
        return "Fwd from $who$sim:\n$body"
    }
}
