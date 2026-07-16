package com.nexusroot.manager.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexusroot.manager.data.DaemonConnector
import com.nexusroot.manager.data.model.DaemonStatus
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HomeUiState(val status: DaemonStatus = DaemonStatus(false, "", "", ""))

class HomeViewModel(private val connector: DaemonConnector) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            connector.daemonStatusFlow.collect { status ->
                _uiState.value = HomeUiState(status)
            }
        }
    }
}