package com.blackboxbench.reproduction

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** A repeating/one-shot alarm as observed in the target app. */
data class Alarm(
    val id: Int,
    val hour: Int,
    val minute: Int,
    val enabled: Boolean = true,
    val days: Set<Int> = emptySet(), // 1=Mon .. 7=Sun, empty = one shot
    val label: String = "",
    val ringtone: String = "默认铃声（Cesium）",
    val vibrate: Boolean = true,
    val isWake: Boolean = false,
)

data class WorldCity(val name: String, val offset: Double)

data class AppSettings(
    val analog: Boolean = false,
    val showSeconds: Boolean = false,
    val autoHomeTime: Boolean = true,
    val homeCity: String = "香港",
    val homeOffset: Double = 8.0,
    val alarmDurationMin: Int = 10,
    val snoozeMin: Int = 10,
    val alarmVolume: Int = 70,
)

data class BedtimeState(
    val enabled: Boolean = false,
    val bedHour: Int = 23,
    val bedMinute: Int = 0,
    val wakeHour: Int = 7,
    val wakeMinute: Int = 0,
    val days: Set<Int> = (1..7).toSet(),
    val sunrise: Boolean = false,
    val sound: String = "默认铃声（Cesium）",
    val vibrate: Boolean = true,
    val remindMinutes: Int = 15,
    val bedtimeMode: Boolean = true,
)

/** Editing draft for the bedtime setup steps; kept in the store so it survives recomposition. */
class BedtimeDraft {
    var wakeHour by mutableStateOf(7)
    var wakeMinute by mutableStateOf(0)
    var bedHour by mutableStateOf(23)
    var bedMinute by mutableStateOf(0)
    var days by mutableStateOf((1..7).toSet())
    var sunrise by mutableStateOf(false)
    var sound by mutableStateOf("默认铃声（Cesium）")
    var vibrate by mutableStateOf(true)
    var remindMinutes by mutableStateOf(15)
    var bedtimeMode by mutableStateOf(true)
}

/** Available device ringtones; maps onto the bundled synthetic audio samples. */
object Ringtones {
    const val SILENT = "静音"
    val names = listOf(
        SILENT,
        "默认铃声（Cesium）",
        "Argon",
        "Barium",
        "BeeBeep Alarm",
        "Beep-Beep-Beep Alarm",
    )

    fun rawRes(name: String): Int? = when (name) {
        SILENT -> null
        "默认铃声（Cesium）" -> R.raw.ringtone_cesium
        "Argon" -> R.raw.ringtone_argon
        "Barium" -> R.raw.ringtone_default
        "BeeBeep Alarm" -> R.raw.ringtone_default
        "Beep-Beep-Beep Alarm" -> R.raw.ringtone_argon
        else -> R.raw.ringtone_cesium
    }
}

object Weekdays {
    val short = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
    val single = listOf("一", "二", "三", "四", "五", "六", "日")

    fun name(day: Int): String = short[(day - 1).coerceIn(0, 6)]
    fun glyph(day: Int): String = single[(day - 1).coerceIn(0, 6)]

    /** Card subtitle: "每天" for all seven, otherwise the selected day list. */
    fun summary(days: Set<Int>): String = when {
        days.isEmpty() -> "仅一次"
        days.size == 7 -> "每天"
        else -> days.sorted().joinToString("、") { name(it) }
    }

    fun fromNowWeekday(dow: DayOfWeek): Int = dow.value

    fun dayOfWeekChar(day: Int): String = when (day) {
        1 -> "一"; 2 -> "二"; 3 -> "三"; 4 -> "四"; 5 -> "五"; 6 -> "六"; else -> "日"
    }
}

fun Int.two(): String = if (this in 0..9) "0$this" else "$this"

fun String.two(): String = if (length >= 2) this else "0$this"

fun hhmm(hour: Int, minute: Int): String = "${hour.toString().two()}:${minute.toString().two()}"

/** Next planned trigger for an alarm, relative to [now]. */
fun nextTrigger(alarm: Alarm, now: LocalDateTime): LocalDateTime? {
    if (alarm.days.isEmpty()) {
        val today = now.toLocalDate().atTime(LocalTime.of(alarm.hour, alarm.minute))
        return if (today.isAfter(now)) today else today.plusDays(1)
    }
    for (offset in 0..7) {
        val date: LocalDate = now.toLocalDate().plusDays(offset.toLong())
        val dow = date.dayOfWeek.value
        if (dow !in alarm.days) continue
        val candidate = date.atTime(LocalTime.of(alarm.hour, alarm.minute))
        if (candidate.isAfter(now)) return candidate
    }
    return null
}

