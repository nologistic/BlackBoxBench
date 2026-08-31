package com.blackboxbench.commercedemo

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.media.MediaPlayer
import android.os.Bundle
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

data class Product(val id: Int, val name: String, val category: String,
                   val price: Int, val subtitle: String)

private val products = listOf(
    Product(101, "旅行相机", "数码", 3299, "轻巧机身 · 4K 视频"),
    Product(102, "机械键盘", "数码", 699, "静音轴 · 暖白背光"),
    Product(103, "陶瓷马克杯", "生活", 89, "手工釉面 · 350ml"),
    Product(104, "阅读台灯", "生活", 259, "无频闪 · 三档色温"),
    Product(105, "城市双肩包", "出行", 399, "防泼水 · 16 寸电脑仓"),
    Product(106, "保温水瓶", "出行", 159, "全天保温 · 轻量设计")
)

private class DemoStore(context: Context) :
    SQLiteOpenHelper(context.applicationContext, "nimbus_demo.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE app_state (key TEXT PRIMARY KEY, value TEXT NOT NULL)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    private fun read(key: String, fallback: String): String =
        readableDatabase.query(
            "app_state", arrayOf("value"), "key = ?", arrayOf(key),
            null, null, null
        ).use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else fallback
        }

    private fun write(key: String, value: String) {
        val row = ContentValues().apply {
            put("key", key)
            put("value", value)
        }
        writableDatabase.insertWithOnConflict(
            "app_state", null, row, SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun boolean(key: String, fallback: Boolean = false) =
        read(key, if (fallback) "1" else "0") == "1"
    fun integer(key: String, fallback: Int = 0) =
        read(key, fallback.toString()).toIntOrNull() ?: fallback
    fun values(key: String): Set<String> =
        read(key, "").split(',').filter { it.isNotBlank() }.toSet()
    fun putBoolean(key: String, value: Boolean) = write(key, if (value) "1" else "0")
    fun putInteger(key: String, value: Int) = write(key, value.toString())
    fun putValues(key: String, value: Set<String>) = write(key, value.sorted().joinToString(","))
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme(colorScheme = lightColorScheme()) { NimbusApp() } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NimbusApp() {
    val context = LocalContext.current
    val store = remember { DemoStore(context) }
    DisposableEffect(store) { onDispose { store.close() } }
    var loggedIn by remember { mutableStateOf(store.boolean("logged_in")) }
    var cart by remember { mutableStateOf(store.values("cart")) }
    var favorites by remember { mutableStateOf(store.values("favorites")) }
    var orders by remember { mutableIntStateOf(store.integer("orders")) }
    fun saveSet(key: String, value: Set<String>) { store.putValues(key, value) }

    if (!loggedIn) {
        LoginScreen { loggedIn = true; store.putBoolean("logged_in", true) }
        return
    }
    var tab by remember { mutableStateOf("首页") }
    var detail by remember { mutableStateOf<Product?>(null) }
    if (detail != null) {
        ProductDetail(detail!!, detail!!.id.toString() in favorites,
            onBack = { detail = null },
            onFavorite = {
                val id = detail!!.id.toString()
                favorites = if (id in favorites) favorites - id else favorites + id
                saveSet("favorites", favorites)
            },
            onCart = {
                cart = cart + detail!!.id.toString(); saveSet("cart", cart)
            })
        return
    }
    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("Nimbus Market") }) },
        bottomBar = {
            NavigationBar {
                listOf("首页", "收藏", "购物车", "订单", "我的").forEach { item ->
                    NavigationBarItem(selected = tab == item, onClick = { tab = item },
                        icon = { Text(if (tab == item) "●" else "○") }, label = { Text(item) })
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (tab) {
                "首页" -> Catalog(products, onOpen = { detail = it })
                "收藏" -> ProductList(products.filter { it.id.toString() in favorites },
                    "还没有收藏商品", onOpen = { detail = it })
                "购物车" -> CartScreen(products.filter { it.id.toString() in cart },
                    onRemove = { p -> cart = cart - p.id.toString(); saveSet("cart", cart) },
                    onCheckout = {
                        if (cart.isNotEmpty()) {
                            orders += 1; cart = emptySet()
                            store.putInteger("orders", orders); store.putValues("cart", cart)
                            tab = "订单"
                        }
                    })
                "订单" -> OrdersScreen(orders)
                else -> ProfileScreen(favorites.size, orders,
                    onMedia = { tab = "媒体" },
                    onLogout = {
                        store.putBoolean("logged_in", false); loggedIn = false
                    })
            }
            if (tab == "媒体") MediaScreen(onBack = { tab = "我的" })
        }
    }
}

@Composable
fun LoginScreen(onLogin: () -> Unit) {
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.Center) {
        Text("欢迎来到 Nimbus", style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp)); Text("登录测试账户以继续")
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(user, { user = it }, label = { Text("用户名") },
            modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(pass, { pass = it }, label = { Text("密码") },
            visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
        if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(20.dp))
        Button({ if (user == "linxi" && pass == "demo123") onLogin()
                 else error = "用户名或密码不正确" }, Modifier.fillMaxWidth()) { Text("登录") }
        TextButton({ user = "linxi"; pass = "demo123" }, Modifier.align(Alignment.CenterHorizontally)) {
            Text("填入测试账号")
        }
    }
}

