package hu.ait.walletify.ui.screens.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import hu.ait.walletify.data.model.AppNotification
import hu.ait.walletify.data.repository.FirestoreNotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface NotificationsUiState {
    data object Loading : NotificationsUiState
    data class Data(val notifications: List<AppNotification>) : NotificationsUiState
}

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val notificationRepository: FirestoreNotificationRepository
) : ViewModel() {

    private val _state = MutableStateFlow<NotificationsUiState>(NotificationsUiState.Loading)
    val state: StateFlow<NotificationsUiState> = _state.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    init {
        viewModelScope.launch {
            notificationRepository.observeNotifications().collect { notifications ->
                _state.value = NotificationsUiState.Data(notifications)
                _unreadCount.value = notifications.count { !it.isRead }
            }
        }
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            notificationRepository.markAsRead(notificationId)
        }
    }

    fun deleteNotification(notificationId: String) {
        viewModelScope.launch {
            notificationRepository.deleteNotification(notificationId)
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            val currentState = _state.value
            if (currentState is NotificationsUiState.Data) {
                currentState.notifications
                    .filter { !it.isRead }
                    .forEach { notificationRepository.markAsRead(it.notificationId) }
            }
        }
    }
}
