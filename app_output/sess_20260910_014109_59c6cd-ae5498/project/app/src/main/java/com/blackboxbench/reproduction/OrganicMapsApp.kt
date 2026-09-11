package com.blackboxbench.reproduction

import android.app.Activity
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import kotlinx.coroutines.delay

private val OrganicGreen = Color(0xFF00843D)
private val MapBlue = Color(0xFF83D0DE)
private val ActionBlue = Color(0xFF2196F3)
private val RoutePurple = Color(0xFF7247CE)
private val WarmMap = Color(0xFFE8E3D2)
private val ParkGreen = Color(0xFFCCD79A)
private val RoadOrange = Color(0xFFFF6B20)

@Composable
fun OrganicMapsApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("organic_repro", Context.MODE_PRIVATE) }
    var onboarded by rememberSaveable { mutableStateOf(prefs.getBoolean("onboarded", false)) }
    var dark by rememberSaveable { mutableStateOf(prefs.getBoolean("dark", false)) }
    var saved by rememberSaveable { mutableStateOf(prefs.getBoolean("saved", false)) }
    var note by rememberSaveable { mutableStateOf(prefs.getString("note", "") ?: "") }
    var newList by rememberSaveable { mutableStateOf(prefs.getBoolean("new_list", false)) }
    var historyHotel by rememberSaveable { mutableStateOf(prefs.getBoolean("history_hotel", false)) }
    var screen by rememberSaveable { mutableStateOf("map") }
    var overlay by rememberSaveable { mutableStateOf("") }
    var recording by rememberSaveable { mutableStateOf(false) }
    var selectedStyle by rememberSaveable { mutableStateOf("户外") }
    var routeMode by rememberSaveable { mutableStateOf("驾车") }
    var mapPoint by rememberSaveable { mutableStateOf(false) }
    val view = LocalView.current
    SideEffect {
        val window = (view.context as? Activity)?.window
        if (window != null) {
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !dark
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !dark
        }
    }

    BenchmarkAppTheme(dark) {
        if (!onboarded) {
            OnboardingScreen {
                onboarded = true
                prefs.edit().putBoolean("onboarded", true).apply()
            }
        } else {
            BackHandler(enabled = screen != "map" || overlay.isNotEmpty()) {
                when {
                    overlay.isNotEmpty() -> overlay = ""
                    screen == "bookmarkList" -> screen = "bookmarks"
                    screen == "editBookmark" -> screen = "place"
                    screen == "activeNav" -> screen = "route"
                    screen == "searchResults" -> screen = "search"
                    else -> screen = "map"
                }
            }

            when (screen) {
                "map" -> MapHome(
                    overlay = overlay,
                    setOverlay = { overlay = it },
                    recording = recording,
                    selectedStyle = selectedStyle,
                    onStyle = { selectedStyle = it },
                    onSearch = { screen = "search" },
                    onBookmarks = { screen = "bookmarks" },
                    onAbout = { screen = "about" },
                    onDownloads = { screen = "downloads" },
                    onSettings = { screen = "settings" },
                    onRecord = { recording = true; overlay = "" },
                    onDeleteTrack = { recording = false; overlay = "" },
                    onAddPlace = { screen = "addPlace"; overlay = "" },
                    onShare = { overlay = "share" },
                    onMapPoint = { mapPoint = true; screen = "place" }
                )
                "search" -> SearchScreen(
                    historyHotel = historyHotel,
                    onBack = { screen = "map" },
                    onHotel = {
                        historyHotel = true
                        prefs.edit().putBoolean("history_hotel", true).apply()
                        screen = "searchResults"
                    },
                    onTextSearch = { screen = "searchResults" },
                    onClearHistory = {
                        historyHotel = false
                        prefs.edit().putBoolean("history_hotel", false).apply()
                    }
                )
                "searchResults" -> SearchResultsScreen(
                    onBack = { screen = "search" },
                    onPlace = { mapPoint = false; screen = "place" }
                )
                "place" -> PlaceScreen(
                    mapPoint = mapPoint,
                    saved = saved,
                    note = note,
                    onClose = { screen = "map" },
                    onSave = {
                        saved = !saved
                        prefs.edit().putBoolean("saved", saved).apply()
                    },
                    onEdit = { screen = "editBookmark" },
                    onRoute = { routeMode = "驾车"; screen = "route" }
                )
                "editBookmark" -> EditBookmarkScreen(
                    note = note,
                    onNote = {
                        note = it
                        prefs.edit().putString("note", it).apply()
                    },
                    onBack = { screen = "place" },
                    onDelete = {
                        saved = false
                        note = ""
                        prefs.edit().putBoolean("saved", false).remove("note").apply()
                        screen = "map"
                    }
                )
                "bookmarks" -> BookmarksOverview(
                    saved = saved,
                    newList = newList,
                    onBack = { screen = "map" },
                    onCreate = {
                        newList = true
                        prefs.edit().putBoolean("new_list", true).apply()
                    },
                    onMyPlaces = { screen = "bookmarkList" }
                )
                "bookmarkList" -> BookmarkListScreen(
                    saved = saved,
                    onBack = { screen = "bookmarks" },
                    onPlace = { mapPoint = false; screen = "place" },
                    onMap = { screen = "map" }
                )
                "downloads" -> DownloadsScreen(onBack = { screen = "map" }, onShowMap = { screen = "map" })
                "settings" -> SettingsScreen(
                    dark = dark,
                    onDark = {
                        dark = it
                        prefs.edit().putBoolean("dark", it).apply()
                    },
                    onBack = { screen = "map" }
                )
                "about" -> AboutScreen(onBack = { screen = "map" })
                "route" -> RouteScreen(
                    mode = routeMode,
                    onMode = { routeMode = it },
                    onClose = { screen = "map" },
                    onStart = { screen = "activeNav" }
                )
                "activeNav" -> ActiveNavigationScreen(onBack = { screen = "route" })
                "addPlace" -> AddPlaceScreen(onBack = { screen = "map" })
            }
        }
    }
}

@Composable
private fun OnboardingScreen(onDone: () -> Unit) {
    var phase by rememberSaveable { mutableIntStateOf(0) }
    var checked by rememberSaveable { mutableStateOf(true) }
    var progress by rememberSaveable { mutableFloatStateOf(0f) }
    var paused by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(phase, paused) {
        if (phase == 1 && !paused) {
            while (progress < 1f) {
                delay(120)
                progress = (progress + 0.07f).coerceAtMost(1f)
            }
            phase = 2
        }
    }

    Surface(color = Color(0xFFF5F5F3), modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Organic Maps", color = OrganicGreen, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(32.dp))
            GlobeIllustration(Modifier.fillMaxWidth().weight(1f))
            when (phase) {
                0 -> {
                    Text("下载世界地图", fontSize = 29.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("69 MB", color = Color.Gray, fontSize = 17.sp)
                    Spacer(Modifier.height(22.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { checked = !checked },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = checked, onCheckedChange = { checked = it })
                        Text("下载奇科的地图吗？", fontSize = 17.sp)
                    }
                    Spacer(Modifier.height(18.dp))
                    Button(
                        onClick = { phase = 1 },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ActionBlue)
                    ) { Text("下载", fontSize = 18.sp) }
                }
                1 -> {
                    Text("正在下载世界地图", fontSize = 27.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    Text((progress * 69).toInt().toString() + " / 69 MB", color = Color.Gray)
                    Spacer(Modifier.height(16.dp))
                    Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(Color(0xFFD0D0D0))) {
                        Box(Modifier.fillMaxWidth(progress).fillMaxHeight().background(ActionBlue))
                    }
                    Spacer(Modifier.height(28.dp))
                    Button(
                        onClick = { paused = !paused },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ActionBlue)
                    ) { Text(if (paused) "继续" else "暂停", fontSize = 18.sp) }
                }
                else -> {
                    Text("世界地图已准备就绪", fontSize = 27.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    Text("正在下载奇科。您现在可以继续查看地图。", color = Color.Gray, fontSize = 17.sp)
                    Spacer(Modifier.height(28.dp))
                    Button(
                        onClick = onDone,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ActionBlue)
                    ) { Text("前往地图", fontSize = 18.sp) }
                }
            }
        }
    }
}

