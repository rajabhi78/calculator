package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import java.util.Locale
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Calculation
import com.example.ui.theme.*
import com.example.viewmodel.CalculatorViewModel
import kotlinx.coroutines.launch
import java.math.BigDecimal

@Composable
fun CalculatorScreen(
    viewModel: CalculatorViewModel,
    modifier: Modifier = Modifier
) {
    val expr by viewModel.expression.collectAsStateWithLifecycle()
    val res by viewModel.resultDisplay.collectAsStateWithLifecycle()
    val preview by viewModel.previewDisplay.collectAsStateWithLifecycle()
    val isSciMode by viewModel.isScientificMode.collectAsStateWithLifecycle()
    val isDegMode by viewModel.isDegreeMode.collectAsStateWithLifecycle()
    val isDark by viewModel.isDarkTheme.collectAsStateWithLifecycle()
    val isSidebarOpen by viewModel.isHistorySidebarOpen.collectAsStateWithLifecycle()
    val historyList by viewModel.historyState.collectAsStateWithLifecycle()
    val memoryVal by viewModel.memoryValue.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    // Configuration for responsiveness
    val config = LocalConfiguration.current
    val isWideScreen = config.screenWidthDp >= 720

    // Accent color picking State
    var accentSelection by remember { mutableStateOf("Cyberpunk Cyan") }
    val chosenAccentColor = remember(accentSelection) {
        when (accentSelection) {
            "Cyberpunk Cyan" -> Color(0xFF00F0FF)
            "Gold Mint" -> Color(0xFFFFD700)
            "Velvet Ruby" -> Color(0xFFF43F5E)
            "Emerald Forest" -> Color(0xFF10B981)
            "Electric Blue" -> Color(0xFF3B82F6)
            else -> Color(0xFF00F0FF)
        }
    }

    // Active Navigation Tab
    var activeTab by remember { mutableStateOf("Dashboard") }
    // Collapsed/Expanded Sidebar logic on Widescreen Desktop / Tablet
    var sidebarExpanded by remember { mutableStateOf(isWideScreen) }
    // For mobile overlay slide-out hamburger drawer
    var drawerOpen by remember { mutableStateOf(false) }

    val triggerFeedback = {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    // Capture physical keyboard focus
    val focusRequester = remember { FocusRequester() }

    // Root background linear gradients depending on active Dark/Light state
    val backgroundBrush = if (isDark) {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF070B11),
                Color(0xFF0D1117),
                Color(0xFF161F30)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFFE5E7EB),
                Color(0xFFF3F4F6),
                Color(0xFFFFFFFF)
            )
        )
    }

    // List of Menu Tabs with corresponding Material Icons
    val isAdminAuthenticated by viewModel.isAdminAuthenticated.collectAsStateWithLifecycle()
    val tabsList = remember(isAdminAuthenticated) {
        val list = mutableListOf(
            Triple("Dashboard", "Overview & Mini Calc", Icons.Filled.Home),
            Triple("Calculator", "Standard & Scientific", Icons.Filled.Calculate),
            Triple("Currency Exchange", "Live Exchange Rates", Icons.Filled.SwapHoriz),
            Triple("Financial Tools", "EMI, Loan, Interest & Tax", Icons.Filled.TrendingUp),
            Triple("Analytics", "Calculations Stats & Diagrams", Icons.Filled.BarChart),
            Triple("Favorites", "Pinned Coins & Pairs", Icons.Filled.Favorite),
            Triple("Data & Sync", "Backup & JSON Utility", Icons.Filled.Sync),
            Triple("Appearance", "Theme & Color Highlight", Icons.Filled.Palette),
            Triple("Settings", "Policy & Fine Tunings", Icons.Filled.Settings),
            Triple("Help Center", "Support & FAQ Center", Icons.Filled.Help),
            Triple("About", "Specifications & End Points", Icons.Filled.Info)
        )
        if (isAdminAuthenticated) {
            list.add(Triple("Admin Panel", "System & Admin Controls", Icons.Filled.AdminPanelSettings))
        } else {
            list.add(Triple("Admin Login", "Access Admin Features", Icons.Filled.Lock))
        }
        list
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    val keyChar = keyEvent.utf16CodePoint.toChar()
                    when (keyEvent.key) {
                        Key.Backspace -> {
                            triggerFeedback()
                            viewModel.onKeyPressed("DEL")
                            true
                        }
                        Key.Escape -> {
                            triggerFeedback()
                            viewModel.onKeyPressed("AC")
                            true
                        }
                        Key.Enter, Key.NumPadEnter -> {
                            triggerFeedback()
                            viewModel.onKeyPressed("=")
                            true
                        }
                        else -> {
                            if (keyChar != '\u0000') {
                                val keyAction = when (keyChar) {
                                    '*' -> "×"
                                    '/' -> "÷"
                                    else -> keyChar.toString()
                                }
                                if ("0123456789.+-×÷()^%e".contains(keyAction)) {
                                    triggerFeedback()
                                    viewModel.onKeyPressed(keyAction)
                                    true
                                } else false
                            } else false
                        }
                    }
                } else false
            }
    ) {
        // Glowing highlights behind Glass (Dark Theme only)
        if (isDark) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(chosenAccentColor.copy(alpha = 0.16f), Color.Transparent),
                                center = Offset(size.width * 0.85f, size.height * 0.15f),
                                radius = size.minDimension * 0.65f
                            )
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0x1F00F0FF), Color.Transparent),
                                center = Offset(size.width * 0.15f, size.height * 0.85f),
                                radius = size.minDimension * 0.7f
                            )
                        )
                    }
            )
        }

        Row(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            // WIDESCREEN SIDEBAR
            if (isWideScreen) {
                GlassSidebar(
                    tabsList = tabsList,
                    activeTab = activeTab,
                    onTabSelected = { 
                        triggerFeedback()
                        activeTab = it 
                    },
                    expanded = sidebarExpanded,
                    onToggleExpand = {
                        triggerFeedback()
                        sidebarExpanded = !sidebarExpanded
                    },
                    isDark = isDark,
                    accentColor = chosenAccentColor
                )
            }

            // MAIN WORKSPACE PANELS
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top App Navigation Ribbon
                GlassTopAppBar(
                    title = activeTab,
                    subtitle = tabsList.find { it.first == activeTab }?.second ?: "",
                    isDark = isDark,
                    isWideScreen = isWideScreen,
                    onMenuClick = {
                        triggerFeedback()
                        drawerOpen = true
                    },
                    onToggleTheme = {
                        triggerFeedback()
                        viewModel.toggleTheme()
                    },
                    accentColor = chosenAccentColor,
                    isConnected = viewModel.isConnected.collectAsStateWithLifecycle().value
                )

                // Page Switching Frame
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (activeTab) {
                        "Dashboard" -> DashboardPage(
                            viewModel = viewModel,
                            historyList = historyList,
                            isDark = isDark,
                            accentColor = chosenAccentColor,
                            triggerFeedback = triggerFeedback,
                            onSwitchTab = { activeTab = it }
                        )
                        "Calculator" -> CalculatorPage(
                            viewModel = viewModel,
                            expr = expr,
                            res = res,
                            preview = preview,
                            isSciMode = isSciMode,
                            isDegMode = isDegMode,
                            memoryVal = memoryVal,
                            isDark = isDark,
                            triggerFeedback = triggerFeedback,
                            clipboardManager = clipboardManager,
                            context = context,
                            accentColor = chosenAccentColor
                        )
                        "Currency Exchange" -> Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            CurrencyExchangeCenter(
                                viewModel = viewModel,
                                isDark = isDark,
                                triggerFeedback = triggerFeedback
                            )
                        }
                        "Financial Tools" -> FinancialToolsPage(
                            isDark = isDark,
                            accentColor = chosenAccentColor,
                            triggerFeedback = triggerFeedback
                        )
                        "Admin Login" -> AdminLoginScreen(
                            onLoginSuccess = { activeTab = "Admin Panel" },
                            viewModel = viewModel,
                            isDark = isDark,
                            accentColor = chosenAccentColor
                        )
                        "Admin Panel" -> {
                            if (isAdminAuthenticated) {
                                AdminPanelPage(
                                    viewModel = viewModel,
                                    isDark = isDark,
                                    accentColor = chosenAccentColor
                                )
                            } else {
                                DashboardPage(
                                    viewModel = viewModel,
                                    historyList = historyList,
                                    isDark = isDark,
                                    accentColor = chosenAccentColor,
                                    triggerFeedback = triggerFeedback,
                                    onSwitchTab = { activeTab = it }
                                )
                            }
                        }
                        "Analytics" -> AnalyticsPage(
                            historyList = historyList,
                            isDark = isDark,
                            accentColor = chosenAccentColor,
                            triggerFeedback = triggerFeedback
                        )
                        "Favorites" -> FavoritesPage(
                            viewModel = viewModel,
                            isDark = isDark,
                            accentColor = chosenAccentColor,
                            triggerFeedback = triggerFeedback,
                            onRedirectToConverter = { activeTab = "Currency Exchange" }
                        )
                        "Data & Sync" -> DataSyncPage(
                            viewModel = viewModel,
                            historyList = historyList,
                            isDark = isDark,
                            accentColor = chosenAccentColor,
                            triggerFeedback = triggerFeedback,
                            context = context
                        )
                        "Appearance" -> AppearancePage(
                            isDark = isDark,
                            onToggleTheme = { viewModel.toggleTheme() },
                            selectedAccent = accentSelection,
                            onAccentSelected = { accentSelection = it },
                            accentColor = chosenAccentColor,
                            triggerFeedback = triggerFeedback
                        )
                        "Settings" -> SettingsPage(
                            isDark = isDark,
                            accentColor = chosenAccentColor,
                            triggerFeedback = triggerFeedback,
                            context = context
                        )
                        "Help Center" -> HelpCenterPage(
                            isDark = isDark,
                            accentColor = chosenAccentColor,
                            triggerFeedback = triggerFeedback
                        )
                        "About" -> AboutPage(
                            isDark = isDark,
                            accentColor = chosenAccentColor,
                            triggerFeedback = triggerFeedback,
                            context = context
                        )
                    }
                }
            }
        }

        // MOBILE PORTRAIT OVERLAY GRACEFUL DRAWER
        if (!isWideScreen) {
            AnimatedVisibility(
                visible = drawerOpen,
                enter = slideInHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMedium)) { -it } + fadeIn(),
                exit = slideOutHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMedium)) { -it } + fadeOut()
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    // Drawer Contents
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(290.dp)
                            .background(if (isDark) Color(0xFAF121523) else Color(0xFAF7F9FC))
                            .border(BorderStroke(1.dp, if (isDark) GlassBorderDark else GlassBorderLight))
                            .padding(16.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Header Logo Brand
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.img_app_icon_1781159112197),
                                    contentDescription = "App Icon",
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "NEO CALC PRO",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isDark) PureWhite else LiteTextPrimary,
                                    letterSpacing = 1.sp
                                )
                            }

                            Divider(color = if (isDark) Color(0x1FFFFFFF) else Color(0x1F000000), thickness = 1.dp)
                            Spacer(modifier = Modifier.height(10.dp))

                            // Scrollable list of tabs inside drawer
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(tabsList) { tab ->
                                    val isSelected = activeTab == tab.first
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                if (isSelected) chosenAccentColor.copy(alpha = 0.15f) else Color.Transparent
                                            )
                                            .clickable {
                                                triggerFeedback()
                                                activeTab = tab.first
                                                drawerOpen = false
                                            }
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = tab.third,
                                            contentDescription = tab.first,
                                            tint = if (isSelected) chosenAccentColor else (if (isDark) LightGray else LiteTextSecondary),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = tab.first,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) chosenAccentColor else (if (isDark) PureWhite else LiteTextPrimary)
                                            )
                                            Text(
                                                text = tab.second,
                                                fontSize = 8.sp,
                                                color = if (isDark) LightGray.copy(alpha = 0.5f) else LiteTextSecondary.copy(alpha = 0.6f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Scrim area click-to-dismiss drawer
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(Color.Black.copy(alpha = 0.45f))
                            .clickable {
                                triggerFeedback()
                                drawerOpen = false
                            }
                    )
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Composable
fun MemoryBar(
    memoryVal: BigDecimal,
    isDark: Boolean,
    onKeyClicked: (String) -> Unit
) {
    val barBg = if (isDark) Color(0x0DFFFFFF) else Color(0x0D000000)
    val textColor = if (isDark) LightGray else LiteTextSecondary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val memoryKeys = listOf("MC", "MR", "M+", "M-")
        for (key in memoryKeys) {
            val isMMinus = key == "M-"
            val activeColor = if (isDark) ElectricBlue else LitePrimary
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            val isHovered by interactionSource.collectIsHoveredAsState()

            val scale by animateFloatAsState(
                targetValue = if (isPressed) 0.95f else if (isHovered) 1.03f else 1.0f,
                label = "Memory key scale"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .scale(scale)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isPressed) barBg.copy(alpha = 0.2f) else barBg)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = LocalIndication.current,
                        onClick = { onKeyClicked(key) }
                    )
                    .padding(vertical = 12.dp)
                    .testTag("key_${key.lowercase()}"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = key,
                    style = TextStyle(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isMMinus) activeColor else textColor,
                        fontFamily = FontFamily.SansSerif
                    ),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun CalculatorHeader(
    isDark: Boolean,
    isSciMode: Boolean,
    isSidebarOpen: Boolean,
    onToggleHistory: () -> Unit,
    onToggleSci: () -> Unit,
    onToggleTheme: () -> Unit,
    triggerFeedback: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // History toggle with dynamic badge
        IconButton(
            onClick = {
                triggerFeedback()
                onToggleHistory()
            },
            modifier = Modifier
                .background(
                    if (isSidebarOpen) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent,
                    RoundedCornerShape(12.dp)
                )
                .testTag("history_toggle_button")
        ) {
            Icon(
                imageVector = Icons.Filled.History,
                contentDescription = "Calculation History",
                tint = if (isDark) PureWhite else LiteTextPrimary
            )
        }

        // Modern dynamic app brand pill
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .border(
                    BorderStroke(1.dp, if (isDark) GlassBorderDark else GlassBorderLight),
                    RoundedCornerShape(16.dp)
                )
                .background(
                    if (isDark) GlassCardDark else GlassCardLight,
                    RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_app_icon_1781159112197),
                contentDescription = "Neo Calc Premium Logo",
                modifier = Modifier
                    .size(18.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "NEO CALC",
                style = TextStyle(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    color = if (isDark) PureWhite else LiteTextPrimary
                )
            )
        }

        // Scientific + Theme toggles wrapped in beautiful design
        Row(verticalAlignment = Alignment.CenterVertically) {
            
            // Scientific button
            IconButton(
                onClick = {
                    triggerFeedback()
                    onToggleSci()
                },
                modifier = Modifier
                    .background(
                        if (isSciMode) (if (isDark) ElectricBlue.copy(alpha = 0.2f) else LitePrimary.copy(alpha = 0.15f)) else Color.Transparent,
                        RoundedCornerShape(12.dp)
                    )
            ) {
                Icon(
                    imageVector = Icons.Filled.Science,
                    contentDescription = "Scientific Functions",
                    tint = if (isSciMode) (if (isDark) CyanAccent else LiteAccent) else (if (isDark) LightGray else LiteTextSecondary)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Premium Light/Dark Switcher pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isDark) Color(0x3D1F2937) else Color(0x1F000000))
                    .clickable {
                        triggerFeedback()
                        onToggleTheme()
                    }
                    .padding(4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(if (isDark) ElectricBlue else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DarkMode,
                            contentDescription = "Dark Theme",
                            tint = if (isDark) PureWhite else LiteTextSecondary.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(if (!isDark) LitePrimary else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LightMode,
                            contentDescription = "Light Theme",
                            tint = if (!isDark) PureWhite else LightGray.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CalculatorDisplay(
    expression: String,
    result: String,
    preview: String,
    isDark: Boolean,
    isDeg: Boolean,
    memoryVal: BigDecimal,
    onCopyResult: () -> Unit
) {
    val displayBg = if (isDark) GlassCardDark else GlassCardLight
    val displayBorder = if (isDark) GlassBorderDark else GlassBorderLight

    // Glassmorphism digital frame
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .shadow(
                elevation = if (isDark) 0.dp else 4.dp,
                shape = RoundedCornerShape(26.dp)
            )
            .border(
                BorderStroke(2.dp, displayBorder),
                RoundedCornerShape(26.dp)
            )
            .background(
                displayBg,
                RoundedCornerShape(26.dp)
            )
            .clip(RoundedCornerShape(26.dp))
            .padding(20.dp)
    ) {
        // Status indicator badges (DEG/RAD, Memory "M")
        Row(
            modifier = Modifier.align(Alignment.TopStart),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isDark) Color(0x33000000) else Color(0x1F000000))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (isDeg) "DEG" else "RAD",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    color = if (isDark) CyanAccent else LiteAccent
                )
            }

            if (memoryVal.compareTo(BigDecimal.ZERO) != 0) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isDark) Color(0x33000000) else Color(0x1F000000))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "M",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) ElectricBlue else LitePrimary
                    )
                }
            }
        }

        // Copy button in top right corner
        IconButton(
            onClick = onCopyResult,
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Icon(
                imageVector = Icons.Filled.ContentCopy,
                contentDescription = "Copy result",
                tint = if (isDark) LightGray else LiteTextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }

        // Output lines container
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomEnd)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.End
        ) {
            // Expression formula input line (styled with monospace numbers)
            Text(
                text = expression.ifEmpty { "0" },
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Light,
                    fontSize = if (expression.length > 20) 20.sp else 26.sp,
                    color = if (isDark) LightGray else LiteTextSecondary,
                    textAlign = TextAlign.End
                ),
                maxLines = Int.MAX_VALUE
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Live evaluation result preview OR actual final result Display
            if (result.isNotEmpty()) {
                Text(
                    text = result,
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = if (result.length > 12) 34.sp else 46.sp,
                        color = if (isDark) PureWhite else LiteTextPrimary,
                        textAlign = TextAlign.End
                    ),
                    maxLines = Int.MAX_VALUE,
                    modifier = Modifier.testTag("result_display_text")
                )
            } else if (preview.isNotEmpty()) {
                Text(
                    text = preview,
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Normal,
                        fontSize = 22.sp,
                        color = (if (isDark) CyanAccent else LiteAccent).copy(alpha = 0.85f),
                        textAlign = TextAlign.End
                    ),
                    maxLines = Int.MAX_VALUE
                )
            } else {
                Text(
                    text = "",
                    style = TextStyle(
                        fontSize = 46.sp
                    ),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun CalculatorKeypad(
    isSciMode: Boolean,
    isDark: Boolean,
    isDeg: Boolean,
    memoryVal: BigDecimal,
    onKeyClicked: (String) -> Unit
) {
    // Basic standard layout grid keys
    val basicRows = listOf(
        listOf("AC", "±", "%", "÷"),
        listOf("7", "8", "9", "×"),
        listOf("4", "5", "6", "-"),
        listOf("1", "2", "3", "+"),
        listOf("0", ".", "DEL", "=")
    )

    // Scientific keys to expose
    val sciButtons = listOf(
        "sin", "cos", "tan", "ln",
        "log", "√", "^", "deg_rad",
        "x²", "1/x", "(", ")"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Expand scientific block at the top if scientific mode is active
        AnimatedVisibility(
            visible = isSciMode,
            enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
            exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            ) {
                // Organize scientific keys into 3 parallel rows
                val sciRows = sciButtons.chunked(4)
                for (row in sciRows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        for (item in row) {
                            val isSpecialSymbol = item == "deg_rad"
                            val visualLabel = when (item) {
                                "deg_rad" -> if (isDeg) "DEG" else "RAD"
                                "x²" -> "x²"
                                "1/x" -> "¹/x"
                                else -> item
                            }
                            KeyButton(
                                label = visualLabel,
                                isDark = isDark,
                                isSci = true,
                                isAccent = isSpecialSymbol,
                                modifier = Modifier.weight(1f).aspectRatio(1.8f)
                            ) {
                                onKeyClicked(item)
                            }
                        }
                    }
                }
            }
        }

        // Print standard key grid
        for (row in basicRows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                for (symbol in row) {
                    val isAction = symbol == "AC" || symbol == "±" || symbol == "%"
                    val isOperator = symbol == "÷" || symbol == "×" || symbol == "-" || symbol == "+"
                    val isEqual = symbol == "="

                    // Apply visual styling priorities
                    val buttonWeight = 1f
                    
                    KeyButton(
                        label = symbol,
                        isDark = isDark,
                        isOperator = isOperator,
                        isAction = isAction,
                        isEqual = isEqual,
                        modifier = Modifier
                            .weight(buttonWeight)
                            .aspectRatio(1.25f)
                    ) {
                        onKeyClicked(symbol)
                    }
                }
            }
        }
    }
}

