package hu.ait.walletify.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable

//Serializable vs Parcelable
// Serial is stored in memory, quicker to access, and can be reflected in Java
// while Parcel would be quicker for cross activity and internet communication,
// Serial would be faster here
@Serializable
data object InitialScreenRoute: NavKey

@Serializable
data object RegistrationQuestionsScreenRoute: NavKey

@Serializable
data object RegistrationCredentialsScreenRoute: NavKey

@Serializable
data object DashboardScreenRoute: NavKey