@Composable
private fun GlobeIllustration(modifier: Modifier) {
    Canvas(modifier) {
        val c = center
        val r = size.minDimension * .33f
        drawCircle(MapBlue, r, c)
        drawCircle(Color.White.copy(alpha = .9f), r, c, style = Stroke(width = 5f))
        drawOval(Color.White.copy(alpha = .85f), topLeft = Offset(c.x-r*.8f,c.y-r*.32f), size = androidx.compose.ui.geometry.Size(r*1.6f,r*.64f))
        drawOval(Color.White.copy(alpha = .75f), topLeft = Offset(c.x-r*.28f,c.y-r), size = androidx.compose.ui.geometry.Size(r*.56f,r*2f))
        drawCircle(OrganicGreen, 18f, Offset(c.x+r*.55f,c.y-r*.45f))
    }
}

@Composable
private fun MapHome(
    overlay: String,
    setOverlay: (String) -> Unit,
    recording: Boolean,
    selectedStyle: String,
    onStyle: (String) -> Unit,
    onSearch: () -> Unit,
    onBookmarks: () -> Unit,
    onAbout: () -> Unit,
    onDownloads: () -> Unit,
    onSettings: () -> Unit,
    onRecord: () -> Unit,
    onDeleteTrack: () -> Unit,
    onAddPlace: () -> Unit,
    onShare: () -> Unit,
    onMapPoint: () -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        MapCanvas(
            style = selectedStyle,
            modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                detectTapGestures(onLongPress = { onMapPoint() })
            }
        )
        MapControls(
            recording = recording,
            onLayers = { setOverlay("layers") },
            onRecording = { setOverlay("track") }
        )
        MainBottomBar(onAbout, onSearch, onBookmarks, { setOverlay("menu") }, recording, Modifier.align(Alignment.BottomCenter))

        if (overlay.isNotEmpty()) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = if (overlay == "share") .18f else .28f)).clickable { setOverlay("") })
            when (overlay) {
                "layers" -> LayerSheet(selectedStyle, onStyle, { setOverlay("") })
                "menu" -> MenuSheet(
                    onAddPlace = onAddPlace,
                    onDownloads = onDownloads,
                    onSettings = onSettings,
                    onRecord = onRecord,
                    onShare = onShare
                )
                "track" -> TrackSheet(onDeleteTrack)
                "share" -> ShareSheet { setOverlay("") }
            }
        }
    }
}

@Composable
private fun MapCanvas(style: String = "户外", route: String = "", modifier: Modifier = Modifier) {
    val darkMap = MaterialTheme.colorScheme.background.luminance() < .5f
    val bg = if (darkMap) Color(0xFF242A27) else if (style == "户外") Color(0xFFE9E4D3) else Color(0xFFE6E5DD)
    val water = if (darkMap) Color(0xFF244E5B) else MapBlue
    val park = if (darkMap) Color(0xFF3B4933) else ParkGreen
    val minorRoad = if (darkMap) Color(0xFF626763) else Color.White
    val majorRoad = if (darkMap) Color(0xFF9B4D29) else RoadOrange
    val lineColor = if (darkMap) Color(0xFF777B77) else Color(0xFFAAAA9E)
    val labelColor = if (darkMap) android.graphics.Color.LTGRAY else android.graphics.Color.DKGRAY
    Canvas(modifier.background(bg)) {
        val w = size.width
        val h = size.height
        drawPath(Path().apply {
            moveTo(0f, h*.09f); lineTo(w*.18f,h*.03f); lineTo(w*.42f,h*.16f); lineTo(w*.72f,h*.10f); lineTo(w,h*.01f); lineTo(w,h*.22f); lineTo(w*.66f,h*.28f); lineTo(w*.31f,h*.23f); lineTo(0f,h*.35f); close()
        }, water)
        drawPath(Path().apply {
            moveTo(0f,h*.0f); lineTo(w*.13f,h*.0f); lineTo(w*.08f,h*.58f); lineTo(0f,h*.72f); close()
        }, park)
        drawPath(Path().apply {
            moveTo(0f,h*.54f); lineTo(w*.16f,h*.48f); lineTo(w*.5f,h*.42f); lineTo(w*.71f,h*.47f); lineTo(w,h*.58f)
        }, minorRoad, style = Stroke(width=18f, cap=StrokeCap.Round))
        drawPath(Path().apply {
            moveTo(w*.30f,h); lineTo(w*.38f,h*.82f); lineTo(w*.70f,h*.67f); lineTo(w*.62f,h*.38f); lineTo(w*.47f,0f)
        }, minorRoad, style = Stroke(width=16f, cap=StrokeCap.Round))
        drawLine(majorRoad, Offset(-80f,h*.66f), Offset(w+90f,h*.24f), strokeWidth=25f, cap=StrokeCap.Round)
        drawLine(lineColor, Offset(0f,h*.38f), Offset(w,h*.57f), strokeWidth=3f)
        drawPath(Path().apply {
            moveTo(w*.04f,h*.15f); cubicTo(w*.22f,h*.32f,w*.18f,h*.67f,w*.40f,h*.86f)
        }, Color(0xFF9D642D), style=Stroke(width=7f))
        if (style == "徒步") {
            drawPath(Path().apply {
                moveTo(w*.12f,h*.72f); cubicTo(w*.28f,h*.58f,w*.44f,h*.72f,w*.75f,h*.61f)
            }, Color(0xFFD54B42), style=Stroke(width=6f))
        }
        if (route.isNotEmpty()) {
            val color = if (route == "驾车") ActionBlue else RoutePurple
            drawPath(Path().apply {
                moveTo(w*.28f,h*.83f); cubicTo(w*.22f,h*.69f,w*.60f,h*.64f,w*.47f,h*.49f); cubicTo(w*.36f,h*.37f,w*.76f,h*.34f,w*.72f,h*.18f)
            }, Color.White, style=Stroke(width=28f, cap=StrokeCap.Round))
            drawPath(Path().apply {
                moveTo(w*.28f,h*.83f); cubicTo(w*.22f,h*.69f,w*.60f,h*.64f,w*.47f,h*.49f); cubicTo(w*.36f,h*.37f,w*.76f,h*.34f,w*.72f,h*.18f)
            }, color, style=Stroke(width=17f, cap=StrokeCap.Round))
            drawCircle(Color.White,18f,Offset(w*.28f,h*.83f))
            drawCircle(OrganicGreen,12f,Offset(w*.28f,h*.83f))
            drawCircle(Color.White,18f,Offset(w*.72f,h*.18f))
            drawCircle(Color.Red,12f,Offset(w*.72f,h*.18f))
        }
        drawPath(Path().apply {
            moveTo(w*.50f,h*.43f); lineTo(w*.46f,h*.51f); lineTo(w*.50f,h*.495f); lineTo(w*.54f,h*.51f); close()
        }, Color(0xFF1976D2))
        drawCircle(Color(0x553B9CFF), 28f, Offset(w*.5f,h*.51f))
        drawIntoCanvas { canvas ->
            val paint = android.graphics.Paint().apply { isAntiAlias=true; color=labelColor; textSize=31f; typeface=android.graphics.Typeface.DEFAULT_BOLD }
            canvas.nativeCanvas.drawText("Vista del Lago Road",w*.59f,h*.58f,paint)
            paint.color=if(darkMap) android.graphics.Color.rgb(130,190,205) else android.graphics.Color.rgb(35,120,160); paint.textSize=27f
            canvas.nativeCanvas.drawText("Lake Mendocino",w*.38f,h*.18f,paint)
            paint.color=labelColor; paint.textSize=24f
            canvas.nativeCanvas.drawText("Bushay Campground",w*.30f,h*.36f,paint)
        }
    }
}

