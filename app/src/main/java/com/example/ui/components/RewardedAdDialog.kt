package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.LocalContrastTokens
import kotlinx.coroutines.delay

import com.example.security.SecurityGuard

@Composable
fun RewardedAdDialog(
    onDismiss: () -> Unit,
    onRewardEarned: (String) -> Unit
) {
    val tokens = LocalContrastTokens.current
    var challengeNonce by remember { mutableStateOf("") }
    var remainingAdSeconds by remember { mutableIntStateOf(5) }
    var isCompleted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        challengeNonce = SecurityGuard.generateAdChallenge()
        while (remainingAdSeconds > 0) {
            delay(1000)
            remainingAdSeconds--
        }
        isCompleted = true
    }

    Dialog(onDismissRequest = {
        if (isCompleted) onDismiss()
    }) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = tokens.cardBackground,
            border = BorderStroke(tokens.cardBorderWidth, tokens.cardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("rewarded_ad_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header badge
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(tokens.accentGlow.copy(alpha = 0.15f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CardGiftcard,
                        contentDescription = "Reward",
                        tint = tokens.accentGlow,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "پاداش: +۱۰ دقیقه اتصال رایگان",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = tokens.accentGlow
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Simulated Ad Media Player Container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFF0F172A),
                                    Color(0xFF1E293B)
                                )
                            )
                        )
                        .border(1.dp, tokens.cardBorder, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        if (!isCompleted) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(tokens.accentGlow.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Playing Ad",
                                    tint = tokens.accentGlow,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "در حال پخش تبلیغ اسپانسر سرورها...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "امکان دریافت پاداش تا $remainingAdSeconds ثانیه دیگر",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8)
                            )
                        } else {
                            AnimatedVisibility(
                                visible = true,
                                enter = scaleIn() + fadeIn()
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Reward Ready",
                                        tint = Color(0xFF00E676),
                                        modifier = Modifier.size(52.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "تبلیغ با موفقیت مشاهده شد!",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                    Text(
                                        text = "+۱۰ دقیقه به نشست فعلی اضافه خواهد شد",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF00E676)
                                    )
                                }
                            }
                        }
                    }

                    // Progress bar across top of ad container
                    if (!isCompleted) {
                        val progress = (5 - remainingAdSeconds) / 5f
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .align(Alignment.TopCenter),
                            color = tokens.accentGlow,
                            trackColor = Color.White.copy(alpha = 0.2f),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action buttons
                if (isCompleted) {
                    Button(
                        onClick = {
                            onRewardEarned(challengeNonce)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = tokens.accentGlow),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("claim_reward_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Claim",
                            tint = Color.Black
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "دریافت ۱۰ دقیقه زمان اضافه",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                } else {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("skip_ad_button")
                    ) {
                        Text(
                            text = "انصراف ($remainingAdSeconds ثانیه باقیمانده)",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
