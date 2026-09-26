@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.pilltrack.nativeapp

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.exp
import androidx.compose.foundation.isSystemInDarkTheme

object AppColors {
    var themeMode by mutableStateOf("system")

    @Composable
    fun isDark(): Boolean = when (themeMode) {
        "dark", "amoled" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }

    val TextPrimary @Composable get() = if (isDark()) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    val TextSecondary @Composable get() = if (isDark()) Color(0xFF94A3B8) else Color(0xFF64748B)
    val TextTertiary @Composable get() = if (isDark()) Color(0xFF64748B) else Color(0xFF94A3B8)
    val Background @Composable get() = if (themeMode == "amoled") Color(0xFF000000) else if (isDark()) Color(0xFF0B0F19) else Color(0xFFF8FAFC)
    val Border @Composable get() = if (themeMode == "amoled") Color(0xFF1F1F1F) else if (isDark()) Color(0xFF334155) else Color(0xFFE2E8F0)
    val SurfaceVariant @Composable get() = if (themeMode == "amoled") Color(0xFF121212) else if (isDark()) Color(0xFF1E293B) else Color(0xFFF1F5F9)
    val Card @Composable get() = if (themeMode == "amoled") Color(0xFF0A0A0A) else if (isDark()) Color(0xFF1E293B) else Color.White
    val Primary @Composable get() = if (isDark()) Color(0xFF3B82F6) else Color(0xFF2563EB)
    val PrimaryContainer @Composable get() = if (isDark()) Color(0xFF1E3A8A) else Color(0xFFEFF6FF)
}
val COLOR_OPTIONS = listOf(
    Pair("#3B82F6", "科技蓝"),
    Pair("#8B5CF6", "紫罗兰"),
    Pair("#EC4899", "玫瑰粉"),
    Pair("#F59E0B", "琥珀金"),
    Pair("#10B981", "翡翠绿"),
    Pair("#06B6D4", "青空蓝"),
    Pair("#EF4444", "珊瑚红")
)

@Composable
fun BackgroundGlow(
    customBitmap: Bitmap? = null,
    bgAlpha: Float = 1.0f,
    bgScale: Float = 1.0f,
    bgRotation: Float = 0f,
    bgOffsetX: Float = 0f,
    bgOffsetY: Float = 0f
) {
    val infiniteTransition = rememberInfiniteTransition(label = "BackgroundGlowTransition")

    val animOffset1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(16000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "animOffset1"
    )

    val animOffset2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(22000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "animOffset2"
    )

    val animScale by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(18000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "animScale"
    )

    val isDark = AppColors.isDark()
    val dstRect = remember { android.graphics.RectF() }
    val bmpPaint = remember { android.graphics.Paint().apply { isFilterBitmap = true } }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                // Isolate the complex background drawing to its own render node to prevent layout scroll invalidation
                clip = true
            }
    ) {
        val w = size.width
        val h = size.height

        // Always draw solid base background color first to prevent any transparency leaking through to window
        val baseColor = if (AppColors.themeMode == "amoled") Color(0xFF000000) else if (isDark) Color(0xFF0B0F19) else Color(0xFFF8FAFC)
        drawRect(color = baseColor)

        if (customBitmap != null) {
            val bmpW = customBitmap.width.toFloat()
            val bmpH = customBitmap.height.toFloat()
            val baseScale = kotlin.math.max(w / bmpW, h / bmpH)
            val scaledW = bmpW * baseScale
            val scaledH = bmpH * baseScale
            val left = (w - scaledW) / 2f
            val top = (h - scaledH) / 2f

            val cx = w / 2f
            val cy = h / 2f

            drawContext.canvas.nativeCanvas.save()
            drawContext.canvas.nativeCanvas.translate(cx + bgOffsetX, cy + bgOffsetY)
            drawContext.canvas.nativeCanvas.rotate(bgRotation)
            drawContext.canvas.nativeCanvas.scale(bgScale, bgScale)
            drawContext.canvas.nativeCanvas.translate(-cx, -cy)

            dstRect.set(left, top, left + scaledW, top + scaledH)
            bmpPaint.alpha = (bgAlpha.coerceIn(0.05f, 1.0f) * 255).toInt()

            drawContext.canvas.nativeCanvas.drawBitmap(customBitmap, null, dstRect, bmpPaint)
            drawContext.canvas.nativeCanvas.restore()

            if (isDark) {
                // Dim custom wallpaper in Dark Mode for immersive, readable contrast (78% opacity)
                val dimColor = if (AppColors.themeMode == "amoled") Color(0xFF000000).copy(alpha = 0.85f) else Color(0xFF0B0F19).copy(alpha = 0.78f)
                drawRect(color = dimColor)
            }
        } else {
            if (AppColors.themeMode == "amoled") {
                // Pure black for OLED, no blobs to save maximum battery
                drawRect(color = Color(0xFF000000))
            } else {
                if (isDark) {
                    // Deep dark midnight cosmic base
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF0B0F19),
                                Color(0xFF0F172A),
                                Color(0xFF1E293B)
                            )
                        )
                    )
                } else {
                    // Fresh, bright, energizing morning porcelain base
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFF8FAFC),
                                Color(0xFFF1F5F9),
                                Color(0xFFEFF6FF)
                            )
                        )
                    )
                }

                // Blob 1: Soft Sky Blue (Top-Left floating)
                val x1 = w * (0.08f + 0.16f * animOffset1)
                val y1 = h * (0.16f + 0.12f * (1f - animOffset2))
                val r1 = w * 0.85f * animScale
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF38BDF8).copy(alpha = 0.35f),
                            Color(0xFF60A5FA).copy(alpha = 0.15f),
                            Color.Transparent
                        ),
                        center = Offset(x1, y1),
                        radius = r1
                    ),
                    radius = r1,
                    center = Offset(x1, y1)
                )

                // Blob 2: Pastel Lavender Violet (Mid-Right drifting)
                val x2 = w * (0.88f - 0.18f * animOffset2)
                val y2 = h * (0.45f + 0.15f * animOffset1)
                val r2 = w * 0.88f * (2f - animScale)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFA78BFA).copy(alpha = 0.30f),
                            Color(0xFFC084FC).copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        center = Offset(x2, y2),
                        radius = r2
                    ),
                    radius = r2,
                    center = Offset(x2, y2)
                )

                // Blob 3: Soft Blossom Peach Pink (Bottom-Center warm glow)
                val x3 = w * (0.40f + 0.25f * animOffset2)
                val y3 = h * (0.85f - 0.12f * animOffset1)
                val r3 = w * 0.80f * animScale
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFB7185).copy(alpha = 0.25f),
                            Color(0xFFF472B6).copy(alpha = 0.10f),
                            Color.Transparent
                        ),
                        center = Offset(x3, y3),
                        radius = r3
                    ),
                    radius = r3,
                    center = Offset(x3, y3)
                )

                // Gentle soft vignette
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.Transparent, Color(0xFF94A3B8).copy(alpha = 0.15f)),
                        center = Offset(w * 0.5f, h * 0.5f),
                        radius = w * 0.95f
                    )
                )
            }
        }
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(24.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = AppColors.isDark()
    Surface(
        modifier = modifier.graphicsLayer {
            // Flatten elevation and transparency renders during scrolls
            clip = true
        },
        shape = shape,
        // Solid high-contrast dark surface in Dark Mode so wallpaper colors never wash out the card
        color = if (isDark) Color(0xFF131D30) else Color.White.copy(alpha = 0.88f),
        border = BorderStroke(
            1.dp,
            if (isDark) Brush.verticalGradient(
                listOf(
                    Color(0xFF334155).copy(alpha = 0.60f),
                    Color(0xFF1E293B).copy(alpha = 0.30f)
                )
            ) else Brush.verticalGradient(
                listOf(
                    Color(0xFFE2E8F0),
                    Color(0xFFF1F5F9)
                )
            )
        ),
        shadowElevation = if (isDark) 12.dp else 0.dp
    ) {
        Column(
            modifier = Modifier
                .background(
                    if (isDark) Brush.verticalGradient(
                        listOf(
                            Color(0xFF182236),
                            Color(0xFF101726)
                        )
                    ) else Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.40f),
                            Color.White.copy(alpha = 0.10f)
                        )
                    )
                )
                .padding(16.dp),
            content = content
        )
    }
}

@Composable
fun AvatarView(
    avatarBitmap: Bitmap?,
    size: Dp = 50.dp,
    showCameraBadge: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (avatarBitmap != null) {
            Image(
                bitmap = remember(avatarBitmap) { avatarBitmap.asImageBitmap() },
                contentDescription = "Avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .border(2.dp, Color.White.copy(alpha = 0.35f), CircleShape)
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(Color(0xFF334155).copy(alpha = 0.85f))
                    .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("👩‍⚕️", fontSize = (size.value * 0.5f).sp)
            }
        }

        if (showCameraBadge) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .align(Alignment.BottomEnd)
                    .background(Color(0xFF3B82F6), CircleShape)
                    .border(1.5.dp, Color(0xFF0F172A), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.PhotoCamera,
                    contentDescription = "更换头像",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
fun TodayScreen(
    logs: List<PillLog>,
    userProfile: UserProfile,
    avatarBitmap: Bitmap?,
    onAvatarClick: () -> Unit,
    onDeleteLog: (PillLog) -> Unit
) {
    val todayLogs = remember(logs) {
        val cal = Calendar.getInstance()
        val today = cal.get(Calendar.DAY_OF_YEAR)
        val todayYear = cal.get(Calendar.YEAR)
        logs.filter {
            cal.timeInMillis = it.time
            cal.get(Calendar.DAY_OF_YEAR) == today && cal.get(Calendar.YEAR) == todayYear
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 44.dp, bottom = 120.dp, start = 18.dp, end = 18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "PillTrack",
                            style = MaterialTheme.typography.headlineMedium,
                            color = AppColors.TextPrimary,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp
                        )
                        val dateStr = SimpleDateFormat("MM月dd日 EEEE", Locale.CHINESE).format(Date())
                        Text(dateStr, color = AppColors.TextSecondary, style = MaterialTheme.typography.bodyMedium)
                    }

                    AvatarView(
                        avatarBitmap = avatarBitmap,
                        size = 50.dp,
                        showCameraBadge = false,
                        onClick = onAvatarClick
                    )
                }
            }

            item(key = "theme_card") {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.ShowChart, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("今日药效监控", color = AppColors.TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color(0xFF10B981), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("代谢中", color = Color(0xFF10B981), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(modifier = Modifier.fillMaxWidth().height(210.dp)) {
                        PharmacokineticsChart(logs = todayLogs, modifier = Modifier.fillMaxSize())
                    }

                    if (todayLogs.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        PharmacokineticsClearanceDashboard(logs = todayLogs)
                    }
                }
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Schedule, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("今日记录", style = MaterialTheme.typography.titleMedium, color = AppColors.TextPrimary, fontWeight = FontWeight.Bold)
                }
            }

            if (todayLogs.isEmpty()) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                            Text("暂无记录，点击右下角 '+' 添加", color = AppColors.TextSecondary, fontSize = 14.sp)
                        }
                    }
                }
            } else {
                items(todayLogs.size, key = { todayLogs[it].id }, contentType = { "LogCard" }) { index ->
                    val log = todayLogs[index]
                    LogCard(
                        log = log,
                        onClick = null,
                        onDelete = { onDeleteLog(log) }
                    )
                }
            }
        }
    }
}

