package com.blackboxbench.reproduction

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.view.WindowCompat
import kotlinx.coroutines.delay

private val Indigo = Color(0xFF3F51B5)
private val Pink = Color(0xFFFF0054)
private val Green = Color(0xFF58735A)
private val SoftGray = Color(0xFF777777)
private val Divider = Color(0xFFE2E2E2)

data class Song(val no:Int,val title:String,val artist:String,val album:String,val duration:Int,val year:Int,val genre:String)

private val library = listOf(
    Song(1,"起飞检查单","夜航电台","北纬四十",214,2024,"合成器流行"),
    Song(2,"跑道灯","夜航电台","北纬四十",198,2024,"合成器流行"),
    Song(3,"云层之上","夜航电台","北纬四十",243,2024,"合成器流行"),
    Song(4,"夜间巡航","夜航电台","北纬四十",261,2024,"合成器流行"),
    Song(5,"穿越颠簸","夜航电台","北纬四十",187,2024,"合成器流行"),
    Song(6,"降落信号","夜航电台","北纬四十",225,2024,"合成器流行"),
    Song(7,"行李转盘","夜航电台","北纬四十",176,2024,"合成器流行"),
    Song(8,"时差","夜航电台","北纬四十",232,2024,"合成器流行"),
    Song(1,"载波","夜航电台","信号塔",205,2026,"合成器流行"),
    Song(2,"静默频道","夜航电台","信号塔",189,2026,"合成器流行"),
    Song(3,"握手协议","夜航电台","信号塔",221,2026,"合成器流行"),
    Song(4,"掉线","夜航电台","信号塔",168,2026,"合成器流行"),
    Song(1,"早春序曲","林间合唱团","叶脉",256,2023,"民谣"),
    Song(2,"橡树下的信","林间合唱团","叶脉",232,2023,"民谣"),
    Song(3,"六月的雨","林间合唱团","叶脉",274,2023,"民谣"),
    Song(4,"采蘑菇的人","林间合唱团","叶脉",208,2023,"民谣"),
    Song(5,"落叶归根","林间合唱团","叶脉",245,2023,"民谣"),
    Song(6,"冬眠","林间合唱团","叶脉",298,2023,"民谣"),
    Song(1,"开机音","像素猫","8-bit 心跳",96,2025,"电子"),
    Song(2,"满血复活","像素猫","8-bit 心跳",142,2025,"电子"),
    Song(3,"隐藏关卡","像素猫","8-bit 心跳",171,2025,"电子"),
    Song(1,"投币口","像素猫","街机黄昏",188,2026,"电子"),
    Song(1,"快门开启","山地静默","长曝光",402,2024,"后摇"),
    Song(2,"星轨","山地静默","长曝光",468,2024,"后摇")
)
private val morningTitles=setOf("起飞检查单","跑道灯","早春序曲","开机音","夜间巡航","橡树下的信")
private val nightTitles=setOf("载波","快门开启","星轨","静默频道","隐藏关卡")
private fun clock(sec:Int)="%d:%02d".format(sec/60,sec%60)

class MainActivity:ComponentActivity(){
    private val imagesPermission=registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){}
    private val audioPermission=registerForActivityResult(ActivityResultContracts.RequestPermission()){requestImages()}
    private val notificationPermission=registerForActivityResult(ActivityResultContracts.RequestPermission()){requestAudio()}
    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState)
        window.statusBarColor=AndroidColor.rgb(63,81,181)
        window.navigationBarColor=AndroidColor.BLACK
        WindowCompat.getInsetsController(window,window.decorView).isAppearanceLightStatusBars=false
        setContent{MaterialTheme(colorScheme=lightColorScheme(primary=Indigo,secondary=Pink,surface=Color.White,background=Color(0xFFFAFAFA))){VinylApp()}}
    }
    private fun requestPermissionsInOrder(){
        if(Build.VERSION.SDK_INT>=33&&ActivityCompat.checkSelfPermission(this,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS) else requestAudio()
    }
    private fun requestAudio(){
        if(Build.VERSION.SDK_INT>=33&&ActivityCompat.checkSelfPermission(this,Manifest.permission.READ_MEDIA_AUDIO)!=PackageManager.PERMISSION_GRANTED)
            audioPermission.launch(Manifest.permission.READ_MEDIA_AUDIO) else requestImages()
    }
    private fun requestImages(){
        if(Build.VERSION.SDK_INT>=33){
            val needed=arrayOf(Manifest.permission.READ_MEDIA_IMAGES,Manifest.permission.READ_MEDIA_VIDEO)
                .filter{ActivityCompat.checkSelfPermission(this,it)!=PackageManager.PERMISSION_GRANTED}.toTypedArray()
            if(needed.isNotEmpty())imagesPermission.launch(needed)
        }
    }
}

