package com.saulredemptionservicesllc.smsrelay

import com.saulredemptionservicesllc.smsrelay.data.Contact
import com.saulredemptionservicesllc.smsrelay.data.Matcher
import com.saulredemptionservicesllc.smsrelay.data.Rule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MatcherTest {
    @Test fun numbersMatchAcrossFormats() {
        assertTrue(Matcher.sameAddress("+91 98765 43210", "9876543210"))
        assertTrue(Matcher.sameAddress("098765-43210", "+919876543210"))
        assertTrue(Matcher.sameAddress("(555) 010-4477", "+15550104477"))
        assertFalse(Matcher.sameAddress("9876543210", "9876543211"))
    }

    @Test fun shortCodesNeedExactMatch() {
        assertTrue(Matcher.sameAddress("57575", "57575"))
        assertFalse(Matcher.sameAddress("575", "1575"))
    }

    @Test fun senderIdsMatchWithCarrierPrefix() {
        assertTrue(Matcher.sameAddress("HDFCBK", "VM-HDFCBK"))
        assertTrue(Matcher.sameAddress("hdfcbk", "AD-HDFCBK-S"))
        assertTrue(Matcher.sameAddress("VM-HDFCBK", "VMHDFCBK"))
        assertFalse(Matcher.sameAddress("HDFCBK", "VM-ICICIB"))
        assertFalse(Matcher.sameAddress("HDFCBK", "9876543210"))
    }

    private val mom = Contact("+1 555 010 2290", "Mom")

    @Test fun anySenderRuleForwardsEverything() {
        val r = Rule("1", recipients = listOf(mom))
        assertEquals(listOf(mom), Matcher.recipientsFor(r, "+15550109999", 0))
    }

    @Test fun specificSendersOnly() {
        val r = Rule("1", anySender = false, senders = listOf(Contact("HDFCBK")), recipients = listOf(mom))
        assertEquals(listOf(mom), Matcher.recipientsFor(r, "VM-HDFCBK", 0))
        assertTrue(Matcher.recipientsFor(r, "+15550109999", 0).isEmpty())
    }

    @Test fun simFilter() {
        val r = Rule("1", recipients = listOf(mom), simSlot = 1)
        assertTrue(Matcher.recipientsFor(r, "x123", 0).isEmpty())
        assertEquals(listOf(mom), Matcher.recipientsFor(r, "x123", 1))
    }

    @Test fun disabledRuleDoesNothing() {
        assertTrue(Matcher.recipientsFor(Rule("1", enabled = false, recipients = listOf(mom)), "+15550109999", 0).isEmpty())
    }

    @Test fun neverForwardsBackToSender() {
        val r = Rule("1", recipients = listOf(mom))
        assertTrue(Matcher.recipientsFor(r, "+15550102290", 0).isEmpty())
    }

    @Test fun echoDetection() {
        val now = 10_000_000L
        val sent = listOf(now - 60_000 to "Fwd from +1555 via SIM 2:\nhi")
        assertTrue(Matcher.isEcho("Fwd from +1555 via SIM 2:\nhi", sent, now))
        assertFalse(Matcher.isEcho("hi", sent, now))
        assertFalse(Matcher.isEcho("Fwd from +1555 via SIM 2:\nhi", listOf(now - 3_600_000 to "Fwd from +1555 via SIM 2:\nhi"), now))
    }

    @Test fun formatting() {
        val r = Rule("1")
        assertEquals("Fwd from Mom (+1555) via SIM 2:\nhi", Matcher.formatForward(r, "+1555", "Mom", 1, 2, "hi"))
        assertEquals("Fwd from +1555:\nhi", Matcher.formatForward(r, "+1555", null, 0, 1, "hi"))
        assertEquals("hi", Matcher.formatForward(r.copy(includeSender = false), "+1555", null, 0, 2, "hi"))
    }
}