@Composable
fun LogCard(log: PillLog, onClick: (() -> Unit)? = null, onDelete: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val pillColor = try {
        Color(android.graphics.Color.parseColor(log.color))
    } catch (e: Exception) {
        Color(0xFF3B82F6)
    }

    val isDark = AppColors.isDark()
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { clip = true }
            .clip(RoundedCornerShape(20.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(20.dp),
        color = if (isDark) Color(0xFF131D30) else Color.White,
        border = BorderStroke(
            1.dp,
            if (isDark) Color(0xFF334155).copy(alpha = 0.50f) else Color(0xFFE2E8F0)
        ),
        // Deep Optimization: Drop expensive runtime shadow calculations for frequently recycled list items
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .background(
                    if (isDark) Brush.verticalGradient(
                        listOf(
                            Color(0xFF182236),
                            Color(0xFF101726)
                        )
                    ) else Brush.verticalGradient(
                        listOf(
                            pillColor.copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    )
                )
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    pillColor.copy(alpha = 0.20f),
                                    pillColor.copy(alpha = 0.08f)
                                )
                            ),
                            RoundedCornerShape(14.dp)
                        )
                        .border(1.dp, pillColor.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Medication,
                        contentDescription = null,
                        tint = pillColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            log.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = AppColors.TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))

                        Box(
                            modifier = Modifier
                                .background(
                                    if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                                    RoundedCornerShape(6.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0),
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (log.stomach == "full") "饱腹" else "空腹",
                                color = if (log.stomach == "full") (if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)) else (if (isDark) Color(0xFF34D399) else Color(0xFF059669)),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "${log.parsedDose.toInt()} mg",
                        color = pillColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(log.time))
                Box(
                    modifier = Modifier
                        .background(
                            if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                            RoundedCornerShape(10.dp)
                        )
                        .border(
                            1.dp,
                            if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0),
                            RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        timeStr,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF334155),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Icon(
                    Icons.Filled.DeleteOutline,
                    contentDescription = "Delete",
                    tint = Color(0xFFEF4444).copy(alpha = 0.8f),
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onDelete()
                        }
                )
            }
        }
    }
}


data class DrugCurveData(
    val drugName: String,
    val color: Color,
    val concentrations: FloatArray,
    val maxDose: Float,
    var touchY: Float? = null,
    var touchVal: Float = 0f
)