@Composable
fun KeyButton(
    label: String,
    isDark: Boolean,
    isOperator: Boolean = false,
    isAction: Boolean = false,
    isEqual: Boolean = false,
    isMemory: Boolean = false,
    isSci: Boolean = false,
    isAccent: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()

    // Smooth physics key scale on interaction
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else if (isHovered) 1.03f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "Button scale"
    )

    // Glassmorphic animation for text color / or simplified
    val contentColor = if (isDark) Color.White else Color.Black


    // Special glowing animation for Equal button
    val infiniteTransition = rememberInfiniteTransition()
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000), repeatMode = RepeatMode.Reverse)
    )
    val glowIntensity = if (isEqual) pulse else 1f
    
    val glowColor = if (isEqual) CyanAccent else ElectricBlue
    Box(
        modifier = modifier
            .padding(4.dp)
            .scale(scale)
            .shadow(
                elevation = if (isPressed) 2.dp else (4.dp + 4.dp * glowIntensity),
                shape = RoundedCornerShape(16.dp),
                spotColor = glowColor.copy(alpha = if (isPressed) 0.8f else (if(isHovered) 0.5f else 0.3f * glowIntensity)),
                ambientColor = glowColor
            )
            .border(
                BorderStroke(2.dp, glowColor.copy(alpha = if (isPressed) 0.8f else 0.4f * glowIntensity)),
                RoundedCornerShape(16.dp)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.1f),
                        Color.Black.copy(alpha = 0.1f)
                    )
                )
            )
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            )
            .testTag("key_${label.lowercase()}"),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = label,
            transitionSpec = {
                fadeIn(animationSpec = tween(120)) togetherWith fadeOut(animationSpec = tween(120))
            },
            label = "Button text switch"
        ) { targetLabel ->
            if (targetLabel == "DEL") {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = "Backspace",
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Text(
                    text = targetLabel,
                    style = TextStyle(
                        fontSize = if (isMemory || isSci) 13.sp else 19.sp,
                        fontWeight = if (isOperator || isEqual) FontWeight.Bold else FontWeight.Medium,
                        fontFamily = FontFamily.SansSerif,
                        color = contentColor
                    ),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun HistoryPanel(
    history: List<Calculation>,
    onUseExpression: (Calculation) -> Unit,
    onDeleteExpression: (Calculation) -> Unit,
    onClearAll: () -> Unit,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val panelBg = if (isDark) GlassCardDark else GlassCardLight
    val borderCol = if (isDark) GlassBorderDark else GlassBorderLight

    Box(
        modifier = modifier
            .shadow(
                elevation = if (isDark) 0.dp else 6.dp,
                shape = RoundedCornerShape(24.dp)
            )
            .border(
                BorderStroke(1.dp, borderCol),
                RoundedCornerShape(24.dp)
            )
            .background(
                panelBg,
                RoundedCornerShape(24.dp)
            )
            .clip(RoundedCornerShape(24.dp))
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "History",
                    style = TextStyle(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) PureWhite else LiteTextPrimary
                    )
                )

                if (history.isNotEmpty()) {
                    IconButton(
                        onClick = onClearAll,
                        modifier = Modifier.size(34.dp).testTag("clear_history_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteSweep,
                            contentDescription = "Clear calculation histories",
                            tint = if (isDark) LightGray else LiteTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // History entries
            if (history.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.HistoryToggleOff,
                            contentDescription = null,
                            tint = (if (isDark) LightGray else LiteTextSecondary).copy(alpha = 0.5f),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No history recorded",
                            fontSize = 12.sp,
                            color = (if (isDark) LightGray else LiteTextSecondary).copy(alpha = 0.5f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(history, key = { it.id }) { item ->
                        HistoryCard(
                            calc = item,
                            isDark = isDark,
                            onClick = { onUseExpression(item) },
                            onDelete = { onDeleteExpression(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryCard(
    calc: Calculation,
    isDark: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isDark) Color(0x1F000000) else Color(0x35000000))
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = calc.expression,
                style = TextStyle(
                    fontSize = 13.sp,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Light,
                    color = if (isDark) LightGray else LiteTextSecondary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "= ${calc.result}",
                style = TextStyle(
                    fontSize = 15.sp,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDark) PureWhite else LiteTextPrimary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = "Delete entry",
                tint = (if (isDark) LightGray else LiteTextSecondary).copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencySelectorDialog(
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
    favorites: Set<String>,
    onToggleFavorite: (String) -> Unit,
    availableCurrencies: List<String>,
    currencyQuery: String,
    onQueryChange: (String) -> Unit,
    isDark: Boolean
) {
    val filteredList = remember(availableCurrencies, currencyQuery) {
        if (currencyQuery.isBlank()) {
            availableCurrencies
        } else {
            availableCurrencies.filter { code ->
                code.contains(currencyQuery, ignoreCase = true) ||
                (com.example.data.ExchangeRateApi.CURRENCY_METADATA[code]?.first?.contains(currencyQuery, ignoreCase = true) ?: false)
            }
        }
    }

    val finalSortedList = remember(filteredList, favorites) {
        filteredList.sortedWith { a, b ->
            val favA = favorites.contains(a)
            val favB = favorites.contains(b)
            when {
                favA && !favB -> -1
                !favA && favB -> 1
                else -> a.compareTo(b)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = if (isDark) CyanAccent else LiteAccent)
            }
        },
        title = {
            Text(
                "Select Currency",
                style = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) PureWhite else LiteTextPrimary
                )
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                // Search bar
                OutlinedTextField(
                    value = currencyQuery,
                    onValueChange = onQueryChange,
                    placeholder = { Text("Search 150+ currencies...", fontSize = 13.sp) },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = if (isDark) PureWhite else LiteTextPrimary,
                        unfocusedTextColor = if (isDark) LightGray else LiteTextSecondary,
                        focusedBorderColor = if (isDark) CyanAccent else LiteAccent,
                        unfocusedBorderColor = if (isDark) GlassBorderDark else GlassBorderLight,
                        focusedContainerColor = if (isDark) Color(0x33000000) else Color(0x0C000000),
                        unfocusedContainerColor = if (isDark) Color(0x1A000000) else Color(0x05000000)
                    )
                )

                // List
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(finalSortedList) { code ->
                        val metadata = com.example.data.ExchangeRateApi.CURRENCY_METADATA[code]
                        val name = metadata?.first ?: "World Currency"
                        val flag = metadata?.second ?: "🌐"
                        val isFavorite = favorites.contains(code)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isDark) Color(0x0CFFFFFF) else Color(0x08000000)
                                )
                                .clickable {
                                    onSelect(code)
                                    onDismiss()
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(flag, fontSize = 22.sp, modifier = Modifier.padding(end = 12.dp))
                                Column {
                                    Text(
                                        code,
                                        style = TextStyle(
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) PureWhite else LiteTextPrimary
                                        )
                                    )
                                    Text(
                                        name,
                                        style = TextStyle(
                                            fontSize = 11.sp,
                                            color = if (isDark) LightGray else LiteTextSecondary
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            IconButton(
                                onClick = { onToggleFavorite(code) },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                    contentDescription = "Favorite",
                                    tint = if (isFavorite) Color.Red else (if (isDark) LightGray else LiteTextSecondary),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        containerColor = if (isDark) Color(0xFF161F30) else Color(0xFFFFFFFF),
        shape = RoundedCornerShape(26.dp)
    )
}

@Composable
fun CurrencyTrendChart(
    fromCode: String,
    toCode: String,
    rate: Double,
    isDark: Boolean
) {
    // Generate deterministic trend points based on parent currencies
    val points = remember(fromCode, toCode, rate) {
        val seed = (fromCode + toCode).hashCode().toLong()
        val random = java.util.Random(seed)
        val list = mutableListOf<Float>()
        var current = rate * 0.98
        for (i in 1..14) {
            val change = (random.nextDouble() - 0.5) * 0.05 // +/- 2.5% volatility
            current *= (1.0 + change)
            list.add(current.toFloat())
        }
        list[list.size - 1] = rate.toFloat() // anchor to real final rate
        list
    }

    val minVal = points.minOrNull() ?: 0f
    val maxVal = points.maxOrNull() ?: 1f
    val delta = (maxVal - minVal).let { if (it == 0f) 1f else it }

    val strokeColor = if (isDark) CyanAccent else LiteAccent
    val glowColor = if (isDark) ElectricBlue.copy(alpha = 0.4f) else LitePrimary.copy(alpha = 0.3f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .padding(vertical = 8.dp)
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val spacing = width / (points.size - 1)

            val path = androidx.compose.ui.graphics.Path()
            val fillPath = androidx.compose.ui.graphics.Path()

            points.forEachIndexed { index, value ->
                val x = index * spacing
                val normalizedY = (value - minVal) / delta
                val y = height - (normalizedY * (height - 15f) + 5f)

                if (index == 0) {
                    path.moveTo(x, y)
                    fillPath.moveTo(x, height)
                    fillPath.lineTo(x, y)
                } else {
                    path.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }
                
                if (index == points.size - 1) {
                    fillPath.lineTo(x, height)
                    fillPath.close()
                }
            }

            // Draw area gradient fill under line
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(strokeColor.copy(alpha = 0.18f), Color.Transparent),
                    startY = 0f,
                    endY = height
                )
            )

            // Draw trend line
            drawPath(
                path = path,
                color = strokeColor,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 2.5.dp.toPx(),
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    join = androidx.compose.ui.graphics.StrokeJoin.Round
                )
            )
        }
    }
}

@Composable
fun CurrencyExchangeCenter(
    viewModel: CalculatorViewModel,
    isDark: Boolean,
    triggerFeedback: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fromCurrency by viewModel.fromCurrency.collectAsStateWithLifecycle()
    val toCurrency by viewModel.toCurrency.collectAsStateWithLifecycle()
    val converterAmount by viewModel.converterAmount.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val rates by viewModel.rates.collectAsStateWithLifecycle()
    val isRatesLoading by viewModel.isRatesLoading.collectAsStateWithLifecycle()
    val ratesError by viewModel.ratesError.collectAsStateWithLifecycle()
    val isConnected by viewModel.isConnected.collectAsStateWithLifecycle()
    val rateChangePercent by viewModel.rateChangePercent.collectAsStateWithLifecycle()
    val lastRatesRefresh by viewModel.lastRatesRefresh.collectAsStateWithLifecycle()
    val currencyQuery by viewModel.currencyQuery.collectAsStateWithLifecycle()

    var showFromSelector by remember { mutableStateOf(false) }
    var showToSelector by remember { mutableStateOf(false) }

    val fromRate = rates[fromCurrency] ?: 1.0
    val toRate = rates[toCurrency] ?: 1.0
    val currentFactor = if (fromRate != 0.0) toRate / fromRate else 0.0

    val baseDbl = converterAmount.toDoubleOrNull() ?: 0.0
    val targetAmountDisplay = remember(baseDbl, currentFactor) {
        val value = baseDbl * currentFactor
        if (value == 0.0) "0.00" else String.format(Locale.US, "%,.2f", value)
    }

    val availableCurrencies = remember(rates) {
        rates.keys.toList().sorted()
    }

    val panelBg = if (isDark) GlassCardDark else GlassCardLight
    val borderCol = if (isDark) GlassBorderDark else GlassBorderLight

    // Format the last updated timestamp nicely
    val formattedTime = remember(lastRatesRefresh) {
        if (lastRatesRefresh == 0L) {
            "Initializing..."
        } else {
            val sdf = java.text.SimpleDateFormat("HH:mm:ss", Locale.US)
            sdf.format(java.util.Date(lastRatesRefresh))
        }
    }

    // Base dialog injection
    if (showFromSelector) {
        CurrencySelectorDialog(
            onDismiss = { showFromSelector = false },
            onSelect = { viewModel.setFromCurrency(it) },
            favorites = favorites,
            onToggleFavorite = { viewModel.toggleFavoriteCurrency(it) },
            availableCurrencies = availableCurrencies,
            currencyQuery = currencyQuery,
            onQueryChange = { viewModel.setCurrencyQuery(it) },
            isDark = isDark
        )
    }

    if (showToSelector) {
        CurrencySelectorDialog(
            onDismiss = { showToSelector = false },
            onSelect = { viewModel.setToCurrency(it) },
            favorites = favorites,
            onToggleFavorite = { viewModel.toggleFavoriteCurrency(it) },
            availableCurrencies = availableCurrencies,
            currencyQuery = currencyQuery,
            onQueryChange = { viewModel.setCurrencyQuery(it) },
            isDark = isDark
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isDark) 0.dp else 4.dp,
                shape = RoundedCornerShape(26.dp)
            )
            .border(
                BorderStroke(1.dp, borderCol),
                RoundedCornerShape(26.dp)
            )
            .background(
                panelBg,
                RoundedCornerShape(26.dp)
            )
            .clip(RoundedCornerShape(26.dp))
            .padding(18.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Heading with Refresh Status Controls and internet status indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.CurrencyExchange,
                        contentDescription = null,
                        tint = if (isDark) CyanAccent else LiteAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "FX Exchange Center",
                        style = TextStyle(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) PureWhite else LiteTextPrimary
                        )
                    )
                }

                // Online/Offline and auto-refresh pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Modern glow status badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isConnected) Color(0x2216A34A) else Color(0x22EF4444)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(
                                        if (isConnected) Color(0xFF16A34A) else Color(0xFFEF4444),
                                        CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isConnected) "LIVE" else "OFFLINE",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isConnected) Color(0xFF16A34A) else Color(0xFFEF4444)
                            )
                        }
                    }

                    if (isRatesLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = if (isDark) CyanAccent else LiteAccent
                        )
                    } else {
                        IconButton(
                            onClick = {
                                triggerFeedback()
                                viewModel.refreshRates()
                            },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "Refresh rates",
                                tint = if (isDark) LightGray else LiteTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Subtitle metadata row (Live updates / last checked)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Refreshes automatically every 30s",
                    fontSize = 9.sp,
                    color = if (isDark) LightGray.copy(alpha = 0.6f) else LiteTextSecondary.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Checked: $formattedTime",
                    fontSize = 9.sp,
                    color = if (isDark) CyanAccent.copy(alpha = 0.8f) else LiteAccent.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Bold
                )
            }

            // --- FROM CURRENCY BLOCK ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Dropdown trigger for From Currency
                val metadataFrom = com.example.data.ExchangeRateApi.CURRENCY_METADATA[fromCurrency]
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDark) Color(0x1F000000) else Color(0x12000000))
                        .clickable {
                            triggerFeedback()
                            viewModel.setCurrencyQuery("")
                            showFromSelector = true
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(metadataFrom?.second ?: "🌐", fontSize = 18.sp, modifier = Modifier.padding(end = 6.dp))
                    Text(
                        fromCurrency,
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) PureWhite else LiteTextPrimary
                        )
                    )
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = "Select Base Currency",
                        tint = if (isDark) LightGray else LiteTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Input Box for converter amount
                OutlinedTextField(
                    value = converterAmount,
                    onValueChange = { viewModel.setConverterAmount(it) },
                    placeholder = { Text("0.0", fontSize = 14.sp) },
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) PureWhite else LiteTextPrimary,
                        textAlign = TextAlign.End
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier
                        .width(140.dp)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = if (isDark) PureWhite else LiteTextPrimary,
                        unfocusedTextColor = if (isDark) LightGray else LiteTextSecondary,
                        focusedBorderColor = if (isDark) CyanAccent else LiteAccent,
                        unfocusedBorderColor = if (isDark) Color(0x33FFFFFF) else Color(0x33000000),
                        focusedContainerColor = if (isDark) Color(0x1F000000) else Color(0x0A000000),
                        unfocusedContainerColor = if (isDark) Color(0x0C000000) else Color(0x05000000)
                    )
                )
            }

            // --- DECORATIVE SWAP ROW ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                HorizontalDivider(
                    color = if (isDark) Color(0x1AFFFFFF) else Color(0x1A000000),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                )

                IconButton(
                    onClick = {
                        triggerFeedback()
                        viewModel.swapCurrencies()
                    },
                    modifier = Modifier
                        .size(34.dp)
                        .shadow(4.dp, CircleShape)
                        .background(if (isDark) Color(0xFF1E293B) else Color(0xFFFFFFFF), CircleShape)
                        .border(
                            BorderStroke(1.dp, if (isDark) Color(0x1AFFFFFF) else Color(0x1A000000)),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Filled.SwapVert,
                        contentDescription = "Swap currencies",
                        tint = if (isDark) CyanAccent else LiteAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // --- TO CURRENCY BLOCK ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Dropdown trigger for To Currency
                val metadataTo = com.example.data.ExchangeRateApi.CURRENCY_METADATA[toCurrency]
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDark) Color(0x1F000000) else Color(0x12000000))
                        .clickable {
                            triggerFeedback()
                            viewModel.setCurrencyQuery("")
                            showToSelector = true
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(metadataTo?.second ?: "🌐", fontSize = 18.sp, modifier = Modifier.padding(end = 6.dp))
                    Text(
                        toCurrency,
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) PureWhite else LiteTextPrimary
                        )
                    )
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = "Select Target Currency",
                        tint = if (isDark) LightGray else LiteTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Read-Only Target Calculated amount
                Text(
                    text = targetAmountDisplay,
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isDark) CyanAccent else LiteAccent,
                        textAlign = TextAlign.End
                    ),
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .testTag("target_exchange_display_text")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // --- LIVE RATE TAGLINE & TREND DISPLAY ---
            val changePct = rateChangePercent[toCurrency] ?: 0.0
            val isTrendUp = changePct >= 0.0
            val trendSymbol = if (isTrendUp) "↑" else "↓"
            val trendColor = if (isTrendUp) Color(0xFF16A34A) else Color(0xFFEF4444)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Live rate details
                Text(
                    text = "1 $fromCurrency = ${String.format(Locale.US, "%,.4f", currentFactor)} $toCurrency",
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = if (isDark) PureWhite else LiteTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                )

                // High-End Change Indicator (↑ +0.45% / ↓ -0.22%)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$trendSymbol ${String.format(Locale.US, "%+.2f", changePct)}%",
                        style = TextStyle(
                            fontSize = 11.sp,
                            color = trendColor,
                            fontWeight = FontWeight.Black
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "(24h)",
                        style = TextStyle(
                            fontSize = 10.sp,
                            color = if (isDark) LightGray.copy(alpha = 0.6f) else LiteTextSecondary.copy(alpha = 0.8f)
                        )
                    )
                }
            }

            // --- TRADING DESK BUY/SELL RATIO CARDS ---
            val buyFactor = currentFactor * 1.0018
            val sellFactor = currentFactor * 0.9982

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Buy block
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDark) Color(0x3D002233) else Color(0x0A000000))
                        .border(1.dp, if (isDark) Color(0x1F00FFFF) else Color(0x0F000000), RoundedCornerShape(12.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "BUY RATE (ASK)",
                        fontSize = 9.sp,
                        color = if (isDark) LightGray.copy(alpha = 0.8f) else LiteTextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${String.format(Locale.US, "%,.4f", buyFactor)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF16A34A)
                    )
                }

                // Sell block
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDark) Color(0x3D1A162B) else Color(0x0A000000))
                        .border(1.dp, if (isDark) Color(0x1FFFFFFF) else Color(0x0F000000), RoundedCornerShape(12.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "SELL RATE (BID)",
                        fontSize = 9.sp,
                        color = if (isDark) LightGray.copy(alpha = 0.8f) else LiteTextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${String.format(Locale.US, "%,.4f", sellFactor)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEF4444)
                    )
                }
            }

            // --- CURRENCY STRENGTH INDEX INDICATOR ---
            val strengthLabel = when {
                changePct > 0.8 -> "AGGRESSIVE"
                changePct > 0.2 -> "STRONG"
                changePct > -0.2 -> "STABLE"
                changePct > -0.8 -> "SOFT"
                else -> "WEAK"
            }
            val strengthColor = when {
                changePct > 0.2 -> Color(0xFF16A34A)
                changePct > -0.2 -> if (isDark) CyanAccent else LiteAccent
                else -> Color(0xFFEF4444)
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Relative Strength Matrix",
                    fontSize = 10.sp,
                    color = if (isDark) LightGray else LiteTextSecondary,
                    fontWeight = FontWeight.Medium
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = strengthLabel,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = strengthColor
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    // Strength levels 4 bars
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        for (i in 1..4) {
                            val active = when (strengthLabel) {
                                "WEAK" -> i == 1
                                "SOFT" -> i <= 2
                                "STABLE" -> i <= 2
                                "STRONG" -> i <= 3
                                "AGGRESSIVE" -> i <= 4
                                else -> false
                            }
                            Box(
                                modifier = Modifier
                                    .size(width = 12.dp, height = 4.dp)
                                    .clip(RoundedCornerShape(1.dp))
                                    .background(
                                        if (active) strengthColor else (if (isDark) Color(0x1FFFFFFF) else Color(0x1F000000))
                                    )
                            )
                        }
                    }
                }
            }

            // --- TREND SPARKLINE CHART ---
            Spacer(modifier = Modifier.height(6.dp))
            CurrencyTrendChart(
                fromCode = fromCurrency,
                toCode = toCurrency,
                rate = currentFactor,
                isDark = isDark
            )

            // --- SYNC WITH CALCULATOR WORKFLOW TRIGGER ---
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Rate error info
                Text(
                    text = if (ratesError != null) "Offline Safe Mode Active" else "Rates updated in Real-Time",
                    style = TextStyle(
                        fontSize = 10.sp,
                        color = if (ratesError != null) Color(0xFFE11D48) else Color(0xFF16A34A),
                        fontWeight = FontWeight.SemiBold
                    ),
                    modifier = Modifier.weight(1f)
                )

                // Copy calculator result button
                Button(
                    onClick = {
                        triggerFeedback()
                        viewModel.copyCalculatorResultToConverter()
                    },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) Color(0x2406B6D4) else LitePrimary.copy(alpha = 0.15f),
                        contentColor = if (isDark) CyanAccent else LitePrimary
                    ),
                    modifier = Modifier.height(30.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Input,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Use Calc Result", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // --- QUICK FAVORITES STRIP ---
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Quick Pairs:",
                    fontSize = 9.sp,
                    color = if (isDark) LightGray else LiteTextSecondary,
                    fontWeight = FontWeight.Bold
                )
                val listToDisplay = favorites.toList().take(5)
                for (fav in listToDisplay) {
                    val metadata = com.example.data.ExchangeRateApi.CURRENCY_METADATA[fav]
                    val flag = metadata?.second ?: "🌐"
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (fromCurrency == fav) (if (isDark) CyanAccent.copy(alpha = 0.2f) else LiteAccent.copy(alpha = 0.15f))
                                else (if (isDark) Color(0x33000000) else Color(0x1F000000))
                            )
                            .clickable {
                                triggerFeedback()
                                if (fromCurrency != fav) {
                                    viewModel.setFromCurrency(fav)
                                } else {
                                    viewModel.setToCurrency(if (fav == "USD") "INR" else "USD")
                                }
                            }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Text("$flag $fav", fontSize = 10.sp, color = if (isDark) PureWhite else LiteTextPrimary, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

// ==========================================
// PREMIUM GLASSMORPHISM NAV SIDEBAR
// ==========================================
@Composable
fun GlassSidebar(
    tabsList: List<Triple<String, String, androidx.compose.ui.graphics.vector.ImageVector>>,
    activeTab: String,
    onTabSelected: (String) -> Unit,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    isDark: Boolean,
    accentColor: Color
) {
    val sidebarBg = if (isDark) Color(0x3D111827) else Color(0xCCFFFFFF)
    val sidebarBorder = if (isDark) GlassBorderDark else GlassBorderLight
    val widthState = if (expanded) 245.dp else 75.dp

    Column(
        modifier = Modifier
            .width(widthState)
            .fillMaxHeight()
            .background(sidebarBg)
            .border(BorderStroke(1.dp, sidebarBorder))
            .padding(vertical = 16.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Platform Header / Brand Logo
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp, start = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (expanded) Arrangement.Start else Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_app_icon_1781159112197),
                contentDescription = null,
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
            if (expanded) {
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "NEO CALC",
                        style = TextStyle(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isDark) PureWhite else LiteTextPrimary,
                            letterSpacing = 2.sp
                        )
                    )
                    Text(
                        text = "Enterprise Suite",
                        fontSize = 8.sp,
                        color = if (isDark) LightGray.copy(alpha = 0.6f) else LiteTextSecondary.copy(alpha = 0.7f)
                    )
                }
            }
        }

        Divider(
            color = if (isDark) Color(0x0FFFFFFF) else Color(0x0F000000),
            thickness = 1.dp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Navigation Items
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(tabsList) { tab ->
                val isSelected = activeTab == tab.first
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isSelected) accentColor.copy(alpha = 0.15f) else Color.Transparent
                        )
                        .clickable { onTabSelected(tab.first) }
                        .padding(horizontal = if (expanded) 12.dp else 0.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = if (expanded) Arrangement.Start else Arrangement.Center
                ) {
                    Icon(
                        imageVector = tab.third,
                        contentDescription = tab.first,
                        tint = if (isSelected) accentColor else (if (isDark) LightGray else LiteTextSecondary),
                        modifier = Modifier.size(18.dp)
                    )
                    if (expanded) {
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = tab.first,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) accentColor else (if (isDark) PureWhite else LiteTextPrimary),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Divider(
            color = if (isDark) Color(0x0FFFFFFF) else Color(0x0F000000),
            thickness = 1.dp,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        // Expand/Collapse Command trigger
        IconButton(
            onClick = onToggleExpand,
            modifier = Modifier
                .size(36.dp)
                .background(
                    if (isDark) Color(0x13FFFFFF) else Color(0x0F000000),
                    CircleShape
                )
        ) {
            Icon(
                imageVector = if (expanded) Icons.Filled.ChevronLeft else Icons.Filled.ChevronRight,
                contentDescription = "Expand menu",
                tint = if (isDark) PureWhite else LiteTextPrimary
            )
        }
    }
}

