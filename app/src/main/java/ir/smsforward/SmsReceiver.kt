package ir.smsforward

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(c: Context, i: Intent) {
        if (i.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val p = Prefs(c)
        if (!p.enabled) return
        val rules = p.rules().filter { it.enabled && it.targets.isNotEmpty() }
        if (rules.isEmpty()) return

        val msgs = Telephony.Sms.Intents.getMessagesFromIntent(i)
        if (msgs.isNullOrEmpty()) return
        val sender = msgs[0].originatingAddress ?: ""
        val body = msgs.joinToString("") { it.messageBody ?: "" }
        val subId = i.getIntExtra("subscription", -1)
        val slot = Forwarder.slotOf(c, subId) // از صفر؛ -1 یعنی نامشخص

        val sent = HashSet<String>()
        p.rules().forEachIndexed { index, all ->
            val rule = rules.firstOrNull { it.id == all.id } ?: return@forEachIndexed

            // فیلتر سیم‌کارت
            if (rule.recvSim == 1 && slot != 0) return@forEachIndexed
            if (rule.recvSim == 2 && slot != 1) return@forEachIndexed

            // فیلتر فرستنده
            if (!rule.allSenders && rule.senders.none { Forwarder.same(sender, it) }) return@forEachIndexed

            // جلوگیری از حلقه: پیامی که از یکی از مقصدها آمده به همان مقصد برنمی‌گردد
            val targets = rule.targets.filterNot { Forwarder.same(sender, it) }
            if (targets.isEmpty()) return@forEachIndexed

            Forwarder.forward(
                c, rule.copy(targets = targets), rule.displayName(c, index),
                sender, body, subId, slot, dedupe = sent
            )
        }
    }
}
