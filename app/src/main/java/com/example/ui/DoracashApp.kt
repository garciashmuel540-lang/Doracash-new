package com.example.ui

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.auth.AuthScreen
import com.example.auth.signOutUser
import com.example.data.BankCard
import com.example.data.BillStatus
import com.example.data.CurrencyCode
import com.example.data.DarkThemeMode
import com.example.data.DoracashRepository
import com.example.data.DoracashSnapshot
import com.example.data.FinanceUtils
import com.example.data.SavingsGoal
import com.example.data.TxType
import com.example.data.UiState
import com.example.data.WalletAccount
import com.example.ai.DoraemonTipUiState
import com.example.ui.components.BellAlertButton
import com.example.ui.components.DoraemonAssistantSheet
import com.example.ui.components.DoraemonGesture
import com.example.ui.components.DoraemonImportantPopup
import com.example.ui.components.DoraemonMiniHeadIcon
import com.example.ui.theme.DoraMintGreen
import com.example.ui.screens.AccountEditDialog
import com.example.ui.screens.BudgetCreateDialog
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.CardChargeDialog
import com.example.ui.screens.CardEditDialog
import com.example.ui.screens.CardPayDebtDialog
import com.example.ui.screens.CardsWalletScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.GoalCreateDialog
import com.example.ui.screens.GoalDepositDialog
import com.example.ui.screens.GoalRecoverDialog
import com.example.ui.screens.GoalsBudgetsScreen
import com.example.ui.screens.ReconcileScreen
import com.example.ui.screens.ScheduleBillDialog
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TransactionModalDialog
import com.example.ui.screens.TransactionsScreen
import com.example.ui.theme.DoracashTheme
import com.example.ui.theme.DoraDoorPink
import com.example.ui.theme.DoraSkyBlue
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.wallet.CardDetector
import com.example.wallet.DetectedCardMetadata
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

enum class DoracashTab(val route: String, val label: String, val icon: ImageVector) {
    DASHBOARD("dashboard", "Inicio", Icons.Filled.Home),
    TRANSACTIONS("transactions", "Movimientos", Icons.AutoMirrored.Filled.ReceiptLong),
    CARDS("cards", "Tarjetas", Icons.Filled.CreditCard),
    RECONCILE("reconcile", "Arqueo", Icons.Filled.Calculate),
    CALENDAR("calendar", "Pagos", Icons.Filled.CalendarMonth),
    GOALS("goals", "Ahorros", Icons.Filled.Savings),
    SETTINGS("settings", "Cuentas", Icons.Filled.Settings);