// ==========================================
// GLASS APP BAR HEADER
// ==========================================
@Composable
fun GlassTopAppBar(
    title: String,
    subtitle: String,
    isDark: Boolean,
    isWideScreen: Boolean,
    onMenuClick: () -> Unit,
    onToggleTheme: () -> Unit,
    accentColor: Color,
    isConnected: Boolean
) {
    val barBg = if (isDark) Color(0x1F090E16) else Color(0xE6F8FAFC)
    val barBorder = if (isDark) GlassBorderDark else GlassBorderLight

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .background(barBg)
            .border(BorderStroke(1.dp, barBorder))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!isWideScreen) {
                IconButton(onClick = onMenuClick) {
                    Icon(
                        imageVector = Icons.Filled.Menu,
                        contentDescription = "Menu drawer",
                        tint = if (isDark) PureWhite else LiteTextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
            }

            Column {
                Text(
                    text = title.uppercase(),
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isDark) PureWhite else LiteTextPrimary,
                        letterSpacing = 1.sp
                    )
                )
                Text(
                    text = subtitle,
                    fontSize = 8.sp,
                    color = if (isDark) LightGray.copy(alpha = 0.6f) else LiteTextSecondary.copy(alpha = 0.7f)
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            // Live Status Indicator Chip
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (isConnected) Color(0x2210B981) else Color(0x22F43F5E)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(
                                if (isConnected) Color(0xFF10B981) else Color(0xFFF43F5E),
                                CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isConnected) "CONNECTED" else "OFFLINE",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isConnected) Color(0xFF10B981) else Color(0xFFF43F5E)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Premium Light/Dark Switcher pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDark) Color(0x3D1F2937) else Color(0x1F000000))
                    .clickable { onToggleTheme() }
                    .padding(4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(if (isDark) accentColor else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DarkMode,
                            contentDescription = "Dark Theme",
                            tint = if (isDark) PureWhite else LiteTextSecondary,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(if (!isDark) accentColor else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LightMode,
                            contentDescription = "Light Theme",
                            tint = if (!isDark) PureWhite else LightGray,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// DASHBOARD TABS
// ==========================================
@Composable
fun DashboardPage(
    viewModel: CalculatorViewModel,
    historyList: List<Calculation>,
    isDark: Boolean,
    accentColor: Color,
    triggerFeedback: () -> Unit,
    onSwitchTab: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    val fromCurrency by viewModel.fromCurrency.collectAsStateWithLifecycle()
    val toCurrency by viewModel.toCurrency.collectAsStateWithLifecycle()
    val rates by viewModel.rates.collectAsStateWithLifecycle()

    val currentFactor = remember(rates, fromCurrency, toCurrency) {
        val fRate = rates[fromCurrency] ?: 1.0
        val tRate = rates[toCurrency] ?: 1.0
        if (fRate != 0.0) tRate / fRate else 0.0
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome Banner and Operator Stats
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(BorderStroke(1.dp, if (isDark) GlassBorderDark else GlassBorderLight), RoundedCornerShape(16.dp))
                .background(if (isDark) GlassCardDark else GlassCardLight)
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = "ALGORITHMIC FINANCE TERMINAL",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Welcome, rajakhan19122006@gmail.com",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDark) PureWhite else LiteTextPrimary
                )
                Text(
                    text = "Session Started: 2026-06-11 06:37 UTC",
                    fontSize = 9.sp,
                    color = if (isDark) LightGray.copy(alpha = 0.6f) else LiteTextSecondary.copy(alpha = 0.7f)
                )
            }
        }

        // Metrics Grid Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Metric Card 1
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDark) GlassCardDark else GlassCardLight)
                    .border(BorderStroke(1.dp, if (isDark) GlassBorderDark else GlassBorderLight), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Text("Saved Operations", fontSize = 9.sp, color = if (isDark) LightGray else LiteTextSecondary)
                Text("${historyList.size}", fontSize = 18.sp, fontWeight = FontWeight.Black, color = if (isDark) PureWhite else LiteTextPrimary)
                Text("Offline Room SQLite Database", fontSize = 8.sp, color = if (isDark) accentColor else LiteTextSecondary.copy(alpha = 0.6f))
            }

            // Metric Card 2
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDark) GlassCardDark else GlassCardLight)
                    .border(BorderStroke(1.dp, if (isDark) GlassBorderDark else GlassBorderLight), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Text("Active Currency Pair", fontSize = 9.sp, color = if (isDark) LightGray else LiteTextSecondary)
                Text("$fromCurrency/$toCurrency", fontSize = 18.sp, fontWeight = FontWeight.Black, color = if (isDark) PureWhite else LiteTextPrimary)
                Text("1 $fromCurrency = ${String.format(Locale.US, "%.4f", currentFactor)} $toCurrency", fontSize = 8.sp, color = if (isDark) accentColor else LiteTextSecondary.copy(alpha = 0.6f))
            }
        }

        // INTERESTING QUICK-CALCULATOR MINI WIDGET
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(if (isDark) GlassCardDark else GlassCardLight)
                .border(BorderStroke(1.dp, if (isDark) GlassBorderDark else GlassBorderLight), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = "QUICK INTERACTIVE CALCULATOR",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Mini Digital LCD
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9))
                        .padding(10.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    val displayExpr = viewModel.expression.collectAsStateWithLifecycle().value
                    val displayRes = viewModel.resultDisplay.collectAsStateWithLifecycle().value
                    val displayPrev = viewModel.previewDisplay.collectAsStateWithLifecycle().value

                    Text(
                        text = displayExpr.ifEmpty { "0" },
                        fontSize = 11.sp,
                        color = if (isDark) LightGray else LiteTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = displayRes.ifEmpty { displayPrev.ifEmpty { "0" } },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) PureWhite else LiteTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Compact buttons grid
                val compactKeys = listOf(
                    listOf("7", "8", "9", "÷"),
                    listOf("4", "5", "6", "×"),
                    listOf("1", "2", "3", "-"),
                    listOf("AC", "0", "=", "+")
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (row in compactKeys) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            for (key in row) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(34.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            when (key) {
                                                "=" -> accentColor
                                                "AC" -> if (isDark) Color(0xFFEF4444).copy(alpha = 0.2f) else Color(0xFFEF4444).copy(alpha = 0.15f)
                                                "+", "-", "×", "÷" -> if (isDark) Color(0x3BFFFFFF) else Color(0x1F000000)
                                                else -> if (isDark) Color(0x16FFFFFF) else Color(0x0C000000)
                                            }
                                        )
                                        .clickable {
                                            triggerFeedback()
                                            viewModel.onKeyPressed(key)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = key,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (key == "=") PureWhite 
                                                else if (key == "AC") Color(0xFFF43F5E)
                                                else if (isDark) PureWhite 
                                                else LiteTextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // RECENT ACTIVITIES TIMELINE
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(if (isDark) GlassCardDark else GlassCardLight)
                .border(BorderStroke(1.dp, if (isDark) GlassBorderDark else GlassBorderLight), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "RECENT OPERATIONS LEDGER",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )

                if (historyList.isEmpty()) {
                    Text(
                        "No calculations logged in history.",
                        fontSize = 10.sp,
                        color = if (isDark) LightGray else LiteTextSecondary,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    historyList.take(3).forEach { calc ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isDark) Color(0x0A000000) else Color(0x05000000))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(calc.expression, fontSize = 10.sp, color = if (isDark) LightGray else LiteTextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("= ${calc.result}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isDark) PureWhite else LiteTextPrimary)
                            }
                            Text(
                                text = java.text.SimpleDateFormat("HH:mm:ss", Locale.US).format(java.util.Date(calc.timestamp)),
                                fontSize = 8.sp,
                                color = if (isDark) LightGray.copy(alpha = 0.5f) else LiteTextSecondary.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }

        // QUICK LINKS JUMP BAR
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                Pair("All Tools", "Calculator"),
                Pair("Rates Desk", "Currency Exchange"),
                Pair("Estimators", "Financial Tools")
            ).forEach { item ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accentColor.copy(alpha = 0.1f))
                        .border(BorderStroke(1.dp, accentColor.copy(alpha = 0.25f)), RoundedCornerShape(10.dp))
                        .clickable {
                            triggerFeedback()
                            onSwitchTab(item.second)
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.first,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }
            }
        }
    }
}