@Composable
private fun MapControls(recording: Boolean, onLayers: () -> Unit, onRecording: () -> Unit) {
    Box(Modifier.fillMaxSize().statusBarsPadding().padding(18.dp)) {
        RoundMapButton("◆", Modifier.align(Alignment.TopStart), onLayers, ActionBlue)
        if (recording) RoundMapButton("◉", Modifier.align(Alignment.TopEnd), onRecording, Color(0xFFD84949))
        Column(Modifier.align(Alignment.CenterEnd), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            RoundMapButton("+", Modifier, {}, MaterialTheme.colorScheme.onSurface.copy(alpha=.78f))
            RoundMapButton("−", Modifier, {}, MaterialTheme.colorScheme.onSurface.copy(alpha=.78f))
        }
        RoundMapButton("◎", Modifier.align(Alignment.BottomEnd).padding(bottom=128.dp), {}, MaterialTheme.colorScheme.onSurface.copy(alpha=.68f))
        Text("50 m", modifier=Modifier.align(Alignment.BottomStart).padding(bottom=126.dp,start=10.dp),fontSize=15.sp,fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.onSurface.copy(alpha=.72f))
    }
}

@Composable
private fun RoundMapButton(text: String, modifier: Modifier, onClick: () -> Unit, tint: Color) {
    Surface(
        modifier = modifier.size(58.dp).clickable(onClick = onClick),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha=.94f),
        shadowElevation = 2.dp
    ) {
        Box(contentAlignment = Alignment.Center) { Text(text,fontSize=31.sp,color=tint,fontWeight=FontWeight.Bold) }
    }
}

@Composable
private fun MainBottomBar(onAbout:()->Unit,onSearch:()->Unit,onBookmarks:()->Unit,onMenu:()->Unit,recording:Boolean,modifier:Modifier=Modifier) {
    Row(
        modifier=modifier.fillMaxWidth().navigationBarsPadding().padding(start=76.dp,end=76.dp,bottom=18.dp),
        horizontalArrangement=Arrangement.SpaceBetween,
        verticalAlignment=Alignment.CenterVertically
    ) {
        SquareMapButton("◉",OrganicGreen,onAbout)
        SquareMapButton("⌕",MaterialTheme.colorScheme.onSurface.copy(alpha=.72f),onSearch)
        SquareMapButton("★⋮",MaterialTheme.colorScheme.onSurface.copy(alpha=.72f),onBookmarks)
        Box {
            SquareMapButton("☰",MaterialTheme.colorScheme.onSurface.copy(alpha=.72f),onMenu)
            if(recording) Box(Modifier.size(11.dp).clip(CircleShape).background(ActionBlue).align(Alignment.TopEnd))
        }
    }
}


@Composable
private fun SquareMapButton(text:String,tint:Color,onClick:()->Unit) {
    Surface(modifier=Modifier.size(62.dp).clickable(onClick=onClick),shape=RoundedCornerShape(18.dp),color=MaterialTheme.colorScheme.surface.copy(alpha=.96f),shadowElevation=2.dp) {
        Box(contentAlignment=Alignment.Center){ Text(text,fontSize=27.sp,color=tint,fontWeight=FontWeight.Bold) }
    }
}

@Composable
private fun BottomSheetBox(height: Int, content:@Composable ColumnScope.()->Unit) {
    Surface(
        modifier=Modifier.fillMaxWidth().height(height.dp).navigationBarsPadding(),
        shape=RoundedCornerShape(topStart=28.dp,topEnd=28.dp),
        color=MaterialTheme.colorScheme.surface,
        shadowElevation=10.dp
    ) {
        Column(Modifier.fillMaxSize(),content=content)
    }
}

