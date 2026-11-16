package com.example.spendsense

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class SummaryTest {

    private fun expense(title: String, amount: Double, category: String, date: LocalDate) =
        Expense(id = 0, title = title, amount = amount, category = category, date = date)

    private val november = YearMonth.of(2026, 11)

    @Test
    fun emptyList_givesZeroTotalAndNoCategories() {
        val result = summarize(emptyList(), november)
        assertEquals(0.0, result.total, 0.001)
        assertTrue(result.byCategory.isEmpty())
    }

    @Test
    fun onlyCountsExpensesInTheRequestedMonth() {
        val expenses = listOf(
            expense("Swiggy", 450.0, "Food", LocalDate.of(2026, 11, 3)),
            expense("Old bill", 999.0, "Bills", LocalDate.of(2026, 10, 30)),
            expense("Next month", 500.0, "Food", LocalDate.of(2026, 12, 1))
        )
        val result = summarize(expenses, november)
        assertEquals(450.0, result.total, 0.001)
        assertEquals(1, result.byCategory.size)
    }

    @Test
    fun groupsByCategory_andSumsEachGroup() {
        val expenses = listOf(
            expense("Swiggy", 450.0, "Food", LocalDate.of(2026, 11, 3)),
            expense("Zomato", 300.0, "Food", LocalDate.of(2026, 11, 9)),
            expense("Uber", 220.0, "Transport", LocalDate.of(2026, 11, 5))
        )
        val result = summarize(expenses, november)
        val food = result.byCategory.first { it.category == "Food" }
        assertEquals(750.0, food.total, 0.001)
        assertEquals(970.0, result.total, 0.001)
    }

    @Test
    fun categoriesAreSortedLargestFirst() {
        val expenses = listOf(
            expense("Uber", 220.0, "Transport", LocalDate.of(2026, 11, 5)),
            expense("Rent", 9000.0, "Bills", LocalDate.of(2026, 11, 1)),
            expense("Swiggy", 450.0, "Food", LocalDate.of(2026, 11, 3))
        )
        val result = summarize(expenses, november)
        assertEquals(listOf("Bills", "Food", "Transport"), result.byCategory.map { it.category })
    }

    @Test
    fun firstAndLastDayOfMonthAreIncluded() {
        val expenses = listOf(
            expense("First", 10.0, "Other", LocalDate.of(2026, 11, 1)),
            expense("Last", 20.0, "Other", LocalDate.of(2026, 11, 30))
        )
        assertEquals(30.0, summarize(expenses, november).total, 0.001)
    }
    @Test
    fun sameMonthDifferentYearsAreIncluded(){
        val expenses =  listOf(
            expense("Uber", 220.0, "Transport", LocalDate.of(2025, 11, 5)),
            expense("Rent", 9000.0, "Bills", LocalDate.of(2026, 11, 1)),
        )

    }
}