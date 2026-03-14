package com.rancon.freelivetv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rancon.freelivetv.ChannelViewModel
import com.rancon.freelivetv.data.Channel
import com.rancon.freelivetv.data.Program
import com.rancon.freelivetv.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun EpgScreen(
    viewModel: ChannelViewModel,
    onChannelClick: (String, String) -> Unit,
    onBackPressed: () -> Unit
) {
    val channels by viewModel.channels.collectAsState()
    val listState = rememberLazyListState()
    
    // Time slots for the header (24 hours)
    val timeSlots = remember {
        val slots = mutableListOf<Long>()
        val calendar = Calendar.getInstance().apply {
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        repeat(24) {
            slots.add(calendar.timeInMillis)
            calendar.add(Calendar.HOUR_OF_DAY, 1)
        }
        slots
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Black)
    ) {
        // Header
        Text(
            text = "TV GUIDE",
            color = White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )

        // Time Header Row
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 120.dp),
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            items(timeSlots) { time ->
                val format = SimpleDateFormat("HH:mm", Locale.getDefault())
                Box(
                    modifier = Modifier
                        .width(200.dp)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = format.format(Date(time)), color = LightGray, fontSize = 14.sp)
                }
            }
        }

        // Channels & Programs Grid
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize()
        ) {
            items(channels.filter { it.healthStatus == "ACTIVE" }) { channel ->
                EpgChannelRow(
                    channel = channel,
                    viewModel = viewModel,
                    timeSlots = timeSlots,
                    onChannelClick = onChannelClick
                )
                Divider(color = Gray.copy(alpha = 0.2f))
            }
        }
    }
}

@Composable
fun EpgChannelRow(
    channel: Channel,
    viewModel: ChannelViewModel,
    timeSlots: List<Long>,
    onChannelClick: (String, String) -> Unit
) {
    // Note: In a real app, you'd fetch all programs for this channel for the 24h window
    // For this implementation, we'll use a placeholder structure
    val currentProgram by viewModel.getCurrentProgram(channel.id).collectAsState(initial = null)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Channel Name Column
        Box(
            modifier = Modifier
                .width(120.dp)
                .fillMaxHeight()
                .background(DarkGray)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = channel.name,
                color = White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                maxLines = 2
            )
        }

        // Programs Row
        LazyRow(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Simplified: We'll show the current program spanning multiple slots for now
            // Future refinement: Map actual Program objects to time slots
            item {
                currentProgram?.let { program ->
                    EpgProgramBlock(
                        program = program,
                        width = 400.dp, // Dynamic width based on duration
                        onClick = { onChannelClick(channel.id, channel.name) }
                    )
                } ?: run {
                    Box(
                        modifier = Modifier
                            .width(600.dp)
                            .fillMaxHeight()
                            .padding(4.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(text = "No Information Available", color = Gray, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun EpgProgramBlock(
    program: Program,
    width: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    
    Card(
        onClick = onClick,
        modifier = Modifier
            .width(width)
            .fillMaxHeight()
            .padding(2.dp)
            .androidx.compose.ui.focus.onFocusChanged { isFocused = it.isFocused },
        colors = CardDefaults.cardColors(
            containerColor = if (isFocused) Red.copy(alpha = 0.8f) else Gray.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(4.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = program.title,
                color = White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = program.getTimeRange(),
                color = if (isFocused) White else LightGray,
                fontSize = 11.sp
            )
        }
    }
}