@Composable
private fun LayerSheet(selected:String,onStyle:(String)->Unit,onClose:()->Unit) {
    Box(Modifier.fillMaxSize(),contentAlignment=Alignment.BottomCenter) {
        BottomSheetBox(340) {
            SheetHandle()
            Row(Modifier.fillMaxWidth().padding(horizontal=24.dp),verticalAlignment=Alignment.CenterVertically) {
                Text("地图样式和图层",fontSize=23.sp,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f))
                Text("✕",fontSize=24.sp,modifier=Modifier.clickable(onClick=onClose).padding(10.dp))
            }
            Spacer(Modifier.height(18.dp))
            val styles=listOf("地铁","等高线","户外","徒步","骑行")
            Row(Modifier.fillMaxWidth().padding(horizontal=16.dp),horizontalArrangement=Arrangement.SpaceEvenly) {
                styles.forEach { s ->
                    Column(
                        modifier=Modifier.width(66.dp).clip(RoundedCornerShape(14.dp))
                            .border(if(selected==s) 2.dp else 0.dp,if(selected==s) ActionBlue else Color.Transparent,RoundedCornerShape(14.dp))
                            .clickable{onStyle(s)}.padding(vertical=12.dp),
                        horizontalAlignment=Alignment.CenterHorizontally
                    ) {
                        Text(if(s=="骑行") "♢" else if(s=="徒步") "⌁" else "▧",fontSize=26.sp,color=if(selected==s) ActionBlue else Color.Gray)
                        Text(s,fontSize=14.sp,color=if(selected==s) ActionBlue else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuSheet(onAddPlace:()->Unit,onDownloads:()->Unit,onSettings:()->Unit,onRecord:()->Unit,onShare:()->Unit) {
    Box(Modifier.fillMaxSize(),contentAlignment=Alignment.BottomCenter) {
        BottomSheetBox(535) {
            SheetHandle()
            MenuRow("+","将地点添加到 OpenStreetMap",onAddPlace)
            MenuRow("⇩","下载地图",onDownloads)
            MenuRow("♥","捐助我们",{})
            MenuRow("⚙","设置",onSettings)
            MenuRow("◌","录制轨迹",onRecord)
            MenuRow("⌯","分享我的位置",onShare)
        }
    }
}

@Composable
private fun MenuRow(icon:String,label:String,onClick:()->Unit) {
    Row(Modifier.fillMaxWidth().height(76.dp).clickable(onClick=onClick).padding(horizontal=30.dp),verticalAlignment=Alignment.CenterVertically) {
        Text(icon,fontSize=29.sp,color=Color.Gray,modifier=Modifier.width(58.dp))
        Text(label,fontSize=20.sp)
    }
}

@Composable
private fun TrackSheet(onDelete:()->Unit) {
    Box(Modifier.fillMaxSize(),contentAlignment=Alignment.BottomCenter) {
        BottomSheetBox(600) {
            SheetHandle()
            Text("0 米 · 0 分钟",fontSize=30.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(horizontal=24.dp))
            Text("轨迹记录",fontSize=18.sp,color=Color.Gray,modifier=Modifier.padding(horizontal=24.dp,vertical=12.dp))
            Box(Modifier.fillMaxWidth().height(150.dp).background(Color(0xFFF1F1F1)))
            Spacer(Modifier.weight(1f))
            HorizontalDivider()
            Column(Modifier.fillMaxWidth().clickable(onClick=onDelete).padding(18.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                Text("▣",fontSize=26.sp,color=Color.Gray)
                Text("删除",fontSize=16.sp,color=Color.Gray)
            }
        }
    }
}

@Composable
private fun ShareSheet(onClose:()->Unit) {
    Box(Modifier.fillMaxSize(),contentAlignment=Alignment.BottomCenter) {
        BottomSheetBox(330) {
            SheetHandle()
            Row(Modifier.fillMaxWidth().padding(horizontal=24.dp),verticalAlignment=Alignment.CenterVertically) {
                Text("分享我的位置",fontSize=24.sp,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f))
                Text("✕",fontSize=24.sp,modifier=Modifier.clickable(onClick=onClose).padding(8.dp))
            }
            Text("39.231° N, 123.164° W",fontSize=18.sp,color=Color.Gray,modifier=Modifier.padding(24.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal=20.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                Button({},modifier=Modifier.weight(1f),colors=ButtonDefaults.buttonColors(containerColor=ActionBlue)){Text("复制链接")}
                Button({},modifier=Modifier.weight(1f),colors=ButtonDefaults.buttonColors(containerColor=ActionBlue)){Text("分享")}
            }
        }
    }
}

@Composable
private fun SheetHandle() {
    Box(Modifier.fillMaxWidth().height(24.dp),contentAlignment=Alignment.Center) {
        Box(Modifier.width(48.dp).height(5.dp).clip(CircleShape).background(Color(0xFFD4D4D4)))
    }
}

@Composable
private fun SearchScreen(historyHotel:Boolean,onBack:()->Unit,onHotel:()->Unit,onTextSearch:()->Unit,onClearHistory:()->Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var tab by rememberSaveable { mutableStateOf("类别") }
    val focus=remember{FocusRequester()}
    val keyboard=LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit){ delay(180); focus.requestFocus() }

    Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(14.dp),verticalAlignment=Alignment.CenterVertically) {
                Surface(shape=RoundedCornerShape(34.dp),color=Color(0xFFE7E7E7),modifier=Modifier.weight(1f).height(58.dp)) {
                    Row(Modifier.fillMaxSize().padding(horizontal=18.dp),verticalAlignment=Alignment.CenterVertically) {
                        Text("⌕",fontSize=28.sp,color=Color.Black)
                        Spacer(Modifier.width(12.dp))
                        BasicTextField(
                            value=query,onValueChange={query=it},
                            modifier=Modifier.weight(1f).focusRequester(focus),
                            textStyle=TextStyle(fontSize=22.sp,color=Color.Black),
                            singleLine=true,
                            keyboardOptions=KeyboardOptions(imeAction=ImeAction.Search),
                            keyboardActions=KeyboardActions(onSearch={keyboard?.hide();onTextSearch() }),
                            decorationBox={ inner -> Box(contentAlignment=Alignment.CenterStart){if(query.isEmpty())Text("搜索",fontSize=21.sp,color=Color.Gray);inner()} }
                        )
                        Text(if(query.isEmpty())"●" else "✕",fontSize=20.sp,color=Color.Black,modifier=Modifier.clickable{query=""}.padding(8.dp))
                    }
                }
                Text("✕",fontSize=28.sp,modifier=Modifier.clickable(onClick=onBack).padding(start=20.dp,end=8.dp))
            }
            if(query.isNotEmpty()) {
                val suggestions=listOf(query+"ing",query+"ing tickets","Parkland","Parkville","Parker")
                suggestions.forEach { s -> SimpleRow("⌕",s,ActionBlue,onClick={onTextSearch()}) }
                HorizontalDivider()
                PlaceResultRow("Kakum National Park","国家公园","Gyankobo, 加纳","12,270 公里",onTextSearch)
                PlaceResultRow("Songor Lagoon","国家公园","加纳","12,330 公里",onTextSearch)
            } else {
                Row(Modifier.fillMaxWidth()) {
                    TabLabel("历史",tab=="历史",Modifier.weight(1f)){tab="历史"}
                    TabLabel("类别",tab=="类别",Modifier.weight(1f)){tab="类别"}
                }
                HorizontalDivider()
                if(tab=="类别") {
                    LazyColumn {
                        items(listOf(
                            "🍴" to "在哪儿吃","▣" to "酒店","🛒" to "食品","★" to "旅游景点",
                            "⌁" to "无线网络","▣" to "交通","▣" to "加油站","P" to "停车场",
                            "▤" to "购物","♻" to "二手","ATM" to "自动取款机","☾" to "夜生活","☺" to "家庭"
                        )) { pair ->
                            SimpleRow(pair.first,pair.second,Color(0xFF7A6A45),onClick={if(pair.second=="酒店")onHotel()})
                        }
                    }
                } else {
                    if(historyHotel) SimpleRow("◴","酒店",Color.Gray,onHotel)
                    SimpleRow("✕","清除搜索记录",Color.Gray,onClearHistory)
                }
            }
        }
    }
}

@Composable
private fun TabLabel(label:String,selected:Boolean,modifier:Modifier,onClick:()->Unit) {
    Column(modifier.clickable(onClick=onClick),horizontalAlignment=Alignment.CenterHorizontally) {
        Text(label,fontSize=18.sp,modifier=Modifier.padding(vertical=20.dp))
        Box(Modifier.fillMaxWidth().height(3.dp).background(if(selected)Color.Black else Color.Transparent))
    }
}

@Composable
private fun SimpleRow(icon:String,label:String,tint:Color,onClick:()->Unit,subtitle:String="") {
    Row(Modifier.fillMaxWidth().clickable(onClick=onClick).padding(horizontal=28.dp,vertical=18.dp),verticalAlignment=Alignment.CenterVertically) {
        Box(Modifier.size(48.dp).clip(CircleShape).background(tint.copy(alpha=.16f)),contentAlignment=Alignment.Center){Text(icon,fontSize=20.sp,color=tint,fontWeight=FontWeight.Bold)}
        Spacer(Modifier.width(20.dp))
        Column(Modifier.weight(1f)){Text(label,fontSize=20.sp);if(subtitle.isNotEmpty())Text(subtitle,fontSize=15.sp,color=Color.Gray)}
    }
}

@Composable
private fun SearchResultsScreen(onBack:()->Unit,onPlace:()->Unit) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(Modifier.fillMaxWidth().weight(.48f)) {
            MapCanvas(modifier=Modifier.fillMaxSize())
            Surface(Modifier.statusBarsPadding().padding(14.dp).fillMaxWidth().height(55.dp),shape=RoundedCornerShape(30.dp),color=Color.White,shadowElevation=5.dp) {
                Row(Modifier.fillMaxSize().padding(horizontal=16.dp),verticalAlignment=Alignment.CenterVertically){
                    Text("‹",fontSize=34.sp,modifier=Modifier.clickable(onClick=onBack))
                    Text("酒店",fontSize=20.sp,modifier=Modifier.padding(start=18.dp).weight(1f),color=Color.Black)
                    Text("✕",fontSize=24.sp,color=Color.Black)
                }
            }
        }
        Surface(Modifier.fillMaxWidth().weight(.52f),shape=RoundedCornerShape(topStart=26.dp,topEnd=26.dp),color=MaterialTheme.colorScheme.surface) {
            LazyColumn {
                item { SheetHandle(); Text("附近的结果",fontSize=22.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(22.dp,4.dp,22.dp,12.dp)) }
                item { PlaceResultRow("Bushay Campground","宿营场地","Lake Mendocino Dr","1.4 公里",onPlace) }
                item { PlaceResultRow("Motel 6 Ukiah","酒店","North State Street","10 公里",onPlace) }
                item { PlaceResultRow("Hampton Inn Ukiah","酒店","South Orchard Avenue","12 公里",onPlace) }
            }
        }
    }
}

@Composable
private fun PlaceResultRow(name:String,category:String,address:String,distance:String,onClick:()->Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick=onClick).padding(22.dp,16.dp),verticalAlignment=Alignment.Top) {
        Box(Modifier.size(50.dp).clip(CircleShape).background(Color(0xFF9A3E0B)),contentAlignment=Alignment.Center){Text("▣",color=Color.White,fontSize=22.sp)}
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)){
            Text(name,fontSize=19.sp,fontWeight=FontWeight.Medium)
            Text(category,fontSize=15.sp,color=Color.Gray)
            Text(address,fontSize=14.sp,color=Color.Gray)
        }
        Text(distance,fontSize=15.sp,color=ActionBlue)
    }
}

