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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
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
                bottomBar = { BottomBar(current, navController) },
                floatingActionButton = {
                    FloatingActionButton(onClick = { navController.navigate("edit/-1") }) {
                        Icon(Icons.Default.Add, contentDescription = "Add expense")
                    }
                }
            ) { padding ->
                ExpenseListScreen(
                    expenses = expenses,
                    onDelete = viewModel::deleteExpense,
                    onClick = { navController.navigate("edit/${it.id}") },
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
    NavigationBar {
        NavigationBarItem(
            selected = current == "list",
            onClick = { navController.navigate("list") { launchSingleTop = true } },
            icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
            label = { Text("Expenses") }
        )
        NavigationBarItem(
            selected = current == "summary",
            onClick = { navController.navigate("summary") { launchSingleTop = true } },
            icon = { Icon(Icons.Default.Info, contentDescription = null) },
            label = { Text("Summary") }
        )
        NavigationBarItem(
            selected = current == "chat",
            onClick = { navController.navigate("chat") { launchSingleTop = true } },
            icon = { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null) },
            label = { Text("Ask AI") }
        )
    }
}

@Composable
fun ExpenseRow(expense: Expense, onClick: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(expense.title, style = MaterialTheme.typography.titleMedium)
            Text(expense.category, style = MaterialTheme.typography.bodySmall)
            Text(
                expense.date.format(DateTimeFormatter.ofPattern("dd MMM")),
                style = MaterialTheme.typography.bodySmall
            )
        }
        Text("₹%.2f".format(expense.amount))
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Delete")
        }
    }
}

@Composable
fun ExpenseListScreen(
    expenses: List<Expense>,
    onDelete: (Int) -> Unit,
    onClick: (Expense) -> Unit,
    modifier: Modifier = Modifier
) {
    var pendingDelete by remember { mutableStateOf<Expense?>(null) }

    if (expenses.isEmpty()) {
        Box(
            modifier.fillMaxSize().padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "No expenses yet.\nTap + to add one, or scan a bill.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        }
    } else {
        LazyColumn(modifier) {
            items(expenses, key = { it.id }) { expense ->
                ExpenseRow(
                    expense,
                    onClick = { onClick(expense) },
                    onDelete = { pendingDelete = expense }
                )
            }
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
            modifier = Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                if (expenseId == -1) "New expense" else "Edit expense",
                style = MaterialTheme.typography.headlineSmall
            )
            OutlinedButton(
                onClick = { filePicker.launch(arrayOf("image/*", "application/pdf")) },
                enabled = scan !is ScanState.Loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (scan is ScanState.Loading) "Reading bill…" else "Scan a bill (photo or PDF)")
            }
            OutlinedButton(
                onClick = { showCamera = true },
                enabled = scan !is ScanState.Loading,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Take a photo of a bill") }
            if (scan is ScanState.Loading) {
                TextButton(onClick = viewModel::cancelScan) { Text("Cancel scan") }
            }
            (scan as? ScanState.Error)?.let {
                Text(it.message, color = MaterialTheme.colorScheme.error)
            }
            OutlinedTextField(
                value = title, onValueChange = { title = it },
                label = { Text("Title") }, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = amount, onValueChange = { amount = it },
                isError = isAmountError,
                supportingText = { if (isAmountError) Text("Enter a valid number") },
                label = { Text("Amount (₹)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = category, onValueChange = { category = it },
                label = { Text("Category") }, modifier = Modifier.fillMaxWidth()
            )
            OutlinedButton(
                onClick = { showPicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Date: " + date.format(DateTimeFormatter.ofPattern("dd MMM yyyy")))
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
                    }
                ) { Text("Save") }
                OutlinedButton(onClick = onDone) { Text("Cancel") }
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

    Column(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            Modifier.fillMaxWidth(),
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
        Text("₹%.2f".format(summary.total), style = MaterialTheme.typography.displaySmall)

        if (summary.byCategory.isEmpty()) {
            Text("No expenses this month.")
        }
        summary.byCategory.forEach { item ->
            Column {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(item.category)
                    Text("₹%.2f".format(item.total))
                }
                Box(
                    Modifier
                        .fillMaxWidth(fraction = (item.total / max).toFloat())
                        .height(10.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(5.dp))
                )
            }
        }
    }
}
