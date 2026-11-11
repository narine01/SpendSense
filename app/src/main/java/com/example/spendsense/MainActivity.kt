package com.example.spendsense

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
import androidx.compose.material.icons.filled.Delete
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
data class  Expense (
    val id:Int,
    val title:String,
    val amount: Double,
    val category: String,
    val date: LocalDate
)
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
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()

    Scaffold { padding ->
        Column(Modifier.padding(padding)) {
            AddExpenseForm(onAdd = viewModel::addExpense)
            ExpenseListScreen(expenses, onDelete = viewModel::deleteExpense, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun AddExpenseForm(onAdd: (String, Double, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    val isAmountError = amount.isNotEmpty() && amount.toDoubleOrNull() == null

    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Title") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it},
            isError = isAmountError,
            supportingText = {
                if (isAmountError) Text("Enter a valid number")
            },
            label = { Text("Amount (₹)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()

        )
        OutlinedTextField(
            value = category,
            onValueChange = { category = it },
            label = { Text("Category") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            enabled = title.isNotBlank() && amount.toDoubleOrNull() != null,

            onClick = {
            val amt = amount.toDoubleOrNull()
            if (title.isNotBlank() && amt != null) {
                onAdd(title, amt, category.ifBlank { "Other" })
                title = ""
                amount = ""
                category = ""
            }
        }) {
            Text("Add expense")
        }
    }
}
@Composable
fun ExpenseRow(expense: Expense, onDelete: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
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
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier) {
        items(expenses, key = { it.id }) { expense ->
            ExpenseRow(expense, onDelete = { onDelete(expense.id) })
        }
    }
}