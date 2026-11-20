package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.auth.authStateFlow
import com.example.auth.onGoogleSignInClicked
import com.example.auth.signOutUser
import com.example.data.ExpenseRepository
import com.example.data.FirestoreExpenseDataSource
import com.example.data.local.CategoryConfigRepository
import com.example.data.local.GuestExpenseDatabase
import com.example.data.local.LocalGuestRepository
import com.example.model.Expense
import com.example.model.PaymentMethod
import com.example.model.UiState
import com.example.ui.viewmodel.ExpenseViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

private const val PREFS_NAME = "gasto_diario_prefs"
private const val KEY_GUEST_MODE = "is_guest_mode"

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val context = LocalContext.current
    val prefs = remember(context) {
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    var isGuestMode by remember {
        mutableStateOf(prefs.getBoolean(KEY_GUEST_MODE, false))
    }

    val currentUser by auth.authStateFlow()
        .collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user != null) {
        LaunchedEffect(user.uid) {
            if (isGuestMode) {
                prefs.edit().putBoolean(KEY_GUEST_MODE, false).apply()
                isGuestMode = false
            }
        }
        key(user.uid) {
            ExpenseControlRoot(
                sessionKey = user.uid,
                currentUser = user,
                onExitGuestMode = {}
            )
        }
    } else if (isGuestMode) {
        key(LocalGuestRepository.GUEST_USER_ID) {
            ExpenseControlRoot(
                sessionKey = LocalGuestRepository.GUEST_USER_ID,
                currentUser = null,
                onExitGuestMode = {
                    prefs.edit().putBoolean(KEY_GUEST_MODE, false).apply()
                    isGuestMode = false
                }
            )
        }
    } else {
        AuthScreen(
            onAuthSuccess = {
                prefs.edit().putBoolean(KEY_GUEST_MODE, false).apply()
                isGuestMode = false
            },
            onContinueAsGuest = {
                prefs.edit().putBoolean(KEY_GUEST_MODE, true).apply()
                isGuestMode = true
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseControlRoot(
    sessionKey: String,
    currentUser: FirebaseUser?,
    onExitGuestMode: () -> Unit,
) {
    val isGuest = currentUser == null

    val viewModel: ExpenseViewModel = viewModel(
        key = sessionKey,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val roomDb = GuestExpenseDatabase.getInstance(app)
                val dataSource = if (currentUser != null) {
                    val databaseId = app.getString(R.string.firestore_database_id)
                    val db = FirebaseFirestore.getInstance(databaseId)
                    FirestoreExpenseDataSource(ExpenseRepository(db), currentUser.uid)
                } else {
                    LocalGuestRepository(roomDb.guestExpenseDao())
                }
                val categoryRepository = CategoryConfigRepository(roomDb.categoryConfigDao())
                ExpenseViewModel(dataSource, categoryRepository, sessionKey)
            }
        }
    )

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val controls by viewModel.controls.collectAsStateWithLifecycle()
    val expensesState by viewModel.expensesState.collectAsStateWithLifecycle()
    val settings by viewModel.settingsState.collectAsStateWithLifecycle()
    val categories by viewModel.categoriesState.collectAsStateWithLifecycle()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showAccountMenu by remember { mutableStateOf(false) }
    var showBudgetDialog by remember { mutableStateOf(false) }
    var showCategorySettingsDialog by remember { mutableStateOf(false) }
    var showExpenseSheet by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<Expense?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(controls.snackbarMessage) {
        val msg = controls.snackbarMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    val userInitial = remember(currentUser) {
        currentUser?.displayName?.firstOrNull()?.uppercaseChar()?.toString()
            ?: currentUser?.email?.firstOrNull()?.uppercaseChar()?.toString()
            ?: "I"
    }

    val recentCardNames = remember(expensesState) {
        val list = (expensesState as? UiState.Success)?.data.orEmpty()
        list.mapNotNull { it.cardName?.trim()?.takeIf { name -> name.isNotEmpty() } }.distinct()
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    ),
                    title = {
                        Column {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            if (currentUser == null) {
                                Text(
                                    text = stringResource(R.string.guest_mode_subtitle),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            } else {
                                currentUser.displayName?.takeIf { it.isNotBlank() }?.let { name ->
                                    Text(
                                        text = "Hola, ${name.substringBefore(" ")}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    },
                    actions = {
                        Box {
                            IconButton(
                                onClick = { showAccountMenu = true },
                                modifier = Modifier.testTag("user_menu_button")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isGuest) MaterialTheme.colorScheme.tertiaryContainer
                                                else MaterialTheme.colorScheme.primaryContainer
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isGuest) {
                                            Icon(
                                                imageVector = Icons.Rounded.PersonOutline,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.tertiary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        } else {
                                            Text(
                                                text = userInitial,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.Rounded.MoreVert,
                                        contentDescription = stringResource(R.string.cd_user_menu),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            DropdownMenu(
                                expanded = showAccountMenu,
                                onDismissRequest = { showAccountMenu = false }
                            ) {
                                if (currentUser == null) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = stringResource(R.string.guest_mode_subtitle),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.tertiary
                                            )
                                        },
                                        onClick = {},
                                        enabled = false
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.label_connect_google)) },
                                        leadingIcon = {
                                            Icon(Icons.Rounded.AccountCircle, contentDescription = null)
                                        },
                                        onClick = {
                                            showAccountMenu = false
                                            onGoogleSignInClicked(
                                                context = context,
                                                credentialManager = credentialManager,
                                                onAuthSuccess = {},
                                                onAuthError = { msg -> viewModel.showMessage(msg) },
                                                scope = scope
                                            )
                                        }
                                    )
                                } else {
                                    currentUser.email?.takeIf { it.isNotBlank() }?.let { email ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = email,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            },
                                            onClick = {},
                                            enabled = false
                                        )
                                    }
                                }

                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.label_manage_categories)) },
                                    leadingIcon = {
                                        Icon(Icons.Rounded.Category, contentDescription = null)
                                    },
                                    onClick = {
                                        showAccountMenu = false
                                        showCategorySettingsDialog = true
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.label_set_budget)) },
                                    leadingIcon = {
                                        Icon(Icons.Rounded.Tune, contentDescription = null)
                                    },
                                    onClick = {
                                        showAccountMenu = false
                                        showBudgetDialog = true
                                    }
                                )

                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (isGuest) stringResource(R.string.label_exit_guest)
                                            else stringResource(R.string.label_sign_out)
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(Icons.AutoMirrored.Rounded.ExitToApp, contentDescription = null)
                                    },
                                    onClick = {
                                        showAccountMenu = false
                                        if (isGuest) {
                                            onExitGuestMode()
                                        } else {
                                            signOutUser(credentialManager, scope)
                                        }
                                    }
                                )
                            }
                        }
                    }
                )
            },
            bottomBar = {
                if (!useNavigationRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        tonalElevation = 2.dp
                    ) {
                        NavigationBarItem(
                            selected = selectedTabIndex == 0,
                            onClick = { selectedTabIndex = 0 },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTabIndex == 0) Icons.Filled.Today else Icons.Outlined.Today,
                                    contentDescription = stringResource(R.string.tab_today)
                                )
                            },
                            label = { Text(stringResource(R.string.tab_today)) },
                            modifier = Modifier.testTag("tab_today")
                        )
                        NavigationBarItem(
                            selected = selectedTabIndex == 1,
                            onClick = { selectedTabIndex = 1 },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTabIndex == 1) Icons.Filled.History else Icons.Outlined.History,
                                    contentDescription = stringResource(R.string.tab_history)
                                )
                            },
                            label = { Text(stringResource(R.string.tab_history)) },
                            modifier = Modifier.testTag("tab_history")
                        )
                        NavigationBarItem(
                            selected = selectedTabIndex == 2,
                            onClick = { selectedTabIndex = 2 },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTabIndex == 2) Icons.Filled.CreditCard else Icons.Outlined.CreditCard,
                                    contentDescription = stringResource(R.string.tab_credit)
                                )
                            },
                            label = { Text(stringResource(R.string.tab_credit)) },
                            modifier = Modifier.testTag("tab_credit")
                        )
                        NavigationBarItem(
                            selected = selectedTabIndex == 3,
                            onClick = { selectedTabIndex = 3 },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTabIndex == 3) Icons.Filled.BarChart else Icons.Outlined.BarChart,
                                    contentDescription = stringResource(R.string.tab_summary)
                                )
                            },
                            label = { Text(stringResource(R.string.tab_summary)) },
                            modifier = Modifier.testTag("tab_summary")
                        )
                    }
                }
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = {
                        editingExpense = null
                        showExpenseSheet = true
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null
                        )
                    },
                    text = {
                        Text(
                            text = if (selectedTabIndex == 2) "Nueva compra"
                            else stringResource(R.string.fab_new_expense),
                            style = MaterialTheme.typography.labelLarge
                        )
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.testTag("fab_add_expense")
                )
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavigationRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))
                        NavigationRailItem(
                            selected = selectedTabIndex == 0,
                            onClick = { selectedTabIndex = 0 },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTabIndex == 0) Icons.Filled.Today else Icons.Outlined.Today,
                                    contentDescription = stringResource(R.string.tab_today)
                                )
                            },
                            label = { Text(stringResource(R.string.tab_today)) },
                            modifier = Modifier.testTag("tab_today_rail")
                        )
                        NavigationRailItem(
                            selected = selectedTabIndex == 1,
                            onClick = { selectedTabIndex = 1 },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTabIndex == 1) Icons.Filled.History else Icons.Outlined.History,
                                    contentDescription = stringResource(R.string.tab_history)
                                )
                            },
                            label = { Text(stringResource(R.string.tab_history)) },
                            modifier = Modifier.testTag("tab_history_rail")
                        )
                        NavigationRailItem(
                            selected = selectedTabIndex == 2,
                            onClick = { selectedTabIndex = 2 },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTabIndex == 2) Icons.Filled.CreditCard else Icons.Outlined.CreditCard,
                                    contentDescription = stringResource(R.string.tab_credit)
                                )
                            },
                            label = { Text(stringResource(R.string.tab_credit)) },
                            modifier = Modifier.testTag("tab_credit_rail")
                        )
                        NavigationRailItem(
                            selected = selectedTabIndex == 3,
                            onClick = { selectedTabIndex = 3 },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTabIndex == 3) Icons.Filled.BarChart else Icons.Outlined.BarChart,
                                    contentDescription = stringResource(R.string.tab_summary)
                                )
                            },
                            label = { Text(stringResource(R.string.tab_summary)) },
                            modifier = Modifier.testTag("tab_summary_rail")
                        )
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    when (val state = expensesState) {
                        is UiState.Loading -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Cargando tus gastos...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        is UiState.Error -> {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = MaterialTheme.colorScheme.errorContainer
                                ) {
                                    Text(
                                        text = state.message.ifBlank {
                                            stringResource(R.string.error_generic_load)
                                        },
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(20.dp)
                                    )
                                }
                            }
                        }

                        is UiState.Success -> {
                            when (selectedTabIndex) {
                                0 -> {
                                    DailyExpensesTab(
                                        expenses = state.data,
                                        categories = categories,
                                        settings = settings,
                                        selectedDateKey = controls.selectedDateKey,
                                        selectedCategoryFilter = controls.selectedCategoryFilter,
                                        onShiftDay = viewModel::shiftDay,
                                        onSelectDate = viewModel::selectDate,
                                        onSelectCategoryFilter = viewModel::selectCategoryFilter,
                                        onEditExpense = { expense ->
                                            editingExpense = expense
                                            showExpenseSheet = true
                                        },
                                        onDeleteExpense = { expense ->
                                            viewModel.deleteExpense(expense.id)
                                        },
                                        onOpenBudgetSettings = { showBudgetDialog = true },
                                        onOpenCategorySettings = { showCategorySettingsDialog = true }
                                    )
                                }

                                1 -> {
                                    HistoryTab(
                                        expenses = state.data,
                                        categories = categories,
                                        settings = settings,
                                        selectedCategoryId = controls.historyCategoryFilter,
                                        searchQuery = controls.historySearchQuery,
                                        selectedPeriod = controls.historyPeriodFilter,
                                        onSelectCategoryFilter = viewModel::selectHistoryCategoryFilter,
                                        onSearchQueryChange = viewModel::setHistorySearchQuery,
                                        onSelectPeriodFilter = viewModel::setHistoryPeriodFilter,
                                        onClearFilters = viewModel::clearHistoryFilters,
                                        onEditExpense = { expense ->
                                            editingExpense = expense
                                            showExpenseSheet = true
                                        },
                                        onDeleteExpense = { expense ->
                                            viewModel.deleteExpense(expense.id)
                                        },
                                        onOpenCategorySettings = { showCategorySettingsDialog = true }
                                    )
                                }

                                2 -> {
                                    CreditTab(
                                        expenses = state.data,
                                        categories = categories,
                                        settings = settings,
                                        onEditExpense = { expense ->
                                            editingExpense = expense
                                            showExpenseSheet = true
                                        },
                                        onToggleSettled = viewModel::toggleCreditSettled
                                    )
                                }

                                else -> {
                                    MonthlySummaryTab(
                                        expenses = state.data,
                                        categories = categories,
                                        settings = settings,
                                        selectedMonthKey = controls.selectedMonthKey,
                                        onShiftMonth = viewModel::shiftMonth,
                                        onCategoryClick = { categoryId ->
                                            viewModel.setHistoryCategoryDirect(categoryId)
                                            selectedTabIndex = 1
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showExpenseSheet) {
        val currentEdit = editingExpense
        ExpenseSheetDialog(
            initialExpense = currentEdit,
            categories = categories,
            defaultDateKey = controls.selectedDateKey,
            defaultPaymentMethod = if (selectedTabIndex == 2) PaymentMethod.CREDIT_CARD else PaymentMethod.CASH,
            recentCardNames = recentCardNames,
            currencySymbol = settings.currencySymbol.ifBlank { "$" },
            isSaving = controls.isSaving,
            onOpenCategorySettings = { showCategorySettingsDialog = true },
            onDismiss = { showExpenseSheet = false },
            onSave = { title, amount, categoryId, categoryLabel, dateKey, note, paymentMethod, cardName, installments, creditSettled ->
                if (currentEdit == null) {
                    viewModel.addExpense(
                        title = title,
                        amount = amount,
                        categoryId = categoryId,
                        categoryLabel = categoryLabel,
                        dateKey = dateKey,
                        note = note,
                        paymentMethod = paymentMethod,
                        cardName = cardName,
                        installments = installments,
                        creditSettled = creditSettled,
                        onDone = { showExpenseSheet = false }
                    )
                } else {
                    viewModel.updateExpense(
                        expenseId = currentEdit.id,
                        title = title,
                        amount = amount,
                        categoryId = categoryId,
                        categoryLabel = categoryLabel,
                        dateKey = dateKey,
                        note = note,
                        paymentMethod = paymentMethod,
                        cardName = cardName,
                        installments = installments,
                        creditSettled = creditSettled,
                        onDone = { showExpenseSheet = false }
                    )
                }
            },
            onDelete = if (currentEdit != null) { id ->
                viewModel.deleteExpense(id, onDone = { showExpenseSheet = false })
            } else null
        )
    }

    if (showBudgetDialog) {
        BudgetSettingsDialog(
            currentSettings = settings,
            isSaving = controls.isSaving,
            onDismiss = { showBudgetDialog = false },
            onSave = { dailyBudget, currencySymbol ->
                viewModel.saveUserSettings(
                    dailyBudget = dailyBudget,
                    currencySymbol = currencySymbol,
                    onDone = { showBudgetDialog = false }
                )
            }
        )
    }

    if (showCategorySettingsDialog) {
        CategorySettingsDialog(
            categories = categories,
            currencySymbol = settings.currencySymbol.ifBlank { "$" },
            isSaving = controls.isSaving,
            onDismiss = { showCategorySettingsDialog = false },
            onSaveCategory = { id, label, iconKey, colorHex, isEnabled, monthlyLimit, onDone ->
                viewModel.saveCategoryConfig(
                    id = id,
                    label = label,
                    iconKey = iconKey,
                    colorHex = colorHex,
                    isEnabled = isEnabled,
                    monthlyLimit = monthlyLimit,
                    onDone = onDone
                )
            },
            onToggleEnabled = viewModel::toggleCategoryEnabled,
            onDeleteCustomCategory = viewModel::deleteCustomCategory,
            onResetDefaults = viewModel::resetCategoryDefaults
        )
    }
}
