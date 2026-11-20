package com.smartsolar.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.smartsolar.app.data.remote.NetworkErrorType
import com.smartsolar.app.data.remote.NetworkResult
import com.smartsolar.app.domain.model.EngineerAvailability
import com.smartsolar.app.domain.model.OrderStatus
import com.smartsolar.app.domain.model.TaskStatus
import com.smartsolar.app.ui.theme.SolarAmberAccent
import com.smartsolar.app.ui.theme.SolarAmberSoft
import com.smartsolar.app.ui.theme.SolarBlueInfo
import com.smartsolar.app.ui.theme.SolarBlueSoft
import com.smartsolar.app.ui.theme.SolarBorderLight
import com.smartsolar.app.ui.theme.SolarGrayMuted
import com.smartsolar.app.ui.theme.SolarGraySoft
import com.smartsolar.app.ui.theme.SolarGreenDark
import com.smartsolar.app.ui.theme.SolarGreenLight
import com.smartsolar.app.ui.theme.SolarGreenPrimary
import com.smartsolar.app.ui.theme.SolarRedError
import com.smartsolar.app.ui.theme.SolarRedSoft
import com.smartsolar.app.ui.theme.SolarTextPrimary
import com.smartsolar.app.ui.theme.SolarTextSecondary
import com.smartsolar.app.ui.theme.SolarWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartSolarTopBar(
    title: String,
    onBackClick: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {}
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = SolarTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            if (onBackClick != null) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.testTag("top_bar_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "رجوع",
                        tint = SolarTextPrimary
                    )
                }
            }
        },
        actions = { actions() },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = SolarWhite,
            titleContentColor = SolarTextPrimary
        )
    )
}

@Composable
fun SolarCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable () -> Unit
) {
    val cardModifier = if (onClick != null) {
        modifier.clickable(onClick = onClick)
    } else {
        modifier
    }
    Card(
        modifier = cardModifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SolarWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, SolarBorderLight)
    ) {
        Box(modifier = Modifier.padding(contentPadding)) {
            content()
        }
    }
}

@Composable
fun SolarNetworkImage(
    imageUrl: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    fallbackDrawableRes: Int = R.drawable.img_solar_empty_state,
    contentScale: ContentScale = ContentScale.Crop
) {
    if (imageUrl.isNotBlank()) {
        AsyncImage(
            model = imageUrl,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale,
            placeholder = painterResource(id = fallbackDrawableRes),
            error = painterResource(id = fallbackDrawableRes)
        )
    } else {
        Image(
            painter = painterResource(id = fallbackDrawableRes),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
    }
}

@Composable
fun LoadingStateView(
    message: String = "جاري تحميل البيانات...",
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(color = SolarGreenPrimary)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = SolarTextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun EmptyStateView(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_solar_empty_state),
            contentDescription = title,
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(20.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = SolarTextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = SolarTextSecondary,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onActionClick,
                colors = ButtonDefaults.buttonColors(containerColor = SolarGreenPrimary)
            ) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
fun ErrorStateView(
    error: NetworkResult.Error,
    onRetry: (() -> Unit)? = null,
    onOpenServerSettings: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val icon: ImageVector = when (error.type) {
        NetworkErrorType.NO_INTERNET -> Icons.Default.CloudOff
        NetworkErrorType.SERVER_URL_NOT_CONFIGURED -> Icons.Default.Settings
        else -> Icons.Default.ErrorOutline
    }
    SolarCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(SolarRedSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = "حالة الخطأ",
                    tint = SolarRedError,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = error.arabicMessage,
                style = MaterialTheme.typography.titleSmall,
                color = SolarTextPrimary,
                textAlign = TextAlign.Center
            )
            if (error.phpEndpoint.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "نقطة التكامل: solar-admin/api/${error.phpEndpoint}",
                    style = MaterialTheme.typography.bodySmall,
                    color = SolarTextSecondary,
                    textAlign = TextAlign.Center
                )
            }
            if (error.needsPhpConfirmation) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = SolarAmberSoft,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = SolarAmberAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "تحتاج هذه العملية لتأكيد تنسيق الحقول من ملف PHP الأصلي.",
                            style = MaterialTheme.typography.labelSmall,
                            color = SolarTextPrimary
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onRetry != null) {
                    Button(
                        onClick = onRetry,
                        modifier = Modifier.testTag("error_retry_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = SolarGreenPrimary)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إعادة المحاولة")
                    }
                }
                if (onOpenServerSettings != null) {
                    OutlinedButton(
                        onClick = onOpenServerSettings,
                        modifier = Modifier.testTag("error_server_settings_button")
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إعدادات الخادم")
                    }
                }
            }
        }
    }
}

@Composable
fun InlineResultBanner(
    message: String,
    isSuccess: Boolean,
    modifier: Modifier = Modifier
) {
    val bg = if (isSuccess) SolarGreenLight else SolarRedSoft
    val fg = if (isSuccess) SolarGreenDark else SolarRedError
    val icon = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.ErrorOutline
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = bg,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = fg,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun EngineerAvailabilityBadge(availability: EngineerAvailability) {
    val (bg, fg) = when (availability) {
        EngineerAvailability.AVAILABLE -> SolarGreenLight to SolarGreenDark
        EngineerAvailability.BUSY -> SolarRedSoft to SolarRedError
        EngineerAvailability.UNAVAILABLE -> SolarGraySoft to SolarTextSecondary
    }
    StatusPill(label = availability.arabicLabel, background = bg, foreground = fg)
}

@Composable
fun TaskStatusBadge(status: TaskStatus) {
    val (bg, fg) = when (status) {
        TaskStatus.PENDING -> SolarAmberSoft to SolarAmberAccent
        TaskStatus.ACCEPTED -> SolarBlueSoft to SolarBlueInfo
        TaskStatus.IN_PROGRESS -> SolarGreenLight to SolarGreenDark
        TaskStatus.COMPLETED -> SolarGreenLight to SolarGreenDark
        TaskStatus.CANCELLED -> SolarRedSoft to SolarRedError
    }
    StatusPill(label = status.arabicLabel, background = bg, foreground = fg)
}

@Composable
fun OrderStatusBadge(status: OrderStatus) {
    val (bg, fg) = when (status) {
        OrderStatus.PENDING -> SolarAmberSoft to SolarAmberAccent
        OrderStatus.CONFIRMED, OrderStatus.PROCESSING, OrderStatus.SHIPPED -> SolarBlueSoft to SolarBlueInfo
        OrderStatus.DELIVERED -> SolarGreenLight to SolarGreenDark
        OrderStatus.CANCELLED -> SolarRedSoft to SolarRedError
    }
    StatusPill(label = status.arabicLabel, background = bg, foreground = fg)
}

@Composable
fun StatusPill(
    label: String,
    background: Color,
    foreground: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = background,
        shape = RoundedCornerShape(50)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = foreground,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun RatingStarsDisplay(
    rating: Double,
    count: Int? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        for (i in 1..5) {
            Icon(
                imageVector = if (i <= rating.toInt()) Icons.Default.Star else Icons.Outlined.StarBorder,
                contentDescription = null,
                tint = SolarAmberAccent,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = if (count != null) "${"%.1f".format(rating)} ($count)" else "${"%.1f".format(rating)}",
            style = MaterialTheme.typography.labelMedium,
            color = SolarTextSecondary
        )
    }
}
