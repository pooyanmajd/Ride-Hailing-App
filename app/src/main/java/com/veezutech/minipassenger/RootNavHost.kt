package com.veezutech.minipassenger

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.veezutech.map.MapRoute
import com.veezutech.map.mapEntries

@Composable
fun RootNavHost(
    modifier: Modifier = Modifier,
) {
    val savedStateConfig = remember {
        SavedStateConfiguration {
            serializersModule = navKeySerializersModule
        }
    }

    // Your custom signature was: rememberNavBackStack(configuration, vararg elements)
    val backStack: NavBackStack<NavKey> = rememberNavBackStack(
        configuration = savedStateConfig,
        MapRoute,  // start destination
    )

    val entryProvider = remember {
        entryProvider {
            mapEntries()
        }
    }

    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider,
    )
}