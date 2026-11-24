package com.example.spendsense

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.spendsense.ui.theme.SpendSenseTheme
import dagger.hilt.android.AndroidEntryPoint
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SpendSenseTheme {
                SpendSenseApp()
            }
        }
    }
}

@Composable
fun SpendSenseApp(viewModel: ExpenseViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val current = backStack?.destination?.route

    NavHost(navController = navController, startDestination = "list") {
        composable("list") {
            val expenses by viewModel.expenses.collectAsStateWithLifecycle()
            Scaffold(
                bottomBar = { BottomBar(current, navController) }
            ) { padding ->
                ExpenseListScreen(
                    expenses = expenses,
                    onDelete = viewModel::deleteExpense,
                    onClick = { navController.navigate("edit/${it.id}") },
                    onAdd = { navController.navigate("edit/-1") },
                    modifier = Modifier.padding(padding)
                )
            }
        }
        composable("summary") {
            val expenses by viewModel.expenses.collectAsStateWithLifecycle()
            Scaffold(bottomBar = { BottomBar(current, navController) }) { padding ->
                SummaryScreen(expenses, Modifier.padding(padding))
            }
        }
        composable("chat") {
            Scaffold(bottomBar = { BottomBar(current, navController) }) { padding ->
                ChatScreen(viewModel, Modifier.padding(padding))
            }
        }
        composable(
            route = "edit/{expenseId}",
            arguments = listOf(navArgument("expenseId") { type = NavType.IntType })
        ) { entry ->
            val id = entry.arguments?.getInt("expenseId") ?: -1
            ExpenseFormScreen(id, viewModel, onDone = { navController.popBackStack() })
        }
    }
}

@Composable
fun BottomBar(current: String?, navController: NavHostController) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp
    ) {
        NavigationBarItem(
            selected = current == "list",
            onClick = { navController.navigate("list") { launchSingleTop = true } },
            icon = { Icon(Icons.Filled.Home, contentDescription = null) },
            label = { Text("Home") }
        )
        NavigationBarItem(
            selected = current == "summary",
            onClick = { navController.navigate("summary") { launchSingleTop = true } },
            icon = { Icon(Icons.Filled.BarChart, contentDescription = null) },
            label = { Text("Insights") }
        )
        NavigationBarItem(
            selected = current == "chat",
            onClick = { navController.navigate("chat") { launchSingleTop = true } },
            icon = { Icon(Icons.Filled.ChatBubbleOutline, contentDescription = null) },
            label = { Text("Ask") }
        )
    }
}

private fun categoryIcon(category: String): ImageVector = when (category.trim().lowercase()) {
    "food", "groceries", "dining" -> Icons.Filled.Restaurant
    "transport", "travel" -> Icons.Filled.DirectionsCar
    "shopping" -> Icons.Filled.ShoppingCart
    "entertainment" -> Icons.Filled.Movie
    "health", "medical" -> Icons.Filled.LocalHospital
    "housing", "home" -> Icons.Filled.Home
    else -> Icons.AutoMirrored.Filled.ReceiptLong
}

