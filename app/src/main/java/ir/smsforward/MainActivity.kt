package ir.smsforward

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.core.view.isVisible
import com.google.android.material.color.MaterialColors
import ir.smsforward.databinding.ActivityMainBinding
import ir.smsforward.databinding.ItemRuleBinding

class MainActivity : BaseActivity() {

    private lateinit var b: ActivityMainBinding
    private lateinit var prefs: Prefs

    override val rootView: View get() = b.root

    /** true وقتی مقدارها از کد تنظیم می‌شوند تا شنونده‌ها دوباره ذخیره نکنند */
    private var binding = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)
        setSupportActionBar(b.toolbar)
        applyEdgeToEdge(b.root, b.topBar, b.scroll)
        prefs = Prefs(this)

        b.switchEnabled.setOnCheckedChangeListener { _, checked ->
            if (binding) return@setOnCheckedChangeListener
            if (checked) tryEnable() else {
                prefs.enabled = false
                render()
            }
        }
        b.btnAddRule.setOnClickListener { openEditor(null) }
    }

    override fun onResume() {
        super.onResume()
        // اگر کاربر دسترسی‌ها را از تنظیمات سیستم لغو کرده باشد
        if (prefs.enabled && !hasSmsPermissions()) {
            prefs.enabled = false
            snack(R.string.perm_revoked)
        }
        render()
    }

    override fun onSmsPermissionsDenied() = render()

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_menu -> {
            MenuSheet.show(
                this,
                onSettings = { startActivity(Intent(this, SettingsActivity::class.java)) },
                onLog = { startActivity(Intent(this, LogActivity::class.java)) },
                onPrivacy = { showPrivacy() },
                onAbout = { showAbout() }
            )
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun openEditor(id: String?) {
        startActivity(Intent(this, RuleActivity::class.java).apply {
            if (id != null) putExtra(RuleActivity.EXTRA_RULE_ID, id)
        })
    }

    // ------------------------------------------------------------------ نمایش

    private fun attrColor(attr: Int): Int = MaterialColors.getColor(b.root, attr)

    private fun render() {
        val on = prefs.enabled
        val rules = prefs.rules()
        val activeCount = rules.count { it.enabled }

        binding = true
        b.switchEnabled.isChecked = on
        binding = false

        b.statusSubtitle.text = when {
            !on -> getString(R.string.status_off)
            activeCount == 0 -> getString(R.string.status_no_active)
            else -> getString(R.string.status_on, activeCount)
        }
        val primary = androidx.appcompat.R.attr.colorPrimary
        b.statusSubtitle.setTextColor(
            attrColor(if (on) primary else com.google.android.material.R.attr.colorOnSurfaceVariant)
        )
        b.cardStatus.strokeColor = attrColor(
            if (on) primary else com.google.android.material.R.attr.colorOutlineVariant
        )

        // وقتی کلید اصلی خاموش است، همه‌ی زیرمجموعه‌ها هم خاموش و کم‌رنگ نمایش داده می‌شوند
        b.rulesSection.alpha = if (on) 1f else 0.5f
        b.rulesHint.isVisible = !on && rules.isNotEmpty()

        renderRules(rules, on)
    }

    private fun renderRules(rules: List<Rule>, masterOn: Boolean) {
        b.emptyRules.isVisible = rules.isEmpty()
        b.rulesList.removeAllViews()
        rules.forEachIndexed { index, rule ->
            val row = ItemRuleBinding.inflate(layoutInflater, b.rulesList, false)
            row.ruleName.text = rule.displayName(this, index)

            val senders = if (rule.allSenders) getString(R.string.sum_all_senders)
            else getString(R.string.sum_n_senders, rule.senders.size)
            row.ruleSenders.text = getString(R.string.sum_from, senders)

            val shown = rule.targets.take(3).joinToString("، ") { bidi(it) }
            val more = if (rule.targets.size > 3) getString(R.string.sum_more, rule.targets.size - 3) else ""
            row.ruleTargets.text = getString(R.string.sum_to, rule.targets.size, shown + more)

            row.ruleSims.text = getString(R.string.sum_sims, simName(rule.recvSim, true), simName(rule.sendSim, false))

            binding = true
            row.ruleSwitch.isChecked = rule.enabled
            binding = false
            row.ruleSwitch.isEnabled = masterOn
            row.root.alpha = if (rule.enabled) 1f else 0.6f
            row.ruleSwitch.setOnCheckedChangeListener { _, checked ->
                if (binding) return@setOnCheckedChangeListener
                if (!prefs.setRuleEnabled(rule.id, checked)) {
                    showErrors(R.string.error_title, listOf(getString(R.string.err_save_failed)))
                }
                render()
            }
            row.root.setOnClickListener { openEditor(rule.id) }
            b.rulesList.addView(row.root)
        }
    }

    private fun simName(v: Int, recv: Boolean): String = when (v) {
        1 -> getString(R.string.chip_sim1)
        2 -> getString(R.string.chip_sim2)
        else -> getString(if (recv) R.string.chip_all_sims else R.string.chip_send_same)
    }

    // ------------------------------------------------------------------ عملیات

    private fun tryEnable() {
        val rules = prefs.rules()
        val errors = ArrayList<String>()
        if (rules.isEmpty()) errors += getString(R.string.err_no_rules)
        else if (rules.none { it.enabled }) errors += getString(R.string.err_no_active_rule)
        if (errors.isNotEmpty()) {
            showErrors(R.string.error_title, errors)
            render() // کلید را به حالت خاموش برمی‌گرداند
            return
        }
        withSmsPermissions {
            prefs.enabled = true
            render()
        }
    }
}
