package hu.ait.walletify

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSavedStateNavEntryDecorator
import androidx.navigation3.scene.rememberSceneSetupNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import hu.ait.walletify.ui.navigation.DashboardScreenRoute
import hu.ait.walletify.ui.navigation.InitialScreenRoute
import hu.ait.walletify.ui.navigation.RegistrationCredentialsScreenRoute
import hu.ait.walletify.ui.navigation.RegistrationQuestionsScreenRoute
import hu.ait.walletify.ui.screens.DashboardScreen
import hu.ait.walletify.ui.screens.InitialScreen
import hu.ait.walletify.ui.screens.RegistrationCredentialsScreen
import hu.ait.walletify.ui.screens.RegistrationQuestionsScreen
import hu.ait.walletify.ui.theme.WalletifyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WalletifyTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavGraph(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}



@Composable
fun NavGraph(modifier: Modifier) {
    val backStack = rememberNavBackStack(InitialScreenRoute)

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
                InitialScreen(
                    onNavigateToRegistration = { backStack.add(RegistrationQuestionsScreenRoute) },
                    onLoginSuccessful = {
                        backStack.add(DashboardScreenRoute);
                        backStack.removeLastOrNull()
                    }
                )
            }
            entry<RegistrationQuestionsScreenRoute> {
                RegistrationQuestionsScreen(
                    onNext = { backStack.add(RegistrationCredentialsScreenRoute) },
                    onBack = { backStack.removeLastOrNull() }
                )
            }
            entry<RegistrationCredentialsScreenRoute> {
                RegistrationCredentialsScreen(
                    onComplete = { backStack.add(DashboardScreenRoute)},
                    onBack = { backStack.removeLastOrNull() }
                )
            }
            entry<DashboardScreenRoute>{
                DashboardScreen()
            }
        }
    )
}