/** "今天07:00" / "明天07:00" / "周四07:00" style label. */
fun relativeLabel(target: LocalDateTime, now: LocalDateTime): String {
    val days = java.time.temporal.ChronoUnit.DAYS.between(now.toLocalDate(), target.toLocalDate())
    val prefix = when (days) {
        0L -> "今天"
        1L -> "明天"
        else -> "周${Weekdays.dayOfWeekChar(target.dayOfWeek.value)}"
    }
    return prefix + hhmm(target.hour, target.minute)
}

fun nextEnabledAlarm(alarms: List<Alarm>, now: LocalDateTime): Pair<Alarm, LocalDateTime>? {
    return alarms.asSequence()
        .filter { it.enabled }
        .mapNotNull { a -> nextTrigger(a, now)?.let { a to it } }
        .minByOrNull { it.second }
}

class ClockStore(context: Context) {
    private val prefs = context.getSharedPreferences("bbb_clock", Context.MODE_PRIVATE)

    val alarms = mutableStateListOf<Alarm>()
    val worldCities = mutableStateListOf<WorldCity>()
    var settings by mutableStateOf(AppSettings())
        private set
    var bedtime by mutableStateOf(BedtimeState())
        private set
    var nextId by mutableStateOf(1)
        private set

    /** Bedtime onboarding step kept in the store so it survives recomposition. */
    var bedtimeStep by mutableStateOf(0)
    val bedtimeDraft = BedtimeDraft()