@Composable
private fun PlaceScreen(mapPoint:Boolean,saved:Boolean,note:String,onClose:()->Unit,onSave:()->Unit,onEdit:()->Unit,onRoute:()->Unit) {
    var expanded by rememberSaveable { mutableStateOf(saved) }
    Box(Modifier.fillMaxSize()) {
        MapCanvas(modifier=Modifier.fillMaxSize())
        Box(Modifier.align(Alignment.Center).padding(bottom=180.dp),contentAlignment=Alignment.Center){
            Box(Modifier.size(32.dp).clip(CircleShape).background(if(mapPoint)ActionBlue else Color(0xFFE82436)),contentAlignment=Alignment.Center){Text(if(mapPoint)"●" else "▣",color=Color.White)}
        }
        Surface(
            modifier=Modifier.fillMaxWidth().height(if(expanded) 635.dp else 360.dp).align(Alignment.BottomCenter).navigationBarsPadding(),
            shape=RoundedCornerShape(topStart=28.dp,topEnd=28.dp),color=MaterialTheme.colorScheme.surface,shadowElevation=12.dp
        ) {
            Column {
                SheetHandle()
                Row(Modifier.fillMaxWidth().clickable{expanded=!expanded}.padding(horizontal=22.dp,vertical=8.dp),verticalAlignment=Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text(if(mapPoint)"地图点" else "Bushay Campground",fontSize=29.sp,fontWeight=FontWeight.Bold)
                        Text(if(mapPoint)"39.231° N, 123.164° W" else "宿营场地",fontSize=17.sp,color=Color.Gray)
                    }
                    Text("⌯",fontSize=26.sp,modifier=Modifier.padding(8.dp))
                    Text("✕",fontSize=26.sp,modifier=Modifier.clickable(onClick=onClose).padding(8.dp))
                }
                if(!mapPoint) {
                    HorizontalDivider()
                    Row(Modifier.fillMaxWidth().padding(20.dp),verticalAlignment=Alignment.CenterVertically) {
                        Box(Modifier.size(52.dp).clip(CircleShape).background(Color(0xFFE51B2A)),contentAlignment=Alignment.Center){Text("▣",color=Color.White,fontSize=24.sp)}
                        Text("我的地点",fontSize=20.sp,color=ActionBlue,modifier=Modifier.padding(start=16.dp).weight(1f))
                        if(saved) Text("✎",fontSize=28.sp,color=ActionBlue,modifier=Modifier.clickable(onClick=onEdit))
                    }
                    if(note.isNotEmpty()) Text(note,fontSize=18.sp,color=Color.Gray,modifier=Modifier.padding(horizontal=28.dp,vertical=8.dp))
                }
                if(expanded) {
                    HorizontalDivider()
                    DetailLine("◎","39.230797, -123.164270")
                    DetailLine("↗","在另一个应用中打开")
                    if(!mapPoint) DetailLine("+","编辑地点 / 添加到 OpenStreetMap")
                }
                Spacer(Modifier.weight(1f))
                HorizontalDivider()
                Row(Modifier.fillMaxWidth().height(86.dp),horizontalArrangement=Arrangement.SpaceEvenly) {
                    PlaceAction("⌖","从这出发",{})
                    PlaceAction(if(saved)"★" else "☆",if(saved)"删除" else "保存",onSave,if(saved)Color(0xFFFFC400) else Color.Gray)
                    PlaceAction("⌖","到这去",onRoute)
                }
            }
        }
    }
}

@Composable
private fun DetailLine(icon:String,text:String) {
    Row(Modifier.fillMaxWidth().padding(horizontal=28.dp,vertical=12.dp),verticalAlignment=Alignment.CenterVertically){
        Text(icon,fontSize=22.sp,color=Color.Gray,modifier=Modifier.width(44.dp))
        Text(text,fontSize=17.sp)
    }
}

@Composable
private fun PlaceAction(icon:String,label:String,onClick:()->Unit,tint:Color=Color.Gray) {
    Column(Modifier.fillMaxHeight().width(110.dp).clickable(onClick=onClick).padding(top=12.dp),horizontalAlignment=Alignment.CenterHorizontally) {
        Text(icon,fontSize=26.sp,color=tint)
        Text(label,fontSize=14.sp,color=Color.Gray)
    }
}

