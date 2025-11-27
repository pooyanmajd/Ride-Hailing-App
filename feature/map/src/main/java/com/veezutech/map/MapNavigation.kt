package com.veezutech.map

import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object MapRoute : NavKey

fun EntryProviderScope<NavKey>.mapEntries() {
    entry<MapRoute> {
        val viewModel: MapViewModel = hiltViewModel()
        val state by viewModel.uiState.collectAsStateWithLifecycle()

        MapScreen(
            state = state,
            onEvent = viewModel::onEvent,
        )
    }
}