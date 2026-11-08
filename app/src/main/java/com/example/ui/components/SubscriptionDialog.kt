package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.crypto.PaymentVerificationResult
import com.example.crypto.TronPaymentService
import com.example.ui.theme.LocalContrastTokens
import com.example.vpn.VpnStateManager
import com.example.vpn.model.SubscriptionPlan
import kotlinx.coroutines.launch

@Composable
fun SubscriptionDialog(
    plan: SubscriptionPlan,
    onDismiss: () -> Unit,
    onSuccessActivated: (SubscriptionPlan) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tokens = LocalContrastTokens.current

    val expectedUsdt = remember(plan.id) {
        TronPaymentService.getPlanAmountUsdt(plan.id)
    }

    var txidInput by remember { mutableStateOf("") }
    var isVerifying by remember { mutableStateOf(false) }
    var verificationStatus by remember { mutableStateOf<PaymentVerificationResult?>(null) }
    var addressCopied by remember { mutableStateOf(false) }
    var showQrCode by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = { if (!isVerifying) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = tokens.cardBackground,
            border = BorderStroke(tokens.cardBorderWidth, tokens.cardBorder),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 16.dp)
                .testTag("crypto_subscription_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        enabled = !isVerifying
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "بستن",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "خرید اشتراک با ارز دیجیتال",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Diamond,
                            contentDescription = "VIP",
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Plan & Required Amount Info
                CyberCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = plan.titleFa,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFFFD700).copy(alpha = 0.2f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "$expectedUsdt USDT",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFFFFD700)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = plan.durationTextFa,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "مبلغ واریز ترون / تتر:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${expectedUsdt.toInt()} تتر (یا معادل ترون TRX)",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = tokens.successColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Target TRON Wallet Address Card
                Text(
                    text = "آدرس اختصاصی واریز (شبکه TRC-20):",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, tokens.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFF3D71))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = TronPaymentService.NETWORK_NAME,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFFFF3D71)
                                )
                            }

                            Row {
                                // Toggle QR Button
                                IconButton(
                                    onClick = { showQrCode = !showQrCode },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QrCode,
                                        contentDescription = "QR Code",
                                        tint = tokens.accentGlow,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                // Copy Address Button
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("TRON Address", TronPaymentService.TRON_WALLET_ADDRESS)
                                        clipboard.setPrimaryClip(clip)
                                        addressCopied = true
                                    },
                                    modifier = Modifier.size(32.dp).testTag("copy_tron_address_btn")
                                ) {
                                    Icon(
                                        imageVector = if (addressCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                                        contentDescription = "Copy",
                                        tint = if (addressCopied) tokens.successColor else tokens.accentGlow,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // The Address String
                        Text(
                            text = TronPaymentService.TRON_WALLET_ADDRESS,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = tokens.accentGlow,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("TRON Address", TronPaymentService.TRON_WALLET_ADDRESS)
                                    clipboard.setPrimaryClip(clip)
                                    addressCopied = true
                                }
                        )

                        if (addressCopied) {
                            Text(
                                text = "آدرس با موفقیت در کلیپ‌بورد کپی شد ✅",
                                style = MaterialTheme.typography.labelSmall,
                                color = tokens.successColor,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }

                // QR Code Display
                AnimatedVisibility(visible = showQrCode) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 12.dp)
                    ) {
                        CryptoQrCodeView(
                            address = TronPaymentService.TRON_WALLET_ADDRESS,
                            sizeDp = 180
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "اسکن آدرس کیف پول با تراست والت، نوبیتکس، بایننس و ...",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // TXID Input Section
                Text(
                    text = "کد هش تراکنش (TXID):",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = txidInput,
                    onValueChange = {
                        txidInput = it.trim()
                        verificationStatus = null
                    },
                    placeholder = {
                        Text(
                            "هش ۶۴ کاراکتری تراکنش واریز...",
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = tokens.accentGlow,
                        unfocusedBorderColor = tokens.cardBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("txid_input_field")
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Verification Result Feedback
                verificationStatus?.let { result ->
                    when (result) {
                        is PaymentVerificationResult.Success -> {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = tokens.successColor.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, tokens.successColor),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Success",
                                        tint = tokens.successColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "تراکنش در شبکه ترون تایید شد!",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            ),
                                            color = tokens.successColor
                                        )
                                        Text(
                                            text = "مبلغ تایید شده: ${result.confirmedAmount} ${result.tokenSymbol} • توکن امنیتی صادر شد",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                    }
                                }
                            }
                        }
                        is PaymentVerificationResult.Pending -> {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = tokens.warningColor.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, tokens.warningColor),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.HourglassTop,
                                        contentDescription = "Pending",
                                        tint = tokens.warningColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = result.messageFa,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }
                        }
                        is PaymentVerificationResult.Failed -> {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFFF3D71).copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, Color(0xFFFF3D71)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Error,
                                        contentDescription = "Failed",
                                        tint = Color(0xFFFF3D71),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = result.reasonFa,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }
                        }
                        is PaymentVerificationResult.NetworkError -> {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, tokens.cardBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Error",
                                        tint = tokens.warningColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = result.errorFa,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Verify Button
                Button(
                    onClick = {
                        if (txidInput.isNotBlank()) {
                            isVerifying = true
                            scope.launch {
                                val result = TronPaymentService.verifyTransaction(
                                    context = context,
                                    txid = txidInput,
                                    expectedUsdtAmount = expectedUsdt
                                )
                                verificationStatus = result
                                isVerifying = false

                                if (result is PaymentVerificationResult.Success) {
                                    // Activate VIP with cryptographic signature
                                    VpnStateManager.activateVip(
                                        context = context,
                                        planId = plan.id,
                                        days = plan.daysCount,
                                        planTitle = plan.titleFa
                                    )
                                    onSuccessActivated(plan)
                                }
                            }
                        }
                    },
                    enabled = txidInput.isNotBlank() && !isVerifying,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFD700),
                        disabledContainerColor = Color(0xFFFFD700).copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("verify_blockchain_payment_button")
                ) {
                    if (isVerifying) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.Black,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "در حال استعلام از بلاک‌چین ترون...",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Verify",
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "استعلام و تأیید تراکنش در شبکه ترون",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Security & Anti-Cheat badge
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(tokens.accentGlow.copy(alpha = 0.08f))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Shield",
                        tint = tokens.accentGlow,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "سامانه ضد تقلب فعال: بررسی مستقیم در بلاک‌چین ترون و مسدودسازی هش‌های تکراری و نامعتبر",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
