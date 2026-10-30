package ir.smsforward

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class Prefs(c: Context) {
    private val ctx = c.applicationContext
    private val sp = ctx.getSharedPreferences("cfg", Context.MODE_PRIVATE)

    /** کلید اصلی؛ وقتی خاموش باشد هیچ قانونی اجرا نمی‌شود. */
    var enabled: Boolean
        get() = sp.getBoolean("enabled", false)
        set(v) = sp.edit().putBoolean("enabled", v).apply()

    /** حالت نمایش: 0=خودکار (طبق سیستم)، 1=روشن، 2=تاریک */
    var themeMode: Int
        get() = sp.getInt("themeMode", 0).coerceIn(0, 2)
        set(v) = sp.edit().putInt("themeMode", v).apply()

    // ---------------------------------------------------------------- قانون‌ها

    fun rules(): List<Rule> = synchronized(LOCK) {
        val raw = sp.getString("rules", null) ?: return@synchronized migrateLegacy()
        try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { Rule.fromJson(arr.getJSONObject(it)) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun rule(id: String): Rule? = rules().firstOrNull { it.id == id }

    /** commit همزمان تا خطای ذخیره‌سازی قابل تشخیص باشد؛ true یعنی ذخیره شد. */
    fun saveRules(list: List<Rule>): Boolean = synchronized(LOCK) {
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        sp.edit().putString("rules", arr.toString()).commit()
    }

    /** اگر شناسه موجود باشد جایگزین می‌شود، وگرنه به انتهای لیست اضافه می‌شود. */
    fun upsertRule(r: Rule): Boolean = synchronized(LOCK) {
        val all = rules()
        val i = all.indexOfFirst { it.id == r.id }
        saveRules(if (i >= 0) all.toMutableList().also { it[i] = r } else all + r)
    }

    fun deleteRule(id: String): Boolean = synchronized(LOCK) { saveRules(rules().filterNot { it.id == id }) }

    fun setRuleEnabled(id: String, on: Boolean): Boolean = synchronized(LOCK) {
        saveRules(rules().map { if (it.id == id) it.copy(enabled = on) else it })
    }

    /** تنظیمات نسخه‌های قبل (یک شماره، یک فیلتر) به یک قانون تبدیل می‌شود. */
    private fun migrateLegacy(): List<Rule> {
        val target = sp.getString("target", "") ?: ""
        val list = if (target.isBlank()) emptyList() else listOf(
            Rule(
                name = ctx.getString(R.string.rule_default_name, 1),
                recvSim = sp.getInt("recvSim", 0).coerceIn(0, 2),
                allSenders = sp.getInt("filterMode", 0) != 1,
                senders = Forwarder.parseList(sp.getString("filterList", "") ?: ""),
                targets = listOf(target),
                sendSim = sp.getInt("sendSim", 0).coerceIn(0, 2),
                template = sp.getString("template", null)
                    ?: if (sp.getBoolean("header", true)) Template.DEFAULT else "{body}"
            )
        )
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        sp.edit().putString("rules", arr.toString())
            .remove("target").remove("recvSim").remove("filterMode").remove("filterList")
            .remove("sendSim").remove("template").remove("header")
            .apply()
        return list
    }

    // ---------------------------------------------------------------- گزارش
    // فقط فرستنده، زمان و وضعیت ذخیره می‌شود؛ متن پیام‌ها هرگز ذخیره نمی‌شود.

    data class LogEntry(val time: Long, val sender: String, val ok: Boolean, val note: String)

    fun logEntries(): List<LogEntry> = synchronized(LOCK) {
        try {
            val arr = JSONArray(sp.getString("log2", "[]"))
            (0 until arr.length()).map {
                val o = arr.getJSONObject(it)
                LogEntry(o.getLong("t"), o.getString("s"), o.getBoolean("ok"), o.optString("n"))
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addLog(sender: String, ok: Boolean, note: String) = synchronized(LOCK) {
        val all = listOf(LogEntry(System.currentTimeMillis(), sender, ok, note)) + logEntries()
        val arr = JSONArray()
        all.take(MAX_LOG).forEach {
            arr.put(JSONObject().put("t", it.time).put("s", it.sender).put("ok", it.ok).put("n", it.note))
        }
        sp.edit().putString("log2", arr.toString()).apply()
    }

    fun clearLog() = synchronized(LOCK) { sp.edit().remove("log2").apply() }

    private companion object {
        val LOCK = Any()
        const val MAX_LOG = 50
    }
}
