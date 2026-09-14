package com.blackboxbench.reproduction

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

private val Bg = Color(0xFFFAFAFF)
private val Ink = Color(0xFF08152E)
private val Soft = Color(0xFFE8ECFC)
private val Soft2 = Color(0xFFD9E2FA)
private val Accent = Color(0xFF7794CF)
private val DarkAccent = Color(0xFF5F687F)
private val ExpenseRed = Color(0xFFD75B5B)
private val IncomeGreen = Color(0xFF55AE45)
private val Muted = Color(0xFF858A98)

data class Account(val id: String, val name: String, val currency: String, val balance: Double, val primary: Boolean)
enum class TxKind { EXPENSE, INCOME, TRANSFER, CORRECTION }
data class Tx(
    val id: String,
    val title: String,
    val amount: Double,
    val kind: TxKind,
    val account: String,
    val destination: String = "",
    val category: String = "",
    val included: Boolean = true
)
data class Budget(val id: String, val name: String, val limit: Double, val spent: Double, val addedOnly: Boolean = false)
data class AppData(
    val onboarded: Boolean = false,
    val accounts: List<Account> = listOf(Account("bank", "Bank", "USD", 0.0, true)),
    val transactions: List<Tx> = emptyList(),
    val budgets: List<Budget> = emptyList(),
    val selectedAccount: String = "bank"
)

private fun money(value: Double, currency: String = "USD"): String {
    val symbol = when (currency) { "EUR" -> "€"; "JPY" -> "¥"; "GBP" -> "£"; "CNY" -> "¥"; else -> "$" }
    val nf = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = if (value % 1.0 == 0.0) 0 else 2
        maximumFractionDigits = 2
    }
    return (if (value < 0) "-" else "") + symbol + nf.format(abs(value))
}

private object Store {
    private const val KEY = "cashew_state"
    fun load(context: Context): AppData {
        val raw = context.getSharedPreferences("cashew", Context.MODE_PRIVATE).getString(KEY, null) ?: return AppData()
        return runCatching {
            val o = JSONObject(raw)
            val accounts = buildList {
                val a = o.getJSONArray("accounts")
                for (i in 0 until a.length()) {
                    val x = a.getJSONObject(i)
                    add(Account(x.getString("id"), x.getString("name"), x.getString("currency"), x.getDouble("balance"), x.getBoolean("primary")))
                }
            }
            val txs = buildList {
                val a = o.getJSONArray("transactions")
                for (i in 0 until a.length()) {
                    val x = a.getJSONObject(i)
                    add(Tx(x.getString("id"), x.getString("title"), x.getDouble("amount"), TxKind.valueOf(x.getString("kind")), x.getString("account"), x.optString("destination"), x.optString("category"), x.optBoolean("included", true)))
                }
            }
            val budgets = buildList {
                val a = o.getJSONArray("budgets")
                for (i in 0 until a.length()) {
                    val x = a.getJSONObject(i)
                    add(Budget(x.getString("id"), x.getString("name"), x.getDouble("limit"), x.getDouble("spent"), x.optBoolean("addedOnly")))
                }
            }
            AppData(o.getBoolean("onboarded"), accounts, txs, budgets, o.optString("selectedAccount", accounts.firstOrNull()?.id ?: "bank"))
        }.getOrElse { AppData() }
    }
    fun save(context: Context, data: AppData) {
        val o = JSONObject()
        o.put("onboarded", data.onboarded)
        o.put("selectedAccount", data.selectedAccount)
        o.put("accounts", JSONArray().apply { data.accounts.forEach { a -> put(JSONObject().put("id", a.id).put("name", a.name).put("currency", a.currency).put("balance", a.balance).put("primary", a.primary)) } })
        o.put("transactions", JSONArray().apply { data.transactions.forEach { t -> put(JSONObject().put("id", t.id).put("title", t.title).put("amount", t.amount).put("kind", t.kind.name).put("account", t.account).put("destination", t.destination).put("category", t.category).put("included", t.included)) } })
        o.put("budgets", JSONArray().apply { data.budgets.forEach { b -> put(JSONObject().put("id", b.id).put("name", b.name).put("limit", b.limit).put("spent", b.spent).put("addedOnly", b.addedOnly)) } })
        context.getSharedPreferences("cashew", Context.MODE_PRIVATE).edit().putString(KEY, o.toString()).apply()
    }
}

class MainActivity : ComponentActivity() {
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ReproducedApp {
                if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }
}

@Composable
fun ReproducedApp(requestNotifications: () -> Unit = {}) {
    val context = LocalContext.current
    var data by remember { mutableStateOf(Store.load(context)) }
    var page by rememberSaveable { mutableIntStateOf(0) }
    var root by rememberSaveable { mutableStateOf("home") }
    var screen by rememberSaveable { mutableStateOf("") }
    var selectedTx by remember { mutableStateOf<Tx?>(null) }

    fun update(next: AppData) {
        data = next
        Store.save(context, next)
    }
    LaunchedEffect(data.onboarded) { if (data.onboarded) requestNotifications() }

    BenchmarkAppTheme {
        Surface(color = Bg, modifier = Modifier.fillMaxSize()) {
            if (!data.onboarded) {
                Onboarding(page, { page = it }, { update(demoData()) }) { amount, currency ->
                    update(AppData(true, listOf(Account("bank", "Bank", currency, 0.0, true)), emptyList(), listOf(Budget("budget", "Budget", amount, 0.0)), "bank"))
                }
            } else if (screen.isNotEmpty()) {
                BackHandler { screen = ""; selectedTx = null }
                when (screen) {
                    "add" -> AddTransactionScreen(data, { screen = "" }) { tx, from, to -> update(addTransaction(data, tx, from, to)); screen = "" }
                    "editTx" -> selectedTx?.let { tx ->
                        EditTransactionScreen(tx, { screen = ""; selectedTx = null }, { title, included ->
                            update(data.copy(transactions = data.transactions.map { if (it.id == tx.id) it.copy(title = title, included = included) else it }))
                            screen = ""; selectedTx = null
                        }, { update(removeTransaction(data, tx)); screen = ""; selectedTx = null })
                    }
                    "addBudget" -> AddBudgetScreen({ screen = "" }) { name, limit, added ->
                        update(data.copy(budgets = data.budgets + Budget("b" + System.currentTimeMillis(), name, limit, 0.0, added))); screen = ""
                    }
                    "budgetDetail" -> BudgetDetailScreen(data.budgets.firstOrNull()) { screen = "" }
                    "accounts" -> AccountsScreen(data, { screen = "" }, { screen = "addAccount" }) { id ->
                        update(data.copy(accounts = data.accounts.map { it.copy(primary = it.id == id) }, selectedAccount = id))
                    }
                    "addAccount" -> AddAccountScreen({ screen = "accounts" }) { name, amount, currency ->
                        val id = "a" + System.currentTimeMillis()
                        val account = Account(id, name, currency, amount, false)
                        val correction = Tx("t" + System.currentTimeMillis(), "Balance Correction", amount, TxKind.CORRECTION, id, category = "Balance Correction")
                        update(data.copy(accounts = data.accounts + account, transactions = data.transactions + correction)); screen = "accounts"
                    }
                    "categories" -> CategoryScreen { screen = "" }
                    "titles" -> TitlesScreen(data) { screen = "" }
                    "settings" -> SettingsScreen { screen = "" }
                    "summary" -> SummaryScreen(data) { screen = "" }
                    "scheduled" -> EmptyFinanceScreen("Scheduled", "$0", "Averaged monthly upcoming", "No transactions found.", onBack = { screen = "" }, onAdd = { screen = "add" })
                    "subscriptions" -> EmptyFinanceScreen("Subscriptions", "$0", "Monthly subscriptions", "No subscription transactions.", onBack = { screen = "" }, onAdd = { screen = "add" })
                    "goals" -> GoalsScreen { screen = "" }
                    "loans" -> EmptyFinanceScreen("Loans", "$0", "0 transactions", "No transactions found.", listOf("All", "Lent", "Borrowed"), { screen = "" }, { screen = "add" })
                    "editHome" -> EditHomeScreen { screen = "" }
                }
            } else {
                MainShell(root, { root = it }, { screen = "add" }) {
                    when (root) {
                        "home" -> HomeScreen(data, { id -> update(data.copy(selectedAccount = id)) }) { screen = "editHome" }
                        "transactions" -> TransactionsScreen(data) { selectedTx = it; screen = "editTx" }
                        "budgets" -> BudgetsScreen(data, { screen = "addBudget" }) { screen = "budgetDetail" }
                        else -> MoreScreen { screen = it }
                    }
                }
            }
        }
    }
}