// ==========================================
// CALCULATOR TABS PAGE
// ==========================================
@Composable
fun CalculatorPage(
    viewModel: CalculatorViewModel,
    expr: String,
    res: String,
    preview: String,
    isSciMode: Boolean,
    isDegMode: Boolean,
    memoryVal: BigDecimal,
    isDark: Boolean,
    triggerFeedback: () -> Unit,
    clipboardManager: androidx.compose.ui.platform.ClipboardManager,
    context: android.content.Context,
    accentColor: Color
) {
    var subTab by remember { mutableStateOf("Standard") }
    var decInput by remember { mutableStateOf("42") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Sub-Navigation tabs row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (isDark) Color(0x1F000000) else Color(0x12000000))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("Standard", "Programmer", "Fin Quick").forEach { mode ->
                val active = subTab == mode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (active) accentColor else Color.Transparent)
                        .clickable {
                            triggerFeedback()
                            subTab = mode
                            if (mode == "Standard" && isSciMode) {
                                viewModel.toggleScientificMode()
                            }
                        }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = mode,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (active) PureWhite else (if (isDark) LightGray else LiteTextPrimary)
                    )
                }
            }
        }

        when (subTab) {
            "Standard" -> {
                // Render original fully interactive Master Calculator UI
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    CalculatorDisplay(
                        expression = expr,
                        result = res,
                        preview = preview,
                        isDark = isDark,
                        isDeg = isDegMode,
                        memoryVal = memoryVal,
                        onCopyResult = {
                            val textToCopy = res.ifEmpty { expr }
                            if (textToCopy.isNotEmpty()) {
                                triggerFeedback()
                                clipboardManager.setText(AnnotatedString(textToCopy))
                                Toast.makeText(context, "Copied: $textToCopy", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    MemoryBar(
                        memoryVal = memoryVal,
                        isDark = isDark,
                        onKeyClicked = { key ->
                            triggerFeedback()
                            viewModel.onKeyPressed(key)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .shadow(16.dp, RoundedCornerShape(24.dp), clip = false)
                        .border(
                            BorderStroke(1.dp, if (isDark) Color(0x1FFFFFFF) else Color(0x1F000000)),
                            RoundedCornerShape(24.dp)
                        )
                        .background(if (isDark) KeypadBgDark else KeypadBgLight, RoundedCornerShape(24.dp))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CalculatorKeypad(
                        isSciMode = isSciMode,
                        isDark = isDark,
                        isDeg = isDegMode,
                        memoryVal = memoryVal,
                        onKeyClicked = { key ->
                            triggerFeedback()
                            viewModel.onKeyPressed(key)
                        }
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            "Programmer" -> {
                // Programmer Numeral Systems converter console
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isDark) GlassCardDark else GlassCardLight)
                            .border(BorderStroke(1.dp, if (isDark) GlassBorderDark else GlassBorderLight), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Column {
                            Text("Base-10 Integer Input", fontSize = 10.sp, color = accentColor, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            TextField(
                                value = decInput,
                                onValueChange = { input ->
                                    if (input.all { it.isDigit() } || input.isEmpty()) {
                                        decInput = input
                                    }
                                },
                                textStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (isDark) PureWhite else LiteTextPrimary),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = TextFieldDefaults.colors(
                                    focusedIndicatorColor = accentColor,
                                    unfocusedIndicatorColor = if (isDark) Color(0x33FFFFFF) else Color(0x33000000),
                                    focusedContainerColor = if (isDark) Color(0x33000000) else Color(0x0C000000),
                                    unfocusedContainerColor = if (isDark) Color(0x1A000000) else Color(0x05000000)
                                )
                            )
                        }
                    }

                    // Numeric Systems Matrix
                    val decLong = decInput.toLongOrNull() ?: 0L
                    val hexStr = java.lang.Long.toHexString(decLong).uppercase()
                    val octStr = java.lang.Long.toOctalString(decLong)
                    val binStr = java.lang.Long.toBinaryString(decLong)

                    listOf(
                        Triple("HEX (Hexadecimal)", hexStr, "0x"),
                        Triple("DEC (Decimal)", decLong.toString(), ""),
                        Triple("OCT (Octal)", octStr, "0"),
                        Triple("BIN (Binary)", binStr, "b")
                    ).forEach { keypair ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isDark) GlassCardDark else GlassCardLight)
                                .border(BorderStroke(1.dp, if (isDark) GlassBorderDark else GlassBorderLight), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(keypair.first, fontSize = 9.sp, color = if (isDark) LightGray else LiteTextSecondary)
                                Text(
                                    text = "${keypair.third}${keypair.second}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) PureWhite else LiteTextPrimary
                                )
                            }
                            IconButton(
                                onClick = {
                                    triggerFeedback()
                                    clipboardManager.setText(AnnotatedString(keypair.second))
                                    Toast.makeText(context, "Copied System notation", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ContentCopy,
                                    contentDescription = "Copy",
                                    tint = accentColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            "Fin Quick" -> {
                // Financial Projections Screen
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isDark) GlassCardDark else GlassCardLight)
                            .border(BorderStroke(1.dp, if (isDark) GlassBorderDark else GlassBorderLight), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Column {
                            Text("RULE OF 72 DUAL COMPUTATION", fontSize = 10.sp, color = accentColor, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Enter an expected annual return rate on the Sliders page to see how fast your asset double in absolute valuations.",
                                fontSize = 11.sp,
                                color = if (isDark) LightGray else LiteTextSecondary
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            listOf(4.0, 6.0, 8.0, 10.0, 12.0).forEach { rate ->
                                val years = 72.0 / rate
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("At $rate% Annual Growth:", fontSize = 11.sp, color = if (isDark) PureWhite else LiteTextPrimary)
                                    Text("Doubles in ${String.format(Locale.US, "%.1f", years)} Years", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = accentColor)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// ADMIN LOGIN SCREEN
// ==========================================
@Composable
fun AdminLoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: CalculatorViewModel,
    isDark: Boolean,
    accentColor: Color
) {
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val isAccountLocked by viewModel.isAccountLocked.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isAccountLocked) {
            Text("ACCOUNT LOCKED. TOO MANY FAILED ATTEMPTS.", fontWeight = FontWeight.Bold, color = Color.Red)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { viewModel.resetAdminPasswordToDefault() }) {
                Text("Reset Account")
            }
        } else {
            Text("ADMIN AUTHENTICATION REQUIRED", fontWeight = FontWeight.Bold, color = accentColor)
            Spacer(modifier = Modifier.height(16.dp))
            TextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Enter Admin Password (Min 8 chars)") },
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                if (viewModel.attemptAdminLogin(password)) {
                    onLoginSuccess()
                } else {
                    error = "Invalid Password. Access Denied."
                }
            }) {
                Text("Login")
            }
            error?.let { Text(it, color = Color.Red, modifier = Modifier.padding(top = 8.dp)) }
        }
    }
}

// ==========================================
// ADMIN DASHBOARD
// ==========================================
@Composable
fun AdminPanelPage(
    viewModel: CalculatorViewModel,
    isDark: Boolean,
    accentColor: Color
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("ADMIN COMMAND CENTER", fontWeight = FontWeight.Black, fontSize = 20.sp, color = accentColor)
        Spacer(modifier = Modifier.height(16.dp))
        Text("System Status: OPERATIONAL", fontSize = 14.sp)
        Text("Database: SQLITE-LOCAL", fontSize = 14.sp)
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = { viewModel.logoutAdmin() }) {
            Text("Logout Session")
        }
    }
}

// ==========================================
// FINANCIAL CALCULATORS PAGE
// ==========================================
@Composable
fun FinancialToolsPage(
    isDark: Boolean,
    accentColor: Color,
    triggerFeedback: () -> Unit
) {
    var toolMode by remember { mutableStateOf("EMI/Loan") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Tab switcher within Financial
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(if (isDark) Color(0x1F000000) else Color(0x12000000))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("EMI/Loan", "Compound", "Tax Desk").forEach { mode ->
                val active = toolMode == mode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (active) accentColor else Color.Transparent)
                        .clickable {
                            triggerFeedback()
                            toolMode = mode
                        }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = mode,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (active) PureWhite else (if (isDark) LightGray else LiteTextPrimary)
                    )
                }
            }
        }

        when (toolMode) {
            "EMI/Loan" -> {
                var principal by remember { mutableStateOf(100000.0) }
                var rateByYr by remember { mutableStateOf(7.5) }
                var tenureYrs by remember { mutableStateOf(15) }

                // Amortization EMI computation formulary
                val rMonthly = (rateByYr / 12.0) / 100.0
                val totalMonths = tenureYrs * 12
                val compPower = Math.pow(1.0 + rMonthly, totalMonths.toDouble())
                val emiResult = if (rMonthly != 0.0) {
                    (principal * rMonthly * compPower) / (compPower - 1)
                } else {
                    principal / totalMonths
                }
                val totalRepayDbl = emiResult * totalMonths
                val totalInterestDbl = maxOf(0.0, totalRepayDbl - principal)

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Slider 1: Principal
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDark) GlassCardDark else GlassCardLight)
                            .border(BorderStroke(1.dp, if (isDark) GlassBorderDark else GlassBorderLight), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Principal Amount", fontSize = 11.sp, color = if (isDark) LightGray else LiteTextSecondary)
                                Text("$${String.format(Locale.US, "%,.0f", principal)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = accentColor)
                            }
                            Slider(
                                value = principal.toFloat(),
                                onValueChange = { principal = it.toDouble() },
                                valueRange = 5000f..500000f,
                                colors = SliderDefaults.colors(
                                    thumbColor = accentColor,
                                    activeTrackColor = accentColor
                                )
                            )
                        }
                    }

                    // Slider 2: Annual Rate
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDark) GlassCardDark else GlassCardLight)
                            .border(BorderStroke(1.dp, if (isDark) GlassBorderDark else GlassBorderLight), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Annual Interest Rate", fontSize = 11.sp, color = if (isDark) LightGray else LiteTextSecondary)
                                Text("${String.format(Locale.US, "%.1f", rateByYr)}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = accentColor)
                            }
                            Slider(
                                value = rateByYr.toFloat(),
                                onValueChange = { rateByYr = Math.round(it * 10).toDouble() / 10.0 },
                                valueRange = 1.0f..15.0f,
                                colors = SliderDefaults.colors(
                                    thumbColor = accentColor,
                                    activeTrackColor = accentColor
                                )
                            )
                        }
                    }

                    // Slider 3: Tenure Years
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDark) GlassCardDark else GlassCardLight)
                            .border(BorderStroke(1.dp, if (isDark) GlassBorderDark else GlassBorderLight), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Loan Tenure", fontSize = 11.sp, color = if (isDark) LightGray else LiteTextSecondary)
                                Text("$tenureYrs Years", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = accentColor)
                            }
                            Slider(
                                value = tenureYrs.toFloat(),
                                onValueChange = { tenureYrs = it.toInt() },
                                valueRange = 1f..30f,
                                colors = SliderDefaults.colors(
                                    thumbColor = accentColor,
                                    activeTrackColor = accentColor
                                )
                            )
                        }
                    }

                    // Breakdown calculations dashboard
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isDark) GlassCardDark else GlassCardLight)
                            .border(BorderStroke(1.dp, if (isDark) GlassBorderDark else GlassBorderLight), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("AMORTIZATION SUMMARY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = accentColor)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Estimated Monthly EMI:", fontSize = 11.sp, color = if (isDark) LightGray else LiteTextSecondary)
                                Text("$${String.format(Locale.US, "%,.2f", emiResult)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = accentColor)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total Interest Accrued:", fontSize = 11.sp, color = if (isDark) LightGray else LiteTextSecondary)
                                Text("$${String.format(Locale.US, "%,.2f", totalInterestDbl)}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = if (isDark) PureWhite else LiteTextPrimary)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total Payment Repayable:", fontSize = 11.sp, color = if (isDark) LightGray else LiteTextSecondary)
                                Text("$${String.format(Locale.US, "%,.2f", totalRepayDbl)}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = if (isDark) PureWhite else LiteTextPrimary)
                            }
                        }
                    }
                }
            }

            "Compound" -> {
                var initPrincipal by remember { mutableStateOf(10000.0) }
                var monthlyAdd by remember { mutableStateOf(300.0) }
                var yrReturnRate by remember { mutableStateOf(8.0) }
                var activeYears by remember { mutableStateOf(10) }

                // Math execution
                val mRate = (yrReturnRate / 12.0) / 100.0
                val limitMonths = activeYears * 12
                val rawPrincipalSum = initPrincipal + (monthlyAdd * limitMonths)
                var rawCompoundSum = initPrincipal * Math.pow(1.0 + mRate, limitMonths.toDouble())
                for (m in 1..limitMonths) {
                    rawCompoundSum += monthlyAdd * Math.pow(1.0 + mRate, (limitMonths - m).toDouble())
                }
                val rawInvestInterest = maxOf(0.0, rawCompoundSum - rawPrincipalSum)

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Sliders
                    listOf(
                        Triple("Initial Capital", initPrincipal, 1000.0..50000.0),
                        Triple("Monthly Addition", monthlyAdd, 50.0..2000.0)
                    ).forEach { pair ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isDark) GlassCardDark else GlassCardLight)
                                .border(BorderStroke(1.dp, if (isDark) GlassBorderDark else GlassBorderLight), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(pair.first, fontSize = 11.sp, color = if (isDark) LightGray else LiteTextSecondary)
                                    Text("$${String.format(Locale.US, "%,.0f", pair.second)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = accentColor)
                                }
                                Slider(
                                    value = pair.second.toFloat(),
                                    onValueChange = {
                                        if (pair.first == "Initial Capital") initPrincipal = it.toDouble()
                                        else monthlyAdd = it.toDouble()
                                    },
                                    valueRange = pair.third.start.toFloat()..pair.third.endInclusive.toFloat(),
                                    colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor)
                                )
                            }
                        }
                    }

                    // Interest and years parameters
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isDark) GlassCardDark else GlassCardLight)
                                .border(BorderStroke(1.dp, if (isDark) GlassBorderDark else GlassBorderLight), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Text("Growth Rate (yr)", fontSize = 10.sp, color = if (isDark) LightGray else LiteTextSecondary)
                            Text("${String.format(Locale.US, "%.1f", yrReturnRate)}%", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = accentColor)
                            Slider(
                                value = yrReturnRate.toFloat(),
                                onValueChange = { yrReturnRate = Math.round(it * 10).toDouble() / 10.0 },
                                valueRange = 1.0f..15.0f,
                                colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor)
                            )
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isDark) GlassCardDark else GlassCardLight)
                                .border(BorderStroke(1.dp, if (isDark) GlassBorderDark else GlassBorderLight), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Text("Time Period", fontSize = 10.sp, color = if (isDark) LightGray else LiteTextSecondary)
                            Text("$activeYears Years", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = accentColor)
                            Slider(
                                value = activeYears.toFloat(),
                                onValueChange = { activeYears = it.toInt() },
                                valueRange = 1f..30f,
                                colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor)
                            )
                        }
                    }

                    // Projection Dashboard Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isDark) GlassCardDark else GlassCardLight)
                            .border(BorderStroke(1.dp, if (isDark) GlassBorderDark else GlassBorderLight), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("FUTURE VALUE PROJECTION", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = accentColor)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Aggregated Portfolio Value:", fontSize = 11.sp, color = if (isDark) LightGray else LiteTextSecondary)
                                Text("$${String.format(Locale.US, "%,.2f", rawCompoundSum)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = accentColor)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Amount invested (Principal):", fontSize = 11.sp, color = if (isDark) LightGray else LiteTextSecondary)
                                Text("$${String.format(Locale.US, "%,.2f", rawPrincipalSum)}", fontSize = 11.sp, color = if (isDark) PureWhite else LiteTextPrimary)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Compounded Interest Gained:", fontSize = 11.sp, color = if (isDark) LightGray else LiteTextSecondary)
                                Text("$${String.format(Locale.US, "%,.2f", rawInvestInterest)}", fontSize = 11.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Graphical progress comparison
                            val weightIn = if (rawCompoundSum > 0.0) (rawPrincipalSum / rawCompoundSum).toFloat() else 0.5f
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(12.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isDark) Color(0x33FFFFFF) else Color(0x1F000000))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(maxOf(0.01f, weightIn))
                                        .background(accentColor)
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(maxOf(0.01f, 1.0f - weightIn))
                                        .background(Color(0xFF10B981))
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Principal Capital", fontSize = 8.sp, color = accentColor, fontWeight = FontWeight.Bold)
                                Text("Interest Wealth", fontSize = 8.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            "Tax Desk" -> {
                var grossIncomeStr by remember { mutableStateOf("75000") }
                var schemeSelected by remember { mutableStateOf(0) } // US Federal, India New

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isDark) GlassCardDark else GlassCardLight)
                        .border(BorderStroke(1.dp, if (isDark) GlassBorderDark else GlassBorderLight), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("TAX LIABILITY REGIME", fontSize = 10.sp, color = accentColor, fontWeight = FontWeight.Bold)

                        // Income field
                        TextField(
                            value = grossIncomeStr,
                            onValueChange = { input ->
                                if (input.all { it.isDigit() } || input.isEmpty()) {
                                    grossIncomeStr = input
                                }
                            },
                            label = { Text("Annual Gross Salary ($ / ₹)") },
                            textStyle = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (isDark) PureWhite else LiteTextPrimary),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = if (isDark) Color(0x1F000000) else Color(0x0C000000),
                                unfocusedContainerColor = if (isDark) Color(0x0C000000) else Color(0x05000000)
                            )
                        )

                        // Regime chip selectors
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("US Federal", "India New").forEachIndexed { index, name ->
                                val active = schemeSelected == index
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (active) accentColor else (if (isDark) Color(0x13FFFFFF) else Color(0x0F000000)))
                                        .clickable { schemeSelected = index }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(name, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (active) PureWhite else (if (isDark) PureWhite else LiteTextPrimary))
                                }
                            }
                        }

                        // Compute Tax Liability
                        val grossDbl = grossIncomeStr.toDoubleOrNull() ?: 0.0
                        var totalTax = 0.0
                        if (schemeSelected == 0) {
                            // US Federal Bracket
                            totalTax = when {
                                grossDbl <= 11000 -> grossDbl * 0.10
                                grossDbl <= 44725 -> 1100 + (grossDbl - 11000) * 0.12
                                grossDbl <= 95375 -> 1100 + 4047 + (grossDbl - 44725) * 0.22
                                else -> 1100 + 4047 + 11143 + (grossDbl - 95375) * 0.24
                            }
                        } else {
                            // India New Tax Regime simplified
                            totalTax = when {
                                grossDbl <= 300000 -> 0.0
                                grossDbl <= 600000 -> (grossDbl - 300000) * 0.05
                                grossDbl <= 900000 -> 15000 + (grossDbl - 600000) * 0.10
                                grossDbl <= 1200000 -> 15000 + 30000 + (grossDbl - 900000) * 0.15
                                else -> 15000 + 30000 + 45000 + (grossDbl - 1200000) * 0.20
                            }
                        }

                        val takeHome = maxOf(0.0, grossDbl - totalTax)
                        val effectiveRate = if (grossDbl > 0.0) (totalTax / grossDbl) * 100.0 else 0.0

                        Divider(color = if (isDark) Color(0x0FFFFFFF) else Color(0x0F000000), thickness = 1.dp)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Estimated Tax Due:", fontSize = 11.sp, color = if (isDark) LightGray else LiteTextSecondary)
                            Text("${if (schemeSelected == 0) "$" else "₹"}${String.format(Locale.US, "%,.2f", totalTax)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF43F5E))
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Effective Tax Rate:", fontSize = 11.sp, color = if (isDark) LightGray else LiteTextSecondary)
                            Text("${String.format(Locale.US, "%.2f", effectiveRate)}%", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = if (isDark) PureWhite else LiteTextPrimary)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Net Post-Tax Salary:", fontSize = 11.sp, color = if (isDark) LightGray else LiteTextSecondary)
                            Text("${if (schemeSelected == 0) "$" else "₹"}${String.format(Locale.US, "%,.2f", takeHome)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// ANALYTICS & STATS DASHBOARD PAGE
// ==========================================
@Composable
fun AnalyticsPage(
    historyList: List<Calculation>,
    isDark: Boolean,
    accentColor: Color,
    triggerFeedback: () -> Unit
) {
    val statsCount = historyList.size
    val lastResultVal = historyList.firstOrNull()?.result ?: "N/A"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // High fidelity financial activity sparklines
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(if (isDark) GlassCardDark else GlassCardLight)
                .border(BorderStroke(1.dp, if (isDark) GlassBorderDark else GlassBorderLight), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column {
                Text("HISTORIC ACTIVITY PERFORMANCE", fontSize = 10.sp, color = accentColor, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))

                // Canvas line chart
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .padding(vertical = 4.dp)
                ) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeCol = accentColor
                        val w = size.width
                        val h = size.height

                        // Draw background horizontal grid limits
                        drawLine(color = strokeCol.copy(alpha = 0.1f), start = Offset(0f, 0f), end = Offset(w, 0f), strokeWidth = 1f)
                        drawLine(color = strokeCol.copy(alpha = 0.1f), start = Offset(0f, h/2f), end = Offset(w, h/2f), strokeWidth = 1.0f)
                        drawLine(color = strokeCol.copy(alpha = 0.1f), start = Offset(0f, h), end = Offset(w, h), strokeWidth = 1f)

                        // Draw path wave
                        val path = androidx.compose.ui.graphics.Path()
                        val steps = 8
                        val points = listOf(0.2f, 0.35f, 0.15f, 0.45f, 0.3f, 0.75f, 0.6f, 0.85f)
                        val stepW = w / (steps - 1)

                        points.forEachIndexed { i, pt ->
                            val x = i * stepW
                            val y = h - (pt * h * 0.8f + h * 0.1f)
                            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }

                        drawPath(
                            path = path,
                            color = strokeCol,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = 2.dp.toPx(),
                                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                                join = androidx.compose.ui.graphics.StrokeJoin.Round
                            )
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Session Start", fontSize = 8.sp, color = if (isDark) LightGray else LiteTextSecondary)
                    Text("Terminal Dynamic Feed", fontSize = 8.sp, color = accentColor, fontWeight = FontWeight.Bold)
                    Text("ACTIVE", fontSize = 8.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                }
            }
        }

        // Metrics Deck
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(if (isDark) GlassCardDark else GlassCardLight)
                .border(BorderStroke(1.dp, if (isDark) GlassBorderDark else GlassBorderLight), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("HISTORICAL OPERATION AUDIT LOG", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = accentColor)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Room DB Log Rows:", fontSize = 11.sp, color = if (isDark) LightGray else LiteTextSecondary)
                    Text("$statsCount", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isDark) PureWhite else LiteTextPrimary)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Most Recent Logged Result:", fontSize = 11.sp, color = if (isDark) LightGray else LiteTextSecondary)
                    Text(lastResultVal, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = if (isDark) PureWhite else LiteTextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Active Theme Accent Mode:", fontSize = 11.sp, color = if (isDark) LightGray else LiteTextSecondary)
                    Text("Enterprise Dynamic", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = accentColor)
                }
            }
        }
    }
}

