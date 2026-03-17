package com.expensio.ui.personal

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.expensio.domain.model.Analytics
import com.expensio.domain.model.MonthlySpend
import java.math.BigDecimal

private val ranges = listOf("3m" to "3M", "6m" to "6M", "1y" to "1Y", "all" to "All")

@Composable
fun PersonalScreen(viewModel: PersonalViewModel = hiltViewModel()) {
    val analytics by viewModel.analytics.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val selectedRange by viewModel.range.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("My Analytics", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        // Range selector chips
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ranges.forEach { (key, label) ->
                FilterChip(
                    selected = selectedRange == key,
                    onClick = { viewModel.setRange(key) },
                    label = { Text(label) }
                )
            }
        }

        when {
            isLoading -> Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            error != null -> Text(
                "Failed to load analytics: $error",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
            analytics != null -> AnalyticsContent(analytics!!)
        }
    }
}

@Composable
private fun AnalyticsContent(data: Analytics) {
    // Summary cards row
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SummaryCard(
            modifier = Modifier.weight(1f),
            label = "Net Balance",
            amount = data.netBalance,
            positive = data.netBalance >= BigDecimal.ZERO
        )
        SummaryCard(
            modifier = Modifier.weight(1f),
            label = "Groups",
            count = data.groupCount
        )
    }

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SummaryCard(
            modifier = Modifier.weight(1f),
            label = "This Month",
            amount = data.thisMonth
        )
        SummaryCard(
            modifier = Modifier.weight(1f),
            label = "Last Month",
            amount = data.lastMonth
        )
    }

    // Monthly bar chart
    if (data.byMonth.isNotEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Monthly Trend", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                MonthlyBarChart(data.byMonth)
            }
        }
    }

    // Category breakdown
    if (data.byCategory.isNotEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("By Category", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                data.byCategory.forEach { cat ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(cat.category, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "₹${cat.amount.setScale(0)} · ${cat.percentage}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        LinearProgressIndicator(
                            progress = { (cat.percentage / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(6.dp),
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                    }
                }
            }
        }
    }

    // Total spent footer
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Total Spent", style = MaterialTheme.typography.titleMedium)
            Text(
                "₹${data.totalSpent.setScale(0)}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun SummaryCard(
    modifier: Modifier = Modifier,
    label: String,
    amount: BigDecimal? = null,
    count: Int? = null,
    positive: Boolean = true,
) {
    Card(modifier = modifier, elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (amount != null) {
                val color = if (positive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                Text(
                    "₹${amount.setScale(0)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            } else if (count != null) {
                Text(
                    "$count",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun MonthlyBarChart(months: List<MonthlySpend>) {
    val barColor = MaterialTheme.colorScheme.primary
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    val maxAmount = months.maxOfOrNull { it.amount.toFloat() }?.takeIf { it > 0f } ?: 1f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        months.forEach { m ->
            val fraction = m.amount.toFloat() / maxAmount
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier
                    .weight(1f)
                    .height(120.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .weight(1f)
                        .width(20.dp)
                ) {
                    val barHeight = size.height * fraction
                    drawRect(
                        color = barColor,
                        topLeft = Offset((size.width - 20.dp.toPx()) / 2f, size.height - barHeight),
                        size = Size(20.dp.toPx(), barHeight)
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = m.month.takeLast(2),
                    style = MaterialTheme.typography.labelSmall,
                    color = labelColor,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