@Composable
fun ExpenseRow(expense: Expense, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 14.dp, bottom = 14.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(44.dp).background(
                    MaterialTheme.colorScheme.secondaryContainer,
                    RoundedCornerShape(14.dp)
                ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = categoryIcon(expense.category),
                    contentDescription = expense.category,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(expense.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${expense.category}  ·  ${expense.date.format(DateTimeFormatter.ofPattern("dd MMM"))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text("₹%.2f".format(expense.amount), style = MaterialTheme.typography.titleSmall)
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete expense",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun ExpenseListScreen(
    expenses: List<Expense>,
    onDelete: (Int) -> Unit,
    onClick: (Expense) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pendingDelete by remember { mutableStateOf<Expense?>(null) }

    val thisMonth = summarize(expenses, YearMonth.now())
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(Modifier.padding(bottom = 4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("SpendSense", style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary)
                    if (BuildConfig.DEBUG) {
                        Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = CircleShape) {
                            Text("DEMO", modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer)
                        }
                    }
                }
                Text("Your money, in focus.", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(Modifier.fillMaxWidth().padding(22.dp)) {
                    Text("SPENT THIS MONTH", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("₹%.2f".format(thisMonth.total), style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(top = 6.dp, bottom = 4.dp))
                    Text("${expenses.count { YearMonth.from(it.date) == YearMonth.now() }} transactions",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Recent expenses", style = MaterialTheme.typography.titleLarge)
                    Text("${expenses.size} total", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(onClick = onAdd, shape = RoundedCornerShape(16.dp)) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add")
                }
            }
        }
        if (expenses.isEmpty()) {
            item {
                Card(shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("A fresh start", style = MaterialTheme.typography.titleMedium)
                        Text("Add an expense or scan a bill to see your spending here.",
                            style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }
        }
        items(expenses, key = { it.id }) { expense ->
            ExpenseRow(expense, onClick = { onClick(expense) }, onDelete = { pendingDelete = expense })
        }
    }

    pendingDelete?.let { expense ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete expense?") },
            text = { Text("\"${expense.title}\" (₹${"%.2f".format(expense.amount)}) will be removed.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(expense.id)
                    pendingDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseFormScreen(expenseId: Int, viewModel: ExpenseViewModel, onDone: () -> Unit) {
    var existing by remember { mutableStateOf<Expense?>(null) }
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var showPicker by remember { mutableStateOf(false) }
    var showCamera by remember { mutableStateOf(false) }

    val scan by viewModel.scanState.collectAsStateWithLifecycle()
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::scanBill)
    }

    // Leaving the form cancels any scan still running, so "Reading bill..." can never get stuck.
    DisposableEffect(Unit) {
        onDispose { viewModel.cancelScan() }
    }

    LaunchedEffect(expenseId) {
        if (expenseId != -1) {
            viewModel.getExpense(expenseId)?.let {
                existing = it
                title = it.title
                amount = it.amount.toString()
                category = it.category
                date = it.date
            }
        }
    }

    // When the AI finishes, prefill the form. The user still reviews and taps Save.
    LaunchedEffect(scan) {
        val s = scan
        if (s is ScanState.Success) {
            title = s.bill.merchant
            amount = s.bill.amount.toString()
            category = s.bill.category
            // Ignore a missing, invalid or future date from the AI: keep the current date.
            s.bill.date.takeIf { it.isNotBlank() }
                ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                ?.takeIf { !it.isAfter(LocalDate.now()) }
                ?.let { date = it }
            viewModel.resetScan()
        }
    }

    val amountValue = amount.toDoubleOrNull()
    val isAmountError = amount.isNotEmpty() && amountValue == null
    val isDateValid = !date.isAfter(LocalDate.now())

    if (showCamera) {
        CameraCapture(
            onCaptured = { uri ->
                showCamera = false
                viewModel.scanBill(uri)
            },
            onClose = { showCamera = false }
        )
        return
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(Modifier.padding(bottom = 4.dp)) {
                Text(if (expenseId == -1) "New expense" else "Edit expense",
                    style = MaterialTheme.typography.headlineMedium)
                Text("Add the details below. You can edit them anytime.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Card(shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("QUICK ADD", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer)
                    Text("Let a bill do the typing for you.", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { filePicker.launch(arrayOf("image/*", "application/pdf")) },
                            enabled = scan !is ScanState.Loading,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (scan is ScanState.Loading) "Reading…" else "Pick file")
                        }
                        OutlinedButton(
                            onClick = { showCamera = true },
                            enabled = scan !is ScanState.Loading,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Camera")
                        }
                    }
                }
            }
            if (scan is ScanState.Loading) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = viewModel::cancelScan) { Text("Cancel scan") }
                }
            }
            (scan as? ScanState.Error)?.let {
                Card(shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text(it.message, color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(14.dp))
                }
            }
            OutlinedTextField(
                value = title, onValueChange = { title = it },
                label = { Text("What was it for?") }, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp), singleLine = true
            )
            OutlinedTextField(
                value = amount, onValueChange = { amount = it },
                isError = isAmountError,
                supportingText = { if (isAmountError) Text("Enter a valid number") },
                label = { Text("Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), singleLine = true,
                prefix = { Text("₹") }
            )
            OutlinedTextField(
                value = category, onValueChange = { category = it },
                label = { Text("Category") }, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp), singleLine = true
            )
            Card(onClick = { showPicker = true }, shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("DATE", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(date.format(DateTimeFormatter.ofPattern("dd MMM yyyy")),
                            style = MaterialTheme.typography.titleMedium)
                    }
                    Text("Change", color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    enabled = title.isNotBlank() && amountValue != null && isDateValid,
                    onClick = {
                        viewModel.saveExpense(
                            id = existing?.id ?: 0,
                            title = title,
                            amount = amountValue!!,
                            category = category.ifBlank { "Other" },
                            date = date
                        )
                        onDone()
                    }, modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(16.dp)
                ) { Text("Save expense") }
                OutlinedButton(onClick = onDone, modifier = Modifier.height(52.dp),
                    shape = RoundedCornerShape(16.dp)) { Text("Cancel") }
            }
        }
    }

    if (showPicker) {
        val todayUtcMillis = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= todayUtcMillis
                override fun isSelectableYear(year: Int) = year <= LocalDate.now().year
            }
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    showPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
fun SummaryScreen(expenses: List<Expense>, modifier: Modifier = Modifier) {
    var month by remember { mutableStateOf(YearMonth.now()) }
    val summary = summarize(expenses, month)
    val max = summary.byCategory.maxOfOrNull { it.total } ?: 1.0

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(Modifier.padding(bottom = 2.dp)) {
            Text("Your summary", style = MaterialTheme.typography.headlineMedium)
            Text("A clear view of where it went.", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(
            Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { month = month.minusMonths(1) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous month")
            }
            Text(
                month.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                style = MaterialTheme.typography.titleMedium
            )
            IconButton(
                onClick = { month = month.plusMonths(1) },
                enabled = month < YearMonth.now()
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next month")
            }
        }
        Card(shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.fillMaxWidth().padding(22.dp)) {
                Text("TOTAL SPENDING", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text("₹%.2f".format(summary.total), style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(top = 6.dp, bottom = 4.dp))
                Text("${expenses.count { YearMonth.from(it.date) == month }} transactions this month",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
        Text("By category", style = MaterialTheme.typography.titleLarge)
        if (summary.byCategory.isEmpty()) {
            Card(shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Text("No spending recorded for this month yet.",
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        summary.byCategory.forEach { item ->
            Card(shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(item.category, style = MaterialTheme.typography.titleMedium)
                        Text("₹%.2f".format(item.total), style = MaterialTheme.typography.titleSmall)
                    }
                    Box(Modifier.fillMaxWidth().height(8.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))) {
                        Box(Modifier.fillMaxWidth(fraction = (item.total / max).toFloat()).height(8.dp)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp)))
                    }
                }
            }
        }
    }
}
