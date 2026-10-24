package com.soulredemption.smsrelay.sms

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.telephony.SmsManager
import android.telephony.SubscriptionManager

data class SimInfo(val slot: Int, val subId: Int, val carrier: String) {
    val label: String get() = "SIM ${slot + 1}" + if (carrier.isNotBlank()) " · $carrier" else ""
}

object Sims {
    /** Debug-only override used to render dual-SIM UI on single-SIM emulators. */
    @Volatile var fakeDualSim = false

    @SuppressLint("MissingPermission")
    fun active(context: Context): List<SimInfo> {
        if (fakeDualSim) return listOf(SimInfo(0, -100, "Carrier A"), SimInfo(1, -101, "Carrier B"))
        val sm = context.getSystemService(SubscriptionManager::class.java) ?: return emptyList()
        return runCatching {
            sm.activeSubscriptionInfoList.orEmpty()
                .map { SimInfo(it.simSlotIndex, it.subscriptionId, it.carrierName?.toString().orEmpty()) }
                .sortedBy { it.slot }
        }.getOrDefault(emptyList())
    }

    fun slotForSubId(context: Context, subId: Int): Int =
        active(context).firstOrNull { it.subId == subId }?.slot ?: -1

    fun subIdForSlot(context: Context, slot: Int): Int? =
        active(context).firstOrNull { it.slot == slot }?.subId

    @Suppress("DEPRECATION")
    fun smsManager(context: Context, subId: Int): SmsManager {
        val valid = subId >= 0 && subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val base = context.getSystemService(SmsManager::class.java)
            if (valid) base.createForSubscriptionId(subId) else base
        } else {
            if (valid) SmsManager.getSmsManagerForSubscriptionId(subId) else SmsManager.getDefault()
        }
    }
}
