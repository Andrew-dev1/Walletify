package hu.ait.walletify.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import hu.ait.walletify.ui.components.WalletifyBottomBar
import hu.ait.walletify.ui.plaid.ConnectBankScreen
import hu.ait.walletify.ui.plaid.PlaidViewModel
import hu.ait.walletify.ui.screens.dashboard.DashboardScreen
import hu.ait.walletify.ui.screens.auth.ForgetPasswordScreen
import hu.ait.walletify.ui.screens.auth.InitialScreen
import hu.ait.walletify.ui.screens.auth.LoginViewModel
import hu.ait.walletify.ui.screens.auth.RegistrationCredentialsScreen
import hu.ait.walletify.ui.screens.auth.RegistrationQuestionsScreen
import hu.ait.walletify.ui.screens.dashboard.DashboardViewModel
import hu.ait.walletify.ui.screens.profile.ProfileScreen
import hu.ait.walletify.ui.screens.profile.ProfileViewModel
import hu.ait.walletify.ui.screens.savings.SavingsScreen
import hu.ait.walletify.ui.screens.savings.SavingsViewModel
import hu.ait.walletify.ui.screens.transctions.TransactionsScreen
import hu.ait.walletify.ui.screens.transctions.TransactionsViewModel
import hu.ait.walletify.ui.screens.auth.LoginUiState
import hu.ait.walletify.ui.screens.notifications.NotificationsScreen
import hu.ait.walletify.ui.screens.notifications.NotificationsViewModel
import hu.ait.walletify.ui.screens.transctions.AddTransactionScreen

