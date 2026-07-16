package com.nexusroot.manager.ui.whitelist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexusroot.manager.data.DaemonConnector
import com.nexusroot.manager.data.model.WhitelistItem
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class WhitelistUiState(val items: List<WhitelistItem> = emptyList(), val query: String = "")

class WhitelistViewModel(private val connector: DaemonConnector) : ViewModel() {
    private val _uiState = MutableStateFlow(WhitelistUiState())
    val uiState: StateFlow<WhitelistUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            connector.whitelistFlow.collect { items ->
                _uiState.value = _uiState.value.copy(items = items)
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
    }

    fun toggleApp(packageName: String, allowed: Boolean) {
        viewModelScope.launch {
            connector.updateWhitelist(packageName, allowed)
        }
    }

    val filteredItems: StateFlow<List<WhitelistItem>> = _uiState
        .map { state ->
            val q = state.query.lowercase()
            state.items.filter {
                it.appName.lowercase().contains(q) || it.packageName.lowercase().contains(q)
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
}