@Composable
fun PharmacokineticsChart(logs: List<PillLog>, modifier: Modifier = Modifier) {
    var touchX by remember { mutableStateOf<Float?>(null) }
        var chartScale by remember { mutableStateOf(1f) }
        var chartOffsetX by remember { mutableStateOf(0f) }

    // Animation states
    val drawProgress = remember { Animatable(0f) }

    LaunchedEffect(logs) {
        drawProgress.snapTo(0f)
        if (logs.isNotEmpty()) {
            drawProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing)
            )
        } else {
            drawProgress.snapTo(1f)
        }
    }

    Column(modifier = modifier) {
        val density = androidx.compose.ui.platform.LocalDensity.current
        val labelTextSize = with(density) { 9.sp.toPx() }
        val labelPaint = remember {
            Paint().apply {
                color = android.graphics.Color.parseColor("#94A3B8")
                typeface = Typeface.DEFAULT_BOLD
                isAntiAlias = true
            }
        }
        labelPaint.textSize = labelTextSize

        val timePaint = remember {
            Paint().apply {
                color = android.graphics.Color.WHITE
                typeface = Typeface.DEFAULT_BOLD
                isAntiAlias = true
            }
        }
        val timeTextSize = with(density) { 12.sp.toPx() }
        timePaint.textSize = timeTextSize

        val detailPaint = remember {
            Paint().apply {
                color = android.graphics.Color.parseColor("#E2E8F0")
                typeface = Typeface.DEFAULT
                isAntiAlias = true
            }
        }
        val detailTextSize = with(density) { 11.sp.toPx() }
        detailPaint.textSize = detailTextSize

        val tooltipBgPaint = remember {
            Paint().apply {
                color = android.graphics.Color.parseColor("#E60F172A")
                style = Paint.Style.FILL
                isAntiAlias = true
            }
        }
        
        val tooltipBorderPaint = remember {
            Paint().apply {
                color = android.graphics.Color.parseColor("#4D3B82F6")
                style = Paint.Style.STROKE
                strokeWidth = 2f
                isAntiAlias = true
            }
        }

        val sqPaint = remember { Paint().apply { style = Paint.Style.FILL; isAntiAlias = true } }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = chartScale,
                            scaleY = chartScale,
                            translationX = chartOffsetX,
                            transformOrigin = TransformOrigin(0f, 0.5f)
                        )
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = { offset ->
                                    touchX = offset.x
                                    tryAwaitRelease()
                                },
                                onTap = { offset ->
                                    touchX = offset.x
                                }
                            )
                        }
                        .pointerInput(Unit) {
                            detectTransformGestures { centroid, pan, zoom, rotation ->
                                chartScale = (chartScale * zoom).coerceIn(1f, 5f)
                                chartOffsetX = (chartOffsetX + pan.x * chartScale).coerceIn(-size.width * (chartScale - 1f), 0f)
                            }
                        }
                ) {
                    val w = size.width
                    val h = size.height
                    val chartBottom = h - 24.dp.toPx()

                    // Baseline
                    drawLine(
                        Color.White.copy(alpha = 0.15f),
                        start = Offset(0f, chartBottom),
                        end = Offset(w, chartBottom),
                        strokeWidth = 1.5f
                    )

                    if (logs.isEmpty()) {
                        // Empty placeholder line labels
                        val emptyLabels = listOf("00:00", "03:30", "07:00", "10:30", "14:00", "17:30", "21:00")
                        emptyLabels.forEachIndexed { index, text ->
                            val x = (index.toFloat() / (emptyLabels.size - 1)) * (w - 20.dp.toPx()) + 10.dp.toPx()
                            drawContext.canvas.nativeCanvas.save()
                            drawContext.canvas.nativeCanvas.translate(x - 8.dp.toPx(), h - 2.dp.toPx())
                            drawContext.canvas.nativeCanvas.rotate(-18f)
                            drawContext.canvas.nativeCanvas.drawText(text, 0f, 0f, labelPaint)
                            drawContext.canvas.nativeCanvas.restore()
                        }
                        return@Canvas
                    }

                    // Baseline dotted line
                    val baselinePath = Path().apply {
                        moveTo(0f, chartBottom)
                        lineTo(w, chartBottom)
                    }
                    drawPath(
                        path = baselinePath,
                        color = Color.White.copy(alpha = 0.35f),
                        style = Stroke(
                            width = 3f,
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                        )
                    )

                    // Therapeutic Window Zone (有效治疗浓度参考区间)
                    val windowTop = chartBottom * 0.30f
                    val windowBottom = chartBottom * 0.70f
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF10B981).copy(alpha = 0.08f),
                                Color(0xFF10B981).copy(alpha = 0.02f)
                            ),
                            startY = windowTop,
                            endY = windowBottom
                        ),
                        topLeft = Offset(0f, windowTop),
                        size = androidx.compose.ui.geometry.Size(w, windowBottom - windowTop)
                    )
                    drawLine(
                        color = Color(0xFF10B981).copy(alpha = 0.25f),
                        start = Offset(0f, windowTop),
                        end = Offset(w, windowTop),
                        strokeWidth = 1.5f,
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                    )
                    drawLine(
                        color = Color(0xFF10B981).copy(alpha = 0.15f),
                        start = Offset(0f, windowBottom),
                        end = Offset(w, windowBottom),
                        strokeWidth = 1.5f,
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                    )

                    val earliestLog = logs.minByOrNull { it.time }!!
                    val latestLog = logs.maxByOrNull { it.time }!!
                    val maxHalfLife = logs.maxOfOrNull { it.halfLife } ?: 3.0f

                    // Timeline start: exactly 1 hour before earliest medication intake (avoids empty flat line)
                    val timelineStart = earliestLog.time - 3600000L

                    // Calculate clearance time: 5 half lives ~ 97% metabolized
                    val hoursFromEarliestToLatest = (latestLog.time - earliestLog.time) / 3600000f
                    val clearanceDuration = maxHalfLife * 5.0f
                    val totalHoursNeeded = 1.0f + hoursFromEarliestToLatest + clearanceDuration + 1.0f

                    // Dynamic timeline span
                    val evalHours = when {
                        totalHoursNeeded > 36f -> 48f
                        totalHoursNeeded > 24f -> 36f
                        totalHoursNeeded > 16f -> 24f
                        totalHoursNeeded > 10f -> 16f
                        totalHoursNeeded > 6f -> 12f
                        else -> 8f
                    }

                    // Dynamic X-axis labels starting from 1 hour before ingestion
                    val numLabels = 6
                    val labels = (0 until numLabels).map { index ->
                        val tMillis = timelineStart + ((index.toFloat() / (numLabels - 1)) * evalHours * 3600000L).toLong()
                        val cal = Calendar.getInstance().apply { timeInMillis = tMillis }
                        val calEarliest = Calendar.getInstance().apply { timeInMillis = earliestLog.time }
                        val dayDiff = cal.get(Calendar.DAY_OF_YEAR) - calEarliest.get(Calendar.DAY_OF_YEAR)
                        val prefix = when {
                            dayDiff == 1 -> "次日"
                            dayDiff >= 2 -> "两日后"
                            dayDiff == -1 -> "昨日"
                            else -> ""
                        }
                        prefix + String.format(Locale.getDefault(), "%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
                    }
                labels.forEachIndexed { index, text ->
                    val x = (index.toFloat() / (labels.size - 1)) * (w - 20.dp.toPx()) + 10.dp.toPx()
                    drawContext.canvas.nativeCanvas.save()
                    drawContext.canvas.nativeCanvas.translate(x - 8.dp.toPx(), h - 2.dp.toPx())
                    drawContext.canvas.nativeCanvas.rotate(-18f)
                    drawContext.canvas.nativeCanvas.drawText(text, 0f, 0f, labelPaint)
                    drawContext.canvas.nativeCanvas.restore()
                }

                val drugGroups = logs.groupBy { it.name }
                val numPoints = 400
                val curTouchX = touchX
                val curvesList = mutableListOf<DrugCurveData>()

                // Calculate curve for each distinct medication using user's customized color!
                drugGroups.entries.forEachIndexed { groupIndex, (drugName, medLogs) ->
                    val colorHex = medLogs.first().color
                    val drugColor = try {
                        Color(android.graphics.Color.parseColor(colorHex))
                    } catch (e: Exception) {
                        Color(0xFF3B82F6)
                    }

                    val firstLog = medLogs.first()
                    val ke = 0.693f / firstLog.halfLife
                    val standardKa = 2.0f
                    var maxAbsorptionFactor = 0f
                    var t = 0f
                    while (t <= 24f) {
                        val v = exp(-ke * t) - exp(-standardKa * t)
                        if (v > maxAbsorptionFactor) maxAbsorptionFactor = v
                        t += 0.1f
                    }
                    if (maxAbsorptionFactor <= 0f) maxAbsorptionFactor = 1f

                    val concs = FloatArray(numPoints + 1)

                    for (i in 0..numPoints) {
                        val tHours = (i.toFloat() / numPoints) * evalHours
                        val absoluteTime = timelineStart + (tHours * 60 * 60 * 1000).toLong()

                        var cSum = 0f
                        medLogs.forEach { log ->
                            val hoursPassed = (absoluteTime - log.time) / (1000 * 60 * 60f)
                            if (hoursPassed >= 0 && hoursPassed < (evalHours + 12f)) {
                                val kElim = 0.693f / log.halfLife
                                val foodFactor = if (log.foodFactor in 0.05f..0.95f) log.foodFactor else 0.4f
                                val ka = if (log.stomach == "full") (2.0f * foodFactor) else 2.0f
                                val c = log.parsedDose * (exp(-kElim * hoursPassed) - exp(-ka * hoursPassed)) / maxAbsorptionFactor
                                if (c > 0) cSum += c
                            }
                        }
                        concs[i] = cSum
                    }

                    // Dynamic scaling based on actual max concentration, not just max dose
                    val actualMaxConc = concs.maxOrNull() ?: 100f
                    val drugMaxDose = medLogs.maxOfOrNull { it.parsedDose }?.coerceAtLeast(100f) ?: 100f
                    val suggestedMax = actualMaxConc.coerceAtLeast(drugMaxDose * 0.5f) * 1.2f // Add 20% headroom

                    val path = Path()
                    var touchY: Float? = null
                    var touchVal = 0f

                    // Apply drawing progress animation limit
                    val pointsToDraw = (numPoints * drawProgress.value).toInt().coerceIn(0, numPoints)

                    var prevX = 0f
                    var prevY = 0f

                    for (i in 0..pointsToDraw) {
                        val tHours = (i.toFloat() / numPoints) * evalHours
                        val absoluteTime = timelineStart + (tHours * 60 * 60 * 1000).toLong()

                        val c = concs[i]
                        val x = (i.toFloat() / numPoints) * w
                        val y = chartBottom - ((c / suggestedMax) * (chartBottom - 20.dp.toPx())).coerceIn(0f, chartBottom)

                        if (i == 0) {
                            path.moveTo(x, y)
                        } else {
                            // Smooth bezier curve implementation
                            val controlPointX = (prevX + x) / 2f
                            path.cubicTo(controlPointX, prevY, controlPointX, y, x, y)
                        }

                        prevX = x
                        prevY = y

                        if (curTouchX != null && kotlin.math.abs(x - curTouchX) < (w / numPoints * 1.2f)) {
                            touchY = y
                            touchVal = c
                        }
                    }

                    if (pointsToDraw > 0) {
                        val fillPath = Path().apply {
                            addPath(path)
                            val lastX = (pointsToDraw.toFloat() / numPoints) * w
                            lineTo(lastX, chartBottom)
                            lineTo(0f, chartBottom)
                            close()
                        }

                        // Animated alpha for area fill
                        val fillAlphaFactor = drawProgress.value * drawProgress.value

                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    drugColor.copy(alpha = 0.55f * fillAlphaFactor),
                                    drugColor.copy(alpha = 0.05f * fillAlphaFactor)
                                ),
                                startY = 0f,
                                endY = chartBottom
                            )
                        )

                        // Draw path shadow (glow)
                        drawPath(
                            path = path,
                            color = drugColor.copy(alpha = 0.15f),
                            style = Stroke(
                                width = 8.0f,
                                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                                join = androidx.compose.ui.graphics.StrokeJoin.Round
                            )
                        )

                        drawPath(
                            path = path,
                            color = drugColor,
                            style = Stroke(
                                width = 3.5f,
                                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                                join = androidx.compose.ui.graphics.StrokeJoin.Round
                            )
                        )
                    }

                    curvesList.add(
                        DrugCurveData(
                            drugName = drugName,
                            color = drugColor,
                            concentrations = concs,
                            maxDose = drugMaxDose,
                            touchY = touchY,
                            touchVal = touchVal
                        )
                    )
                }

                // Interactive touch indicator & Multi-drug Tooltip
                if (curTouchX != null && curvesList.isNotEmpty() && drawProgress.value == 1f) {
                    drawLine(
                        color = Color(0xFF93C5FD).copy(alpha = 0.6f),
                        start = Offset(curTouchX, 0f),
                        end = Offset(curTouchX, chartBottom),
                        strokeWidth = 1.5f
                    )

                    curvesList.forEach { curve ->
                        val pY = curve.touchY
                        if (pY != null && curve.touchVal > 0.5f) {
                            drawCircle(
                                color = curve.color.copy(alpha = 0.35f),
                                radius = 12.dp.toPx(),
                                center = Offset(curTouchX, pY)
                            )
                            drawCircle(
                                color = curve.color,
                                radius = 5.dp.toPx(),
                                center = Offset(curTouchX, pY)
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 2.5f.dp.toPx(),
                                center = Offset(curTouchX, pY)
                            )
                        }
                    }

                    val touchHour = (curTouchX / w) * evalHours
                    val touchTimeMillis = timelineStart + (touchHour * 3600000).toLong()
                    val calTouch = Calendar.getInstance().apply { timeInMillis = touchTimeMillis }
                    val calEarliest = Calendar.getInstance().apply { timeInMillis = earliestLog.time }
                    val dayDiff = calTouch.get(Calendar.DAY_OF_YEAR) - calEarliest.get(Calendar.DAY_OF_YEAR)
                    val dayPrefix = when {
                        dayDiff == 1 -> "次日 "
                        dayDiff >= 2 -> "两日后 "
                        dayDiff == -1 -> "昨日 "
                        else -> ""
                    }
                    val timeStr = dayPrefix + String.format(Locale.getDefault(), "%02d:%02d", calTouch.get(Calendar.HOUR_OF_DAY), calTouch.get(Calendar.MINUTE))

                    val activeCurves = curvesList.filter { it.touchVal > 0.1f }.ifEmpty { listOf(curvesList.first()) }
                    var maxLineWidth = timePaint.measureText(timeStr)

                    val lineStrings = activeCurves.map { curve ->
                        val rem = String.format(Locale.getDefault(), "%.1f", curve.touchVal)
                        val perc = if (curve.maxDose > 0) Math.round((curve.touchVal / curve.maxDose) * 100).coerceIn(0, 100) else 0
                        val s = "${curve.drugName}: $rem mg ($perc%)"
                        val wStr = detailPaint.measureText(s) + 16.dp.toPx()
                        if (wStr > maxLineWidth) maxLineWidth = wStr
                        Pair(curve, s)
                    }

                    val tooltipWidth = maxLineWidth + 32.dp.toPx()
                    val lineHeight = 20.dp.toPx()
                    val tooltipHeight = 32.dp.toPx() + (lineStrings.size * lineHeight)

                    // Modern glassmorphism tooltip logic
                    var tooltipX = curTouchX + 12.dp.toPx()
                    // Flip tooltip to the left if it's too close to the right edge
                    if (tooltipX + tooltipWidth > w - 8.dp.toPx()) {
                        tooltipX = curTouchX - tooltipWidth - 12.dp.toPx()
                    }
                    if (tooltipX < 8.dp.toPx()) tooltipX = 8.dp.toPx()

                    // Try to place the tooltip near the highest active touch point,
                    // but clamp it so it doesn't go off-screen
                    val maxTouchY = activeCurves.minOfOrNull { it.touchY ?: 0f } ?: (h / 2f)
                    var tooltipY = maxTouchY - (tooltipHeight / 2f)
                    tooltipY = tooltipY.coerceIn(8.dp.toPx(), chartBottom - tooltipHeight - 8.dp.toPx())

                    val rect = RectF(tooltipX, tooltipY, tooltipX + tooltipWidth, tooltipY + tooltipHeight)

                    // Glass background
                    drawContext.canvas.nativeCanvas.drawRoundRect(rect, 24f, 24f, android.graphics.Paint().apply {
                        color = android.graphics.Color.parseColor("#E60B0F19")
                        setShadowLayer(16f, 0f, 8f, android.graphics.Color.parseColor("#80000000"))
                        isAntiAlias = true
                    })

                    // Subtle border
                    drawContext.canvas.nativeCanvas.drawRoundRect(rect, 24f, 24f, android.graphics.Paint().apply {
                        color = android.graphics.Color.parseColor("#33FFFFFF")
                        style = android.graphics.Paint.Style.STROKE
                        strokeWidth = 2f
                        isAntiAlias = true
                    })

                    // Draw Time header
                    drawContext.canvas.nativeCanvas.drawText(
                        timeStr,
                        tooltipX + 16.dp.toPx(),
                        tooltipY + 24.dp.toPx(),
                        timePaint
                    )

                    // Draw separator line
                    drawContext.canvas.nativeCanvas.drawLine(
                        tooltipX + 16.dp.toPx(),
                        tooltipY + 32.dp.toPx(),
                        tooltipX + tooltipWidth - 16.dp.toPx(),
                        tooltipY + 32.dp.toPx(),
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.parseColor("#33FFFFFF")
                            strokeWidth = 2f
                            isAntiAlias = true
                        }
                    )

                    // Draw drug entries
                    lineStrings.forEachIndexed { idx, pair ->
                        val (curve, str) = pair
                        val yOffset = tooltipY + 48.dp.toPx() + (idx * lineHeight)

                        sqPaint.color = android.graphics.Color.rgb(
                            (curve.color.red * 255).toInt(),
                            (curve.color.green * 255).toInt(),
                            (curve.color.blue * 255).toInt()
                        )
                        val sqX = tooltipX + 16.dp.toPx()
                        val sqY = yOffset - 9.dp.toPx()
                        val sqSize = 8.dp.toPx()

                        // Rounded color indicator
                        drawContext.canvas.nativeCanvas.drawRoundRect(
                            RectF(sqX, sqY, sqX + sqSize, sqY + sqSize),
                            sqSize/2, sqSize/2, sqPaint
                        )

                        drawContext.canvas.nativeCanvas.drawText(
                            str,
                            sqX + sqSize + 8.dp.toPx(),
                            yOffset,
                            detailPaint
                        )
                    }
                }
            }
        }

        // Color Legends below chart for each distinct drug
        if (logs.isNotEmpty()) {
            val distinctDrugs = logs.groupBy { it.name }
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(distinctDrugs.entries.toList(), key = { it.key }, contentType = { "DrugLegend" }) { entry ->
                    val drugName = entry.key
                    val colorHex = entry.value.first().color
                    val color = try {
                        Color(android.graphics.Color.parseColor(colorHex))
                    } catch (e: Exception) {
                        Color(0xFF3B82F6)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(color, RoundedCornerShape(3.dp))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(drugName, color = AppColors.TextTertiary, fontSize = 12.sp)
                    }
                }
            }
            if (logs.any { it.stomach == "full" }) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "💡 饱腹服药减缓胃排空，吸收平稳释放，血药峰值自然削平",
                    color = AppColors.TextSecondary,
                    fontSize = 11.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