@Composable
fun NavHost(modifier: Modifier) {
    val backStack = rememberNavBackStack(InitialScreenRoute)
    val dashboardViewModel: DashboardViewModel = viewModel()
    val transactionsViewModel: TransactionsViewModel = viewModel()
    val savingsViewModel: SavingsViewModel = viewModel()
    val profileViewModel: ProfileViewModel = viewModel()
    val loginViewModel: LoginViewModel = viewModel()
    val plaidViewModel: PlaidViewModel = viewModel()
    val notificationsViewModel: NotificationsViewModel = viewModel()

    NavDisplay(
        //modifier = modifier,
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        entryProvider = entryProvider {
            entry<InitialScreenRoute> {
                val state by loginViewModel.loginUiState.collectAsStateWithLifecycle()

                LaunchedEffect(state) {
                    if (state is LoginUiState.LoginSuccess) {
                        backStack.clear()
                        backStack.add(ConnectBankScreenRoute)
                        loginViewModel.resetState()
                    }
                }

                InitialScreen(
                    state = state,
                    onNavigateToRegistration = { backStack.add(RegistrationQuestionsScreenRoute) },
                    onLogin = loginViewModel::loginUser,
                    onNavigateToForgetPassword = { backStack.add(ForgetPasswordScreenRoute(email = it)) }
                )
            }
            entry<RegistrationQuestionsScreenRoute> {
                RegistrationQuestionsScreen(

                    onNext = { purpose, source ->
                        backStack.add(
                            RegistrationCredentialsScreenRoute(purpose,source)
                        ) },
                    onBack = { backStack.removeLastOrNull() }
                )
            }
            entry<RegistrationCredentialsScreenRoute> {(purpose, source) ->
                val state by loginViewModel.loginUiState.collectAsStateWithLifecycle()

                LaunchedEffect(state) {
                    if (state is LoginUiState.RegisterSuccess) {
                        backStack.clear()
                        backStack.add(ConnectBankScreenRoute)
                        loginViewModel.resetState()
                    }
                }

                RegistrationCredentialsScreen(
                    purpose = purpose,
                    source = source,
                    onComplete = { },
                    onBack = { backStack.removeLastOrNull()},
                    onRegisterUser = (loginViewModel::registerUser),
                    state = state

                )
            }

            entry<ConnectBankScreenRoute> {
                val state by plaidViewModel.plaidUiState.collectAsStateWithLifecycle()

                ConnectBankScreen(
                    state= state,
                    viewModel = plaidViewModel,
                    onConnected = {
                        backStack.clear()
                        backStack.add(MainRoute)
                    },
                    onSkip = {
                        backStack.clear()
                        backStack.add(MainRoute)
                    }
                )
            }

            entry<ForgetPasswordScreenRoute> {

                ForgetPasswordScreen(
                    emailInput = it.email,
                    onBack = { backStack.removeLastOrNull() },
                    onReset = (loginViewModel::forgetPassword),
                    modifier = modifier
                )
            }
            entry<DashboardScreenRoute>{
                val state by dashboardViewModel.state.collectAsStateWithLifecycle()
                DashboardScreen(
                    state = state,
                    modifier = modifier
                )
            }
            entry<NotificationsScreenRoute> {
                val notificationsState by notificationsViewModel.state.collectAsStateWithLifecycle()
                NotificationsScreen(
                    state = notificationsState,
                    onReturn = { backStack.removeLastOrNull() },
                    onMarkAsRead = notificationsViewModel::markAsRead,
                    onDelete = notificationsViewModel::deleteNotification,
                    onMarkAllAsRead = notificationsViewModel::markAllAsRead,
                    modifier = modifier
                )
            }
            entry<AddTransactionScreenRoute> {
                val state by transactionsViewModel.state.collectAsStateWithLifecycle()
                AddTransactionScreen(
                    state = state,
                    onSave = { merchant, category, amount, isDebit, date ->
                        transactionsViewModel.addManualTransaction(merchant, category, amount, isDebit, date)
                        backStack.removeLastOrNull()
                    },
                    onCancel = { backStack.removeLastOrNull() },
                    modifier = modifier
                )
            }

            entry<MainRoute> {
                MainScreen(
                    dashboardViewModel = dashboardViewModel,
                    transactionsViewModel = transactionsViewModel,
                    savingsViewModel = savingsViewModel,
                    profileViewModel = profileViewModel,
                    onNotifications = {
                        backStack.add(NotificationsScreenRoute)
                    },
                    onAddTransaction = {
                        backStack.add(AddTransactionScreenRoute)
                    },
                    onLogout = {
                        profileViewModel.logout()
                        backStack.clear()
                        loginViewModel.resetState()
                        backStack.add(InitialScreenRoute)
                    }
                )
            }

        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreen(
    dashboardViewModel: DashboardViewModel,
    transactionsViewModel: TransactionsViewModel,
    savingsViewModel: SavingsViewModel,
    profileViewModel: ProfileViewModel,
    onAddTransaction: () -> Unit,
    onNotifications: () -> Unit,
    onLogout: () -> Unit
) {
    val destinations = listOf<BottomDestination>(
        DashboardRoute,
        TransactionsRoute,
        SavingsRoute,
        ProfileRoute
    )
    var currentDestination by rememberSaveable(stateSaver = BottomDestinationSaver) {
        mutableStateOf(DashboardRoute)
    }

    Scaffold(
        topBar = {

            TopAppBar(
                title = {
                    Text(
                        currentDestination.label,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {when (currentDestination) {
                    DashboardRoute -> {
                        IconButton(onClick = onNotifications) {
                            Badge(
                                containerColor = Color(0xFFFF5252)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Notifications"
                                )
                            }
                        }
                    }
                    TransactionsRoute -> {
                        IconButton(onClick = onAddTransaction) {
                            Icon(
                                imageVector = Icons.Default.AddCircle,
                                contentDescription = "Notifications"
                            )
                        }
                    }
                    SavingsRoute -> {}
                    ProfileRoute -> {}
                    }
                },

                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },

        bottomBar = {
            WalletifyBottomBar(
                destinations = destinations,
                current = currentDestination,
                onDestinationSelected = { currentDestination = it }
            )
        }
    ) { padding ->
        when (currentDestination) {
            DashboardRoute -> {
                val state by dashboardViewModel.state.collectAsStateWithLifecycle()
                DashboardScreen(state = state, modifier = Modifier.padding(padding))
            }
            TransactionsRoute -> {
                val state by transactionsViewModel.state.collectAsStateWithLifecycle()
                TransactionsScreen(
                    state = state,
                    onCategorySelected = transactionsViewModel::onCategorySelected,
                    modifier = Modifier.padding(padding)
                )
            }
            SavingsRoute -> {
                val state by savingsViewModel.state.collectAsStateWithLifecycle()
                val showDialog by savingsViewModel.showCreateDialog.collectAsStateWithLifecycle()
                val editingBudget by savingsViewModel.editingBudget.collectAsStateWithLifecycle()
                SavingsScreen(
                    state = state,
                    showCreateDialog = showDialog,
                    editingBudget = editingBudget,
                    onShowCreateDialog = savingsViewModel::showCreateBudgetDialog,
                    onDismissCreateDialog = savingsViewModel::hideCreateBudgetDialog,
                    onCreateBudget = savingsViewModel::createBudget,
                    onDeleteBudget = savingsViewModel::deleteBudget,
                    onEditBudget = savingsViewModel::startEditingBudget,
                    onCancelEditBudget = savingsViewModel::cancelEditingBudget,
                    onUpdateBudget = savingsViewModel::updateBudget,
                    modifier = Modifier.padding(padding)
                )
            }
            ProfileRoute -> {
                val state by profileViewModel.state.collectAsStateWithLifecycle()
                ProfileScreen(
                    state = state,
                    onLinkPlaid = profileViewModel::linkPlaidSandbox,
                    onExchangePublicToken = profileViewModel::exchangePublicToken,
                    onUpdateProfile = profileViewModel::updateProfile,
                    onToggleNotifications = profileViewModel::toggleNotifications,
                    onChangePassword = profileViewModel::changePassword,
                    onLogout = onLogout,
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}

// converts the data objects into simple type String to save easily to bundle and then back
private val BottomDestinationSaver = Saver<BottomDestination, String>(
    save = { destination ->
        // Convert BottomDestination to String for saving
        when (destination) {
            is DashboardRoute -> "dashboard"
            is TransactionsRoute -> "transactions"
            is SavingsRoute -> "savings"
            is ProfileRoute -> "profile"
        }
    },
    restore = { savedLabel ->
        // Convert String back to BottomDestination
        when (savedLabel) {
            "dashboard" -> DashboardRoute
            "transactions" -> TransactionsRoute
            "savings" -> SavingsRoute
            "profile" -> ProfileRoute
            else -> DashboardRoute
        }
    }
)