private fun demoData(): AppData {
    val accounts = listOf(
        Account("cash", "Cash Wallet", "CNY", 860.0, false),
        Account("salary", "Salary Card", "CNY", 15240.5, true),
        Account("travel", "Travel Card", "USD", 320.0, false),
        Account("saving", "Euro Savings", "EUR", 1500.0, false)
    )
    val txs = listOf(
        Tx("d1", "August salary", 18500.0, TxKind.INCOME, "salary", category = "Income"),
        Tx("d2", "Monthly rent", 4200.0, TxKind.EXPENSE, "salary", category = "Bills & Fees"),
        Tx("d3", "Weekly groceries", 186.5, TxKind.EXPENSE, "salary", category = "Groceries"),
        Tx("d4", "Metro pass", 210.0, TxKind.EXPENSE, "salary", category = "Transit")
    )
    return AppData(true, accounts, txs, listOf(Budget("budget", "Budget", 6000.0, 596.5), Budget("food", "Food", 800.0, 186.5)), "salary")
}

private fun addTransaction(data: AppData, tx: Tx, from: String, to: String): AppData {
    var accounts = data.accounts
    var budgets = data.budgets
    when (tx.kind) {
        TxKind.EXPENSE -> {
            accounts = accounts.map { if (it.id == from) it.copy(balance = it.balance - tx.amount) else it }
            if (tx.included) budgets = budgets.map { it.copy(spent = it.spent + tx.amount) }
        }
        TxKind.INCOME -> accounts = accounts.map { if (it.id == from) it.copy(balance = it.balance + tx.amount) else it }
        TxKind.TRANSFER -> accounts = accounts.map { when (it.id) { from -> it.copy(balance = it.balance - tx.amount); to -> it.copy(balance = it.balance + tx.amount); else -> it } }
        TxKind.CORRECTION -> Unit
    }
    return data.copy(accounts = accounts, budgets = budgets, transactions = data.transactions + tx.copy(account = from, destination = to))
}

private fun removeTransaction(data: AppData, tx: Tx): AppData {
    var accounts = data.accounts
    var budgets = data.budgets
    when (tx.kind) {
        TxKind.EXPENSE -> {
            accounts = accounts.map { if (it.id == tx.account) it.copy(balance = it.balance + tx.amount) else it }
            if (tx.included) budgets = budgets.map { it.copy(spent = max(0.0, it.spent - tx.amount)) }
        }
        TxKind.INCOME -> accounts = accounts.map { if (it.id == tx.account) it.copy(balance = it.balance - tx.amount) else it }
        TxKind.TRANSFER -> accounts = accounts.map { when (it.id) { tx.account -> it.copy(balance = it.balance + tx.amount); tx.destination -> it.copy(balance = it.balance - tx.amount); else -> it } }
        TxKind.CORRECTION -> Unit
    }
    return data.copy(accounts = accounts, budgets = budgets, transactions = data.transactions.filterNot { it.id == tx.id })
}

