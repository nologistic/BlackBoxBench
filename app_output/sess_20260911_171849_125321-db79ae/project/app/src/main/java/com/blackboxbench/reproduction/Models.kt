package com.blackboxbench.reproduction

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

enum class ViewMode(val label: String) {
    DAY("日视图"), WEEK("周视图"), MONTH("月视图"),
    MONTH_DAY("月视图和日视图"), YEAR("年视图"), AGENDA("简单活动列表")
}
enum class Page { CALENDAR, SETTINGS, APPEARANCE, CALENDARS, ABOUT }
enum class ItemKind { EVENT, TASK }

data class CalendarBook(
    val id: String,
    val name: String,
    val color: Long,
    val visible: Boolean = true
)

data class PlannerItem(
    val id: String,
    val calendarId: String,
    val kind: ItemKind,
    val title: String,
    val date: LocalDate,
    val time: LocalTime = LocalTime.of(9, 0),
    val endTime: LocalTime = LocalTime.of(10, 0),
    val allDay: Boolean = false,
    val location: String = "",
    val description: String = "",
    val reminder: Int = 10,
    val repeat: String = "",
    val done: Boolean = false,
    val status: String = "已确认"
)

class PlannerState(private val context: Context) {
    var page by mutableStateOf(Page.CALENDAR)
    var view by mutableStateOf(ViewMode.MONTH)
    var selectedDate by mutableStateOf(LocalDate.of(2026, 9, 11))
    var query by mutableStateOf("")
    var searching by mutableStateOf(false)
    var darkTheme by mutableStateOf(false)
    var weekStartsMonday by mutableStateOf(true)
    var showWeekNumbers by mutableStateOf(false)
    var highlightWeekends by mutableStateOf(false)
    var allowTasks by mutableStateOf(true)
    var editorItem by mutableStateOf<PlannerItem?>(null)
    var editorIsNew by mutableStateOf(false)

    val calendars = mutableStateListOf<CalendarBook>()
    val items = mutableStateListOf<PlannerItem>()
    private val prefs = context.getSharedPreferences("calendar_state", Context.MODE_PRIVATE)

    init {
        load()
    }

    private fun parseColor(value: String): Long =
        try { android.graphics.Color.parseColor(value).toLong() } catch (_: Exception) { 0xFF7E9CFF }

    private fun load() {
        val saved = prefs.getString("payload", null)
        if (saved != null) {
            readPayload(JSONObject(saved))
        } else {
            val root = JSONObject(AssetStore.readText(context, "events.json"))
            root.getJSONArray("calendars").let { array ->
                for (i in 0 until array.length()) {
                    val c = array.getJSONObject(i)
                    calendars.add(CalendarBook(c.getString("id"), c.getString("name"), parseColor(c.getString("color")), c.optBoolean("visible", true)))
                }
            }
            root.getJSONArray("events").let { array ->
                for (i in 0 until array.length()) {
                    val e = array.getJSONObject(i)
                    val start = LocalDateTime.parse(e.getString("start"))
                    val end = LocalDateTime.parse(e.getString("end"))
                    items.add(
                        PlannerItem(
                            id=e.getString("id"), calendarId=e.getString("calendar"), kind=ItemKind.EVENT,
                            title=e.getString("title"), date=start.toLocalDate(), time=start.toLocalTime(),
                            endTime=end.toLocalTime(), allDay=e.optBoolean("all_day"), location=e.optString("location"),
                            reminder=e.optInt("reminder_minutes", 0), repeat=e.optString("rrule")
                        )
                    )
                }
            }
            root.getJSONArray("tasks").let { array ->
                for (i in 0 until array.length()) {
                    val t = array.getJSONObject(i)
                    items.add(
                        PlannerItem(
                            id=t.getString("id"), calendarId=t.getString("calendar"), kind=ItemKind.TASK,
                            title=t.getString("title"), date=LocalDate.parse(t.getString("due")),
                            time=LocalTime.of(18, 0), endTime=LocalTime.of(18, 0), done=t.optBoolean("done")
                        )
                    )
                }
            }
            persist()
        }
    }

    private fun readPayload(root: JSONObject) {
        val cals = root.optJSONArray("calendars") ?: JSONArray()
        for (i in 0 until cals.length()) {
            val c = cals.getJSONObject(i)
            calendars.add(CalendarBook(c.getString("id"), c.getString("name"), c.getLong("color"), c.optBoolean("visible", true)))
        }
        val savedItems = root.optJSONArray("items") ?: JSONArray()
        for (i in 0 until savedItems.length()) {
            val e = savedItems.getJSONObject(i)
            items.add(
                PlannerItem(
                    id=e.getString("id"), calendarId=e.getString("calendarId"),
                    kind=ItemKind.valueOf(e.getString("kind")), title=e.getString("title"),
                    date=LocalDate.parse(e.getString("date")), time=LocalTime.parse(e.getString("time")),
                    endTime=LocalTime.parse(e.getString("endTime")), allDay=e.optBoolean("allDay"),
                    location=e.optString("location"), description=e.optString("description"),
                    reminder=e.optInt("reminder", 10), repeat=e.optString("repeat"),
                    done=e.optBoolean("done"), status=e.optString("status", "已确认")
                )
            )
        }
        darkTheme = root.optBoolean("darkTheme", false)
        weekStartsMonday = root.optBoolean("weekStartsMonday", true)
        showWeekNumbers = root.optBoolean("showWeekNumbers", false)
        highlightWeekends = root.optBoolean("highlightWeekends", false)
        allowTasks = root.optBoolean("allowTasks", true)
    }

