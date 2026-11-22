package com.example.util

import com.google.firebase.Timestamp
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CurrencyFormatter {
    private val numberFormat: NumberFormat = NumberFormat.getNumberInstance(Locale.US)

    fun formatTzs(amount: Long): String {
        val formatted = numberFormat.format(amount)
        return "TZS $formatted"
    }

    fun formatCompactTzs(amount: Long): String {
        return when {
            amount >= 1_000_000L -> String.format(Locale.US, "TZS %.1fM", amount / 1_000_000.0)
            amount >= 10_000L -> String.format(Locale.US, "TZS %.1fK", amount / 1_000.0)
            else -> formatTzs(amount)
        }
    }

    fun formatDateTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatRelativeTime(timestamp: Timestamp?): String {
        val millis = timestamp?.toDate()?.time ?: System.currentTimeMillis()
        return formatRelativeTime(millis)
    }

    fun formatRelativeTime(timestamp: Long): String {
        val diff = (System.currentTimeMillis() - timestamp).coerceAtLeast(0L)
        val minutes = diff / 60_000L
        val hours = diff / 3_600_000L
        val days = diff / 86_400_000L
        return when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            days < 7 -> "${days}d ago"
            else -> formatDateTime(timestamp)
        }
    }
}
