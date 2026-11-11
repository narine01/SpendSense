package com.example.spendsense

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class ExpenseViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = Room.databaseBuilder(app, AppDatabase::class.java, "spendsense.db")
        .build()
        .expenseDao()

    val expenses: StateFlow<List<Expense>> = dao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addExpense(title: String, amount: Double, category: String) {
        viewModelScope.launch {
            dao.insert(
                Expense(title = title, amount = amount, category = category, date = LocalDate.now())
            )
        }
    }

    fun deleteExpense(id: Int) {
        viewModelScope.launch { dao.deleteById(id) }
    }
}