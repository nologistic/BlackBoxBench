package com.blackboxbench.reproduction

import android.os.Bundle
import android.graphics.Color as AndroidColor
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private val Navy = Color(0xFF303641)
private val Blue = Color(0xFF2F6FDF)
private val Teal = Color(0xFF009688)
private val Divider = Color(0xFFE0E0E0)
private val Muted = Color(0xFF6B7280)

private enum class Page { NOTES, EDITOR, SEARCH, TAGS, SETTINGS, SETTING_DETAIL, TRASH, DRAWING }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = AndroidColor.rgb(48, 54, 65)
        window.navigationBarColor = AndroidColor.WHITE
        setContent { ReproducedApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReproducedApp() {
    val context = LocalContext.current
    val notes = remember { mutableStateListOf<NoteItem>().apply { addAll(NoteStore.loadNotes(context)) } }
    val notebooks = remember { mutableStateListOf<String>().apply { addAll(NoteStore.loadNotebooks(context)) } }
    var page by remember { mutableStateOf(Page.NOTES) }
    var selectedNotebook by remember { mutableStateOf(NoteStore.loadSelected(context)) }
    var allNotes by remember { mutableStateOf(false) }
    var currentId by remember { mutableStateOf<String?>(null) }
    var detail by remember { mutableStateOf("常规") }
    var showAdd by remember { mutableStateOf(false) }
    var showSort by remember { mutableStateOf(false) }
    var showSync by remember { mutableStateOf(false) }
    var showNewNotebook by remember { mutableStateOf(false) }
    var sortTitle by remember { mutableStateOf(false) }
    var reverseSort by remember { mutableStateOf(true) }
    var showCompleted by remember { mutableStateOf(true) }
    var selectedTag by remember { mutableStateOf<String?>(null) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LaunchedEffect(notes.toList()) { NoteStore.saveNotes(context, notes) }
    LaunchedEffect(notebooks.toList()) { NoteStore.saveNotebooks(context, notebooks) }
    LaunchedEffect(selectedNotebook) { NoteStore.saveSelected(context, selectedNotebook) }

    fun openNew(todo: Boolean = false, attachment: String? = null, drawing: Boolean = false) {
        val notebook = if (allNotes || selectedNotebook.trim() !in notebooks.map { it.trim() }) "Project" else selectedNotebook.trim()
        val n = NoteItem(id=UUID.randomUUID().toString(), notebook=notebook, title="", body=attachment ?: "", isTodo=todo, updated=System.currentTimeMillis())
        notes.add(n); currentId=n.id; page=if (drawing) Page.DRAWING else Page.EDITOR
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = page == Page.NOTES || page == Page.TRASH,
        drawerContent = {
            ModalDrawerSheet(modifier=Modifier.width(328.dp), drawerContainerColor=Color.White) {
                Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
                Spacer(Modifier.height(8.dp))
                DrawerItem("▰", "全部笔记", allNotes && page==Page.NOTES) {
                    selectedTag=null; allNotes=true; page=Page.NOTES; scope.launch{drawerState.close()}
                }
                DividerLine()
                Text("笔记本", modifier=Modifier.padding(horizontal=66.dp, vertical=14.dp), fontSize=18.sp)
                notebooks.forEach { name ->
                    val child=name.startsWith("  ")
                    DrawerItem(if(child) "▢" else "▱", name.trim(), !allNotes && selectedNotebook==name.trim() && page==Page.NOTES, indent=if(child) 24.dp else 0.dp) {
                        selectedTag=null; selectedNotebook=name.trim(); allNotes=false; page=Page.NOTES; scope.launch{drawerState.close()}
                    }
                }
                DrawerItem("▣", "回收站", page==Page.TRASH) { page=Page.TRASH; scope.launch{drawerState.close()} }
                Spacer(Modifier.weight(1f))
                DividerLine()
                DrawerItem("▱", "新建笔记本", false) { showNewNotebook=true }
                DrawerItem("◆", "标签", page==Page.TAGS) { page=Page.TAGS; scope.launch{drawerState.close()} }
                DrawerItem("⚙", "设置", page==Page.SETTINGS || page==Page.SETTING_DETAIL) { page=Page.SETTINGS; scope.launch{drawerState.close()} }
                DividerLine()
                DrawerItem("⟳", "同步", false) { showSync=true; scope.launch{drawerState.close()} }
                Spacer(Modifier.height(18.dp))
            }
        }
    ) {
        when (page) {
            Page.NOTES -> NotesPage(
                title=selectedTag ?: if(allNotes) "全部笔记" else selectedNotebook,
                notes=filteredNotes(notes, allNotes, selectedNotebook, selectedTag, showCompleted, sortTitle, reverseSort),
                onMenu={scope.launch{drawerState.open()}},
                onSearch={page=Page.SEARCH},
                onSort={showSort=true},
                onOpen={currentId=it; page=Page.EDITOR},
                onToggle={id -> updateNote(notes,id){it.copy(completed=!it.completed,updated=System.currentTimeMillis())}},
                onAdd={showAdd=true},
                onLongPressCopy={ id ->
                    val n=notes.first{it.id==id}
                    notes.add(n.copy(id=UUID.randomUUID().toString(),title=uniqueCopyTitle(n.title,notes),updated=System.currentTimeMillis()))
                }
            )
            Page.TRASH -> TrashPage(
                notes=notes.filter{it.deleted},
                onMenu={scope.launch{drawerState.open()}},
                onOpen={currentId=it; page=Page.EDITOR},
                onSearch={page=Page.SEARCH},
                onSort={showSort=true}
            )
            Page.EDITOR -> {
                val note=currentId?.let{id->notes.firstOrNull{it.id==id}}
                if(note==null) page=Page.NOTES else EditorPage(
                    note=note,
                    notebooks=notebooks.map{it.trim()}.distinct(),
                    inTrash=note.deleted,
                    onBack={page=if(note.deleted) Page.TRASH else Page.NOTES},
                    onChange={changed->updateNote(notes,note.id){changed.copy(updated=System.currentTimeMillis())}},
                    onDelete={updateNote(notes,note.id){it.copy(deleted=true)};page=Page.NOTES},
                    onRestore={updateNote(notes,note.id){it.copy(deleted=false)};page=Page.TRASH},
                    onPermanent={notes.removeAll{it.id==note.id};page=Page.TRASH},
                    onDrawing={page=Page.DRAWING}
                )
            }
            Page.DRAWING -> DrawingPage(
                onClose={
                    currentId?.let{id->updateNote(notes,id){n->n.copy(body=(n.body+"\n![绘图](:/drawing-resource)").trim())}}
                    page=Page.EDITOR
                }
            )
            Page.SEARCH -> SearchPage(notes=notes.filter{!it.deleted},onBack={page=Page.NOTES},onOpen={currentId=it;page=Page.EDITOR})
            Page.TAGS -> TagsPage(notes=notes,onBack={page=Page.NOTES},onTag={selectedTag=it;allNotes=true;page=Page.NOTES})
            Page.SETTINGS -> SettingsPage(onBack={page=Page.NOTES},onOpen={detail=it;page=Page.SETTING_DETAIL})
            Page.SETTING_DETAIL -> SettingDetailPage(detail=detail,onBack={page=Page.SETTINGS})
        }
    }

    if(showAdd) {
        ModalBottomSheet(onDismissRequest={showAdd=false},containerColor=Color.White) {
            AddAction("▣","扫描笔记本"){showAdd=false;openNew(attachment="![扫描笔记](:/scan-resource)")}
            AddAction("▰","附件"){showAdd=false;openNew()}
            AddAction("●","录音"){showAdd=false;openNew(); }
            AddAction("▣","相机"){showAdd=false;openNew(attachment="![照片](:/photo-resource)")}
            AddAction("✎","绘图"){showAdd=false;openNew(drawing=true)}
            AddAction("☑","新建待办"){showAdd=false;openNew(todo=true)}
            AddAction("▤","新建笔记"){showAdd=false;openNew()}
            Spacer(Modifier.height(24.dp))
        }
    }
    if(showSort) SortDialog(
        sortTitle,reverseSort,showCompleted,
        onDismiss={showSort=false},
        onTitle={sortTitle=it},
        onReverse={reverseSort=it},
        onCompleted={showCompleted=it}
    )
    if(showSync) SyncDialog{showSync=false}
    if(showNewNotebook) NewNotebookDialog(
        notebooks=notebooks.map{it.trim()},
        onDismiss={showNewNotebook=false},
        onCreate={name,parent->
            val stored=if(parent.isBlank()) name else "  "+name
            notebooks.add(stored); selectedNotebook=name; allNotes=false; showNewNotebook=false; page=Page.NOTES
            scope.launch{drawerState.close()}
        }
    )
}

private fun filteredNotes(notes: List<NoteItem>, all: Boolean, notebook: String, tag: String?, showCompleted: Boolean, sortTitle: Boolean, reverse: Boolean): List<NoteItem> {
    var result=notes.filter{!it.deleted && (all || it.notebook==notebook) && (tag==null || tag in it.tags) && (showCompleted || !it.completed)}
    result=if(sortTitle) result.sortedBy{it.title.lowercase()} else result.sortedWith(compareByDescending<NoteItem>{it.pinned}.thenByDescending{it.updated})
    return if(reverse && sortTitle) result.reversed() else result
}

private fun uniqueCopyTitle(title:String,notes:List<NoteItem>):String {
    var n=1; var candidate=title+" (1)"
    while(notes.any{it.title==candidate}){n++;candidate=title+" ("+n+")"}
    return candidate
}

private fun updateNote(notes:SnapshotStateList<NoteItem>,id:String,block:(NoteItem)->NoteItem){
    val i=notes.indexOfFirst{it.id==id}; if(i>=0) notes[i]=block(notes[i])
}

@Composable
private fun DividerLine()=HorizontalDivider(color=Divider,thickness=1.dp)

@Composable
private fun DrawerItem(icon:String,label:String,selected:Boolean,indent:Dp=0.dp,onClick:()->Unit){
    Row(
        Modifier.fillMaxWidth().height(56.dp).background(if(selected) Color(0xFFE3E3E3) else Color.Transparent).clickable{onClick()}.padding(start=22.dp+indent,end=12.dp),
        verticalAlignment=Alignment.CenterVertically
    ){
        Text(icon,fontSize=25.sp,color=Navy,modifier=Modifier.width(44.dp))
        Text(label,fontSize=19.sp,color=Navy,maxLines=1,overflow=TextOverflow.Ellipsis)
    }
}

@Composable
private fun AppBar(title:String,onBack:(()->Unit)?=null,onMenu:(()->Unit)?=null,actions:@Composable RowScope.()->Unit={}){
    Column(Modifier.fillMaxWidth().background(Navy)){
        Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
        Row(Modifier.fillMaxWidth().height(64.dp).padding(horizontal=14.dp),verticalAlignment=Alignment.CenterVertically){
            Text(if(onBack!=null)"‹" else "☰",fontSize=38.sp,color=Color.White,modifier=Modifier.width(50.dp).clickable{(onBack?:onMenu)?.invoke()})
            Text(title,fontSize=22.sp,fontWeight=FontWeight.SemiBold,color=Color.White,modifier=Modifier.weight(1f),maxLines=1,overflow=TextOverflow.Ellipsis)
            actions()
        }
    }
}

@Composable
private fun NotesPage(title:String,notes:List<NoteItem>,onMenu:()->Unit,onSearch:()->Unit,onSort:()->Unit,onOpen:(String)->Unit,onToggle:(String)->Unit,onAdd:()->Unit,onLongPressCopy:(String)->Unit){
    Box(Modifier.fillMaxSize().background(Color.White)){
        Column(Modifier.fillMaxSize()){
            AppBar(title,onMenu=onMenu){
                Text("⌕",fontSize=40.sp,color=Color.White,modifier=Modifier.padding(horizontal=12.dp).clickable{onSearch()})
                Text("≡",fontSize=38.sp,color=Color.White,modifier=Modifier.padding(start=12.dp).clickable{onSort()})
            }
            if(notes.isEmpty()){
                Text("当前没有任何笔记。点击 (+) 按钮创建一个。",modifier=Modifier.align(Alignment.CenterHorizontally).padding(top=34.dp),fontSize=17.sp,color=Muted)
            } else LazyColumn(Modifier.fillMaxSize()){
                items(notes,key={it.id}){note->
                    Row(
                        Modifier.fillMaxWidth().heightIn(min=72.dp).clickable{onOpen(note.id)}.padding(horizontal=18.dp,vertical=16.dp),
                        verticalAlignment=Alignment.CenterVertically
                    ){
                        if(note.isTodo) Checkbox(checked=note.completed,onCheckedChange={onToggle(note.id)},colors=CheckboxDefaults.colors(checkedColor=Color.Gray))
                        Text(
                            if(note.title.isBlank())"无标题" else note.title,
                            fontSize=20.sp,
                            color=if(note.completed) Color(0xFF9DA1A7) else Navy,
                            modifier=Modifier.weight(1f),
                            maxLines=1,
                            overflow=TextOverflow.Ellipsis
                        )
                        Text("⧉",color=Color.Transparent,modifier=Modifier.clickable{onLongPressCopy(note.id)})
                    }
                    DividerLine()
                }
            }
        }
        FloatingActionButton(onClick=onAdd,containerColor=Blue,contentColor=Color.White,shape=RoundedCornerShape(18.dp),modifier=Modifier.align(Alignment.BottomEnd).padding(24.dp).size(72.dp)){
            Text("+",fontSize=42.sp,fontWeight=FontWeight.Light)
        }
    }
}

@Composable
private fun TrashPage(notes:List<NoteItem>,onMenu:()->Unit,onOpen:(String)->Unit,onSearch:()->Unit,onSort:()->Unit){
    Column(Modifier.fillMaxSize().background(Color.White)){
        AppBar("回收站",onMenu=onMenu){
            Text("⌕",fontSize=40.sp,color=Color.White,modifier=Modifier.padding(horizontal=12.dp).clickable{onSearch()})
            Text("≡",fontSize=38.sp,color=Color.White,modifier=Modifier.padding(start=12.dp).clickable{onSort()})
        }
        if(notes.isEmpty()) Text("回收站中没有笔记。",fontSize=20.sp,color=Navy,modifier=Modifier.align(Alignment.CenterHorizontally).padding(top=30.dp))
        else notes.forEach{n->
            Text(if(n.title.isBlank())"无标题" else n.title,fontSize=20.sp,modifier=Modifier.fillMaxWidth().clickable{onOpen(n.id)}.padding(18.dp))
            DividerLine()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorPage(note:NoteItem,notebooks:List<String>,inTrash:Boolean,onBack:()->Unit,onChange:(NoteItem)->Unit,onDelete:()->Unit,onRestore:()->Unit,onPermanent:()->Unit,onDrawing:()->Unit){
    var preview by remember(note.id){mutableStateOf(note.title.isNotBlank() || note.body.isNotBlank())}
    var menu by remember{mutableStateOf(false)}
    var books by remember{mutableStateOf(false)}
    var tags by remember{mutableStateOf(false)}
    var props by remember{mutableStateOf(false)}
    var attachment by remember{mutableStateOf(false)}
    var recorder by remember{mutableStateOf(false)}
    var recording by remember{mutableStateOf(false)}
    var reminder by remember{mutableStateOf(false)}
    Column(Modifier.fillMaxSize().background(Color.White)){
        AppBar(if(inTrash)"回收站" else note.notebook,onBack=onBack){
            if(!inTrash){
                Box{
                    Text("▼",fontSize=20.sp,color=Color.White,modifier=Modifier.padding(14.dp).clickable{books=true})
                    DropdownMenu(expanded=books,onDismissRequest={books=false}){
                        notebooks.forEach{b->DropdownMenuItem(text={Text(b)},onClick={onChange(note.copy(notebook=b));books=false})}
                    }
                }
                Text(if(preview) "✎" else "▣",fontSize=30.sp,color=Color.White,modifier=Modifier.padding(horizontal=12.dp).clickable{preview=!preview})
            }
            Box{
                Text("⋮",fontSize=35.sp,color=Color.White,modifier=Modifier.padding(horizontal=8.dp).clickable{menu=true})
                DropdownMenu(expanded=menu,onDismissRequest={menu=false}){
                    if(!inTrash){
                        DropdownMenuItem({Text("添加……")},{menu=false;attachment=true})
                        DropdownMenuItem({Text("添加绘图")},{menu=false;onDrawing()})
                        if(note.isTodo) DropdownMenuItem({Text("设置提醒")},{menu=false;reminder=true})
                        DropdownMenuItem({Text("分享")},{menu=false})
                        DropdownMenuItem({Text("使用语音输入……")},{menu=false})
                        DropdownMenuItem({Text("标签")},{menu=false;tags=true})
                        DropdownMenuItem({Text(if(note.isTodo)"转换为笔记" else "转换为待办")},{menu=false;onChange(note.copy(isTodo=!note.isTodo))})
                        DropdownMenuItem({Text("复制 Markdown 链接")},{menu=false})
                        DropdownMenuItem({Text("复制外部链接地址")},{menu=false})
                    }
                    DropdownMenuItem({Text("笔记属性")},{menu=false;props=true})
                    DropdownMenuItem({Text("Reveal in notebook")},{menu=false})
                    if(inTrash){
                        DropdownMenuItem({Text("恢复")},{menu=false;onRestore()})
                        DropdownMenuItem({Text("永久删除笔记")},{menu=false;onPermanent()})
                    } else DropdownMenuItem({Text("删除")},{menu=false;onDelete()})
                }
            }
        }
        if(preview){
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(18.dp)){
                if(note.title.isBlank()) Text("添加标题",fontSize=22.sp,fontWeight=FontWeight.Bold,color=Color(0xFF7B8797))
                else Text(note.title,fontSize=24.sp,fontWeight=FontWeight.Bold,color=Navy)
                Spacer(Modifier.height(18.dp))
                MarkdownPreview(note.body){lineIndex,checked->
                    val lines=note.body.lines().toMutableList()
                    if(lineIndex in lines.indices) lines[lineIndex]=lines[lineIndex].replace(if(checked)"[ ]" else "[x]",if(checked)"[x]" else "[ ]")
                    onChange(note.copy(body=lines.joinToString("\n")))
                }
            }
        } else {
            OutlinedTextField(
                value=note.title,
                onValueChange={onChange(note.copy(title=it))},
                placeholder={Text("添加标题",fontWeight=FontWeight.Bold)},
                singleLine=true,
                modifier=Modifier.fillMaxWidth(),
                colors=OutlinedTextFieldDefaults.colors(unfocusedBorderColor=Divider,focusedBorderColor=Divider)
            )
            OutlinedTextField(
                value=note.body,
                onValueChange={text->onChange(note.copy(body=text,title=if(note.title.isBlank()) text.lineSequence().firstOrNull()?.take(45)?:"" else note.title))},
                modifier=Modifier.weight(1f).fillMaxWidth(),
                keyboardOptions=KeyboardOptions(capitalization=KeyboardCapitalization.Sentences),
                colors=OutlinedTextFieldDefaults.colors(unfocusedBorderColor=Color.Transparent,focusedBorderColor=Color.Transparent)
            )
            Row(Modifier.fillMaxWidth().height(58.dp).background(Color(0xFFF4F5F7)),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceAround){
                listOf("⌕","H2","B","I","{…}","Σ","≣","☷").forEachIndexed{i,label->
                    Text(label,fontSize=if(i==0)30.sp else 23.sp,fontWeight=if(label=="B")FontWeight.Bold else FontWeight.Normal,modifier=Modifier.clickable{
                        when(label){
                            "⌕"->attachment=true
                            "H2"->onChange(note.copy(body=note.body+"\n## "))
                            "B"->onChange(note.copy(body=note.body+"****"))
                            "I"->onChange(note.copy(body=note.body+"**"))
                            "≣"->onChange(note.copy(body=note.body+"\n1. "))
                            "☷"->onChange(note.copy(body=note.body+"\n- "))
                        }
                    })
                }
            }
        }
        if(recorder){
            Row(Modifier.fillMaxWidth().background(Color(0xFFF2F2F2)).padding(18.dp),verticalAlignment=Alignment.CenterVertically){
                Text("●",fontSize=34.sp,color=Navy);Spacer(Modifier.width(18.dp))
                Column(Modifier.weight(1f)){Text(if(recording)"正在录音……" else "语音录音器",fontSize=19.sp);Text(if(recording)"0:24" else "单击“开始”以附加一条新的语音记忆到笔记中。",color=Muted)}
                TextButton(onClick={recorder=false}){Text("取消")}
                Button(onClick={
                    if(recording){onChange(note.copy(body=(note.body+"\n[recording-10/09/2026 15:33.m4a](:/audio-resource)").trim()));recorder=false}
                    else recording=true
                }){Text(if(recording)"完成" else "开始录制")}
            }
        }
    }
    if(attachment) AlertDialog(onDismissRequest={attachment=false},title={Text("选择一个选项")},text={
        Column{
            ListItem(headlineContent={Text("添加文件")},modifier=Modifier.clickable{onChange(note.copy(body=(note.body+"\n[sample_note.txt](:/sample-resource)").trim()));attachment=false})
            ListItem(headlineContent={Text("录制音频")},modifier=Modifier.clickable{attachment=false;recorder=true})
            ListItem(headlineContent={Text("拍照")},modifier=Modifier.clickable{onChange(note.copy(body=(note.body+"\n![照片](:/photo-resource)").trim()));attachment=false})
        }
    },confirmButton={})
    if(tags) TagDialog(note,onDismiss={tags=false},onApply={onChange(note.copy(tags=it));tags=false})
    if(props) AlertDialog(onDismissRequest={props=false},title={Text("笔记属性")},text={
        Column{Text("已创建: 10/09/2026 14:48");Spacer(Modifier.height(12.dp));Text("已更新: "+SimpleDateFormat("dd/MM/yyyy HH:mm",Locale.getDefault()).format(Date(note.updated)));Spacer(Modifier.height(20.dp));Text("在地图上查看",color=Blue);Spacer(Modifier.height(16.dp));Text("历史版本",color=Blue)}
    },confirmButton={TextButton(onClick={props=false}){Text("关闭")}})
    if(reminder) AlertDialog(onDismissRequest={reminder=false},title={Text("设置提醒")},text={Column{Text("设置日期");Spacer(Modifier.height(16.dp));Text("11/09/2026 15:19",color=Muted)}},confirmButton={TextButton(onClick={reminder=false}){Text("保存提醒")}},dismissButton={TextButton(onClick={reminder=false}){Text("取消")}})
}

@Composable
private fun MarkdownPreview(body:String,onToggleTask:(Int,Boolean)->Unit){
    var fence=false
    body.lines().forEachIndexed{index,line->
        when{
            line.startsWith("```")-> {fence=!fence;Spacer(Modifier.height(4.dp))}
            fence -> Text(line,fontFamily=FontFamily.Monospace,fontSize=15.sp,modifier=Modifier.fillMaxWidth().background(Color(0xFFF1F2F3)).padding(horizontal=10.dp,vertical=4.dp))
            line.startsWith("###### ")->Text(inline(line.drop(7)),fontSize=16.sp,fontWeight=FontWeight.Bold)
            line.startsWith("##### ")->Text(inline(line.drop(6)),fontSize=17.sp,fontWeight=FontWeight.Bold)
            line.startsWith("#### ")->Text(inline(line.drop(5)),fontSize=18.sp,fontWeight=FontWeight.Bold)
            line.startsWith("### ")->Text(inline(line.drop(4)),fontSize=20.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=10.dp))
            line.startsWith("## ")->Text(inline(line.drop(3)),fontSize=23.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=12.dp,bottom=4.dp))
            line.startsWith("# ")->Text(inline(line.drop(2)),fontSize=28.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(bottom=8.dp))
            line.startsWith("- [ ] ")||line.startsWith("- [x] ")->Row(verticalAlignment=Alignment.CenterVertically){val checked=line.startsWith("- [x]");Checkbox(checked=checked,onCheckedChange={onToggleTask(index,it)});Text(inline(line.drop(6)),fontSize=18.sp)}
            line.startsWith("> ")->Text(inline(line.drop(2)),fontSize=17.sp,fontStyle=FontStyle.Italic,modifier=Modifier.fillMaxWidth().background(Color(0xFFF0F2F5)).border(width=3.dp,color=Blue).padding(12.dp))
            line=="---"->HorizontalDivider(modifier=Modifier.padding(vertical=10.dp))
            line.startsWith("|")->Text(line,fontFamily=FontFamily.Monospace,fontSize=14.sp,modifier=Modifier.fillMaxWidth().border(1.dp,Divider).padding(5.dp))
            line.startsWith("![")->Box(Modifier.fillMaxWidth().height(170.dp).background(Color(0xFFF4F0F5)),contentAlignment=Alignment.Center){Canvas(Modifier.size(240.dp,110.dp)){drawLine(Color(0xFF8E2E8F),Offset(20f,15f),Offset(size.width-20f,size.height-15f),strokeWidth=12f)}}
            line.startsWith("- ")->Text("•  "+inline(line.drop(2)),fontSize=18.sp,modifier=Modifier.padding(vertical=3.dp))
            line.matches(Regex("\\d+\\. .*"))->Text(inline(line),fontSize=18.sp,modifier=Modifier.padding(vertical=3.dp))
            line.isBlank()->Spacer(Modifier.height(9.dp))
            else->Text(inline(line),fontSize=18.sp,lineHeight=27.sp)
        }
    }
}

private fun inline(source:String):AnnotatedString{
    val b=AnnotatedString.Builder();var i=0
    val pattern=Regex("(\\*\\*[^*]+\\*\\*|\\*[^*]+\\*|`[^`]+`|\\[[^]]+\\]\\([^)]*\\))")
    pattern.findAll(source).forEach{m->
        if(m.range.first>i)b.append(source.substring(i,m.range.first))
        val raw=m.value
        when{
            raw.startsWith("**")->b.withStyle(SpanStyle(fontWeight=FontWeight.Bold)){append(raw.removePrefix("**").removeSuffix("**"))}
            raw.startsWith("*")->b.withStyle(SpanStyle(fontStyle=FontStyle.Italic)){append(raw.removePrefix("*").removeSuffix("*"))}
            raw.startsWith("`")->b.withStyle(SpanStyle(fontFamily=FontFamily.Monospace,background=Color(0xFFECEFF1))){append(raw.trim('`'))}
            raw.startsWith("[")->b.withStyle(SpanStyle(color=Blue)){append(raw.substringAfter("[").substringBefore("]"))}
        }
        i=m.range.last+1
    }
    if(i<source.length)b.append(source.substring(i))
    return b.toAnnotatedString()
}

@Composable
private fun AddAction(icon:String,label:String,onClick:()->Unit){
    Row(Modifier.fillMaxWidth().clickable{onClick()}.padding(horizontal=28.dp,vertical=12.dp),verticalAlignment=Alignment.CenterVertically){
        Text(icon,fontSize=24.sp,color=Navy,modifier=Modifier.width(48.dp));Text(label,fontSize=18.sp)
    }
}

@Composable
private fun SearchPage(notes:List<NoteItem>,onBack:()->Unit,onOpen:(String)->Unit){
    var q by remember{mutableStateOf("")}
    Column(Modifier.fillMaxSize().background(Color.White)){
        AppBar("搜索",onBack=onBack)
        OutlinedTextField(value=q,onValueChange={q=it},singleLine=true,placeholder={Text("搜索笔记……")},trailingIcon={if(q.isNotEmpty())Text("×",fontSize=28.sp,modifier=Modifier.clickable{q=""})},modifier=Modifier.fillMaxWidth().padding(12.dp))
        val results=if(q.isBlank()) emptyList() else notes.filter{it.title.contains(q,true)||it.body.contains(q,true)}
        results.forEach{n->
            Column(Modifier.fillMaxWidth().clickable{onOpen(n.id)}.padding(18.dp)){Text(n.title.ifBlank{"无标题"},fontSize=20.sp,fontWeight=FontWeight.Medium);Text(n.body.lineSequence().firstOrNull()?:"",maxLines=1,color=Muted)}
            DividerLine()
        }
        if(q.isNotBlank()&&results.isEmpty()) Text("没有找到笔记。",modifier=Modifier.align(Alignment.CenterHorizontally).padding(28.dp),color=Muted)
    }
}

@Composable
private fun TagsPage(notes:List<NoteItem>,onBack:()->Unit,onTag:(String)->Unit){
    val tags=notes.filter{!it.deleted}.flatMap{it.tags}.distinct().sorted()
    Column(Modifier.fillMaxSize().background(Color.White)){
        AppBar("标签",onBack=onBack){Text("⌕",fontSize=38.sp,color=Color.White)}
        tags.forEach{tag->Row(Modifier.fillMaxWidth().clickable{onTag(tag)}.padding(20.dp),verticalAlignment=Alignment.CenterVertically){Text("◆",color=Navy,modifier=Modifier.width(42.dp));Text(tag,fontSize=20.sp)};DividerLine()}
        if(tags.isEmpty())Text("无标签",modifier=Modifier.padding(24.dp),color=Muted)
    }
}

@Composable
private fun TagDialog(note:NoteItem,onDismiss:()->Unit,onApply:(List<String>)->Unit){
    var current by remember{mutableStateOf(note.tags.toSet())}
    var text by remember{mutableStateOf("")}
    AlertDialog(onDismissRequest=onDismiss,title={Text("关联标签")},text={
        Column{
            if(current.isEmpty())Text("无标签",color=Muted) else Text(current.joinToString("   "))
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(value=text,onValueChange={text=it},label={Text("新建标签")},singleLine=true)
            if(text.isNotBlank())Text("＋ 新增 “"+text+"”",color=Blue,modifier=Modifier.fillMaxWidth().clickable{current=current+text.trim();text=""}.padding(vertical=14.dp))
        }
    },confirmButton={TextButton(onClick={onApply(current.toList())}){Text("应用")}},dismissButton={TextButton(onClick=onDismiss){Text("取消")}})
}

@Composable
private fun SortDialog(sortTitle:Boolean,reverse:Boolean,showCompleted:Boolean,onDismiss:()->Unit,onTitle:(Boolean)->Unit,onReverse:(Boolean)->Unit,onCompleted:(Boolean)->Unit){
    AlertDialog(onDismissRequest=onDismiss,title={Text("笔记排序")},text={
        Column{
            listOf("更新日期","创建日期","标题","自定义顺序","截止日期","完成日期").forEach{label->
                Row(Modifier.fillMaxWidth().clickable{onTitle(label=="标题")}.padding(vertical=5.dp),verticalAlignment=Alignment.CenterVertically){RadioButton(selected=if(label=="标题")sortTitle else label=="更新日期"&&!sortTitle,onClick={onTitle(label=="标题")});Text(label)}
            }
            Row(verticalAlignment=Alignment.CenterVertically){Checkbox(reverse,onCheckedChange=onReverse);Text("倒序")}
            Row(verticalAlignment=Alignment.CenterVertically){Checkbox(true,onCheckedChange={});Text("置顶未完成待办")}
            Row(verticalAlignment=Alignment.CenterVertically){Checkbox(showCompleted,onCheckedChange=onCompleted);Text("显示已完成待办")}
        }
    },confirmButton={TextButton(onClick=onDismiss){Text("完成")}})
}

@Composable
private fun SyncDialog(onDismiss:()->Unit){
    AlertDialog(onDismissRequest=onDismiss,title={Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("同步");Text("×",modifier=Modifier.clickable{onDismiss()})}},text={
        Column{
            Text("Joplin 可以使用多种途径来同步您的笔记。可以从下列选项中选择一种方式。")
            Spacer(Modifier.height(16.dp))
            Card(Modifier.fillMaxWidth().clickable{}){Column(Modifier.padding(18.dp)){Text("☁  Joplin 云",fontSize=20.sp,fontWeight=FontWeight.Bold);Text("Joplin 自己的同步服务。还可以发布笔记或者与他人协作笔记本。");Text("✓ 同步笔记\n✓ 将笔记发布到互联网\n✓ 与他人共同协作笔记本",modifier=Modifier.padding(top=10.dp))}}
            Spacer(Modifier.height(12.dp))
            Card(Modifier.fillMaxWidth().clickable{}){Column(Modifier.padding(18.dp)){Text("•••  Other",fontSize=20.sp,fontWeight=FontWeight.Bold);Text("Select one of the other supported sync targets.")}}
        }
    },confirmButton={})
}

@Composable
private fun NewNotebookDialog(notebooks:List<String>,onDismiss:()->Unit,onCreate:(String,String)->Unit){
    var name by remember{mutableStateOf("")};var parent by remember{mutableStateOf("")};var menu by remember{mutableStateOf(false)}
    AlertDialog(onDismissRequest=onDismiss,title={Text("新建笔记本")},text={
        Column{
            OutlinedTextField(value=name,onValueChange={name=it},label={Text("标题")},singleLine=true)
            Spacer(Modifier.height(12.dp))
            Box{OutlinedButton(onClick={menu=true},modifier=Modifier.fillMaxWidth()){Text(if(parent.isBlank())"父笔记本（无）" else parent)}
                DropdownMenu(expanded=menu,onDismissRequest={menu=false}){DropdownMenuItem({Text("无")},{parent="";menu=false});notebooks.forEach{b->DropdownMenuItem({Text(b)},{parent=b;menu=false})}}
            }
        }
    },confirmButton={TextButton(enabled=name.isNotBlank(),onClick={onCreate(name.trim(),parent)}){Text("创建")}},dismissButton={TextButton(onClick=onDismiss){Text("取消")}})
}

@Composable
private fun SettingsPage(onBack:()->Unit,onOpen:(String)->Unit){
    val sections=listOf(
        Triple("☷","常规","语言与日期格式设置"),
        Triple("▰","外观","Themes"),
        Triple("⟳","同步","同步、加密、代理"),
        Triple("✎","编辑器","Typography, spellcheck, layout"),
        Triple("♣","插件","启用或禁用插件"),
        Triple("M↓","Markdown","媒体播放器、数学公式、流程图、目录"),
        Triple("▰","笔记","Geolocation, image resize"),
        Triple("◴","笔记历史","设置是否保留笔记历史版本"),
        Triple("▣","工具","日志、配置档案、同步状态"),
        Triple("⇥","导入和导出","导入或导出数据"),
        Triple("●","更多信息","捐助与官网信息")
    )
    Column(Modifier.fillMaxSize().background(Color.White)){
        AppBar("设置",onBack=onBack){Text("⌕",fontSize=38.sp,color=Color.White)}
        LazyColumn{items(sections){s->
            Row(Modifier.fillMaxWidth().clickable{onOpen(s.second)}.padding(horizontal=24.dp,vertical=16.dp),verticalAlignment=Alignment.CenterVertically){Text(s.first,fontSize=24.sp,color=Muted,modifier=Modifier.width(50.dp));Column{Text(s.second,fontSize=20.sp,color=Navy);Text(s.third,fontSize=16.sp,color=Muted)}}
        }}
    }
}

@Composable
private fun SettingDetailPage(detail:String,onBack:()->Unit){
    Column(Modifier.fillMaxSize().background(Color.White)){
        AppBar(detail,onBack=onBack){Text("⌕",fontSize=38.sp,color=Color.White)}
        when(detail){
            "常规"->GeneralSettings()
            "外观"->AppearanceSettings()
            "同步"->SyncSettings()
            "编辑器"->EditorSettings()
            "插件"->PluginSettings()
            "Markdown"->MarkdownSettings()
            "笔记"->NotesSettings()
            "笔记历史"->HistorySettings()
            "工具"->ToolsSettings()
            "导入和导出"->ImportExportSettings()
            else->AboutSettings()
        }
    }
}

@Composable
private fun SettingsScroller(content:@Composable ColumnScope.()->Unit)=Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),content=content)
@Composable private fun SettingRow(label:String,value:String?=null,toggle:Boolean?=null){
    var checked by remember{mutableStateOf(toggle?:false)}
    Row(Modifier.fillMaxWidth().padding(horizontal=24.dp,vertical=16.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(label,fontSize=18.sp);if(value!=null)Text(value,color=Muted,fontSize=15.sp)};if(toggle!=null)Switch(checked,onCheckedChange={checked=it},colors=SwitchDefaults.colors(checkedThumbColor=Teal,checkedTrackColor=Color(0xFF9CD2CC)))};DividerLine()
}
@Composable private fun GeneralSettings()=SettingsScroller{SettingRow("语言","中文（简体）（96%）");SettingRow("日期格式","30/01/2017");SettingRow("时间格式","20:30");SettingRow("使用生物识别技术解锁 Joplin",toggle=false);SettingRow("几天后自动删除回收站笔记","90",toggle=true)}
@Composable private fun AppearanceSettings()=SettingsScroller{SettingRow("自动与系统主题匹配",toggle=true);SettingRow("首选亮色主题","亮色");SettingRow("首选暗色主题","暗色");SettingRow("笔记查看器字体大小","16")}
@Composable private fun SyncSettings()=SettingsScroller{SettingRow("同步目标","None");SettingRow("同步间隔","5 分钟");SettingRow("仅在 Wi-Fi 连接时同步",toggle=false);BigButton("打开同步向导");BigButton("加密配置");SettingRow("高级设置","⌄")}
@Composable private fun EditorSettings()=SettingsScroller{SettingRow("使用纯文本编辑器",toggle=false);SettingRow("启用拼写检查",toggle=true);SettingRow("显示 Markdown 工具栏",toggle=true);SettingRow("编辑器字体大小","15");SettingRow("编辑器字体","默认");SettingRow("默认笔记视图","View mode");SettingRow("渲染标记",toggle=true);SettingRow("渲染图片",toggle=true);SettingRow("突出显示活动行",toggle=false)}
@Composable private fun PluginSettings()=SettingsScroller{OutlinedTextField(value="",onValueChange={},placeholder={Text("搜索插件……")},modifier=Modifier.fillMaxWidth().padding(20.dp));Text("没有任何已安装的插件。",fontSize=18.sp,modifier=Modifier.padding(24.dp));SettingRow("高级设置","⌄")}
@Composable private fun MarkdownSettings()=SettingsScroller{listOf("启用软换行" to false,"启用 Typographer 支持" to false,"启用自动链接（Linkify）" to true,"启用数学表达式" to true,"启用 Fountain 语法支持" to false,"启用 Mermaid 流程图支持" to true,"Enable ABC musical notation support" to true,"启用音频播放器" to true,"启用视频播放器" to true,"启用 ==mark== 语法" to true,"启用脚注" to true,"启用目录扩展" to true,"启用 ~sub~ 语法" to false,"启用 ^sup^ 语法" to false).forEach{SettingRow(it.first,toggle=it.second)}}
@Composable private fun NotesSettings()=SettingsScroller{SettingRow("保存地理位置",toggle=false);SettingRow("缩放大图片","每次询问");SettingRow("语音输入语言","系统默认");SettingRow("扫描笔记标题模板","Scan: {date} ({count})");SettingRow("为笔记链接显示 Joplin 图标",toggle=true)}
@Composable private fun HistorySettings()=SettingsScroller{SettingRow("启用笔记历史",toggle=true);SettingRow("保留笔记历史（天）","90")}
@Composable private fun ToolsSettings()=SettingsScroller{BigButton("管理配置档案");BigButton("笔记附件");BigButton("同步状态");BigButton("日志");BigButton("删除记录");BigButton("修复搜索索引");Text("如果搜索功能遇到问题，可以使用这个重建索引。所需时间取决于笔记数量。",color=Muted,modifier=Modifier.padding(20.dp))}
@Composable private fun ImportExportSettings()=SettingsScroller{BigButton("导出所有笔记（JEX）");BigButton("导入 JEX");BigButton("IMPORT FROM TXT");BigButton("导出调试报告");BigButton("导出配置档案")}
@Composable private fun AboutSettings()=SettingsScroller{Text("Joplin",fontSize=34.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(24.dp));SettingRow("开源 Markdown 笔记与待办应用","本地复现版 1.0");SettingRow("帮助与官网","离线演示")}
@Composable private fun BigButton(text:String){Button(onClick={},modifier=Modifier.fillMaxWidth().padding(horizontal=20.dp,vertical=10.dp),shape=RoundedCornerShape(2.dp),colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF2196F3))){Text(text,fontSize=18.sp)}}

