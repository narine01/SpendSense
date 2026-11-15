package com.example.spendsense

import android.R.attr.onClick
import android.os.Bundle
import android.text.style.TtsSpan
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.spendsense.ui.theme.SpendSenseTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Button
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.text.input.KeyboardType
import androidx.core.text.isDigitsOnly
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

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
//@Composable
//fun Greeting(name: String, modifier: Modifier = Modifier) {
//    Text(
//        text = "Hello $name!",
//        modifier = modifier
//    )
//}
@Composable
fun ExpenseRow(expense:Expense){
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(expense.title, style = MaterialTheme.typography.titleMedium)
            Text(expense.category, style = MaterialTheme.typography.bodySmall)
            Text(expense.date.format(DateTimeFormatter.ofPattern("dd MMM")), style = MaterialTheme.typography.bodySmall)
        }
        Text("₹%.2f".format(expense.amount))


    }
}

@Composable
fun ExpenseListScreen(
    expenses: List<Expense>, modifier: Modifier = Modifier
){
LazyColumn(modifier) {
     items(expenses) {ExpenseRow(it)}
}
}

@Composable
fun SpendSenseApp(viewModel: ExpenseViewModel = viewModel()) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "list") {
        composable("list") {
            val expenses by viewModel.expenses.collectAsStateWithLifecycle()
            Scaffold(
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
fun ExpenseFormScreen(expenseId: Int, viewModel: ExpenseViewModel, onDone: () -> Unit) {
    var existing by remember { mutableStateOf<Expense?>(null) }
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }

    LaunchedEffect(expenseId) {
        if (expenseId != -1) {
            viewModel.getExpense(expenseId)?.let {
                existing = it
                title = it.title
                amount = it.amount.toString()
                category = it.category
            }
        }
    }

    val amountValue = amount.toDoubleOrNull()
    val isAmountError = amount.isNotEmpty() && amountValue == null

    Scaffold { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                if (expenseId == -1) "New expense" else "Edit expense",
                style = MaterialTheme.typography.headlineSmall
            )
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    enabled = title.isNotBlank() && amountValue != null,
                    onClick = {
                        viewModel.saveExpense(
                            id = existing?.id ?: 0,
                            title = title,
                            amount = amountValue!!,
                            category = category.ifBlank { "Other" },
                            date = existing?.date ?: LocalDate.now()
                        )
                        onDone()
                    }
                ) { Text("Save") }
                OutlinedButton(onClick = onDone) { Text("Cancel") }
            }
        }
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
    LazyColumn(modifier) {
        items(expenses, key = { it.id }) { expense ->
            ExpenseRow(expense, onClick = { onClick(expense) }, onDelete = { onDelete(expense.id) })
        }
    }
}