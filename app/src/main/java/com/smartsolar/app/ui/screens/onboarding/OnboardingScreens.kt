package com.smartsolar.app.ui.screens.onboarding

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.smartsolar.app.domain.model.ApprovalStatus
import com.smartsolar.app.domain.model.UserSession
import com.smartsolar.app.ui.components.SolarCard
import com.smartsolar.app.ui.theme.SolarAmberSoft
import com.smartsolar.app.ui.theme.SolarBackground
import com.smartsolar.app.ui.theme.SolarGreenDark
import com.smartsolar.app.ui.theme.SolarGreenLight
import com.smartsolar.app.ui.theme.SolarGreenPrimary
import com.smartsolar.app.ui.theme.SolarTextPrimary
import com.smartsolar.app.ui.theme.SolarTextSecondary
import com.smartsolar.app.ui.theme.SolarWhite
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(
    isInitialized: Boolean,
    onTimeout: () -> Unit
) {
    var startAnim by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0.78f,
        animationSpec = tween(durationMillis = 650),
        label = "splash_scale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0f,
        animationSpec = tween(durationMillis = 650),
        label = "splash_alpha"
    )

    LaunchedEffect(Unit) {
        startAnim = true
    }

    LaunchedEffect(isInitialized) {
        if (isInitialized) {
            delay(1300)
            onTimeout()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(SolarWhite, SolarGreenLight.copy(alpha = 0.45f), SolarBackground)
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .scale(scale)
                .alpha(alpha)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_smart_solar_mark),
                contentDescription = "شعار Smart Solar",
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(28.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "SMART SOLAR",
                style = MaterialTheme.typography.displaySmall,
                color = SolarGreenDark,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "الطاقة الشمسية الذكية",
                style = MaterialTheme.typography.headlineSmall,
                color = SolarTextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "منصة متكاملة لمنتجات وخدمات الطاقة الشمسية",
                style = MaterialTheme.typography.bodyMedium,
                color = SolarTextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun WelcomeScreen(
    onStartOnboarding: () -> Unit,
    onGoToLogin: () -> Unit,
    onOpenServerSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SolarBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.widthIn(max = 560.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_smart_solar_mark),
                        contentDescription = "شعار التطبيق",
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "SMART SOLAR",
                            style = MaterialTheme.typography.titleMedium,
                            color = SolarGreenDark
                        )
                        Text(
                            text = "الطاقة الشمسية الذكية",
                            style = MaterialTheme.typography.bodySmall,
                            color = SolarTextSecondary
                        )
                    }
                }
                OutlinedButton(
                    onClick = onOpenServerSettings,
                    modifier = Modifier.testTag("welcome_server_settings_button")
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("الخادم")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            SolarCard(modifier = Modifier.fillMaxWidth()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(id = R.drawable.img_solar_hero_banner),
                        contentDescription = "منزل يعمل بالطاقة الشمسية",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp)
                            .clip(RoundedCornerShape(14.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Surface(
                        color = SolarGreenLight,
                        shape = RoundedCornerShape(50)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.WbSunny,
                                contentDescription = null,
                                tint = SolarGreenDark,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "طاقة نظيفة ومستدامة لمنزلك",
                                style = MaterialTheme.typography.labelMedium,
                                color = SolarGreenDark
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "أهلاً بك في الطاقة الشمسية الذكية",
                        style = MaterialTheme.typography.headlineMedium,
                        color = SolarTextPrimary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "منصة واحدة تجمع العملاء والمهندسين المتخصصين وبائعي معدات الطاقة الشمسية مع حاسبة هندسية دقيقة لاحتياجات منزلك.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = SolarTextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onStartOnboarding,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("welcome_explore_button"),
                colors = ButtonDefaults.buttonColors(containerColor = SolarGreenPrimary),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("جولة تعريفية في المميزات", style = MaterialTheme.typography.labelLarge)
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onGoToLogin,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("welcome_login_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("تسجيل الدخول أو إنشاء حساب", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

private data class OnboardingPageData(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val imageRes: Int
)

@Composable
fun OnboardingPagerScreen(
    onFinished: () -> Unit
) {
    val pages = remember {
        listOf(
            OnboardingPageData(
                title = "اكتشف منتجات الطاقة الشمسية",
                description = "تصفح الألواح الشمسية، البطاريات، الانفرترات، ومنظمات الشحن من بائعين معتمدين مع متابعة التوفر الفوري في المخزون.",
                icon = Icons.Default.ShoppingBag,
                imageRes = R.drawable.img_solar_hero_banner
            ),
            OnboardingPageData(
                title = "احسب احتياجات منزلك من الطاقة",
                description = "أدخل أجهزتك الكهربائية وساعات التشغيل لتقدير قدرة المنظومة وعدد الألواح والبطاريات والانفرتر وفق معادلات هندسية واضحة.",
                icon = Icons.Default.Calculate,
                imageRes = R.drawable.img_solar_calculator_art
            ),
            OnboardingPageData(
                title = "اطلب تركيب منظومتك الشمسية",
                description = "أرسل طلب تركيب متكامل مع موقعك وتفاصيل المنظومة، واختر المهندس المناسب لتنفيذ المشروع باحترافية.",
                icon = Icons.Default.Construction,
                imageRes = R.drawable.img_solar_hero_banner
            ),
            OnboardingPageData(
                title = "احصل على خدمات الصيانة",
                description = "أبلغ عن أي عطل أو انخفاض في كفاءة منظومتك وتابع حالة الصيانة خطوة بخطوة مع تقارير فنية موثقة.",
                icon = Icons.Default.Build,
                imageRes = R.drawable.img_solar_engineer_service
            ),
            OnboardingPageData(
                title = "تواصل مع المهندسين المتخصصين",
                description = "استعرض ملفات المهندسين، سنوات الخبرة، الأعمال السابقة، الشهادات، وتقييمات العملاء قبل اختيارك.",
                icon = Icons.Default.Engineering,
                imageRes = R.drawable.img_solar_engineer_service
            )
        )
    }

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SolarBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${pagerState.currentPage + 1} / ${pages.size}",
                style = MaterialTheme.typography.labelLarge,
                color = SolarTextSecondary
            )
            TextButton(
                onClick = onFinished,
                modifier = Modifier.testTag("onboarding_skip_button")
            ) {
                Text("تخطي", color = SolarGreenDark)
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .widthIn(max = 560.dp)
        ) { index ->
            val page = pages[index]
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                SolarCard(modifier = Modifier.fillMaxWidth()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(
                            painter = painterResource(id = page.imageRes),
                            contentDescription = page.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(14.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(SolarGreenLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = page.icon,
                                contentDescription = null,
                                tint = SolarGreenDark,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = page.title,
                            style = MaterialTheme.typography.headlineMedium,
                            color = SolarTextPrimary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = page.description,
                            style = MaterialTheme.typography.bodyLarge,
                            color = SolarTextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Page Indicators
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(pages.size) { index ->
                val isSelected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .height(8.dp)
                        .width(if (isSelected) 24.dp else 8.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) SolarGreenPrimary else SolarGreenLight)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        val isLastPage = pagerState.currentPage == pages.lastIndex
        Button(
            onClick = {
                if (isLastPage) {
                    onFinished()
                } else {
                    scope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp)
                .height(52.dp)
                .testTag(if (isLastPage) "onboarding_start_button" else "onboarding_next_button"),
            colors = ButtonDefaults.buttonColors(containerColor = SolarGreenPrimary),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                text = if (isLastPage) "البدء" else "التالي",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
fun PendingApprovalScreen(
    session: UserSession,
    onRefreshStatus: () -> Unit,
    onLogout: () -> Unit
) {
    val isRejected = session.approvalStatus == ApprovalStatus.REJECTED
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SolarBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        SolarCard(modifier = Modifier.widthIn(max = 520.dp)) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(SolarAmberSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.HourglassTop,
                        contentDescription = "حالة الموافقة",
                        tint = SolarTextPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (isRejected) "تم رفض طلب الانضمام" else "حالة الطلب: قيد المراجعة",
                    style = MaterialTheme.typography.headlineMedium,
                    color = SolarTextPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = SolarAmberSoft,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = if (isRejected) "حساب مرفوض" else "بانتظار موافقة مدير النظام",
                        style = MaterialTheme.typography.labelLarge,
                        color = SolarTextPrimary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = if (isRejected) {
                        "نأسف، لم تتم الموافقة على تفعيل حساب (${session.role.arabicLabel}) من قبل إدارة المنصة."
                    } else {
                        "مرحباً ${session.fullName.ifBlank { session.email }}! تم إرسال وحفظ طلب تسجيلك بصفتك (${session.role.arabicLabel}) وهو قيد المراجعة حالياً. لا يمكن الدخول إلى لوحة التحكم قبل موافقة مدير النظام."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = SolarTextSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(22.dp))
                Button(
                    onClick = onRefreshStatus,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("pending_refresh_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = SolarGreenPrimary)
                ) {
                    Text("التحقق من حالة الموافقة")
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onLogout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("pending_logout_button")
                ) {
                    Text("تسجيل الخروج والعودة")
                }
            }
        }
    }
}
