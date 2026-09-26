package com.pilltrack.nativeapp

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val profile = LocalStorage.loadProfile(this)
        AppColors.themeMode = profile.themeMode
        val isDark = when (profile.themeMode) {
            "dark" -> true
            "light" -> false
            else -> {
                val nightMask = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
                nightMask == android.content.res.Configuration.UI_MODE_NIGHT_YES
            }
        }
        window.decorView.setBackgroundColor(
            if (isDark) android.graphics.Color.parseColor("#0B0F19")
            else android.graphics.Color.parseColor("#F8FAFC")
        )
        setContent {
            val isThemeDark = AppColors.isDark()
            MaterialTheme(
                colorScheme = if (isThemeDark) {
                    darkColorScheme(
                        primary = Color(0xFF3B82F6),
                        secondary = Color(0xFF38BDF8),
                        background = Color(0xFF0B0F19),
                        surface = Color(0xFF1E293B)
                    )
                } else {
                    lightColorScheme(
                        primary = Color(0xFF2563EB),
                        secondary = Color(0xFF38BDF8),
                        background = Color(0xFFF8FAFC),
                        surface = Color.White
                    )
                }
            ) {
                PillTrackApp()
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PillTrackApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showSplash by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(1200)
        showSplash = false
    }

    var selectedTab by remember { mutableStateOf("today") }
    var detailDate by remember { mutableStateOf<String?>(null) }

    var logs by remember { mutableStateOf(LocalStorage.loadLogs(context)) }
    var userProfile by remember { mutableStateOf(LocalStorage.loadProfile(context)) }
    LaunchedEffect(userProfile.themeMode) {
        AppColors.themeMode = userProfile.themeMode
    }
    var avatarBitmap by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(userProfile.avatarPath) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            avatarBitmap = LocalStorage.loadAvatarBitmap(userProfile.avatarPath)
        }
    }
    var customBgBitmap by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(userProfile.backgroundPath) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            customBgBitmap = LocalStorage.loadBackgroundBitmap(userProfile.backgroundPath)
        }
    }

    var showAddSheet by remember { mutableStateOf(false) }
    var addSheetInitialTime by remember { mutableStateOf<Long?>(null) }
    var showStatsDetail by remember { mutableStateOf(false) }

    var pendingBgBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showBgAdjustSheet by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            scope.launch {
                try {
                    context.contentResolver.openInputStream(selectedUri)?.use { inputStream ->
                        val savedPath = LocalStorage.saveAvatarImage(context, inputStream)
                        val updated = userProfile.copy(avatarPath = savedPath)
                        userProfile = updated
                        avatarBitmap = LocalStorage.loadAvatarBitmap(savedPath)
                        LocalStorage.saveProfile(context, updated)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    val bgPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            scope.launch {
                try {
                    context.contentResolver.openInputStream(selectedUri)?.use { inputStream ->
                        // P1-2: 先读字节再采样解码，4K壁纸不直接进内存
                        val bytes = inputStream.readBytes()
                        val bmp = LocalStorage.decodeSampledBytes(bytes, 1920)
                        if (bmp != null) {
                            pendingBgBitmap = bmp
                            showBgAdjustSheet = true
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    val isOverlayActive = showAddSheet || showStatsDetail || showBgAdjustSheet || detailDate != null
    // FAB is visible ONLY when user is in the "历史" tab! In "今日" and "我的", it sinks and hides!
    val isFabVisible = selectedTab == "history" && !isOverlayActive

    // --- Hierarchical Back Gesture Handlers ---
    // 1. Wallpaper adjustment screen
    BackHandler(enabled = showBgAdjustSheet) {
        showBgAdjustSheet = false
        pendingBgBitmap = null
    }

    // 2. Stats detail sheet
    BackHandler(enabled = !showBgAdjustSheet && showStatsDetail) {
        showStatsDetail = false
    }

    // 3. Add log sheet
    BackHandler(enabled = !showBgAdjustSheet && !showStatsDetail && showAddSheet) {
        showAddSheet = false
        addSheetInitialTime = null
    }

    // 4. Day detail screen
    BackHandler(enabled = !isOverlayActive && detailDate != null) {
        detailDate = null
    }

    // 5. Secondary tabs ("history", "profile") return to "today" tab
    BackHandler(enabled = !isOverlayActive && detailDate == null && selectedTab != "today") {
        selectedTab = "today"
    }

    // 6. Double back to exit when on root "today" tab with no overlays
    var lastBackPressTime by remember { mutableStateOf(0L) }
    var showExitToast by remember { mutableStateOf(false) }
    LaunchedEffect(showExitToast) {
        if (showExitToast) {
            delay(2000L)
            showExitToast = false
        }
    }

    BackHandler(enabled = !isOverlayActive && detailDate == null && selectedTab == "today") {
        val now = System.currentTimeMillis()
        if (now - lastBackPressTime < 2000L) {
            (context as? android.app.Activity)?.finish()
        } else {
            lastBackPressTime = now
            showExitToast = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
    ) {
        Scaffold(
            contentColor = AppColors.TextPrimary,
            containerColor = Color.Transparent
        ) { _ ->
            Box(modifier = Modifier.fillMaxSize()) {
                BackgroundGlow(
                    customBitmap = customBgBitmap,
                    bgAlpha = userProfile.backgroundAlpha,
                    bgScale = userProfile.backgroundScale,
                    bgRotation = userProfile.backgroundRotation,
                    bgOffsetX = userProfile.backgroundOffsetX,
                    bgOffsetY = userProfile.backgroundOffsetY
                )

                // Tab Content with Directional Horizontal Slide Animation
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        val tabOrder = mapOf("today" to 0, "history" to 1, "profile" to 2)
                        val fromIndex = tabOrder[initialState] ?: 0
                        val toIndex = tabOrder[targetState] ?: 0
                        // Using a fluid spring animation for tab switches instead of basic tween
                        val slideSpec = spring<androidx.compose.ui.unit.IntOffset>(
                            dampingRatio = 0.8f,
                            stiffness = 300f
                        )
                        val fadeSpec = spring<Float>(dampingRatio = 0.8f, stiffness = 300f)

                        if (toIndex > fromIndex) {
                            (slideInHorizontally(animationSpec = slideSpec) { it } + fadeIn(animationSpec = fadeSpec)) with
                                (slideOutHorizontally(animationSpec = slideSpec) { -it / 3 } + fadeOut(animationSpec = fadeSpec))
                        } else {
                            (slideInHorizontally(animationSpec = slideSpec) { -it } + fadeIn(animationSpec = fadeSpec)) with
                                (slideOutHorizontally(animationSpec = slideSpec) { it / 3 } + fadeOut(animationSpec = fadeSpec))
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                ) { tab ->
                    when (tab) {
                        "today" -> {
                            TodayScreen(
                                logs = logs,
                                userProfile = userProfile,
                                avatarBitmap = avatarBitmap,
                                onAvatarClick = { photoPickerLauncher.launch("image/*") },
                                onDeleteLog = { logToDelete ->
                                    val newLogs = logs.filter { it.time != logToDelete.time }
                                    logs = newLogs
                                    LocalStorage.saveLogs(context, newLogs)
                                    PillTrackWidgetProvider.updateAllWidgets(context)
                                }
                            )
                        }
                        "history" -> {
                            AnimatedContent(
                                targetState = detailDate,
                                transitionSpec = {
                                    if (targetState != null) {
                                        (slideInHorizontally(animationSpec = tween(320, easing = FastOutSlowInEasing)) { it } + fadeIn(tween(250))) with
                                            (slideOutHorizontally(animationSpec = tween(320, easing = FastOutSlowInEasing)) { -it / 3 } + fadeOut(tween(200)))
                                    } else {
                                        (slideInHorizontally(animationSpec = tween(320, easing = FastOutSlowInEasing)) { -it / 3 } + fadeIn(tween(250))) with
                                            (slideOutHorizontally(animationSpec = tween(320, easing = FastOutSlowInEasing)) { it } + fadeOut(tween(200)))
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            ) { currentDetailDate ->
                                if (currentDetailDate == null) {
                                    HistoryScreen(
                                        logs = logs,
                                        onDateClick = { date -> detailDate = date },
                                        onAddLogClick = {
                                            addSheetInitialTime = System.currentTimeMillis()
                                            showAddSheet = true
                                        }
                                    )
                                } else {
                                    DetailScreen(
                                        dateStr = currentDetailDate,
                                        logs = logs,
                                        onAddLogForDate = {
                                            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                            val targetTime = try {
                                                sdf.parse(currentDetailDate)?.let { d ->
                                                    val now = Calendar.getInstance()
                                                    Calendar.getInstance().apply {
                                                        time = d
                                                        set(Calendar.HOUR_OF_DAY, now.get(Calendar.HOUR_OF_DAY))
                                                        set(Calendar.MINUTE, now.get(Calendar.MINUTE))
                                                    }.timeInMillis
                                                } ?: System.currentTimeMillis()
                                            } catch (e: Exception) {
                                                System.currentTimeMillis()
                                            }
                                            addSheetInitialTime = targetTime
                                            showAddSheet = true
                                        },
                                        onUpdateLog = { oldLog, newLog ->
                                            val index = logs.indexOfFirst { it.time == oldLog.time }
                                            if (index != -1) {
                                                val updatedList = logs.toMutableList()
                                                updatedList[index] = newLog
                                                logs = updatedList
                                                LocalStorage.saveLogs(context, updatedList)
                                                PillTrackWidgetProvider.updateAllWidgets(context)
                                            }
                                        },
                                        onDeleteLog = { logToDelete ->
                                            val newLogs = logs.filter { it.time != logToDelete.time }
                                            logs = newLogs
                                            LocalStorage.saveLogs(context, newLogs)
                                            PillTrackWidgetProvider.updateAllWidgets(context)
                                        },
                                        onBack = { detailDate = null }
                                    )
                                }
                            }
                        }
                        "profile" -> {
                            ProfileScreen(
                                userProfile = userProfile,
                                logs = logs,
                                avatarBitmap = avatarBitmap,
                                customBgBitmap = customBgBitmap,
                                onAvatarClick = { photoPickerLauncher.launch("image/*") },
                                onSelectBackground = { bgPickerLauncher.launch("image/*") },
                                onAdjustBackground = {
                                    if (customBgBitmap != null) {
                                        pendingBgBitmap = customBgBitmap
                                        showBgAdjustSheet = true
                                    }
                                },
                                onResetBackground = {
                                    LocalStorage.deleteBackgroundImage(context)
                                    val updated = userProfile.copy(backgroundPath = null)
                                    userProfile = updated
                                    customBgBitmap = null
                                    LocalStorage.saveProfile(context, updated)
                                },
                                onUpdateProfile = { updated ->
                                    userProfile = updated
                                    AppColors.themeMode = updated.themeMode
                                    LocalStorage.saveProfile(context, updated)
                                },
                                onShowStatsDetail = { showStatsDetail = true }
                            )
                        }
                    }
                }

                // Hidden FAB that pops up from behind the Bottom Navigation Bar
                AnimatedVisibility(
                    visible = isFabVisible,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(400, easing = FastOutSlowInEasing)
                    ) + fadeIn(tween(300, easing = LinearEasing)),
                    exit = slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(300, easing = FastOutLinearInEasing)
                    ) + fadeOut(tween(250, easing = LinearEasing)),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 100.dp, end = 24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            Color(0xFF2563EB).copy(alpha = 0.35f),
                                            Color.Transparent
                                        )
                                    ),
                                    shape = CircleShape
                                )
                                .scale(1.8f)
                        )
                        FloatingActionButton(
                            onClick = {
                                addSheetInitialTime = System.currentTimeMillis()
                                showAddSheet = true
                            },
                            containerColor = Color(0xFF2563EB),
                            contentColor = Color.White,
                            shape = CircleShape,
                            modifier = Modifier.size(56.dp),
                            elevation = FloatingActionButtonDefaults.elevation(8.dp)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "添加", modifier = Modifier.size(28.dp))
                        }
                    }
                }

                // Bottom Navigation Bar with Apple-style Frosted Glass
                AnimatedVisibility(
                    visible = !isOverlayActive,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = spring(stiffness = 300f, dampingRatio = 0.7f)
                    ) + fadeIn(spring(stiffness = 300f)),
                    exit = slideOutVertically(targetOffsetY = { it }, animationSpec = spring(stiffness = 400f, dampingRatio = 0.8f)) + fadeOut(spring(stiffness = 400f)),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp)
                ) {
                    val isDarkNav = AppColors.isDark()
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth(0.88f)
                            .height(72.dp),
                        shape = RoundedCornerShape(36.dp),
                        color = if (isDarkNav) Color(0xFF0F172A).copy(alpha = 0.45f) else Color.White.copy(alpha = 0.55f),
                        contentColor = AppColors.TextPrimary,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isDarkNav) Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.35f),
                                    Color.White.copy(alpha = 0.05f)
                                )
                            ) else Brush.linearGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.9f),
                                    Color.White.copy(alpha = 0.2f),
                                    Color.White.copy(alpha = 0.7f)
                                )
                            )
                        ),
                        shadowElevation = if (isDarkNav) 0.dp else 24.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    if (isDarkNav) Brush.verticalGradient(
                                        listOf(
                                            Color.White.copy(alpha = 0.12f),
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.5f)
                                        )
                                    ) else Brush.verticalGradient(
                                        listOf(
                                            Color.White.copy(alpha = 0.65f),
                                            Color.White.copy(alpha = 0.25f),
                                            Color.White.copy(alpha = 0.55f)
                                        )
                                    )
                                ),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val haptic = LocalHapticFeedback.current
                            NavItem(
                                icon = Icons.Filled.Home,
                                label = "今日",
                                isSelected = selectedTab == "today",
                                modifier = Modifier.weight(1f)
                            ) {
                                if (selectedTab != "today") haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedTab = "today"
                            }
                            NavItem(
                                icon = Icons.Filled.Schedule,
                                label = "记录",
                                isSelected = selectedTab == "history",
                                modifier = Modifier.weight(1f)
                            ) {
                                if (selectedTab != "history") haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedTab = "history"
                            }
                            NavItem(
                                icon = Icons.Filled.Person,
                                label = "我的",
                                isSelected = selectedTab == "profile",
                                modifier = Modifier.weight(1f)
                            ) {
                                if (selectedTab != "profile") haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedTab = "profile"
                            }
                        }
                    }
                }
            }

            // Stats Detail Modal Sheet (Root Level)
            AnimatedVisibility(
                visible = showStatsDetail,
                enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(300, easing = FastOutSlowInEasing)) + fadeIn(tween(300, easing = LinearEasing)),
                exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300, easing = FastOutSlowInEasing)) + fadeOut(tween(300, easing = LinearEasing)),
                modifier = Modifier.fillMaxSize()
            ) {
                StatsDetailSheet(
                    logs = logs,
                    onDismiss = { showStatsDetail = false }
                )
            }

            // Wallpaper Adjust & Preview Interactive Studio (Root Level)
            AnimatedVisibility(
                visible = showBgAdjustSheet && pendingBgBitmap != null,
                enter = fadeIn(tween(250)),
                exit = fadeOut(tween(250)),
                modifier = Modifier.fillMaxSize()
            ) {
                pendingBgBitmap?.let { bmp ->
                    WallpaperAdjustScreen(
                        bitmap = bmp,
                        initialAlpha = userProfile.backgroundAlpha,
                        initialScale = userProfile.backgroundScale,
                        initialRotation = userProfile.backgroundRotation,
                        initialOffsetX = userProfile.backgroundOffsetX,
                        initialOffsetY = userProfile.backgroundOffsetY,
                        onDismiss = {
                            showBgAdjustSheet = false
                            pendingBgBitmap = null
                        },
                        onApply = { alpha, scale, rotation, offsetX, offsetY ->
                            scope.launch {
                                val bgFile = java.io.File(context.filesDir, "custom_background.jpg")
                                java.io.FileOutputStream(bgFile).use { out ->
                                    bmp.compress(Bitmap.CompressFormat.JPEG, 90, out)
                                }
                                val updated = userProfile.copy(
                                    backgroundPath = bgFile.absolutePath,
                                    backgroundAlpha = alpha,
                                    backgroundScale = scale,
                                    backgroundRotation = rotation,
                                    backgroundOffsetX = offsetX,
                                    backgroundOffsetY = offsetY
                                )
                                userProfile = updated
                                customBgBitmap = bmp
                                LocalStorage.saveProfile(context, updated)
                                showBgAdjustSheet = false
                                pendingBgBitmap = null
                            }
                        }
                    )
                }
            }

            // Add Log Modal Sheet (Root Level)
            AnimatedVisibility(
                visible = showAddSheet,
                enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(300, easing = FastOutSlowInEasing)) + fadeIn(tween(300, easing = LinearEasing)),
                exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300, easing = FastOutSlowInEasing)) + fadeOut(tween(300, easing = LinearEasing)),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.40f))
                        .pointerInput(Unit) {
                            detectTapGestures {
                                showAddSheet = false
                                addSheetInitialTime = null
                            }
                        },
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(Unit) { detectTapGestures { } }, // consume click
                        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                        color = AppColors.Card,
                        border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.Border),
                        shadowElevation = 24.dp
                    ) {
                        AddLogSheet(
                            initialTimeMillis = addSheetInitialTime ?: System.currentTimeMillis(),
                            onDismiss = {
                                showAddSheet = false
                                addSheetInitialTime = null
                            },
                            onSave = { name, dose, stomach, timeMillis, colorHex ->
                                scope.launch {
                                    val medInfo = WikipediaApi.searchMedicationLocal(name)
                                    val parsedDose = if (dose.isBlank()) medInfo.standardDose else dose.replace("[^0-9.]".toRegex(), "").toFloatOrNull() ?: medInfo.standardDose
                                    val newLog = PillLog(
                                        name = name,
                                        dose = dose.ifBlank { "${medInfo.standardDose}${medInfo.unit}" },
                                        time = timeMillis,
                                        color = colorHex,
                                        stomach = stomach,
                                        parsedDose = parsedDose,
                                        halfLife = medInfo.halfLifeHours,
                                        foodFactor = medInfo.foodEffectKaMultiplier
                                    )
                                    val newLogs = logs + newLog
                                    logs = newLogs
                                    LocalStorage.saveLogs(context, newLogs)
                                    PillTrackWidgetProvider.updateAllWidgets(context)

                                    // Also deduct from inventory!
                                    LocalStorage.deductInventory(context, name, dose)

                                    showAddSheet = false
                                    addSheetInitialTime = null
                                }
                            }
                        )
                    }
                }
            }
        }

        // Custom App-level Toast for Exit Warning
        AnimatedVisibility(
            visible = showExitToast,
            enter = slideInVertically(initialOffsetY = { 50 }, animationSpec = spring(stiffness = Spring.StiffnessLow)) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { 50 }, animationSpec = tween(250)) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 120.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF1E293B).copy(alpha = 0.9f),
                shadowElevation = 8.dp
            ) {
                Text(
                    text = "再按一次返回键退出应用",
                    color = Color.White,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }

        // App Launch Splash Animation Overlay
        AnimatedVisibility(
            visible = showSplash,
            enter = fadeIn(tween(100)),
            exit = fadeOut(animationSpec = tween(500)) + slideOutVertically(animationSpec = tween(500)) { -it / 4 }
        ) {
            SplashScreen()
        }
    }
}