@Composable
private fun Onboarding(page: Int, onPage: (Int) -> Unit, onDemo: () -> Unit, onFinish: (Double, String) -> Unit) {
    var amountText by rememberSaveable { mutableStateOf("") }
    var currency by rememberSaveable { mutableStateOf("CNY") }
    when (page) {
        0 -> Column(Modifier.fillMaxSize().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(150.dp))
            Text("Track your spending habits with Cashew!", color = Ink, fontSize = 36.sp, fontWeight = FontWeight.Black, lineHeight = 41.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(54.dp))
            Illustration("◔", "▦", "↗")
            Spacer(Modifier.weight(1f))
            Text("See where your money goes and build better habits.", color = Muted, fontSize = 17.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(26.dp)); Dots(0); Spacer(Modifier.height(22.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedButton(onClick = onDemo, modifier = Modifier.weight(1f).height(58.dp), shape = RoundedCornerShape(22.dp)) { Text("Preview Demo") }
                Button(onClick = { onPage(1) }, modifier = Modifier.weight(1f).height(58.dp), shape = RoundedCornerShape(22.dp), colors = ButtonDefaults.buttonColors(containerColor = Accent)) { Text("Next") }
            }
        }
        1 -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(120.dp)); Text("Create a budget", color = Ink, fontSize = 42.sp, fontWeight = FontWeight.Black); Spacer(Modifier.height(45.dp))
            Card(colors = CardDefaults.cardColors(containerColor = Soft), shape = RoundedCornerShape(32.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Your monthly budget", color = Muted, fontSize = 17.sp)
                    OutlinedTextField(
                        value = amountText, onValueChange = { amountText = it.filter(Char::isDigit).take(8) }, modifier = Modifier.fillMaxWidth(),
                        textStyle = LocalTextStyle.current.copy(fontSize = 36.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                        prefix = { Text(if (currency == "USD") "$" else if (currency == "EUR") "€" else "¥", fontSize = 32.sp) },
                        placeholder = { Text("0", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, shape = RoundedCornerShape(20.dp)
                    )
                    Spacer(Modifier.height(22.dp)); Segmented(listOf("Daily", "Weekly", "Monthly", "Yearly"), "Monthly") {}; Spacer(Modifier.height(20.dp))
                    Text("Beginning September 1", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(22.dp)); Text("Change Currency", fontSize = 20.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { listOf("USD", "EUR", "JPY", "CNY").forEach { code -> ChoiceChip(code, currency == code) { currency = code } } }
            Spacer(Modifier.height(28.dp)); Dots(1); Spacer(Modifier.weight(1f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedButton(onClick = { onPage(0) }, modifier = Modifier.weight(1f).height(58.dp), shape = RoundedCornerShape(22.dp)) { Text("Back") }
                Button(onClick = { if ((amountText.toDoubleOrNull() ?: 0.0) > 0) onPage(2) }, enabled = (amountText.toDoubleOrNull() ?: 0.0) > 0, modifier = Modifier.weight(1f).height(58.dp), shape = RoundedCornerShape(22.dp), colors = ButtonDefaults.buttonColors(containerColor = Accent)) { Text("Next") }
            }
        }
        else -> Column(Modifier.fillMaxSize().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(165.dp)); Text("Welcome to Cashew!", color = Ink, fontSize = 42.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
            Spacer(Modifier.height(55.dp)); Illustration("♢", "✓", "☆"); Spacer(Modifier.weight(1f))
            Button(onClick = {}, modifier = Modifier.fillMaxWidth().height(62.dp), shape = RoundedCornerShape(22.dp), colors = ButtonDefaults.buttonColors(containerColor = Soft, contentColor = Ink)) { Text("G   Sign In with Google", fontSize = 17.sp) }
            Spacer(Modifier.height(14.dp))
            Button(onClick = { onFinish(amountText.toDoubleOrNull() ?: 1000.0, currency) }, modifier = Modifier.fillMaxWidth().height(62.dp), shape = RoundedCornerShape(22.dp), colors = ButtonDefaults.buttonColors(containerColor = Accent)) { Text("Continue Without Sign In", fontSize = 17.sp) }
            Spacer(Modifier.height(14.dp)); TextButton(onClick = { onPage(1) }) { Text("Back") }; Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable private fun Illustration(a: String, b: String, c: String) {
    Box(Modifier.size(275.dp).clip(RoundedCornerShape(90.dp)).background(Soft), contentAlignment = Alignment.Center) { Text("$a   $b   $c", fontSize = 46.sp, color = Accent, fontWeight = FontWeight.Bold) }
}
@Composable private fun Dots(selected: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { repeat(3) { Box(Modifier.size(if (it == selected) 12.dp else 8.dp).clip(CircleShape).background(if (it == selected) Ink else Soft2)) } }
}

@Composable
private fun MainShell(root: String, onRoot: (String) -> Unit, onAdd: () -> Unit, content: @Composable () -> Unit) {
    Scaffold(
        containerColor = Bg,
        bottomBar = { BottomNav(root, onRoot) },
        floatingActionButton = { FloatingActionButton(onClick = onAdd, containerColor = DarkAccent, contentColor = Color.White, shape = RoundedCornerShape(22.dp), modifier = Modifier.size(64.dp)) { Text("+", fontSize = 38.sp, fontWeight = FontWeight.Light) } }
    ) { pad -> Box(Modifier.padding(pad).fillMaxSize()) { content() } }
}

@Composable private fun BottomNav(root: String, onRoot: (String) -> Unit) {
    NavigationBar(containerColor = Soft, tonalElevation = 0.dp, modifier = Modifier.height(96.dp)) {
        listOf("home" to "⌂\nHome", "transactions" to "▣\nTransactions", "budgets" to "◔\nBudgets", "more" to "•••\nMore").forEach { (id, label) ->
            NavigationBarItem(
                selected = root == id, onClick = { onRoot(id) },
                icon = { Text(label.substringBefore("\n"), fontSize = 24.sp, fontWeight = FontWeight.Bold) },
                label = { Text(label.substringAfter("\n"), fontSize = 14.sp, fontWeight = if (root == id) FontWeight.Bold else FontWeight.Normal) },
                colors = NavigationBarItemDefaults.colors(indicatorColor = Color(0xFFB9C8EB), selectedIconColor = Ink, selectedTextColor = Ink, unselectedIconColor = Ink, unselectedTextColor = Ink)
            )
        }
    }
}

@Composable private fun PageTop(title: String, back: (() -> Unit)? = null, action: String? = null, onAction: (() -> Unit)? = null) {
    Column {
        Spacer(Modifier.height(38.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            if (back != null) Text("‹", modifier = Modifier.clickable { back() }.padding(8.dp), color = Muted, fontSize = 48.sp) else Spacer(Modifier.width(48.dp))
            Spacer(Modifier.weight(1f))
            if (action != null) Text(action, modifier = Modifier.clickable { onAction?.invoke() }.padding(10.dp), color = Ink, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(92.dp))
        Text(title, color = Ink, fontSize = 39.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 20.dp))
        Spacer(Modifier.height(22.dp))
    }
}

@Composable private fun HomeScreen(data: AppData, onSelect: (String) -> Unit, onEditHome: () -> Unit) {
    val selected = data.accounts.firstOrNull { it.id == data.selectedAccount } ?: data.accounts.first()
    val budget = data.budgets.firstOrNull()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 90.dp)) {
        item { PageTop("Home", action = "⋮", onAction = onEditHome) }
        item {
            LazyRow(contentPadding = PaddingValues(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(data.accounts) { account ->
                    val selectedCard = account.id == selected.id
                    Card(
                        modifier = Modifier.width(150.dp).height(112.dp).then(if (selectedCard) Modifier.border(2.dp, Accent, RoundedCornerShape(22.dp)) else Modifier).clickable { onSelect(account.id) },
                        colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(22.dp)
                    ) {
                        Column(Modifier.padding(18.dp)) {
                            Row { Text(account.name, fontSize = 21.sp, fontWeight = FontWeight.Black, color = Ink, maxLines = 1); Spacer(Modifier.weight(1f)); Box(Modifier.size(20.dp).clip(CircleShape).background(Accent)) }
                            Text("${money(account.balance, account.currency)} ${account.currency}", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                            val count = data.transactions.count { it.account == account.id || it.destination == account.id }
                            Text("$count transaction${if (count == 1) "" else "s"}", color = Muted, fontSize = 14.sp)
                        }
                    }
                }
                item {
                    Card(Modifier.width(150.dp).height(112.dp), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(22.dp), border = BorderStroke(1.5.dp, Color.LightGray)) {
                        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Text("☷⁺", fontSize = 29.sp, color = Color.LightGray); Text("Account", color = Muted) }
                    }
                }
            }
        }
        if (budget != null) item { Spacer(Modifier.height(14.dp)); BudgetCard(budget) }
        item { Spacer(Modifier.height(14.dp)); TrendCard(selected.balance) }
    }
}

@Composable private fun BudgetCard(budget: Budget) {
    val remaining = budget.limit - budget.spent
    val progress = if (budget.limit == 0.0) 0f else (budget.spent / budget.limit).toFloat().coerceIn(0f, 1f)
    Card(Modifier.padding(horizontal = 14.dp).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(26.dp)) {
        Column {
            Box(Modifier.fillMaxWidth().background(Soft2).padding(22.dp)) {
                Column {
                    Text(budget.name, fontSize = 26.sp, fontWeight = FontWeight.Black, color = Ink)
                    Row(verticalAlignment = Alignment.Bottom) { Text(money(remaining), fontSize = 25.sp, fontWeight = FontWeight.Black); Text(" left of ${money(budget.limit)}", fontSize = 16.sp) }
                }
                Text("◷", modifier = Modifier.align(Alignment.TopEnd), color = Ink, fontSize = 26.sp)
            }
            Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Sep 1"); LinearProgressIndicator(progress = { progress }, modifier = Modifier.padding(horizontal = 12.dp).weight(1f).height(18.dp).clip(CircleShape), color = Accent, trackColor = Color(0xFFE1E2E7)); Text("Sep 30")
                }
                Text("${(progress * 100).toInt()}%", fontSize = 17.sp, fontWeight = FontWeight.Black)
                Text("You can spend ${money(max(0.0, remaining) / 20)}/day for 20 more days", color = Muted, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable private fun TrendCard(balance: Double) {
    Card(Modifier.padding(horizontal = 14.dp).fillMaxWidth().height(250.dp), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(26.dp)) {
        Column(Modifier.padding(20.dp)) {
            Canvas(Modifier.fillMaxWidth().weight(1f)) {
                val p = Path().apply { moveTo(size.width * .08f, size.height * .88f); lineTo(size.width * .93f, size.height * .15f) }
                drawPath(p, Accent, style = Stroke(5f, cap = StrokeCap.Round))
                repeat(4) { i -> drawLine(Soft2, Offset(size.width * (.1f + i * .25f), 0f), Offset(size.width * (.1f + i * .25f), size.height), strokeWidth = 2f) }
                repeat(3) { i -> drawLine(Soft2, Offset(0f, size.height * (.3f + i * .25f)), Offset(size.width, size.height * (.3f + i * .25f)), strokeWidth = 2f) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("$0", color = Muted); Text(money(abs(balance)), color = Muted) }
            Spacer(Modifier.height(10.dp)); Segmented(listOf("All", "Expense", "Income"), "All") {}
        }
    }
}

@Composable private fun Segmented(values: List<String>, selected: String, onChange: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Soft)) {
        values.forEach { v ->
            Box(Modifier.weight(1f).clip(RoundedCornerShape(20.dp)).background(if (selected == v) Color(0xFFAFBFE5) else Color.Transparent).clickable { onChange(v) }.padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
                Text(v, fontSize = 15.sp, color = if (selected == v) Ink else Muted, fontWeight = if (selected == v) FontWeight.Bold else FontWeight.Normal, textAlign = TextAlign.Center)
            }
        }
    }
}
@Composable private fun ChoiceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(Modifier.clip(RoundedCornerShape(16.dp)).background(if (selected) Color(0xFFB6C5E7) else Soft).border(if (selected) 1.5.dp else 0.dp, if (selected) Accent else Color.Transparent, RoundedCornerShape(16.dp)).clickable { onClick() }.padding(horizontal = 16.dp, vertical = 11.dp)) {
        Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable private fun TransactionsScreen(data: AppData, onEdit: (Tx) -> Unit) {
    val expenses = data.transactions.filter { it.kind == TxKind.EXPENSE && it.included }.sumOf { it.amount }
    val incomes = data.transactions.filter { it.kind == TxKind.INCOME && it.included }.sumOf { it.amount }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 90.dp)) {
        item { PageTop("Transactions", action = "⌕") }
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(38.dp)) {
                listOf("July", "August", "September", "October", "November").forEach { Text(it, modifier = Modifier.padding(8.dp), fontSize = 18.sp, fontWeight = if (it == "September") FontWeight.Black else FontWeight.Normal, color = if (it == "September") Ink else Muted) }
            }
            Row(Modifier.padding(horizontal = 14.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Soft2).padding(12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                Text("▼ ${money(expenses)}", color = ExpenseRed, fontSize = 18.sp); Text("▲ ${money(incomes)}", color = IncomeGreen, fontSize = 18.sp); Text("= ${money(incomes - expenses)}", fontSize = 18.sp)
            }
            Row(Modifier.padding(18.dp).fillMaxWidth()) { Text("Today, September 11", color = Muted); Spacer(Modifier.weight(1f)); Text(money(incomes - expenses), color = Muted) }
        }
        items(data.transactions.asReversed()) { tx ->
            if (tx.kind == TxKind.TRANSFER) {
                TransactionRow("Bank Transfer In", tx.amount, TxKind.INCOME, tx.included) { onEdit(tx) }
                TransactionRow("Cash Transfer Out", tx.amount, TxKind.EXPENSE, tx.included) { onEdit(tx) }
            } else TransactionRow(tx.title, tx.amount, tx.kind, tx.included) { onEdit(tx) }
        }
        item {
            val flow = data.transactions.filter { it.kind != TxKind.TRANSFER }.sumOf { if (it.kind == TxKind.EXPENSE) -it.amount else it.amount }
            Text("Total cash flow: ${money(flow)}\n${data.transactions.fold(0) { total, item -> total + if (item.kind == TxKind.TRANSFER) 2 else 1 }} transactions", modifier = Modifier.fillMaxWidth().padding(28.dp), textAlign = TextAlign.Center, color = Muted, fontSize = 16.sp)
        }
    }
}

@Composable private fun TransactionRow(title: String, amount: Double, kind: TxKind, included: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 20.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(52.dp).clip(CircleShape).background(when(kind) { TxKind.INCOME -> Color(0xFFE1D2F6); TxKind.EXPENSE -> Color(0xFFD5EDCE); else -> Color(0xFFD4DEE2) }), contentAlignment = Alignment.Center) {
            Text(when(kind) { TxKind.INCOME -> "♛"; TxKind.EXPENSE -> "🛍"; else -> "⌁" }, fontSize = 25.sp)
        }
        Spacer(Modifier.width(14.dp)); Text(title, modifier = Modifier.weight(1f), fontSize = 19.sp, color = if (included) Ink else Muted)
        val color = if (kind == TxKind.EXPENSE) ExpenseRed else IncomeGreen
        val prefix = if (kind == TxKind.EXPENSE) "▼" else "▲"
        Text("$prefix ${money(amount)}", color = if (included) color else Muted, fontSize = 19.sp, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun AddTransactionScreen(data: AppData, onBack: () -> Unit, onSave: (Tx, String, String) -> Unit) {
    var kind by remember { mutableStateOf(TxKind.EXPENSE) }
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var account by remember { mutableStateOf(data.selectedAccount) }
    var destination by remember { mutableStateOf(data.accounts.firstOrNull { it.id != account }?.id ?: account) }
    var showTitle by remember { mutableStateOf(true) }
    var showCategory by remember { mutableStateOf(false) }
    val valid = (amount.toDoubleOrNull() ?: 0.0) > 0 && (kind == TxKind.TRANSFER || category.isNotBlank())
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        PageTop("Add Transaction", back = onBack)
        Segmented(listOf("Expense", "Income", "Transfer"), when(kind){TxKind.EXPENSE->"Expense";TxKind.INCOME->"Income";else->"Transfer"}) {
            kind = when(it) { "Income" -> TxKind.INCOME; "Transfer" -> TxKind.TRANSFER; else -> TxKind.EXPENSE }
            category = if (kind == TxKind.INCOME) "Income" else ""
        }
        Box(Modifier.fillMaxWidth().background(if(kind == TxKind.INCOME) Color(0xFFE8DCF3) else Soft2).padding(26.dp)) {
            Text(if (kind == TxKind.TRANSFER) "⇄" else if (kind == TxKind.INCOME) "♛" else "🛍", fontSize = 58.sp)
            Text(money(amount.toDoubleOrNull() ?: 0.0), modifier = Modifier.align(Alignment.CenterEnd), fontSize = 36.sp, fontWeight = FontWeight.Black)
        }
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Today                                      18 : 02", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            if (kind == TxKind.TRANSFER) {
                Text("Transfer between accounts", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { data.accounts.forEach { ChoiceChip(it.name, account == it.id) { account = it.id; if(destination == it.id) destination = data.accounts.firstOrNull { a -> a.id != it.id }?.id ?: it.id } } }
                Text("To", color = Muted)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { data.accounts.filter { it.id != account }.forEach { ChoiceChip(it.name, destination == it.id) { destination = it.id } } }
            } else {
                OutlinedButton(onClick = { showCategory = true }, modifier = Modifier.fillMaxWidth().height(58.dp), shape = RoundedCornerShape(18.dp)) { Text(if(category.isBlank()) "Select Category" else category) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { data.accounts.forEach { ChoiceChip(it.name, account == it.id) { account = it.id } } }
            }
            OutlinedTextField(amount, { amount = it.filter { ch -> ch.isDigit() || ch == '.' }.take(10) }, label = { Text("Amount") }, leadingIcon = { Text("$") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(), textStyle = LocalTextStyle.current.copy(fontSize = 27.sp, fontWeight = FontWeight.Bold), shape = RoundedCornerShape(18.dp))
            OutlinedTextField(title, { title = it }, label = { Text(if(kind == TxKind.TRANSFER) "Transfer Balance" else "Title") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp))
            OutlinedTextField("", {}, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth().height(110.dp), shape = RoundedCornerShape(18.dp))
            Text("📎  Add attachment                                      +", modifier = Modifier.fillMaxWidth().padding(10.dp), fontSize = 17.sp)
            Button(onClick = {
                val finalTitle = title.ifBlank { if(kind == TxKind.TRANSFER) "Transfer Balance" else category }
                onSave(Tx("t" + System.currentTimeMillis(), finalTitle, amount.toDoubleOrNull() ?: 0.0, kind, account, destination, category), account, destination)
            }, enabled = valid, modifier = Modifier.fillMaxWidth().height(60.dp), shape = RoundedCornerShape(22.dp), colors = ButtonDefaults.buttonColors(containerColor = Accent)) {
                Text(if(kind == TxKind.TRANSFER) "Transfer Amount" else "Add Transaction", fontSize = 18.sp)
            }
            Spacer(Modifier.height(26.dp))
        }
    }
    if (showTitle) {
        ModalBottomSheet(onDismissRequest = { showTitle = false }, containerColor = Bg, shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)) {
            Column(Modifier.padding(22.dp).padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Enter Title", fontSize = 34.sp, fontWeight = FontWeight.Black, color = Ink)
                OutlinedTextField(title, { title = it }, placeholder = { Text("Title") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(18.dp))
                Button(onClick = { showTitle = false; showCategory = true }, modifier = Modifier.fillMaxWidth().height(58.dp), shape = RoundedCornerShape(20.dp), colors = ButtonDefaults.buttonColors(containerColor = Accent)) { Text("Select Category") }
            }
        }
    }
    if (showCategory) CategoryPicker(kind, { showCategory = false }) { category = it; if (kind == TxKind.EXPENSE) title = title.ifBlank { it }; showCategory = false }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun CategoryPicker(kind: TxKind, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    val expenses = listOf("Dining" to "🍴", "Groceries" to "🛍", "Shopping" to "🛍", "Transit" to "🚊", "Entertainment" to "🍿", "Bills & Fees" to "💵", "Gifts" to "🎁", "Beauty" to "✿", "Work" to "▣", "Travel" to "✈")
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Bg) {
        Column(Modifier.padding(18.dp).padding(bottom = 28.dp)) {
            Text("Select Category", fontSize = 30.sp, fontWeight = FontWeight.Black, color = Ink); Spacer(Modifier.height(14.dp))
            if (kind == TxKind.INCOME) CategoryTile("Income", "♛", Color(0xFFDCCAF3)) { onPick("Income") }
            else expenses.chunked(4).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    row.forEach { (name, icon) -> CategoryTile(name, icon, Color(0xFFD7E9D2)) { onPick(name) } }
                    repeat(4 - row.size) { Spacer(Modifier.width(82.dp)) }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}
@Composable private fun CategoryTile(name: String, icon: String, color: Color, onClick: () -> Unit) {
    Column(Modifier.width(82.dp).clickable { onClick() }, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(68.dp).clip(RoundedCornerShape(20.dp)).background(color), contentAlignment = Alignment.Center) { Text(icon, fontSize = 31.sp) }
        Text(name, fontSize = 12.sp, textAlign = TextAlign.Center, maxLines = 1)
    }
}

@Composable private fun EditTransactionScreen(tx: Tx, onBack: () -> Unit, onSave: (String, Boolean) -> Unit, onDelete: () -> Unit) {
    var title by remember { mutableStateOf(tx.title) }
    var included by remember { mutableStateOf(tx.included) }
    var confirm by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        PageTop("Edit Transaction", back = onBack, action = "▣", onAction = { confirm = true })
        Segmented(listOf("Expense", "Income"), if(tx.kind == TxKind.INCOME) "Income" else "Expense") {}
        Box(Modifier.fillMaxWidth().background(Soft2).padding(30.dp)) { Text(if(tx.kind == TxKind.INCOME) "♛" else "🛍", fontSize = 56.sp); Text(money(tx.amount), modifier = Modifier.align(Alignment.CenterEnd), fontSize = 36.sp, fontWeight = FontWeight.Black) }
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Today                                      18 : 02", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Segmented(listOf("Default", "Upcoming", "Subscription"), "Default") {}
            OutlinedTextField(title, { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp))
            OutlinedTextField("", {}, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth().height(110.dp), shape = RoundedCornerShape(18.dp))
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Soft).padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Include Amount", modifier = Modifier.weight(1f), fontSize = 18.sp, fontWeight = FontWeight.Bold); Switch(included, { included = it })
            }
            Button(onClick = { onSave(title, included) }, modifier = Modifier.fillMaxWidth().height(60.dp), shape = RoundedCornerShape(22.dp), colors = ButtonDefaults.buttonColors(containerColor = Accent)) { Text("Save Changes") }
        }
    }
    if (confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { Text("Delete transaction?") }, text = { Text("This change will update the account balance and budget.") }, confirmButton = { TextButton(onClick = onDelete) { Text("Delete", color = ExpenseRed) } }, dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancel") } })
}

@Composable private fun BudgetsScreen(data: AppData, onAdd: () -> Unit, onDetail: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 90.dp)) {
        item { PageTop("Budgets", action = "✎") }
        items(data.budgets) { budget -> Box(Modifier.clickable { onDetail() }) { BudgetCard(budget) }; Spacer(Modifier.height(14.dp)) }
        item { OutlinedCard(onClick = onAdd, modifier = Modifier.padding(14.dp).fillMaxWidth().height(160.dp), shape = RoundedCornerShape(25.dp), border = BorderStroke(1.5.dp, Color.LightGray)) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("+", color = Color.LightGray, fontSize = 42.sp) } } }
    }
}

@Composable private fun BudgetDetailScreen(budget: Budget?, onBack: () -> Unit) {
    val b = budget ?: Budget("none", "Budget", 0.0, 0.0)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        PageTop(b.name, back = onBack, action = "✎")
        Text("${money(b.limit - b.spent)} remaining", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 32.sp, fontWeight = FontWeight.Black)
        Text("of ${money(b.limit)}", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = Muted); Spacer(Modifier.height(30.dp))
        Box(Modifier.padding(30.dp).fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(185.dp)) { drawArc(Soft2, -90f, 360f, false, style = Stroke(28f)); drawArc(Accent, -90f, ((b.spent / max(1.0,b.limit))*360).toFloat(), false, style = Stroke(28f, cap = StrokeCap.Round)) }
            Text("${((b.spent/max(1.0,b.limit))*100).toInt()}%", fontSize = 27.sp, fontWeight = FontWeight.Black)
        }
        Card(Modifier.padding(18.dp).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Soft), shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.padding(22.dp)) { Text("Groceries", fontSize = 21.sp, fontWeight = FontWeight.Bold); Text("${money(b.spent)}  •  ${if(b.spent > 0) "1 transaction" else "0 transactions"}", color = Muted); Spacer(Modifier.height(16.dp)); LinearProgressIndicator(progress = { (b.spent/max(1.0,b.limit)).toFloat().coerceIn(0f,1f) }, modifier = Modifier.fillMaxWidth().height(16.dp).clip(CircleShape), color = Accent) }
        }
        TrendCard(b.spent)
    }
}