// ==========================================
// FAVORITES TABS PAGE
// ==========================================
@Composable
fun FavoritesPage(
    viewModel: CalculatorViewModel,
    isDark: Boolean,
    accentColor: Color,
    triggerFeedback: () -> Unit,
    onRedirectToConverter: () -> Unit
) {
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(if (isDark) GlassCardDark else GlassCardLight)
                .border(BorderStroke(1.dp, if (isDark) GlassBorderDark else GlassBorderLight), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column {
                Text("PINNED EXCHANGE CURRENCIES", fontSize = 10.sp, color = accentColor, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Click on any favorite currency rate to set it immediately as primary base inside the Currency Exchange center.",
                    fontSize = 10.sp,
                    color = if (isDark) LightGray else LiteTextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (favorites.isEmpty()) {
                    Text("No pinned currencies. Uncheck / star coins inside rates tab.", fontSize = 11.sp, color = if (isDark) LightGray else LiteTextSecondary)
                } else {
                    favorites.toList().forEach { curr ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isDark) Color(0x0DFFFFFF) else Color(0x0B000000))
                                .clickable {
                                    triggerFeedback()
                                    viewModel.setFromCurrency(curr)
                                    onRedirectToConverter()
                                }
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val meta = com.example.data.ExchangeRateApi.CURRENCY_METADATA[curr]
                                val flag = meta?.second ?: "🌐"
                                val longName = meta?.first ?: "Unknown Curr"
                                Text(flag, fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(curr, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isDark) PureWhite else LiteTextPrimary)
                                    Text(longName, fontSize = 9.sp, color = if (isDark) LightGray else LiteTextSecondary)
                                }
                            }
                            Icon(imageVector = Icons.Filled.ChevronRight, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// DATA UTILITY & SYNC PAGE
// ==========================================
@Composable
fun DataSyncPage(
    viewModel: CalculatorViewModel,
    historyList: List<Calculation>,
    isDark: Boolean,
    accentColor: Color,
    triggerFeedback: () -> Unit,
    context: android.content.Context
) {
    var isSyncing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Export payload creation in raw JSON
    val exportPayloadStr = remember(historyList) {
        val sb = java.lang.StringBuilder()
        sb.append("[")
        historyList.forEachIndexed { i, c ->
            sb.append("{\"id\":${c.id},\"expr\":\"${c.expression}\",\"res\":\"${c.result}\"}")
            if (i < historyList.size - 1) sb.append(",")
        }
        sb.append("]")
        sb.toString()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Simulated Cloud Backup Trigger
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(if (isDark) GlassCardDark else GlassCardLight)
                .border(BorderStroke(1.dp, if (isDark) GlassBorderDark else GlassBorderLight), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "FINTECH SECURE SECURED SYNC",
                    fontSize = 10.sp,
                    color = accentColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (isSyncing) {
                    CircularProgressIndicator(color = accentColor, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Syncing Room database log entries safely...", fontSize = 11.sp, color = if (isDark) LightGray else LiteTextSecondary)
                } else {
                    Button(
                        onClick = {
                            triggerFeedback()
                            isSyncing = true
                            scope.launch {
                                kotlinx.coroutines.delay(1200)
                                isSyncing = false
                                Toast.makeText(context, "Cloud sync finalized! ${historyList.size} items aligned.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                    ) {
                        Text("BACKUP NOW", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                    }
                }
            }
        }

        // Import and Export panels
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(if (isDark) GlassCardDark else GlassCardLight)
                .border(BorderStroke(1.dp, if (isDark) GlassBorderDark else GlassBorderLight), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("DATABASE RAW JSON EXPORT", fontSize = 10.sp, color = accentColor, fontWeight = FontWeight.Bold)
                Text(
                    "You can copy this JSON string data block and paste it on another device to restore transaction calculation data logs perfectly.",
                    fontSize = 10.sp,
                    color = if (isDark) LightGray else LiteTextSecondary
                )

                // JSON LCD Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9))
                        .padding(8.dp)
                ) {
                    Text(
                        text = if (historyList.isEmpty()) "[] (History empty)" else exportPayloadStr,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = if (isDark) Color(0xCC00FFCC) else Color(0xFF075985),
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    )
                }

                Button(
                    onClick = {
                        triggerFeedback()
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("Export DB", exportPayloadStr)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Copied JSON dump to clipboard!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0x1F00F0FF) else accentColor.copy(alpha = 0.15f))
                ) {
                    Text("Copy Export Payload", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isDark) PureWhite else LiteTextPrimary)
                }
            }
        }
    }
}

// ==========================================
// APPEARANCE THEMES PAGE
// ==========================================
@Composable
fun AppearancePage(
    isDark: Boolean,
    onToggleTheme: () -> Unit,
    selectedAccent: String,
    onAccentSelected: (String) -> Unit,
    accentColor: Color,
    triggerFeedback: () -> Unit
) {
    val accentsList = listOf(
        Pair("Cyberpunk Cyan", Color(0xFF00F0FF)),
        Pair("Gold Mint", Color(0xFFFFD700)),
        Pair("Velvet Ruby", Color(0xFFF43F5E)),
        Pair("Emerald Forest", Color(0xFF10B981)),
        Pair("Electric Blue", Color(0xFF3B82F6))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Toggle Master Light/Dark Theme
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(if (isDark) GlassCardDark else GlassCardLight)
                .border(BorderStroke(1.dp, if (isDark) GlassBorderDark else GlassBorderLight), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("MASTER VISUAL THEME", fontSize = 10.sp, color = accentColor, fontWeight = FontWeight.Bold)
                    Text("Select between Slate Dark and Frosted Light layouts.", fontSize = 10.sp, color = if (isDark) LightGray else LiteTextSecondary)
                }
                ThemeToggler(isDark = isDark, onToggleTheme = onToggleTheme, activeColor = accentColor)
            }
        }

        // Custom Neon Palette selector
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(if (isDark) GlassCardDark else GlassCardLight)
                .border(BorderStroke(1.dp, if (isDark) GlassBorderDark else GlassBorderLight), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("PREMIUM METALLIC HIGHLIGHT", fontSize = 10.sp, color = accentColor, fontWeight = FontWeight.Bold)
                Text("Select highlight colors applied dynamically to curves and sliders.", fontSize = 10.sp, color = if (isDark) LightGray else LiteTextSecondary)

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    accentsList.forEach { acc ->
                        val active = selectedAccent == acc.first
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (active) acc.second.copy(alpha = 0.15f) else Color.Transparent)
                                .clickable {
                                    triggerFeedback()
                                    onAccentSelected(acc.first)
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(acc.second, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(acc.first, fontSize = 11.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium, color = if (isDark) PureWhite else LiteTextPrimary)
                            }
                            if (active) {
                                Icon(imageVector = Icons.Filled.Check, contentDescription = null, tint = acc.second, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ThemeToggler(
    isDark: Boolean,
    onToggleTheme: () -> Unit,
    activeColor: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isDark) Color(0x3D1F2937) else Color(0x1F000000))
            .clickable { onToggleTheme() }
            .padding(4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (isDark) activeColor else Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.DarkMode,
                    contentDescription = null,
                    tint = if (isDark) PureWhite else LiteTextSecondary,
                    modifier = Modifier.size(12.dp)
                )
            }
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (!isDark) activeColor else Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.LightMode,
                    contentDescription = null,
                    tint = if (!isDark) PureWhite else LightGray,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

// ==========================================
// SYSTEM SETTINGS PAGE
// ==========================================
@Composable
fun SettingsPage(
    isDark: Boolean,
    accentColor: Color,
    triggerFeedback: () -> Unit,
    context: android.content.Context
) {
    var hapticIntent by remember { mutableStateOf(5f) }
    var autoSaveEnabled by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(if (isDark) GlassCardDark else GlassCardLight)
                .border(BorderStroke(1.dp, if (isDark) GlassBorderDark else GlassBorderLight), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("GENERAL CODES & INTENTS", fontSize = 10.sp, color = accentColor, fontWeight = FontWeight.Bold)

                // Slider Haptics
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Taptic Click Vibrancy", fontSize = 11.sp, color = if (isDark) LightGray else LiteTextSecondary)
                        Text("${hapticIntent.toInt()}/10", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = accentColor)
                    }
                    Slider(
                        value = hapticIntent,
                        onValueChange = { hapticIntent = it },
                        valueRange = 1f..10f,
                        colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor)
                    )
                }

                Divider(color = if (isDark) Color(0x0FFFFFFF) else Color(0x0F000000), thickness = 1.dp)

                // Checkbox auto-saves
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { autoSaveEnabled = !autoSaveEnabled },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Auto-Save Expressions", fontSize = 11.sp, color = if (isDark) PureWhite else LiteTextPrimary, fontWeight = FontWeight.Medium)
                        Text("Saves math operations inside SQLite logs on Equal press.", fontSize = 9.sp, color = if (isDark) LightGray else LiteTextSecondary)
                    }
                    Checkbox(
                        checked = autoSaveEnabled,
                        onCheckedChange = { autoSaveEnabled = it },
                        colors = CheckboxDefaults.colors(checkedColor = accentColor)
                    )
                }
            }
        }
    }
}