@Composable
private fun DrawingPage(onClose:()->Unit){
    var stroke by remember{mutableStateOf(false)}
    Column(Modifier.fillMaxSize().background(Color.White)){
        Row(Modifier.fillMaxWidth().height(64.dp).background(Color(0xFFE8E8E8)).padding(horizontal=18.dp),verticalAlignment=Alignment.CenterVertically){
            Text("关闭",fontSize=20.sp,modifier=Modifier.clickable{onClose()});Spacer(Modifier.weight(1f));Text("↪",fontSize=36.sp,color=Color.Gray);Spacer(Modifier.width(44.dp));Text("↩",fontSize=36.sp);Spacer(Modifier.weight(1f));Text("保存",fontSize=20.sp,color=Color.Gray)
        }
        Row(Modifier.fillMaxWidth().height(64.dp).background(Color(0xFFF2F3F5)),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceAround){listOf("✎","✎","▰","⌫","▢","T","✋","▤","▣").forEach{Text(it,fontSize=27.sp,color=Muted)}}
        Canvas(Modifier.fillMaxSize().pointerInput(Unit){detectDragGestures(onDragStart={stroke=true}){change,_->change.consume()}}){
            if(stroke)drawLine(Color(0xFF8E2E8F),Offset(size.width*.23f,size.height*.13f),Offset(size.width*.72f,size.height*.36f),strokeWidth=18f)
        }
    }
}
