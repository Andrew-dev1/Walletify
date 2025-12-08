package hu.ait.walletify.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSavedStateNavEntryDecorator
import androidx.navigation3.scene.rememberSceneSetupNavEntryDecorator
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

@Composable
fun NavHost(modifier: Modifier) {
    val backStack = rememberNavBackStack(InitialScreenRoute)
    val dashboardViewModel: DashboardViewModel = viewModel()
    val transactionsViewModel: TransactionsViewModel = viewModel()
    val savingsViewModel: SavingsViewModel = viewModel()
    val profileViewModel: ProfileViewModel = viewModel()
    val loginViewModel: LoginViewModel = viewModel()
    val plaidViewModel: PlaidViewModel = viewModel()

    NavDisplay(
        //modifier = modifier,
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSceneSetupNavEntryDecorator(),
            rememberSavedStateNavEntryDecorator(),
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
                    onComplete = { }, /* Moved Navigation to LaunchedEffect */
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
            entry<MainRoute> {
                MainScreen(
                    dashboardViewModel = dashboardViewModel,
                    transactionsViewModel = transactionsViewModel,
                    savingsViewModel = savingsViewModel,
                    profileViewModel = profileViewModel,
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
    onLogout: () -> Unit
) {
    val destinations = listOf<BottomDestination>(
        DashboardRoute,
        TransactionsRoute,
        SavingsRoute,
        ProfileRoute
    )
    var currentDestination by remember { mutableStateOf<BottomDestination>(DashboardRoute) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        currentDestination.label,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { /* Open notifications */ }) {
                        Badge(
                            containerColor = Color(0xFFFF5252)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notifications"
                            )
                        }
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
                SavingsScreen(
                    state = state,
                    modifier = Modifier.padding(padding)
                )
            }
            ProfileRoute -> {
                val state by profileViewModel.state.collectAsStateWithLifecycle()
                ProfileScreen(
                    state = state,
                    onLinkPlaid = profileViewModel::linkPlaidSandbox,
                    onExchangePublicToken = profileViewModel::exchangePublicToken,
                    onLogout = onLogout,
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}