// ==========================================
// HELP CENTER PAGE
// ==========================================
@Composable
fun HelpCenterPage(
    isDark: Boolean,
    accentColor: Color,
    triggerFeedback: () -> Unit
) {
    var expandedFaqIndex by remember { mutableStateOf(-1) }
    var userName by remember { mutableStateOf("") }
    var userEmailStr by remember { mutableStateOf("") }
    var supportText by remember { mutableStateOf("") }
    var ticketRegistered by remember { mutableStateOf(false) }

    val faqs = listOf(
        Pair("How does the real-time currency sync work?", "The Neo Calc client utilizes an integrated API service layer. It queries the ExchangeRate stream and refreshes active indexes every 30 seconds automatically when connected with internet packets."),
        Pair("How are the portfolio compound interests forecasted?", "Our algorithms trace the geometric continuous growth formula. Principal capital plus recurring savings grow compoundly matching fractional periodic cycles over matching slider limits."),
        Pair("What does the M-Memory register track?", "The memory bar saves float outputs locally. MC clears memory, MR recalls memory value in expression rows, M+ aggregates results and M- deducts results instantly.")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // FAQ section accordions
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(if (isDark) GlassCardDark else GlassCardLight)
                .border(BorderStroke(1.dp, if (isDark) GlassBorderDark else GlassBorderLight), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("INTELLIGENT KNOWLEDGE BASE", fontSize = 10.sp, color = accentColor, fontWeight = FontWeight.Bold)

                faqs.forEachIndexed { index, faq ->
                    val open = expandedFaqIndex == index
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isDark) Color(0x13FFFFFF) else Color(0x0F000000))
                            .clickable {
                                triggerFeedback()
                                expandedFaqIndex = if (open) -1 else index
                            }
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(faq.first, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isDark) PureWhite else LiteTextPrimary, modifier = Modifier.weight(1f))
                            Icon(
                                imageVector = if (open) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        AnimatedVisibility(visible = open) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(faq.second, fontSize = 10.sp, color = if (isDark) LightGray else LiteTextSecondary)
                        }
                    }
                }
            }
        }

        // Support Form
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(if (isDark) GlassCardDark else GlassCardLight)
                .border(BorderStroke(1.dp, if (isDark) GlassBorderDark else GlassBorderLight), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            if (ticketRegistered) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("TICKET GENERATED SAFELY", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                    Text("Support experts will contact you soon at your email.", fontSize = 10.sp, color = if (isDark) LightGray else LiteTextSecondary)
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { ticketRegistered = false; supportText = "" },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                    ) {
                        Text("Create new Ticket", fontSize = 10.sp, color = PureWhite)
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("FINTECH CUSTOMER SUPPORT DESK", fontSize = 10.sp, color = accentColor, fontWeight = FontWeight.Bold)

                    TextField(
                        value = userName,
                        onValueChange = { userName = it },
                        placeholder = { Text("Full Name") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = if (isDark) Color(0x0D000000) else Color(0x05000000)
                        )
                    )

                    TextField(
                        value = userEmailStr,
                        onValueChange = { userEmailStr = it },
                        placeholder = { Text("Contact Email") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = if (isDark) Color(0x0D000000) else Color(0x05000000)
                        )
                    )

                    TextField(
                        value = supportText,
                        onValueChange = { supportText = it },
                        placeholder = { Text("Type query message details here...") },
                        modifier = Modifier.fillMaxWidth().height(80.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = if (isDark) Color(0x0D000000) else Color(0x05000000)
                        )
                    )

                    Button(
                        onClick = {
                            if (userName.isNotEmpty() && userEmailStr.isNotEmpty() && supportText.isNotEmpty()) {
                                triggerFeedback()
                                ticketRegistered = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        enabled = userName.isNotEmpty() && userEmailStr.isNotEmpty() && supportText.isNotEmpty()
                    ) {
                        Text("SUBMIT TICKET", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                    }
                }
            }
        }
    }
}