@Composable
fun PharmacokineticsClearanceDashboard(logs: List<PillLog>, modifier: Modifier = Modifier) {
    if (logs.isEmpty()) return

    val isDark = AppColors.isDark()
    val currentTime = remember { System.currentTimeMillis() }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    // Group logs by medication name and find the latest dose
    val latestLogsByDrug = remember(logs) {
        logs.groupBy { it.name }.map { (_, drugLogs) ->
            drugLogs.maxByOrNull { it.time }!!
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (AppColors.themeMode == "amoled") Color(0xFF000000) else if (isDark) Color(0xFF0F172A).copy(alpha = 0.65f) else Color(0xFFF8FAFC).copy(alpha = 0.95f))
            .border(1.dp, if (AppColors.themeMode == "amoled") Color(0xFF1F1F1F) else if (isDark) Color(0xFF334155).copy(alpha = 0.60f) else Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("⏳", fontSize = 13.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "体内代谢清空预估",
                    color = AppColors.TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp, 3.dp)
                        .background(Color(0xFF10B981), RoundedCornerShape(2.dp))
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "绿色虚线: 推荐起效区间",
                    color = Color(0xFF10B981),
                    fontSize = 10.sp
                )
            }
        }

        latestLogsByDrug.forEach { log ->
            val halfLife = if (log.halfLife > 0.1f) log.halfLife else 3.0f
            // Full clearance (to <5% peak) is approximately 4.5 half-lives
            val totalClearanceMillis = (halfLife * 4.5f * 60 * 60 * 1000).toLong()
            val clearanceTimeMillis = log.time + totalClearanceMillis
            val elapsedMillis = (currentTime - log.time).coerceAtLeast(0L)
            val remainingMillis = (clearanceTimeMillis - currentTime).coerceAtLeast(0L)
            val progress = (elapsedMillis.toFloat() / totalClearanceMillis.toFloat()).coerceIn(0f, 1f)

            val (statusText, statusColor) = when {
                progress >= 1f -> "✨ 基本清空" to Color(0xFF94A3B8)
                progress >= 0.65f -> "📉 衰减清除期" to Color(0xFFF59E0B)
                progress >= 0.25f -> "⚖️ 稳定起效中" to Color(0xFF10B981)
                else -> "🚀 吸收达峰期" to Color(0xFF38BDF8)
            }

            val remainingHours = remainingMillis / (1000 * 60 * 60)
            val remainingMins = (remainingMillis / (1000 * 60)) % 60
            val clearanceTimeStr = timeFormat.format(Date(clearanceTimeMillis))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (AppColors.themeMode == "amoled") Color(0xFF0A0A0A) else if (isDark) Color(0xFF1E293B).copy(alpha = 0.70f) else Color.White)
                    .border(1.dp, if (AppColors.themeMode == "amoled") Color(0xFF1F1F1F) else if (isDark) Color(0xFF334155).copy(alpha = 0.40f) else Color(0xFFE2E8F0).copy(alpha = 0.80f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${log.name} (${log.dose})",
                        color = AppColors.TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = statusColor.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.35f))
                    ) {
                        Text(
                            statusText,
                            color = statusColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Progress bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (AppColors.themeMode == "amoled") Color(0xFF1A1A1A) else if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF3B82F6), statusColor)
                                )
                            )
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        if (remainingMillis > 0) "预计 ${clearanceTimeStr} 代谢完成" else "已于 ${clearanceTimeStr} 代谢完成",
                        color = AppColors.TextSecondary,
                        fontSize = 10.sp
                    )
                    Text(
                        if (remainingMillis > 0) "还剩 ${remainingHours}h ${remainingMins}m (${(progress * 100).toInt()}% 已代谢)" else "100% 已代谢",
                        color = statusColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun EditLogSheet(
    log: PillLog,
    onDismiss: () -> Unit,
    onSave: (PillLog) -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var medName by remember { mutableStateOf(log.name) }
    var doseStr by remember { mutableStateOf(log.parsedDose.toInt().toString()) }
    var stomachState by remember { mutableStateOf(log.stomach) }
    var selectedColor by remember { mutableStateOf(log.color) }
    var recordTimeMillis by remember { mutableStateOf(log.time) }

    val cal = remember(recordTimeMillis) { Calendar.getInstance().apply { timeInMillis = recordTimeMillis } }
    val dateDisplay = remember(recordTimeMillis) { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(recordTimeMillis)) }
    val timeDisplay = remember(recordTimeMillis) { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(recordTimeMillis)) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.40f))
            .pointerInput(Unit) { detectTapGestures { onDismiss() } },
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) { detectTapGestures { } },
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = AppColors.Card,
            border = BorderStroke(1.dp, AppColors.Border),
            shadowElevation = 24.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .padding(bottom = 24.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .width(44.dp)
                            .height(5.dp)
                            .background(Color(0xFFCBD5E1), RoundedCornerShape(3.dp))
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "编辑用药记录",
                        style = MaterialTheme.typography.titleLarge,
                        color = AppColors.TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Filled.Delete, contentDescription = "删除", tint = Color(0xFFEF4444))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = medName,
                    onValueChange = { medName = it },
                    label = { Text("药物名称", color = AppColors.TextSecondary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = AppColors.TextPrimary,
                        containerColor = AppColors.Background,
                        unfocusedBorderColor = AppColors.Border,
                        focusedBorderColor = Color(0xFF2563EB)
                    ),
                    shape = RoundedCornerShape(16.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = doseStr,
                    onValueChange = { doseStr = it },
                    label = { Text("剂量 (mg)", color = AppColors.TextSecondary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = AppColors.TextPrimary,
                        containerColor = AppColors.Background,
                        unfocusedBorderColor = AppColors.Border,
                        focusedBorderColor = Color(0xFF2563EB)
                    ),
                    shape = RoundedCornerShape(16.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Date and Time picker row
                Text("用药时间", color = AppColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        modifier = Modifier.weight(1.3f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                            DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    val newCal = Calendar.getInstance().apply {
                                        timeInMillis = recordTimeMillis
                                        set(Calendar.YEAR, y)
                                        set(Calendar.MONTH, m)
                                        set(Calendar.DAY_OF_MONTH, d)
                                    }
                                    recordTimeMillis = newCal.timeInMillis
                                },
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.CalendarToday, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(dateDisplay, color = AppColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                            TimePickerDialog(
                                context,
                                { _, hourOfDay, minute ->
                                    val newCal = Calendar.getInstance().apply {
                                        timeInMillis = recordTimeMillis
                                        set(Calendar.HOUR_OF_DAY, hourOfDay)
                                        set(Calendar.MINUTE, minute)
                                    }
                                    recordTimeMillis = newCal.timeInMillis
                                },
                                cal.get(Calendar.HOUR_OF_DAY),
                                cal.get(Calendar.MINUTE),
                                true
                            ).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.AccessTime, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(timeDisplay, color = AppColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Color Selector
                Text("主题颜色", color = AppColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(COLOR_OPTIONS, key = { it.first }, contentType = { "ColorOption" }) { (hex, _) ->
                        val c = Color(android.graphics.Color.parseColor(hex))
                        val isSelected = selectedColor.equals(hex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(c, CircleShape)
                                .border(
                                    if (isSelected) 2.5.dp else 1.dp,
                                    if (isSelected) Color(0xFF0F172A) else Color.Transparent,
                                    CircleShape
                                )
                                .clickable { selectedColor = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("服药状态", color = AppColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    FilterChip(
                        selected = stomachState == "empty",
                        onClick = { stomachState = "empty" },
                        label = { Text("空腹") },
                        modifier = Modifier.weight(1f).height(46.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFD1FAE5),
                            selectedLabelColor = Color(0xFF059669)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = if (stomachState == "empty") Color(0xFF10B981) else Color(0xFFE2E8F0)
                        )
                    )
                    FilterChip(
                        selected = stomachState == "full",
                        onClick = { stomachState = "full" },
                        label = { Text("饱腹") },
                        modifier = Modifier.weight(1f).height(46.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFDBEAFE),
                            selectedLabelColor = Color(0xFF1D4ED8)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = if (stomachState == "full") Color(0xFF3B82F6) else Color(0xFFE2E8F0)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("取消", color = AppColors.TextSecondary)
                    }

                    Button(
                        onClick = {
                            val newDose = doseStr.replace("[^0-9.]".toRegex(), "").toFloatOrNull() ?: log.parsedDose
                            val factor = if (log.foodFactor in 0.05f..0.95f) log.foodFactor else 0.4f
                            val updated = log.copy(
                                name = medName.ifBlank { log.name },
                                parsedDose = newDose,
                                dose = "${newDose.toInt()}mg",
                                stomach = stomachState,
                                color = selectedColor,
                                time = recordTimeMillis,
                                foodFactor = factor
                            )
                            onSave(updated)
                        },
                        modifier = Modifier.weight(1.8f).height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("保存修改", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryScreen(
    logs: List<PillLog>,
    onDateClick: (String) -> Unit,
    onAddLogClick: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        val grouped = remember(logs, searchQuery) {
            val filteredLogs = if (searchQuery.isBlank()) {
                logs
            } else {
                logs.filter { it.name.contains(searchQuery, ignoreCase = true) }
            }

            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            filteredLogs.groupBy {
                sdf.format(Date(it.time))
            }.toSortedMap(reverseOrder())
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 48.dp, bottom = 120.dp, start = 18.dp, end = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    "用药记录",
                    style = MaterialTheme.typography.headlineMedium,
                    color = AppColors.TextPrimary,
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("搜索药品名称...", color = AppColors.TextSecondary) },
                    leadingIcon = {
                        Icon(Icons.Filled.Search, contentDescription = "Search", tint = AppColors.TextSecondary)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            Icon(
                                Icons.Filled.Clear,
                                contentDescription = "Clear",
                                tint = AppColors.TextSecondary,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable { searchQuery = "" }
                            )
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = AppColors.TextPrimary,
                        containerColor = AppColors.SurfaceVariant.copy(alpha = 0.5f),
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = AppColors.Primary
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (grouped.isEmpty()) {
                item(key = "empty_history") {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                            Text("暂无用药记录，点击右下角【+】添加用药", color = AppColors.TextSecondary, fontSize = 13.sp)
                        }
                    }
                }
            } else {
                grouped.forEach { (dateStr, dayLogs) ->
                    item {
                        val isDark = AppColors.isDark()
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { onDateClick(dateStr) },
                            shape = RoundedCornerShape(20.dp),
                            color = if (isDark) Color(0xFF131D30) else Color.White.copy(alpha = 0.88f),
                            border = BorderStroke(
                                1.dp,
                                if (isDark) Brush.verticalGradient(
                                    listOf(
                                        Color(0xFF334155).copy(alpha = 0.60f),
                                        Color(0xFF1E293B).copy(alpha = 0.30f)
                                    )
                                ) else Brush.verticalGradient(
                                    listOf(
                                        Color.White,
                                        Color.White.copy(alpha = 0.60f)
                                    )
                                )
                            ),
                            shadowElevation = if (isDark) 10.dp else 6.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .background(
                                        if (isDark) Brush.verticalGradient(
                                            listOf(
                                                Color(0xFF182236),
                                                Color(0xFF101726)
                                            )
                                        ) else Brush.verticalGradient(
                                            listOf(
                                                Color.White.copy(alpha = 0.40f),
                                                Color.White.copy(alpha = 0.10f)
                                            )
                                        )
                                    )
                                    .padding(20.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        dateStr,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = AppColors.TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("共 ${dayLogs.size} 次用药", color = AppColors.TextSecondary, fontSize = 13.sp)
                                }
                                Icon(Icons.Filled.KeyboardArrowRight, contentDescription = "详情", tint = Color(0xFF64748B))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailScreen(
    dateStr: String,
    logs: List<PillLog>,
    onAddLogForDate: () -> Unit,
    onUpdateLog: (PillLog, PillLog) -> Unit,
    onDeleteLog: (PillLog) -> Unit,
    onBack: () -> Unit
) {
    var editingLog by remember { mutableStateOf<PillLog?>(null) }

    // Intercept back gesture: dismiss editing sheet first if active, otherwise trigger onBack()
    BackHandler(enabled = editingLog != null) {
        editingLog = null
    }
    BackHandler(enabled = editingLog == null) {
        onBack()
    }

    val dayLogs = remember(logs, dateStr) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        logs.filter { sdf.format(Date(it.time)) == dateStr }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 48.dp, bottom = 48.dp, start = 18.dp, end = 18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val haptic = LocalHapticFeedback.current
                        Icon(
                            Icons.Filled.KeyboardArrowLeft,
                            contentDescription = "Back",
                            tint = Color(0xFF3B82F6),
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onBack()
                                }
                                .padding(end = 8.dp)
                        )
                        Text(
                            dateStr,
                            style = MaterialTheme.typography.headlineMedium,
                            color = AppColors.TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Add log for this specific date button!
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onAddLogForDate() },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF2563EB),
                        shadowElevation = 6.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("添加记录", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "当日药效监控",
                        color = AppColors.TextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(210.dp)) {
                        PharmacokineticsChart(logs = dayLogs, modifier = Modifier.fillMaxSize())
                    }

                    if (dayLogs.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        PharmacokineticsClearanceDashboard(logs = dayLogs)
                    }
                }
            }

            if (dayLogs.isEmpty()) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                            Text("当日无用药记录，点击右上角【添加记录】", color = AppColors.TextSecondary, fontSize = 13.sp)
                        }
                    }
                }
            } else {
                items(dayLogs.size, key = { dayLogs[it].id }, contentType = { "LogCard" }) { index ->
                    val log = dayLogs[index]
                    LogCard(
                        log = log,
                        onClick = { editingLog = log },
                        onDelete = { onDeleteLog(log) }
                    )
                }
            }
        }

        if (editingLog != null) {
            EditLogSheet(
                log = editingLog!!,
                onDismiss = { editingLog = null },
                onSave = { updatedLog ->
                    onUpdateLog(editingLog!!, updatedLog)
                    editingLog = null
                },
                onDelete = {
                    onDeleteLog(editingLog!!)
                    editingLog = null
                }
            )
        }
    }
}

@Composable
fun StatsDetailSheet(
    logs: List<PillLog>,
    onDismiss: () -> Unit
) {
    val totalDoses = logs.size
    val drugGroups = logs.groupBy { it.name }
    val distinctDrugCount = drugGroups.size
    val totalMg = logs.sumOf { it.parsedDose.toDouble() }.toFloat()

    // Animating summary stats
    var showStats by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        showStats = true
    }

    val animTotalDoses by animateIntAsState(
        targetValue = if (showStats) totalDoses else 0,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow)
    )
    val animTotalMg by animateFloatAsState(
        targetValue = if (showStats) totalMg else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow)
    )
    val animDistinctDrugCount by animateIntAsState(
        targetValue = if (showStats) distinctDrugCount else 0,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow)
    )

    // Stomach breakdown
    val fullCount = logs.count { it.stomach == "full" }
    val emptyCount = totalDoses - fullCount
    val fullPercent = if (totalDoses > 0) ((fullCount.toFloat() / totalDoses) * 100).toInt() else 0
    val emptyPercent = if (totalDoses > 0) (100 - fullPercent) else 0

    // Time of day breakdown
    val cal = Calendar.getInstance()
    var morningCount = 0   // 06:00 - 12:00
    var afternoonCount = 0 // 12:00 - 18:00
    var eveningCount = 0   // 18:00 - 24:00
    var nightCount = 0     // 00:00 - 06:00

    logs.forEach { log ->
        cal.timeInMillis = log.time
        when (cal.get(Calendar.HOUR_OF_DAY)) {
            in 6..11 -> morningCount++
            in 12..17 -> afternoonCount++
            in 18..23 -> eveningCount++
            else -> nightCount++
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
            .pointerInput(Unit) { detectTapGestures { onDismiss() } },
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .pointerInput(Unit) { detectTapGestures { } }, // absorb click to prevent dismiss
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = AppColors.Background,
            border = BorderStroke(1.dp, AppColors.Border),
            shadowElevation = 24.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                // Drag handle
                Box(
                    modifier = Modifier
                        .size(width = 44.dp, height = 4.dp)
                        .background(Color(0xFFCBD5E1), CircleShape)
                        .align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "📊 数据统计与健康明细",
                            style = MaterialTheme.typography.titleLarge,
                            color = AppColors.TextPrimary,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            "基于历史全部 $totalDoses 次用药记录的多维分析",
                            color = AppColors.TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "关闭", tint = Color(0xFF64748B))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    // Summary Banner
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            color = AppColors.Card,
                            border = BorderStroke(1.dp, AppColors.Border),
                            shadowElevation = 2.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("$animTotalDoses", color = Color(0xFF2563EB), fontSize = 20.sp, fontWeight = FontWeight.Black)
                                    Text("累计用药(次)", color = AppColors.TextSecondary, fontSize = 11.sp)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("${animTotalMg.toInt()} mg", color = Color(0xFF059669), fontSize = 20.sp, fontWeight = FontWeight.Black)
                                    Text("总摄入量", color = AppColors.TextSecondary, fontSize = 11.sp)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("$animDistinctDrugCount", color = Color(0xFF7C3AED), fontSize = 20.sp, fontWeight = FontWeight.Black)
                                    Text("记录药种", color = AppColors.TextSecondary, fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    // Card 1: Drug Breakdown List
                    // Section Title
                    item {
                        Text(
                            "💊 各药品累计用药明细",
                            color = AppColors.TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 4.dp)
                        )
                    }

                    if (drugGroups.isEmpty()) {
                        item {
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Text("暂无药品数据", color = AppColors.TextTertiary, fontSize = 12.sp)
                            }
                        }
                    } else {
                        val sortedDrugs = drugGroups.entries.sortedByDescending { it.value.size }
                        items(sortedDrugs.size, key = { sortedDrugs[it].key }, contentType = { "DrugStatCard" }) { index ->
                            val entry = sortedDrugs[index]
                            val drugName = entry.key
                            val medLogs = entry.value
                            val colorHex = medLogs.first().color
                            val drugColor = try {
                                Color(android.graphics.Color.parseColor(colorHex))
                            } catch (e: Exception) {
                                Color(0xFF3B82F6)
                            }
                            val drugCount = medLogs.size
                            val drugTotalMg = medLogs.sumOf { it.parsedDose.toDouble() }.toInt()
                            val drugPercent = if (totalDoses > 0) drugCount.toFloat() / totalDoses else 0f

                            val latestTime = medLogs.maxOf { it.time }
                            val latestTimeStr = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(latestTime))

                            GlassCard(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .background(drugColor, CircleShape)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(drugName, color = AppColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        }
                                        Text(
                                            "共 $drugCount 次 · 累计 $drugTotalMg mg",
                                            color = AppColors.TextSecondary,
                                            fontSize = 12.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Custom Progress bar
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .background(Color(0xFFE2E8F0), RoundedCornerShape(3.dp))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(drugPercent)
                                                .height(6.dp)
                                                .background(drugColor, RoundedCornerShape(3.dp))
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "最近一次用药: $latestTimeStr (${(drugPercent * 100).toInt()}%)",
                                        color = AppColors.TextSecondary,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }

                    // Card 2: Stomach Habit
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Text("🍽️ 进食与服药习惯", color = AppColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(8.dp).background(Color(0xFF10B981), CircleShape))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("饱腹服药: $fullCount 次 ($fullPercent%)", color = Color(0xFF334155), fontSize = 12.sp)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(8.dp).background(Color(0xFF2563EB), CircleShape))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("空腹服药: $emptyCount 次 ($emptyPercent%)", color = Color(0xFF334155), fontSize = 12.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            ) {
                                if (fullCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .weight(fullCount.toFloat())
                                            .fillMaxHeight()
                                            .background(Color(0xFF10B981))
                                    )
                                }
                                if (emptyCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .weight(emptyCount.toFloat())
                                            .fillMaxHeight()
                                            .background(Color(0xFF3B82F6))
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                "💡 提示：多数对胃肠有刺激的药物（如布洛芬、阿司匹林）建议餐后饱腹服用以减轻胃黏膜负担。",
                                color = AppColors.TextSecondary,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    // Card 3: Time Slot Distribution
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Text("⏰ 服药时段分布", color = AppColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Spacer(modifier = Modifier.height(14.dp))

                            val slots = listOf(
                                Triple("早晨", "06-12h", morningCount),
                                Triple("下午", "12-18h", afternoonCount),
                                Triple("晚间", "18-24h", eveningCount),
                                Triple("夜间", "00-06h", nightCount)
                            )
                            val maxSlotCount = slots.maxOf { it.third }.coerceAtLeast(1)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                slots.forEach { (slotName, slotRange, count) ->
                                    val barHeightRatio = count.toFloat() / maxSlotCount
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Bottom
                                    ) {
                                        Text("$count", color = AppColors.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .width(28.dp)
                                                .height((48 * barHeightRatio).coerceAtLeast(4f).dp)
                                                .background(
                                                    Color(0xFF3B82F6),
                                                    RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                                                )
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(slotName, color = Color(0xFF334155), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                        Text(slotRange, color = AppColors.TextSecondary, fontSize = 9.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Deep Optimization: Extract pure functional components to prevent List-wide recomposition
@Composable
fun ProfileHeaderCard(
    userProfile: UserProfile,
    avatarBitmap: Bitmap?,
    onAvatarClick: () -> Unit,
    onEditProfileClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AvatarView(
            avatarBitmap = avatarBitmap,
            size = 96.dp,
            showCameraBadge = true,
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onAvatarClick()
            }
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onEditProfileClick()
                }
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                userProfile.nickname,
                style = MaterialTheme.typography.titleLarge,
                color = AppColors.TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                Icons.Filled.Edit,
                contentDescription = "编辑资料",
                tint = AppColors.Primary,
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            userProfile.signature,
            color = AppColors.TextSecondary,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun MonthAdherenceCard(monthAdherence: List<Pair<Int, Boolean>>, showStats: Boolean) {
    val completedCount = remember(monthAdherence) { monthAdherence.count { it.second } }
    val daysInMonth = monthAdherence.size
    val animCompletedCount by animateIntAsState(
        targetValue = if (showStats) completedCount else 0,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow)
    )
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("📅 本月用药统计", color = AppColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text("$animCompletedCount/$daysInMonth 天", color = AppColors.Primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        val columns = 7
        val rows = (monthAdherence.size + columns - 1) / columns
        Column(modifier = Modifier.fillMaxWidth()) {
            for (r in 0 until rows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    for (c in 0 until columns) {
                        val index = r * columns + c
                        if (index < monthAdherence.size) {
                            val (day, isChecked) = monthAdherence[index]
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(
                                            if (isChecked) Color(0xFF10B981).copy(alpha = 0.25f)
                                            else AppColors.SurfaceVariant,
                                            CircleShape
                                        )
                                        .border(
                                            1.dp,
                                            if (isChecked) Color(0xFF10B981) else AppColors.Border,
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isChecked) {
                                        Text(day.toString(), color = Color(0xFF10B981), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    } else {
                                        Text(day.toString(), color = AppColors.TextTertiary, fontSize = 12.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
                if (r < rows - 1) Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun OverallStatsCard(logsSize: Int, medTypes: Int, showStats: Boolean, onShowStatsDetail: () -> Unit) {
    val animLogsSize by animateIntAsState(
        targetValue = if (showStats) logsSize else 0,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow)
    )
    val animMedTypes by animateIntAsState(
        targetValue = if (showStats) medTypes else 0,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow)
    )

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable { onShowStatsDetail() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("📊 数据统计与汇总", color = AppColors.TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(AppColors.PrimaryContainer, RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("查看明细", color = AppColors.Primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(2.dp))
                Icon(Icons.Filled.KeyboardArrowRight, contentDescription = "查看明细", tint = AppColors.Primary, modifier = Modifier.size(14.dp))
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$animLogsSize", color = AppColors.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Black)
                Text("总服药记录", color = AppColors.TextSecondary, fontSize = 12.sp)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$animMedTypes", color = AppColors.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Black)
                Text("常备药品", color = AppColors.TextSecondary, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun ThemeSettingsCard(userProfile: UserProfile, onUpdateProfile: (UserProfile) -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(AppColors.PrimaryContainer, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🌓", fontSize = 22.sp)
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text("外观与深色模式", color = AppColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    val currentModeLabel = when (userProfile.themeMode) {
                        "dark" -> "当前为强制深色模式"
                        "amoled" -> "当前为AMOLED纯黑模式"
                        "light" -> "当前为强制浅色模式"
                        else -> "当前跟随手机系统"
                    }
                    Text(
                        currentModeLabel,
                        color = AppColors.TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val modes = listOf(
                Triple("system", "跟随", Icons.Filled.SettingsBrightness),
                Triple("light", "浅色", Icons.Filled.LightMode),
                Triple("dark", "深色", Icons.Filled.DarkMode),
                Triple("amoled", "纯黑", Icons.Filled.Nightlight)
            )
            modes.forEach { (mode, label, icon) ->
                val isSelected = userProfile.themeMode == mode
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            if (!isSelected) {
                                val updated = userProfile.copy(themeMode = mode)
                                AppColors.themeMode = mode
                                onUpdateProfile(updated)
                            }
                        },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) AppColors.Primary else AppColors.Card.copy(alpha = 0.5f),
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) AppColors.Primary else AppColors.Border
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = if (isSelected) Color.White else AppColors.TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            label,
                            color = if (isSelected) Color.White else AppColors.TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WallpaperSettingsCard(
    customBgBitmap: Bitmap?,
    onSelectBackground: () -> Unit,
    onAdjustBackground: () -> Unit,
    onResetBackground: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(AppColors.PrimaryContainer, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🖼️", fontSize = 22.sp)
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text("个性化壁纸设置", color = AppColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        if (customBgBitmap != null) "已启用相册自定义壁纸" else "当前使用默认晨光亮色主题",
                        color = AppColors.TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onSelectBackground,
                modifier = Modifier.weight(1.2f).height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary)
            ) {
                Icon(Icons.Filled.Image, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("从相册导入", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            if (customBgBitmap != null) {
                OutlinedButton(
                    onClick = onAdjustBackground,
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Primary),
                    border = BorderStroke(1.dp, AppColors.Primary)
                ) {
                    Text("调节效果", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onResetBackground,
                    modifier = Modifier.weight(0.9f).height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.TextSecondary),
                    border = BorderStroke(1.dp, AppColors.Border)
                ) {
                    Text("恢复默认", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun ProfileScreen(
    userProfile: UserProfile,
    logs: List<PillLog>,
    avatarBitmap: Bitmap?,
    customBgBitmap: Bitmap? = null,
    onAvatarClick: () -> Unit,
    onSelectBackground: () -> Unit,
    onAdjustBackground: () -> Unit = {},
    onResetBackground: () -> Unit,
    onUpdateProfile: (UserProfile) -> Unit,
    onShowStatsDetail: () -> Unit
) {
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var editNickname by remember { mutableStateOf(userProfile.nickname) }
    var editSignature by remember { mutableStateOf(userProfile.signature) }

    val monthAdherence = remember(logs) { LocalStorage.getMonthAdherence(logs) }
    val medTypes = remember(logs) { logs.map { it.name }.distinct().size }

    // Use a spring physics animation trigger so it doesn't drop frames during quick scrolling
    var showStats by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        showStats = true
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 44.dp, start = 18.dp, end = 18.dp),
            contentPadding = PaddingValues(bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item(key = "header_profile") {
                ProfileHeaderCard(
                    userProfile = userProfile,
                    avatarBitmap = avatarBitmap,
                    onAvatarClick = onAvatarClick,
                    onEditProfileClick = {
                        editNickname = userProfile.nickname
                        editSignature = userProfile.signature
                        showEditProfileDialog = true
                    }
                )
            }

            item(key = "month_adherence_card") {
                MonthAdherenceCard(monthAdherence = monthAdherence, showStats = showStats)
            }

            item(key = "inventory_card") {
                InventoryCard()
            }

            item(key = "stats_card") {
                OverallStatsCard(
                    logsSize = logs.size,
                    medTypes = medTypes,
                    showStats = showStats,
                    onShowStatsDetail = onShowStatsDetail
                )
            }

            item(key = "theme_card") {
                ThemeSettingsCard(userProfile = userProfile, onUpdateProfile = onUpdateProfile)
            }

            item(key = "wallpaper_card") {
                WallpaperSettingsCard(
                    customBgBitmap = customBgBitmap,
                    onSelectBackground = onSelectBackground,
                    onAdjustBackground = onAdjustBackground,
                    onResetBackground = onResetBackground
                )
            }

            // Version info footer
            item(key = "footer") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "个人药品记录",
                        color = AppColors.TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "版本号: v1.29 · Build 31",
                        color = AppColors.TextTertiary,
                        fontSize = 11.sp
                    )
                }
            }
        }

        if (showEditProfileDialog) {
            AlertDialog(
                onDismissRequest = { showEditProfileDialog = false },
                containerColor = AppColors.Card,
                shape = RoundedCornerShape(24.dp),
                title = { Text("修改个人资料", color = AppColors.TextPrimary, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        OutlinedTextField(
                            value = editNickname,
                            onValueChange = { editNickname = it },
                            label = { Text("昵称", color = AppColors.TextSecondary) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                textColor = AppColors.TextPrimary,
                                containerColor = AppColors.Background,
                                unfocusedBorderColor = AppColors.Border,
                                focusedBorderColor = AppColors.Primary
                            ),
                            shape = RoundedCornerShape(14.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = editSignature,
                            onValueChange = { editSignature = it },
                            label = { Text("健康签名", color = AppColors.TextSecondary) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                textColor = AppColors.TextPrimary,
                                containerColor = AppColors.Background,
                                unfocusedBorderColor = AppColors.Border,
                                focusedBorderColor = AppColors.Primary
                            ),
                            shape = RoundedCornerShape(14.dp)
                        )
                    }
                },
                confirmButton = {
                    val haptic = LocalHapticFeedback.current
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val updated = userProfile.copy(
                                nickname = editNickname.ifBlank { userProfile.nickname },
                                signature = editSignature.ifBlank { userProfile.signature }
                            )
                            onUpdateProfile(updated)
                            showEditProfileDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("保存", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditProfileDialog = false }) {
                        Text("取消", color = AppColors.TextSecondary)
                    }
                }
            )
        }
    }
}

@Composable
fun AddLogSheet(
    initialTimeMillis: Long = System.currentTimeMillis(),
    onDismiss: () -> Unit,
    onSave: (name: String, dose: String, stomach: String, timeMillis: Long, colorHex: String) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var medName by remember { mutableStateOf("") }
    var doseStr by remember { mutableStateOf("") }
    var stomachState by remember { mutableStateOf("empty") }
    var selectedColor by remember { mutableStateOf("#3B82F6") }
    var recordTimeMillis by remember { mutableStateOf(initialTimeMillis) }
    var isSearching by remember { mutableStateOf(false) }

    val cal = remember(recordTimeMillis) { Calendar.getInstance().apply { timeInMillis = recordTimeMillis } }
    val dateDisplay = remember(recordTimeMillis) { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(recordTimeMillis)) }
    val timeDisplay = remember(recordTimeMillis) { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(recordTimeMillis)) }

    var customDrugs by remember { mutableStateOf(LocalStorage.loadCustomDrugs(context)) }
    var isManagingDrugs by remember { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            kotlinx.coroutines.delay(2000L)
            toastMessage = null
        }
    }

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .padding(bottom = 24.dp)
        ) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .width(44.dp)
                    .height(5.dp)
                    .background(Color(0xFFCBD5E1), RoundedCornerShape(3.dp))
            )
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text("添加用药记录", style = MaterialTheme.typography.titleLarge, color = AppColors.TextPrimary, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        // Quick common drug chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("快捷选择常用药", color = AppColors.TextSecondary, fontSize = 12.sp)
            Text(
                if (isManagingDrugs) "完成管理" else "管理",
                color = Color(0xFF2563EB),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { isManagingDrugs = !isManagingDrugs }
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(customDrugs.size, contentType = { "CustomDrug" }) { index ->
                val (dName, dDose, dStomach) = customDrugs[index]
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFEFF6FF),
                    border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            if (!isManagingDrugs) {
                                medName = dName
                                doseStr = dDose
                                stomachState = dStomach
                            } else {
                                val newDrugs = customDrugs.filter { it.first != dName || it.second != dDose }
                                customDrugs = newDrugs
                                LocalStorage.saveCustomDrugs(context, newDrugs)
                            }
                        }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                        Text(
                            "$dName ${dDose}mg",
                            color = Color(0xFF1D4ED8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        if (isManagingDrugs) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Filled.Close, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
            if (isManagingDrugs) {
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                if (medName.isNotBlank() && doseStr.isNotBlank()) {
                                    // Prevent exact duplicates from crashing Compose by ensuring it's not already in list
                                    val isDuplicate = customDrugs.any { it.first == medName && it.second == doseStr }
                                    if (!isDuplicate) {
                                        val newDrugs = customDrugs + Triple(medName, doseStr, stomachState)
                                        customDrugs = newDrugs
                                        LocalStorage.saveCustomDrugs(context, newDrugs)
                                        medName = ""
                                        doseStr = ""
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    } else {
                                        toastMessage = "该药品已存在"
                                    }
                                } else {
                                    toastMessage = "请先在下方输入框填写药物名称和剂量"
                                }
                            }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                            Icon(Icons.Filled.Add, contentDescription = "Add", tint = Color(0xFF64748B), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("添加当前输入", color = Color(0xFF64748B), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = medName,
            onValueChange = { medName = it },
            label = { Text("药物名称 (如: 布洛芬)", color = AppColors.TextSecondary) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                textColor = AppColors.TextPrimary,
                containerColor = AppColors.Background,
                unfocusedBorderColor = AppColors.Border,
                focusedBorderColor = Color(0xFF2563EB)
            ),
            shape = RoundedCornerShape(16.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = doseStr,
            onValueChange = { doseStr = it },
            label = { Text("剂量 (如: 400)", color = AppColors.TextSecondary) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                textColor = AppColors.TextPrimary,
                containerColor = AppColors.Background,
                unfocusedBorderColor = AppColors.Border,
                focusedBorderColor = Color(0xFF2563EB)
            ),
            shape = RoundedCornerShape(16.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Date and Time picker section (全局时间与日期选择)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("用药时间与日期", color = AppColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)

            // Quick "现在" button
            Text(
                "设为现在",
                color = Color(0xFF2563EB),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { recordTimeMillis = System.currentTimeMillis() }
                    .padding(4.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // Date Picker trigger
            Surface(
                modifier = Modifier.weight(1.3f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                    DatePickerDialog(
                        context,
                        { _, y, m, d ->
                            val newCal = Calendar.getInstance().apply {
                                timeInMillis = recordTimeMillis
                                set(Calendar.YEAR, y)
                                set(Calendar.MONTH, m)
                                set(Calendar.DAY_OF_MONTH, d)
                            }
                            recordTimeMillis = newCal.timeInMillis
                        },
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH),
                        cal.get(Calendar.DAY_OF_MONTH)
                    ).show()
                },
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF1F5F9),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CalendarToday, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(dateDisplay, color = AppColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }

            // Time Picker trigger
            Surface(
                modifier = Modifier.weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                    TimePickerDialog(
                        context,
                        { _, hourOfDay, minute ->
                            val newCal = Calendar.getInstance().apply {
                                timeInMillis = recordTimeMillis
                                set(Calendar.HOUR_OF_DAY, hourOfDay)
                                set(Calendar.MINUTE, minute)
                            }
                            recordTimeMillis = newCal.timeInMillis
                        },
                        cal.get(Calendar.HOUR_OF_DAY),
                        cal.get(Calendar.MINUTE),
                        true
                    ).show()
                },
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF1F5F9),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.AccessTime, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(timeDisplay, color = AppColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Color Picker (添加药物时可自选颜色)
        Text("标记主题色", color = AppColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(COLOR_OPTIONS, key = { it.first }, contentType = { "ColorOption" }) { (hex, _) ->
                val c = Color(android.graphics.Color.parseColor(hex))
                val isSelected = selectedColor.equals(hex, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(c, CircleShape)
                        .border(
                            if (isSelected) 2.5.dp else 1.dp,
                            if (isSelected) Color(0xFF0F172A) else Color.Transparent,
                            CircleShape
                        )
                        .clickable { selectedColor = hex },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("服药状态", color = AppColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            FilterChip(
                selected = stomachState == "empty",
                onClick = { stomachState = "empty" },
                label = { Text("空腹") },
                modifier = Modifier.weight(1f).height(46.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFFD1FAE5),
                    selectedLabelColor = Color(0xFF059669)
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = if (stomachState == "empty") Color(0xFF10B981) else Color(0xFFE2E8F0)
                )
            )
            FilterChip(
                selected = stomachState == "full",
                onClick = { stomachState = "full" },
                label = { Text("饱腹") },
                modifier = Modifier.weight(1f).height(46.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFFDBEAFE),
                    selectedLabelColor = Color(0xFF1D4ED8)
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = if (stomachState == "full") Color(0xFF3B82F6) else Color(0xFFE2E8F0)
                )
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onDismiss,
                modifier = Modifier.weight(1f).height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("取消", color = AppColors.TextSecondary)
            }
            Button(
                onClick = {
                    if (medName.isNotBlank()) {
                        isSearching = true
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSave(medName, doseStr, stomachState, recordTimeMillis, selectedColor)
                    } else {
                        toastMessage = "药物名称不能为空"
                    }
                },
                modifier = Modifier.weight(1.8f).height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(if (isSearching) "查询中..." else "确认记录", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        }

        // Custom Compose Toast/Snackbar
        AnimatedVisibility(
            visible = toastMessage != null,
            enter = slideInVertically(initialOffsetY = { 50 }, animationSpec = spring(stiffness = Spring.StiffnessLow)) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { 50 }, animationSpec = tween(250)) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 90.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF1E293B).copy(alpha = 0.9f),
                shadowElevation = 8.dp
            ) {
                Text(
                    text = toastMessage ?: "",
                    color = Color.White,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }
    }
}

@Composable
fun InventoryCard() {
    val context = LocalContext.current
    var inventory by remember { mutableStateOf(LocalStorage.loadInventory(context)) }
    var showAddInventoryDialog by remember { mutableStateOf(false) }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(Color(0xFFE0E7FF), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("📦", fontSize = 22.sp)
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text("我的药箱", color = AppColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("库存管理与提醒", color = AppColors.TextSecondary, fontSize = 12.sp)
                }
            }
            IconButton(onClick = { showAddInventoryDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add Inventory", tint = AppColors.Primary)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (inventory.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(AppColors.SurfaceVariant, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("药箱空空如也，点击右上角添加", color = AppColors.TextTertiary, fontSize = 12.sp)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                inventory.forEach { item ->
                    val percent = if (item.totalCapacity > 0) item.quantity.toFloat() / item.totalCapacity else 0f
                    val isLow = item.quantity <= (item.totalCapacity * 0.2f).coerceAtLeast(5f)

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = AppColors.Background,
                        border = BorderStroke(1.dp, if (isLow) Color(0xFFEF4444).copy(alpha = 0.5f) else AppColors.Border)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(item.name, color = AppColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    if (isLow) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFFFEE2E2)
                                        ) {
                                            Text("余量不足", color = Color(0xFFEF4444), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                        }
                                    }
                                }
                                Text("${item.dose}mg", color = AppColors.TextSecondary, fontSize = 11.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("剩余 ${item.quantity} / ${item.totalCapacity}", color = if (isLow) Color(0xFFEF4444) else AppColors.Primary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(modifier = Modifier.width(80.dp).height(4.dp).background(AppColors.SurfaceVariant, RoundedCornerShape(2.dp))) {
                                    Box(modifier = Modifier.fillMaxWidth(percent).height(4.dp).background(if (isLow) Color(0xFFEF4444) else AppColors.Primary, RoundedCornerShape(2.dp)))
                                }
                            }
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "Delete",
                                tint = AppColors.TextTertiary,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        val newInv = inventory.filter { it.id != item.id }
                                        inventory = newInv
                                        LocalStorage.saveInventory(context, newInv)
                                    }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddInventoryDialog) {
        var newInvName by remember { mutableStateOf("") }
        var newInvDose by remember { mutableStateOf("") }
        var newInvQuantity by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddInventoryDialog = false },
            containerColor = AppColors.Card,
            title = { Text("添加药品库存", color = AppColors.TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newInvName,
                        onValueChange = { newInvName = it },
                        label = { Text("药物名称", color = AppColors.TextSecondary) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newInvDose,
                        onValueChange = { newInvDose = it },
                        label = { Text("单次剂量 (mg)", color = AppColors.TextSecondary) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newInvQuantity,
                        onValueChange = { newInvQuantity = it },
                        label = { Text("当前总数量", color = AppColors.TextSecondary) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val q = newInvQuantity.toIntOrNull() ?: 0
                    if (newInvName.isNotBlank() && q > 0) {
                        val newItem = InventoryItem(name = newInvName, dose = newInvDose, quantity = q, totalCapacity = q)
                        val newInv = inventory + newItem
                        inventory = newInv
                        LocalStorage.saveInventory(context, newInv)
                        showAddInventoryDialog = false
                    }
                }) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { showAddInventoryDialog = false }) { Text("取消") }
            }
        )
    }
}

@Composable
fun WallpaperAdjustScreen(
    bitmap: Bitmap,
    initialAlpha: Float = 1.0f,
    initialScale: Float = 1.0f,
    initialRotation: Float = 0f,
    initialOffsetX: Float = 0f,
    initialOffsetY: Float = 0f,
    onDismiss: () -> Unit,
    onApply: (alpha: Float, scale: Float, rotation: Float, offsetX: Float, offsetY: Float) -> Unit
) {
    var alpha by remember { mutableStateOf(initialAlpha) }
    var scale by remember { mutableStateOf(initialScale) }
    var rotation by remember { mutableStateOf(initialRotation) }
    var offsetX by remember { mutableStateOf(initialOffsetX) }
    var offsetY by remember { mutableStateOf(initialOffsetY) }

    var showUiPreview by remember { mutableStateOf(true) }
    var showAlphaControl by remember { mutableStateOf(false) }

    val dstRect = remember { android.graphics.RectF() }
    val bmpPaint = remember { android.graphics.Paint().apply { isFilterBitmap = true } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        // 1. Full-screen Interactive Wallpaper Canvas with pinch-to-zoom & pan gestures
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures(panZoomLock = false) { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.4f, 6.0f)
                        offsetX += pan.x
                        offsetY += pan.y
                    }
                }
        ) {
            val w = size.width
            val h = size.height
            val bmpW = bitmap.width.toFloat()
            val bmpH = bitmap.height.toFloat()
            val baseScale = kotlin.math.max(w / bmpW, h / bmpH)
            val scaledW = bmpW * baseScale
            val scaledH = bmpH * baseScale
            val left = (w - scaledW) / 2f
            val top = (h - scaledH) / 2f

            val cx = w / 2f
            val cy = h / 2f

            drawContext.canvas.nativeCanvas.save()
            drawContext.canvas.nativeCanvas.translate(cx + offsetX, cy + offsetY)
            drawContext.canvas.nativeCanvas.rotate(rotation)
            drawContext.canvas.nativeCanvas.scale(scale, scale)
            drawContext.canvas.nativeCanvas.translate(-cx, -cy)

            dstRect.set(left, top, left + scaledW, top + scaledH)
            bmpPaint.alpha = (alpha.coerceIn(0.05f, 1.0f) * 255).toInt()
            
            drawContext.canvas.nativeCanvas.drawBitmap(bitmap, null, dstRect, bmpPaint)
            drawContext.canvas.nativeCanvas.restore()

            // Pure custom wallpaper: NO white gradient/scrim is drawn on top!
        }

        // 2. Simulated App UI Overlay for live preview (can be toggled)
        if (showUiPreview) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 90.dp, bottom = 140.dp, start = 20.dp, end = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Simulated Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "历史记录",
                        style = MaterialTheme.typography.headlineMedium,
                        color = AppColors.TextPrimary,
                        fontWeight = FontWeight.Black
                    )
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.88f),
                        shadowElevation = 2.dp
                    ) {
                        Text(
                            "UI效果预览中",
                            color = Color(0xFF2563EB),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                // Simulated Record Card 1
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.88f),
                    border = BorderStroke(1.dp, Color.White),
                    shadowElevation = 6.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("2026-09-24", style = MaterialTheme.typography.titleMedium, color = AppColors.TextPrimary, fontWeight = FontWeight.Bold)
                            Text("共 1 次用药", color = AppColors.TextSecondary, fontSize = 13.sp)
                        }
                        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Color(0xFF94A3B8))
                    }
                }

                // Simulated Record Card 2
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.88f),
                    border = BorderStroke(1.dp, Color.White),
                    shadowElevation = 6.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("2026-09-23", style = MaterialTheme.typography.titleMedium, color = AppColors.TextPrimary, fontWeight = FontWeight.Bold)
                            Text("共 2 次用药", color = AppColors.TextSecondary, fontSize = 13.sp)
                        }
                        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Color(0xFF94A3B8))
                    }
                }
            }
        }

        // 3. Top Floating Glass Bar (Instructions & Controls)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 28.dp, start = 16.dp, end = 16.dp)
                .fillMaxWidth()
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = AppColors.TextPrimary.copy(alpha = 0.75f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                shadowElevation = 12.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.ZoomIn, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("双指缩放 · 拖拽平移", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("全屏直接实时预览", color = AppColors.TextTertiary, fontSize = 10.sp)
                        }
                    }

                    // Toggle UI preview button
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (showUiPreview) Color(0xFF2563EB) else Color.White.copy(alpha = 0.15f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showUiPreview = !showUiPreview }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                if (showUiPreview) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                if (showUiPreview) "UI: 开" else "UI: 关",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 4. Bottom Floating Glass Control Bar
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Optional expanded Alpha slider panel
            AnimatedVisibility(
                visible = showAlphaControl,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = AppColors.TextPrimary.copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                    shadowElevation = 10.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("透明度", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("${(alpha * 100).toInt()}%", color = Color(0xFF38BDF8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = alpha,
                            onValueChange = { alpha = it },
                            valueRange = 0.2f..1.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF38BDF8),
                                activeTrackColor = Color(0xFF38BDF8),
                                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.height(26.dp)
                        )
                    }
                }
            }

            // Quick Tool Bar & Main Action Buttons
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = AppColors.TextPrimary.copy(alpha = 0.88f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                shadowElevation = 16.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Quick Tools Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Rotate 90° button
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.12f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { rotation = (rotation + 90f) % 360f }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.RotateRight, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("旋转 90°", color = Color.White, fontSize = 12.sp)
                            }
                        }

                        // Reset button
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.12f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    scale = 1.0f
                                    offsetX = 0f
                                    offsetY = 0f
                                    rotation = 0f
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.RestartAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("复位居中", color = Color.White, fontSize = 12.sp)
                            }
                        }

                        // Alpha slider toggle
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (showAlphaControl) Color(0xFF38BDF8).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.12f),
                            border = BorderStroke(
                                1.dp,
                                if (showAlphaControl) Color(0xFF38BDF8) else Color.Transparent
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showAlphaControl = !showAlphaControl }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Opacity, contentDescription = null, tint = if (showAlphaControl) Color(0xFF38BDF8) else Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("透明度", color = if (showAlphaControl) Color(0xFF38BDF8) else Color.White, fontSize = 12.sp)
                            }
                        }
                    }

                    // Main Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f).height(46.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Card.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("取消", color = Color.White, fontSize = 14.sp)
                        }

                        Button(
                            onClick = { onApply(alpha, scale, rotation, offsetX, offsetY) },
                            modifier = Modifier.weight(1.5f).height(46.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("保存并应用", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