    fun persist() {
        val root = JSONObject()
        val cals = JSONArray()
        calendars.forEach { c ->
            cals.put(JSONObject().put("id", c.id).put("name", c.name).put("color", c.color).put("visible", c.visible))
        }
        val arr = JSONArray()
        items.forEach { e ->
            arr.put(
                JSONObject().put("id", e.id).put("calendarId", e.calendarId).put("kind", e.kind.name)
                    .put("title", e.title).put("date", e.date.toString()).put("time", e.time.toString())
                    .put("endTime", e.endTime.toString()).put("allDay", e.allDay).put("location", e.location)
                    .put("description", e.description).put("reminder", e.reminder).put("repeat", e.repeat)
                    .put("done", e.done).put("status", e.status)
            )
        }
        root.put("calendars", cals).put("items", arr).put("darkTheme", darkTheme)
            .put("weekStartsMonday", weekStartsMonday).put("showWeekNumbers", showWeekNumbers)
            .put("highlightWeekends", highlightWeekends).put("allowTasks", allowTasks)
        prefs.edit().putString("payload", root.toString()).apply()
    }

    fun calendar(id: String): CalendarBook = calendars.firstOrNull { it.id == id } ?: calendars.first()
    fun visibleItems(): List<PlannerItem> {
        val visible = calendars.filter { it.visible }.map { it.id }.toSet()
        return items.filter { it.calendarId in visible }
    }

    fun occursOn(item: PlannerItem, date: LocalDate): Boolean {
        if (date.isBefore(item.date)) return false
        val rule = item.repeat
        if (rule.isBlank()) return date == item.date
        if (rule.contains("COUNT=4") && date.isAfter(item.date.plusWeeks(3))) return false
        return when {
            rule.contains("FREQ=DAILY") -> {
                val interval = if (rule.contains("INTERVAL=2")) 2 else 1
                java.time.temporal.ChronoUnit.DAYS.between(item.date, date) % interval == 0L
            }
            rule.contains("FREQ=WEEKLY") -> {
                val code = when (date.dayOfWeek) {
                    DayOfWeek.MONDAY -> "MO"; DayOfWeek.TUESDAY -> "TU"; DayOfWeek.WEDNESDAY -> "WE"
                    DayOfWeek.THURSDAY -> "TH"; DayOfWeek.FRIDAY -> "FR"; DayOfWeek.SATURDAY -> "SA"
                    DayOfWeek.SUNDAY -> "SU"
                }
                rule.substringAfter("BYDAY=", "").substringBefore(";").split(",").contains(code)
            }
            rule.contains("FREQ=MONTHLY") -> date.dayOfMonth == rule.substringAfter("BYMONTHDAY=", "1").substringBefore(";").toIntOrNull()
            rule.contains("FREQ=YEARLY") -> {
                val m = rule.substringAfter("BYMONTH=", "1").substringBefore(";").toIntOrNull()
                val d = rule.substringAfter("BYMONTHDAY=", "1").substringBefore(";").toIntOrNull()
                date.monthValue == m && date.dayOfMonth == d
            }
            else -> date == item.date
        }
    }

    fun itemsOn(date: LocalDate): List<PlannerItem> =
        visibleItems().filter { occursOn(it, date) }.sortedWith(compareBy<PlannerItem> { !it.allDay }.thenBy { it.time })

    fun startNew(kind: ItemKind) {
        val calId = calendars.firstOrNull { it.visible }?.id ?: calendars.first().id
        editorItem = PlannerItem(
            id=UUID.randomUUID().toString(), calendarId=calId, kind=kind, title="", date=selectedDate,
            time=LocalTime.of(18, 0), endTime=LocalTime.of(19, 0)
        )
        editorIsNew = true
    }

    fun saveItem(item: PlannerItem) {
        val index = items.indexOfFirst { it.id == item.id }
        if (index >= 0) items[index] = item else items.add(item)
        editorItem = null
        editorIsNew = false
        persist()
    }

    fun deleteItem(id: String) {
        items.removeAll { it.id == id }
        editorItem = null
        persist()
    }

    fun duplicate(item: PlannerItem) {
        editorItem = item.copy(id=UUID.randomUUID().toString())
        editorIsNew = true
    }

    fun toggleDone(item: PlannerItem) {
        val index = items.indexOfFirst { it.id == item.id }
        if (index >= 0) items[index] = item.copy(done=!item.done)
        editorItem = null
        persist()
    }

    fun addCalendar(name: String, color: Long) {
        calendars.add(CalendarBook(UUID.randomUUID().toString(), name, color, true))
        persist()
    }

    fun toggleCalendar(id: String) {
        val index = calendars.indexOfFirst { it.id == id }
        if (index >= 0) calendars[index] = calendars[index].copy(visible=!calendars[index].visible)
        persist()
    }

    fun deleteCalendar(id: String) {
        if (calendars.size <= 1) return
        items.removeAll { it.calendarId == id }
        calendars.removeAll { it.id == id }
        persist()
    }
}