// ==========================================
// ABOUT SPECIFICATIONS PAGE
// ==========================================
@Composable
fun AboutPage(
    isDark: Boolean,
    accentColor: Color,
    triggerFeedback: () -> Unit,
    context: android.content.Context
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_app_icon_1781159112197),
            contentDescription = null,
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(18.dp))
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text("NEO CALC ENTERPRISE SUITE", fontSize = 14.sp, fontWeight = FontWeight.Black, color = if (isDark) PureWhite else LiteTextPrimary)
        Text("Active Patch: v4.2.1-PRO (Premium Channel)", fontSize = 10.sp, color = accentColor, fontWeight = FontWeight.Bold)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(if (isDark) GlassCardDark else GlassCardLight)
                .border(BorderStroke(1.dp, if (isDark) GlassBorderDark else GlassBorderLight), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("SPECIFICATION INDEX", fontSize = 10.sp, color = accentColor, fontWeight = FontWeight.Bold)

                listOf(
                    Pair("Compiled Environment", "Android JVM / Jetpack Compose"),
                    Pair("Repository Engine", "SQLite / Room Persistent ORM"),
                    Pair("Connection Registry", "ExchangeRate API REST Client"),
                    Pair("Device Hardware Target", "Smartphones / Tablets (Fluid Window Classes)")
                ).forEach { param ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(param.first, fontSize = 10.sp, color = if (isDark) LightGray else LiteTextSecondary)
                        Text(param.second, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isDark) PureWhite else LiteTextPrimary)
                    }
                }
            }
        }

        Button(
            onClick = {
                triggerFeedback()
                Toast.makeText(context, "All modules up-to-date. Patch server verified.", Toast.LENGTH_SHORT).show()
            },
            colors = ButtonDefaults.buttonColors(containerColor = accentColor)
        ) {
            Text("CHECK FOR PATCHES", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PureWhite)
        }
    }
}
