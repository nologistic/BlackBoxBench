@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.blackboxbench.reproduction

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { FossifyCalendarApp() }
    }
}

private val chineseMonths = listOf("一月","二月","三月","四月","五月","六月","七月","八月","九月","十月","十一月","十二月")
private val weekNames = listOf("周一","周二","周三","周四","周五","周六","周日")
private val compactWeekNames = listOf("一","二","三","四","五","六","日")
private fun monthName(date: LocalDate) = chineseMonths[date.monthValue - 1]
private fun dateTitle(date: LocalDate) = monthName(date) + " " + date.dayOfMonth + " (" + weekNames[date.dayOfWeek.value - 1] + ")"
private fun repeatLabel(rule: String): String = when {
    rule.isBlank() -> "不重复"
    rule.contains("FREQ=DAILY") -> "每天"
    rule.contains("FREQ=WEEKLY") -> "每周"
    rule.contains("FREQ=MONTHLY") -> "每月"
    rule.contains("FREQ=YEARLY") -> "每年"
    else -> "自定义"
}
private fun Color.toLongArgb(): Long {
    val a = (alpha * 255).toLong()
    val r = (red * 255).toLong()
    val g = (green * 255).toLong()
    val b = (blue * 255).toLong()
    return (a shl 24) or (r shl 16) or (g shl 8) or b
}

@Composable
fun FossifyCalendarApp() {
    val context = LocalContext.current
    val state = remember { PlannerState(context.applicationContext) }
    val colors = if (state.darkTheme) {
        darkColorScheme(primary=Color(0xFFAFC6FF), primaryContainer=Color(0xFF425887), surface=Color(0xFF17131A), background=Color(0xFF17131A))
    } else {
        lightColorScheme(primary=Color(0xFF7D93D1), primaryContainer=Color(0xFFDDE4F7), surface=Color(0xFFFFF9FF), background=Color(0xFFFFF9FF), onSurface=Color(0xFF071C4D))
    }
    MaterialTheme(colorScheme=colors) {
        Surface(Modifier.fillMaxSize(), color=MaterialTheme.colorScheme.background) {
            val editor = state.editorItem
            if (editor != null) {
                ItemEditor(state, editor)
            } else {
                when (state.page) {
                    Page.CALENDAR -> CalendarPage(state)
                    Page.SETTINGS -> SettingsPage(state)
                    Page.APPEARANCE -> AppearancePage(state)
                    Page.CALENDARS -> CalendarsPage(state)
                    Page.ABOUT -> AboutPage(state)
                }
            }
        }
    }
}

