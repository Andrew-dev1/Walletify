package hu.ait.walletify.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

//Serializable vs Parcelable
// Serial is stored in memory, quicker to access, and can be reflected in Java
// while Parcel would be quicker for cross activity and internet communication,
// Serial would be faster here
@Serializable
data object InitialScreenRoute: NavKey

@Serializable
data class ForgetPasswordScreenRoute(
    val email:String
): NavKey

@Serializable
data object RegistrationQuestionsScreenRoute: NavKey

@Serializable
data class RegistrationCredentialsScreenRoute(
    val purpose: String,
    val source: String
) : NavKey

@Serializable
data object DashboardScreenRoute: NavKey

@Serializable
data object ConnectBankScreenRoute : NavKey

// later swaps
@Serializable
data object MainRoute : NavKey

/**
 * Bottom navigation destinations.
 */
sealed interface BottomDestination : NavKey {
    val icon: String
    val label: String
}

@Serializable
data object DashboardRoute : BottomDestination {
    override val icon: String = "Dashboard"
    override val label: String = "Overview"
}

@Serializable
data object TransactionsRoute : BottomDestination {
    override val icon: String = "Transactions"
    override val label: String = "Transactions"
}

@Serializable
data object SavingsRoute : BottomDestination {
    override val icon: String = "Savings"
    override val label: String = "Savings"
}

@Serializable
data object ProfileRoute : BottomDestination {
    override val icon: String = "Profile"
    override val label: String = "Profile"
}