@Composable private fun AddBudgetScreen(onBack: () -> Unit, onSave: (String, Double, Boolean) -> Unit) {
    var name by remember { mutableStateOf("") }; var amount by remember { mutableStateOf("") }; var addedOnly by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        PageTop("Add Budget", back = onBack); Segmented(listOf("Expense budget", "Savings budget"), "Expense budget") {}
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp))
            OutlinedTextField(amount, { amount = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("Amount") }, prefix = { Text("$") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp))
            Text("1 month  •  Beginning September 1", fontWeight = FontWeight.Bold, fontSize = 18.sp); Text("Budget Type", fontSize = 20.sp, fontWeight = FontWeight.Black)
            Segmented(listOf("All transactions", "Added only"), if(addedOnly) "Added only" else "All transactions") { addedOnly = it == "Added only" }
            Card(colors = CardDefaults.cardColors(containerColor = Soft), shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp)) { Text("Set Category Spending Goals", fontWeight = FontWeight.Bold); Text("Select accounts and categories to include", color = Muted) } }
            Text("Colors", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) { listOf(Accent, IncomeGreen, Color(0xFF2FA99E), Color(0xFF2DB9C5), Color(0xFF3E9DDE)).forEach { Box(Modifier.size(52.dp).clip(CircleShape).background(it)) } }
            Button(onClick = { onSave(name, amount.toDoubleOrNull() ?: 0.0, addedOnly) }, enabled = name.isNotBlank() && (amount.toDoubleOrNull() ?: 0.0) > 0, modifier = Modifier.fillMaxWidth().height(60.dp), shape = RoundedCornerShape(22.dp), colors = ButtonDefaults.buttonColors(containerColor = Accent)) { Text(if(name.isBlank()) "Set Name" else "Add Budget") }
        }
    }
}