@Composable
private fun CalendarPage(state: PlannerState) {
    var addOpen by remember { mutableStateOf(false) }
    var viewOpen by remember { mutableStateOf(false) }
    var filterOpen by remember { mutableStateOf(false) }
    var overflowOpen by remember { mutableStateOf(false) }
    var gotoOpen by remember { mutableStateOf(false) }
    var holidayOpen by remember { mutableStateOf(false) }
    var birthdayOpen by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(bottom=72.dp)) {
            Surface(color=MaterialTheme.colorScheme.surface) {
                Box(Modifier.statusBarsPadding().padding(horizontal=16.dp, vertical=12.dp)) {
                    Surface(color=MaterialTheme.colorScheme.primaryContainer, shape=RoundedCornerShape(36.dp)) {
                        Row(Modifier.fillMaxWidth().height(54.dp), verticalAlignment=Alignment.CenterVertically) {
                            if (state.searching) {
                                TextButton(onClick={ state.searching=false; state.query="" }) { Text("←", fontSize=25.sp) }
                                TextField(
                                    value=state.query, onValueChange={ state.query=it }, placeholder={ Text("搜索活动和任务") },
                                    singleLine=true, colors=TextFieldDefaults.colors(
                                        focusedContainerColor=Color.Transparent, unfocusedContainerColor=Color.Transparent,
                                        focusedIndicatorColor=Color.Transparent, unfocusedIndicatorColor=Color.Transparent
                                    ), modifier=Modifier.weight(1f)
                                )
                            } else {
                                Row(Modifier.weight(1f).clickable { state.searching=true }.padding(horizontal=20.dp), verticalAlignment=Alignment.CenterVertically) {
                                    Text("⌕", fontSize=32.sp)
                                    Spacer(Modifier.width(12.dp))
                                    Text("搜索", fontSize=22.sp, color=MaterialTheme.colorScheme.onSurface.copy(alpha=.48f))
                                }
                            }
                            Box {
                                TextButton(onClick={viewOpen=true}) { Text("▦", fontSize=27.sp, color=MaterialTheme.colorScheme.onSurface) }
                            }
                            Box {
                                TextButton(onClick={filterOpen=true}) { Text("≡", fontSize=29.sp, color=MaterialTheme.colorScheme.onSurface) }
                            }
                            Box {
                                TextButton(onClick={overflowOpen=true}) { Text("⋮", fontSize=30.sp, color=MaterialTheme.colorScheme.onSurface) }
                            }
                        }
                    }
                }
            }

            if (state.searching && state.query.isNotBlank()) {
                AgendaView(state, state.query)
            } else {
                when (state.view) {
                    ViewMode.DAY -> DayView(state)
                    ViewMode.WEEK -> WeekView(state)
                    ViewMode.MONTH -> MonthView(state)
                    ViewMode.MONTH_DAY -> MonthDayView(state)
                    ViewMode.YEAR -> YearView(state)
                    ViewMode.AGENDA -> AgendaView(state, "")
                }
            }
        }

        Surface(
            modifier=Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(72.dp),
            tonalElevation=2.dp, color=MaterialTheme.colorScheme.surface
        ) {
            Row(Modifier.fillMaxSize().padding(horizontal=10.dp), verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.SpaceEvenly) {
                state.calendars.forEach { cal ->
                    TextButton(onClick={ state.toggleCalendar(cal.id) }) {
                        Box(Modifier.size(10.dp).clip(CircleShape).background(Color(cal.color)))
                        Spacer(Modifier.width(5.dp))
                        Text(cal.name, color=if(cal.visible) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha=.35f), fontWeight=if(cal.visible) FontWeight.SemiBold else FontWeight.Normal)
                    }
                }
            }
        }

        Box(Modifier.align(Alignment.BottomEnd).padding(end=20.dp, bottom=88.dp)) {
            LargeFloatingActionButton(onClick={addOpen=true}, shape=RoundedCornerShape(24.dp), containerColor=MaterialTheme.colorScheme.primary) {
                Text("+", fontSize=38.sp)
            }
        }
    }

    if (overflowOpen) {
        AlertDialog(
            onDismissRequest={overflowOpen=false},
            title={Text("更多")},
            text={
                Column {
                    listOf("转到日期","打印","添加节假日","添加联系人生日","添加联系人纪念日","设置","关于").forEach { label ->
                        Row(Modifier.fillMaxWidth().clickable {
                            overflowOpen=false
                            when(label) {
                                "转到日期" -> gotoOpen=true
                                "打印" -> Toast.makeText(context, "已准备当前日历用于打印", Toast.LENGTH_SHORT).show()
                                "添加节假日" -> holidayOpen=true
                                "添加联系人生日", "添加联系人纪念日" -> birthdayOpen=true
                                "设置" -> state.page=Page.SETTINGS
                                "关于" -> state.page=Page.ABOUT
                            }
                        }.padding(vertical=11.dp), verticalAlignment=Alignment.CenterVertically) {
                            Text(label, fontSize=19.sp)
                        }
                    }
                }
            },
            confirmButton={}
        )
    }
    if (addOpen) {
        AlertDialog(
            onDismissRequest={addOpen=false},
            title={Text("新建")},
            text={
                Column {
                    if (state.allowTasks) {
                        Row(Modifier.fillMaxWidth().clickable {addOpen=false; state.startNew(ItemKind.TASK)}.padding(vertical=16.dp), verticalAlignment=Alignment.CenterVertically) {
                            Text("◉", fontSize=27.sp); Spacer(Modifier.width(18.dp)); Text("任务", fontSize=21.sp)
                        }
                    }
                    Row(Modifier.fillMaxWidth().clickable {addOpen=false; state.startNew(ItemKind.EVENT)}.padding(vertical=16.dp), verticalAlignment=Alignment.CenterVertically) {
                        Text("▣", fontSize=27.sp); Spacer(Modifier.width(18.dp)); Text("活动", fontSize=21.sp)
                    }
                }
            },
            confirmButton={}
        )
    }
    if (viewOpen) {
        AlertDialog(onDismissRequest={viewOpen=false}, confirmButton={}, text={
            Column {
                ViewMode.entries.forEach { mode ->
                    Row(Modifier.fillMaxWidth().clickable { state.view=mode; viewOpen=false }.padding(vertical=10.dp), verticalAlignment=Alignment.CenterVertically) {
                        RadioButton(selected=state.view==mode, onClick={state.view=mode; viewOpen=false})
                        Text(mode.label, fontSize=19.sp)
                    }
                }
            }
        })
    }
    if (filterOpen) {
        AlertDialog(
            onDismissRequest={filterOpen=false}, title={Text("显示的日历")},
            text={Column { state.calendars.forEach { cal ->
                Row(Modifier.fillMaxWidth().clickable { state.toggleCalendar(cal.id) }.padding(vertical=7.dp), verticalAlignment=Alignment.CenterVertically) {
                    Checkbox(cal.visible, onCheckedChange={state.toggleCalendar(cal.id)})
                    Box(Modifier.size(15.dp).clip(CircleShape).background(Color(cal.color)))
                    Spacer(Modifier.width(12.dp)); Text(cal.name, fontSize=18.sp)
                }
            }}},
            confirmButton={TextButton(onClick={filterOpen=false}){Text("确定")}},
            dismissButton={TextButton(onClick={filterOpen=false}){Text("取消")}}
        )
    }
    if (gotoOpen) {
        var year by remember { mutableIntStateOf(state.selectedDate.year) }
        var month by remember { mutableIntStateOf(state.selectedDate.monthValue) }
        AlertDialog(onDismissRequest={gotoOpen=false}, title={Text("转到日期")}, text={
            Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceEvenly) {
                CounterPicker(year.toString(), {year--}, {year++})
                CounterPicker(chineseMonths[month-1], {month=if(month==1)12 else month-1}, {month=if(month==12)1 else month+1})
            }
        }, confirmButton={TextButton(onClick={state.selectedDate=LocalDate.of(year,month,1);gotoOpen=false}){Text("确定")}},
            dismissButton={TextButton(onClick={gotoOpen=false}){Text("取消")}})
    }
    if (holidayOpen) {
        val countries=listOf("Argentina","Australia","Belgique","Bolivia","Brasil","Canada","Colombia","Costa Rica","Cộng hòa Xã hội chủ nghĩa Việt Nam","Danmark","Deutschland","Eesti","España","France","Guatemala")
        AlertDialog(onDismissRequest={holidayOpen=false}, confirmButton={}, text={
            LazyColumn(Modifier.heightIn(max=610.dp)) { lazyItems(countries.indices.toList()) { index ->
                Row(Modifier.fillMaxWidth().clickable {
                    state.addCalendar(countries[index]+" 节假日", 0xFFE25656); holidayOpen=false
                }.padding(vertical=10.dp), verticalAlignment=Alignment.CenterVertically) {
                    RadioButton(false, onClick=null); Text(countries[index], fontSize=18.sp)
                }
            }}
        })
    }
    if (birthdayOpen) {
        var automatic by remember { mutableStateOf(false) }
        AlertDialog(onDismissRequest={birthdayOpen=false}, title={Text("活动提醒")}, text={
            Column {
                Row(verticalAlignment=Alignment.CenterVertically) { Text("♟", fontSize=24.sp); Spacer(Modifier.width(16.dp)); Text("不再提醒", fontSize=19.sp) }
                Spacer(Modifier.height(18.dp))
                Row(Modifier.clickable {automatic=!automatic}, verticalAlignment=Alignment.CenterVertically) {
                    Checkbox(automatic,{automatic=it}); Text("自动添加新的生日或纪念日", fontSize=17.sp)
                }
            }
        }, confirmButton={TextButton(onClick={birthdayOpen=false;Toast.makeText(context,"通讯录中没有可导入的日期",Toast.LENGTH_SHORT).show()}){Text("确定")}},
            dismissButton={TextButton(onClick={birthdayOpen=false}){Text("取消")}})
    }
}