@Composable
private fun EditBookmarkScreen(note:String,onNote:(String)->Unit,onBack:()->Unit,onDelete:()->Unit) {
    var name by rememberSaveable { mutableStateOf("Bushay Campground") }
    Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            AppTopBar("编辑书签",onBack)
            LabeledField("名称",name,{name=it})
            LabeledField("列表","我的地点",{})
            LabeledField("个人备注",note,onNote,true)
            Spacer(Modifier.weight(1f))
            TextButton(onClick=onDelete,modifier=Modifier.fillMaxWidth().padding(20.dp)){Text("删除书签",color=Color(0xFFD13A3A),fontSize=18.sp)}
        }
    }
}

@Composable
private fun LabeledField(label:String,value:String,onChange:(String)->Unit,multiline:Boolean=false) {
    Column(Modifier.fillMaxWidth().padding(horizontal=24.dp,vertical=12.dp)) {
        Text(label,fontSize=15.sp,color=Color.Gray)
        Surface(Modifier.fillMaxWidth().height(if(multiline)120.dp else 58.dp),shape=RoundedCornerShape(14.dp),color=Color(0xFFEDEDED)) {
            BasicTextField(value,onChange,Modifier.fillMaxSize().padding(16.dp),textStyle=TextStyle(fontSize=18.sp,color=Color.Black),singleLine=!multiline)
        }
    }
}

@Composable
private fun BookmarksOverview(saved:Boolean,newList:Boolean,onBack:()->Unit,onCreate:()->Unit,onMyPlaces:()->Unit) {
    var showDialog by remember { mutableStateOf(false) }
    Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize()) {
            GreenTopBar("书签和轨迹",onBack)
            Row(Modifier.fillMaxWidth().padding(22.dp),verticalAlignment=Alignment.CenterVertically) {
                Text("列表",fontSize=28.sp,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f))
                Text("全部隐藏",fontSize=18.sp,color=ActionBlue)
            }
            HorizontalDivider()
            if(newList) BookmarkListRow("新的列表","0 个书签，0 个轨迹",{})
            BookmarkListRow("我的地点",(if(saved)"1" else "0")+" 个书签",onMyPlaces)
            HorizontalDivider()
            SimpleActionRow("⊞","创建新的列表"){showDialog=true}
            SimpleActionRow("⇩","导入书签和轨迹",{})
            SimpleActionRow("⇧","导出所有书签和轨迹",{})
        }
    }
    if(showDialog) {
        AlertDialog(
            onDismissRequest={showDialog=false},
            title={Text("创建新的列表")},
            text={Surface(Modifier.fillMaxWidth().height(55.dp),shape=RoundedCornerShape(12.dp),color=Color(0xFFECECEC)){Box(Modifier.padding(16.dp)){Text("新的列表",color=Color.Black)}}},
            confirmButton={TextButton(onClick={showDialog=false;onCreate()}){Text("创建")}},
            dismissButton={TextButton(onClick={showDialog=false}){Text("取消")}}
        )
    }
}

@Composable
private fun GreenTopBar(title:String,onBack:()->Unit) {
    Surface(color=OrganicGreen,modifier=Modifier.fillMaxWidth()) {
        Row(Modifier.statusBarsPadding().height(78.dp).fillMaxWidth().padding(horizontal=18.dp),verticalAlignment=Alignment.CenterVertically) {
            Text("‹",fontSize=40.sp,color=Color.White,modifier=Modifier.clickable(onClick=onBack).padding(8.dp))
            Text(title,fontSize=27.sp,color=Color.White,fontWeight=FontWeight.Bold,modifier=Modifier.padding(start=14.dp))
        }
    }
}

@Composable
private fun AppTopBar(title:String,onBack:()->Unit,action:String="") {
    Row(Modifier.fillMaxWidth().height(76.dp).padding(horizontal=18.dp),verticalAlignment=Alignment.CenterVertically) {
        Text("‹",fontSize=40.sp,modifier=Modifier.clickable(onClick=onBack).padding(8.dp))
        Text(title,fontSize=27.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(start=12.dp).weight(1f))
        if(action.isNotEmpty()) Text(action,fontSize=27.sp,modifier=Modifier.padding(8.dp))
    }
}

@Composable
private fun BookmarkListRow(title:String,subtitle:String,onClick:()->Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick=onClick).padding(horizontal=28.dp,vertical=18.dp),verticalAlignment=Alignment.CenterVertically) {
        Text("◉",fontSize=27.sp,color=ActionBlue)
        Column(Modifier.padding(start=20.dp).weight(1f)){Text(title,fontSize=21.sp);Text(subtitle,fontSize=16.sp,color=Color.Gray)}
        Text("⋮",fontSize=28.sp,color=Color.Gray)
    }
}

@Composable
private fun SimpleActionRow(icon:String,title:String,onClick:()->Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick=onClick).padding(horizontal=30.dp,vertical=20.dp),verticalAlignment=Alignment.CenterVertically){
        Text(icon,fontSize=25.sp,color=ActionBlue,modifier=Modifier.width(55.dp))
        Text(title,fontSize=20.sp,color=ActionBlue)
    }
    HorizontalDivider()
}

@Composable
private fun BookmarkListScreen(saved:Boolean,onBack:()->Unit,onPlace:()->Unit,onMap:()->Unit) {
    Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            AppTopBar("我的地点",onBack,"⋮")
            Text("注释",fontSize=16.sp,color=Color.Gray,modifier=Modifier.padding(start=30.dp,top=10.dp))
            Surface(Modifier.fillMaxWidth().padding(20.dp).height(60.dp),shape=RoundedCornerShape(28.dp),color=Color(0xFFECECEC)){Box(Modifier.padding(18.dp)){Text("我的地点",fontSize=18.sp,color=Color.Black)}}
            Text("书签",fontSize=17.sp,color=Color.Gray,modifier=Modifier.padding(horizontal=30.dp,vertical=6.dp))
            if(saved) PlaceResultRow("Bushay Campground","宿营场地","","1.4 公里",onPlace)
            Spacer(Modifier.weight(1f))
            Button(onClick=onMap,modifier=Modifier.align(Alignment.CenterHorizontally).padding(24.dp),colors=ButtonDefaults.buttonColors(containerColor=ActionBlue)){Text("▱  在地图上查看",fontSize=17.sp)}
        }
    }
}

@Composable
private fun DownloadsScreen(onBack:()->Unit,onShowMap:()->Unit) {
    var level by rememberSaveable { mutableStateOf("root") }
    var chicoSheet by remember { mutableStateOf(false) }
    Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            AppTopBar(if(level=="root")"下载地图" else if(level=="us")"美国" else "加利福尼亚州",{if(level=="root")onBack() else level=if(level=="ca")"us" else "root"},"⌕")
            when(level) {
                "root" -> {
                    DownloadRow("世界","61 MB","✓"){}
                    DownloadRow("世界海岸线","8 MB","✓"){}
                    DownloadRow("美国","61 MB · 1/154","›"){level="us"}
                }
                "us" -> DownloadRow("加利福尼亚州","61 MB · 1/58","›"){level="ca"}
                else -> {
                    DownloadRow("奇科","61 MB","✓"){chicoSheet=true}
                    DownloadRow("萨克拉门托","85 MB","",""){}
                    DownloadRow("旧金山湾区","124 MB","",""){}
                }
            }
            Spacer(Modifier.weight(1f))
            Surface(Modifier.align(Alignment.End).padding(24.dp).size(62.dp),shape=CircleShape,color=ActionBlue,shadowElevation=6.dp){Box(contentAlignment=Alignment.Center){Text("+",fontSize=34.sp,color=Color.White)}}
        }
    }
    if(chicoSheet) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(.25f)).clickable{chicoSheet=false},contentAlignment=Alignment.BottomCenter) {
            BottomSheetBox(245) {
                SheetHandle()
                MenuRow("▱","在地图上显示"){chicoSheet=false;onShowMap()}
                MenuRow("▣","删除地图"){chicoSheet=false}
            }
        }
    }
}