@Composable private fun MoreScreen(onOpen: (String) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 90.dp)) {
        item { PageTop("More Actions", action = "?") }
        item {
            Card(Modifier.padding(horizontal = 10.dp).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Soft2), shape = RoundedCornerShape(24.dp)) {
                Row(Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("Cashew  Pro", color = Ink, fontSize = 25.sp, fontWeight = FontWeight.Black); Text("Budget like a pro with Cashew Pro", fontSize = 16.sp) }; Text("›", fontSize = 42.sp) }
            }
            Spacer(Modifier.height(12.dp))
            ActionWide("⚙", "Settings & Customization", "Theme, Language, Import/Export CSV") { onOpen("settings") }
            ActionWide("▤", "All Spending Summary", "Your spending statistics all in one place") { onOpen("summary") }
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) { ActionHalf("ⓘ", "About Cashew", Modifier.weight(1f)) {}; ActionHalf("✎", "Feedback", Modifier.weight(1f)) {} }
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) { ActionHalf("♟", "Notifications", Modifier.weight(1f)) {}; ActionHalf("G", "Login", Modifier.weight(1f)) {} }
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) { ActionHalf("◴", "Subscriptions", Modifier.weight(1f)) { onOpen("subscriptions") }; ActionHalf("▣", "Scheduled", Modifier.weight(1f)) { onOpen("scheduled") } }
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) { ActionHalf("♟", "Goals", Modifier.weight(1f)) { onOpen("goals") }; ActionHalf("▣", "Loans", Modifier.weight(1f)) { onOpen("loans") } }
            Row(Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniAction("▣", "Accounts", Modifier.weight(1f)) { onOpen("accounts") }
                MiniAction("◔", "Budgets", Modifier.weight(1f)) { onOpen("addBudget") }
                MiniAction("♣", "Categories", Modifier.weight(1f)) { onOpen("categories") }
                MiniAction("Tᵀ", "Titles", Modifier.weight(1f)) { onOpen("titles") }
            }
        }
    }
}
@Composable private fun ActionWide(icon:String,title:String,subtitle:String,onClick:()->Unit){ OutlinedCard(onClick=onClick,modifier=Modifier.padding(8.dp).fillMaxWidth(),shape=RoundedCornerShape(18.dp)){Row(Modifier.padding(18.dp),verticalAlignment=Alignment.CenterVertically){Text(icon,fontSize=27.sp,color=DarkAccent);Spacer(Modifier.width(16.dp));Column{Text(title,fontSize=20.sp,fontWeight=FontWeight.Medium);if(subtitle.isNotEmpty())Text(subtitle,fontSize=14.sp,color=Muted)}}}}
@Composable private fun ActionHalf(icon:String,title:String,modifier:Modifier,onClick:()->Unit){OutlinedCard(onClick=onClick,modifier=modifier.padding(4.dp).height(72.dp),shape=RoundedCornerShape(18.dp)){Row(Modifier.fillMaxSize().padding(14.dp),verticalAlignment=Alignment.CenterVertically){Text(icon,fontSize=25.sp,color=DarkAccent);Spacer(Modifier.width(12.dp));Text(title,fontSize=16.sp)}}}
@Composable private fun MiniAction(icon:String,title:String,modifier:Modifier,onClick:()->Unit){OutlinedCard(onClick=onClick,modifier=modifier.height(106.dp),shape=RoundedCornerShape(18.dp)){Column(Modifier.fillMaxSize(),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){Text(icon,fontSize=29.sp,color=DarkAccent,fontWeight=FontWeight.Bold);Spacer(Modifier.height(8.dp));Text(title,fontSize=13.sp)}}}

