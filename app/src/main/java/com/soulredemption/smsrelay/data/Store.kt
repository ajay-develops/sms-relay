package com.soulredemption.smsrelay.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import java.io.File

/**
 * Tiny on-device JSON store for rules and the activity log. Everything stays in the app's
 * private storage; nothing is synced or uploaded.
 */
class Store private constructor(context: Context) {
    private val dir = context.filesDir
    private val rulesFile = File(dir, "rules.json")
    private val logFile = File(dir, "log.json")
    private val lock = Any()

    private val _rules = MutableStateFlow(readRules())
    val rules: StateFlow<List<Rule>> = _rules
    private val _log = MutableStateFlow(readLog())
    val log: StateFlow<List<LogEntry>> = _log

    fun saveRule(rule: Rule) = synchronized(lock) {
        val list = _rules.value.toMutableList()
        val i = list.indexOfFirst { it.id == rule.id }
        if (i >= 0) list[i] = rule else list.add(rule)
        writeRules(list)
    }

    fun deleteRule(id: String) = synchronized(lock) { writeRules(_rules.value.filterNot { it.id == id }) }

    fun addLog(entry: LogEntry) = synchronized(lock) { writeLog(listOf(entry) + _log.value) }

    fun updateLog(id: String, transform: (LogEntry) -> LogEntry): LogEntry? = synchronized(lock) {
        var updated: LogEntry? = null
        writeLog(_log.value.map { if (it.id == id) transform(it).also { u -> updated = u } else it })
        updated
    }

    fun findLog(id: String): LogEntry? = _log.value.firstOrNull { it.id == id }

    fun clearLog() = synchronized(lock) { writeLog(emptyList()) }

    private fun readRules(): List<Rule> = runCatching {
        val a = JSONArray(rulesFile.readText())
        (0 until a.length()).map { Rule.fromJson(a.getJSONObject(it)) }
    }.getOrDefault(emptyList())

    private fun readLog(): List<LogEntry> = runCatching {
        val a = JSONArray(logFile.readText())
        (0 until a.length()).map { LogEntry.fromJson(a.getJSONObject(it)) }
    }.getOrDefault(emptyList())

    private fun writeRules(list: List<Rule>) {
        atomicWrite(rulesFile, JSONArray(list.map { it.toJson() }).toString())
        _rules.value = list
    }

    private fun writeLog(list: List<LogEntry>) {
        val capped = list.take(MAX_LOG)
        atomicWrite(logFile, JSONArray(capped.map { it.toJson() }).toString())
        _log.value = capped
    }

    private fun atomicWrite(file: File, text: String) {
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(text)
        if (!tmp.renameTo(file)) { file.writeText(text); tmp.delete() }
    }

    companion object {
        private const val MAX_LOG = 200

        @Volatile private var instance: Store? = null
        fun get(context: Context): Store =
            instance ?: synchronized(this) { instance ?: Store(context.applicationContext).also { instance = it } }
    }
}