@Composable private fun VinylApp(){
    val context=LocalContext.current
    val prefs=remember{context.getSharedPreferences("vinyl_state",Context.MODE_PRIVATE)}
    var screen by rememberSaveable{mutableStateOf("library")}
    var selectedTab by rememberSaveable{mutableIntStateOf(prefs.getInt("tab",0))}
    var drawer by remember{mutableStateOf(false)}
    var globalMenu by remember{mutableStateOf(false)}
    var dialog by remember{mutableStateOf("")}
    var dialogSong by remember{mutableStateOf<Song?>(null)}
    var editSong by remember{mutableStateOf<Song?>(null)}
    var detailTitle by remember{mutableStateOf("")}
    var detailSubtitle by remember{mutableStateOf("")}
    var detailSongs by remember{mutableStateOf(emptyList<Song>())}
    var playlistDetail by remember{mutableStateOf("")}
    var newName by remember{mutableStateOf("")}
    var searchQuery by rememberSaveable{mutableStateOf("")}
    var folderDepth by rememberSaveable{mutableIntStateOf(0)}
    var repeatMode by rememberSaveable{mutableIntStateOf(0)}
    var shuffle by rememberSaveable{mutableStateOf(false)}
    var timerCurrentOnly by remember{mutableStateOf(false)}
    var playerMenu by remember{mutableStateOf(false)}
    var savingQueue by remember{mutableStateOf(false)}

    val favorites=remember{mutableStateListOf<String>().apply{addAll(prefs.getStringSet("favorites",emptySet())?:emptySet())}}
    val customNames=remember{mutableStateListOf<String>().apply{addAll((prefs.getString("playlists","")?:"").split("|").filter{it.isNotBlank()})}}
    val customTracks=remember{mutableStateMapOf<String,List<String>>().apply{
        customNames.forEach{name->put(name,prefs.getString("playlist_$name","")?.split("|")?.filter{it.isNotBlank()}?:emptyList())}
    }}
    val categories=remember{mutableStateMapOf(
        "歌曲" to prefs.getBoolean("cat_歌曲",true),"专辑" to prefs.getBoolean("cat_专辑",true),
        "艺术家" to prefs.getBoolean("cat_艺术家",true),"音乐类型" to prefs.getBoolean("cat_音乐类型",true),
        "播放列表" to prefs.getBoolean("cat_播放列表",true)
    )}
    val rememberedSong=remember{prefs.getString("current",null)?.let{t->library.firstOrNull{it.title==t}}}
    var current by remember{mutableStateOf(rememberedSong)}
    var queue by remember{mutableStateOf(library)}
    var playing by remember{mutableStateOf(false)}
    var position by remember{mutableIntStateOf(0)}
    val media=remember{MediaPlayer.create(context,R.raw.ambient).apply{isLooping=true}}
    DisposableEffect(Unit){onDispose{media.release()}}
    LaunchedEffect(playing){if(playing)runCatching{media.start()}else runCatching{if(media.isPlaying)media.pause()}}
    LaunchedEffect(playing,current?.title){
        while(playing&&current!=null){delay(1000);position++
            if(position>=(current?.duration?:1)){val i=queue.indexOfFirst{it.title==current?.title};current=queue[(i+1).mod(queue.size)];position=0;prefs.edit().putString("current",current?.title).apply()}
        }
    }
    fun startSong(song:Song,list:List<Song>){queue=if(shuffle)list.shuffled()else list;current=song;position=0;playing=true;prefs.edit().putString("current",song.title).apply()}
    fun openDetail(title:String,subtitle:String,songs:List<Song>,playlist:String=""){detailTitle=title;detailSubtitle=subtitle;detailSongs=songs;playlistDetail=playlist;screen="detail"}
    fun persistFavorites(){prefs.edit().putStringSet("favorites",favorites.toSet()).apply()}
    fun persistPlaylists(){prefs.edit().putString("playlists",customNames.joinToString("|")).apply();customTracks.forEach{(n,t)->prefs.edit().putString("playlist_$n",t.joinToString("|")).apply()}}
    fun playlistSongs(name:String)=when(name){"收藏夹"->library.filter{favorites.contains(it.title)};"晨间通勤"->library.filter{morningTitles.contains(it.title)};"深夜编码"->library.filter{nightTitles.contains(it.title)};else->library.filter{customTracks[name]?.contains(it.title)==true}}
    fun nextSong(delta:Int){val now=current?:return;val i=queue.indexOfFirst{it.title==now.title}.coerceAtLeast(0);current=queue[(i+delta+queue.size).mod(queue.size)];position=0;prefs.edit().putString("current",current?.title).apply()}
    fun songAction(action:String,song:Song){
        dialogSong=song
        when(action){
            "next"->{val m=queue.toMutableList();val at=current?.let{n->m.indexOfFirst{it.title==n.title}}?:-1;m.removeAll{it.title==song.title};m.add((at+1).coerceAtLeast(0),song);queue=m}
            "queue"->if(queue.none{it.title==song.title})queue=queue+song
            "playlist"->dialog="addPlaylist"
            "album"->openDetail(song.album,"${song.artist} · ${song.year}",library.filter{it.album==song.album})
            "artist"->openDetail(song.artist,"${library.count{it.artist==song.artist}} 首歌曲",library.filter{it.artist==song.artist})
            "share"->dialog="share";"tag"->{editSong=song;screen="tag"};"details"->dialog="details";"ringtone"->dialog="ringtone";"delete"->dialog="deleteSong"
        }
    }
    BackHandler{
        when{dialog.isNotEmpty()->dialog="";drawer->drawer=false;playerMenu->playerMenu=false;screen=="player"->screen="library";screen=="tag"->screen="detail";screen in listOf("detail","folders","settings","about","search")->screen="library"}
    }

    Box(Modifier.fillMaxSize().background(Indigo)){
      Box(Modifier.fillMaxSize().statusBarsPadding().background(Color(0xFFFAFAFA))){
        when(screen){
            "library"->LibraryScreen(selectedTab,listOf("歌曲","专辑","艺术家","音乐类型","播放列表").filter{categories[it]==true},favorites.toSet(),customNames,::playlistSongs,
                {selectedTab=it;prefs.edit().putInt("tab",it).apply()},{drawer=true},{screen="search";searchQuery=""},{globalMenu=true},
                {s,l->startSong(s,l)},::songAction,::openDetail)
            "search"->SearchScreen(searchQuery,{searchQuery=it},favorites.toSet(),{screen="library"},
                {s->startSong(s,library.filter{it.title.contains(searchQuery,true)||it.artist.contains(searchQuery,true)||it.album.contains(searchQuery,true)})},::songAction)
            "detail"->DetailScreen(detailTitle,detailSubtitle,detailSongs,playlistDetail,favorites.toSet(),{screen="library"},
                {startSong(it,detailSongs)},{detailSongs.firstOrNull()?.let{startSong(it,detailSongs)}},::songAction,
                {dialog=if(playlistDetail.isNotEmpty())"playlistMenu" else "detailMenu"})
            "player"->PlayerScreen(current?:library.first(),queue,playing,position,current?.let{favorites.contains(it.title)}==true,repeatMode,shuffle,playerMenu,
                {screen="library"},{current?.let{if(favorites.contains(it.title))favorites.remove(it.title)else favorites.add(it.title);persistFavorites()}},
                {playing=!playing},{nextSong(-1)},{nextSong(1)},{repeatMode=(repeatMode+1)%3},{shuffle=!shuffle},{playerMenu=!playerMenu},{playerMenu=false},
                {playerMenu=false;when(it){"clear"->dialog="clearQueue";"save"->{savingQueue=true;newName="";dialog="newPlaylist"};"timer"->dialog="sleep";"equalizer"->dialog="equalizer"}},
                {startSong(it,queue)},::songAction)
            "folders"->FolderScreen(folderDepth,current,{folderDepth=it},{drawer=true},{startSong(library.first(),library)},::songAction)
            "settings"->SettingsScreen({screen="library"},{dialog="categories"},{dialog="equalizer"},{dialog="exported"},{dialog="imported"})
            "about"->AboutScreen({screen="library"},{dialog="changelog"})
            "tag"->TagEditorScreen(editSong?:library.first(),{screen="detail"},{dialog="tagSaved"})
        }
        if(current!=null&&screen in listOf("library","detail","folders"))MiniPlayer(current!!,playing,{screen="player"},{playing=!playing},Modifier.align(Alignment.BottomCenter))
        if(globalMenu){
            Box(Modifier.fillMaxSize().clickable{globalMenu=false})
            Surface(color=Color.White,shadowElevation=10.dp,modifier=Modifier.align(Alignment.TopEnd).padding(top=66.dp,end=8.dp).width(236.dp)){
                Column{
                    Row(Modifier.fillMaxWidth().height(56.dp).clickable{globalMenu=false;shuffle=true;startSong(library.shuffled().first(),library)}.padding(horizontal=18.dp),verticalAlignment=Alignment.CenterVertically){Text("随机播放所有歌曲",fontSize=18.sp)}
                    Row(Modifier.fillMaxWidth().height(56.dp).clickable{globalMenu=false;savingQueue=false;newName="";dialog="newPlaylist"}.padding(horizontal=18.dp),verticalAlignment=Alignment.CenterVertically){Text("新建播放列表",fontSize=18.sp)}
                }
            }
        }
        if(drawer)NavigationDrawer(current,{drawer=false}){drawer=false;when(it){"library"->screen="library";"folders"->{folderDepth=0;screen="folders"};"rescan"->dialog="rescan";"settings"->screen="settings";"about"->screen="about"}}
        when(dialog){
            "newPlaylist"->NameDialog("新建播放列表",newName,"创建",{newName=it},{dialog=""}){
                if(newName.isNotBlank()){if(!customNames.contains(newName))customNames.add(newName);customTracks[newName]=if(savingQueue)queue.map{it.title}else emptyList();persistPlaylists();dialog=""}
            }
            "addPlaylist"->PlaylistPicker(listOf("收藏夹","晨间通勤","深夜编码")+customNames,{dialog=""},{dialog="newPlaylist";savingQueue=false}){name->
                val s=dialogSong;if(name=="收藏夹"&&s!=null){if(!favorites.contains(s.title))favorites.add(s.title);persistFavorites()}
                else if(s!=null&&customNames.contains(name)){customTracks[name]=((customTracks[name]?:emptyList())+s.title).distinct();persistPlaylists()};dialog=""
            }
            "details"->DetailsDialog(dialogSong?:current?:library.first()){dialog=""}
            "share"->InfoDialog("分享","已准备分享“${dialogSong?.title?:""}”。"){dialog=""}
            "ringtone"->InfoDialog("设为铃声","已将“${dialogSong?.title?:""}”设为铃声。"){dialog=""}
            "deleteSong"->ConfirmDialog("从设备中删除","确定要从设备中删除“${dialogSong?.title?:""}”吗？","删除",{dialog=""},{dialog=""})
            "clearQueue"->ConfirmDialog("清空播放队列","清空后将停止当前播放。","清空",{playing=false;current=null;prefs.edit().remove("current").apply();dialog=""},{dialog=""})
            "sleep"->SleepDialog(timerCurrentOnly,{timerCurrentOnly=it},{dialog=""},{dialog=""})
            "equalizer"->InfoDialog("均衡器","系统均衡器不可用。你仍可在设置中调整 ReplayGain。"){dialog=""}
            "rescan"->ConfirmDialog("重新扫描媒体库","这会清空并重建应用内部的歌曲数据库。设备上的歌曲不会被删除，但播放历史可能会被清除。","重新扫描媒体库",{dialog=""},{dialog=""})
            "categories"->CategoriesDialog(categories,{dialog=""},{categories.keys.forEach{categories[it]=true}}){
                categories.forEach{(k,v)->prefs.edit().putBoolean("cat_$k",v).apply()};dialog="";selectedTab=selectedTab.coerceIn(0,(categories.count{it.value}-1).coerceAtLeast(0))
            }
            "detailMenu"->SimpleMenuDialog("列表操作",listOf("作为下一首播放","加入播放队列","加入播放列表…")){dialog=""}
            "playlistMenu"->PlaylistMenuDialog({dialog=""},{dialog="renamePlaylist";newName=playlistDetail},{dialog="deletePlaylist"})
            "renamePlaylist"->NameDialog("重命名播放列表",newName,"重命名",{newName=it},{dialog=""}){
                val old=playlistDetail;if(newName.isNotBlank()&&customNames.contains(old)){val idx=customNames.indexOf(old);customNames[idx]=newName;customTracks[newName]=customTracks.remove(old)?:emptyList();playlistDetail=newName;detailTitle=newName;persistPlaylists()};dialog=""
            }
            "deletePlaylist"->ConfirmDialog("删除播放列表","确定要删除播放列表“$playlistDetail”吗？","删除",{customNames.remove(playlistDetail);customTracks.remove(playlistDetail);persistPlaylists();dialog="";screen="library"},{dialog=""})
            "changelog"->ChangelogDialog{dialog=""}
            "tagSaved"->InfoDialog("音乐标签编辑器","标签已保存，媒体库聚合已刷新。"){dialog="";screen="detail"}
            "exported"->InfoDialog("导出设置","设置已导出。"){dialog=""}
            "imported"->InfoDialog("导入设置","已导入设置。"){dialog=""}
        }
      }
    }
}