@Composable private fun AccountsScreen(data: AppData, onBack: () -> Unit, onAdd: () -> Unit, onPrimary: (String) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        PageTop("Edit Accounts", back = onBack, action = "+", onAction = onAdd)
        OutlinedTextField("", {}, placeholder = { Text("Search accounts...") }, modifier = Modifier.padding(horizontal = 18.dp).fillMaxWidth(), shape = RoundedCornerShape(18.dp))
        Row(Modifier.padding(18.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("▰", fontSize = 26.sp, color = DarkAccent); Spacer(Modifier.width(15.dp)); Column(Modifier.weight(1f)) { Text("Account Label", fontWeight = FontWeight.Bold); Text("Add account label for all transactions", color = Muted) }; Switch(false,{}) }
        Text("▣   Exchange Rates                                      ›", modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp), fontWeight = FontWeight.Bold, fontSize = 18.sp)
        data.accounts.forEach { a ->
            Card(Modifier.padding(horizontal = 12.dp, vertical = 6.dp).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Soft), shape = RoundedCornerShape(22.dp)) {
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(a.name, fontSize = 23.sp, fontWeight = FontWeight.Black); Text("${money(a.balance,a.currency)} ${a.currency}", fontSize = 17.sp)
                        if(a.primary) Text("Primary", modifier=Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFFB6C5E7)).padding(horizontal=8.dp,vertical=2.dp),fontSize=12.sp)
                        Text("${data.transactions.count{it.account==a.id||it.destination==a.id}} transactions",color=Muted)
                    }
                    Text(if(a.primary) "★" else "☆", modifier = Modifier.clickable { onPrimary(a.id) }.padding(8.dp), fontSize = 29.sp); Text("  ≡", fontSize = 27.sp)
                }
            }
        }
        Spacer(Modifier.weight(1f)); Box(Modifier.padding(18.dp).size(64.dp).align(Alignment.End).clip(RoundedCornerShape(22.dp)).background(DarkAccent).clickable{onAdd()},contentAlignment=Alignment.Center){Text("+",fontSize=38.sp,color=Color.White)}
    }
}

