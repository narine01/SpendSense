package com.example.spendsense

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ChatScreen(viewModel: ExpenseViewModel, modifier: Modifier = Modifier) {
    val messages by viewModel.chat.collectAsStateWithLifecycle()
    val asking by viewModel.asking.collectAsStateWithLifecycle()
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, asking) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }
    // Keep following the text while the answer grows.
    val lastLength = messages.lastOrNull()?.text?.length ?: 0
    LaunchedEffect(lastLength) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    Column(modifier.fillMaxSize().imePadding()) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp)) {
            Text("Ask SpendSense", style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary)
            Text("Your spending, answered.", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    Card(shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                        Column(Modifier.fillMaxWidth().padding(20.dp)) {
                            Text("Hi there 👋", style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("Ask a question about your expenses. Your data is used only to answer your request.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(top = 6.dp))
                            Spacer(Modifier.height(14.dp))
                            Text("Try: “How much did I spend on food this month?”",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
            }
            items(messages) { ChatBubble(it) }
        }
        Text("Your expenses are sent securely to answer questions.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text("Ask about your spending") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                maxLines = 3
            )
            if (asking) {
                OutlinedButton(onClick = viewModel::stopAsking) { Text("Stop") }
            } else {
                Button(
                    enabled = input.isNotBlank(),
                    onClick = {
                        viewModel.ask(input)
                        input = ""
                    }
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Send")
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.fromUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (message.fromUser) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp),
            tonalElevation = if (message.fromUser) 0.dp else 2.dp,
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Text(
                message.text.ifEmpty { "Thinking…" },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                color = if (message.fromUser) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