@Composable private fun Header(title:String="Vinyl Music Player",back:Boolean=false,onNav:()->Unit,search:Boolean=false,onSearch:()->Unit={},overflow:Boolean=false,onOverflow:()->Unit={}){
    Row(Modifier.fillMaxWidth().height(72.dp).background(Indigo).padding(horizontal=8.dp),verticalAlignment=Alignment.CenterVertically){
        Box(Modifier.width(52.dp).fillMaxHeight().clickable(onClick=onNav),contentAlignment=Alignment.Center){Text(if(back)"‹"else"☰",color=Color.White,fontSize=28.sp)}
        Text(title,color=Color.White,fontWeight=FontWeight.Medium,fontSize=22.sp,maxLines=1,overflow=TextOverflow.Ellipsis,modifier=Modifier.weight(1f))
        if(search)Box(Modifier.width(52.dp).fillMaxHeight().clickable(onClick=onSearch),contentAlignment=Alignment.Center){Text("⌕",color=Color.White,fontSize=30.sp)}
        if(overflow)Box(Modifier.width(52.dp).fillMaxHeight().clickable(onClick=onOverflow),contentAlignment=Alignment.Center){Text("⋮",color=Color.White,fontSize=28.sp)}
    }
}

@Composable private fun LibraryScreen(
    selectedTab:Int,enabledTabs:List<String>,favorites:Set<String>,customNames:List<String>,playlistSongs:(String)->List<Song>,
    onSelectTab:(Int)->Unit,onDrawer:()->Unit,onSearch:()->Unit,onGlobalMenu:()->Unit,onSong:(Song,List<Song>)->Unit,
    onSongAction:(String,Song)->Unit,onOpenDetail:(String,String,List<Song>,String)->Unit
){
    val tabs=if(enabledTabs.isEmpty())listOf("歌曲")else enabledTabs
    val tab=tabs.getOrElse(selectedTab){tabs.first()}
    Column(Modifier.fillMaxSize().padding(bottom=62.dp)){
        Header(onNav=onDrawer,search=true,onSearch=onSearch,overflow=true,onOverflow=onGlobalMenu)
        Row(Modifier.fillMaxWidth().height(48.dp).background(Indigo).horizontalScroll(rememberScrollState())){
            tabs.forEachIndexed{i,name->Column(Modifier.width(92.dp).fillMaxHeight().clickable{onSelectTab(i)},horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Bottom){
                Text(name,color=if(i==selectedTab)Color.White else Color.White.copy(alpha=.68f),fontSize=17.sp,fontWeight=if(i==selectedTab)FontWeight.Bold else FontWeight.Normal,modifier=Modifier.padding(bottom=10.dp),maxLines=1)
                Box(Modifier.fillMaxWidth().height(2.dp).background(if(i==selectedTab)Pink else Color.Transparent))
            }}
        }
        when(tab){
            "歌曲"->SongList(library,favorites,{onSong(it,library)},onSongAction,true)
            "专辑"->AlbumGrid(onOpenDetail)
            "艺术家"->GroupList(library.groupBy{it.artist}.toList(),"♬"){n,s->onOpenDetail(n,"${s.size} 首歌曲",s,"")}
            "音乐类型"->GroupList(library.groupBy{it.genre}.toList(),"◉"){n,s->onOpenDetail(n,"${s.size} 首歌曲",s,"")}
            "播放列表"->PlaylistsView(favorites,customNames,playlistSongs){n,s->onOpenDetail(n,"${s.size} 首歌曲",s,n)}
        }
    }
}
@Composable private fun SongList(songs:List<Song>,favorites:Set<String>,onSong:(Song)->Unit,onAction:(String,Song)->Unit,showShuffle:Boolean=false){
    LazyColumn(Modifier.fillMaxSize()){
        if(showShuffle)item{Row(Modifier.fillMaxWidth().height(64.dp).clickable{songs.shuffled().firstOrNull()?.let(onSong)}.padding(horizontal=24.dp),verticalAlignment=Alignment.CenterVertically){
            Text("⤨",fontSize=30.sp,color=Pink,modifier=Modifier.width(54.dp));Text("随机播放所有歌曲",color=Pink,fontSize=20.sp,fontWeight=FontWeight.Medium)
        };ThinDivider()}
        items(songs,key={it.artist+it.album+it.no}){song->SongRow(song,favorites.contains(song.title),{onSong(song)},{onAction(it,song)})}
    }
}
@Composable private fun SongRow(song:Song,favorite:Boolean,onClick:()->Unit,onAction:(String)->Unit){
    var menu by remember{mutableStateOf(false)}
    Row(Modifier.fillMaxWidth().height(76.dp).clickable(onClick=onClick).padding(horizontal=16.dp),verticalAlignment=Alignment.CenterVertically){
        Cover(song,48.dp)
        Column(Modifier.weight(1f).padding(start=14.dp)){Row{Text(song.title,fontSize=18.sp,maxLines=1,overflow=TextOverflow.Ellipsis);if(favorite)Text(" ♥",color=Pink)};Text(song.artist,color=SoftGray,fontSize=15.sp,maxLines=1)}
        Box{Text("⋮",fontSize=28.sp,color=SoftGray,textAlign=TextAlign.Center,modifier=Modifier.width(44.dp).padding(vertical=12.dp).clickable{menu=true})
            DropdownMenu(menu,{menu=false}){
                listOf("作为下一首播放" to "next","加入播放队列" to "queue","加入播放列表…" to "playlist","查看专辑" to "album","查看艺术家" to "artist","分享" to "share","音乐标签编辑器" to "tag","详情" to "details","设为铃声" to "ringtone","从设备中删除" to "delete").forEach{(l,a)->
                    DropdownMenuItem({Text(l,color=if(a=="delete")Color(0xFFE53900)else Color(0xFF222222))},{menu=false;onAction(a)})
                }
            }
        }
    };ThinDivider(78.dp)
}
@Composable private fun Cover(song:Song,boxSize:androidx.compose.ui.unit.Dp){
    val colors=listOf(Color(0xFF58735A),Color(0xFF6A637D),Color(0xFF8A643E),Color(0xFF526C80),Color(0xFF805C68),Color(0xFF4F7775));val c=colors[kotlin.math.abs(song.album.hashCode())%colors.size]
    Box(Modifier.size(boxSize).background(c).border(1.dp,Color.White.copy(alpha=.5f)),contentAlignment=Alignment.Center){
        Canvas(Modifier.fillMaxSize().padding(4.dp)){drawRect(Color.White.copy(alpha=.35f),style=Stroke(1.5f));drawRect(Color.White.copy(alpha=.45f),Offset(size.width*.12f,size.height*.12f),androidx.compose.ui.geometry.Size(size.width*.76f,size.height*.76f),style=Stroke(1.5f));drawCircle(Color.White.copy(alpha=.8f),size.minDimension*.11f,Offset(size.width*.70f,size.height*.28f))}
        Text(song.album.take(4),color=Color.White,fontSize=(boxSize.value/9).sp,modifier=Modifier.align(Alignment.BottomCenter).padding(bottom=2.dp),maxLines=1)
    }
}
@Composable private fun Cover(song:Song,modifier:Modifier){
    val colors=listOf(Color(0xFF58735A),Color(0xFF6A637D),Color(0xFF8A643E),Color(0xFF526C80),Color(0xFF805C68),Color(0xFF4F7775));val c=colors[kotlin.math.abs(song.album.hashCode())%colors.size]
    Box(modifier.aspectRatio(1f).background(c),contentAlignment=Alignment.Center){
        Canvas(Modifier.fillMaxSize().padding(13.dp)){drawRoundRect(Color.White.copy(alpha=.46f),style=Stroke(3f),cornerRadius=androidx.compose.ui.geometry.CornerRadius(12f));drawRoundRect(Color.White.copy(alpha=.55f),Offset(size.width*.08f,size.height*.08f),androidx.compose.ui.geometry.Size(size.width*.84f,size.height*.84f),androidx.compose.ui.geometry.CornerRadius(10f),style=Stroke(3f));drawCircle(Color.White.copy(alpha=.86f),size.minDimension*.13f,Offset(size.width*.69f,size.height*.28f))}
        Text(song.album,color=Color.White,fontSize=20.sp,fontWeight=FontWeight.Light,modifier=Modifier.align(Alignment.BottomCenter).padding(bottom=16.dp))
    }
}
@Composable private fun AlbumGrid(onOpen:(String,String,List<Song>,String)->Unit){
    val albums=library.groupBy{it.album}.toList()
    LazyVerticalGrid(GridCells.Fixed(2),Modifier.fillMaxSize().padding(12.dp),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
        gridItems(albums){(n,s)->Column(Modifier.clickable{onOpen(n,"${s.first().artist} · ${s.first().year}",s,"")}){Cover(s.first(),Modifier.fillMaxWidth());Text(n,fontSize=18.sp,fontWeight=FontWeight.Medium,modifier=Modifier.padding(top=8.dp));Text("${s.first().artist} · ${s.size} 首",fontSize=14.sp,color=SoftGray)}}
    }
}
@Composable private fun GroupList(groups:List<Pair<String,List<Song>>>,icon:String,onOpen:(String,List<Song>)->Unit){
    LazyColumn(Modifier.fillMaxSize()){items(groups){(n,s)->Row(Modifier.fillMaxWidth().height(82.dp).clickable{onOpen(n,s)}.padding(horizontal=22.dp),verticalAlignment=Alignment.CenterVertically){
        Text(icon,fontSize=29.sp,color=SoftGray,modifier=Modifier.width(54.dp));Column(Modifier.weight(1f)){Text(n,fontSize=20.sp);Text("${s.size} 首歌曲",fontSize=16.sp,color=SoftGray)};Text("›",fontSize=30.sp,color=SoftGray)
    };ThinDivider(78.dp)}}
}
@Composable private fun PlaylistsView(favorites:Set<String>,customNames:List<String>,playlistSongs:(String)->List<Song>,onOpen:(String,List<Song>)->Unit){
    val fixed=listOf(Triple("最近添加","▣",library),Triple("播放历史","◷",library.take(8)),Triple("最近未播放过","◔",library.drop(8)),Triple("最喜爱的歌曲","↗",library.filter{favorites.contains(it.title)}),Triple("收藏夹","♥",playlistSongs("收藏夹")),Triple("晨间通勤","☀",playlistSongs("晨间通勤")),Triple("深夜编码","☾",playlistSongs("深夜编码")))
    LazyColumn(Modifier.fillMaxSize()){items(fixed){(n,i,s)->PlaylistRow(n,i,s){onOpen(n,s)}};items(customNames){n->PlaylistRow(n,"♬",playlistSongs(n)){onOpen(n,playlistSongs(n))}}}
}
@Composable private fun PlaylistRow(name:String,icon:String,songs:List<Song>,onClick:()->Unit){
    Row(Modifier.fillMaxWidth().height(80.dp).clickable(onClick=onClick).padding(horizontal=22.dp),verticalAlignment=Alignment.CenterVertically){
        Text(icon,fontSize=29.sp,color=SoftGray,modifier=Modifier.width(56.dp));Column(Modifier.weight(1f)){Text(name,fontSize=20.sp);Text("${songs.size} 首歌曲",fontSize=16.sp,color=SoftGray)};Text("⋮",fontSize=28.sp,color=SoftGray)
    };ThinDivider(78.dp)
}
@Composable private fun DetailScreen(title:String,subtitle:String,songs:List<Song>,playlistName:String,favorites:Set<String>,onBack:()->Unit,onSong:(Song)->Unit,onPlayAll:()->Unit,onSongAction:(String,Song)->Unit,onMenu:()->Unit){
    Column(Modifier.fillMaxSize().padding(bottom=62.dp)){Header(title,true,onBack,overflow=true,onOverflow=onMenu)
        Row(Modifier.fillMaxWidth().height(70.dp).padding(horizontal=20.dp),verticalAlignment=Alignment.CenterVertically){Text("⤨",fontSize=28.sp,color=Pink,modifier=Modifier.width(48.dp).clickable{onPlayAll()});Text(if(playlistName.isNotEmpty())"${songs.size} 首歌曲 · ${clock(songs.sumOf{it.duration})}"else subtitle,color=SoftGray,fontSize=15.sp,modifier=Modifier.weight(1f));Text("▶",fontSize=28.sp,color=Pink,modifier=Modifier.padding(14.dp).clickable{onPlayAll()})}
        ThinDivider();if(songs.isEmpty())Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text("没有歌曲",color=SoftGray,fontSize=19.sp)}else SongList(songs,favorites,onSong,onSongAction)
    }
}
@Composable private fun SearchScreen(query:String,onQuery:(String)->Unit,favorites:Set<String>,onBack:()->Unit,onSong:(Song)->Unit,onSongAction:(String,Song)->Unit){
    val results=if(query.isBlank())emptyList()else library.filter{it.title.contains(query,true)||it.artist.contains(query,true)||it.album.contains(query,true)}
    Column(Modifier.fillMaxSize()){Row(Modifier.fillMaxWidth().height(72.dp).background(Indigo).padding(horizontal=10.dp),verticalAlignment=Alignment.CenterVertically){
        Text("‹",color=Color.White,fontSize=38.sp,modifier=Modifier.width(48.dp).clickable{onBack()})
        TextField(query,onQuery,modifier=Modifier.weight(1f),singleLine=true,placeholder={Text("搜索歌曲、艺人或专辑",color=Color.White.copy(alpha=.7f))},colors=TextFieldDefaults.colors(focusedTextColor=Color.White,unfocusedTextColor=Color.White,focusedContainerColor=Color.Transparent,unfocusedContainerColor=Color.Transparent,focusedIndicatorColor=Color.White,unfocusedIndicatorColor=Color.White.copy(alpha=.5f)))
        if(query.isNotEmpty())Text("×",color=Color.White,fontSize=30.sp,modifier=Modifier.padding(10.dp).clickable{onQuery("")})
    };if(results.isEmpty())Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text(if(query.isBlank())"输入关键词搜索媒体库"else"没有找到结果",color=SoftGray,fontSize=18.sp)}
    else{Text("歌曲",color=Pink,fontSize=15.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(20.dp,14.dp,20.dp,4.dp));SongList(results,favorites,onSong,onSongAction)}}
}
@Composable private fun MiniPlayer(song:Song,playing:Boolean,onExpand:()->Unit,onPlay:()->Unit,modifier:Modifier=Modifier){
    Column(modifier.fillMaxWidth().background(Color.White).clickable{onExpand()}){Box(Modifier.fillMaxWidth().height(2.dp).background(Pink));Row(Modifier.fillMaxWidth().height(60.dp).padding(horizontal=18.dp),verticalAlignment=Alignment.CenterVertically){
        Text("⌃",fontSize=26.sp,color=SoftGray,modifier=Modifier.width(52.dp));Text(song.title,fontSize=18.sp,modifier=Modifier.weight(1f),maxLines=1,overflow=TextOverflow.Ellipsis);Text(if(playing)"Ⅱ"else"▶",fontSize=25.sp,color=SoftGray,textAlign=TextAlign.Center,modifier=Modifier.width(54.dp).clickable{onPlay()})
    }}
}
@Composable private fun PlayerScreen(song:Song,queue:List<Song>,playing:Boolean,position:Int,favorite:Boolean,repeatMode:Int,shuffle:Boolean,menuExpanded:Boolean,onClose:()->Unit,onFavorite:()->Unit,onPlay:()->Unit,onPrevious:()->Unit,onNext:()->Unit,onRepeat:()->Unit,onShuffle:()->Unit,onMenu:()->Unit,onDismissMenu:()->Unit,onMenuAction:(String)->Unit,onSong:(Song)->Unit,onSongAction:(String,Song)->Unit){
    val transition=rememberInfiniteTransition(label="vinyl");val angle by transition.animateFloat(0f,360f,infiniteRepeatable(tween(8000,easing=LinearEasing)),label="spin")
    Column(Modifier.fillMaxSize().background(Green)){Row(Modifier.fillMaxWidth().height(62.dp).padding(horizontal=14.dp),verticalAlignment=Alignment.CenterVertically){
        Text("×",fontSize=38.sp,color=Color.White,modifier=Modifier.width(54.dp).clickable{onClose()});Spacer(Modifier.weight(1f));Text(if(favorite)"♥"else"♡",fontSize=35.sp,color=Color.White,modifier=Modifier.width(54.dp).clickable{onFavorite()})
        Box{Text("⋮",fontSize=34.sp,color=Color.White,textAlign=TextAlign.Center,modifier=Modifier.width(42.dp).clickable{onMenu()});DropdownMenu(menuExpanded,onDismissMenu){DropdownMenuItem({Text("清空播放队列")},{onMenuAction("clear")});DropdownMenuItem({Text("保存播放队列")},{onMenuAction("save")});DropdownMenuItem({Text("睡眠定时器")},{onMenuAction("timer")});DropdownMenuItem({Text("均衡器")},{onMenuAction("equalizer")})}}
    };Box(Modifier.fillMaxWidth().height(356.dp).padding(horizontal=22.dp),contentAlignment=Alignment.Center){Box(Modifier.size(326.dp).clip(CircleShape).rotate(if(playing)angle else 0f)){Cover(song,Modifier.fillMaxSize())}}
        Row(Modifier.fillMaxWidth().padding(horizontal=10.dp),verticalAlignment=Alignment.CenterVertically){Text(clock(position),color=Color.White,fontSize=14.sp);Box(Modifier.weight(1f).height(3.dp).padding(horizontal=8.dp).background(Color.White.copy(alpha=.35f))){Box(Modifier.fillMaxWidth((position.toFloat()/song.duration).coerceIn(0f,1f)).height(3.dp).background(Color.White))};Text(clock(song.duration),color=Color.White,fontSize=14.sp)}
        Row(Modifier.fillMaxWidth().height(105.dp).padding(horizontal=30.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){
            Text(when(repeatMode){1->"↻";2->"↻¹";else->"↔"},fontSize=29.sp,color=if(repeatMode>0)Color.White else Color.White.copy(alpha=.35f),modifier=Modifier.clickable{onRepeat()});Text("◀|",fontSize=28.sp,color=Color.White,modifier=Modifier.clickable{onPrevious()})
            Surface(shape=CircleShape,color=Color.White,shadowElevation=7.dp,modifier=Modifier.size(72.dp).clickable{onPlay()}){Box(contentAlignment=Alignment.Center){Text(if(playing)"Ⅱ"else"▶",fontSize=31.sp,color=Color(0xFF1E2529))}}
            Text("|▶",fontSize=28.sp,color=Color.White,modifier=Modifier.clickable{onNext()});Text("⤨",fontSize=30.sp,color=if(shuffle)Color.White else Color.White.copy(alpha=.35f),modifier=Modifier.clickable{onShuffle()})
        }
        Surface(color=Color.White,shape=RoundedCornerShape(topStart=4.dp,topEnd=4.dp),modifier=Modifier.fillMaxWidth().weight(1f).padding(horizontal=16.dp)){LazyColumn{
            item{Row(Modifier.fillMaxWidth().height(76.dp).padding(horizontal=18.dp),verticalAlignment=Alignment.CenterVertically){Text("◉",fontSize=28.sp,color=SoftGray,modifier=Modifier.width(48.dp));Column(Modifier.weight(1f)){Text(song.title,fontSize=18.sp,fontWeight=FontWeight.Bold);Text(song.artist,color=SoftGray,fontSize=15.sp)};Text("⋮",fontSize=28.sp,color=SoftGray)};ThinDivider();Text("即将播放 · ${clock(queue.sumOf{it.duration})} · ${queue.size}/${queue.size}",color=Green,fontWeight=FontWeight.Bold,fontSize=15.sp,modifier=Modifier.padding(18.dp,14.dp))}
            items(queue.take(8)){q->Row(Modifier.fillMaxWidth().height(63.dp).clickable{onSong(q)}.padding(horizontal=18.dp),verticalAlignment=Alignment.CenterVertically){Text(q.no.toString(),fontSize=17.sp,color=SoftGray,modifier=Modifier.width(40.dp));Column(Modifier.weight(1f)){Text(q.title,fontSize=17.sp,maxLines=1);Text(q.artist,color=SoftGray,fontSize=14.sp)};Text("⋮",fontSize=26.sp,color=SoftGray,modifier=Modifier.clickable{onSongAction("details",q)})}}
        }}
    }
}
@Composable private fun FolderScreen(depth:Int,current:Song?,onDepth:(Int)->Unit,onDrawer:()->Unit,onSong:()->Unit,onSongAction:(String,Song)->Unit){
    var menu by remember{mutableStateOf(false)};var sortMenu by remember{mutableStateOf(false)}
    Column(Modifier.fillMaxSize().padding(bottom=if(current!=null)62.dp else 0.dp)){Header(if(depth==0)"Music"else if(depth==1)"Download"else"BlackBoxBench",onNav=onDrawer,overflow=true,onOverflow={menu=true})
        Row(Modifier.fillMaxWidth().height(52.dp).horizontalScroll(rememberScrollState()).padding(horizontal=12.dp),verticalAlignment=Alignment.CenterVertically){listOf("ROOT","STORAGE","EMULATED","0").forEach{Text("$it  ›  ",color=Indigo,fontSize=13.sp,modifier=Modifier.clickable{onDepth(0)})};if(depth>=1)Text("DOWNLOAD  ›  ",color=Indigo,fontSize=13.sp);if(depth>=2)Text("BLACKBOXBENCH",color=Indigo,fontSize=13.sp)}
        ThinDivider();when(depth){0->{val dirs=listOf("Alarms","Android","Audiobooks","DCIM","Documents","Download","Movies","Music","Notifications","Pictures","Podcasts");LazyColumn{items(dirs){n->Row(Modifier.fillMaxWidth().height(66.dp).clickable{if(n=="Download")onDepth(1)}.padding(horizontal=20.dp),verticalAlignment=Alignment.CenterVertically){Text("▣",fontSize=27.sp,color=SoftGray,modifier=Modifier.width(56.dp));Text(n,fontSize=18.sp,modifier=Modifier.weight(1f));Text("⋮",fontSize=26.sp,color=SoftGray)};ThinDivider(74.dp)}}}
            1->Row(Modifier.fillMaxWidth().height(70.dp).clickable{onDepth(2)}.padding(horizontal=20.dp),verticalAlignment=Alignment.CenterVertically){Text("▣",fontSize=27.sp,color=SoftGray,modifier=Modifier.width(56.dp));Text("BlackBoxBench",fontSize=18.sp,modifier=Modifier.weight(1f));Text("⋮",fontSize=26.sp,color=SoftGray)}
            else->{SongRow(library.first(),false,onSong){onSongAction(it,library.first())};Text("ambient.wav · 258 KB",color=SoftGray,fontSize=14.sp,modifier=Modifier.padding(start=79.dp))}
        }
        Box(Modifier.align(Alignment.End)){DropdownMenu(menu,{menu=false}){DropdownMenuItem({Text("扫描音乐")},{menu=false});DropdownMenuItem({Text("排序方式  ›")},{menu=false;sortMenu=true})};DropdownMenu(sortMenu,{sortMenu=false}){listOf("按首字符（正序）","按首字符（倒序）","Modified","Modified - Recent first").forEachIndexed{i,s->DropdownMenuItem({Row(verticalAlignment=Alignment.CenterVertically){RadioButton(i==0,{});Text(s)}},{sortMenu=false})}}}
    }
}
@Composable private fun SettingsScreen(onBack:()->Unit,onCategories:()->Unit,onEqualizer:()->Unit,onExport:()->Unit,onImport:()->Unit){
    val switches=remember{mutableStateMapOf("记住最后显示页面" to true,"白名单" to false,"着色导航栏" to true,"着色应用快捷方式" to true,"控制背景透明" to true,"传统通知" to false,"同步歌词" to true,"正在播放动画图标" to false,"显示轨道编号" to false,"失去音频焦点时降低音量" to true,"无缝播放" to false,"记住随机播放" to true,"最喜爱的歌曲智能列表" to true,"已跳过的歌曲" to false,"收集崩溃报告" to false,"同步队列与媒体标签更新" to false)}
    Column(Modifier.fillMaxSize()){Header("设置",true,onBack);LazyColumn(Modifier.fillMaxSize()){
        item{Section("媒体库")};item{SettingRow("媒体库类别","配置媒体库类别可见性和顺序。",onCategories)}
        items(listOf("记住最后显示页面","白名单")){k->SwitchRow(k,if(k=="记住最后显示页面")"启动时回到上次打开的视图"else"只显示起始目录：/storage/emulated/0/Music",switches[k]?:false){switches[k]=it}}
        item{SettingRow("黑名单","黑名单中文件夹将在媒体库中隐藏。")};item{Section("颜色")};item{SettingRow("全局主题","浅色")};item{SettingRow("Theme style","Classic")};item{ColorRow("主色调","主题主色调，默认为靛蓝色。",Indigo)};item{ColorRow("强调色","主题强调色，默认为粉色。",Pink)}
        items(listOf("着色导航栏","着色应用快捷方式")){k->SwitchRow(k,if(k=="着色导航栏")"用主色调着色导航栏。"else"用主色调着色应用快捷方式。",switches[k]?:false){switches[k]=it}}
        item{Section("通知与正在播放")};items(listOf("控制背景透明","传统通知","同步歌词","正在播放动画图标","显示轨道编号")){k->SwitchRow(k,when(k){"控制背景透明"->"使用透明的媒体控制背景。";"传统通知"->"使用旧式播放通知。";"同步歌词"->"在正在播放页滚动歌词。";"正在播放动画图标"->"播放时显示动画图标。";else->"在队列中显示轨道编号。"},switches[k]?:false){switches[k]=it}}
        item{SettingRow("正在播放外观","Card")};item{SettingRow("浏览时点击的默认操作","播放")};item{Section("图片、声音与播放列表")};item{SettingRow("自动下载元数据","仅 Wi-Fi")}
        items(listOf("失去音频焦点时降低音量","无缝播放","记住随机播放")){k->SwitchRow(k,"",switches[k]?:false){switches[k]=it}}
        item{SettingRow("均衡器","打开系统音效设置",onEqualizer)};item{SettingRow("ReplayGain 来源","无")};item{Section("智能列表与迁移")}
        items(listOf("最喜爱的歌曲智能列表","已跳过的歌曲")){k->SwitchRow(k,"",switches[k]?:false){switches[k]=it}}
        item{SettingRow("导出设置","保存当前应用设置",onExport)};item{SettingRow("导入设置","从备份恢复应用设置",onImport)};item{Section("实验性")}
        items(listOf("收集崩溃报告","同步队列与媒体标签更新")){k->SwitchRow(k,"",switches[k]?:false){switches[k]=it}};item{Spacer(Modifier.height(24.dp))}
    }}
}
@Composable private fun Section(t:String){Text(t,color=Pink,fontWeight=FontWeight.Bold,fontSize=15.sp,modifier=Modifier.padding(16.dp,20.dp,16.dp,8.dp))}
@Composable private fun SettingRow(t:String,s:String,onClick:()->Unit={}){Column(Modifier.fillMaxWidth().clickable(onClick=onClick).padding(horizontal=16.dp,vertical=12.dp)){Text(t,fontSize=19.sp);if(s.isNotEmpty())Text(s,color=SoftGray,fontSize=15.sp,modifier=Modifier.padding(top=3.dp))}}
@Composable private fun SwitchRow(t:String,s:String,c:Boolean,onChecked:(Boolean)->Unit){Row(Modifier.fillMaxWidth().clickable{onChecked(!c)}.padding(start=16.dp,end=10.dp,top=9.dp,bottom=9.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(t,fontSize=19.sp);if(s.isNotEmpty())Text(s,color=SoftGray,fontSize=14.sp)};Switch(c,onChecked)}}
@Composable private fun ColorRow(t:String,s:String,c:Color){Row(Modifier.fillMaxWidth().padding(16.dp,12.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(t,fontSize=19.sp);Text(s,color=SoftGray,fontSize=14.sp)};Box(Modifier.size(34.dp).clip(CircleShape).background(c))}}

@Composable private fun AboutScreen(onBack:()->Unit,onChangelog:()->Unit){
    Column(Modifier.fillMaxSize()){Header("关于",true,onBack);Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=24.dp),horizontalAlignment=Alignment.CenterHorizontally){
        Spacer(Modifier.height(28.dp));Surface(shape=RoundedCornerShape(20.dp),color=Indigo,modifier=Modifier.size(92.dp)){Box(contentAlignment=Alignment.Center){Text("V",fontSize=52.sp,fontWeight=FontWeight.Bold,color=Color.White)}};Text("Vinyl Music Player",fontSize=26.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=14.dp));Text("版本 1.11.0",color=SoftGray,fontSize=16.sp,modifier=Modifier.padding(top=4.dp))
        AboutLink("更新日志",onChangelog);AboutLink("应用介绍");AboutLink("在 GitHub 上创建分支");AboutLink("开源许可");Text("支持开发者",color=Pink,fontWeight=FontWeight.Bold,fontSize=15.sp,modifier=Modifier.fillMaxWidth().padding(top=26.dp,bottom=8.dp));AboutLink("报告问题");AboutLink("为应用评分")
        Text("维护者",color=Pink,fontWeight=FontWeight.Bold,fontSize=15.sp,modifier=Modifier.fillMaxWidth().padding(top=26.dp,bottom=12.dp));Text("Vinyl 开源社区",fontSize=19.sp);Text("贡献者",color=Pink,fontWeight=FontWeight.Bold,fontSize=15.sp,modifier=Modifier.fillMaxWidth().padding(top=28.dp,bottom=14.dp));Text("林深  ·  阿澈  ·  夏原  ·  北岛\\n微光  ·  远山  ·  雾桥  ·  木叶",fontSize=18.sp,lineHeight=34.sp,textAlign=TextAlign.Center);Text("以及许多其他贡献者",color=SoftGray,fontSize=16.sp,modifier=Modifier.padding(top=18.dp));Text("特别感谢",color=Pink,fontWeight=FontWeight.Bold,fontSize=15.sp,modifier=Modifier.fillMaxWidth().padding(top=28.dp,bottom=10.dp));Text("开源社区与所有音乐爱好者",fontSize=18.sp,modifier=Modifier.padding(bottom=36.dp))
    }}
}
@Composable private fun AboutLink(t:String,onClick:()->Unit={}){Row(Modifier.fillMaxWidth().height(54.dp).clickable(onClick=onClick),verticalAlignment=Alignment.CenterVertically){Text(t,fontSize=18.sp,modifier=Modifier.weight(1f));Text("›",fontSize=28.sp,color=SoftGray)}}
@Composable private fun TagEditorScreen(song:Song,onBack:()->Unit,onSave:()->Unit){
    var title by remember(song){mutableStateOf(song.title)};var album by remember(song){mutableStateOf(song.album)};var artist by remember(song){mutableStateOf(song.artist)};var genre by remember(song){mutableStateOf(song.genre)};var year by remember(song){mutableStateOf(song.year.toString())}
    Column(Modifier.fillMaxSize()){Header("音乐标签编辑器",true,onBack);Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(18.dp)){Cover(song,Modifier.fillMaxWidth().padding(horizontal=86.dp));EditField("歌曲",title){title=it};EditField("专辑",album){album=it};EditField("作者（每行一个）",artist){artist=it};EditField("音乐类型（每行一个）",genre){genre=it};EditField("年份",year){year=it};EditField("轨道",song.no.toString()){};EditField("光盘","1"){};EditField("歌词",""){};Spacer(Modifier.height(90.dp))}
        Box(Modifier.fillMaxWidth().padding(18.dp),contentAlignment=Alignment.CenterEnd){Surface(shape=CircleShape,color=Pink,shadowElevation=6.dp,modifier=Modifier.size(60.dp).clickable{onSave()}){Box(contentAlignment=Alignment.Center){Text("✓",color=Color.White,fontSize=28.sp)}}}
    }
}
@Composable private fun EditField(l:String,v:String,onV:(String)->Unit){TextField(v,onV,label={Text(l)},singleLine=l!="歌词",modifier=Modifier.fillMaxWidth().padding(top=10.dp))}
@Composable private fun NavigationDrawer(current:Song?,onDismiss:()->Unit,onRoute:(String)->Unit){
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=.45f)).clickable{onDismiss()}){Surface(color=Color.White,shadowElevation=12.dp,modifier=Modifier.fillMaxHeight().width(320.dp).clickable(enabled=false){}){
        Column{Box(Modifier.fillMaxWidth().height(214.dp).background(Green).clip(RoundedCornerShape(0.dp))){current?.let{Cover(it,Modifier.fillMaxSize())};Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().background(Color.Black.copy(alpha=.35f)).padding(18.dp)){Text(current?.title?:"Vinyl Music Player",color=Color.White,fontSize=20.sp,fontWeight=FontWeight.Bold);Text(current?.artist?:"本地音乐库",color=Color.White.copy(alpha=.85f),fontSize=15.sp)}};DrawerItem("▦","媒体库"){onRoute("library")};DrawerItem("▣","文件夹"){onRoute("folders")};DrawerItem("↻","重新扫描媒体库"){onRoute("rescan")};ThinDivider();DrawerItem("⚙","设置"){onRoute("settings")};DrawerItem("ⓘ","关于"){onRoute("about")}}
    }}
}
@Composable private fun DrawerItem(i:String,l:String,onClick:()->Unit){Row(Modifier.fillMaxWidth().height(62.dp).clickable(onClick=onClick).padding(horizontal=22.dp),verticalAlignment=Alignment.CenterVertically){Text(i,fontSize=25.sp,color=SoftGray,modifier=Modifier.width(54.dp));Text(l,fontSize=18.sp)}}
@Composable private fun NameDialog(t:String,v:String,a:String,onV:(String)->Unit,onDismiss:()->Unit,onConfirm:()->Unit){AlertDialog(onDismissRequest=onDismiss,title={Text(t)},text={TextField(v,onV,singleLine=true,placeholder={Text("播放列表名称")},modifier=Modifier.fillMaxWidth())},confirmButton={TextButton(onConfirm){Text(a,color=Pink)}},dismissButton={TextButton(onDismiss){Text("取消",color=Pink)}})}
@Composable private fun PlaylistPicker(names:List<String>,onDismiss:()->Unit,onNew:()->Unit,onPick:(String)->Unit){AlertDialog(onDismissRequest=onDismiss,title={Text("添加到播放列表")},text={Column(Modifier.fillMaxWidth().height(330.dp).verticalScroll(rememberScrollState())){Text("新建播放列表…",fontSize=19.sp,color=SoftGray,modifier=Modifier.fillMaxWidth().clickable(onClick=onNew).padding(vertical=14.dp));names.forEach{Text(it,fontSize=19.sp,modifier=Modifier.fillMaxWidth().clickable{onPick(it)}.padding(vertical=14.dp))}}},confirmButton={})}
@Composable private fun DetailsDialog(song:Song,onDismiss:()->Unit){AlertDialog(onDismissRequest=onDismiss,title={Text("详情")},text={Column(Modifier.height(470.dp).verticalScroll(rememberScrollState())){listOf("文件" to "/storage/emulated/0/Music/audio.wav","大小" to "258 KB","格式" to "WAV","比特率" to "705 kbps","采样率" to "44100 Hz","添加时间" to "2026-09-12","修改时间" to "2026-09-12","轨道" to song.no.toString(),"光盘" to "1","标题" to song.title,"艺术家" to song.artist,"专辑" to song.album,"专辑艺术家" to song.artist,"音乐类型" to song.genre,"年份" to song.year.toString(),"长度" to clock(song.duration),"ReplayGain" to "—","峰值" to "—").forEach{(k,v)->Row(Modifier.fillMaxWidth().padding(vertical=5.dp)){Text(k,color=SoftGray,modifier=Modifier.width(92.dp));Text(v,modifier=Modifier.weight(1f))}}}},confirmButton={TextButton(onDismiss){Text("确定",color=Pink)}})}
@Composable private fun ConfirmDialog(t:String,s:String,a:String,onConfirm:()->Unit,onDismiss:()->Unit){AlertDialog(onDismissRequest=onDismiss,title={Text(t)},text={Text(s,lineHeight=24.sp)},confirmButton={TextButton(onConfirm){Text(a,color=Pink)}},dismissButton={TextButton(onDismiss){Text("取消",color=Pink)}})}
@Composable private fun InfoDialog(t:String,s:String,onDismiss:()->Unit){AlertDialog(onDismissRequest=onDismiss,title={Text(t)},text={Text(s)},confirmButton={TextButton(onDismiss){Text("确定",color=Pink)}})}
@Composable private fun SleepDialog(c:Boolean,onC:(Boolean)->Unit,onSet:()->Unit,onDismiss:()->Unit){AlertDialog(onDismissRequest=onDismiss,title={Text("睡眠定时器")},text={Column(horizontalAlignment=Alignment.CenterHorizontally,modifier=Modifier.fillMaxWidth()){Box(Modifier.size(170.dp),contentAlignment=Alignment.Center){Canvas(Modifier.fillMaxSize()){drawCircle(Pink.copy(alpha=.18f),style=Stroke(12f));drawArc(Pink,-90f,210f,false,style=Stroke(12f,cap=StrokeCap.Round))};Column(horizontalAlignment=Alignment.CenterHorizontally){Text("30",fontSize=42.sp);Text("分钟",color=SoftGray)}};Row(verticalAlignment=Alignment.CenterVertically,modifier=Modifier.fillMaxWidth().clickable{onC(!c)}){Checkbox(c,onC);Text("播完当前音乐")}}},confirmButton={TextButton(onSet){Text("设置",color=Pink)}},dismissButton={TextButton(onDismiss){Text("取消",color=Pink)}})}
@Composable private fun CategoriesDialog(c:MutableMap<String,Boolean>,onDismiss:()->Unit,onReset:()->Unit,onConfirm:()->Unit){AlertDialog(onDismissRequest=onDismiss,title={Text("媒体库类别")},text={Column{listOf("歌曲","专辑","艺术家","音乐类型","播放列表").forEach{k->Row(Modifier.fillMaxWidth().height(52.dp),verticalAlignment=Alignment.CenterVertically){Text("☰",color=SoftGray,modifier=Modifier.width(36.dp));Checkbox(c[k]?:true,{c[k]=it});Text(k,fontSize=18.sp)}}}},confirmButton={TextButton(onConfirm){Text("确定",color=Pink)}},dismissButton={Row{TextButton(onReset){Text("重置",color=Pink)};TextButton(onDismiss){Text("取消",color=Pink)}}})}
@Composable private fun SimpleMenuDialog(t:String,items:List<String>,onDismiss:()->Unit){AlertDialog(onDismissRequest=onDismiss,title={Text(t)},text={Column{items.forEach{Text(it,fontSize=18.sp,modifier=Modifier.fillMaxWidth().padding(vertical=12.dp))}}},confirmButton={TextButton(onDismiss){Text("关闭",color=Pink)}})}
@Composable private fun PlaylistMenuDialog(onDismiss:()->Unit,onRename:()->Unit,onDelete:()->Unit){AlertDialog(onDismissRequest=onDismiss,title={Text("播放列表操作")},text={Column{listOf("作为下一首播放","加入播放队列","加入播放列表…","另存为…").forEach{Text(it,fontSize=18.sp,modifier=Modifier.fillMaxWidth().padding(vertical=10.dp))};Text("重命名",fontSize=18.sp,modifier=Modifier.fillMaxWidth().clickable(onClick=onRename).padding(vertical=10.dp));Text("删除",fontSize=18.sp,color=Color(0xFFE53900),modifier=Modifier.fillMaxWidth().clickable(onClick=onDelete).padding(vertical=10.dp))}},confirmButton={TextButton(onDismiss){Text("关闭",color=Pink)}})}
@Composable private fun ChangelogDialog(onDismiss:()->Unit){AlertDialog(onDismissRequest=onDismiss,title={Text("更新日志")},text={Column(Modifier.height(430.dp).verticalScroll(rememberScrollState())){Text("[1.11.0] · 2024-08-18",fontWeight=FontWeight.Bold,fontSize=18.sp);Text("新功能",color=Pink,fontWeight=FontWeight.Bold);Text("改进媒体访问、播放队列和专辑浏览。");Text("修复",color=Pink,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=16.dp));Text("修复封面显示与播放列表排序问题。");Text("其他变更",color=Pink,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=16.dp));Text("更新翻译并感谢新的社区贡献者。")}},confirmButton={TextButton(onDismiss){Text("确定",color=Pink)}})}
@Composable private fun ThinDivider(start:androidx.compose.ui.unit.Dp=0.dp){Box(Modifier.fillMaxWidth().padding(start=start).height(1.dp).background(Divider))}