@Composable private fun AddAccountScreen(onBack:()->Unit,onSave:(String,Double,String)->Unit){
    var name by remember{ mutableStateOf("") }; var amount by remember{ mutableStateOf("") }; var currency by remember{ mutableStateOf("USD") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        PageTop("Add Account",back=onBack)
        Column(Modifier.padding(24.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
            OutlinedTextField(name,{name=it},label={Text("Name")},modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp))
            Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){listOf(Accent,IncomeGreen,Color(0xFF2AA89C),Color(0xFF2ABFD1),Color(0xFF409DE1),Color(0xFF4055B9)).forEach{Box(Modifier.size(50.dp).clip(CircleShape).background(it))}}
            OutlinedTextField(amount,{amount=it.filter{c->c.isDigit()||c=='.'}},label={Text("Starting at")},prefix={Text(if(currency=="EUR")"€" else if(currency=="JPY")"¥" else "$")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp))
            ActionWide(".00","Decimal Precision",""){}
            OutlinedTextField("",{},placeholder={Text("Search currencies...")},modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp))
            listOf(listOf("USD","EUR","JPY"),listOf("GBP","AUD","CAD")).forEach{row->
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){row.forEach{code->
                    Card(Modifier.weight(1f).height(112.dp).then(if(currency==code)Modifier.border(1.5.dp,DarkAccent,RoundedCornerShape(18.dp)) else Modifier).clickable{currency=code},colors=CardDefaults.cardColors(containerColor=Soft),shape=RoundedCornerShape(18.dp)){
                        Column(Modifier.fillMaxSize(),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){Text(code,fontSize=19.sp);Text(when(code){"EUR"->"€";"JPY"->"¥";"GBP"->"£";else->"$"},fontSize=28.sp,fontWeight=FontWeight.Bold)}
                    }
                }}
            }
            Button(onClick={onSave(name,amount.toDoubleOrNull()?:0.0,currency)},enabled=name.isNotBlank(),modifier=Modifier.fillMaxWidth().height(60.dp),shape=RoundedCornerShape(22.dp),colors=ButtonDefaults.buttonColors(containerColor=Accent)){Text(if(name.isBlank())"Set Name" else "Add Account")}
        }
    }
}

@Composable private fun CategoryScreen(onBack:()->Unit){
    val cats=listOf("Dining" to "🍴","Groceries" to "🛍","Shopping" to "🛍","Transit" to "🚊","Entertainment" to "🍿","Bills & Fees" to "💵","Gifts" to "🎁")
    LazyColumn(Modifier.fillMaxSize()) {
        item{PageTop("Edit Categories",back=onBack,action="+")}
        item{OutlinedTextField("",{},placeholder={Text("Search categories...")},modifier=Modifier.padding(18.dp).fillMaxWidth(),shape=RoundedCornerShape(18.dp))}
        items(cats){(name,icon)->
            Card(Modifier.padding(horizontal=12.dp,vertical=5.dp).fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=Soft),shape=RoundedCornerShape(22.dp)){
                Row(Modifier.padding(18.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(62.dp).clip(CircleShape).background(Color(0xFFD5E7D1)),contentAlignment=Alignment.Center){Text(icon,fontSize=29.sp)};Spacer(Modifier.width(15.dp));Column(Modifier.weight(1f)){Text(name,fontSize=22.sp,fontWeight=FontWeight.Black);Text("Expense\n${if(name=="Groceries")"1 transaction" else "0 transactions"}",color=Muted)};Text("▣   ≡",fontSize=23.sp)}
            }
        }
    }
}
@Composable private fun TitlesScreen(data:AppData,onBack:()->Unit){
    var ask by remember{mutableStateOf(true)};var note by remember{mutableStateOf(false)};var auto by remember{mutableStateOf(true)}
    Column(Modifier.fillMaxSize()){
        PageTop("Edit Titles",back=onBack,action="+");OutlinedTextField("",{},placeholder={Text("Search titles...")},modifier=Modifier.padding(18.dp).fillMaxWidth(),shape=RoundedCornerShape(18.dp))
        SettingToggle("Tᵀ","Ask For Transaction Title","When adding a transaction",ask){ask=it};SettingToggle("▤","Ask For Note With Title","When entering the title",note){note=it};SettingToggle("+","Automatically Add Titles","When a transaction is created",auto){auto=it}
        data.transactions.map{it.title}.filter{it.isNotBlank()}.distinct().takeLast(4).forEach{t->Card(Modifier.padding(horizontal=14.dp,vertical=5.dp).fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=Soft),shape=RoundedCornerShape(20.dp)){Row(Modifier.padding(18.dp)){Text("🛍  $t",modifier=Modifier.weight(1f),fontSize=18.sp);Text("▣  ≡")}}}
    }
}
@Composable private fun SettingToggle(icon:String,title:String,sub:String,value:Boolean,onChange:(Boolean)->Unit){Row(Modifier.fillMaxWidth().padding(horizontal=22.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically){Text(icon,fontSize=25.sp,color=DarkAccent);Spacer(Modifier.width(18.dp));Column(Modifier.weight(1f)){Text(title,fontSize=18.sp,fontWeight=FontWeight.Bold);Text(sub,fontSize=14.sp,color=Muted)};Switch(value,onChange)}}

@Composable private fun SettingsScreen(onBack:()->Unit){
    var material by remember{mutableStateOf(true)};var bio by remember{mutableStateOf(false)}
    LazyColumn(Modifier.fillMaxSize()){
        item{PageTop("Settings",back=onBack);ActionWide("●","Accent Color","Blue"){};SettingToggle("◉","Material You","Use system colors",material){material=it};ActionWide("◐","Theme Mode","System"){};ActionWide("⌂","Edit Home Page","Customize visible sections"){};SettingToggle("▣","Biometric Lock","Require authentication",bio){bio=it};ActionWide("文","Language","System"){};ActionWide("⚙","More Options","Advanced settings"){};ActionWide("÷","Bill Splitter","Split a transaction"){};Spacer(Modifier.height(18.dp));Text("Import & Export",modifier=Modifier.padding(20.dp),fontSize=22.sp,fontWeight=FontWeight.Black);ActionWide("⇩","Export CSV File","Share a spreadsheet"){};ActionWide("⇧","Import CSV File","Template available"){};ActionWide("▣","Google Drive","Backup and restore"){}}
    }
}

@Composable private fun SummaryScreen(data:AppData,onBack:()->Unit){
    val ex=data.transactions.filter{it.kind==TxKind.EXPENSE&&it.included}.sumOf{it.amount};val inc=data.transactions.filter{it.kind==TxKind.INCOME&&it.included}.sumOf{it.amount}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())){
        PageTop("All Spending Summary",back=onBack);Segmented(listOf("Current","History"),"Current"){};Spacer(Modifier.height(22.dp));Text("All Time",modifier=Modifier.fillMaxWidth(),textAlign=TextAlign.Center,fontSize=19.sp,fontWeight=FontWeight.Bold)
        Text(money(inc-ex),modifier=Modifier.fillMaxWidth().padding(top=24.dp),textAlign=TextAlign.Center,fontSize=38.sp,fontWeight=FontWeight.Black);Text("Net Total  •  ${data.transactions.size} transactions",modifier=Modifier.fillMaxWidth(),textAlign=TextAlign.Center,color=Muted)
        Row(Modifier.padding(18.dp).fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly){Column(horizontalAlignment=Alignment.CenterHorizontally){Text("Expense",color=ExpenseRed);Text(money(ex),fontSize=24.sp,fontWeight=FontWeight.Bold)};Column(horizontalAlignment=Alignment.CenterHorizontally){Text("Income",color=IncomeGreen);Text(money(inc),fontSize=24.sp,fontWeight=FontWeight.Bold)}}
        Box(Modifier.fillMaxWidth().height(250.dp),contentAlignment=Alignment.Center){Canvas(Modifier.size(190.dp)){drawArc(Soft2,-90f,360f,false,style=Stroke(30f));val sweep=if(ex+inc==0.0)0f else (ex/(ex+inc)*360).toFloat();drawArc(Accent,-90f,sweep,false,style=Stroke(30f,cap=StrokeCap.Round))};Text("Groceries\n${if(ex==0.0)0 else 100}%",textAlign=TextAlign.Center,fontWeight=FontWeight.Bold)}
        ActionWide("🛍","Groceries","${money(ex)}  •  ${data.transactions.count{it.kind==TxKind.EXPENSE}} transactions"){}
    }
}

