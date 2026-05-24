package com.example.diphone.feature.calls

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.diphone.core.data.system.SoundHelper
import com.example.diphone.theme.AppColors

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DialPadSheet(
    onCall: (String) -> Unit
) {
    var dialString by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.Surface)
            .padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Number display
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = dialString.ifEmpty { "Enter number" },
                fontSize = if (dialString.isEmpty()) 18.sp else 32.sp,
                fontWeight = FontWeight.Bold,
                color = if (dialString.isEmpty()) AppColors.Subtitle else Color.White,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
        }

        // 3×4 key grid
        val rows = listOf(
            listOf("1" to "∞", "2" to "ABC", "3" to "DEF"),
            listOf("4" to "GHI", "5" to "JKL", "6" to "MNO"),
            listOf("7" to "PQRS", "8" to "TUV", "9" to "WXYZ"),
            listOf("*" to "", "0" to "+", "#" to "")
        )

        Column(
            modifier = Modifier.padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            rows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    row.forEach { (num, letters) ->
                        DialKey(
                            digit = num,
                            letters = letters,
                            onPress = { 
                                dialString += it
                                SoundHelper.playDtmfTone(it[0])
                            },
                            onLongPress = { digit ->
                                when (digit) {
                                    "0" -> {
                                        dialString += "+"
                                        SoundHelper.playDtmfTone('+')
                                    }
                                    "1" -> onCall("voicemail") // Long-press 1 = voicemail
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action row: Contacts icon | Call button | Backspace
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Contacts shortcut (left)
                IconButton(
                    onClick = { /* Contact picker — future */ },
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Contacts,
                        contentDescription = "Contacts",
                        tint = AppColors.Subtitle,
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Green call button (centre)
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(
                            if (dialString.isNotEmpty()) AppColors.Primary
                            else AppColors.Surface
                        )
                        .combinedClickable(
                            onClick = {
                                if (dialString.isNotEmpty()) {
                                    SoundHelper.playButtonClickTone()
                                    onCall(dialString)
                                }
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Call",
                        tint = if (dialString.isNotEmpty()) Color.White else AppColors.Subtitle,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Backspace (right)
                BackspaceKey(
                    onPress = {
                        if (dialString.isNotEmpty()) {
                            SoundHelper.playButtonClickTone()
                            dialString = dialString.dropLast(1)
                        }
                    },
                    onLongPress = { 
                        if (dialString.isNotEmpty()) {
                            SoundHelper.playButtonClickTone()
                            dialString = ""
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DialKey(
    digit: String,
    letters: String,
    onPress: (String) -> Unit,
    onLongPress: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.05f))
            .combinedClickable(
                onClick = { onPress(digit) },
                onLongClick = { onLongPress(digit) }
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = digit,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            if (letters.isNotEmpty()) {
                Text(
                    text = letters,
                    fontSize = 9.sp,
                    color = AppColors.Subtitle,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BackspaceKey(
    onPress: () -> Unit,
    onLongPress: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .combinedClickable(
                onClick = onPress,
                onLongClick = onLongPress
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Backspace,
            contentDescription = "Backspace",
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}
