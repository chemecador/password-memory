package com.chemecador.passwordmemory.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.chemecador.passwordmemory.ui.backup.BackupScreen
import com.chemecador.passwordmemory.ui.detail.EntryDetailScreen
import com.chemecador.passwordmemory.ui.edit.EditEntryScreen
import com.chemecador.passwordmemory.ui.list.EntryListScreen

@Composable
fun PasswordMemoryNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Destinations.LIST,
        modifier = modifier
    ) {
        composable(Destinations.LIST) {
            EntryListScreen(
                onEntryClick = { navController.navigate(Destinations.detail(it)) },
                onCreateClick = { navController.navigate(Destinations.edit()) },
                onBackupClick = { navController.navigate(Destinations.BACKUP) }
            )
        }
        composable(
            route = Destinations.DETAIL,
            arguments = listOf(navArgument(Destinations.ARG_ENTRY_ID) { type = NavType.LongType })
        ) {
            EntryDetailScreen(
                onBack = navController::popBackStack,
                onEditClick = { navController.navigate(Destinations.edit(it)) }
            )
        }
        composable(
            route = Destinations.EDIT,
            arguments = listOf(navArgument(Destinations.ARG_ENTRY_ID) { type = NavType.LongType })
        ) {
            EditEntryScreen(onDone = navController::popBackStack)
        }
        composable(Destinations.BACKUP) {
            BackupScreen(onBack = navController::popBackStack)
        }
    }
}