@Composable private fun EmptyFinanceScreen(title:String,total:String,subtitle:String,empty:String,tabs:List<String> = listOf("All","Upcoming","Overdue"),onBack:()->Unit,onAdd:()->Unit){
    Column(Modifier.fillMaxSize()){
        PageTop(title,back=onBack,action=if(title=="Scheduled"||title=="Subscriptions")"⚙" else null);Text(total,modifier=Modifier.fillMaxWidth(),textAlign=TextAlign.Center,fontSize=38.sp,fontWeight=FontWeight.Black);Text(subtitle,modifier=Modifier.fillMaxWidth(),textAlign=TextAlign.Center,fontSize=19.sp);Spacer(Modifier.height(22.dp));Segmented(if(title=="Subscriptions")listOf("Monthly","Yearly","Total") else tabs,if(title=="Subscriptions")"Monthly" else tabs.first()){};Spacer(Modifier.height(50.dp))
        Box(Modifier.align(Alignment.CenterHorizontally).size(270.dp).clip(RoundedCornerShape(80.dp)).background(Soft),contentAlignment=Alignment.Center){Text("♟   ☆\n    ◇",fontSize=42.sp,textAlign=TextAlign.Center,color=Accent)}
        Text(empty,modifier=Modifier.fillMaxWidth().padding(22.dp),textAlign=TextAlign.Center,color=Muted,fontSize=18.sp);Spacer(Modifier.weight(1f));Box(Modifier.padding(18.dp).size(64.dp).align(Alignment.End).clip(RoundedCornerShape(22.dp)).background(DarkAccent).clickable{onAdd()},contentAlignment=Alignment.Center){Text("+",fontSize=38.sp,color=Color.White)}
    }
}

@Composable private fun GoalsScreen(onBack:()->Unit){
    Column(Modifier.fillMaxSize()){
        PageTop("Goals",back=onBack,action="✎");OutlinedCard(onClick={},modifier=Modifier.padding(16.dp).fillMaxWidth().height(160.dp),shape=RoundedCornerShape(22.dp)){Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text("+",fontSize=38.sp,color=Color.LightGray)}};Text("Example Goals",modifier=Modifier.fillMaxWidth().padding(12.dp),textAlign=TextAlign.Center,color=Muted)
        listOf("Trip Savings Jar" to "$1,110 / $1,500","Car Loan Payment" to "$990 / $2,000").forEach{(n,a)->Card(Modifier.padding(14.dp).fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=Color(0xFFF6F6FA)),shape=RoundedCornerShape(22.dp)){Column(Modifier.padding(22.dp)){Text(n,fontSize=23.sp,fontWeight=FontWeight.Black,color=Muted);Text("September 1",color=Muted);Text(a,modifier=Modifier.fillMaxWidth(),textAlign=TextAlign.End,fontSize=21.sp,fontWeight=FontWeight.Bold,color=Muted);LinearProgressIndicator(progress={.6f},modifier=Modifier.fillMaxWidth().height(14.dp).clip(CircleShape),color=Color.LightGray)}}}
    }
}

@Composable private fun EditHomeScreen(onBack:()->Unit){
    val names=listOf("Homepage Banner","Accounts","Accounts List","Budgets","Goals","Income & Expenses","Net Worth","Overdue & Upcoming","Loans","Long Term Loans")
    val active=remember{mutableStateMapOf<String,Boolean>().apply{put("Homepage Banner",true);put("Accounts",true);put("Budgets",true)}}
    LazyColumn(Modifier.fillMaxSize()){
        item{PageTop("Edit Home",back=onBack);Text("Tap each section to customize",modifier=Modifier.padding(horizontal=20.dp),fontSize=20.sp);Spacer(Modifier.height(12.dp))}
        items(names){n->Card(Modifier.padding(horizontal=12.dp,vertical=6.dp).fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=Soft),shape=RoundedCornerShape(22.dp)){Row(Modifier.padding(18.dp),verticalAlignment=Alignment.CenterVertically){Text("◈",fontSize=27.sp,color=DarkAccent);Spacer(Modifier.width(16.dp));Text(n,modifier=Modifier.weight(1f),fontSize=20.sp,fontWeight=FontWeight.Bold);Text("⋮");Switch(active[n]==true,{active[n]=it});Text(" ≡",fontSize=22.sp)}}}
    }
}