@Composable
private fun CounterPicker(label:String, minus:()->Unit, plus:()->Unit) {
    Column(horizontalAlignment=Alignment.CenterHorizontally) {
        TextButton(onClick=plus){Text("▲")}
        Text(label, fontSize=22.sp, modifier=Modifier.padding(12.dp))
        TextButton(onClick=minus){Text("▼")}
    }
}

@Composable
private fun PeriodHeader(state:PlannerState, title:String, previous:()->Unit, next:()->Unit) {
    Row(Modifier.fillMaxWidth().height(58.dp), verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.SpaceBetween) {
        TextButton(onClick=previous){Text("‹", fontSize=38.sp)}
        Text(title, fontSize=27.sp, fontWeight=FontWeight.Medium)
        TextButton(onClick=next){Text("›", fontSize=38.sp)}
    }
}

@Composable
private fun MonthView(state:PlannerState) {
    Column(Modifier.fillMaxSize()) {
        PeriodHeader(state, monthName(state.selectedDate), {state.selectedDate=state.selectedDate.minusMonths(1)}, {state.selectedDate=state.selectedDate.plusMonths(1)})
        MonthGrid(state, Modifier.weight(1f), false)
    }
}

@Composable
private fun MonthGrid(state:PlannerState, modifier:Modifier=Modifier, compact:Boolean) {
    val first=state.selectedDate.withDayOfMonth(1)
    val offset=if(state.weekStartsMonday) first.dayOfWeek.value-1 else first.dayOfWeek.value%7
    val start=first.minusDays(offset.toLong())
    val names=if(state.weekStartsMonday) weekNames else listOf("周日","周一","周二","周三","周四","周五","周六")
    Column(modifier) {
        Row(Modifier.fillMaxWidth().height(if(compact)28.dp else 42.dp)) {
            names.forEachIndexed { index, name ->
                Text(name, modifier=Modifier.weight(1f), fontSize=if(compact)13.sp else 17.sp,
                    color=if(index>=5 && state.highlightWeekends) Color(0xFFD94A4A) else MaterialTheme.colorScheme.onSurface,
                    textAlign=androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
        repeat(6) { row ->
            Row(Modifier.fillMaxWidth().weight(1f)) {
                repeat(7) { col ->
                    val date=start.plusDays((row*7+col).toLong())
                    val dayItems=state.itemsOn(date)
                    val selected=date==state.selectedDate
                    Column(
                        Modifier.weight(1f).fillMaxHeight().clickable {state.selectedDate=date}
                            .padding(horizontal=2.dp, vertical=1.dp), horizontalAlignment=Alignment.CenterHorizontally
                    ) {
                        Box(Modifier.size(if(compact)30.dp else 34.dp).clip(CircleShape)
                            .background(if(selected) MaterialTheme.colorScheme.primary else Color.Transparent), contentAlignment=Alignment.Center) {
                            Text(date.dayOfMonth.toString(), fontSize=if(compact)13.sp else 17.sp,
                                color=when { selected -> Color.White; date.monthValue!=first.monthValue -> MaterialTheme.colorScheme.onSurface.copy(alpha=.35f); else -> MaterialTheme.colorScheme.onSurface })
                        }
                        if(!compact) dayItems.take(3).forEach { item ->
                            val cal=state.calendar(item.calendarId)
                            Text((if(item.kind==ItemKind.TASK)"✓ " else "")+item.title,
                                modifier=Modifier.fillMaxWidth().padding(vertical=1.dp).clip(RoundedCornerShape(4.dp)).background(Color(cal.color).copy(alpha=.75f)).padding(horizontal=2.dp),
                                fontSize=10.sp, maxLines=1, overflow=TextOverflow.Ellipsis,
                                textDecoration=if(item.done)TextDecoration.LineThrough else TextDecoration.None)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayView(state:PlannerState) {
    Column(Modifier.fillMaxSize()) {
        PeriodHeader(state, dateTitle(state.selectedDate), {state.selectedDate=state.selectedDate.minusDays(1)}, {state.selectedDate=state.selectedDate.plusDays(1)})
        DateItems(state, state.selectedDate, Modifier.fillMaxSize())
    }
}

@Composable
private fun DateItems(state:PlannerState, date:LocalDate, modifier:Modifier=Modifier) {
    val dayItems=state.itemsOn(date)
    LazyColumn(modifier.padding(horizontal=16.dp)) {
        item { Text(monthName(date), fontSize=31.sp, color=MaterialTheme.colorScheme.primary, modifier=Modifier.padding(vertical=8.dp)) }
        item { Text(date.dayOfMonth.toString()+" "+weekNames[date.dayOfWeek.value-1], color=MaterialTheme.colorScheme.primary, fontSize=17.sp, modifier=Modifier.padding(start=16.dp,bottom=8.dp)) }
        if(dayItems.isEmpty()) item { Text("这一天没有活动", modifier=Modifier.fillMaxWidth().padding(48.dp), color=MaterialTheme.colorScheme.onSurface.copy(alpha=.5f)) }
        lazyItems(dayItems, key={it.id}) { item -> EventCard(state,item) }
    }
}

@Composable
private fun EventCard(state:PlannerState, item:PlannerItem) {
    val cal=state.calendar(item.calendarId)
    Row(
        Modifier.fillMaxWidth().padding(vertical=5.dp).heightIn(min=82.dp).clip(RoundedCornerShape(20.dp))
            .border(1.dp,Color(cal.color).copy(alpha=.65f),RoundedCornerShape(20.dp)).clickable {state.editorItem=item;state.editorIsNew=false}
    ) {
        Box(Modifier.width(15.dp).fillMaxHeight().background(Color(cal.color)))
        Column(Modifier.padding(horizontal=12.dp,vertical=8.dp)) {
            Text((if(item.kind==ItemKind.TASK)"◉ " else "")+item.title, fontSize=21.sp,
                textDecoration=if(item.done)TextDecoration.LineThrough else TextDecoration.None,
                color=MaterialTheme.colorScheme.onSurface.copy(alpha=if(item.done).55f else 1f))
            Text(if(item.allDay)"全天" else item.time.format(DateTimeFormatter.ofPattern("HH:mm")), fontSize=16.sp, color=MaterialTheme.colorScheme.onSurface.copy(alpha=.58f))
            if(item.description.isNotBlank()) Text(item.description, fontSize=15.sp, color=MaterialTheme.colorScheme.onSurface.copy(alpha=.55f))
            else if(item.location.isNotBlank()) Text(item.location, fontSize=15.sp, color=MaterialTheme.colorScheme.onSurface.copy(alpha=.55f))
        }
    }
}

@Composable
private fun MonthDayView(state:PlannerState) {
    Column(Modifier.fillMaxSize()) {
        MonthGrid(state, Modifier.fillMaxWidth().height(390.dp), true)
        HorizontalDivider()
        DateItems(state,state.selectedDate,Modifier.weight(1f))
    }
}

@Composable
private fun WeekView(state:PlannerState) {
    val monday=state.selectedDate.minusDays((state.selectedDate.dayOfWeek.value-1).toLong())
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().height(72.dp)) {
            Column(Modifier.width(56.dp).padding(5.dp)) { Text(monthName(state.selectedDate)); Text("周 "+state.selectedDate.get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear())) }
            repeat(7) { i ->
                val d=monday.plusDays(i.toLong())
                Column(Modifier.weight(1f).clickable {state.selectedDate=d}, horizontalAlignment=Alignment.CenterHorizontally) {
                    Text(weekNames[i], fontSize=14.sp, color=if(d==state.selectedDate)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                    Box(Modifier.size(32.dp).clip(CircleShape).background(if(d==state.selectedDate)MaterialTheme.colorScheme.primary else Color.Transparent), contentAlignment=Alignment.Center) {
                        Text(d.dayOfMonth.toString(), color=if(d==state.selectedDate)Color.White else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
        HorizontalDivider()
        LazyColumn(Modifier.weight(1f)) {
            lazyItems((0 until 14).toList()) { index ->
                val hour=7+index
                Row(Modifier.fillMaxWidth().height(64.dp)) {
                    Text(hour.toString().padStart(2,'0')+":00",Modifier.width(58.dp).padding(6.dp),fontSize=14.sp)
                    repeat(7) { i ->
                        val d=monday.plusDays(i.toLong())
                        val atHour=state.itemsOn(d).filter { !it.allDay && it.time.hour==hour }
                        Box(Modifier.weight(1f).fillMaxHeight().border(.3.dp,MaterialTheme.colorScheme.onSurface.copy(alpha=.18f))) {
                            atHour.take(1).forEach { e ->
                                Text(e.title, Modifier.fillMaxWidth().background(Color(state.calendar(e.calendarId).color).copy(alpha=.72f)).padding(2.dp), fontSize=9.sp, maxLines=3)
                            }
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().height(45.dp).padding(horizontal=24.dp),verticalAlignment=Alignment.CenterVertically) {
            Slider(1f,{ },Modifier.weight(1f),enabled=false); Text("7 天",fontSize=20.sp)
        }
    }
}

@Composable
private fun YearView(state:PlannerState) {
    Column {
        PeriodHeader(state,state.selectedDate.year.toString(),{state.selectedDate=state.selectedDate.minusYears(1)},{state.selectedDate=state.selectedDate.plusYears(1)})
        Column(Modifier.weight(1f)) {
            repeat(4) { row ->
                Row(Modifier.fillMaxWidth().weight(1f)) {
                    repeat(3) { col ->
                        val month=row*3+col+1
                        MiniMonth(state,state.selectedDate.year,month,Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniMonth(state:PlannerState, year:Int, month:Int, modifier:Modifier) {
    val first=LocalDate.of(year,month,1)
    val offset=first.dayOfWeek.value-1
    Column(modifier.clickable {state.selectedDate=first;state.view=ViewMode.MONTH}.padding(7.dp),horizontalAlignment=Alignment.CenterHorizontally) {
        Text(chineseMonths[month-1],fontSize=18.sp,color=if(month==state.selectedDate.monthValue)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
        repeat(6) { row ->
            Row(Modifier.fillMaxWidth()) {
                repeat(7) { col ->
                    val n=row*7+col-offset+1
                    Text(if(n in 1..first.lengthOfMonth())n.toString() else "",Modifier.weight(1f),fontSize=9.sp,textAlign=androidx.compose.ui.text.style.TextAlign.Center,
                        color=if(n==state.selectedDate.dayOfMonth && month==state.selectedDate.monthValue)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha=.65f))
                }
            }
        }
    }
}

@Composable
private fun AgendaView(state:PlannerState, query:String) {
    val dates=remember(state.items.size,state.calendars.toList(),query) {
        val q=query.trim().lowercase()
        val direct=state.visibleItems().filter { q.isBlank() || it.title.lowercase().contains(q) || it.description.lowercase().contains(q) || it.location.lowercase().contains(q) }
        direct.flatMap { item ->
            if(item.repeat.isBlank()) listOf(item.date)
            else (0..180).map { state.selectedDate.minusDays(30).plusDays(it.toLong()) }.filter { state.occursOn(item,it) }
        }.distinct().sorted()
    }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal=16.dp)) {
        if(dates.isEmpty()) item { Text("没有找到活动或任务",Modifier.fillMaxWidth().padding(48.dp),color=MaterialTheme.colorScheme.onSurface.copy(alpha=.55f)) }
        lazyItems(dates) { date ->
            Text(monthName(date),fontSize=28.sp,color=MaterialTheme.colorScheme.primary,modifier=Modifier.padding(top=16.dp,bottom=6.dp))
            Text(date.dayOfMonth.toString()+" "+weekNames[date.dayOfWeek.value-1],fontSize=17.sp,color=MaterialTheme.colorScheme.primary,modifier=Modifier.padding(start=16.dp,bottom=6.dp))
            state.itemsOn(date).filter { query.isBlank() || it.title.contains(query,true) || it.description.contains(query,true) || it.location.contains(query,true) }.forEach { EventCard(state,it) }
        }
    }
}

@Composable
private fun ItemEditor(state:PlannerState, original:PlannerItem) {
    var item by remember(original.id) { mutableStateOf(original) }
    var deleteConfirm by remember { mutableStateOf(false) }
    var repeatMenu by remember { mutableStateOf(false) }
    var calendarMenu by remember { mutableStateOf(false) }
    var statusMenu by remember { mutableStateOf(false) }
    var reminderMenu by remember { mutableStateOf(false) }
    val context=LocalContext.current
    val isEvent=item.kind==ItemKind.EVENT

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title={Text(if(state.editorIsNew) if(isEvent)"新活动" else "新任务" else if(isEvent)"编辑活动" else "编辑任务")},
            navigationIcon={TextButton(onClick={state.editorItem=null}){Text("←",fontSize=27.sp)}},
            actions={
                IconButton(onClick={
                    if(item.title.isBlank()) Toast.makeText(context,"请输入标题",Toast.LENGTH_SHORT).show()
                    else state.saveItem(item)
                }){Text("✓",fontSize=27.sp)}
                if(!state.editorIsNew) IconButton(onClick={deleteConfirm=true}){Text("▣",fontSize=24.sp)}
                if(!state.editorIsNew) IconButton(onClick={ state.duplicate(item) }){Text("▢",fontSize=28.sp)}
                IconButton(onClick={Toast.makeText(context,"可分享此活动",Toast.LENGTH_SHORT).show()}){Text("⋮",fontSize=28.sp)}
            }
        )
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom=24.dp)) {
            OutlinedTextField(item.title,{item=item.copy(title=it)},Modifier.fillMaxWidth().padding(horizontal=20.dp,vertical=7.dp),label={Text("标题")},singleLine=true)
            if(isEvent) OutlinedTextField(item.location,{item=item.copy(location=it)},Modifier.fillMaxWidth().padding(horizontal=20.dp,vertical=7.dp),label={Text("位置")},trailingIcon={Text("⌖")},singleLine=true)
            OutlinedTextField(item.description,{item=item.copy(description=it)},Modifier.fillMaxWidth().padding(horizontal=20.dp,vertical=7.dp),label={Text("描述")},minLines=2)
            SettingRow("◷","全天",right={Switch(item.allDay,{item=item.copy(allDay=it)})})
            SettingRow("","开始日期",item.date.toString(),onClick={
                DatePickerDialog(context,{_,y,m,d->item=item.copy(date=LocalDate.of(y,m+1,d))},item.date.year,item.date.monthValue-1,item.date.dayOfMonth).show()
            })
            if(!item.allDay) SettingRow("","开始时间",item.time.format(DateTimeFormatter.ofPattern("HH:mm")),onClick={
                TimePickerDialog(context,{_,h,m->item=item.copy(time=LocalTime.of(h,m))},item.time.hour,item.time.minute,true).show()
            })
            if(isEvent) {
                SettingRow("","结束日期",item.date.toString(),onClick={
                    DatePickerDialog(context,{_,y,m,d->item=item.copy(date=LocalDate.of(y,m+1,d))},item.date.year,item.date.monthValue-1,item.date.dayOfMonth).show()
                })
                if(!item.allDay) SettingRow("","结束时间",item.endTime.format(DateTimeFormatter.ofPattern("HH:mm")),onClick={
                    TimePickerDialog(context,{_,h,m->item=item.copy(endTime=LocalTime.of(h,m))},item.endTime.hour,item.endTime.minute,true).show()
                })
            }
            Box {
                SettingRow("♟","提醒",if(item.reminder==0)"不提醒" else item.reminder.toString()+" 分钟前",onClick={reminderMenu=true})
                DropdownMenu(reminderMenu,{reminderMenu=false}) {
                    listOf(0,5,10,15,30,60,720,1440).forEach { minutes ->
                        DropdownMenuItem(text={Text(if(minutes==0)"不提醒" else minutes.toString()+" 分钟前")},onClick={item=item.copy(reminder=minutes);reminderMenu=false})
                    }
                }
            }
            if(isEvent) SettingRow("＋","添加其他提醒","")
            Box {
                SettingRow("⟳","重复",repeatLabel(item.repeat),onClick={repeatMenu=true})
                DropdownMenu(repeatMenu,{repeatMenu=false}) {
                    listOf("不重复" to "","每天" to "FREQ=DAILY;INTERVAL=1","每周" to "FREQ=WEEKLY;BYDAY="+when(item.date.dayOfWeek.value){1->"MO";2->"TU";3->"WE";4->"TH";5->"FR";6->"SA";else->"SU"},"每月" to "FREQ=MONTHLY;BYMONTHDAY="+item.date.dayOfMonth,"每年" to "FREQ=YEARLY;BYMONTH="+item.date.monthValue+";BYMONTHDAY="+item.date.dayOfMonth).forEach { option ->
                        DropdownMenuItem(text={Text(option.first)},onClick={item=item.copy(repeat=option.second);repeatMenu=false})
                    }
                }
            }
            if(isEvent) Box {
                SettingRow("☷","状态",item.status,onClick={statusMenu=true})
                DropdownMenu(statusMenu,{statusMenu=false}) {
                    listOf("暂定","已确认","已取消").forEach { s -> DropdownMenuItem(text={Text(s)},onClick={item=item.copy(status=s);statusMenu=false}) }
                }
            }
            Box {
                SettingRow("▣","日历",state.calendar(item.calendarId).name,onClick={calendarMenu=true})
                DropdownMenu(calendarMenu,{calendarMenu=false}) {
                    state.calendars.forEach { cal -> DropdownMenuItem(text={Text(cal.name)},onClick={item=item.copy(calendarId=cal.id);calendarMenu=false}) }
                }
            }
            if(isEvent) SettingRow("◉","活动颜色","",right={Box(Modifier.size(34.dp).clip(CircleShape).background(Color(state.calendar(item.calendarId).color)))})
            if(item.kind==ItemKind.TASK && !state.editorIsNew) {
                Spacer(Modifier.height(22.dp))
                FilledTonalButton(onClick={ state.toggleDone(item) },Modifier.fillMaxWidth().padding(horizontal=22.dp)) {
                    Text(if(item.done)"标记为未完成" else "标记为已完成",fontSize=18.sp)
                }
            }
        }
    }

    if(deleteConfirm) {
        AlertDialog(onDismissRequest={deleteConfirm=false},text={Text("确定要继续删除吗？",fontSize=20.sp)},
            confirmButton={TextButton(onClick={state.deleteItem(item.id)}){Text("是")}},
            dismissButton={TextButton(onClick={deleteConfirm=false}){Text("否")}})
    }
}

@Composable
private fun SettingRow(icon:String,title:String,subtitle:String="",onClick:(()->Unit)?=null,right:(@Composable ()->Unit)?=null) {
    Row(
        Modifier.fillMaxWidth().heightIn(min=70.dp).then(if(onClick!=null)Modifier.clickable(onClick=onClick)else Modifier).padding(horizontal=22.dp,vertical=10.dp),
        verticalAlignment=Alignment.CenterVertically
    ) {
        Text(icon,Modifier.width(42.dp),fontSize=24.sp)
        Column(Modifier.weight(1f)) { Text(title,fontSize=18.sp); if(subtitle.isNotBlank())Text(subtitle,fontSize=14.sp,color=MaterialTheme.colorScheme.onSurface.copy(alpha=.6f)) }
        if(right!=null) right() else if(onClick!=null) Text("›",fontSize=28.sp,color=MaterialTheme.colorScheme.onSurface.copy(alpha=.5f))
    }
    HorizontalDivider(color=MaterialTheme.colorScheme.onSurface.copy(alpha=.08f))
}

@Composable
private fun SettingsPage(state:PlannerState) {
    var autoBackup by remember { mutableStateOf(false) }
    var vibrate by remember { mutableStateOf(false) }
    var caldav by remember { mutableStateOf(false) }
    val context=LocalContext.current
    Column {
        SimpleTopBar("设置"){state.page=Page.CALENDAR}
        LazyColumn(Modifier.weight(1f)) {
            item {
                SectionLabel("外观")
                SettingRow("◉","自定义外观","主题、图标颜色和字体"){state.page=Page.APPEARANCE}
                SettingRow("文","语言","中文")
                SettingRow("▣","管理日历","创建、编辑、删除本地日历"){state.page=Page.CALENDARS}
                SettingRow("1","一周开始于",if(state.weekStartsMonday)"周一" else "周日",onClick={state.weekStartsMonday=!state.weekStartsMonday;state.persist()})
                SettingRow("24","使用24小时制",right={Switch(true,null)})
                SettingRow("","突出显示周末",right={Switch(state.highlightWeekends,{state.highlightWeekends=it;state.persist()})})
                SectionLabel("提醒")
                SettingRow("♟","自定义通知")
                SettingRow("♫","音频流","通知")
                SettingRow("","振动",right={Switch(vibrate,{vibrate=it})})
                SettingRow("","重复提醒直到关闭",right={Switch(false,null)})
                SettingRow("","总是使用相同的延迟时间","10 分钟")
                SectionLabel("同步")
                SettingRow("↻","CalDAV 同步",if(caldav)"已启用（没有可同步的系统日历）" else "关闭",right={Switch(caldav,{caldav=it;if(it)Toast.makeText(context,"没有找到可同步的日历",Toast.LENGTH_SHORT).show()})})
                SectionLabel("日历视图")
                SettingRow("","月视图中显示周数",right={Switch(state.showWeekNumbers,{state.showWeekNumbers=it;state.persist()})})
                SettingRow("","显示月视图网格",right={Switch(false,null)})
                SettingRow("","活动列表显示描述和位置",right={Switch(true,null)})
                SectionLabel("任务")
                SettingRow("✓","允许任务",right={Switch(state.allowTasks,{state.allowTasks=it;state.persist()})})
                SettingRow("","淡化已完成的任务",right={Switch(true,null)})
                SectionLabel("备份与迁移")
                SettingRow("⇩","自动备份","关闭",right={Switch(autoBackup,{autoBackup=it})})
                SettingRow("⇧","导出活动到 .ics","包括活动、任务和日历",onClick={Toast.makeText(context,"已生成日历_20260911.ics",Toast.LENGTH_SHORT).show()})
                SettingRow("⇩","从 .ics 导入活动","选择一个日历文件",onClick={Toast.makeText(context,"没有选择文件",Toast.LENGTH_SHORT).show()})
                SettingRow("⇧","导出设置","")
                SettingRow("⇩","导入设置","")
            }
        }
    }
}

@Composable
private fun SectionLabel(text:String) {
    Text(text,color=MaterialTheme.colorScheme.primary,fontSize=16.sp,modifier=Modifier.fillMaxWidth().padding(start=56.dp,top=20.dp,bottom=8.dp))
}

@Composable
private fun SimpleTopBar(title:String, back:()->Unit) {
    TopAppBar(title={Text(title,fontSize=26.sp)},navigationIcon={TextButton(onClick=back){Text("←",fontSize=28.sp)}})
}

@Composable
private fun AppearancePage(state:PlannerState) {
    var font by remember { mutableStateOf("中等") }
    Column {
        SimpleTopBar("自定义外观"){state.page=Page.SETTINGS}
        LazyColumn {
            item {
                SectionLabel("应用主题")
                listOf("跟随系统","浅色","深色","深红","白色","黑白","自定义").forEach { label ->
                    Row(Modifier.fillMaxWidth().clickable {
                        state.darkTheme=label=="深色"||label=="深红"||label=="黑白"
                        state.persist()
                    }.padding(horizontal=24.dp,vertical=11.dp),verticalAlignment=Alignment.CenterVertically) {
                        RadioButton(selected=(state.darkTheme && label=="深色")||(!state.darkTheme&&label=="跟随系统"),onClick=null)
                        Text(label,fontSize=18.sp)
                    }
                }
                SectionLabel("颜色")
                SettingRow("●","应用图标颜色","#7E94D6",right={Row { listOf(Color(0xFFDE3333),Color(0xFF3E75D8),Color(0xFF43A66D),Color(0xFFF1B644),Color(0xFFE7863B)).forEach { c -> Box(Modifier.padding(3.dp).size(25.dp).clip(CircleShape).background(c)) } }})
                SettingRow("A","字体",font,onClick={font=if(font=="中等")"大" else "中等"})
            }
        }
    }
}

@Composable
private fun CalendarsPage(state:PlannerState) {
    var adding by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var editId by remember { mutableStateOf<String?>(null) }
    Column {
        TopAppBar(title={Text("管理日历",fontSize=26.sp)},navigationIcon={TextButton(onClick={state.page=Page.SETTINGS}){Text("←",fontSize=28.sp)}},actions={IconButton(onClick={adding=true}){Text("+",fontSize=30.sp)}})
        LazyColumn {
            lazyItems(state.calendars,key={it.id}) { cal ->
                Row(Modifier.fillMaxWidth().height(76.dp).padding(horizontal=20.dp),verticalAlignment=Alignment.CenterVertically) {
                    Box(Modifier.size(22.dp).clip(CircleShape).background(Color(cal.color)))
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)){Text(cal.name,fontSize=20.sp);Text(if(cal.id.startsWith("cal_"))"样本日历" else "本地日历",color=MaterialTheme.colorScheme.onSurface.copy(alpha=.55f))}
                    Box {
                        TextButton(onClick={editId=cal.id}){Text("⋮",fontSize=28.sp)}
                        DropdownMenu(editId==cal.id,{editId=null}) {
                            DropdownMenuItem(text={Text("编辑")},onClick={editId=null;name=cal.name;adding=true})
                            DropdownMenuItem(text={Text("删除")},onClick={editId=null;state.deleteCalendar(cal.id)})
                        }
                    }
                }
                HorizontalDivider()
            }
        }
    }
    if(adding) {
        AlertDialog(onDismissRequest={adding=false},title={Text("添加日历")},text={
            Column {
                OutlinedTextField(name,{name=it},label={Text("标题")},singleLine=true)
                Spacer(Modifier.height(18.dp))
                Row { listOf(0xFFDE3333,0xFF3E75D8,0xFF43A66D,0xFFF1B644,0xFFE7863B).forEach { c -> Box(Modifier.padding(6.dp).size(32.dp).clip(CircleShape).background(Color(c))) } }
            }
        },confirmButton={TextButton(onClick={if(name.isNotBlank())state.addCalendar(name,0xFF7E9CFF);name="";adding=false}){Text("确定")}},
            dismissButton={TextButton(onClick={adding=false}){Text("取消")}})
    }
}

@Composable
private fun AboutPage(state:PlannerState) {
    Column {
        SimpleTopBar("关于"){state.page=Page.CALENDAR}
        LazyColumn {
            item {
                SectionLabel("支持")
                SettingRow("?","常见问题")
                SettingRow("♟","已知问题")
                SettingRow("?","hello@fossify.org")
                SectionLabel("帮助我们")
                SettingRow("⌯","分享给好友")
                SettingRow("♣","贡献者")
                SettingRow("♡","向 Fossify 捐赠")
                SectionLabel("社交网络")
                SettingRow("●","GitHub")
                SettingRow("●","Reddit")
                SettingRow("●","Telegram")
                SectionLabel("其他")
                SettingRow("@","隐私政策")
                SettingRow("▤","第三方许可")
                SettingRow("ⓘ","版本 1.10.3","org.fossify.calendar")
            }
        }
    }
}