@Composable
fun SplashScreen(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "SplashTransitions")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val auraAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "auraAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        AppColors.Background,
                        Color(0xFFEFF6FF),
                        Color(0xFFEEF2FF)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Modern Glassmorphism Capsule Logo
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .scale(pulseScale),
                contentAlignment = Alignment.Center
            ) {
                // Glow behind the logo
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    Color(0xFF38BDF8).copy(alpha = auraAlpha * 0.8f),
                                    Color(0xFF8B5CF6).copy(alpha = auraAlpha * 0.4f),
                                    Color.Transparent
                                )
                            ),
                            CircleShape
                        )
                )

                // The Logo Icon itself
                Canvas(modifier = Modifier.size(80.dp)) {
                    val w = size.width
                    val h = size.height

                    drawContext.canvas.nativeCanvas.save()
                    val cx = w / 2f
                    val cy = h / 2f
                    drawContext.canvas.nativeCanvas.translate(cx, cy)
                    drawContext.canvas.nativeCanvas.rotate(45f)
                    drawContext.canvas.nativeCanvas.translate(-cx, -cy)

                    val capsuleWidth = w * 0.45f
                    val capsuleHeight = h * 0.9f
                    val left = (w - capsuleWidth) / 2f
                    val top = (h - capsuleHeight) / 2f

                    // Top half (purple/pink)
                    val topRect = androidx.compose.ui.geometry.Rect(left, top, left + capsuleWidth, top + capsuleHeight / 2f)
                    val topPath = androidx.compose.ui.graphics.Path().apply {
                        addRoundRect(
                            androidx.compose.ui.geometry.RoundRect(
                                topRect,
                                topLeft = androidx.compose.ui.geometry.CornerRadius(capsuleWidth / 2f),
                                topRight = androidx.compose.ui.geometry.CornerRadius(capsuleWidth / 2f)
                            )
                        )
                    }
                    drawPath(
                        path = topPath,
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFFC084FC), Color(0xFF8B5CF6)),
                            start = Offset(left, top),
                            end = Offset(left + capsuleWidth, top + capsuleHeight / 2f)
                        )
                    )

                    // Bottom half (cyan/blue)
                    val bottomRect = androidx.compose.ui.geometry.Rect(left, top + capsuleHeight / 2f, left + capsuleWidth, top + capsuleHeight)
                    val bottomPath = androidx.compose.ui.graphics.Path().apply {
                        addRoundRect(
                            androidx.compose.ui.geometry.RoundRect(
                                bottomRect,
                                bottomLeft = androidx.compose.ui.geometry.CornerRadius(capsuleWidth / 2f),
                                bottomRight = androidx.compose.ui.geometry.CornerRadius(capsuleWidth / 2f)
                            )
                        )
                    }
                    drawPath(
                        path = bottomPath,
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF38BDF8), Color(0xFF2563EB)),
                            start = Offset(left, top + capsuleHeight / 2f),
                            end = Offset(left + capsuleWidth, top + capsuleHeight)
                        )
                    )
                    
                    // Highlight reflection (Glass effect)
                    val highlightPath = androidx.compose.ui.graphics.Path().apply {
                        addRoundRect(
                            androidx.compose.ui.geometry.RoundRect(
                                left = left + 4.dp.toPx(),
                                top = top + 4.dp.toPx(),
                                right = left + capsuleWidth * 0.4f,
                                bottom = top + capsuleHeight - 8.dp.toPx(),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(capsuleWidth / 3f)
                            )
                        )
                    }
                    drawPath(
                        path = highlightPath,
                        brush = Brush.linearGradient(
                            colors = listOf(Color.White.copy(alpha = 0.5f), Color.Transparent),
                            start = Offset(left, top),
                            end = Offset(left, top + capsuleHeight)
                        )
                    )

                    drawContext.canvas.nativeCanvas.restore()
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                "个人药品记录",
                color = Color(0xFF0F172A),
                fontSize = 25.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                "智能追踪 · 科学管理用药健康",
                color = Color(0xFF64748B),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(18.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = AppColors.Card.copy(alpha = 0.95f),
                border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.Border),
                shadowElevation = 2.dp
            ) {
                Text(
                    "v1.29",
                    color = AppColors.Primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                )
            }
        }
    }
}

@Composable
fun NavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.15f else 1.0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "NavItemScale"
    )
    val contentColor = if (isSelected) Color(0xFF2563EB) else if (AppColors.isDark()) Color(0xFF94A3B8) else Color(0xFF64748B)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(36.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .scale(scale)
                .background(
                    if (isSelected) {
                        if (AppColors.isDark()) Color(0xFF1E3A8A).copy(alpha = 0.5f) else Color(0xFFEFF6FF).copy(alpha = 0.8f)
                    } else Color.Transparent,
                    RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = label,
                color = contentColor,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}



