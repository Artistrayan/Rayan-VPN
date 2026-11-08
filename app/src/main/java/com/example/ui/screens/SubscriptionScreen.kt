package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.ui.components.CyberCard
import com.example.ui.components.SubscriptionDialog
import com.example.ui.theme.LocalContrastTokens
import com.example.vpn.VpnStateManager
import com.example.vpn.model.SubscriptionPlan

@Composable
fun SubscriptionScreen(
    isVipActive: Boolean,
    vipExpiryDate: String,
    onVipActivated: (SubscriptionPlan) -> Unit
) {
    val tokens = LocalContrastTokens.current
    var selectedPlanToBuy by remember { mutableStateOf<SubscriptionPlan?>(null) }
    var showActivationCelebration by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("subscription_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // VIP Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF1E1B4B),
                            Color(0xFF312E81),
                            Color(0xFF0F172A)
                        )
                    )
                )
                .border(
                    width = if (tokens.isHighContrast) 2.dp else 1.dp,
                    color = Color(0xFFFFD700).copy(alpha = 0.6f),
                    shape = RoundedCornerShape(22.dp)
                )
                .padding(20.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFD700).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Diamond,
                        contentDescription = "VIP Diamond",
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (isVipActive) "اشتراک طلایی VIP شما فعال است" else "ارتقا به اشتراک طلایی Aria VIP",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (isVipActive)
                        "شما به تمام سرورهای ۱۰ گیگابیت بدون محدودیت زمانی دسترسی دارید ($vipExpiryDate)"
                    else
                        "سرعت خیره‌کننده، اتصال نامحدود بدون تایمر، بدون تبلیغات و آی‌پی ثابت",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFCBD5E1),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Dedicated Crypto Payment Banner with User's TRON Address
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = tokens.cardBackground,
            border = BorderStroke(tokens.cardBorderWidth, tokens.accentGlow),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF3D71).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = Color(0xFFFF3D71),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "درگاه واریز ارز دیجیتال (TRC-20)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF00E676).copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "ضد تقلب فعال",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                            color = Color(0xFF00E676)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "آدرس اختصاصی شبکه ترون جهت خرید اشتراک:\nTBMvBiVB6mhu1gnaAAE1Pg5YohKvV1NSnB",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                    color = tokens.accentGlow
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "سیستم ضد دورزدن: تمامی تراکنش‌ها مستقیماً در اکسپلورر شبکه ترون تایید و هش‌های تکراری مسدود می‌شوند.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Perks List
        Text(
            text = "مزایای اشتراک پریمیوم",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PerkRowItem(
                icon = Icons.Default.AllInclusive,
                title = "زمان اتصال ۱۰۰٪ نامحدود",
                desc = "حذف کامل تایمر ۱ ساعته و اتصال پیوسته بدون قطعی"
            )
            PerkRowItem(
                icon = Icons.Default.FlashOn,
                title = "سرعت فوق‌العاده ۱۰ گیگابیت",
                desc = "پهنای باند اختصاصی با پینگ زیر ۳۰ میلی‌ثانیه"
            )
            PerkRowItem(
                icon = Icons.Default.Block,
                title = "حذف دائمی تمام تبلیغات",
                desc = "تجربه‌ای کاملاً تمیز و بی‌صدا بدون هیچ آگهی درون‌برنامه‌ای"
            )
            PerkRowItem(
                icon = Icons.Default.SportsEsports,
                title = "نودهای گیمینگ و استریمینگ",
                desc = "سرورهای بهینه‌شده برای بازی آنلاین، یوتیوب، اینستاگرام و ترید"
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Subscription Plans
        Text(
            text = "پلن‌های اشتراک",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        VpnStateManager.subscriptionPlans.forEach { plan ->
            PlanSelectionCard(
                plan = plan,
                isVipActive = isVipActive,
                onBuyClick = { selectedPlanToBuy = plan }
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        Spacer(modifier = Modifier.height(30.dp))
    }

    // Checkout Modal
    selectedPlanToBuy?.let { plan ->
        SubscriptionDialog(
            plan = plan,
            onDismiss = { selectedPlanToBuy = null },
            onSuccessActivated = { activatedPlan ->
                selectedPlanToBuy = null
                showActivationCelebration = true
                onVipActivated(activatedPlan)
            }
        )
    }
}

@Composable
private fun PerkRowItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    desc: String
) {
    val tokens = LocalContrastTokens.current
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = tokens.cardBackground,
        border = BorderStroke(tokens.cardBorderWidth, tokens.cardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFD700).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PlanSelectionCard(
    plan: SubscriptionPlan,
    isVipActive: Boolean,
    onBuyClick: () -> Unit
) {
    val tokens = LocalContrastTokens.current

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (plan.isPopular) Color(0xFFFFD700).copy(alpha = 0.08f) else tokens.cardBackground,
        border = BorderStroke(
            if (plan.isPopular) 2.dp else tokens.cardBorderWidth,
            if (plan.isPopular) Color(0xFFFFD700) else tokens.cardBorder
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("plan_card_${plan.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = plan.titleFa,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (plan.discountBadgeFa != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF00E676))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = plan.discountBadgeFa,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                color = Color.Black
                            )
                        }
                    }
                }

                Text(
                    text = plan.priceUsd,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFFFFD700)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = plan.durationTextFa,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = plan.priceFa,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = tokens.accentGlow
                )

                Button(
                    onClick = onBuyClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (plan.isPopular) Color(0xFFFFD700) else tokens.accentGlow
                    ),
                    modifier = Modifier.height(44.dp).testTag("select_plan_btn_${plan.id}")
                ) {
                    Text(
                        text = if (isVipActive) "تمدید اشتراک" else "انتخاب و خرید",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}