@Composable
private fun DownloadRow(title:String,subtitle:String,end:String,unused:String="",onClick:()->Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick=onClick).padding(horizontal=26.dp,vertical=20.dp),verticalAlignment=Alignment.CenterVertically) {
        Text(if(end=="✓")"▣" else "▤",fontSize=25.sp,color=if(end=="✓")ActionBlue else Color.Gray,modifier=Modifier.width(52.dp))
        Column(Modifier.weight(1f)){Text(title,fontSize=20.sp);Text(subtitle,fontSize=15.sp,color=Color.Gray)}
        Text(end,fontSize=23.sp,color=if(end=="✓")OrganicGreen else Color.Gray)
    }
    HorizontalDivider()
}

@Composable
private fun SettingsScreen(dark:Boolean,onDark:(Boolean)->Unit,onBack:()->Unit) {
    var appearanceDialog by remember { mutableStateOf(false) }
    var unitDialog by remember { mutableStateOf(false) }
    var showZoom by rememberSaveable { mutableStateOf(true) }
    var buildings by rememberSaveable { mutableStateOf(true) }
    var autoDownload by rememberSaveable { mutableStateOf(true) }
    var keepOn by rememberSaveable { mutableStateOf(false) }
    var voice by rememberSaveable { mutableStateOf(true) }
    var history by rememberSaveable { mutableStateOf(true) }
    Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            AppTopBar("设置",onBack)
            LazyColumn(Modifier.navigationBarsPadding()) {
                item { SectionTitle("常规") }
                item { SettingLine("OpenStreetMap 个人资料","未登录",{}) }
                item { SettingLine("外观",if(dark)"深色" else "跟随系统",{appearanceDialog=true}) }
                item { SettingLine("测量单位","公里",{unitDialog=true}) }
                item { SwitchLine("在地图上显示 + / −",showZoom){showZoom=it} }
                item { SwitchLine("3D 建筑",buildings){buildings=it} }
                item { SwitchLine("自动下载地图",autoDownload){autoDownload=it} }
                item { SettingLine("已下载区域颜色","紫色",{}) }
                item { SettingLine("地图字体大小","正常",{}) }
                item { SwitchLine("将非拉丁名称转写为拉丁文",true){} }
                item { SettingLine("地图存储","内部存储",{}) }
                item { SwitchLine("记录日志",false){} }
                item { SectionTitle("网络与显示") }
                item { SwitchLine("使用移动网络下载地图",false){} }
                item { SwitchLine("省电模式",false){} }
                item { SwitchLine("在地图上显示书签名称",true){} }
                item { SwitchLine("保持屏幕常亮",keepOn){keepOn=it} }
                item { SwitchLine("在锁定屏幕上显示",false){} }
                item { SettingLine("地图语言","中文",{}) }
                item { SettingLine("卫星图像","无",{}) }
                item { SectionTitle("导航") }
                item { SettingLine("夜间样式","自动",{}) }
                item { SettingLine("地图透视","3D",{}) }
                item { SwitchLine("自动缩放",true){} }
                item { SwitchLine("语音指导",voice){voice=it} }
                item { SwitchLine("绕行设置",false){} }
                item { SectionTitle("隐私") }
                item { SwitchLine("Google Play 位置信息",true){} }
                item { SwitchLine("搜索历史",history){history=it} }
                item { SettingLine("帮助",""){} }
            }
        }
    }
    if(appearanceDialog) RadioChoiceDialog("外观",listOf("跟随系统","浅色","深色","从黎明到黄昏使用浅色"),if(dark)"深色" else "跟随系统",{
        onDark(it=="深色");appearanceDialog=false
    },{appearanceDialog=false})
    if(unitDialog) RadioChoiceDialog("测量单位",listOf("公里","英里"),"公里",{unitDialog=false},{unitDialog=false})
}

@Composable
private fun SectionTitle(text:String) {
    Text(text,fontSize=16.sp,fontWeight=FontWeight.Bold,color=OrganicGreen,modifier=Modifier.fillMaxWidth().padding(horizontal=24.dp,vertical=14.dp))
}

@Composable
private fun SettingLine(title:String,subtitle:String="",onClick:()->Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick=onClick).padding(horizontal=24.dp,vertical=14.dp),verticalAlignment=Alignment.CenterVertically) {
        Column(Modifier.weight(1f)){Text(title,fontSize=18.sp);if(subtitle.isNotEmpty())Text(subtitle,fontSize=14.sp,color=Color.Gray)}
        Text("›",fontSize=24.sp,color=Color.Gray)
    }
    HorizontalDivider(color=MaterialTheme.colorScheme.onSurface.copy(.08f))
}

@Composable
private fun SwitchLine(title:String,checked:Boolean,onChecked:(Boolean)->Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal=24.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically) {
        Text(title,fontSize=18.sp,modifier=Modifier.weight(1f))
        Switch(checked,onChecked)
    }
    HorizontalDivider(color=MaterialTheme.colorScheme.onSurface.copy(.08f))
}

@Composable
private fun RadioChoiceDialog(title:String,items:List<String>,selected:String,onSelect:(String)->Unit,onCancel:()->Unit) {
    AlertDialog(
        onDismissRequest=onCancel,
        title={Text(title)},
        text={Column{items.forEach{item->Row(Modifier.fillMaxWidth().clickable{onSelect(item)}.padding(vertical=8.dp),verticalAlignment=Alignment.CenterVertically){RadioButton(selected==item,onClick={onSelect(item)});Text(item,fontSize=18.sp)}}}},
        confirmButton={},
        dismissButton={TextButton(onClick=onCancel){Text("取消")}}
    )
}

@Composable
private fun AboutScreen(onBack:()->Unit) {
    Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background) {
        LazyColumn(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            item { AppTopBar("关于 Organic Maps",onBack) }
            item {
                Column(Modifier.fillMaxWidth().padding(28.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                    Surface(Modifier.size(105.dp),shape=RoundedCornerShape(26.dp),color=OrganicGreen){Box(contentAlignment=Alignment.Center){Text("◉",fontSize=60.sp,color=Color.White)}}
                    Spacer(Modifier.height(18.dp))
                    Text("Organic Maps",fontSize=31.sp,fontWeight=FontWeight.Bold,color=OrganicGreen)
                    Text("2026.08.27-18-Web",fontSize=15.sp,color=Color.Gray)
                    Spacer(Modifier.height(20.dp))
                    Text("免费的离线地图与 GPS 导航",fontSize=20.sp,fontWeight=FontWeight.Bold)
                    Text("无广告 · 无跟踪 · 尊重隐私",fontSize=18.sp,color=Color.Gray,modifier=Modifier.padding(top=8.dp))
                }
            }
            item { Surface(Modifier.fillMaxWidth().padding(20.dp),shape=RoundedCornerShape(18.dp),color=Color(0xFFFFF1C8)){Text("♥  支持 Organic Maps 项目",fontSize=19.sp,modifier=Modifier.padding(22.dp),color=Color(0xFF7B5A00))} }
            item { AboutRow("OpenStreetMap 数据","2026 年 8 月") }
            item { AboutRow("问题与答案","常见使用帮助") }
            item { AboutRow("报告问题","帮助我们改进地图") }
            item { AboutRow("支持我们","捐助或参与社区") }
            item { AboutRow("新闻与更新","了解最新版本") }
        }
    }
}

