package com.example.spendsense

import java.time.YearMonth

data class CategoryTotal(val category: String, val total: Double)

data class MonthSummary(val total: Double, val byCategory: List<CategoryTotal>)

fun summarize(expenses: List<Expense>, month: YearMonth): MonthSummary {
    val inMonth = expenses.filter { YearMonth.from(it.date) == month }
    val byCategory = inMonth
        .groupBy { it.category }
        .map { (category, items) -> CategoryTotal(category, items.sumOf { it.amount }) }
        .sortedByDescending { it.total }
    return MonthSummary(inMonth.sumOf { it.amount }, byCategory)
}