    init {
        load()
        bedtimeStep = if (bedtime.enabled) 3 else 0
    }
    private fun load() {
        val alarmJson = prefs.getString(KEY_ALARMS, null)
        if (alarmJson.isNullOrBlank()) {
            alarms.add(Alarm(id = 1, hour = 8, minute = 30, enabled = false, days = setOf(1, 2, 3, 4, 5)))
            alarms.add(Alarm(id = 2, hour = 9, minute = 0, enabled = false, days = setOf(6, 7)))
            nextId = 3
        } else {
            val arr = JSONArray(alarmJson)
            for (i in 0 until arr.length()) alarms.add(fromJson(arr.getJSONObject(i)))
            nextId = prefs.getInt(KEY_NEXT_ID, (alarms.maxOfOrNull { it.id } ?: 0) + 1)
        }
        prefs.getString(KEY_SETTINGS, null)?.let { runCatching { readSettings(JSONObject(it)) }.onSuccess { settings = it } }
        prefs.getString(KEY_BEDTIME, null)?.let { runCatching { readBedtime(JSONObject(it)) }.onSuccess { bedtime = it } }
        val cityJson = prefs.getString(KEY_CITIES, null)
        if (!cityJson.isNullOrBlank()) {
            val arr = JSONArray(cityJson)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                worldCities.add(WorldCity(o.getString("name"), o.getDouble("offset")))
            }
        }
    }

    fun persist() {
        val arr = JSONArray()
        alarms.forEach { arr.put(toJson(it)) }
        prefs.edit()
            .putString(KEY_ALARMS, arr.toString())
            .putInt(KEY_NEXT_ID, nextId)
            .putString(KEY_SETTINGS, settingsJson().toString())
            .putString(KEY_BEDTIME, bedtimeJson().toString())
            .putString(KEY_CITIES, citiesJson().toString())
            .apply()
    }

    fun addAlarm(hour: Int, minute: Int, days: Set<Int> = emptySet()): Alarm {
        val a = Alarm(id = nextId++, hour = hour, minute = minute, enabled = true, days = days)
        alarms.add(a)
        persist()
        return a
    }

    fun updateAlarm(updated: Alarm) {
        val idx = alarms.indexOfFirst { it.id == updated.id }
        if (idx >= 0) alarms[idx] = updated
        persist()
    }

    fun deleteAlarm(id: Int) {
        alarms.removeAll { it.id == id }
        persist()
    }

    fun updateSettings(block: (AppSettings) -> AppSettings) {
        settings = block(settings)
        persist()
    }

    /** Enabling bedtime creates (or refreshes) the dedicated wake alarm group entry. */
    fun enableBedtime(configured: BedtimeState) {
        bedtime = configured.copy(enabled = true)
        val existing = alarms.firstOrNull { it.isWake }
        if (existing == null) {
            alarms.add(
                Alarm(
                    id = nextId++,
                    hour = configured.wakeHour,
                    minute = configured.wakeMinute,
                    days = configured.days,
                    enabled = true,
                    isWake = true,
                    ringtone = configured.sound,
                    vibrate = configured.vibrate,
                ),
            )
        } else {
            updateAlarm(
                existing.copy(
                    hour = configured.wakeHour,
                    minute = configured.wakeMinute,
                    days = configured.days,
                    enabled = true,
                    ringtone = configured.sound,
                    vibrate = configured.vibrate,
                ),
            )
        }
        persist()
    }

    fun addCity(name: String, offset: Double) {
        if (worldCities.none { it.name == name }) worldCities.add(WorldCity(name, offset))
        persist()
    }

    fun removeCity(name: String) {
        worldCities.removeAll { it.name == name }
        persist()
    }

    private fun toJson(a: Alarm): JSONObject = JSONObject().apply {
        put("id", a.id); put("hour", a.hour); put("minute", a.minute)
        put("enabled", a.enabled)
        put("days", JSONArray(a.days.sorted()))
        put("label", a.label); put("ringtone", a.ringtone)
        put("vibrate", a.vibrate); put("isWake", a.isWake)
    }

    private fun fromJson(o: JSONObject): Alarm {
        val days = mutableSetOf<Int>()
        o.optJSONArray("days")?.let { arr -> for (i in 0 until arr.length()) days.add(arr.getInt(i)) }
        return Alarm(
            id = o.getInt("id"), hour = o.getInt("hour"), minute = o.getInt("minute"),
            enabled = o.optBoolean("enabled", true), days = days,
            label = o.optString("label", ""), ringtone = o.optString("ringtone", "默认铃声（Cesium）"),
            vibrate = o.optBoolean("vibrate", true), isWake = o.optBoolean("isWake", false),
        )
    }

    private fun settingsJson(): JSONObject = JSONObject().apply {
        put("analog", settings.analog); put("showSeconds", settings.showSeconds)
        put("autoHomeTime", settings.autoHomeTime); put("homeCity", settings.homeCity)
        put("homeOffset", settings.homeOffset); put("alarmDurationMin", settings.alarmDurationMin)
        put("snoozeMin", settings.snoozeMin); put("alarmVolume", settings.alarmVolume)
    }

    private fun readSettings(o: JSONObject) = AppSettings(
        analog = o.optBoolean("analog", false),
        showSeconds = o.optBoolean("showSeconds", false),
        autoHomeTime = o.optBoolean("autoHomeTime", true),
        homeCity = o.optString("homeCity", "香港"),
        homeOffset = o.optDouble("homeOffset", 8.0),
        alarmDurationMin = o.optInt("alarmDurationMin", 10),
        snoozeMin = o.optInt("snoozeMin", 10),
        alarmVolume = o.optInt("alarmVolume", 70),
    )

    private fun bedtimeJson(): JSONObject = JSONObject().apply {
        put("enabled", bedtime.enabled); put("bedHour", bedtime.bedHour); put("bedMinute", bedtime.bedMinute)
        put("wakeHour", bedtime.wakeHour); put("wakeMinute", bedtime.wakeMinute)
        put("days", JSONArray(bedtime.days.sorted()))
        put("sunrise", bedtime.sunrise); put("sound", bedtime.sound); put("vibrate", bedtime.vibrate)
        put("remindMinutes", bedtime.remindMinutes); put("bedtimeMode", bedtime.bedtimeMode)
    }

    private fun readBedtime(o: JSONObject): BedtimeState {
        val days = mutableSetOf<Int>()
        o.optJSONArray("days")?.let { arr -> for (i in 0 until arr.length()) days.add(arr.getInt(i)) }
        return BedtimeState(
            enabled = o.optBoolean("enabled", false),
            bedHour = o.optInt("bedHour", 23), bedMinute = o.optInt("bedMinute", 0),
            wakeHour = o.optInt("wakeHour", 7), wakeMinute = o.optInt("wakeMinute", 0),
            days = if (days.isEmpty()) (1..7).toSet() else days,
            sunrise = o.optBoolean("sunrise", false), sound = o.optString("sound", "默认铃声（Cesium）"),
            vibrate = o.optBoolean("vibrate", true), remindMinutes = o.optInt("remindMinutes", 15),
            bedtimeMode = o.optBoolean("bedtimeMode", true),
        )
    }

    private fun citiesJson(): JSONArray = JSONArray().apply {
        worldCities.forEach { c -> put(JSONObject().apply { put("name", c.name); put("offset", c.offset) }) }
    }

    companion object {
        private const val KEY_ALARMS = "alarms"
        private const val KEY_NEXT_ID = "next_id"
        private const val KEY_SETTINGS = "settings"
        private const val KEY_BEDTIME = "bedtime"
        private const val KEY_CITIES = "cities"
    }
}