@Composable
fun Catalog(all: List<Product>, onOpen: (Product) -> Unit) {
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("全部") }
    val shown = all.filter { (category == "全部" || it.category == category) &&
            (query.isBlank() || it.name.contains(query, true)) }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        OutlinedTextField(query, { query = it }, label = { Text("搜索商品") },
            modifier = Modifier.fillMaxWidth())
        Row(Modifier.padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("全部", "数码", "生活", "出行").forEach {
                FilterChip(selected = category == it, onClick = { category = it }, label = { Text(it) })
            }
        }
        ProductList(shown, "没有找到商品", onOpen, Modifier.weight(1f))
    }
}

@Composable
fun ProductList(list: List<Product>, empty: String, onOpen: (Product) -> Unit,
                modifier: Modifier = Modifier) {
    if (list.isEmpty()) Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(empty) }
    else LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(list) { product ->
            ElevatedCard(Modifier.fillMaxWidth().clickable { onOpen(product) }) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(R.drawable.product_camera), null,
                        Modifier.size(72.dp), contentScale = ContentScale.Crop)
                    Column(Modifier.padding(start = 14.dp).weight(1f)) {
                        Text(product.name, fontWeight = FontWeight.Bold); Text(product.subtitle)
                        Text("¥${product.price}", color = MaterialTheme.colorScheme.primary)
                    }; Text("›")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetail(product: Product, favorite: Boolean, onBack: () -> Unit,
                  onFavorite: () -> Unit, onCart: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text(product.name) },
        navigationIcon = { TextButton(onBack) { Text("返回") } }) }) { padding ->
        Column(Modifier.padding(padding).padding(20.dp)) {
            Image(painterResource(R.drawable.product_camera), null,
                Modifier.fillMaxWidth().height(260.dp), contentScale = ContentScale.Crop)
            Spacer(Modifier.height(20.dp)); Text(product.name, style = MaterialTheme.typography.headlineSmall)
            Text(product.subtitle); Text("¥${product.price}", style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(24.dp))
            OutlinedButton(onFavorite, Modifier.fillMaxWidth()) { Text(if (favorite) "取消收藏" else "收藏") }
            Button(onCart, Modifier.fillMaxWidth()) { Text("加入购物车") }
        }
    }
}

@Composable
fun CartScreen(items: List<Product>, onRemove: (Product) -> Unit, onCheckout: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("购物车", style = MaterialTheme.typography.headlineSmall)
        if (items.isEmpty()) Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text("购物车是空的") }
        else LazyColumn(Modifier.weight(1f)) { items(items) { p ->
            ListItem(headlineContent = { Text(p.name) }, supportingContent = { Text("¥${p.price}") },
                trailingContent = { TextButton({ onRemove(p) }) { Text("移除") } })
        } }
        Text("合计 ¥${items.sumOf { it.price }}", fontWeight = FontWeight.Bold)
        Button(onCheckout, enabled = items.isNotEmpty(), modifier = Modifier.fillMaxWidth()) { Text("模拟结算") }
    }
}

@Composable fun OrdersScreen(count: Int) {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text("我的订单", style = MaterialTheme.typography.headlineSmall)
        if (count == 0) Text("暂无订单") else repeat(count) { index ->
            ElevatedCard(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Column(Modifier.padding(16.dp)) { Text("订单 NM-${1001 + index}", fontWeight = FontWeight.Bold)
                    Text("已支付 · 准备发货") }
            }
        }
    }
}

@Composable fun ProfileScreen(favorites: Int, orders: Int, onMedia: () -> Unit, onLogout: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("林溪", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("linxi@example.test · 测试用户")
        Spacer(Modifier.height(24.dp)); Text("收藏 $favorites    订单 $orders")
        Spacer(Modifier.height(20.dp)); OutlinedButton(onMedia, Modifier.fillMaxWidth()) { Text("媒体展示") }
        TextButton(onLogout) { Text("退出登录") }
    }
}

@Composable fun MediaScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val player = remember { MediaPlayer.create(context, R.raw.notification) }
    DisposableEffect(Unit) { onDispose { player.release() } }
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.padding(20.dp)) {
            TextButton(onBack) { Text("返回") }; Text("媒体展示", style = MaterialTheme.typography.headlineSmall)
            Button({ if (player.isPlaying) player.pause() else player.start() }) { Text("播放/暂停提示音") }
            Spacer(Modifier.height(16.dp))
            AndroidView(factory = { ctx -> VideoView(ctx).apply {
                setVideoPath("android.resource://${ctx.packageName}/${R.raw.product_demo}")
                setOnPreparedListener { it.isLooping = true; start() }
            } }, modifier = Modifier.fillMaxWidth().height(260.dp))
            Text("离线商品演示视频")
        }
    }
}
