package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.GeminiCardioService
import com.example.ui.theme.*
import com.example.ui.viewmodel.CardioViewModel
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(
    viewModel: CardioViewModel,
    modifier: Modifier = Modifier
) {
    val messages by viewModel.chatMessages.collectAsState()
    val isLoading by viewModel.isChatLoading.collectAsState()
    val selectedModel by viewModel.selectedChatModel.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var inputMessage by remember { mutableStateOf("") }

    // Auto-scroll when messages change
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val sampleQueries = listOf(
        "How does Anterior ST elevation relate to LAD stenosis?",
        "Explain Inferior RWMA in RCA disease",
        "Why is >=50% narrowing the threshold for CAD?",
        "What are the indications for PCI vs CABG in multi-vessel CAD?"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CardioDarkBackground)
            .padding(horizontal = 16.dp)
            .navigationBarsPadding()
    ) {
        // Chat Header & Model Selector
        Card(
            colors = CardDefaults.cardColors(containerColor = CardioSurfaceDark),
            shape = RoundedCornerShape(18.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardioBorderDark),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 8.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Psychology, contentDescription = null, tint = VenousCyan, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cardiology AI Consultant", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                    }

                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(PerfusionHealthyEmerald, CircleShape)
                    )
                }

                // Model Switcher Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val models = listOf(
                        Triple(GeminiCardioService.MODEL_FAST_LITE, "Flash-Lite", "Fast"),
                        Triple(GeminiCardioService.MODEL_SEARCH_FLASH, "Flash + Search", "General"),
                        Triple(GeminiCardioService.MODEL_DEEP_THINKING, "Pro Think", "Complex")
                    )

                    models.forEach { (modelId, label, tag) ->
                        val isSelected = selectedModel == modelId
                        Surface(
                            onClick = { viewModel.setChatModel(modelId) },
                            color = if (isSelected) CardioSurfaceHighlight else CardioSurfaceVariantDark,
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isSelected) 1.2.dp else 1.dp,
                                color = if (isSelected) VenousCyan else CardioBorderDark
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) VenousCyan else TextPrimaryDark
                                )
                                Text(text = tag, fontSize = 9.sp, color = TextMutedDark)
                            }
                        }
                    }
                }
            }
        }

        // Suggested Query Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            items(sampleQueries) { q ->
                Surface(
                    onClick = {
                        inputMessage = q
                        viewModel.sendChatMessage(q)
                    },
                    color = CardioSurfaceDark,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardioBorderDark)
                ) {
                    Text(
                        text = q,
                        fontSize = 11.sp,
                        color = TextSecondaryDark,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Scrollable Message Thread
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                val isUser = msg.role == "user"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    Surface(
                        color = if (isUser) ArterialRedPrimary else CardioSurfaceDark,
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 16.dp
                        ),
                        border = if (isUser) null else androidx.compose.foundation.BorderStroke(1.dp, CardioBorderDark),
                        modifier = Modifier.widthIn(max = 300.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            if (!isUser) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = VenousCyan, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = msg.modelTag?.substringAfterLast("/") ?: "CardioVision AI",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = VenousCyan
                                    )
                                }
                            }
                            Text(
                                text = msg.content,
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                                color = if (isUser) Color.White else TextPrimaryDark
                            )
                        }
                    }
                }
            }

            if (isLoading) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Surface(
                            color = CardioSurfaceDark,
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardioBorderDark)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = VenousCyan, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("CardioVision AI is analyzing...", fontSize = 11.sp, color = TextSecondaryDark)
                            }
                        }
                    }
                }
            }
        }

        // Input Field & Send Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputMessage,
                onValueChange = { inputMessage = it },
                placeholder = { Text("Ask cardiology AI question...", fontSize = 12.sp, color = TextMutedDark) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input_field"),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VenousCyan,
                    unfocusedBorderColor = CardioBorderDark,
                    focusedTextColor = TextPrimaryDark,
                    unfocusedTextColor = TextPrimaryDark,
                    focusedContainerColor = CardioSurfaceDark,
                    unfocusedContainerColor = CardioSurfaceDark
                ),
                maxLines = 3
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    if (inputMessage.isNotBlank()) {
                        val text = inputMessage
                        inputMessage = ""
                        viewModel.sendChatMessage(text)
                    }
                },
                enabled = inputMessage.isNotBlank() && !isLoading,
                modifier = Modifier
                    .size(48.dp)
                    .background(if (inputMessage.isNotBlank()) ArterialRedPrimary else CardioSurfaceDark, CircleShape)
                    .testTag("chat_send_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = if (inputMessage.isNotBlank()) Color.White else TextMutedDark,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