@Composable
private fun AboutRow(title:String,subtitle:String) {
    Row(Modifier.fillMaxWidth().padding(horizontal=26.dp,vertical=18.dp)){
        Column(Modifier.weight(1f)){Text(title,fontSize=19.sp);Text(subtitle,fontSize=15.sp,color=Color.Gray)}
        Text("›",fontSize=24.sp,color=Color.Gray)
    }
    HorizontalDivider()
}

@Composable
private fun RouteScreen(mode:String,onMode:(String)->Unit,onClose:()->Unit,onStart:()->Unit) {
    val context = LocalContext.current
    var safety by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        MapCanvas(route=mode,modifier=Modifier.fillMaxSize())
        Surface(Modifier.fillMaxWidth().statusBarsPadding().padding(12.dp).height(72.dp),shape=RoundedCornerShape(20.dp),color=Color.White,shadowElevation=6.dp) {
            Row(Modifier.fillMaxSize(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceEvenly) {
                listOf("驾车" to "🚗","步行" to "♟","公交" to "▣","骑行" to "♢","直线" to "╱").forEach { pair ->
                    Column(Modifier.width(58.dp).clickable{onMode(pair.first)},horizontalAlignment=Alignment.CenterHorizontally) {
                        Text(pair.second,fontSize=22.sp,color=if(mode==pair.first)ActionBlue else Color.Gray)
                        Text(pair.first,fontSize=12.sp,color=if(mode==pair.first)ActionBlue else Color.Gray)
                    }
                }
                Text("✕",fontSize=26.sp,color=Color.Black,modifier=Modifier.clickable(onClick=onClose).padding(8.dp))
            }
        }
        Surface(Modifier.fillMaxWidth().height(265.dp).align(Alignment.BottomCenter).navigationBarsPadding(),shape=RoundedCornerShape(topStart=28.dp,topEnd=28.dp),color=MaterialTheme.colorScheme.surface,shadowElevation=10.dp) {
            Column {
                SheetHandle()
                val stats=when(mode){"步行"->"34 分钟 · 2.6 公里";"骑行"->"12 分钟 · 2.6 公里";"直线"->"1.4 公里";"公交"->"此区域没有公交路线";else->"1 小时 31 分钟 · 29 公里"}
                Text(stats,fontSize=25.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(horizontal=24.dp))
                if(mode=="步行"||mode=="骑行") {
                    Text("上升 49 米   下降 44 米",fontSize=15.sp,color=Color.Gray,modifier=Modifier.padding(horizontal=24.dp,vertical=6.dp))
                    MiniElevation()
                } else Spacer(Modifier.height(26.dp))
                Button(
                    onClick={if(mode!="直线"&&mode!="公交")safety=true},
                    enabled=mode!="直线"&&mode!="公交",
                    modifier=Modifier.fillMaxWidth().padding(horizontal=22.dp).height(52.dp),
                    colors=ButtonDefaults.buttonColors(containerColor=ActionBlue)
                ){Text("开始",fontSize=18.sp)}
            }
        }
    }
    if(safety) {
        AlertDialog(
            onDismissRequest={safety=false},
            title={Text("安全提示")},
            text={Text("地图和路线可能不准确。请始终遵守交通规则，注意实际道路标志与周围环境。")},
            dismissButton={TextButton(onClick={safety=false}){Text("拒绝")}},
            confirmButton={TextButton(onClick={
                safety=false
                (context as? MainActivity)?.requestNotificationPermission()
                onStart()
            }){Text("接受")}}
        )
    }
}

@Composable
private fun MiniElevation() {
    Canvas(Modifier.fillMaxWidth().height(62.dp).padding(horizontal=24.dp)) {
        val path=Path().apply{moveTo(0f,size.height*.8f);lineTo(size.width*.15f,size.height*.55f);lineTo(size.width*.35f,size.height*.68f);lineTo(size.width*.58f,size.height*.25f);lineTo(size.width*.78f,size.height*.46f);lineTo(size.width,size.height*.2f)}
        drawPath(path,RoutePurple,style=Stroke(width=4f))
    }
}

@Composable
private fun ActiveNavigationScreen(onBack:()->Unit) {
    Box(Modifier.fillMaxSize()) {
        MapCanvas(route="驾车",modifier=Modifier.fillMaxSize())
        Surface(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal=10.dp,vertical=8.dp).height(116.dp),shape=RoundedCornerShape(18.dp),color=Color.White,shadowElevation=6.dp) {
            Row(Modifier.fillMaxSize().padding(16.dp),verticalAlignment=Alignment.CenterVertically) {
                Text("↱",fontSize=48.sp,color=OrganicGreen,fontWeight=FontWeight.Bold)
                Column(Modifier.padding(start=18.dp).weight(1f)){Text("16 公里",fontSize=25.sp,fontWeight=FontWeight.Bold,color=Color.Black);Text("Lake Mendocino Drive",fontSize=16.sp,color=Color.DarkGray)}
                Text("✕",fontSize=24.sp,color=Color.Black,modifier=Modifier.clickable(onClick=onBack))
            }
        }
        Surface(Modifier.fillMaxWidth().height(188.dp).align(Alignment.BottomCenter).navigationBarsPadding(),shape=RoundedCornerShape(topStart=26.dp,topEnd=26.dp),color=Color.White,shadowElevation=10.dp) {
            Row(Modifier.fillMaxSize().padding(20.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceEvenly) {
                NavStat("0","公里/小时")
                NavStat("1:31","剩余")
                NavStat("04:04","到达")
                NavStat("29","公里")
            }
        }
    }
}

@Composable
private fun NavStat(value:String,label:String) {
    Column(horizontalAlignment=Alignment.CenterHorizontally){Text(value,fontSize=25.sp,fontWeight=FontWeight.Bold,color=Color.Black);Text(label,fontSize=13.sp,color=Color.Gray)}
}

@Composable
private fun AddPlaceScreen(onBack:()->Unit) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            AppTopBar("添加地点",onBack)
            Text("将地点添加到 OpenStreetMap",fontSize=18.sp,color=Color.Gray,modifier=Modifier.padding(24.dp))
            LabeledField("名称",name,{name=it})
            LabeledField("类别",category,{category=it})
            LabeledField("地址","Vista del Lago Road",{})
            Spacer(Modifier.weight(1f))
            Button(onClick=onBack,enabled=name.isNotEmpty(),modifier=Modifier.fillMaxWidth().padding(24.dp).height(54.dp),colors=ButtonDefaults.buttonColors(containerColor=OrganicGreen)){Text("保存",fontSize=18.sp)}
        }
    }
}