    companion object {
        val compactBottomTabs = listOf(DASHBOARD, TRANSACTIONS, CARDS, CALENDAR, GOALS, SETTINGS)
        fun fromRoute(route: String): DoracashTab = entries.firstOrNull { it.route == route } ?: DASHBOARD
    }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    var currentUser by remember { mutableStateOf<FirebaseUser?>(auth.currentUser) }

    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            currentUser = firebaseAuth.currentUser
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    val user = currentUser
    if (user == null) {
        DoracashTheme {
            AuthScreen(onAuthSuccess = { currentUser = auth.currentUser })
        }
    } else {
        key(user.uid) {
            AuthenticatedDoracashShell(
                currentUserId = user.uid,
                userDisplayName = user.displayName ?: "Usuario Doracash",
                userEmail = user.email
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedDoracashShell(
    currentUserId: String,
    userDisplayName: String,
    userEmail: String?
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: DoracashViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                val prefs = app.getSharedPreferences("doracash_prefs", Context.MODE_PRIVATE)
                DoracashViewModel(
                    repository = DoracashRepository(db),
                    currentUserId = currentUserId,
                    displayName = userDisplayName,
                    prefs = prefs
                )
            }
        }
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val feedback by viewModel.feedbackMessage.collectAsStateWithLifecycle()
    val doraemonEnabled by viewModel.doraemonEnabled.collectAsStateWithLifecycle()
    val doraemonTipState by viewModel.doraemonTipState.collectAsStateWithLifecycle()
    val doraemonMessages by viewModel.doraemonMessages.collectAsStateWithLifecycle()
    val doraemonChatState by viewModel.doraemonChatState.collectAsStateWithLifecycle()
    val requestedTab by viewModel.requestedTabNavigation.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(feedback) {
        feedback?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearFeedback()
        }
    }

    val snapshot = (uiState as? UiState.Success)?.data
    val themeMode = snapshot?.let { DarkThemeMode.safeValueOf(it.profile.darkThemeMode) } ?: DarkThemeMode.SYSTEM
    val useDarkTheme = when (themeMode) {
        DarkThemeMode.SYSTEM -> isSystemInDarkTheme()
        DarkThemeMode.LIGHT -> false
        DarkThemeMode.DARK -> true
    }

    var currentTab by remember { mutableStateOf(DoracashTab.DASHBOARD) }
    var txModalType by remember { mutableStateOf<TxType?>(null) }
    var billModalDate by remember { mutableStateOf<String?>(null) }
    var accountModalTarget by remember { mutableStateOf<Pair<Boolean, WalletAccount?>>(false to null) }
    var showBudgetModal by remember { mutableStateOf(false) }
    var showGoalModal by remember { mutableStateOf(false) }
    var depositGoalTarget by remember { mutableStateOf<SavingsGoal?>(null) }
    var recoverGoalTarget by remember { mutableStateOf<SavingsGoal?>(null) }
    var cardModalTarget by remember { mutableStateOf<Triple<Boolean, BankCard?, DetectedCardMetadata?>>(Triple(false, null, null)) }
    var chargeCardTarget by remember { mutableStateOf<BankCard?>(null) }
    var payCardTarget by remember { mutableStateOf<BankCard?>(null) }
    var showDoraemonSheet by remember { mutableStateOf(false) }
    var isDoraemonOverlayVisible by rememberSaveable { mutableStateOf(false) }
    var overlayGestureOverride by remember { mutableStateOf<DoraemonGesture?>(null) }

    LaunchedEffect(snapshot, doraemonEnabled) {
        val snap = snapshot ?: return@LaunchedEffect
        if (doraemonEnabled && snap.accounts.isNotEmpty()) {
            delay(1500L)
            viewModel.evaluateImportantMoment(snap)
        }
    }

    LaunchedEffect(requestedTab) {
        requestedTab?.let { target ->
            currentTab = target
            viewModel.consumeTabNavigation()
        }
    }

    BackHandler(enabled = currentTab != DoracashTab.DASHBOARD && !showDoraemonSheet) {
        currentTab = DoracashTab.DASHBOARD
    }

    DoracashTheme(darkTheme = useDarkTheme) {
        val alertCount = remember(snapshot?.scheduledBills) {
            if (snapshot == null) 0
            else {
                val now = Calendar.getInstance()
                FinanceUtils.occurrencesForMonth(
                    snapshot.scheduledBills,
                    now.get(Calendar.YEAR),
                    now.get(Calendar.MONTH)
                ).count { it.status == BillStatus.OVERDUE || it.status == BillStatus.SOON }
            }
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWide = maxWidth >= 600.dp

            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = if (currentTab == DoracashTab.DASHBOARD) "Doracash" else currentTab.label,
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = DoraSkyBlue
                                )
                                Text(
                                    text = "Tu bolsillo al instante",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        actions = {
                            if (snapshot != null) {
                                Surface(
                                    onClick = { viewModel.toggleDisplayCurrency(snapshot) },
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier
                                        .padding(end = 6.dp)
                                        .testTag("currency_toggle_button")
                                ) {
                                    Text(
                                        text = snapshot.profile.displayCurrency,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)
                                    )
                                }
                            }
                            BellAlertButton(
                                alertCount = alertCount,
                                onClick = { currentTab = DoracashTab.CALENDAR },
                                modifier = Modifier
                                    .padding(end = 6.dp)
                                    .testTag("bell_alerts_button")
                            )
                            // Trigger mechanism to show/hide the DoraemonAssistant overlay on the main screen
                            Surface(
                                onClick = {
                                    val nextVisible = !isDoraemonOverlayVisible
                                    isDoraemonOverlayVisible = nextVisible
                                    if (nextVisible) {
                                        overlayGestureOverride = DoraemonGesture.SALUTE
                                    } else {
                                        overlayGestureOverride = null
                                        viewModel.dismissDoraemonNotice()
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                color = if (isDoraemonOverlayVisible) {
                                    DoraSkyBlue.copy(alpha = 0.22f)
                                } else if (doraemonEnabled) {
                                    DoraSkyBlue.copy(alpha = 0.12f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                modifier = Modifier
                                    .padding(end = 6.dp)
                                    .size(48.dp)
                                    .testTag("doraemon_toggle_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    DoraemonMiniHeadIcon(
                                        active = isDoraemonOverlayVisible || doraemonEnabled,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(5.dp)
                                            .size(10.dp)
                                            .background(
                                                color = when {
                                                    isDoraemonOverlayVisible -> DoraMintGreen
                                                    doraemonEnabled -> DoraSkyBlue
                                                    else -> Color(0xFF94A3B8)
                                                },
                                                shape = CircleShape
                                            )
                                            .border(1.5.dp, MaterialTheme.colorScheme.background, CircleShape)
                                    )
                                }
                            }
                            if (snapshot != null) {
                                Surface(
                                    onClick = { showDoraemonSheet = true },
                                    shape = RoundedCornerShape(14.dp),
                                    color = DoraSkyBlue.copy(alpha = 0.14f),
                                    modifier = Modifier
                                        .padding(end = 8.dp)
                                        .size(48.dp)
                                        .testTag("doraemon_chat_button")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Chat,
                                            contentDescription = "Hablar con Doraemon IA",
                                            tint = DoraSkyBlue,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        )
                    )
                },
                bottomBar = {
                    if (!isWide) {
                        NavigationBar(
                            windowInsets = WindowInsets.navigationBars,
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            DoracashTab.compactBottomTabs.forEach { tab ->
                                NavigationBarItem(
                                    selected = currentTab == tab,
                                    onClick = { currentTab = tab },
                                    icon = { Icon(tab.icon, contentDescription = tab.label) },
                                    label = { Text(tab.label, maxLines = 1) },
                                    modifier = Modifier.testTag("nav_${tab.route}")
                                )
                            }
                        }
                    }
                },
                floatingActionButton = {
                    if (snapshot != null) {
                        if (isWide) {
                            ExtendedFloatingActionButton(
                                onClick = { txModalType = TxType.EXPENSE },
                                containerColor = DoraDoorPink,
                                contentColor = Color.White,
                                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                                text = { Text(stringResource(R.string.action_magic_door)) },
                                modifier = Modifier.testTag("puerta_magica_fab")
                            )
                        } else {
                            FloatingActionButton(
                                onClick = { txModalType = TxType.EXPENSE },
                                containerColor = DoraDoorPink,
                                contentColor = Color.White,
                                modifier = Modifier.testTag("puerta_magica_fab")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = stringResource(R.string.cd_magic_door)
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (isWide) {
                        NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
                            Spacer(modifier = Modifier.height(12.dp))
                            DoracashTab.entries.forEach { tab ->
                                NavigationRailItem(
                                    selected = currentTab == tab,
                                    onClick = { currentTab = tab },
                                    icon = { Icon(tab.icon, contentDescription = tab.label) },
                                    label = { Text(tab.label) },
                                    modifier = Modifier.testTag("rail_${tab.route}")
                                )
                            }
                        }
                    }

                    Box(
                        contentAlignment = Alignment.TopCenter,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                    ) {
                        when (val state = uiState) {
                            is UiState.Loading -> {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    CircularProgressIndicator(color = DoraSkyBlue)
                                }
                            }
                            is UiState.Error -> {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(24.dp)
                                ) {
                                    Text(
                                        text = "No se pudo conectar al bolsillo",
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = state.message,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(onClick = {
                                        signOutUser(credentialManager, scope)
                                    }) {
                                        Text("Cerrar sesión")
                                    }
                                }
                            }
                            is UiState.Success -> {
                                val data = state.data
                                val contentModifier = Modifier
                                    .widthIn(max = 760.dp)
                                    .fillMaxWidth()

                                when (currentTab) {
                                    DoracashTab.DASHBOARD -> DashboardScreen(
                                        snapshot = data,
                                        onEdit = viewModel::editTransaction,
                                        onSettle = { viewModel.settlePending(it, snapshot.profile.exchangeRate) },
                                        onOpenTxModal = { txModalType = it },
                                        onNavigateTab = { currentTab = DoracashTab.fromRoute(it) },
                                        onPayBill = { occ ->
                                            val acc = data.accounts.firstOrNull { it.id == occ.bill.accountId }
                                            viewModel.payScheduledBill(
                                                occ.bill,
                                                occ.dateIso,
                                                acc,
                                                data.profile.exchangeRate,
                                                data.goals
                                            )
                                        },
                                        onLoadDemo = { viewModel.loadDemoData() },
                                        modifier = contentModifier
                                    )
                                    DoracashTab.TRANSACTIONS -> TransactionsScreen(
                                        snapshot = data,
                                        onDeleteTx = { tx ->
                                            viewModel.deleteTransaction(
                                                tx,
                                                data.accounts,
                                                data.profile.exchangeRate,
                                                data.goals
                                            )
                                        },
                                        modifier = contentModifier
                                    )
                                    DoracashTab.CARDS -> CardsWalletScreen(
                                        snapshot = data,
                                        onOpenNewCard = { cardModalTarget = Triple(true, null, null) },
                                        onImportDetectedCard = { detected ->
                                            val dup = CardDetector.isDuplicateCard(data.cards, detected)
                                            cardModalTarget = Triple(true, dup, detected)
                                        },
                                        onOpenEditCard = { cardModalTarget = Triple(true, it, null) },
                                        onOpenChargeCard = { chargeCardTarget = it },
                                        onOpenPayCard = { payCardTarget = it },
                                        onToggleFrozen = { viewModel.toggleCardFrozen(it) },
                                        onToggleAutoDebit = { viewModel.toggleCardAutoDebit(it) },
                                        onToggleWalletLink = { viewModel.toggleCardWalletLink(it) },
                                        onDeleteCard = { viewModel.deleteCard(it.id) },
                                        onFeedback = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } },
                                        modifier = contentModifier
                                    )
                                    DoracashTab.RECONCILE -> ReconcileScreen(
                                        snapshot = data,
                                        onSaveReconciliation = { acc, counted, adjust, note ->
                                            viewModel.saveReconciliation(acc, counted, adjust, note)
                                        },
                                        modifier = contentModifier
                                    )
                                    DoracashTab.CALENDAR -> CalendarScreen(
                                        snapshot = data,
                                        onPayBill = { occ ->
                                            val acc = data.accounts.firstOrNull { it.id == occ.bill.accountId }
                                            viewModel.payScheduledBill(
                                                occ.bill,
                                                occ.dateIso,
                                                acc,
                                                data.profile.exchangeRate,
                                                data.goals
                                            )
                                        },
                                        onDeleteBill = { viewModel.deleteScheduledBill(it) },
                                        onOpenNewBillModal = { billModalDate = it },
                                        modifier = contentModifier
                                    )
                                    DoracashTab.GOALS -> GoalsBudgetsScreen(
                                        snapshot = data,
                                        onOpenNewBudget = { showBudgetModal = true },
                                        onDeleteBudget = { viewModel.deleteBudget(it.id) },
                                        onOpenNewGoal = { showGoalModal = true },
                                        onOpenDepositGoal = { depositGoalTarget = it },
                                        onOpenRecoverGoal = { recoverGoalTarget = it },
                                        onDeleteGoal = { viewModel.deleteGoal(it.id) },
                                        modifier = contentModifier
                                    )
                                    DoracashTab.SETTINGS -> SettingsScreen(
                                        snapshot = data,
                                        userEmail = userEmail,
                                        doraemonEnabled = doraemonEnabled,
                                        onToggleDoraemon = { enable ->
                                            viewModel.setDoraemonEnabled(enable, data)
                                            isDoraemonOverlayVisible = enable
                                            overlayGestureOverride = if (enable) DoraemonGesture.SALUTE else null
                                        },
                                        onOpenDoraemonChat = { showDoraemonSheet = true },
                                        onOpenAccountModal = { accountModalTarget = true to it },
                                        onDeleteAccount = { viewModel.deleteAccount(it.id) },
                                        onUpdateSettings = { c, r, t -> viewModel.updateSettings(c, r, t) },
                                        onLoadDemoData = { viewModel.loadDemoData() },
                                        onClearAllData = { viewModel.clearAllData() },
                                        onSignOut = { signOutUser(credentialManager, scope) },
                                        modifier = contentModifier
                                    )
                                }
                            }
                        }

                        if (snapshot != null) {
                            val disp = CurrencyCode.safeValueOf(snapshot.profile.displayCurrency)
                            val netWorth = remember(snapshot.accounts, disp, snapshot.profile.exchangeRate) {
                                snapshot.accounts.sumOf {
                                    FinanceUtils.convertCurrency(it.balance, it.currencyCode, disp, snapshot.profile.exchangeRate)
                                }
                            }
                            val defaultGreeting = "¡Hola! Tienes ${snapshot.accounts.size} cuentas y saldo total de ${FinanceUtils.formatMoney(netWorth, disp)}."

                            DoraemonImportantPopup(
                                visible = isDoraemonOverlayVisible,
                                tipState = doraemonTipState,
                                defaultThoughtText = defaultGreeting,
                                gestureOverride = overlayGestureOverride,
                                onGestureSelect = { chosen -> overlayGestureOverride = chosen },
                                onDismissNotice = {
                                    isDoraemonOverlayVisible = false
                                    overlayGestureOverride = null
                                    viewModel.dismissDoraemonNotice()
                                },
                                onRetryCheck = { viewModel.evaluateImportantMoment(snapshot, force = true) },
                                onOpenAssistant = {
                                    isDoraemonOverlayVisible = false
                                    overlayGestureOverride = null
                                    viewModel.dismissDoraemonNotice()
                                    showDoraemonSheet = true
                                },
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(start = 12.dp, bottom = 10.dp)
                            )
                        }
                    }
                }
            }

            // Modals
            if (snapshot != null) {
                txModalType?.let { type ->
                    TransactionModalDialog(
                        initialType = type,
                        accounts = snapshot.accounts,
                        goals = snapshot.goals,
                        cards = snapshot.cards,
                        exchangeRate = snapshot.profile.exchangeRate,
                        onDismiss = { txModalType = null },
                        onSave = { t, amt, acc, toAcc, catId, note, date, sType, targetGoal, customSavingsName, applyAutoDebit, pending ->
                            if (pending) viewModel.savePending(com.example.data.FinanceTransaction(txType = t.name, amount = FinanceUtils.round2(amt), currency = acc.currency, accountId = acc.id, toAccountId = toAcc?.id, categoryId = catId, note = note, dateIso = date)) else viewModel.addTransaction(
                                txType = t,
                                amount = amt,
                                account = acc,
                                toAccount = toAcc,
                                categoryId = catId,
                                note = note,
                                dateIso = date,
                                exchangeRate = snapshot.profile.exchangeRate,
                                savingsType = sType,
                                targetSavingsGoal = targetGoal,
                                existingGoals = snapshot.goals,
                                customSavingsName = customSavingsName,
                                existingCards = snapshot.cards,
                                applyAutoCardDebit = applyAutoDebit
                            )
                            txModalType = null
                        }
                    )
                }

                billModalDate?.let { dIso ->
                    ScheduleBillDialog(
                        defaultDateIso = dIso,
                        accounts = snapshot.accounts,
                        goals = snapshot.goals,
                        onDismiss = { billModalDate = null },
                        onSave = { title, amt, curr, accId, catId, freq, start ->
                            viewModel.addScheduledBill(title, amt, curr, accId, catId, freq, start)
                            billModalDate = null
                        }
                    )
                }

                if (accountModalTarget.first) {
                    val existing = accountModalTarget.second
                    AccountEditDialog(
                        existing = existing,
                        onDismiss = { accountModalTarget = false to null },
                        onSave = { name, type, curr, bal, iconKey ->
                            viewModel.saveAccount(existing, name, type, curr, bal, iconKey)
                            accountModalTarget = false to null
                        }
                    )
                }

                if (showBudgetModal) {
                    BudgetCreateDialog(
                        onDismiss = { showBudgetModal = false },
                        onSave = { catId, limit ->
                            val existing = snapshot.budgets.firstOrNull { it.categoryId == catId }
                            viewModel.saveBudget(existing, catId, limit)
                            showBudgetModal = false
                        }
                    )
                }

                if (showGoalModal) {
                    GoalCreateDialog(
                        onDismiss = { showGoalModal = false },
                        onSave = { name, target, iconKey, deadline, initialNio ->
                            viewModel.addGoal(name, target, iconKey, deadline, initialNio)
                            showGoalModal = false
                        }
                    )
                }

                depositGoalTarget?.let { goal ->
                    GoalDepositDialog(
                        goal = goal,
                        accounts = snapshot.accounts,
                        onDismiss = { depositGoalTarget = null },
                        onDeposit = { acc, amt ->
                            viewModel.depositToGoal(goal, acc, amt, snapshot.profile.exchangeRate)
                            depositGoalTarget = null
                        }
                    )
                }

                recoverGoalTarget?.let { goal ->
                    GoalRecoverDialog(
                        goal = goal,
                        accounts = snapshot.accounts,
                        onDismiss = { recoverGoalTarget = null },
                        onRecover = { acc, amt ->
                            viewModel.recoverFromGoal(goal, acc, amt, snapshot.profile.exchangeRate)
                            recoverGoalTarget = null
                        }
                    )
                }

                if (cardModalTarget.first) {
                    val existingCard = cardModalTarget.second
                    val prefill = cardModalTarget.third
                    CardEditDialog(
                        existing = existingCard,
                        accounts = snapshot.accounts,
                        prefill = prefill,
                        onDismiss = { cardModalTarget = Triple(false, null, null) },
                        onSave = { alias, bankName, kind, net, last4, curr, debt, limit, linkedId, autoOn, autoMode, autoVal, gWallet, frozen, cutDay, dueDay, colorTheme ->
                            viewModel.saveCard(
                                editCard = existingCard,
                                alias = alias,
                                bankName = bankName,
                                cardKind = kind,
                                network = net,
                                last4 = last4,
                                currency = curr,
                                debtBalance = debt,
                                creditLimit = limit,
                                linkedAccountId = linkedId,
                                autoDebitEnabled = autoOn,
                                autoDebitMode = autoMode,
                                autoDebitValue = autoVal,
                                googleWalletLinked = gWallet,
                                cardFrozen = frozen,
                                cutoffDay = cutDay,
                                dueDay = dueDay,
                                colorTheme = colorTheme
                            )
                            cardModalTarget = Triple(false, null, null)
                        }
                    )
                }

                chargeCardTarget?.let { card ->
                    CardChargeDialog(
                        card = card,
                        onDismiss = { chargeCardTarget = null },
                        onConfirmCharge = { amt, catId, note, dateIso ->
                            viewModel.recordCardCharge(
                                card = card,
                                amountInCardCurrency = amt,
                                categoryId = catId,
                                note = note,
                                dateIso = dateIso,
                                accounts = snapshot.accounts,
                                exchangeRate = snapshot.profile.exchangeRate
                            )
                            chargeCardTarget = null
                        }
                    )
                }

                payCardTarget?.let { card ->
                    CardPayDebtDialog(
                        card = card,
                        accounts = snapshot.accounts,
                        exchangeRate = snapshot.profile.exchangeRate,
                        onDismiss = { payCardTarget = null },
                        onConfirmPay = { payAcc, amtInAccCurr ->
                            viewModel.payCardDebt(
                                card = card,
                                payAccount = payAcc,
                                amountInAccountCurrency = amtInAccCurr,
                                exchangeRate = snapshot.profile.exchangeRate
                            )
                            payCardTarget = null
                        }
                    )
                }

                if (showDoraemonSheet) {
                    DoraemonAssistantSheet(
                        messages = doraemonMessages,
                        chatState = doraemonChatState,
                        doraemonEnabled = doraemonEnabled,
                        onToggleEnabled = { viewModel.setDoraemonEnabled(it, snapshot) },
                        onSendMessage = { text -> viewModel.sendDoraemonMessage(text, snapshot) },
                        onClearChat = { viewModel.clearDoraemonChat() },
                        onDismiss = { showDoraemonSheet = false }
                    )
                }
            }
        }
    }
}
